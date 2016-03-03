/*******************************************************************************
*
* Copyright (c) 2002-2007. John R. Nestor. All rights reserved.
*
* See the file XLICENSE.txt for use and redistribution information.
*
*******************************************************************************/

package com.persist.xact.exec;

import java.io.*;
import java.util.*;

import com.persist.xact.system.*;
import com.persist.xact.bind.*;
import com.persist.xdom.*;
import com.persist.xact.value.*;

public final class OpExec {
    private XThread xt;
    private Exec exec;
    private VariableStack stack;
    private Errors errors;
    private Value value;
    private EValue evalue;
    private Join join;
    private CallExec callExec;
    private XURL xurl;
    private XOption option;
    
    private BIExec biExec;

    public OpExec(Exec exec) {
	this.exec = exec;
    }

    public void init() {
	xt = exec.xt;
	stack = exec.stack;
	errors = exec.xt.errors;
	value = exec.xt.cmd.value;
	evalue = exec.evalue;
	join = exec.join;
	biExec = exec.biExec;
	callExec = exec.callExec;
	xurl = xt.xurl;
	option = xt.cmd.option;
    }

    /*************************************************************************
    *
    *   Assign Equal Percent Eq Neq
    *
    *************************************************************************/

    public void execAssign(XDOMCall tree) {
	exec.execExp(tree.getArg(2),value.ctxEval);
	Object oval = stack.getTopOval();
	long ival = stack.getTopIval();
	if (! value.isError(oval,ival)) {
	    int top1 = stack.getTop();
	    exec.execExp(tree.getArg(1),value.ctxAssign);
	    int top2 = stack.getTop();
	    if (top1 != top2) {
		errors.error(Errors.INTERNAL,tree.pos,"assign stack sync error "+top1+":"+top2);
	    }
	}
	stack.pop();
	stack.pushNull();
	if (exec.debug) {
	    xt.cmd.dconnect.assign(xt,oval,ival);
	}
    }

    public void execEqual(XDOMCall tree) {
	errors.error(Errors.EXEC,tree.pos,"= not permitted here");
	exec.execExp(tree.getArg(2),value.ctxEval);
    }

    public void execPercent(XDOMCall tree) {
	errors.error(Errors.EXEC,tree.pos,"% not permitted here");
	stack.pushError();
    }


    public void execTreeValue(XDOMCall tree) {
	errors.error(Errors.EXEC,tree.pos,"** not permitted here");
	stack.pushError();
    }

    public void execXcomp(XDOMCall tree,boolean eq) {
	XDOM arg1 = tree.getArg(1);
	XDOM arg2 = tree.getArg(2);
	exec.execExp(arg1,value.ctxEval);
	Object oval1 = stack.getTopOval();
	long ival1 = stack.getTopIval();
	stack.pop();
	if (value.isError(oval1,ival1)) {
	    stack.pushError();
	    return;
	}
	exec.execExp(arg2,value.ctxEval);
	Object oval2 = stack.getTopOval();
	long ival2 = stack.getTopIval();
	stack.pop();
	if (value.isError(oval2,ival2)) {
	    stack.pushError();
	    return;
	}
	if (oval1 == oval2 && ival1 == ival2) {
	    stack.push(value.vlBool,VLBool.toI(eq));
	    return;
	}
	stack.push(oval1,ival1);
	boolean s1 = false;
	boolean s2 = false;
	if (evalue.getS((XPos) null)) {
	    oval1 = stack.getTopOval();
	    ival1 = stack.getTopIval();
	    s1 = true;
	}
	stack.pop();
	stack.push(oval2,ival2);
	if (evalue.getS((XPos) null)) {
	    oval2 = stack.getTopOval();
	    ival2 = stack.getTopIval();
	    s2 = true;
	}
	stack.pop();
	if (s1 && s2) {
	    int cmp = Value.stringComp(oval1,ival1,oval2,ival2);
	    if (cmp == 0) {
		stack.push(value.vlBool,VLBool.toI(eq));
		return;
	    }
	}
	stack.push(value.vlBool,VLBool.toI(! eq));
    }

    /*************************************************************************
    *
    *   Range(low,high) Range[low,high](body) Range[low,high]
    *
    *************************************************************************/

    public void execDoBreak(XDOMName ntree) {
	if (xt.doBreak) {
	    stack.push(value.vlBool,1);
	} else {
	    stack.push(value.vlBool,0);
	}
    }

    public void execLangCurrent(XDOMName ntree) {
	stack.push(xt.currentLang,0);
    }
    
    public void Range(long low,long high,VLFunc vlf,XDOM caller,Ctx ctx) {
	boolean save = xt.doBreak;
	xt.doBreak = false;
	join.start(ctx);
	int last = (int) high;
	for (int i = (int) low;i <= last; i++) {
	    stack.push(value.vlInt,i);
	    callExec.execCallFunc(vlf,0,1,caller,value.ctxEval);
	    join.add(caller.pos);
	    if (xt.doBreak) break;
	}
	xt.doBreak = save;
	join.pop();
    }

    private void pushRange(long low,long high) {
	VLExt vle = new VLExt();
	vle.foval = xt.cmd.biBind.rangeE;
	vle.fival = 0;
	vle.eovals = new Object[2];
	vle.eivals = new long[2];
	vle.eovals[0] = value.vlInt;
	vle.eivals[0] = low;
	vle.eovals[1] = value.vlInt;
	vle.eivals[1] = high;
	stack.push(vle,0);
    }

    public void execRange(XDOMCall tree) {
	/* result is vlext for range[low,high] */
	boolean ok = true;
	XDOM arg1 = tree.getArg(1);
	XDOM arg2 = tree.getArg(2);
	long i1 = 0;
	long i2 = 0;
	if (evalue.getI(arg1)) {
	    i1 = stack.getTopIval();
	} else {
	    ok = false;
	}
	stack.pop();
	if (evalue.getI(arg2)) {
	    i2 = stack.getTopIval();
	} else {
	    ok = false;
	}
	stack.pop();
	if (ok) {
	    pushRange(i1,i2);
	} else {
	    stack.pushError();
	}
    }

    public void execRangeE(XDOMCall tree,Ctx ctx) {
	XDOM arg1 = tree.getArg(1);
	XDOM func = tree.getFunc();
	boolean ok = true;
	long i1 = 0;
	long i2 = 0;
	if (func instanceof XDOMName) {
	    XDOMName xn = (XDOMName) func;
	    int exts = xn.extSize();
	    if (exts == 2) {
		XDOM ext1 = xn.getExt(1);
		XDOM ext2 = xn.getExt(2);
		if (evalue.getI(ext1)) {
		    i1 = stack.getTopIval();
		} else {
		    ok = false;
		}
		stack.pop();
		if (evalue.getI(ext2)) {
		    i2 = stack.getTopIval();
		} else {
		    ok = false;
		}
		stack.pop();
	    } else {
		errors.error(Errors.EXEC,tree.pos,"range call has wrong form");
		ok = false;
	    }
	} else {
	    errors.error(Errors.EXEC,tree.pos,"range call has wrong form");
	    ok = false;
	}
	if (ok) {
	    exec.execExp(arg1,value.ctxEval);
	    Object oval = stack.getTopOval();
	    long ival = stack.getTopIval();
	    stack.pop();
	    if (oval instanceof VLFunc) {
		Range(i1,i2,(VLFunc) oval,tree,ctx);
	    } else {
		if (! value.isError(oval,ival)){
		    errors.error(Errors.EXEC,arg1.pos,"not a function");
		}
		stack.pushError();
	    }
	}
    }

    public void execRangeOpt(XDOMCall tree,Ctx ctx) {
	if (ctx == value.ctxAssign) {
	    errors.error(Errors.EXEC,tree.pos,"can't assign to this expression");
	    return;
	}
	XDOM arg1 = tree.getArg(1);
	XDOMCall func = (XDOMCall) tree.getFunc();
	boolean ok = true;
	long i1 = 0;
	long i2 = 0;
	XDOM a1 = func.getArg(1);
	XDOM a2 = func.getArg(2);
	if (evalue.getI(a1)) {
	    i1 = stack.getTopIval();
	} else {
	    ok = false;
	}
	stack.pop();
	if (evalue.getI(a2)) {
	    i2 = stack.getTopIval();
	} else {
	    ok = false;
	}
	stack.pop();
	if (ok) {
	    exec.execExp(arg1,value.ctxEval);
	    Object oval = stack.getTopOval();
	    long ival = stack.getTopIval();
	    stack.pop();
	    if (oval instanceof VLFunc) {
		Range(i1,i2,(VLFunc) oval,tree,ctx);
	    } else {
		if (! value.isError(oval,ival)){
		    errors.error(Errors.EXEC,arg1.pos,"not a function");
		}
		stack.pushError();
	    }
	}
    }

    public void execRangeE(XDOMName tree) {
	int exts = tree.extSize();
	if (exts != 2) {
	    errors.error(Errors.EXEC,tree.pos,"wrong number of name exts");
	} else {
	    boolean ok = true;
	    XDOM ext1 = tree.getExt(1);
	    XDOM ext2 = tree.getExt(2);
	    long i1 = 0;
	    long i2 = 0;
	    if (evalue.getI(ext1)) {
		i1 = stack.getTopIval();
	    } else {
		ok = false;
	    }
	    stack.pop();
	    if (evalue.getI(ext2)) {
		i2 = stack.getTopIval();
	    } else {
		ok = false;
	    }
	    stack.pop();
	    if (ok) {
		pushRange(i1,i2);
		return;
	    }
	}
	stack.pushError();
    }

    /*************************************************************************
    *
    *   Chars[s] Chars[s](body) 
    *   Lines[s] Lines[s](body) 
    *
    *************************************************************************/

    private void Chars1(Object oval,long ival,VLFunc vlf,XDOM caller) {
	if (oval instanceof VLCat) {
	    VLCat vlc = (VLCat) oval;
	    int size = vlc.ovals.length;
	    for (int i = 0;i < size;i++) {
		//Chars1(vlc.ovals[i],vlc.ivals[i],vlf,caller);
		Chars1(vlc.ovals[i],0,vlf,caller);
		if (xt.doBreak) break;
	    }
	} else if (oval instanceof VLChar) {
	    stack.push(value.vlInt,ival);
	    callExec.execCallFunc(vlf,0,1,caller,value.ctxEval);
	    join.add(caller.pos);
	} else {
	    String s = (String) oval;
	    int size = s.length();
	    for (int i = 0;i < size; i++) {
		char ch = s.charAt(i);
		stack.push(value.vlInt,ch);
		callExec.execCallFunc(vlf,0,1,caller,value.ctxEval);
		join.add(caller.pos);
		if (xt.doBreak) break;
	    }
	}
    }

    public void Chars(Object oval,long ival,VLFunc vlf,XDOM caller,Ctx ctx) {
	boolean save = xt.doBreak;
	xt.doBreak = false;
	join.start(ctx);
	Chars1(oval,ival,vlf,caller);
	xt.doBreak = save;
	join.pop();
    }

    private boolean isSimple(XDOM x) {
	int kind = x.getXKind();
	if (kind == XDOM.XSTRING) return true;
	if (kind == XDOM.XNAME) {
	    XDOMName xn = (XDOMName) x;
	    if (! (xn.def instanceof XDefFunc)) return true;
	}
	return false;
    }

    public void execChars(XDOMCall tree,Ctx ctx) {
	XDOM func = tree.getFunc();
	XDOM arg1 = tree.getArg(1);
	exec.execExp(arg1,value.ctxEval);
	Object oval = stack.getTopOval();
	long ival = stack.getTopIval();
	stack.pop();
	if (! (oval instanceof VLFunc)) {
	    if (! value.isError(oval,ival)){
		errors.error(Errors.EXEC,arg1.pos,"not a function");
	    }
	} else if (! (func instanceof XDOMName)) {
	    errors.error(Errors.EXEC,tree.pos,"Chars call has wrong form");
	} else {
	    VLFunc vlf = (VLFunc) oval;
	    XDOMName xn = (XDOMName) func;
	    int exts = xn.extSize();
	    if (exts == 1) {
		final XDOM ext1 = xn.getExt(1);
/*	does not handle charSet, just remove for now	
		if (option.optimize && TreeUtil.isFileRead(ext1)) {
		    boolean save = xt.doBreak;
		    xt.doBreak = false;
		    join.start(ctx);
		    XDOM x = TreeUtil.fileReadName(ext1);
		    if (evalue.getS(x)) {
			Object oval1 = stack.getTopOval();
			long ival1 = stack.getTopIval();
			stack.pop();
			String fname = XDOMValue.getString(oval1,ival1);
			XRCFile r = new XRCFile();
			if (r.open(fname)) {
			    char[] buff = new char[512];
			    while (true) {
				int cnt = r.read(buff);
				if (cnt == -1) break;
				String s = new String(buff,0,cnt).intern();
				Chars1(s,0,vlf,tree);
				if (xt.doBreak) break;
			    }
			} else {
			    stack.pushError();
			}
		    } else {
			stack.pop();
		    }
		    xt.doBreak = save;
		    join.pop();
		} else
*/		
		    if (option.optimize && ! isSimple(ext1)) {
		    boolean save = xt.doBreak;
		    xt.doBreak = false;
		    join.start(ctx);
		    final XThread xt1 = new XThread("Chars",xt.cmd);
		    xt1.parent = xt;
		    xt1.parentFrame = exec.frames.getTop();
		    xt1.exec.frames.setTopSlink(exec.frames.getTopFrame());
		    //xt1.loads = xt.loads;
		    xt1.errors = xt.errors;
		    VLQueue vlq = new VLQueue(1);
		    XR r = new XRQ(vlq);
		    final XW w = new XWQ(vlq);
		    final CtxStream ctxs = new CtxStream(w);
		    Thread t = new Thread(new Runnable() {
			public void run() {
			    try {
				if (xt1.exec.evalue.getS(ext1,ctxs)) {
				    Object oval1  = xt1.exec.stack.getTopOval();
				    long ival1 = xt1.exec.stack.getTopIval();
				    xt1.exec.stack.pop();
				    String s = XDOMValue.getString(oval1,ival1);
				    w.write(s);
				    ctxs.sendSpace();
				    w.close();
				}
			    } catch(Throwable e) {
				String s = e.getMessage();
				System.out.println("Chars thread fail:"+xt1.name+"="+e);
				xt.errors.fail(e,xt1.name);
			    }
			}
		    });
		    t.start();
		    char[] buff = new char[512];
		    while (true) {
			int cnt = -1;
			cnt = r.read(buff);
			if (cnt == -1) break;
			if (! xt.doBreak) {
			    String s = new String(buff,0,cnt).intern();
			    Chars1(s,0,vlf,tree);
			}
		    }
		    try {
			t.join();
		    } catch(Exception e) {
		    }
		    xt.doBreak = save;
		    join.pop();
		} else {
		    if (evalue.getS(ext1)) {
			Object oval1  = stack.getTopOval();
			long ival1 = stack.getTopIval();
			stack.pop();
			Chars(oval1,ival1,vlf,tree,ctx);
		    }
		}
		return;
	    } else {
		errors.error(Errors.EXEC,tree.pos,"Chars call has wrong form");
	    }
	}
	stack.pushError();
    }

