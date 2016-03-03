/*******************************************************************************
*
* Copyright (c) 2002-2007. John R. Nestor. All rights reserved.
*
* See the file XLICENSE.txt for use and redistribution information.
*
*******************************************************************************/

package com.persist.xact.exec;

import java.util.*;
import com.persist.xact.system.*;
import com.persist.xdom.*;
import com.persist.xact.value.*;
import com.persist.xact.bind.*;

public final class Frames {
    private XCmd cmd;
    private XThread xt;
    private Exec exec;
    private VariableStack stack;
    private VariableStack viewStack;
    private Errors errors;
    private Value value;

    private Vector<XFrame> frames;
    private int top;
    private long seed;

    private boolean debugFrames = false;

    private XFrame topSlink;
    private XFrame slink;

    private void check(String name) {
	if (slink != getTopFrame()) {
	    System.out.println(name+"1"+slink);
	    System.out.println(name+"2="+getTopFrame());
	    System.out.println();
	}
    }

    public void setTopSlink(XFrame newSlink) {
	topSlink = newSlink ;
	if (top < 0) slink = newSlink;
    }

    public void setSlink(XFrame newSlink) {
	slink = newSlink;
//	System.out.println("set:"+slink);
    }

    public Frames(Exec exec) {
	this.exec = exec;
    }

    public void init() {
	xt = exec.xt;
	errors = xt.errors;
	cmd = xt.cmd;
	value = cmd.value;
	frames = new Vector<XFrame>(20);
	stack = exec.stack;
	viewStack = exec.viewStack;
	top = -1;
	seed = 0;
	topSlink = null;
	slink = null;
    }

    private XFrame push1() {
	top = top + 1;
	XFrame f = null;
	if (frames.size() <= top) {
	    f = new XFrame();
	    f.uid = -1;
	    frames.addElement(f);
	    return f;
	} else {
	    f = frames.elementAt(top);
	}
	return f;
    }

    public void push(XScope scope) {
	check("push");
	int size = scope.size;
	XFrame f = push1();
	f.level = scope.level;
//	if (top <= 0) {
//	    f.slink = topSlink;
//	} else {
//	    f.slink = frames.elementAt(top-1);
//	}
	f.slink = slink;
	slink = f;
	
	f.base = stack.getTop() + 1;
	f.size = size;
	seed ++;
	f.uid = seed;
	f.scope = scope;
	f.caller = null;
	f.stack = stack;
	for (int i = 0; i < size; i++) {
	    stack.pushUninit();
	}
	if (debugFrames) {
	    System.out.println("push "+f.uid+","+f.level+" "+f);
	}
	check("xpush");
    }

    public void push(XScope scope,XFrame newSlink) {
	check("ypush0");
	int size = scope.size;
	XFrame f = push1();
	f.level = scope.level;
	f.slink = newSlink;
	slink = f;
	check("ypush1"); 
	f.base = stack.getTop() + 1;
	f.size = size;
	seed ++;
	f.uid = seed;
	f.scope = scope;
	f.caller = null;
	f.stack = stack;
	for (int i = 0; i < size; i++) {
	    stack.pushUninit();
	}
	if (debugFrames) {
	    System.out.println("push "+f.uid+","+f.level+" "+f);
	}
	check("ypush");
    }

    public void push(VLFunc vlf,int formals,XDOMElement tree,XDOM caller) {
	XScope scope = tree.scope;
	int size = scope.size - formals;
	XFrame f = push1();
	f.level = scope.level;
	f.slink = vlf.context;
	slink = f;
	f.base = stack.getTop() + 1 - formals;
	f.size = scope.size;
	seed ++;
	f.uid = seed;
	f.scope = scope;
	f.caller = caller;
	f.stack = stack;
	for (int i = 0; i < size; i++) {
	    stack.pushUninit();
	}
	if (debugFrames) {
	    System.out.println("pushfunc "+f.uid+","+f.level+" "+f);
	}
	check("zpush");
    }

    public void pushSelf(VLView vlv) {
	XScope scope = vlv.def.self.scope;
	int size = scope.size;
	XFrame f = push1();
	f.level = scope.level;
	f.slink = vlv.frame;
	slink = f;
	f.base = viewStack.getTop() + 1;
	f.size = scope.size;
	seed ++;
	f.uid = seed;
	f.scope = scope;
	f.caller = null;
	f.stack = viewStack;
	for (int i = 0; i < size; i++) {
	    f.stack.setVal(i,null,-2);
	}
	if (debugFrames) {
	    System.out.println("pushself "+f.uid+","+f.level+"("+size+")"+" "+f);
	}
	check("apush");
    }

    public XFrame pushFrame(XFrame newf) {
//	System.out.println("oldtop="+slink+" "+top);
	XFrame f = push1();
	//newf.save = f;
	frames.setElementAt(newf,top);
	slink = newf;
	if (debugFrames) {
	    System.out.println("pushframe "+newf.uid+","+newf.level+" "+newf);
	    System.out.println("oldpush="+f);
	}
	check("bpush");
	return f;
    }

