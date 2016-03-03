/*******************************************************************************
*
* Copyright (c) 2002-2007. John R. Nestor. All rights reserved.
*
* See the file XLICENSE.txt for use and redistribution information.
*
*******************************************************************************/

package com.persist.xact.exec;

import com.persist.xact.system.*;
import com.persist.xact.value.*;
import com.persist.xdom.*;
import java.util.*;

public final class Join {
    private XThread xt;
    private Exec exec;
    private VariableStack stack;
    private EValue evalue;
    private Errors errors;
    private Value value;

    private Vector<JoinInfo> joins;
    private int last;
    private StringBuffer sb;
    private final int CLUMP = 1024;     // maximum clumped size 
    private final int TOCLUMP = 12;   // strings this size or less are clumped 
	    
    public Join(Exec exec) {
	this.exec = exec;
	joins = new Vector<JoinInfo>(100);
	last = -1;
	sb = new StringBuffer(1024);
    }

    public void init() {
	xt = exec.xt;
	value = xt.cmd.value;
	stack = exec.stack;
	evalue = exec.evalue;
	errors = xt.errors;
    }

    public void start(Ctx ctx) {
	last ++;
	if (last >= joins.size()) {
	    joins.addElement(new JoinInfo());
	}
	JoinInfo ji = joins.elementAt(last);
	ji.stackFirst = stack.getTop() + 1;
	ji.bufferFirst = sb.length();
	ji.ctx = ctx;
	ji.onlyString = true;
	ji.isError = false;
	ji.spaceBefore = XDOM.EMPTY;
	ji.spaceAfter = XDOM.EMPTY;
    }

    private void write(XW w,Object oval,long ival,XPos pos) {
	if (oval == value.vlChar) {
	    w.write((char) ival);
	} else {
	    if (! (oval instanceof String)) {
		stack.push(oval,ival);
		evalue.getS(pos); // make sure its a String or VLCat 
		oval = stack.getTopOval();
		ival = stack.getTopIval();
		stack.pop();
	    }
	    if (oval instanceof String) {
		String s = (String) oval;
		w.write(s);
	    } else if (oval instanceof VLCat) {
		VLCat vlc = (VLCat) oval;
		int size = vlc.ovals.length;
		for (int i = 0; i < size; i++) {
		    write(w,vlc.ovals[i],0,pos);
		}
	    }
	}
    }

    private void flushBufferS(JoinInfo ji) {
	String s = sb.substring(ji.bufferFirst).intern();
	CtxStream cts = (CtxStream) ji.ctx;
	cts.w.write(s);
	sb.setLength(ji.bufferFirst);
    }
    
    private void flushBuffer1(JoinInfo ji) {
	String s = sb.substring(ji.bufferFirst).intern();
	stack.push(s,0);
	sb.setLength(ji.bufferFirst);
    }

    private void flushBuffer(JoinInfo ji) {
	Object oval = stack.getTopOval();
	long ival = stack.getTopIval();
	stack.pop();
	flushBuffer1(ji);
	stack.push(oval,ival);
    }

    private void add1(XPos pos) {
	Object oval = stack.getTopOval();
	long ival = stack.getTopIval();
	if (! XDOMValue.isString(oval,ival)) {
	    // make sure its a string
	    stack.pop();
	    int oldRunLevel = exec.runLevel;
	    exec.runLevel ++;
	    exec.opExec.makeString(oval,ival,value.ctxEval,false);
	    oval = stack.getTopOval();
	    ival = stack.getTopIval();
	    exec.runLevel = oldRunLevel;
	}
	JoinInfo ji = joins.elementAt(last);
	int bufferSize = sb.length() - ji.bufferFirst;
	int stackFirst = ji.stackFirst;
	int top = stack.getTop();
	if (ji.ctx instanceof CtxStream) {
	    CtxStream cts = (CtxStream) ji.ctx;
	    stack.pop();
	    if (cts.spaceAfter != XDOM.EMPTY) {
		cts.sendSpace();
	    }
	    write(cts.w,oval,ival,pos);
	} else {
	    VLCat vlc = null;
	    if (oval instanceof VLCat) {
		vlc = (VLCat) oval;
		if (stackFirst == top && bufferSize == 0) {
		    ji.spaceBefore = XDOM.spaceJoin(ji.spaceBefore,vlc.spaceBefore);
		} else {
		    ji.spaceAfter = XDOM.spaceJoin(ji.spaceAfter,vlc.spaceBefore);
		}
	    }
	    byte sa = ji.spaceAfter; 
	    if (sa != XDOM.EMPTY) {
		// insert spacer
		ji.spaceAfter = XDOM.EMPTY;
		stack.pop();
		flushBuffer1(ji);
		stack.push(XDOM.spaceString[sa],0);
		add1(pos); // this can change this ji !!!
		bufferSize = sb.length() - ji.bufferFirst;
		stack.push(oval,ival);
	    }
	    if	(oval == value.vlChar) {
		stack.pop();
		if (bufferSize >= CLUMP) {
		    flushBuffer1(ji);
		}
		sb.append((char) ival);
	    } else {
		if (! (oval instanceof String)) {
		    evalue.getS(pos);  // make sure its a String or VLCat 
		    oval = stack.getTopOval();
		    ival = stack.getTopIval();
		}
		if (oval instanceof String) {
		    String s = (String) oval;
		    int	size = s.length();
		    if (size == 0) {
			stack.pop();
		    } else if (size <= TOCLUMP) {
			stack.pop();
			if (bufferSize + size > CLUMP) {
			    flushBuffer1(ji);
			}
			sb.append(s);
		    } else {
			if (bufferSize > 0) {
			    flushBuffer(ji);
			}
		    }
		} else {
		    if (bufferSize > 0) {
			flushBuffer(ji);
		    }
		}
	    }
	    if (vlc != null) {
		ji.spaceAfter = XDOM.spaceJoin(ji.spaceAfter,vlc.spaceAfter);
	    }
	}
    }
    