    private void pushChars(Object oval,long ival) {
	VLExt vle = new VLExt();
	vle.foval = xt.cmd.biBind.chars;
	vle.fival = 0;
	vle.eovals = new Object[1];
	vle.eivals = new long[1];
	vle.eovals[0] = oval;
	vle.eivals[0] = ival;
	stack.push(vle,0);
    }

    public void execChars(XDOMName tree) {
	int exts = tree.extSize();
	if (exts != 1) {
	    errors.error(Errors.EXEC,tree.pos,"wrong number of name exts");
	    stack.pushError();
	} else {
	    boolean ok = true;
	    XDOM ext1 = tree.getExt(1);
	    if (evalue.getS(ext1)) {
		Object oval = stack.getTopOval();
		long ival = stack.getTopIval();
		stack.pop();
		pushChars(oval,ival);
	    }
	}
    }

    private void Lines1(Object oval,long ival,VLFunc vlf,XDOM caller,
			StringBuffer sb) {
	if (oval instanceof VLCat) {
	    VLCat vlc = (VLCat) oval;
	    int size = vlc.ovals.length;
	    for (int i = 0;i < size;i++) {
		//Lines1(vlc.ovals[i],vlc.ivals[i],vlf,caller,sb);
		Lines1(vlc.ovals[i],0,vlf,caller,sb);
		if (xt.doBreak) break;
	    }
	} else if (oval instanceof VLChar) {
	    if (ival == '\r') {
	    } else if (ival == '\n') {
		String val = sb.toString().intern();
		sb.setLength(0);
		stack.push(val,0);
		callExec.execCallFunc(vlf,0,1,caller,value.ctxEval);
		join.add(caller.pos);
	    } else {
		sb.append((char) ival);
	    }
	} else {
	    String s = (String) oval;
	    int size = s.length();
	    for (int i = 0;i < size; i++) {
		char ch = s.charAt(i);
		if (ch == '\r') {
		} else if (ch == '\n') {
		    String val = sb.toString().intern();
		    sb.setLength(0);
		    stack.push(val,0);
		    callExec.execCallFunc(vlf,0,1,caller,value.ctxEval);
		    join.add(caller.pos);
		} else {
		    sb.append(ch);
		}
		if (xt.doBreak) break;
	    }
	}
	String val = sb.toString().intern();
	sb.setLength(0);
	stack.push(val,0);
	callExec.execCallFunc(vlf,0,1,caller,value.ctxEval);
	join.add(caller.pos);
    }

    public void Lines(Object oval,long ival,VLFunc vlf,XDOM caller,Ctx ctx) {
	StringBuffer sb = new StringBuffer(150);
	boolean save = xt.doBreak;
	xt.doBreak = false;
	join.start(ctx);
	Lines1(oval,ival,vlf,caller,sb);
	if (sb.length() != 0 && ! xt.doBreak) {
	    String val = sb.toString().intern();
	    sb.setLength(0);
	    stack.push(val,0);
	    callExec.execCallFunc(vlf,0,1,caller,value.ctxEval);
	    join.add(caller.pos);
	}
	xt.doBreak = save;
	join.pop();
    }

    public void execLines(XDOMCall tree,Ctx ctx) {
	XDOM func = tree.getFunc();
	XDOM arg1 = tree.getArg(1);
	exec.execExp(arg1,value.ctxEval);
	Object oval = stack.getTopOval();
	long ival = stack.getTopIval();
	stack.pop();
	if (! (oval instanceof VLFunc)) {
	    if (! value.isError(oval,ival)){
		errors.error(Errors.EXEC,arg1.pos,"not a function");
	    }
	} else if (! (func instanceof XDOMName)) {
	    errors.error(Errors.EXEC,tree.pos,"Lines call has wrong form");
	} else {
	    VLFunc vlf = (VLFunc) oval;
	    XDOMName xn = (XDOMName) func;
	    int exts = xn.extSize();
	    if (exts == 1) {
		final XDOM ext1 = xn.getExt(1);
/*	does not handle charSet, just remove for now	
		if (option.optimize && TreeUtil.isFileRead(ext1)) {
		    StringBuffer sb = new StringBuffer(150);
		    boolean save = xt.doBreak;
		    xt.doBreak = false;
		    join.start(ctx);
		    XDOM x = TreeUtil.fileReadName(ext1);
		    if (evalue.getS(x)) {
			Object oval1 = stack.getTopOval();
			long ival1 = stack.getTopIval();
			stack.pop();
			String fname = XDOMValue.getString(oval1,ival1);
			XRCFile r = new XRCFile();
			if (r.open(fname)) {
			    char[] buff = new char[512];
			    while (true) {
				int cnt = r.read(buff);
				if (cnt == -1) break;
				String s = new String(buff,0,cnt).intern();
				Lines1(s,0,vlf,tree,sb);
				if (xt.doBreak) break;
			    }
			} else {
			    stack.pushError();
			}
		    } else {
			stack.pop();
		    }
		    if (sb.length() != 0 && ! xt.doBreak) {
			String val = sb.toString().intern();
			sb.setLength(0);
			stack.push(val,0);
			callExec.execCallFunc(vlf,0,1,tree,value.ctxEval);
			join.add(tree.pos);
		    }
		    xt.doBreak = save;
		    join.pop();
		} else
*/		
		if (option.optimize && ! isSimple(ext1)) {
		    StringBuffer sb = new StringBuffer(150);
		    boolean save = xt.doBreak;
		    xt.doBreak = false;
		    join.start(ctx);
		    final XThread xt1 = new XThread("Lines",xt.cmd);
		    xt1.parent = xt;
		    xt1.parentFrame = exec.frames.getTop();
		    xt1.exec.frames.setTopSlink(exec.frames.getTopFrame());
		    //xt1.loads = xt.loads;
		    xt1.errors = xt.errors;
		    VLQueue vlq = new VLQueue(1);
		    XR r = new XRQ(vlq);
		    final XW w = new XWQ(vlq);
		    final CtxStream ctxs = new CtxStream(w);
		    Thread t = new Thread(new Runnable() {
			public void run() {
			    try {
				if (xt1.exec.evalue.getS(ext1,ctxs)) {
				    Object oval1  = xt1.exec.stack.getTopOval();
				    long ival1 = xt1.exec.stack.getTopIval();
				    xt1.exec.stack.pop();
				    String s = XDOMValue.getString(oval1,ival1);
				    ctxs.sendSpace();
				    w.write(s);
				    w.close();
				}
			    } catch(Throwable e) {
				String s = e.getMessage();
				System.out.println("Lines thread fail:"+xt1.name+"="+e);
				xt.errors.fail(e,xt1.name);
			    }
			}
		    });
		    t.start();
		    char[] buff = new char[512];
		    while (true) {
			int cnt = -1;
			cnt = r.read(buff);
			if (cnt == -1) break;
			if (! xt.doBreak) {
			    String s = new String(buff,0,cnt).intern();
			    Lines1(s,0,vlf,tree,sb);
			}
		    }
		    if (sb.length() != 0 && ! xt.doBreak) {
			String val = sb.toString().intern();
			sb.setLength(0);
			stack.push(val,0);
			callExec.execCallFunc(vlf,0,1,tree,value.ctxEval);
			join.add(tree.pos);
		    }
		    try {
			t.join();
		    } catch(Exception e) {
		    }
		    xt.doBreak = save;
		    join.pop();
		} else {
		    if (evalue.getS(ext1)) {
			Object oval1  = stack.getTopOval();
			long ival1 = stack.getTopIval();
			stack.pop();
			Lines(oval1,ival1,vlf,tree,ctx);
		    }
		}
		return;
	    } else {
		errors.error(Errors.EXEC,tree.pos,"Lines call has wrong form");
	    }
	}
	stack.pushError();
    }

    private void pushLines(Object oval,long ival) {
	VLExt vle = new VLExt();
	vle.foval = xt.cmd.biBind.lines;
	vle.fival = 0;
	vle.eovals = new Object[1];
	vle.eivals = new long[1];
	vle.eovals[0] = oval;
	vle.eivals[0] = ival;
	stack.push(vle,0);
    }

    public void execLines(XDOMName tree) {
	int exts = tree.extSize();
	if (exts != 1) {
	    errors.error(Errors.EXEC,tree.pos,"wrong number of name exts");
	    stack.pushError();
	} else {
	    boolean ok = true;
	    XDOM ext1 = tree.getExt(1);
	    if (evalue.getS(ext1)) {
		Object oval = stack.getTopOval();
		long ival = stack.getTopIval();
		stack.pop();
		pushLines(oval,ival);
	    }
	}
    }

    /*************************************************************************
    *
    *   Files
    *
    *************************************************************************/

    private String findCharSet(XDOM arg) {
	if (arg.getXKind() != XDOM.XCALL) return null;

	XDOMCall xc = (XDOMCall) arg;
	XDOM func = xc.getFunc();
	if (xc.argSize() != 2 || func.getXKind() != XDOM.XNAME) return null;
	XDOM arg1 = xc.getArg(1);
	XDOM arg2 = xc.getArg(2);
	XDOMName xn = (XDOMName) func;
	if (xn.getName() != "Equal") return null;
	if (arg1.getXKind() != XDOM.XNAME) return null;
	XDOMName xn1 = (XDOMName) arg1;
	if (xn1.getName() != "charSet") return null;
	if (! evalue.getS(arg2,value.ctxEval)) {
	    stack.pop();
	    return null;
	}
	Object oval = stack.getTopOval();
	long ival = stack.getTopIval();
	stack.pop();
	return XDOMValue.getString(oval,ival);
    }

    public void execRead(Object oval,long ival,XDOMCall tree,Ctx ctx) {
	int size = tree.argSize();
	String charSet = "ISO-8859-1";
	if (size > 0) {
	    XDOM arg1 = tree.getArg(1);
	    String newCharSet = findCharSet(arg1);
	    if (newCharSet == null) {
		errors.error(Errors.EXEC,arg1.pos,"bad charSet parameter");
	    } else {
		charSet = newCharSet;
	    }
	    if (size > 1) {
		errors.error(Errors.EXEC,tree.pos,"wrong number of actual parameters");
	    }
	}
	join.start(ctx);
	String fname = XDOMValue.getString(oval,ival);
	XRFile r;
	if (charSet == "byte") {
	    r = new XRBFile();
	} else {
	    r = new XRCFile(charSet);
	}
	if (r.open(fname)) {
	    char[] buff = new char[512];
	    while (true) {
		int cnt = r.read(buff);
		if (cnt == -1) break;
		String s = new String(buff,0,cnt).intern();
		stack.push(s,0);
		join.add(null);
	    }
	    r.close();
	} else {
	    errors.error(Errors.EXEC,tree.pos,"can't read file: "+fname);
	}
	join.pop();
    }

    private void write(XW w,Object oval,long ival) {
	if (oval instanceof String) {
	    w.write((String) oval);
	} else if (oval instanceof VLCat) {
	    VLCat vlc = (VLCat) oval;
	    int i;
	    int size = vlc.ovals.length;
	    for (i = 0; i < size; i++) {
		write(w,vlc.ovals[i],0);
	    }
	}
    }

    private void write(Object oval,long ival,XDOMCall tree,boolean append) {
	int size = tree.argSize();
	String charSet = "ISO-8859-1";
	if (size < 1 || size > 2) {
	    errors.error(Errors.EXEC,tree.pos,"wrong number of actual parameters");
	    stack.pushNull();
	    return;
	} else if (size == 2) {
	    XDOM arg2 = tree.getArg(2);
	    String newCharSet = findCharSet(arg2);
	    if (newCharSet == null) {
		errors.error(Errors.EXEC,arg2.pos,"bad charSet parameter");
	    } else {
		charSet = newCharSet;
	    }
	}
	XWFile w;
	if (charSet == "byte") {
	    w = new XWBFile();
	} else {
	    w = new XWCFile(charSet);
	}
	String fname = XDOMValue.getString(oval,ival);
	if (w.open(fname,append)) {
	    Ctx ctx = value.ctxEval;
	    if (option.optimize) {
		ctx = new CtxStream(w);
	    }
	    XDOM arg1 = tree.getArg(1);
	    if (evalue.getS(arg1,ctx)) {
		Object oval1 = stack.getTopOval();
		long ival1 = stack.getTopIval();
		stack.pop();
		write(w,oval1,ival1);
	    } else {
		stack.pop();
	    }
	    ctx.sendSpace();
	    w.close();
	} else {
	    errors.error(Errors.EXEC,tree.pos,"can't write file: "+fname);
	}
	stack.pushNull();
    }

    public void execWrite(Object oval,long ival,XDOMCall tree) {
	write(oval,ival,tree,false);
    }

    public void execAppend(Object oval,long ival,XDOMCall tree) {
	write(oval,ival,tree,true);
    }

    public void execExists(Object oval,long ival,XDOMCall tree) {
	String fname = XDOMValue.getString(oval,ival);
	File f = new File(fname);
	if (f.exists()) {
	    stack.push(value.vlBool,1);
	} else {
	    stack.push(value.vlBool,0);
	}
    }

    public void execCreate(Object oval,long ival,XDOMCall tree) {
	String fname = XDOMValue.getString(oval,ival);
	File f = new File(fname);
	try {
	    if (f.createNewFile()) {
		stack.push(value.vlBool,1);
	    } else {
		stack.push(value.vlBool,0);
	    }
	} catch(Exception e) {
	    errors.error(Errors.EXEC,tree.pos,"file create failed");
	    stack.pushError();
	}
    }

    public void execDelete(Object oval,long ival,XDOMCall tree) {
	String fname = XDOMValue.getString(oval,ival);
	File f = new File(fname);
	f.delete();
	stack.pushNull();
    }


    public void execRename(Object oval,long ival,XDOMCall tree) {
	String fname = XDOMValue.getString(oval,ival);
	File f = new File(fname);
	XDOM arg1 = tree.getArg(1);
	String fname1 = "";
	boolean ok = true;
	if (evalue.getS(arg1)) {
	    Object oval1 = stack.getTopOval();
	    long ival1 = stack.getTopIval();
	    fname1 = XDOMValue.getString(oval1,ival1);
	} else {
	    ok = false;
	}
	stack.pop();
	if (ok) {
	    System.out.println("RENAME");
	    File f1 = new File(fname1);
	    f.renameTo(f1);
	}
	stack.pushNull();
    }

    public void execIsDirectory(Object oval,long ival,XDOMCall tree) {
	String fname = XDOMValue.getString(oval,ival);
	File f = new File(fname);
	if (f.isDirectory()) {
	    stack.push(value.vlBool,1);
	} else {
	    stack.push(value.vlBool,0);
	}
    }

    public void execCreateDirectory(Object oval,long ival,XDOMCall tree) {
	String fname = XDOMValue.getString(oval,ival);
	File f = new File(fname);
	f.mkdir();
	stack.pushNull();
    }