    public void popFrame(boolean hasResult,XFrame old) {
//	System.out.println("oldpop="+old);
	pop(hasResult);
	frames.setElementAt(old,top+1);
    }

    public void pop(boolean hasResult) {
	XFrame f = frames.elementAt(top);
	//XFrame f1 = f;
	//if (f.save != null) {
	//f1 = f.save;
	//frames.setElementAt(f1,top);
	//}
	//f1.uid = -1;
	top = top - 1;
//	System.out.println("top="+top);
	slink = getFrame(top);
	check("pop1");
	if (hasResult && f.stack instanceof VariableStack) {
	    VariableStack vs = (VariableStack) f.stack;
	    int stop = vs.getTop();
	    if (f.base != stop) {
		vs.setVal(f.base,f.stack.getOval(stop),f.stack.getIval(stop));
		vs.setTop(f.base);
	    }
	}
	if (debugFrames) {
	    System.out.println("pop "+f);
	}
//	System.out.println("popnewtop="+getTopFrame()+" "+top);
	check("pop");
    }

    public int getTop() {
	return top;
    }

    public XFrame getFrame(int index) {
	if (index == -1) return topSlink;
	return frames.elementAt(index);
    }

    public XFrame getTopFrame() {
//	if (top == -1) return topSlink;
//	return frames.elementAt(top);
	return slink;
    }

    private void getDef(XFrame f1,XDefName def,int offset,XPos pos) {
	XFrame f = f1;
	int level = def.scope.level;
	if (def.scope.top) {
	    Load ld = cmd.loads.getLoad(level);
	    f = ld.frame;
	    Object oval = f.stack.getOval(offset);
	    long ival = f.stack.getIval(offset);
	    if (value.isUninit(oval,ival)) {
		if (pos != null) {
		    errors.error(Errors.EXEC,pos,"use of unitialized name");
		    stack.pushError();
		}
		stack.pushError();
	    } else {
		stack.push(oval,ival);
	    }
	    return;
	} else {
	    if (f == null) {
//		if (top == -1) {
//		    f = topSlink;
//		} else {
//		    f = frames.elementAt(top);
//		}
		f = slink;
	    }
//	    System.out.println("startgetdef:"+level+def.name);
	    while (true) {
		if (f == null) {
//		    System.out.println("*************************************get sync"+def.name);
		    errors.error(Errors.INTERNAL,null,"get sync error: can't find local "+def.name);
		    stack.pushError();
		    return;
		}
//		System.out.println("level="+f.level+" "+f.scope.tree.getTag());
		if (f.level == level) {
		    int index = f.base+offset;
		    Object oval = f.stack.getOval(index);
		    long ival = f.stack.getIval(index);
		    if (value.isUninit(oval,ival)) {
			if (pos != null) {
			    errors.error(Errors.EXEC,pos,"use of unitialized name");
			}
			stack.pushError();
		    } else {
			stack.push(oval,ival);
		    }
//		    System.out.println("endgetdef");
		    return;
		}
		f = f.slink;
	    }
	}
    }

    public void getDefVal(XFrame f,XDefName def,XPos pos) {
	getDef(f,def,def.offset,pos);
    }

    public void getDefVal(XDefName def,XPos pos) {
	getDef(null,def,def.offset,pos);
    }

    public void getDefView(XDefName def) {
	getDef(null,def,def.offset+1,null);
    }
    public void getDefView(XFrame f,XDefName def) {
	getDef(f,def,def.offset+1,null);
    }

    private void setDef(XFrame f,XDefName def,int offset) {
	int level = def.scope.level;
	if (def.scope.top) {
	    Load ld = cmd.loads.getLoad(level);
	    XFrame f1 = ld.frame;
	    Object oval = stack.getTopOval();
	    long ival = stack.getTopIval();
	    f1.stack.setVal(offset,oval,ival);
	    return;
	} else {
	    if (f == null) {
//		if (top == -1) {
//		    f = topSlink;
//		} else {
//		    f = frames.elementAt(top);
//		}
		f = slink;
	    }
//	    System.out.println("startsetdef:"+level);
	    while (true) {
		if (f == null) {
		    errors.error(Errors.INTERNAL,null,"set sync error: can't find local "+def.name);
		    return;
		}
//		System.out.println("level="+f.level);
		if (f.level == level) {
		    int index = f.base+offset;
		    Object oval = stack.getTopOval();
		    long ival = stack.getTopIval();
		    f.stack.setVal(index,oval,ival);
//		    System.out.println("endsetdef");
		    return;
		}
		f = f.slink;
	    }
	}
    }

    public void setDefVal(XDefName def) {
	setDef(null,def,def.offset);
    }

    public void setDefVal(XFrame f,XDefName def) {
	setDef(f,def,def.offset);
    }

    public void setDefView(XDefName def) {
	setDef (null,def,def.offset+1);
    }

}