    public void add(XPos pos) {
	Object oval = stack.getTopOval();
	long ival = stack.getTopIval();

	if (value.isNull(oval,ival)) {
	    stack.pop();
	    return;
	}

	JoinInfo ji = joins.elementAt(last);
	if (value.isError(oval,ival)) {
	    stack.pop();
	    ji.isError = true;
	    return;
	}
	
	int stackFirst = ji.stackFirst;
	int top = stack.getTop();
	int bufferSize = sb.length() - ji.bufferFirst;

	if (ji.ctx instanceof CtxStream) {
	    add1(pos);
	} else if (stackFirst == top && bufferSize == 0 && ji.spaceBefore == XDOM.EMPTY) {
	    // first add - leave as is 
	} else if (stackFirst+1 == top && bufferSize == 0 && ji.spaceBefore == XDOM.EMPTY) {
	    // second add - fix up first and second
	    stack.pop();
	    add1(pos);
	    stack.push(oval,ival);
	    add1(pos);
	} else {
	    // third or later add
	    add1(pos);
	}
    }

    public void addSpace(byte space,XPos pos) {
	// if to stream send space to stream ???
	JoinInfo ji = joins.elementAt(last);
	if (ji.ctx instanceof CtxStream) {
	    CtxStream cts = (CtxStream) ji.ctx;
	    cts.spaceAfter = XDOM.spaceJoin(cts.spaceAfter,space);
	    return;
	}
	int stackFirst = ji.stackFirst;
	int top = stack.getTop();
	int bufferSize = sb.length() - ji.bufferFirst;
	if (stackFirst == top && bufferSize == 0 && ji.spaceBefore == XDOM.EMPTY) {
	    // fix up singleton (make sure its a string)
	    add1(pos);
	}
	if (stackFirst == top + 1 && bufferSize == 0) {
	    ji.spaceBefore = XDOM.spaceJoin(ji.spaceBefore,space);
	} else {
   	    ji.spaceAfter = XDOM.spaceJoin(ji.spaceAfter,space);
	}
    }

    
    public void pop() {
	JoinInfo ji = joins.elementAt(last);
	int stackFirst = ji.stackFirst;
	int top = stack.getTop();
	int bufferFirst = ji.bufferFirst;
	int bufferSize = sb.length() - bufferFirst;
	if (bufferSize > 0) {
	    if (ji.ctx instanceof CtxStream) {
		flushBufferS(ji);
	    } else {
		flushBuffer1(ji);
	    }
	}
	top = stack.getTop();
	int chars = 0;
	if (top == stackFirst-1) {
	    if (ji.isError) {
		stack.pushError();
	    } else {
		stack.pushNull();
	    }
	} else if (top != stackFirst || ji.spaceBefore != XDOM.EMPTY || ji.spaceAfter != XDOM.EMPTY) {
	    int size = top-stackFirst+1;
	    VLCat vlc = new VLCat(size);
	    for (int i = 0; i < size; i++) {
		Object oval = stack.getOval(stackFirst+i);
		vlc.ovals[i] = oval;
		chars = chars + Value.stringSize(oval);
	    }
	    vlc.size = chars;
	    vlc.spaceBefore = ji.spaceBefore;
	    vlc.spaceAfter = ji.spaceAfter;
	    stack.setTop(stackFirst-1);
	    stack.push(vlc,0);
	}
	last --;
    }
}