    public void execFiles(Object oval,long ival,XDOMCall tree,Ctx ctx) {
	String fname = XDOMValue.getString(oval,ival);
	File f = new File(fname);
	XDOM arg1 = tree.getArg(1);
	exec.execExp(arg1,value.ctxEval);
	Object oval1 = stack.getTopOval();
	long ival1 = stack.getTopIval();
	stack.pop();
	if (oval1 instanceof VLFunc) {
	    VLFunc vlf = (VLFunc) oval1;
	    join.start(ctx);
	    if (f.isDirectory()) {
		int i;
		boolean save = xt.doBreak;
		String[] names = f.list();
		int size = names.length;
		xt.doBreak = false;
		for (i = 0; i < size; i++) {
		    stack.push(names[i],0);
		    callExec.execCallFunc(vlf,0,1,tree,value.ctxEval);
		    join.add(arg1.pos);
		    if (xt.doBreak) break;
		}
		xt.doBreak = save;
	    }
	    join.pop();
	} else {
	    if (! value.isError(oval1,ival1)) {
		errors.error(Errors.EXEC,arg1.pos,"iterator parameter not a function");
	    }
	    stack.pushError();
	}
    }

    /*************************************************************************
    *
    *   Errors Processing
    *
    *************************************************************************/

    private void execMsg(int kind,XDOMCall tree) {
	XPos pos = tree.pos;
	int args = tree.argSize();
	if (args < 1 || args > 2) {
	    errors.error(Errors.EXEC,tree.pos,"wrong number of actual parameters");
	    stack.pushError();
	    return;
	}
	if (evalue.getS(tree.getArg(1))) {
	    Object oval = stack.getTopOval();
	    long ival = stack.getTopIval();
	    String msg = XDOMValue.getString(oval,ival);
	    if (args == 2) {
		XDOM arg2 = tree.getArg(2);
		if (TreeUtil.isEqual(arg2)) {
		    String name = TreeUtil.getEqualName(arg2);
		    XDOM x = TreeUtil.stripEqual(arg2);
		    if (name == "where") {
			exec.execExp(x,value.ctxEval);
			Object oval1 = stack.getTopOval();
			long ival1 = stack.getTopIval();
			stack.pop();
			if (oval1 instanceof XDOM) {
			    XDOM x1 = (XDOM) oval1;
			    if (x1.pos != null) pos = x1.pos;
			} else {
			    errors.error(Errors.EXEC,x.pos,"not an xdom");
			}
		    } else {
			errors.error(Errors.EXEC,arg2.pos,"not where=");
		    }
		} else {
		    errors.error(Errors.EXEC,arg2.pos,"not where=");
		}
	    }
	    if (kind == Errors.ERROR) {
		errors.error(Errors.EXEC,pos,msg);
	    } else if (kind == Errors.WARN) {
		errors.warn(Errors.EXEC,pos,msg);
	    } else {
		errors.log(Errors.EXEC,pos,msg);
	    }
	}
	stack.pop();
	stack.pushNull();
    }

    public void execError(XDOMCall tree) {
	execMsg(Errors.ERROR,tree);
    }

    public void execWarn(XDOMCall tree) {
	execMsg(Errors.WARN,tree);
    }

    public void execLog(XDOMCall tree) {
	execMsg(Errors.LOG,tree);
    }

    /*************************************************************************
    *
    *   Type[...]
    *
    *************************************************************************/

    public void execType(Object oval,long ival) {
	if (value.isNull(oval,ival)) {
	    stack.push(value.vlBIT,VLBIV.Tvoid);
	} else if (oval == value.vlInt) {
	    stack.push(value.vlBIT,VLBIV.Tint);
	} else if (oval == value.vlFloat) {
	    stack.push(value.vlBIT,VLBIV.Tfloat);
	} else if (XDOMValue.isString(oval,ival)) {
	    stack.push(value.vlBIT,VLBIV.Tstring);
	} else if (value.isType(oval,ival)) {
	    stack.push(value.vlBIT,VLBIV.Ttype);
	} else if (value.isView(oval,ival)) {
	    stack.push(value.vlBIT,VLBIV.Tview);
	} else if (oval instanceof VLThread) {
	    stack.push(value.vlBIT,VLBIT.Tthread);
	} else if (oval instanceof VLQueue) {
	    stack.push(value.vlBIT,VLBIT.Tqueue);
	} else if (oval instanceof VLStream) {
	    stack.push(value.vlBIT,VLBIT.Tstream);
	} else if (oval instanceof VLLock) {
	    stack.push(value.vlBIT,VLBIT.Tlock);
	} else if (oval instanceof VLLxact) {
	    stack.push(value.vlBIT,VLBIV.TLxact);
	} else if (oval instanceof VLLrender) {
	    stack.push(value.vlBIT,VLBIV.TLrender);
	} else if (oval instanceof VLLerror) {
	    stack.push(value.vlBIT,VLBIV.TLerror);
	} else if (oval instanceof VLObj) {
	    VLObj vlo = (VLObj) oval;
	    stack.push(vlo.type,0);
	} else if (oval instanceof XDOM) {
	    XDOM x = (XDOM) oval;
	    int kind = x.getXKind();
	    switch (kind) {
		case XDOM.XELEMENT:{stack.push(value.vlBIT,VLBIV.TtreeElement);break;}
		case XDOM.XNAME:{stack.push(value.vlBIT,VLBIV.TtreeName);break;}
		case XDOM.XINT:{stack.push(value.vlBIT,VLBIV.TtreeInt);break;}
		case XDOM.XFLOAT:{stack.push(value.vlBIT,VLBIV.TtreeFloat);break;}
		case XDOM.XSTRING:{stack.push(value.vlBIT,VLBIV.TtreeString);break;}
		case XDOM.XCALL:{stack.push(value.vlBIT,VLBIV.TtreeCall);break;}
		case XDOM.XVALUE:{stack.push(value.vlBIT,VLBIV.TtreeValue);break;}
		default:{stack.pushError();}
	    }
	} else if (value.isFunc(oval,ival)) {
	    stack.push(value.vlBIT,VLBIV.Tfunc);
	} else if (value.isUninit(oval,ival)) {
	    stack.pushUninit();
	} else if (oval instanceof VLJava) {
	    stack.push(value.vlBIT,VLBIV.Tjava);
	} else if (value.isJObject(oval,ival)) {
	    stack.push(oval.getClass(),-1);
	} else {
	    stack.pushError();
	}
    }


    public void execType(XDOMName ntree) {
	exec.execExp(ntree.getExt(1),value.ctxEval);
	Object oval = stack.getTopOval();
	long ival = stack.getTopIval();
	stack.pop();
	execType(oval,ival);
    }

    /*************************************************************************
    *
    *   Question Tilde Assert
    *
    *************************************************************************/

    private void execTest(XDOMCall tree,boolean question) {
	int size = tree.argSize();
	boolean result = false;
	boolean ok = true;

	if (size != 2) {
	    errors.error(Errors.EXEC,tree.pos,"wrong number of actual parameters");
	    ok = false;
	}  else {
	    int code;
	    exec.execExp(tree.getArg(1),value.ctxEval);
	    if (! question && ! option.check) return;
	    Object oval = stack.getTopOval();
	    long ival = stack.getTopIval();
	    if (question) {
		stack.pop();
	    }
	    exec.execExp(tree.getArg(2),value.ctxEval);
	    Object oval1 = stack.getTopOval();
	    long ival1 = stack.getTopIval();
	    stack.pop();
	    code = biExec.hasView(oval,ival,oval1,ival1,tree.getArg(2));
	    if (code < 0) {
		ok = false;
	    } else if (code > 0) {
		result = true;
	    }
	}
	if (question) {
	    if (ok) {
		stack.push(value.vlBool,VLBool.toI(result));
	    } else {
		stack.pushError();
	    }
	} else {
	    if (ok) {
		if (! result) {
		    errors.error(Errors.EXEC,tree.getArg(1).pos,"does not have specified view");
		    stack.pop();
		    stack.pushError();
		}
	    } else {
		stack.pop();
		stack.pushError();
	    }
	}
    }

    public void execTilde(XDOMCall tree) {
	execTest(tree,false);
    }

    public boolean execAssert(XDOMCall tree,XPos cpos) {
	boolean result = true;
	if (option.check) {
	    if (evalue.getS(tree.getArg(1))) {
		Object oval = stack.getTopOval();
		long ival = stack.getTopIval();
		String s = XDOMValue.getString(oval,ival);
		if (s != "true") {
		    result = false;
		    if (s == "false") {
			s = "assertion failed";
		    }
		    if (cpos != null) {
			errors.error(Errors.EXEC,tree.pos,cpos,s);
		    } else {
			errors.error(Errors.EXEC,tree.pos,s);
		    }
		}
	    }
	    stack.pop();
	}
	stack.pushNull();
	return result;
    }

    public void execQuestion(XDOMCall tree) {
	execTest(tree,true);
    }

    /*************************************************************************
    *
    *   Extensible Ops
    *
    *************************************************************************/

    boolean execSysCover(Object oval1,long ival1,Object oval2,long ival2,XDOM caller) {
	if (oval1 instanceof VLView) {
	    VLView vlv = (VLView) oval1;
	    XDef def = callExec.findCallView(vlv,"sys:Cover",false,null);
	    if (def instanceof XDefFunc) {
		XDefFunc fdef = (XDefFunc) def;
		if (fdef.hasArgs) {
		    stack.push(oval2,ival2);
		    callExec.execCallView(def,vlv,true,
					  0,1,caller,value.ctxEval);
		    boolean isB = evalue.getB((XPos) null);
		    Object ovalR = stack.getTopOval();
		    long ivalR = stack.getTopIval();
		    stack.pop();
		    if (isB) {
			if (ivalR == 1) return true;
		    } else if (value.isError(ovalR,ivalR)) {
		    } else {
			errors.error(Errors.EXEC,caller.pos,"result of sys:Cover is not a boolean");
		    }
		} else {
		    errors.error(Errors.EXEC,caller.pos,"sys:Cover function has no parameters");
		}
	    }
	} else {
//	    errors.error(Errors.EXEC,caller.pos,"sys:Cover parameter not a view");
	}
	return false;
    }
    
    void execCover(Object oval1,long ival1,Object oval2,long ival2,XDOM caller) {
	if (oval1 == oval2 && ival1 == ival2) {
	    stack.push(oval1,ival1);
	    return;
	} else if (oval1 instanceof VLView && oval2 instanceof VLView) {
	    // check for same basetype
	    VLView vlv1 = (VLView) oval1;
	    VLView vlv2 = (VLView) oval2;
	    if (vlv1.def == vlv2.def) {
		stack.push(oval1,ival1);
		return;
	    }
	} else if (oval1 == value.vlBIT && oval2 == value.vlBIT) {
	    if (ival1 == VLBIV.Tfloat) {
		if (ival2 == VLBIV.Tint) {
		    stack.push(oval1,ival1); // float
		    return;
		}
	    } else if (ival2 == VLBIV.Tfloat) {
		if (ival1 == VLBIV.Tint) {
		    stack.push(oval2,ival2); // float
		    return;
		}
	    }
	}
	if (execSysCover(oval1,ival1,oval2,ival2,caller)) {
	    stack.push(oval1,ival1);
	    return;
	}
	if (execSysCover(oval2,ival2,oval1,ival1,caller)) {
	    stack.push(oval2,ival2);
	    return;
	}
	stack.pushNull();
    }

    public void execCover(XDOMCall tree,Ctx ctx) {
	XDOM arg1 = tree.getArg(1);
	exec.execExp(arg1,value.ctxEval);
	Object oval1 = stack.getTopOval();
	long ival1 = stack.getTopIval();
	stack.pop();
	XDOM arg2 = tree.getArg(2);
	exec.execExp(arg2,value.ctxEval);
	Object oval2 = stack.getTopOval();
	long ival2 = stack.getTopIval();
	stack.pop();
	execCover(oval1,ival1,oval2,ival2,tree);
    }
    void execInOpCall(String op,Object ovalt,long ivalt,Object oval1,long ival1,Object oval2,long ival2,XDOM caller) {
	if (value.isNull(ovalt,ivalt)) {
	    errors.error(Errors.EXEC,caller.pos,"unsupported "+op+" operand types");
	    stack.pushError();
	    return;
	}
	String sysop = ("sys:"+op).intern();
	if (ovalt instanceof VLView) {
	    VLView vlv = (VLView) ovalt;
	    XDef def = callExec.findCallView(vlv,sysop,false,null);
	    if (def instanceof XDefFunc) {
		XDefFunc fdef = (XDefFunc) def;
		if (fdef.hasArgs) {
		    stack.push(oval1,ival1);
		    stack.push(oval2,ival2);
		    callExec.execCallView(def,vlv,true,
					  0,2,caller,value.ctxEval);
		    return;
		}
	    }
	}
	errors.error(Errors.EXEC,caller.pos,"no "+sysop+" function");
	stack.pushError();
    }


    public void execAdd(XDOMCall tree,Ctx ctx) {
	int i;
	int size = tree.argSize();
	if (size == 0) {
	    errors.error(Errors.EXEC,tree.pos,"must have at least one actual parameter");
	    stack.pushError();
	} else {
	    XDOM arg1 = tree.getArg(1);
	    exec.execExp(arg1,value.ctxEval);
	    Object oval1 = stack.getTopOval();
	    long ival1 = stack.getTopIval();
	    stack.pop();
	    for (i = 2; i <= size; i++) {
		execType(oval1,ival1);
		Object oval1t = stack.getTopOval();
		long ival1t = stack.getTopIval();
		stack.pop();
		XDOM arg2 = tree.getArg(i);
		exec.execExp(arg2,value.ctxEval);
		Object oval2 = stack.getTopOval();
		long ival2 = stack.getTopIval();
		stack.pop();
		execType(oval2,ival2);
		Object oval2t = stack.getTopOval();
		long ival2t = stack.getTopIval();
		stack.pop();
		execCover(oval1t,ival1t,oval2t,ival2t,tree);
		Object ovalt = stack.getTopOval();
		long ivalt = stack.getTopIval();
		stack.pop();
		if (value.isError(oval1,ival1) || value.isError(oval2,ival2)) {
		    stack.pushError();
		} else if (ovalt == value.vlBIT && ivalt == VLBIV.Tint) {
		    execIadd(ival1,ival2);
		} else if (ovalt == value.vlBIT && ivalt == VLBIV.Tfloat) {
		    execFadd(oval1,ival1,tree.pos,oval2,ival2,tree.pos);
		} else {
		    execInOpCall("Add",ovalt,ivalt,oval1,ival1,oval2,ival2,tree);
		}
		oval1 = stack.getTopOval();
		ival1 = stack.getTopIval();
		stack.pop();
	    }
	    stack.push(oval1,ival1);
	}
    }

    public void execSub(XDOMCall tree,Ctx ctx) {
	XDOM arg1 = tree.getArg(1);
	exec.execExp(arg1,value.ctxEval);
	Object oval1 = stack.getTopOval();
	long ival1 = stack.getTopIval();
	stack.pop();
	execType(oval1,ival1);
	Object oval1t = stack.getTopOval();
	long ival1t = stack.getTopIval();
	stack.pop();
	XDOM arg2 = tree.getArg(2);
	exec.execExp(arg2,value.ctxEval);
	Object oval2 = stack.getTopOval();
	long ival2 = stack.getTopIval();
	stack.pop();
	execType(oval2,ival2);
	Object oval2t = stack.getTopOval();
	long ival2t = stack.getTopIval();
	stack.pop();
	execCover(oval1t,ival1t,oval2t,ival2t,tree);
	Object ovalt = stack.getTopOval();
	long ivalt = stack.getTopIval();
	stack.pop();

	if (value.isError(oval1,ival1) || value.isError(oval2,ival2)) {
	    stack.pushError();
	} else if (ovalt == value.vlBIT && ivalt == VLBIV.Tint) {
	    execIsub(ival1,ival2);
	} else if (ovalt == value.vlBIT && ivalt == VLBIV.Tfloat) {
	    execFsub(oval1,ival1,tree.pos,oval2,ival2,tree.pos);
	} else {
	    execInOpCall("Sub",ovalt,ivalt,oval1,ival1,oval2,ival2,tree);
	}
    }

    public void execMult(XDOMCall tree,Ctx ctx) {
	int i;
	int size = tree.argSize();
	if (size == 0) {
	    errors.error(Errors.EXEC,tree.pos,"must have at least one actual parameter");
	    stack.pushError();
	} else {
	    XDOM arg1 = tree.getArg(1);
	    exec.execExp(arg1,value.ctxEval);
	    Object oval1 = stack.getTopOval();
	    long ival1 = stack.getTopIval();
	    stack.pop();
	    for (i = 2; i <= size; i++) {
		execType(oval1,ival1);
		Object oval1t = stack.getTopOval();
		long ival1t = stack.getTopIval();
		stack.pop();
		XDOM arg2 = tree.getArg(i);
		exec.execExp(arg2,value.ctxEval);
		Object oval2 = stack.getTopOval();
		long ival2 = stack.getTopIval();
		stack.pop();
		execType(oval2,ival2);
		Object oval2t = stack.getTopOval();
		long ival2t = stack.getTopIval();
		stack.pop();
		execCover(oval1t,ival1t,oval2t,ival2t,tree);
		Object ovalt = stack.getTopOval();
		long ivalt = stack.getTopIval();
		stack.pop();
		if (value.isError(oval1,ival1) || value.isError(oval2,ival2)) {
		    stack.pushError();
		} else if (ovalt == value.vlBIT && ivalt == VLBIV.Tint) {
		    execImult(ival1,ival2);
		} else if (ovalt == value.vlBIT && ivalt == VLBIV.Tfloat) {
		    execFmult(oval1,ival1,tree.pos,oval2,ival2,tree.pos);
		} else {
		    execInOpCall("Mult",ovalt,ivalt,oval1,ival1,oval2,ival2,tree);
		}
		oval1 = stack.getTopOval();
		ival1 = stack.getTopIval();
		stack.pop();
	    }
	    stack.push(oval1,ival1);
	}
    }

    public void execDiv(XDOMCall tree,Ctx ctx) {
	XDOM arg1 = tree.getArg(1);
	exec.execExp(arg1,value.ctxEval);
	Object oval1 = stack.getTopOval();
	long ival1 = stack.getTopIval();
	stack.pop();
	execType(oval1,ival1);
	Object oval1t = stack.getTopOval();
	long ival1t = stack.getTopIval();
	stack.pop();
	XDOM arg2 = tree.getArg(2);
	exec.execExp(arg2,value.ctxEval);
	Object oval2 = stack.getTopOval();
	long ival2 = stack.getTopIval();
	stack.pop();
	execType(oval2,ival2);
	Object oval2t = stack.getTopOval();
	long ival2t = stack.getTopIval();
	stack.pop();
	execCover(oval1t,ival1t,oval2t,ival2t,tree);
	Object ovalt = stack.getTopOval();
	long ivalt = stack.getTopIval();
	stack.pop();

	if (value.isError(oval1,ival1) || value.isError(oval2,ival2)) {
	    stack.pushError();
	} else if (ovalt == value.vlBIT && ivalt == VLBIV.Tint) {
	    execIdiv(ival1,ival2,tree.pos);
	} else if (ovalt == value.vlBIT && ivalt == VLBIV.Tfloat) {
	    execFdiv(oval1,ival1,tree.pos,oval2,ival2,tree.pos,tree.pos);
	} else {
	    execInOpCall("Div",ovalt,ivalt,oval1,ival1,oval2,ival2,tree);
	}
    }

    public void execRem(XDOMCall tree,Ctx ctx) {
	XDOM arg1 = tree.getArg(1);
	exec.execExp(arg1,value.ctxEval);
	Object oval1 = stack.getTopOval();
	long ival1 = stack.getTopIval();
	stack.pop();
	execType(oval1,ival1);
	Object oval1t = stack.getTopOval();
	long ival1t = stack.getTopIval();
	stack.pop();
	XDOM arg2 = tree.getArg(2);
	exec.execExp(arg2,value.ctxEval);
	Object oval2 = stack.getTopOval();
	long ival2 = stack.getTopIval();
	stack.pop();
	execType(oval2,ival2);
	Object oval2t = stack.getTopOval();
	long ival2t = stack.getTopIval();
	stack.pop();
	execCover(oval1t,ival1t,oval2t,ival2t,tree);
	Object ovalt = stack.getTopOval();
	long ivalt = stack.getTopIval();
	stack.pop();

	if (value.isError(oval1,ival1) || value.isError(oval2,ival2)) {
	    stack.pushError();
	} else if (ovalt == value.vlBIT && ivalt == VLBIV.Tint) {
	    execIrem(ival1,ival2,tree.pos);
	} else {
	    execInOpCall("Rem",ovalt,ivalt,oval1,ival1,oval2,ival2,tree);
	}
    }

    public void execMinus(XDOMCall tree,Ctx ctx) {
	XDOM arg1 = tree.getArg(1);
	exec.execExp(arg1,value.ctxEval);
	Object oval1 = stack.getTopOval();
	long ival1 = stack.getTopIval();
	stack.pop();
	execType(oval1,ival1);
	Object oval1t = stack.getTopOval();
	long ival1t = stack.getTopIval();
	stack.pop();

	if (value.isError(oval1,ival1)) {
	    stack.pushError();
	} else if (ival1t == VLBIV.Tint) {
	    execIminus(ival1);
	} else if (ival1t == VLBIV.Tfloat) {
	    execFminus(oval1,ival1,tree.pos);
	} else {
	    errors.error(Errors.EXEC,tree.pos,"illegal Minus operand type");
	    stack.pushError();
	}
    }

    public void execCat(XDOMCall tree,Ctx ctx) {
	int size = tree.argSize();
	if (size == 0) {
	    errors.error(Errors.EXEC,tree.pos,"must have at least one actual parameter");
	    stack.pushError();
	} else {
	    join.start(ctx);
	    int i;
	    for (i = 1; i <= size; i++) {
		XDOM arg = tree.getArg(i);
		if (evalue.getS(arg)) {
		    join.add(arg.pos);
		}
	    }
	    join.pop();
	}
    }

    public void execAnd(XDOMCall tree,Ctx ctx) {
	int i;
	int size = tree.argSize();
	long val = 1;
	boolean ok = true;

	for (i = 1; i <= size; i++) {
	    if (evalue.getB(tree.getArg(i))) {
		long ival = stack.getTopIval();
		if (ival == 0) val = 0;
	    } else {
		ok = false;
	    }
	    stack.pop();
	    if (val == 0) break;
	}
	if (ok) {
	    stack.push(value.vlBool,val);
	} else {
	    stack.pushError();
	}
    }

    public void execOr(XDOMCall tree,Ctx ctx) {
	int i;
	int size = tree.argSize();
	long val = 0;
	boolean ok = true;

	for (i = 1; i <= size; i++) {
	    if (evalue.getB(tree.getArg(i))) {
		long ival = stack.getTopIval();
		if (ival == 1) val = 1;
	    } else {
		ok = false;
	    }
	    stack.pop();
	    if (val == 1) break;
	}
	if (ok) {
	    stack.push(value.vlBool,val);
	} else {
	    stack.pushError();
	}
    }

    public void execXor(XDOMCall tree,Ctx ctx) {
	execBxor(tree);
    }

    public void execNot(XDOMCall tree,Ctx ctx) {
	execBnot(tree);
    }

    public void execEq(XDOMCall tree,Ctx ctx) {
	XDOM arg1 = tree.getArg(1);
	exec.execExp(arg1,value.ctxEval);
	Object oval1 = stack.getTopOval();
	long ival1 = stack.getTopIval();
	stack.pop();
	execType(oval1,ival1);
	Object oval1t = stack.getTopOval();
	long ival1t = stack.getTopIval();
	stack.pop();
	XDOM arg2 = tree.getArg(2);
	exec.execExp(arg2,value.ctxEval);
	Object oval2 = stack.getTopOval();
	long ival2 = stack.getTopIval();
	stack.pop();
	execType(oval2,ival2);
	Object oval2t = stack.getTopOval();
	long ival2t = stack.getTopIval();
	stack.pop();
	execCover(oval1t,ival1t,oval2t,ival2t,tree);
	Object ovalt = stack.getTopOval();
	long ivalt = stack.getTopIval();
	stack.pop();

	if (value.isError(oval1,ival1) || value.isError(oval2,ival2)) {
	    stack.pushError();
	} else if (ovalt == value.vlBIT && ivalt == VLBIV.Tstring) {
	    execScomp(oval1,ival1,tree.pos,oval2,ival2,tree.pos,false,true,false);
	} else if (ovalt == value.vlBIT && ivalt == VLBIV.Tint) {
	    execIeq(ival1,ival2);
	} else if (ovalt == value.vlBIT && ivalt == VLBIV.Tfloat) {
	    execFcomp(oval1,ival1,tree.pos,oval2,ival2,tree.pos,false,true,false);
	} else {
	    if (oval1 == oval2 && ival1 == ival2) {
		stack.push(value.vlBool,1);
	    } else {
		stack.push(value.vlBool,0);
	    }
	}
    }

    public void execNe(XDOMCall tree,Ctx ctx) {
	XDOM arg1 = tree.getArg(1);
	exec.execExp(arg1,value.ctxEval);
	Object oval1 = stack.getTopOval();
	long ival1 = stack.getTopIval();
	stack.pop();
	execType(oval1,ival1);
	Object oval1t = stack.getTopOval();
	long ival1t = stack.getTopIval();
	stack.pop();
	XDOM arg2 = tree.getArg(2);
	exec.execExp(arg2,value.ctxEval);
	Object oval2 = stack.getTopOval();
	long ival2 = stack.getTopIval();
	stack.pop();
	execType(oval2,ival2);
	Object oval2t = stack.getTopOval();
	long ival2t = stack.getTopIval();
	stack.pop();
	execCover(oval1t,ival1t,oval2t,ival2t,tree);
	Object ovalt = stack.getTopOval();
	long ivalt = stack.getTopIval();
	stack.pop();

	if (value.isError(oval1,ival1) || value.isError(oval2,ival2)) {
	    stack.pushError();
	} else if (ovalt == value.vlBIT && ivalt == VLBIV.Tstring) {
	    execScomp(oval1,ival1,tree.pos,oval2,ival2,tree.pos,true,false,true);
	} else if (ovalt == value.vlBIT && ivalt == VLBIV.Tint) {
	    execIne(ival1,ival2);
	} else if (ovalt == value.vlBIT && ivalt == VLBIV.Tfloat) {
	    execFcomp(oval1,ival1,tree.pos,oval2,ival2,tree.pos,true,false,true);
	} else {
	    if (oval1 == oval2 && ival1 == ival2) {
		stack.push(value.vlBool,0);
	    } else {
		stack.push(value.vlBool,1);
	    }
	}
    }

    public void execLs(XDOMCall tree,Ctx ctx) {
	XDOM arg1 = tree.getArg(1);
	exec.execExp(arg1,value.ctxEval);
	Object oval1 = stack.getTopOval();
	long ival1 = stack.getTopIval();
	stack.pop();
	execType(oval1,ival1);
	Object oval1t = stack.getTopOval();
	long ival1t = stack.getTopIval();
	stack.pop();
	XDOM arg2 = tree.getArg(2);
	exec.execExp(arg2,value.ctxEval);
	Object oval2 = stack.getTopOval();
	long ival2 = stack.getTopIval();
	stack.pop();
	execType(oval2,ival2);
	Object oval2t = stack.getTopOval();
	long ival2t = stack.getTopIval();
	stack.pop();
	execCover(oval1t,ival1t,oval2t,ival2t,tree);
	Object ovalt = stack.getTopOval();
	long ivalt = stack.getTopIval();
	stack.pop();

	if (value.isError(oval1,ival1) || value.isError(oval2,ival2)) {
	    stack.pushError();
	} else if (ovalt == value.vlBIT && ivalt == VLBIV.Tstring) {
	    execScomp(oval1,ival1,tree.pos,oval2,ival2,tree.pos,true,false,false);
	} else if (ovalt == value.vlBIT && ivalt == VLBIV.Tint) {
	    execIls(ival1,ival2);
	} else if (ovalt == value.vlBIT && ivalt == VLBIV.Tfloat) {
	    execFcomp(oval1,ival1,tree.pos,oval2,ival2,tree.pos,true,false,false);
	} else {
	    errors.error(Errors.EXEC,tree.pos,"illegal Less operand type");
	    stack.pushError();
	}
    }

    public void execLe(XDOMCall tree,Ctx ctx) {
	XDOM arg1 = tree.getArg(1);
	exec.execExp(arg1,value.ctxEval);
	Object oval1 = stack.getTopOval();
	long ival1 = stack.getTopIval();
	stack.pop();
	execType(oval1,ival1);
	Object oval1t = stack.getTopOval();
	long ival1t = stack.getTopIval();
	stack.pop();
	XDOM arg2 = tree.getArg(2);
	exec.execExp(arg2,value.ctxEval);
	Object oval2 = stack.getTopOval();
	long ival2 = stack.getTopIval();
	stack.pop();
	execType(oval2,ival2);
	Object oval2t = stack.getTopOval();
	long ival2t = stack.getTopIval();
	stack.pop();
	execCover(oval1t,ival1t,oval2t,ival2t,tree);
	Object ovalt = stack.getTopOval();
	long ivalt = stack.getTopIval();
	stack.pop();

	if (value.isError(oval1,ival1) || value.isError(oval2,ival2)) {
	    stack.pushError();
	} else if (ovalt == value.vlBIT && ivalt == VLBIV.Tstring) {
	    execScomp(oval1,ival1,tree.pos,oval2,ival2,tree.pos,true,true,false);
	} else if (ovalt == value.vlBIT && ivalt == VLBIV.Tint) {
	    execIle(ival1,ival2);
	} else if (ovalt == value.vlBIT && ivalt == VLBIV.Tfloat) {
	    execFcomp(oval1,ival1,tree.pos,oval2,ival2,tree.pos,true,true,false);
	} else {
	    errors.error(Errors.EXEC,tree.pos,"illegal LessEq operand type");
	    stack.pushError();
	}
    }

    public void execGt(XDOMCall tree,Ctx ctx) {
	XDOM arg1 = tree.getArg(1);
	exec.execExp(arg1,value.ctxEval);
	Object oval1 = stack.getTopOval();
	long ival1 = stack.getTopIval();
	stack.pop();
	execType(oval1,ival1);
	Object oval1t = stack.getTopOval();
	long ival1t = stack.getTopIval();
	stack.pop();
	XDOM arg2 = tree.getArg(2);
	exec.execExp(arg2,value.ctxEval);
	Object oval2 = stack.getTopOval();
	long ival2 = stack.getTopIval();
	stack.pop();
	execType(oval2,ival2);
	Object oval2t = stack.getTopOval();
	long ival2t = stack.getTopIval();
	stack.pop();
	execCover(oval1t,ival1t,oval2t,ival2t,tree);
	Object ovalt = stack.getTopOval();
	long ivalt = stack.getTopIval();
	stack.pop();

	if (value.isError(oval1,ival1) || value.isError(oval2,ival2)) {
	    stack.pushError();
	} else if (ovalt == value.vlBIT && ivalt == VLBIV.Tstring) {
	    execScomp(oval1,ival1,tree.pos,oval2,ival2,tree.pos,false,false,true);
	} else if (ovalt == value.vlBIT && ivalt == VLBIV.Tint) {
	    execIgt(ival1,ival2);
	} else if (ovalt == value.vlBIT && ivalt == VLBIV.Tfloat) {
	    execFcomp(oval1,ival1,tree.pos,oval2,ival2,tree.pos,false,false,true);
	} else {
	    errors.error(Errors.EXEC,tree.pos,"illegal Greater operand type");
	    stack.pushError();
	}
    }

    public void execGe(XDOMCall tree,Ctx ctx) {
	XDOM arg1 = tree.getArg(1);
	exec.execExp(arg1,value.ctxEval);
	Object oval1 = stack.getTopOval();
	long ival1 = stack.getTopIval();
	stack.pop();
	execType(oval1,ival1);
	Object oval1t = stack.getTopOval();
	long ival1t = stack.getTopIval();
	stack.pop();
	XDOM arg2 = tree.getArg(2);
	exec.execExp(arg2,value.ctxEval);
	Object oval2 = stack.getTopOval();
	long ival2 = stack.getTopIval();
	stack.pop();
	execType(oval2,ival2);
	Object oval2t = stack.getTopOval();
	long ival2t = stack.getTopIval();
	stack.pop();
	execCover(oval1t,ival1t,oval2t,ival2t,tree);
	Object ovalt = stack.getTopOval();
	long ivalt = stack.getTopIval();
	stack.pop();

	if (value.isError(oval1,ival1) || value.isError(oval2,ival2)) {
	    stack.pushError();
	} else if (ovalt == value.vlBIT && ivalt == VLBIV.Tstring) {
	    execScomp(oval1,ival1,tree.pos,oval2,ival2,tree.pos,false,true,true);
	} else if (ovalt == value.vlBIT && ivalt == VLBIV.Tint) {
	    execIge(ival1,ival2);
	} else if (ovalt == value.vlBIT && ivalt == VLBIV.Tfloat) {
	    execFcomp(oval1,ival1,tree.pos,oval2,ival2,tree.pos,false,true,true);
	} else {
	    errors.error(Errors.EXEC,tree.pos,"illegal GreaterEq operand type");
	    stack.pushError();
	}
    }

    /*************************************************************************
    *
    *   String Ops
    *
    *************************************************************************/

    private void outVarExt(Object oval,long ival,Ctx ctx,boolean debug) {
	if (oval instanceof XDOMElement) {
	    XDOMElement e = (XDOMElement) oval;
	    int size = e.attrSize();
	    String sep = "";
	    for(int i=1; i <= size; i++) {
		if (sep == "") {
		    sep = ",";
		} else {
		    stack.push(sep,0);
		    join.add(null);
		}
		XDOM attr = e.getAttr(i);
		makeString(attr,0,ctx,debug);
		join.add(null);
	    }
	} else {
	    stack.push("???",0);
	    join.add(null);
	}
    }

    private void makeTypeString(VLView vlv,Ctx ctx,boolean debug) {
	join.start(ctx);
	XDOMName ntree = TreeUtil.getName(vlv.def.vtree);
	stack.push(ntree.getName(),0);
	join.add(null);
	String ext = "";
	if (vlv.def.hasExt) {
	    stack.push("[",0);
	    join.add(null);
	    String sep = "";
	    int i;
	    if (vlv.frame == null) {
		stack.push("...",0);
		join.add(null);
	    } else {
		for (i = 0; i < vlv.def.exts; i++) {
		    Object oval1= vlv.frame.stack.getOval(i);
		    long ival1 = vlv.frame.stack.getIval(i);
		    if (sep != "") {
			stack.push(sep,0);
			join.add(null);
		    }
		    if (vlv.def.varExts) {
			outVarExt(oval1,ival1,ctx,debug);
		    } else {
			makeString(oval1,ival1,ctx,debug);
			join.add(null);
		    }
		    sep = ",";
		}
	    }
	    stack.push("]",0);
	    join.add(null);
	}
	join.pop();
    }

    public void makeString(Object oval,long ival,Ctx ctx,boolean debug) {
	if (value.isNull(oval,ival)) {
	    stack.push("null",0);
	} else if (value.isError(oval,ival)) {
	    stack.push("error",0);
	} else if (value.isUninit(oval,ival)) {
	    stack.push("uninit",0);
	} else if (value.isInt(oval,ival)) {
	    stack.push(XDOMInt.toString(ival),0);
	} else if (value.isFloat(oval,ival)) {
	    double fval = Double.longBitsToDouble(ival);
	    stack.push(XDOMFloat.toString(fval),0);
	} else if (XDOMValue.isString(oval,ival)) {
	    stack.push(oval,ival);
	} else if (oval instanceof VL) {
	    VL vl = (VL) oval;
	    int kind = vl.getVKind();
	    switch (kind) {
		case VL.VJAVA:
		{
		    VLJava vlj = (VLJava) vl;
		    if (vlj.pkg == "") {
			stack.push("Java",0);
		    } else {
			stack.push(("Java."+vlj.pkg).intern(),0);
		    }
		    break;
		}
		case VL.VBIV: case VL.VBIT:
		{
		    stack.push(VLBIV.name[(int) ival],0);
		    break;
		}
		case VL.VBIF:
		{
		    VLBIF vlf = (VLBIF) oval;
		    stack.push(VLBIF.name[vlf.kind],0);
		    break;
		}
		case VL.VVIEW: case VL.VTYPE:
		{
		    VLView vlv = (VLView) oval;
		    makeTypeString(vlv,ctx,debug);
		    break;
		}
		case VL.VTHREAD:
		{
		    VLThread vlt = (VLThread) oval;
		    stack.push("thread:"+vlt.xt.name,0);
		    break;
		}
		case VL.VQUEUE:
		{
		    stack.push("queue",0);
		    break;
		}
		case VL.VSTREAM:
		{
		    stack.push("stream",0);
		    break;
		}
		case VL.VLOCK:
		{
		    stack.push("lock",0);
		    break;
		}
		case VL.VOBJ:
		{
		    if (debug) {
			// ???, temp hack no calls or errors while debugging
			stack.push("???",0);
		    } else {
			VLObj vlo = (VLObj) oval;
			VLType vlt = vlo.type;
			XDef def = callExec.findCallObject(vlt,"toString",false,null);
			if (def instanceof XDefFunc) {
			    XDefFunc fdef = (XDefFunc) def;
			    if (fdef.hasArgs) {
				callExec.execCallObject(def,vlt,vlo,0,true,
				    0,0,null,value.ctxEval);
			    } else {
				errors.error(Errors.EXEC,(XPos) null,"toString function has no parameters");
				stack.push("error",0);
			    } 
			} else {
			    errors.error(Errors.EXEC,(XPos) null,"no toString function");
			    stack.push("error",0);
			}
		    }
		    break;
		}
		case VL.VFUNC:
		{
		    VLFunc vlf = (VLFunc) oval;
		    XDOMName ntree = vlf.def.tree;
		    stack.push(ntree.getName(),0);
		    break;
		}
		case VL.VEXT:
		{
		    join.start(ctx);
		    VLExt vle = (VLExt) oval;
		    makeString(vle.foval,vle.fival,ctx,debug);
		    join.add(null);
		    stack.push("[",0);
		    join.add(null);
		    String sep = "";
		    int cnt = vle.eovals.length;
		    int i;
		    for (i = 0; i < cnt; i++) {
			if (sep != "") {
			    stack.push(sep,0);
			    join.add(null);
			}
			makeString(vle.eovals[i],vle.eivals[i],ctx,debug);
			join.add(null);
			sep = ",";
		    }
		    stack.push("]",0);
		    join.add(null);
		    join.pop();
		    break;
		}
		case VL.VDOT:
		{
		    VLDot vlc = (VLDot) oval;
		    join.start(ctx);
		    if (vlc.hasSelf) {
			makeString(vlc.selfoval,vlc.selfival,ctx,debug);
		    } else {
			makeString(vlc.voval,vlc.vival,ctx,debug);
		    }
		    join.add(null);
		    stack.push(".",0);
		    join.add(null);
		    if (vlc.f instanceof XDef) {
			XDef def = (XDef) vlc.f;
			stack.push(def.name,0);
		    } else {
			VLBIF vlf = (VLBIF) vlc.f;
			stack.push(VLBIF.name[vlf.kind],0);
		    }
		    join.add(null);
		    join.pop();
		    break;
		}
		case VL.VJMETHOD:
		{
		    VLJMethod vlm = (VLJMethod) oval;
		    join.start(ctx);
		    makeString(vlm.obj,-1,ctx,debug);
		    join.add(null);
		    stack.push(".",0);
		    join.add(null);
		    stack.push(vlm.name,0);
		    join.add(null);
		    join.pop();
		    break;
		}
		case VL.VLXACT:
		{
		    VLLxact vla = (VLLxact) oval;
		    join.start(ctx);
		    stack.push("langtype:xact(",0);
		    join.add(null);
		    makeString(vla.underLang,0,ctx,debug);
		    join.add(null);
		    stack.push(")",0);
		    join.add(null);
		    join.pop();
		    break;
		}
		case VL.VLERROR:
		{
		    stack.push("langtype:error()",0);
		    break;
		}
		case VL.VLRENDER:
		{
		    VLLrender vlr = (VLLrender) oval;
		    join.start(ctx);
		    stack.push("langtype:render(\"",0);
		    join.add(null);
		    stack.push(VLLrender.kinds[vlr.kind],0);
		    join.add(null);
		    stack.push("\")",0);
		    join.add(null);
		    join.pop();
		    break;
		}
		default:
		{
		    stack.push("???",0);
		    break;
		}
	    }
	} else if (oval instanceof Class) {
	    Class c = (Class) oval;
	    stack.push(c.getName(),0);
	} else if (oval instanceof XDOM) {
	    join.start(ctx);
	    if (oval instanceof XDOMElement) {
		stack.push("E..",0);
		join.add(null);
	    } else if (oval instanceof XDOMName) {
		XDOMName xn = (XDOMName) oval;
		stack.push(xn.getName(),0);
		join.add(null);
	    } else if (oval instanceof XDOMInt) {
		XDOMInt xi = (XDOMInt) oval;
		stack.push(xi.getString(),0);
		join.add(null);
	    } else if (oval instanceof XDOMFloat) {
		XDOMFloat xf = (XDOMFloat) oval;
		stack.push("F..",0);
		join.add(null);
	    } else if (oval instanceof XDOMString) {
		XDOMString xs = (XDOMString) oval;
		stack.push("S..",0);
		join.add(null);
	    } else if (oval instanceof XDOMCall) {
		XDOMCall xc = (XDOMCall) oval;
		int size = xc.argSize();
		XDOM f = xc.getFunc();
		boolean equal = false;
		if (xc.getKind() == XDOMCall.COP && size == 2 && f instanceof XDOMName) {
		    XDOMName xn = (XDOMName) f;
		    if (xn.getName() == "Equal") {
			equal = true;
		    }
		}
		if (equal) {
		    makeString(xc.getArg(1),0,ctx,debug);
		    join.add(null);
		    stack.push("=",0);
		    join.add(null);
		    makeString(xc.getArg(2),0,ctx,debug);
		    join.add(null);
		} else {
		    makeString(f,0,ctx,debug);
		    join.add(null);
		    stack.push("(",0);
		    join.add(null);
		    for (int i = 1; i <=size; i++) {
		    }
		    stack.push(")",0);
		    join.add(null);
		}
	    } else {
		stack.push("?..",0);
		join.add(null);
	    }
	    join.pop();
	} else if (value.isJObject(oval,ival)) {
	    stack.push(oval.toString(),0);
	} else {
	    stack.pushError();
	}
    }

    public void makeString(XDOMCall tree,Ctx ctx) {
	int size = tree.argSize();
	if (size == 1) {
	    XDOM arg1 = tree.getArg(1);
	    if (TreeUtil.isEqual(arg1) && TreeUtil.getEqualName(arg1) == "code") {
		XDOM x = TreeUtil.stripEqual(arg1);
		if (evalue.getI(x)) {
		    long ival = stack.getTopIval();
		    stack.pop();
		    stack.push(value.vlChar,ival);
		}
	    } else {
		exec.execExp(arg1,value.ctxEval);
		Object oval = stack.getTopOval();
		long ival = stack.getTopIval();
		stack.pop();
		makeString(oval,ival,ctx,false);
	    }
	} else {
	    errors.error(Errors.EXEC,tree.pos,"wrong number of actual parameters");
	    stack.pushError();
	}
    }

    private void fold(Object oval,long ival) {
	if (oval == value.vlChar) {
	    char ch = (char) ival;
	    if ('A' <= ch && ch <= 'Z') ch = (char) (ch - 'A' + 'a');
	    stack.push(value.vlChar,ch);
	    join.add(null);
	} else if (oval instanceof String) {
	    String s = (String) oval;
	    int size = s.length();
	    for (int i=0; i < size; i++) {
		char ch = s.charAt(i);
		if ('A' <= ch && ch <= 'Z') ch = (char) (ch - 'A' + 'a');
		stack.push(value.vlChar,ch);
		join.add(null);
	    }
	} else {
	    VLCat vlc = (VLCat) oval;
	    int size = vlc.ovals.length;
	    for (int i = 0; i < size; i++) {
		Object oval1 = vlc.ovals[i];
		fold(oval1,0);
	    }
	}
    }

    public void makeFold(XDOMCall tree,Ctx ctx) {
	int args = tree.argSize();
	if (args == 1) {
	    XDOM arg1 = tree.getArg(1);
	    if (evalue.getS(arg1)) {
		Object oval = stack.getTopOval();
		long ival = stack.getTopIval();
		stack.pop();
		join.start(ctx);
		fold(oval,ival);
		join.pop();
	    }
	} else {
	    errors.error(Errors.EXEC,tree.pos,"wrong number of actual parameters");
	    stack.pushError();
	}
    }

    public void execScat(XDOMCall tree,Ctx ctx) {
	execCat(tree,ctx);
    }

    void execScomp(Object oval1,long ival1,XPos pos1,
		   Object oval2,long ival2,XPos pos2,
		   boolean lss, boolean eq, boolean gt) {
	boolean ok = true;
	stack.push(oval1,ival1);
	if (evalue.getS(pos1)) {
	    oval1 = stack.getTopOval();
	    ival1 = stack.getTopIval();
	} else {
	    ok = false;
	}
	stack.pop();

	stack.push(oval2,ival2);
	if (evalue.getS(pos2)) {
	    oval2 = stack.getTopOval();
	    ival2 = stack.getTopIval();
	} else {
	    ok = false;
	}
	stack.pop();

	if (ok) {
	    int i = Value.stringComp(oval1,ival1,oval2,ival2);
	    boolean result = (lss && i <  0) || (eq && i == 0) || (gt && i > 0);
	    stack.push(value.vlBool,VLBool.toI(result));
	} else {
	    stack.pushError();
	}
    }

    public void execScomp(XDOMCall tree,boolean lss,boolean eq,boolean gt) {
	XDOM arg1 = tree.getArg(1);
	exec.execExp(arg1,value.ctxEval);
	Object oval1 = stack.getTopOval();
	long ival1 = stack.getTopIval();
	stack.pop();

	XDOM arg2 = tree.getArg(2);
	exec.execExp(arg1,value.ctxEval);
	Object oval2 = stack.getTopOval();
	long ival2 = stack.getTopIval();
	stack.pop();

	execScomp(oval1,ival1,arg1.pos,oval2,ival2,arg2.pos,lss,eq,gt);
    }


    public void execSFcomp(XDOMCall tree,boolean lss,boolean eq,boolean gt) {
	Object oval1 = null;
	Object oval2 = null;
	long ival1 = 0;
	long ival2 = 0;
	boolean ok = true;

	if (evalue.getS(tree.getArg(1))) {
	    oval1 = stack.getTopOval();
	    ival1 = stack.getTopIval();
	} else {
	    ok = false;
	}
	stack.pop();
	if (evalue.getS(tree.getArg(2))) {
	    oval2 = stack.getTopOval();
	    ival2 = stack.getTopIval();
	} else {
	    ok = false;
	}
	stack.pop();
	if (ok) {
	    int i = Value.stringFComp(oval1,ival1,oval2,ival2);
	    boolean result = (lss && i <  0) || (eq && i == 0) || (gt && i > 0);
	    stack.push(value.vlBool,VLBool.toI(result));
	} else {
	    stack.pushError();
	}
    }

    private boolean isRange(XDOM x) {
	if (x.getXKind() == XDOM.XCALL) {
	    XDOMCall xc = (XDOMCall) x;
	    XDOM func = xc.getFunc();
	    if (func.getXKind() == XDOM.XNAME && xc.argSize() == 2) {
		XDOMName xn = (XDOMName) func;
		if (xn.getName() == "Range") return true;
	    }
	}
	return false;
    }

    private void substr(Object oval,int low,int high) {
	if (low == 1 && high == Value.stringSize(oval)) {
	    stack.push(oval,0);
	    join.add(null);
	} else if (oval instanceof String) {
	    String s = (String) oval;
	    stack.push(s.substring(low-1,high).intern(),0);
	    join.add(null);
	} else {
	    int low1 = low;
	    int high1 = high;
	    VLCat vlc = (VLCat) oval;
	    int size = vlc.ovals.length;
	    for (int i = 0; i < size; i++) {
		Object oval1 = vlc.ovals[i];
		int chars = Value.stringSize(oval1);
		if (low1 <= chars) {
		    int high2 = high1;
		    if (high2 > chars) high2 = chars;
		    substr(oval1,low1,high2);
		    low1 = high2 + 1;
		}
		low1 = low1 - chars;
		high1 = high1 - chars;
		if (low1 <= 0) break;
	    }
	}
    }
    
    public void execSSubscript(Object oval,long ival,XDOMCall tree,Ctx ctx) {
	if (ctx == value.ctxAssign) {
	    errors.error(Errors.EXEC,tree.pos,"can't assign to this expression");
	    return;
	}
	stack.push(oval,ival);
	evalue.getS(tree.pos);
	Object ovals = stack.getTopOval();
	long ivals = stack.getTopIval();
	stack.pop();
	int chars = Value.stringSize(ovals);
	boolean ok = false;

	XDOM arg1 = tree.getArg(1);
	int low = 0;
	int high = -1;
	if (isRange(arg1)) {
	    XDOMCall xc = (XDOMCall) arg1;
	    XDOM v1 = xc.getArg(1);
	    XDOM v2 = xc.getArg(2);
	    ok = true;
	    if (evalue.getI(v1)) {
		low = (int) stack.getTopIval();
	    } else {
		ok = false;
	    }
	    stack.pop();
	    if (evalue.getI(v2)) {
		high = (int) stack.getTopIval();
	    } else {
		ok = false;
	    }
	    stack.pop();
	    if (ok) {
		if (low == high+1 || 1 <= low && low <= high && high <= chars) {
		} else {
		    errors.error(Errors.EXEC,arg1.pos,"string subscript "+
				 low+".."+high+" not in range(1.."+chars+")");
		    ok = false;
		}
	    }
	} else {
	    exec.execExp(arg1,value.ctxEval);
	    Object oval1 = stack.getTopOval();
	    long ival1 = stack.getTopIval();
	    if (oval1 instanceof VLExt) {
		VLExt vle = (VLExt) oval1;
		if (vle.foval == xt.cmd.biBind.rangeE) {
		    ok = true;
		    low = (int) vle.eivals[0];
		    high = (int) vle.eivals[1];
		    if (low == high+1 || 1 <= low && low <= high && high <= chars) {
			ok = true;
		    } else {
			errors.error(Errors.EXEC,arg1.pos,"string subscript "+
				     low+".."+high+" not in range(1.."+chars+")");
		    }
		} else {
		    errors.error(Errors.EXEC,arg1.pos,"illegal string subscript");
		}
	    } else if (evalue.getI(arg1.pos)) {
		low = (int) stack.getTopIval();
		if (1 <= low && low <= chars) {
		    ok = true;
		} else {
		    errors.error(Errors.EXEC,arg1.pos,"string subscript "+low+" not in range(1.."+chars+")");
		}
	    }
	    stack.pop();
	}

	if (ok) {
	    if (high == -1) {
		stack.push(value.vlInt,Value.getChar(ovals,ivals,low-1));
	    } else {
		if (low == high+1) {
		    stack.push("",0);
		} else if (low == high) {
		    stack.push(value.vlChar,Value.getChar(ovals,ivals,low-1));
		} else {
		    join.start(ctx);
		    substr(ovals,low,high);
		    join.pop();
		}
	    }
	} else {
	    stack.pushError();
	}
    }

    /*************************************************************************
    *
    *   date ops
    *
    *************************************************************************/

    private TimeZone DTtz;
    private boolean DThastz;
    private boolean[] DThas = new boolean[7];
    private int[] DTval = new int[7];
    private String[] DTname = {
	"year", "month", "day", "hour", "minute", "second", "millisecond",
	"dayOfWeek", "dayOfYear" };

    private int[] DTfield = {
	Calendar.YEAR,
	Calendar.MONTH,
	Calendar.DAY_OF_MONTH,
	Calendar.HOUR_OF_DAY,
	Calendar.MINUTE,
	Calendar.SECOND,
	Calendar.MILLISECOND,
	Calendar.DAY_OF_WEEK,
	Calendar.DAY_OF_YEAR };

    private int[] DToff = {
	0, 1, 0, 0, 0, 0, 0,
	0, 0
    };
    private int[] DTdefault = {
	2000, 0, 1, 0, 0, 0, 0,
    };

    private void DTfill(XDOMCall tree,int first,boolean allowTZ) {
	int size = DThas.length;
	for (int i = 0; i < size; i++) {
	    DThas[i] = false;
	}
	DThastz = false;
	int args = tree.argSize();
	for (int i = first; i <= args; i++) {
	    XDOM arg = tree.getArg(i);
	    if (TreeUtil.isEqual(arg)) {
		String name = TreeUtil.getEqualName(arg);
		XDOM x = TreeUtil.stripEqual(arg);
		boolean found = false;
		if (allowTZ && name == "timeZone") {
		    if (DThastz) {
			errors.error(Errors.EXEC,arg.pos,"duplicate date field name");
		    } else {
			if (evalue.getS(x)) {
			    Object oval = stack.getTopOval();
			    long ival = stack.getTopIval();
			    stack.pop();
			    String tzs = XDOMValue.getString(oval,ival);
			    if (tzs == "GMT") {
				DTtz = VLDate.gmtTZ;
				DThastz = true;
			    } else {
				DTtz = TimeZone.getTimeZone(tzs);
				if (DTtz.getDisplayName(false,TimeZone.SHORT).equals("GMT")) {
				    errors.error(Errors.EXEC,arg.pos,"unrecognized time zone");
				} else {
				    DThastz = true;
				}
			    }
			}
		    }
		} else {
		    for (int j = 0; j < size; j++) {
			if (name == DTname[j]) {
			    found = true;
			    if (DThas[j]) {
				errors.error(Errors.EXEC,arg.pos,"duplicate date field name");
			    } else {
				if (evalue.getI(x)) {
				    long ival = stack.getTopIval();
				    DThas[j] = true;
				    DTval[j] = (int) ival;
				    stack.pop();
				    break;
				}
				stack.pop();
			    }
			    break;
			}
		    }
		    if (! found) {
			errors.error(Errors.EXEC,arg.pos,"unrecognized date field name");
		    }
		}
	    } else {
		errors.error(Errors.EXEC,arg.pos,"missing name=");
	    }
	}
    }

    public void makeDate(XDOMCall tree,Ctx ctx) {
	int args = tree.argSize();
	int first = 1;
	if (args >= 1) {
	    XDOM arg1 = tree.getArg(1);
	    if (! TreeUtil.isEqual(arg1)) {
		if (evalue.getDate(arg1)) {
		    VLDate vld = (VLDate) stack.getTopOval();
		    long ival = stack.getTopIval();
		    if (args == 1) return;
		    stack.pop();
		    xt.cal.clear();
		    xt.cal.setTimeZone(vld.tz);
		    xt.date.setTime(ival);
		    xt.cal.setTime(xt.date);
		    first = 2;
		} else {
		    return;
		}
	    }
	} else {
	    xt.cal.clear();
	}
	DTfill(tree,first,true);
	if (DThastz) {
	    xt.cal.setTimeZone(DTtz);
	} else if (first == 1) {
	    xt.cal.setTimeZone(VLDate.localTZ);
	}
	int size = DThas.length;
	for (int i = 0; i < size; i++) {
	    if (DThas[i]) {
		xt.cal.set(DTfield[i],DTval[i]-DToff[i]);
	    } else if (first == 1) {
		if (i == 0) {
		    errors.error(Errors.EXEC,tree.pos,"year not specified");
		    stack.pushError();
		    return;
		}
		xt.cal.set(DTfield[i],DTdefault[i]);
	    }
	}
	VLDate vld = value.makeVLDate(xt.cal.getTimeZone());
	stack.push(vld,xt.cal.getTime().getTime());
    }

    public void execDTeq(XDOMCall tree) {
	long i1 = 0;
	long i2 = 0;
	boolean ok = true;

	if (evalue.getDate(tree.getArg(1))) {
	    i1 = stack.getTopIval();
	} else {
	    ok = false;
	}
	stack.pop();
	if (evalue.getDate(tree.getArg(2))) {
	    i2 = stack.getTopIval();
	} else {
	    ok = false;
	}
	stack.pop();
	if (ok) {
	    stack.push(value.vlBool,VLBool.toI(i1 == i2));
	} else {
	    stack.pushError();
	}
    }

    public void execDTne(XDOMCall tree) {
	long i1 = 0;
	long i2 = 0;
	boolean ok = true;

	if (evalue.getDate(tree.getArg(1))) {
	    i1 = stack.getTopIval();
	} else {
	    ok = false;
	}
	stack.pop();
	if (evalue.getDate(tree.getArg(2))) {
	    i2 = stack.getTopIval();
	} else {
	    ok = false;
	}
	stack.pop();
	if (ok) {
	    stack.push(value.vlBool,VLBool.toI(i1 != i2));
	} else {
	    stack.pushError();
	}
    }

    public void execDTls(XDOMCall tree) {
	long i1 = 0;
	long i2 = 0;
	boolean ok = true;

	if (evalue.getDate(tree.getArg(1))) {
	    i1 = stack.getTopIval();
	} else {
	    ok = false;
	}
	stack.pop();
	if (evalue.getDate(tree.getArg(2))) {
	    i2 = stack.getTopIval();
	} else {
	    ok = false;
	}
	stack.pop();
	if (ok) {
	    stack.push(value.vlBool,VLBool.toI(i1 < i2));
	} else {
	    stack.pushError();
	}
    }

    public void execDTle(XDOMCall tree) {
	long i1 = 0;
	long i2 = 0;
	boolean ok = true;

	if (evalue.getDate(tree.getArg(1))) {
	    i1 = stack.getTopIval();
	} else {
	    ok = false;
	}
	stack.pop();
	if (evalue.getDate(tree.getArg(2))) {
	    i2 = stack.getTopIval();
	} else {
	    ok = false;
	}
	stack.pop();
	if (ok) {
	    stack.push(value.vlBool,VLBool.toI(i1 <= i2));
	} else {
	    stack.pushError();
	}
    }

    public void execDTgt(XDOMCall tree) {
	long i1 = 0;
	long i2 = 0;
	boolean ok = true;

	if (evalue.getDate(tree.getArg(1))) {
	    i1 = stack.getTopIval();
	} else {
	    ok = false;
	}
	stack.pop();
	if (evalue.getDate(tree.getArg(2))) {
	    i2 = stack.getTopIval();
	} else {
	    ok = false;
	}
	stack.pop();
	if (ok) {
	    stack.push(value.vlBool,VLBool.toI(i1 > i2));
	} else {
	    stack.pushError();
	}
    }

    public void execDTge(XDOMCall tree) {
	long i1 = 0;
	long i2 = 0;
	boolean ok = true;

	if (evalue.getDate(tree.getArg(1))) {
	    i1 = stack.getTopIval();
	} else {
	    ok = false;
	}
	stack.pop();
	if (evalue.getDate(tree.getArg(2))) {
	    i2 = stack.getTopIval();
	} else {
	    ok = false;
	}
	stack.pop();
	if (ok) {
	    stack.push(value.vlBool,VLBool.toI(i1 >= i2));
	} else {
	    stack.pushError();
	}
    }

    public void execDTsub(XDOMCall tree) {
	long i1 = 0;
	long i2 = 0;
	boolean ok = true;

	if (evalue.getDate(tree.getArg(1))) {
	    i1 = stack.getTopIval();
	} else {
	    ok = false;
	}
	stack.pop();
	if (evalue.getDate(tree.getArg(2))) {
	    i2 = stack.getTopIval();
	} else {
	    ok = false;
	}
	stack.pop();
	if (ok) {
	    stack.push(value.vlInt,i1 - i2);
	} else {
	    stack.pushError();
	}
    }

    public void execDTnow(XDOMCall tree) {
	stack.push(value.localDate,
		   System.currentTimeMillis());
    }

    public void execDTgmt(XDOMCall tree) {
	stack.push(value.gmtDate,
		   System.currentTimeMillis());
    }

    public void DTadd(Object oval,long ival,XDOMCall tree) {
	stack.push(oval,ival);
	if (evalue.getDate(tree.pos)) {
	    VLDate vld = (VLDate) stack.getTopOval();
	    long millis=stack.getTopIval();
	    stack.pop();
	    xt.date.setTime(millis);
	    xt.cal.setTime(xt.date);
	    xt.cal.setTimeZone(vld.tz);
	    DTfill(tree,1,false);
	    int size = DThas.length;
	    for (int i = 0; i < size; i++) {
		if (DThas[i]) {
		    xt.cal.add(DTfield[i],DTval[i]);
		}
	    }
	    stack.push(vld,xt.cal.getTime().getTime());
	}
    }

    public boolean DTfield(XDOM tree,String name,XPos pos,Ctx ctx) {
	if (evalue.getDate(tree)) {
	    VLDate vld = (VLDate) stack.getTopOval();
	    long ival = stack.getTopIval();
	    stack.pop();
	    xt.cal.setTimeZone(VLDate.gmtTZ);
	    xt.date.setTime(ival);
	    xt.cal.setTime(xt.date);
	    xt.cal.setTimeZone(vld.tz);
	    if (name == "timeZone") {
		stack.push(vld.tz.getDisplayName(
		    vld.tz.inDaylightTime(xt.cal.getTime()),
		    TimeZone.SHORT),0);
		return true;
	    } else if (name == "longTimeZone") {
		stack.push(vld.tz.getDisplayName(
		    vld.tz.inDaylightTime(xt.cal.getTime()),
		    TimeZone.LONG),0);
		return true;
	    } else if (name == "dst") {
		boolean dst = vld.tz.inDaylightTime(xt.cal.getTime());
		stack.push(value.vlBool,VLBool.toI(dst));
		return true;
	    }
	    int size = DTname.length;
	    for (int i = 0; i < size; i++) {
		if (name == DTname[i]) {
		    stack.push(value.vlInt,xt.cal.get(DTfield[i])+DToff[i]);
		    return true;
		}
	    }
	}
	stack.pop();
	return false;
    }
    
    /*************************************************************************
    *
    *   int ops
    *
    *************************************************************************/

    public void makeInt(XDOMCall tree) {
	int args = tree.argSize();
	if (args == 1) {
	    XDOM arg1 = tree.getArg(1);
	    exec.execExp(arg1,value.ctxEval);
	    Object oval = stack.getTopOval();
	    long ival = stack.getTopIval();
	    stack.pop();
	    if (oval instanceof VLInt) {
		stack.push(value.vlInt,ival);
	    } else if (oval instanceof VLFloat) {
		double f = Double.longBitsToDouble(ival);
		stack.push(value.vlInt,(long) f);
	    } else if (XDOMValue.isString(oval,ival)) {
		String s = XDOMValue.getString(oval,ival);
		if (XDOMInt.isInt(s)) {
		    long i = XDOMInt.toInt(s);
		    stack.push(value.vlInt,i);
		} else if (XDOMFloat.isFloat(s)) {
		    double f = XDOMFloat.toFloat(s);
		    stack.push(value.vlInt,(long) f);
		} else {
		    errors.error(Errors.EXEC,arg1.pos,"string value is not an int or float");
		    stack.pushError();
		}
	    } else {
		errors.error(Errors.EXEC,arg1.pos,"not an int, float or string");
	    }
	} else {
	    errors.error(Errors.EXEC,tree.pos,"wrong number of actual parameters");
	    stack.pushError();
	}
    }

    void execIadd(long ival1,long ival2) {
	stack.push(value.vlInt,ival1+ival2);
    }

    public void execIadd(XDOMCall tree) {
	long i1 = 0;
	long i2 = 0;
	boolean ok = true;

	if (evalue.getI(tree.getArg(1))) {
	    i1 = stack.getTopIval();
	} else {
	    ok = false;
	}
	stack.pop();
	if (evalue.getI(tree.getArg(2))) {
	    i2 = stack.getTopIval();
	} else {
	    ok = false;
	}
	stack.pop();
	if (ok) {
	    execIadd(i1,i2);
	} else {
	    stack.pushError();
	}
    }

    void execIsub(long ival1,long ival2) {
	stack.push(value.vlInt,ival1-ival2);
    }

    public void execIsub(XDOMCall tree) {
	long i1 = 0;
	long i2 = 0;
	boolean ok = true;

	if (evalue.getI(tree.getArg(1))) {
	    i1 = stack.getTopIval();
	} else {
	    ok = false;
	}
	stack.pop();
	if (evalue.getI(tree.getArg(2))) {
	    i2 = stack.getTopIval();
	} else {
	    ok = false;
	}
	stack.pop();
	if (ok) {
	    execIsub(i1,i2);
	} else {
	    stack.pushError();
	}
    }

    void execIminus(long ival1) {
	stack.push(value.vlInt,-ival1);
    }

    public void execIminus(XDOMCall tree) {
	long i1 = 0;
	boolean ok = true;

	if (evalue.getI(tree.getArg(1))) {
	    i1 = stack.getTopIval();
	} else {
	    ok = false;
	}
	stack.pop();
	if (ok) {
	    execIminus(i1);
	} else {
	    stack.pushError();
	}
    }

    void execImult(long ival1,long ival2) {
	stack.push(value.vlInt,ival1*ival2);
    }

    public void execImult(XDOMCall tree) {
	long i1 = 0;
	long i2 = 0;
	boolean ok = true;

	if (evalue.getI(tree.getArg(1))) {
	    i1 = stack.getTopIval();
	} else {
	    ok = false;
	}
	stack.pop();
	if (evalue.getI(tree.getArg(2))) {
	    i2 = stack.getTopIval();
	} else {
	    ok = false;
	}
	stack.pop();
	if (ok) {
	    execImult(i1,i2);
	} else {
	    stack.pushError();
	}
    }

    void execIdiv(long ival1,long ival2,XPos pos) {
	if (ival2 == 0) {
	    errors.error(Errors.EXEC,pos,"divide by 0");
	    stack.pushError();
	} else {
	    stack.push(value.vlInt,ival1/ival2);
	}
    }

    public void execIdiv(XDOMCall tree) {
	long i1 = 0;
	long i2 = 0;
	boolean ok = true;

	XDOM arg1 = tree.getArg(1);
	if (evalue.getI(arg1)) {
	    i1 = stack.getTopIval();
	} else {
	    ok = false;
	}
	stack.pop();
	XDOM arg2 = tree.getArg(2);
	if (evalue.getI(arg2)) {
	    i2 = stack.getTopIval();
	} else {
	    ok = false;
	}
	stack.pop();
	if (ok) {
	    execIdiv(i1,i2,tree.pos);
	} else {
	    stack.pushError();
	}
    }

    void execIrem(long ival1,long ival2,XPos pos) {
	if (ival2 == 0) {
	    errors.error(Errors.EXEC,pos,"divide by 0");
	    stack.pushError();
	} else {
	    stack.push(value.vlInt,ival1%ival2);
	}
    }

    public void execIrem(XDOMCall tree) {
	long i1 = 0;
	long i2 = 0;
	boolean ok = true;

	if (evalue.getI(tree.getArg(1))) {
	    i1 = stack.getTopIval();
	} else {
	    ok = false;
	}
	stack.pop();
	if (evalue.getI(tree.getArg(2))) {
	    i2 = stack.getTopIval();
	} else {
	    ok = false;
	}
	stack.pop();
	if (ok) {
	    execIrem(i1,i2,tree.pos);
	} else {
	    stack.pushError();
	}
    }

    void execIeq(long ival1,long ival2) {
	stack.push(value.vlBool,VLBool.toI(ival1 == ival2));
    }

    public void execIeq(XDOMCall tree) {
	long i1 = 0;
	long i2 = 0;
	boolean ok = true;

	if (evalue.getI(tree.getArg(1))) {
	    i1 = stack.getTopIval();
	} else {
	    ok = false;
	}
	stack.pop();
	if (evalue.getI(tree.getArg(2))) {
	    i2 = stack.getTopIval();
	} else {
	    ok = false;
	}
	stack.pop();
	if (ok) {
	    execIeq(i1,i2);
	} else {
	    stack.pushError();
	}
    }

    void execIne(long ival1,long ival2) {
	stack.push(value.vlBool,VLBool.toI(ival1 != ival2));
    }

    public void execIne(XDOMCall tree) {
	long i1 = 0;
	long i2 = 0;
	boolean ok = true;

	if (evalue.getI(tree.getArg(1))) {
	    i1 = stack.getTopIval();
	} else {
	    ok = false;
	}
	stack.pop();
	if (evalue.getI(tree.getArg(2))) {
	    i2 = stack.getTopIval();
	} else {
	    ok = false;
	}
	stack.pop();
	if (ok) {
	    execIne(i1,i2);
	} else {
	    stack.pushError();
	}
    }

    void execIls(long ival1,long ival2) {
	stack.push(value.vlBool,VLBool.toI(ival1 < ival2));
    }

    public void execIls(XDOMCall tree) {
	long i1 = 0;
	long i2 = 0;
	boolean ok = true;
	
	if (evalue.getI(tree.getArg(1))) {
	    i1 = stack.getTopIval();
	} else {
	    ok = false;
	}
	stack.pop();
	if (evalue.getI(tree.getArg(2))) {
	    i2 = stack.getTopIval();
	} else {
	    ok = false;
	}
	stack.pop();
	if (ok) {
	    execIls(i1,i2);
	} else {
	    stack.pushError();
	}
    }

    void execIle(long ival1,long ival2) {
	stack.push(value.vlBool,VLBool.toI(ival1 <= ival2));
    }

    public void execIle(XDOMCall tree) {
	long i1 = 0;
	long i2 = 0;
	boolean ok = true;

	if (evalue.getI(tree.getArg(1))) {
	    i1 = stack.getTopIval();
	} else {
	    ok = false;
	}
	stack.pop();
	if (evalue.getI(tree.getArg(2))) {
	    i2 = stack.getTopIval();
	} else {
	    ok = false;
	}
	stack.pop();
	if (ok) {
	    execIle(i1,i2);
	} else {
	    stack.pushError();
	}
    }

    void execIgt(long ival1,long ival2) {
	stack.push(value.vlBool,VLBool.toI(ival1 > ival2));
    }

    public void execIgt(XDOMCall tree) {
	long i1 = 0;
	long i2 = 0;
	boolean ok = true;

	if (evalue.getI(tree.getArg(1))) {
	    i1 = stack.getTopIval();
	} else {
	    ok = false;
	}
	stack.pop();
	if (evalue.getI(tree.getArg(2))) {
	    i2 = stack.getTopIval();
	} else {
	    ok = false;
	}
	stack.pop();
	if (ok) {
	    execIgt(i1,i2);
	} else {
	    stack.pushError();
	}
    }

    void execIge(long ival1,long ival2) {
	stack.push(value.vlBool,VLBool.toI(ival1 >= ival2));
    }

    public void execIge(XDOMCall tree) {
	long i1 = 0;
	long i2 = 0;
	boolean ok = true;

	if (evalue.getI(tree.getArg(1))) {
	    i1 = stack.getTopIval();
	} else {
	    ok = false;
	}
	stack.pop();
	if (evalue.getI(tree.getArg(2))) {
	    i2 = stack.getTopIval();
	} else {
	    ok = false;
	}
	stack.pop();
	if (ok) {
	    execIge(i1,i2);
	} else {
	    stack.pushError();
	}
    }

    /*************************************************************************
    *
    *   float ops
    *
    *************************************************************************/

    public void makeFloat(XDOMCall tree) {
	int args = tree.argSize();
	if (args == 1) {
	    XDOM arg1 = tree.getArg(1);
	    exec.execExp(arg1,value.ctxEval);
	    Object oval = stack.getTopOval();
	    long ival = stack.getTopIval();
	    stack.pop();
	    if (oval instanceof VLInt) {
		double f = (double) ival;
		stack.push(value.vlFloat,Double.doubleToLongBits(f));
	    } else if (oval instanceof VLFloat) {
		stack.push(value.vlFloat,ival);
	    } else if (XDOMValue.isString(oval,ival)) {
		String s = XDOMValue.getString(oval,ival);
		if (XDOMInt.isInt(s)) {
		    long i = XDOMInt.toInt(s);
		    double f = (double) i;
		    stack.push(value.vlFloat,Double.doubleToLongBits(f));
		} else if (XDOMFloat.isFloat(s)) {
		    double f = XDOMFloat.toFloat(s);
		    stack.push(value.vlFloat,Double.doubleToLongBits(f));
		} else {
		    errors.error(Errors.EXEC,arg1.pos,"string value is not an int or float");
		    stack.pushError();
		}
	    } else {
		errors.error(Errors.EXEC,arg1.pos,"not an int, float or string");
	    }
	} else {
	    errors.error(Errors.EXEC,tree.pos,"wrong number of actual parameters");
	    stack.pushError();
	}
//	int args = tree.argSize();
//	if (args == 1) {
//	    XDOM arg1 = tree.getArg(1);
//	    evalue.getF(arg1);
//	} else {
//	    errors.error(Errors.EXEC,tree.pos,"wrong number of actual parameters");
//	    stack.pushError();
//	}
    }

    void execFadd(Object oval1,long ival1,XPos pos1,Object oval2,long ival2,XPos pos2) {
	stack.push(oval1,ival1);
	evalue.getF(pos1);
	Object oval1f = stack.getTopOval();
	long ival1f = stack.getTopIval();
	stack.pop();
	stack.push(oval2,ival2);
	evalue.getF(pos2);
	Object oval2f = stack.getTopOval();
	long ival2f = stack.getTopIval();
	stack.pop();
	if (value.isFloat(oval1f,ival1f) && value.isFloat(oval2f,ival2f)) {
	    double f1 = Double.longBitsToDouble(ival1f);
	    double f2 = Double.longBitsToDouble(ival2f);
	    stack.push(value.vlFloat,Double.doubleToLongBits(f1+f2));
	} else {
	    stack.pushError();
	}
    }

    public void execFadd(XDOMCall tree) {
	XDOM arg1 = tree.getArg(1);
	exec.execExp(arg1,value.ctxEval);
	Object oval1 = stack.getTopOval();
	long ival1 = stack.getTopIval();
	stack.pop();
	XDOM arg2 = tree.getArg(2);
	exec.execExp(arg2,value.ctxEval);
	Object oval2 = stack.getTopOval();
	long ival2 = stack.getTopIval();
	stack.pop();
	execFadd(oval1,ival1,arg1.pos,oval2,ival2,arg2.pos);
    }

    void execFsub(Object oval1,long ival1,XPos pos1,Object oval2,long ival2,XPos pos2) {
	stack.push(oval1,ival1);
	evalue.getF(pos1);
	Object oval1f = stack.getTopOval();
	long ival1f = stack.getTopIval();
	stack.pop();
	stack.push(oval2,ival2);
	evalue.getF(pos2);
	Object oval2f = stack.getTopOval();
	long ival2f = stack.getTopIval();
	stack.pop();
	if (value.isFloat(oval1f,ival1f) && value.isFloat(oval2f,ival2f)) {
	    double f1 = Double.longBitsToDouble(ival1f);
	    double f2 = Double.longBitsToDouble(ival2f);
	    stack.push(value.vlFloat,Double.doubleToLongBits(f1-f2));
	} else {
	    stack.pushError();
	}
    }

    public void execFsub(XDOMCall tree) {
	XDOM arg1 = tree.getArg(1);
	exec.execExp(arg1,value.ctxEval);
	Object oval1 = stack.getTopOval();
	long ival1 = stack.getTopIval();
	stack.pop();
	XDOM arg2 = tree.getArg(2);
	exec.execExp(arg2,value.ctxEval);
	Object oval2 = stack.getTopOval();
	long ival2 = stack.getTopIval();
	stack.pop();
	execFadd(oval1,ival1,arg1.pos,oval2,ival2,arg2.pos);
    }

    void execFminus(Object oval1,long ival1,XPos pos1) {
	stack.push(oval1,ival1);
	evalue.getF(pos1);
	Object oval1f = stack.getTopOval();
	long ival1f = stack.getTopIval();
	stack.pop();
	if (value.isFloat(oval1f,ival1f)) {
	    double f1 = Double.longBitsToDouble(ival1f);
	    stack.push(value.vlFloat,Double.doubleToLongBits(-f1));
	} else {
	    stack.pushError();
	}
    }

    public void execFminus(XDOMCall tree) {
	XDOM arg1 = tree.getArg(1);
	exec.execExp(arg1,value.ctxEval);
	Object oval1 = stack.getTopOval();
	long ival1 = stack.getTopIval();
	stack.pop();
	execFminus(oval1,ival1,arg1.pos);
    }


    void execFmult(Object oval1,long ival1,XPos pos1,Object oval2,long ival2,XPos pos2) {
	stack.push(oval1,ival1);
	evalue.getF(pos1);
	Object oval1f = stack.getTopOval();
	long ival1f = stack.getTopIval();
	stack.pop();
	stack.push(oval2,ival2);
	evalue.getF(pos2);
	Object oval2f = stack.getTopOval();
	long ival2f = stack.getTopIval();
	stack.pop();
	if (value.isFloat(oval1f,ival1f) && value.isFloat(oval2f,ival2f)) {
	    double f1 = Double.longBitsToDouble(ival1f);
	    double f2 = Double.longBitsToDouble(ival2f);
	    stack.push(value.vlFloat,Double.doubleToLongBits(f1*f2));
	} else {
	    stack.pushError();
	}
    }

    public void execFmult(XDOMCall tree) {
	XDOM arg1 = tree.getArg(1);
	exec.execExp(arg1,value.ctxEval);
	Object oval1 = stack.getTopOval();
	long ival1 = stack.getTopIval();
	stack.pop();
	XDOM arg2 = tree.getArg(2);
	exec.execExp(arg2,value.ctxEval);
	Object oval2 = stack.getTopOval();
	long ival2 = stack.getTopIval();
	stack.pop();
	execFmult(oval1,ival1,arg1.pos,oval2,ival2,arg2.pos);
    }


    void execFdiv(Object oval1,long ival1,XPos pos1,Object oval2,long ival2,XPos pos2,XPos pos) {
	stack.push(oval1,ival1);
	evalue.getF(pos1);
	Object oval1f = stack.getTopOval();
	long ival1f = stack.getTopIval();
	stack.pop();
	stack.push(oval2,ival2);
	evalue.getF(pos2);
	Object oval2f = stack.getTopOval();
	long ival2f = stack.getTopIval();
	stack.pop();
	if (value.isFloat(oval1f,ival1f) && value.isFloat(oval2f,ival2f)) {
	    double f1 = Double.longBitsToDouble(ival1f);
	    double f2 = Double.longBitsToDouble(ival2f);
	    if (f2 == 0.0) {
		errors.error(Errors.EXEC,pos,"divide by 0.0");
		stack.pushError();
	    } else {
		stack.push(value.vlFloat,Double.doubleToLongBits(f1/f2));
	    }
	} else {
	    stack.pushError();
	}
    }

    public void execFdiv(XDOMCall tree) {
	XDOM arg1 = tree.getArg(1);
	exec.execExp(arg1,value.ctxEval);
	Object oval1 = stack.getTopOval();
	long ival1 = stack.getTopIval();
	stack.pop();
	XDOM arg2 = tree.getArg(2);
	exec.execExp(arg2,value.ctxEval);
	Object oval2 = stack.getTopOval();
	long ival2 = stack.getTopIval();
	stack.pop();
	execFdiv(oval1,ival1,arg1.pos,oval2,ival2,arg2.pos,tree.pos);
    }

    public void execFeq(Object oval1,long ival1,XPos pos1,Object oval2,long ival2,XPos pos2) {
	double f1 = 0.0;
	double f2 = 0.0;
	boolean ok = true;
	stack.push(oval1,ival1);
	if (evalue.getF(pos1)) {
	    f1 = Double.longBitsToDouble(stack.getTopIval());
	} else {
	    ok = false;
	}
	stack.pop();
	stack.push(oval2,ival2);
	if (evalue.getF(pos2)) {
	    f2 = Double.longBitsToDouble(stack.getTopIval());
	} else {
	    ok = false;
	}
	stack.pop();
	if (ok) {
	    stack.push(value.vlBool,VLBool.toI(f1 == f2));
	} else {
	    stack.pushError();
	}
    }

    void execFcomp(Object oval1,long ival1,XPos pos1,Object oval2,long ival2,XPos pos2,
			 boolean less,boolean eq,boolean greater) {
	double f1 = 0.0;
	double f2 = 0.0;
	boolean ok = true;
	stack.push(oval1,ival1);
	if (evalue.getF(pos1)) {
	    f1 = Double.longBitsToDouble(stack.getTopIval());
	} else {
	    ok = false;
	}
	stack.pop();
	stack.push(oval2,ival2);
	if (evalue.getF(pos2)) {
	    f2 = Double.longBitsToDouble(stack.getTopIval());
	} else {
	    ok = false;
	}
	stack.pop();
	if (ok) {
	    boolean result = false;
	    if (less && f1 < f2) result = true;
	    if (eq && f1 == f2) result = true;
	    if (greater && f1 > f2) result = true;
	    stack.push(value.vlBool,VLBool.toI(result));
	} else {
	    stack.pushError();
	}
    }

    public void execFcomp(XDOMCall tree,boolean less,boolean eq,boolean greater) {
	XDOM arg1 = tree.getArg(1);
	exec.execExp(arg1,value.ctxEval);
	Object oval1 = stack.getTopOval();
	long ival1 = stack.getTopIval();
	stack.pop();
	XDOM arg2 = tree.getArg(2);
	exec.execExp(arg2,value.ctxEval);
	Object oval2 = stack.getTopOval();
	long ival2 = stack.getTopIval();
	stack.pop();
	execFcomp(oval1,ival1,arg1.pos,oval2,ival2,arg2.pos,less,eq,greater);
	
    }

    /*************************************************************************
    *
    *   boolean ops
    *
    *************************************************************************/

    public void execBor(XDOMCall tree) {
	execOr(tree,value.ctxEval);
    }

    public void execBxor(XDOMCall tree) {
	int i;
	int size = tree.argSize();
	long val = 0;
	boolean ok = true;

	for (i = 1; i <= size; i++) {
	    if (evalue.getB(tree.getArg(i))) {
		long ival = stack.getTopIval();
		if (ival == 1) {
		    if (val == 0) {
			val = 1;
		    } else {
			val = 0;
		    }
		}
	    } else {
		ok = false;
	    }
	    stack.pop();
	}
	if (ok) {
	    stack.push(value.vlBool,val);
	} else {
	    stack.pushError();
	}
    }

    public void execBand(XDOMCall tree) {
	execAnd(tree,value.ctxEval);
    }

    public void execBnot(XDOMCall tree) {
	boolean ok = true;
	long val = 0;

	if (evalue.getB(tree.getArg(1))) {
	    if (stack.getTopIval() == 0) {
		val = 1;
	    }
	} else {
	    ok = false;
	}
	stack.pop();
	if (ok) {
	    stack.push(value.vlBool,val);
	} else {
	    stack.pushError();
	}
    }

    /*************************************************************************
    *
    *   queue ops
    *
    *************************************************************************/

    public void makeQueue(XDOMCall tree) {
	int size = tree.argSize();
	if (size == 1) {
	    XDOM arg1 = tree.getArg(1);
	    if (evalue.getI(arg1)) {
		long ival = stack.getTopIval();
		stack.pop();
		VLQueue vlq = new VLQueue((int) ival);
		stack.push(vlq,0);
	    }
	} else {
	    errors.error(Errors.EXEC,tree.pos,"wrong number of actual parameters");
	    stack.pushError();
	}
    }

    public void execQueueSend(VLQueue vlq,XDOMCall tree) {
	XDOM arg1 = tree.getArg(1);
	exec.execExp(arg1,value.ctxEval);
	Object oval = stack.getTopOval();
	long ival = stack.getTopIval();
	stack.pop();
	if (! value.isError(oval,ival)) {
	    vlq.send(oval,ival);
	}
	stack.pushNull();
    }

    public void execQueueReceive(VLQueue vlq,XDOMCall tree) {
	vlq.receive(stack);
    }


    /*************************************************************************
    *
    *   stream ops
    *
    *************************************************************************/

    public void makeStream(XDOMCall tree) {
	int args = tree.argSize();
	if (args == 0) {
	    VLStream vls = new VLStream(1);
	    stack.push(vls,0);
	} else if (args == 1) {
	    XDOM arg1 = tree.getArg(1);
	    if (evalue.getI(arg1)) {
		long size = stack.getTopIval();
		stack.pop();
		VLStream vls = new VLStream((int) size);
		stack.push(vls,0);
	    }
	} else {
	    errors.error(Errors.EXEC,tree.pos,"wrong number of actual parameters");
	    stack.pushError();
	}
    }

    public void execStreamSend(VLStream vls,XDOMCall tree) {
	XDOM arg1 = tree.getArg(1);
	Ctx ctx = value.ctxEval;
	if (option.optimize) {
	    XW w = new XWStream(vls);
	    ctx = new CtxStream(w);
	}
	if (evalue.getS(arg1,ctx)) {
	    Object oval = stack.getTopOval();
	    long ival = stack.getTopIval();
	    int size = vls.q.length;
	    for (int i = 0; i < size; i++) {
		vls.q[i].send(oval,ival);
	    }
	}
	ctx.sendSpace();
	stack.pop();
	stack.pushNull();
    }

    public void execStreamClose(VLStream vls,XDOMCall tree) {
	int size = vls.q.length;
	for (int i = 0; i < size; i++) {
	    vls.q[i].send(null,0);
	}
	stack.pushNull();
    }

    public void execStreamReceive(VLStream vls,XDOMCall tree,Ctx ctx) {
	int args = tree.argSize();
	int i = 0;
	if (args == 0) {
	} else if (args == 1) {
	    XDOM arg1 = tree.getArg(1);
	    if (evalue.getI(arg1)) {
		long ival = stack.getTopIval();
		int size = vls.q.length;
		if (1 <= ival && ival <= size) {
		    i = (int) ival-1;
		} else {
		    errors.error(Errors.EXEC,arg1.pos,"stream index out of bounds");
		}
	    }
	    stack.pop();
	} else {
	    errors.error(Errors.EXEC,tree.pos,"wrong number of actual parameters");
	}
	join.start(ctx);
	while (true) {
	    vls.q[i].receive(stack);
	    Object oval = stack.getTopOval();
	    long ival = stack.getTopIval();
	    if (value.isNull(oval,ival)) {
		stack.pop();
		break;
	    } else {
		join.add(tree.pos);
	    }
	}
	join.pop();
    }

    /*************************************************************************
    *
    *   lock ops
    *
    *************************************************************************/

    public void makeLock(XDOMCall tree) {
	int size = tree.argSize();
	if (size == 0) {
	    VLLock vll = new VLLock();
	    stack.push(vll,0);
	} else {
	    errors.error(Errors.EXEC,tree.pos,"wrong number of actual parameters");
	    stack.pushError();
	}
    }

    public void execLockWait(VLLock vll,XDOMCall tree) {
	int size = tree.argSize();
	if (size == 0) {
	    try {
		vll.wait();
	    } catch(Exception e) {
		errors.error(Errors.EXEC,tree.pos,"wait call failed");
	    }
	} else if (size == 1) {
	    XDOM arg1 = tree.getArg(1);
	    if (evalue.getI(arg1)) {
		long millis = stack.getTopIval();
		stack.pop();
		try {
		    vll.wait(millis);
		} catch(Exception e) {
		    errors.error(Errors.EXEC,tree.pos,"wait call failed");
		}
	    } else {
		stack.pop();
	    }
	} else if (size == 2) {
	    XDOM arg1 = tree.getArg(1);
	    XDOM arg2 = tree.getArg(2);
	    long millis = 0;
	    int nanos = 0;
	    boolean ok = true;
	    if (evalue.getI(arg1)) {
		millis = stack.getTopIval();
	    } else {
		ok = false;
	    }
	    stack.pop();
	    if (evalue.getI(arg1)) {
		nanos = (int) stack.getTopIval();
	    } else {
		ok = false;
	    }
	    stack.pop();
	    if (ok) {
		try {
		    vll.wait(millis,nanos);
		} catch(Exception e) {
		    errors.error(Errors.EXEC,tree.pos,"wait call failed");
		}
	    }
	} else {
	    errors.error(Errors.EXEC,tree.pos,"wrong number of actual parameters");
	}	
	stack.pushNull();
    }

    public void execLockNotify(VLLock vll,XDOMCall tree) {
	vll.notify();
	stack.pushNull();
    }

    public void execLockNotifyAll(VLLock vll,XDOMCall tree) {
	vll.notifyAll();
	stack.pushNull();
    }

    /*************************************************************************
    *
    *   URLParseQuery URLUnparseQuery
    *
    *************************************************************************/

    public void URLParseQuery(XDOMCall tree) {
	int size = tree.argSize();
	if (size == 1) {
	    XDOM arg1 = tree.getArg(1);
	    if (evalue.getS(arg1)) {
		Object oval = stack.getTopOval();
		long ival = stack.getTopIval();
		stack.pop();
		String s = XDOMValue.getString(oval,ival);
		XDOM x = xurl.URLParseQuery(s);
		stack.push(x,0);
	    } else {
		errors.error(Errors.EXEC,arg1.pos,"not a tree");
		stack.pushError();
	    }
	} else {
	    errors.error(Errors.EXEC,tree.pos,"wrong number of actual parameters");
	    stack.pushError();
	}
    }

    public void URLUnparseQuery(XDOMCall tree) {
	int size = tree.argSize();
	if (size == 1) {
	    XDOM arg1 = tree.getArg(1);
	    exec.execExp(arg1,value.ctxEval);
	    Object oval = stack.getTopOval();
	    stack.pop();
	    if (oval instanceof XDOMCall) {
		String s = xurl.URLUnparseQuery((XDOMCall) oval);
		stack.push(s,0);
	    } else {
		errors.error(Errors.EXEC,arg1.pos,"not a xdom:call");
		stack.pushError();
	    }
	} else {
	    errors.error(Errors.EXEC,tree.pos,"wrong number of actual parameters");
	    stack.pushError();
	}
    }
}
