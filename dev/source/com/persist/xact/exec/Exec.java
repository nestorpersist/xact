/******************************************************************************
*
* Copyright (c) 2002-2007. John R. Nestor. All rights reserved.
*
* See the file XLICENSE.txt for use and redistribution information.
*
*******************************************************************************/

package com.persist.xact.exec;

import com.persist.xact.system.*;
import com.persist.xdom.*;
import com.persist.xact.value.*;
import com.persist.xact.bind.*;

public final class Exec {
    // parent 
    public XThread xt;
    // children 
    public VariableStack stack;
    public VariableStack viewStack;
    public LayerStack layerStack;
    public Frames frames;
    public Join join;
    public EValue evalue;
    public TreeExec treeExec;
    public OpExec opExec;
    public InterExec interExec;
    public BIExec biExec;
    public CallExec callExec;
    public JExec jExec;

    private Errors errors;
    private Value value;
    private XScope biscope;
    private XOption option;

    private boolean debugView = false;

    public boolean debug = false;
    private boolean debugBefore = true;
    private boolean debugAfter = false;
    private int debugLevel = -1;
    public int runLevel = 0;

    // additional Space Code
    private final static byte CANCEL = 10;
    
    public Exec(XThread xt) {
	this.xt = xt;
	callExec = new CallExec(this);
	stack = new VariableStack();
	viewStack = new VariableStack();
	layerStack = new LayerStack();
	layerStack.push(new Layer(null,InterExec.PNONE));
	frames = new Frames(this);
	evalue = new EValue(this);
	join = new Join(this);
	treeExec = new TreeExec(this);
	opExec = new OpExec(this);
	interExec = new InterExec(this);
	jExec = new JExec(this);
	biExec = new BIExec(this);
    }

    public void init() {
	value = xt.cmd.value;
	biscope = xt.cmd.biBind.biscope;
	errors = xt.errors;
	option = xt.cmd.option;
	callExec.init();
	frames.init();
	evalue.init();
	join.init();
	treeExec.init();
	opExec.init();
	interExec.init();
	jExec.init();
	biExec.init();
    }

    private void debugBeforeAct(XPos pos,Ctx ctx) {
	if (debugBefore) {
	    if (debugLevel == -1 || runLevel <= debugLevel) {
		Object oval = null;
		long ival = 0;
		boolean hasVal = false;
		if (ctx == value.ctxAssign) {
		    oval = stack.getTopOval();
		    ival = stack.getTopIval();
		    hasVal = true;
		}
		xt.cmd.dconnect.before(xt,pos,hasVal,oval,ival);
	    }
	}
    }

    private boolean debugAfterAct(XPos pos,Ctx ctx,boolean assigned) {
	boolean redo = false;
	if (debugAfter) {
	    if (debugLevel == -1 || runLevel <= debugLevel) {
		Object oval = null;
		long ival = 0;
		boolean hasVal = false;
		if (ctx != value.ctxAssign) {
		    oval = stack.getTopOval();
		    ival = stack.getTopIval();
		    hasVal = true;
		}
		if (option.flushDebugAfter) {
		    if (ctx instanceof CtxStream) {
			CtxStream cts = (CtxStream) ctx;
			try { cts.w.flush(); } catch(Exception e) {}
		    } else {
			try {xt.cmd.w.flush();} catch(Exception e) {}
		    }
		}
		redo = xt.cmd.dconnect.after(xt,pos,hasVal,oval,ival,assigned);
		if (redo && ctx != value.ctxAssign) {
		    stack.pop();
		}
	    }
	}
	return redo;
    }

    public void haltDebug() {
	debug = true;
	debugBefore = true;
	debugAfter = true;
	debugLevel = -1;
    }

    public void setDebug(boolean debug,boolean before,boolean after,int which) {
	this.debug = debug;
	debugBefore = before;
	debugAfter = after;
	if (which == 0) {
	    debugLevel = runLevel;
	} else if (which == -1) {
	    debugLevel = runLevel - 1;
	} else {
	    debugLevel = -1;
	}
    }

    public boolean checkExt(Object oval,long ival,int index,XDOMElement etree,XPos cpos) {
	boolean result = true;
	XDOMName ntree = TreeUtil.getName(etree);
	if (ntree.hasExt()) {
	    if (index < ntree.extSize()) {
		XDOM view = TreeUtil.getView(ntree.getExt(index+1));
		if (view != null) {
		    result = checkView(oval,ival,view,"name ext "+(index+1),cpos);
		}
	    }
	}
	return result;
    }

    public boolean checkArg(Object oval,long ival,int index,XDOMElement etree,XPos cpos) {
	boolean result = true;
	if (etree.attrSize() > 0) {
	    XDOM spec = TreeUtil.stripView(etree.getAttr(1));
	    if (spec instanceof XDOMCall) {
		XDOMCall ctree = (XDOMCall) spec;
		if (index < ctree.argSize()) {
		    XDOM view = TreeUtil.getView(ctree.getArg(index+1));
		    if (view != null) {
			result = checkView(oval,ival,view,"actual parameter "+(index+1),cpos);
		    }
		}
	    }
	}
	return result;
    }

    public boolean checkResult(XDOMElement etree) {
	boolean result = true;
	if (etree.attrSize() > 0) {
	    XDOM view = TreeUtil.getView(etree.getAttr(1));
	    Object oval = stack.getTopOval();
	    long ival = stack.getTopIval();
	    result = checkView(oval,ival,view,"function result",null);
	}
	return result;
    }

    public void execNameDef(XFrame f,XDefName ndef,XDOM caller,Ctx ctx) {
	XPos pos = null;
	if (caller != null) pos = caller.pos;
	if (ctx == value.ctxAssign) {
	    if (ndef.readOnly) {
		errors.error(Errors.EXEC,pos,"can't assign to this expression");
	    } else {
		boolean ok = true;
		if (ndef.hasView && option.check) {
		    Object oval = stack.getTopOval();
		    long ival = stack.getTopIval();
		    frames.getDefView(f,ndef);
		    Object voval = stack.getTopOval();
		    long vival = stack.getTopIval();
		    stack.pop();
		    int code = biExec.hasView(oval,ival,voval,vival,caller);
		    if (code != 1) {
			ok = false;
			if (code == 0) {
			    errors.error(Errors.EXEC,pos,
					 "value to be assigned not in target view");
			}
		    }
		}
		if (ok) {
		    frames.setDefVal(f,ndef);
		}
	    }
	} else {
	    frames.getDefVal(f,ndef,pos);
	}
    }       

    private void execName1(XDOMName ntree,Ctx ctx) {
	XDef def = ntree.def;
	if (def != null) {
	    def = def.getReal();
	}
	if (def == null) {
	    errors.error(Errors.EXEC,ntree.pos,"undefined name "+ntree.getName());
	    if (ctx != value.ctxAssign) {
		stack.pushError();
	    }
	} else if (def instanceof XDefVal) {
	    XDefVal vdef = (XDefVal) def;
	    stack.push(vdef.oval,vdef.ival);
	    if (callExec.execNameCall(ntree,ctx)) {
	    } else if (ctx == value.ctxAssign) {
		stack.pop();
		errors.error(Errors.EXEC,ntree.pos,"can not assign to a predefined value");
	    }
	} else if (def instanceof XDefName) {
	    XDefName ndef = (XDefName) def;
	    if (ctx == value.ctxAssign) {
		frames.getDefVal(ndef,null);
	    } else {
		frames.getDefVal(ndef,ntree.pos);
	    }
	    if (callExec.execNameCall(ntree,ctx)) {
	    } else if (ctx == value.ctxAssign) {
		boolean ok = true;
		stack.pop();
		execNameDef(null,ndef,ntree,value.ctxAssign);
	    }
	} else {
	    errors.error(Errors.INTERNAL,ntree.pos,"bad name def type");
	}
    }

    private void execName(XDOMName ntree,Ctx ctx) {
	execName1(ntree,ctx);
    }

    public boolean debugExecExp(XDOM tree,Ctx ctx) {
	if (tree.getNoStep()) return false;
	if (tree instanceof XDOMInt) {
	    return false;
	} else if (tree instanceof XDOMFloat) {
	    return false;
	} else if (tree instanceof XDOMString) {
	    return false;
	} else if (tree instanceof XDOMName) {
	    XDOMName ntree = (XDOMName) tree;
	    if (ntree.def instanceof XDefVal) return false;
	} else if (tree instanceof XDOMCall) {
	    XDOMCall ctree = (XDOMCall) tree;
	    byte kind = ctree.getKind();
	    if (kind == XDOMCall.CSHORT) return false;
	    if (kind == XDOMCall.CLONG) return false;
	}
	return true;
    }

    public void execExp(XDOM tree,Ctx ctx) {
	int oldRunLevel = runLevel;
	runLevel ++;
	while (true) {
	    if (debug) {
		runLevel = oldRunLevel + 1;
		if (debugExecExp(tree,ctx)) {
		    debugBeforeAct(tree.pos,ctx);
		}
	    }
	    int kind = tree.getXKind();

	    switch (kind) {
		case XDOM.XINT:
		{
		    if (ctx == value.ctxAssign) {
			errors.error(Errors.EXEC,tree.pos,"can not assign to an int literal");
		    } else {
			XDOMInt ntree = (XDOMInt) tree;
			stack.push(value.vlInt,ntree.getInt());
		    }
		    break;
		}
		case XDOM.XFLOAT:
		{
		    if (ctx == value.ctxAssign) {
			errors.error(Errors.EXEC,tree.pos,"can not assign to a float literal");
		    } else {
			XDOMFloat ntree = (XDOMFloat) tree;
			long num = Double.doubleToLongBits(ntree.getFloat());
			stack.push(value.vlFloat,num);
		    }
		    break;
		}
		case XDOM.XSTRING:
		{
		    if (ctx == value.ctxAssign) {
			errors.error(Errors.EXEC,tree.pos,"can not assign to a string");
		    } else {
			XDOMString stree = (XDOMString) tree;
			byte skind = stree.getKind();
			if (skind == XDOMString.SCOMMENT) {
			    join.start(ctx);
			    join.addSpace(stree.getSpaceBefore(),stree.pos);
			    stack.push("<!--",0);
			    join.add(stree.pos);
			    stack.push(value.escapeString(stree.getVal(),' '),0);
			    join.add(stree.pos);
			    stack.push("-->",0);
			    join.add(stree.pos);
			    join.addSpace(stree.getSpaceAfter(),stree.pos);
			    join.pop();
			} else if (skind == XDOMString.SXML) {
			    join.start(ctx);
			    join.addSpace(stree.getSpaceBefore(),stree.pos);
			    stack.push(value.escapeXMLTxt(stree.getVal()),0);
			    join.add(stree.pos);
			    join.addSpace(stree.getSpaceAfter(),stree.pos);
			    join.pop();
			} else {
			    stack.push(stree.getOval(),stree.getIval());
			}
		    }
		    break;
		}
		case XDOM.XNAME:
		{
		    XDOMName ntree = (XDOMName) tree;
		    if (ntree.getKind() == XDOMName.NXML) {
			join.start(ctx);
			join.addSpace(ntree.getSpaceBefore(),ntree.pos);
			execName(ntree,ctx);
			if (evalue.getS(tree.pos)) {
			    Object oval = stack.getTopOval();
			    long ival = stack.getTopIval();
			    stack.pop();
			    String s = XDOMValue.getString(oval,ival);
			    stack.push(value.escapeXMLTxt(s),0);
			} else {
			    stack.pushError();

			}
			join.add(ntree.pos);
			join.addSpace(ntree.getSpaceAfter(),ntree.pos);
			join.pop();
		    } else {
			execName(ntree,ctx);
		    }
		    break;
		}
		case XDOM.XCALL:
		{
		    XDOMCall ctree = (XDOMCall) tree;
		    callExec.execCall(ctree,ctx);
		    break;
		}
		default:
		{
		    if (ctx != value.ctxAssign) {
			stack.pushError();
		    }
		}
	    }
	    if (debug) {
		if (debugExecExp(tree,ctx)) {
		    boolean assigned = false;
		    if (tree instanceof XDOMCall) {
			XDOMCall ctree = (XDOMCall) tree;
			if (ctree.getKind() == XDOMCall.COP && ctree.getFunc() instanceof XDOMName) {
			    XDOMName ntree = (XDOMName) ctree.getFunc();
			    if (ntree.getName() == "Assign" && ! ntree.hasExt()) {
				assigned = true;
			    }
			}
		    }
		    if (! debugAfterAct(tree.pos,ctx,assigned)) break;
		} else {
		    break;
		}
	    } else {
		break;
	    }
	} 
	runLevel = oldRunLevel;
    }

    public void execFork(XDOMElement tree) {
	execBody(tree,false,value.ctxEval);
	XScope scope = tree.scope;
	XDef def = scope.defs;
	while (def != null) {
	    if (def instanceof XDefThread) {
		XDefThread tdef = (XDefThread) def;
		frames.getDefVal(tdef,(XPos) null);
		Object oval = stack.getTopOval();
		long ival = stack.getTopIval();
		stack.pop();
		if (oval instanceof VLThread) {
		    VLThread vlt = (VLThread) oval;
		    try {
			vlt.thread.join();
		    } catch(Exception e) {
		    }
		}
	    }
	    def = def.next;
	}
    }

    public void execThread(XDOMElement tree) {
	XDOMName ntree = TreeUtil.getName(tree);
	if (ntree != null && ntree.def != null && ntree.def instanceof XDefThread) {
	    XDefThread def = (XDefThread) ntree.def;
	    final XThread xt1 = new XThread(TreeUtil.getName(tree).getName(),xt.cmd);
	    xt1.parent = xt;
	    xt1.parentFrame = frames.getTop();
	    xt1.errors = xt.errors;
	    final Exec exec1 = xt1.exec;
	    final XDOMElement tree1 = tree;
	    VLThread vlt = new VLThread();
	    XFrame f = frames.getTopFrame();
	    vlt.xt = xt1;
	    vlt.def = def;
	    vlt.context = f;
	    vlt.uid = f.getUid();
	    final XFrame slink = frames.getTopFrame();
	    Thread t = new Thread(new Runnable() {
		public void run() {
		    try {
			exec1.frames.push(tree1.scope,slink);
			exec1.execBody(tree1,true,value.ctxEval);
			exec1.stack.pop();
			exec1.frames.pop(false);
		    } catch(Throwable e) {
			String s = e.getMessage();
			System.out.println("thread fail:"+xt1.name+"="+e);
			xt.errors.fail(e,xt1.name);
		    }
		}
	    });
	    vlt.thread = t;
	    stack.push(vlt,0);
	    frames.setDefVal(def);
	    stack.pop();
	    t.start();
	} else {
	    errors.error(Errors.EXEC,tree.pos,"can't process thread");
	}
	stack.pushNull();
    }

    private void execExclude(XDOMElement tree,Ctx ctx) {
	XDOM attr1 = tree.getAttr(1);
	execExp(attr1,value.ctxEval);
	Object oval = stack.getTopOval();
	long ival = stack.getTopIval();
	stack.pop();
	if (oval instanceof VLLock) {
	    VLLock lock = (VLLock) oval;
	    synchronized(lock) {
		execBody(tree,false,ctx);
	    }
	} else {
	    if (! value.isError(oval,ival)) {
		errors.error(Errors.EXEC,attr1.pos,"not a lock");
	    }
	    stack.pushError();
	}
    }

    public void execBody1(XDOMElement tree,boolean debugMe,Ctx ctx,
			   boolean render) {
	int oldRunLevel = runLevel;
	runLevel ++;
	int i;
	int bsize = tree.bodySize();
	while(true) {
	    if (debug) {
		if (debugMe) {
		    debugBeforeAct(tree.pos,value.ctxEval);
		}
	    }
	    join.start(ctx);
	    for (i = 1; i < bsize; i++) {
		XDOM x = tree.getBody(i);
		XDOM xnext = tree.getBody(i+1);
		boolean isXML = false;
		if (x instanceof XDOMElement) {
		    XDOMElement xe = (XDOMElement) x;
		    isXML = xe.getKind() == XDOMElement.EXML;
		}
		boolean nextXML = false;
		if (xnext instanceof XDOMElement) {
		    XDOMElement xe = (XDOMElement) xnext;
		    nextXML = xe.getKind() == XDOMElement.EXML;
		}
		execTree(x,ctx);
		join.add(x.pos);
	    }
	    if (bsize > 0) {
		XDOM x = tree.getBody(bsize);
		boolean isXML = false;
		if (x instanceof XDOMElement) {
		    XDOMElement xe = (XDOMElement) x;
		    isXML = xe.getKind() == XDOMElement.EXML;
		}
		execTree(x,ctx);
		join.add(x.pos);
	    }
	    join.pop();
	    if (debug) {
		if (debugMe) {
		    if (! debugAfterAct(tree.pos,value.ctxEval,false)) break;
		} else {
		    break;
		}
	    } else {
		break;
	    }
	} 
	runLevel = oldRunLevel;
    }

    public void execBody(XDOMElement tree,boolean debugMe,Ctx ctx) {
	int kind = tree.getKind();
	if (kind == XDOMElement.EXML) {
	    execBody1(tree,debugMe,ctx,true);
	} else {
	    execBody1(tree,debugMe,ctx,false);
	}
    }
    public void execBodyLang(XDOMElement tree,boolean debugMe,Ctx ctx,Object lang) {
	Object saveLang = xt.currentLang;
	xt.currentLang = lang;
	execBody(tree,debugMe,ctx);
	xt.currentLang = saveLang;
    }

    private void execIf(XDOMElement tree,Ctx ctx,VLLxact langx) {
	evalue.getB(tree.getAttr(1));
	Object oval = stack.getTopOval();
	long ival = stack.getTopIval();
	stack.pop();
	if (value.isError(oval,ival)) {
	    stack.pushError();
	} else {
	    if (ival == 0) {
		stack.pushNull();
		langx.doElse = true;
	    } else {
		execBody(tree,false,ctx);
		langx.doElse = false;
	    }
	}
    }

    private void execOrif(XDOMElement tree,Ctx ctx,VLLxact langx) {
	if (langx.doElse) {
	    execIf(tree,ctx,langx);
	} else {
	    stack.pushNull();
	}
    }

    private void execElse(XDOMElement tree,Ctx ctx,VLLxact langx) {
	if (langx.doElse) {
	    execBody(tree,false,ctx);
	} else {
	    stack.pushNull();
	}
    }

    private boolean checkViewKeep(Object oval,long ival,XDOM view,String where,XPos cpos) {
	boolean result = true;
	Object voval;
	long vival;
	int code;
	execExp(view,value.ctxEval);
	voval = stack.getTopOval();
	vival = stack.getTopIval();
	code = biExec.hasView(oval,ival,voval,vival,view);
	if (code == 0) {
	    result = false;
	    if (cpos != null) {
		errors.error(Errors.EXEC,view.pos,cpos,where+" not in view");
	    } else {
		errors.error(Errors.EXEC,view.pos,where+" not in view");
	    }
	}
	return result;
    }

    private boolean checkView(Object oval,long ival,XDOM view,String where,XPos cpos) {
	boolean result = true;
	if (view != null) {
	    result = checkViewKeep(oval,ival,view,where,cpos);
	    stack.pop();
	}
	return result;
    }

    private void execVar(XDOMElement tree,String where) {
	int asize = tree.attrSize();
//	int bsize = tree.bodySize();
	XDOMElement tval = null;
//	if (bsize > 0) {
//	    XDOM x = tree.getBody(1);
//	    if (x instanceof XDOMElement) {
//		tval = (XDOMElement) x;
//	    }
//	}
	if (asize > 0) {
	    XDOM spec = tree.getAttr(1);
	    XDOM specx = TreeUtil.stripView(spec);
	    if (specx instanceof XDOMName) {
		boolean ok = true;
		XDOMName ntree = (XDOMName) specx;
		XDefName ndef = (XDefName) ntree.def;
		if (option.check && ndef.hasView) {
		    XDOM view = TreeUtil.getView(spec);
		    if (tval != null) {
			if (! checkViewKeep(tval,0,view,where+" initial value",null)) {
			    ok = false;
			}
		    } else {
			execExp(view,value.ctxEval);
		    }
		    frames.setDefView(ndef);
		    stack.pop();
		}
		if (tval != null) {
		    if (debug) {
			xt.cmd.dconnect.assign(xt,tval,0);
		    }
		    if (ok) {
			stack.push(tval,0);
		    } else {
			stack.pushError();
		    }
		    frames.setDefVal(ndef);
		    stack.pop();
		}
	    } else if (spec instanceof XDOMCall) {
		XDOMCall ctree = (XDOMCall) spec;
		int size = ctree.argSize();
		if (size == 2) {
		    XDOM spec1 = TreeUtil.stripView(ctree.getArg(1));
		    if (spec1 instanceof XDOMName) {
			XDOMName ntree = (XDOMName) spec1;
			XDefName ndef = (XDefName) ntree.def;
			execExp(ctree.getArg(2),value.ctxEval);
			if (option.check) {
			    XDOM view = TreeUtil.getView(ctree.getArg(1));
			    if (view != null) {
				Object oval = stack.getTopOval();
				long ival = stack.getTopIval();
				if (! checkViewKeep(oval,ival,view,where+" initial value",null)) {
				    stack.pop();
				    stack.pushError();
				}
				if (ndef.hasView) {
				    frames.setDefView(ndef);
				}
				stack.pop();
			    }
			}
			if (debug) {
			    Object oval = stack.getTopOval();
			    long ival = stack.getTopIval();
			    xt.cmd.dconnect.assign(xt,oval,ival);
			}
			if (ndef == null) System.out.println("name="+ntree.getName());
			frames.setDefVal(ndef);
			stack.pop();
		    }
		}
	    }
	}
	stack.pushNull();
    }

    private void execFunc(XDOMElement tree) {
	XDOMName ntree = TreeUtil.getName(tree);
	if (ntree != null && ntree.def != null && ntree.def instanceof XDefFunc) {
	    XDefFunc fdef = (XDefFunc) ntree.def;
	    XFrame f = frames.getTopFrame();
	    VLFunc vlf = new VLFunc();
	    vlf.def = fdef;
	    vlf.context = f;
	    vlf.uid = f.getUid();
	    vlf.lang = xt.currentLang;
	    stack.push(vlf,0);
	    frames.setDefVal(fdef);
	    stack.pop();
	} else {
	    errors.error(Errors.EXEC,tree.pos,"can't process function");
	}
	stack.pushNull();
    }

    private boolean sameAttr(XDOM a1,XDOM a2) {
	if (a1.getXKind() != a2.getXKind()) return false;
	if (a1 instanceof XDOMCall && a2 instanceof XDOMCall) {
	    XDOMCall c1 = (XDOMCall) a1;
	    XDOMCall c2 = (XDOMCall) a2;
	    if (c1.argSize() != 2 || c2.argSize() != 2) return false;
	    XDOM func1 = c1.getFunc();
	    XDOM func2 = c2.getFunc();
	    if (func1.getXKind() != XDOM.XNAME || func2.getXKind() != XDOM.XNAME) return false;
	    XDOMName fn1 = (XDOMName) func1;
	    XDOMName fn2 = (XDOMName) func1;
	    if (fn1.getName() != "Equal" || fn2.getName() != "Equal") return false;
	    XDOM a11 = c1.getArg(1);
	    XDOM a21 = c2.getArg(1);
	    if (a11.getXKind() != XDOM.XNAME || a21.getXKind() != XDOM.XNAME) return false;
	    XDOMName n1 = (XDOMName) a11;
	    XDOMName n2 = (XDOMName) a21;
	    if (n1.getName() != n2.getName()) return false;
	    return sameAttr(c1.getArg(2),c2.getArg(2));
	} else if (a1 instanceof XDOMInt && a2 instanceof XDOMInt) {
	    XDOMInt i1 = (XDOMInt) a1;
	    XDOMInt i2 = (XDOMInt) a2;
	    if (i1.getInt() != i2.getInt()) return false;
	} else if (a1 instanceof XDOMFloat && a2 instanceof XDOMFloat) {
	    XDOMFloat f1 = (XDOMFloat) a1;
	    XDOMFloat f2 = (XDOMFloat) a2;
	    if (f1.getFloat() != f2.getFloat()) return false;
	} else if (a1 instanceof XDOMString && a2 instanceof XDOMString) {
	    XDOMString s1 = (XDOMString) a1;
	    XDOMString s2 = (XDOMString) a2;
	    if (s1.getVal() != s2.getVal()) return false;
	} else if (a1 instanceof XDOMValue && a2 instanceof XDOMValue) {
	    XDOMValue v1 = (XDOMValue) a1;
	    XDOMValue v2 = (XDOMValue) a2;
	    Object oval1 = v1.getOval();
	    long ival1 = v1.getIval();
	    Object oval2 = v2.getOval();
	    long ival2 = v2.getIval();
	    if (oval1 != oval2) return false;
	    if (ival1 != ival2) return false;
	} else {
	    return false;
	}
	return true;
    }
    
    private boolean sameView(int top,int extCnt,Stack stack1,boolean varExt) {
	for (int i = 0; i < extCnt; i++) {
	    Object oval1 = stack.getOval(top+i+1);
	    long ival1 = stack.getIval(top+i+1);
	    Object oval2 = stack1.getOval(i);
	    long ival2 = stack1.getIval(i);
	 /* should call @equal operator ??? */
	 /* and use sys:TEq op ??? */
	    if (oval1 == oval2 && ival1 == ival2) {
	    } else if (varExt) {
		if (oval1 instanceof XDOMElement && oval2 instanceof XDOMElement) {
		    XDOMElement e1 = (XDOMElement) oval1;
		    XDOMElement e2 = (XDOMElement) oval2;
		    int size1 = e1.attrSize();
		    int size2 = e2.attrSize();
		    if (size1 != size2) return false;
		    int j;
		    for (j = 1; j <= size1; j++) {
			XDOM attr1 = e1.getAttr(j);
			XDOM attr2 = e2.getAttr(j);
			if (! sameAttr(attr1,attr2)) return false;
		    }
		} else {
		    return false;
		}
	    } else {
		return false;
	    }
	}
	return true;
    }

    public void makeView(VLView vlv,XDOMName ntree) {
	if (vlv.frame != null) {
	    stack.push(vlv,0);
	    return;
	}
	if (vlv.def.hasExt) {
	    int i;
	    int top = stack.getTop();
	    callExec.pushExt(vlv.def,ntree);
	    int extCnt = stack.getTop() - top;

	    if (vlv.def.exts != extCnt) {
		errors.error(Errors.EXEC,ntree.pos,"extension has wrong size: "+extCnt+
			     " (expect "+vlv.def.exts+")");
		stack.pushError();
		return;
	    }

	    VLView vlv1 = vlv.next;
	    while (vlv1 != null) {
		if (sameView(top,extCnt,vlv1.frame.stack,vlv.def.varExts)) {
		    stack.setTop(top);
		    stack.push(vlv1,0);
		    if (debugView) {
			System.out.println("reusing view "+ntree.getName()+"[...]");
		    }
		    return;
		}
		vlv1 = vlv1.next;
	    }

	    if (debugView) {
		System.out.println("creating view "+ntree.getName()+"[...]");
	    }
	    if (vlv instanceof VLType) {
		vlv1 = new VLType();
	    } else {
		vlv1 = new VLView();
	    }
	    vlv1.next = vlv.next;
	    vlv.next = vlv1;

	    vlv1.def = vlv.def;
	    vlv1.uid = vlv.uid;
	    vlv1.lang = vlv.lang;
	    vlv1.context = vlv.context;

	    XFrame f = new XFrame();
	    XDOMElement tree = vlv.def.vtree;
	    vlv1.frame = f;
	    f.uid = -1; // use parent 
	    f.slink = vlv.context;
	    f.stack = new FixedStack(tree.scope.size);
	    f.base = 0;
	    f.scope = tree.scope;
	    f.caller = null;
	    f.level = tree.scope.level;
	    f.size = tree.scope.size;
	    for (i = 0; i < extCnt; i++) {
		Object oval = stack.getOval(top+i+1);
		long ival = stack.getIval(top+i+1);
		f.stack.setVal(i,oval,ival);
	     //f.stack.push(oval,ival);
	    }
	    stack.setTop(top);
	    XFrame old = frames.pushFrame(vlv1.frame);
	    vlv1.frame.caller = ntree;
	    if (option.check) {
		boolean fail = false;
		for (i = 0; i < extCnt; i++) {
		    Object oval = f.stack.getOval(i);
		    long ival = f.stack.getIval(i);
		    if (! checkExt(oval,ival,i,vlv.def.vtree,ntree.pos)) {
			fail = true;
		    }
		}
		if (! fail && vlv.def.assert1 != -1) {
		    XDOMCall xc = (XDOMCall) vlv.def.vtree.getAttr(vlv.def.assert1);
		    opExec.execAssert(xc,ntree.pos);
		    stack.pop();
		}
	    }
	    execBody(tree,true,value.ctxEval);
	    stack.pop();
	    frames.popFrame(false,old);
	    Object oval = f.stack.getOval(0);
	    long ival = f.stack.getIval(0);
	    vlv1.frame.caller = null;
	    stack.push(vlv1,0);
	} else {
	    if (vlv.frame == null) {
		XFrame f = new XFrame();
		XDOMElement tree = vlv.def.vtree;
		if (debugView) {
		    System.out.println("creating view "+ntree.getName());
		}
		vlv.frame = f;
		f.uid = -1; // use parent 
		f.slink = vlv.context;
		f.stack = new FixedStack(tree.scope.size);
		f.base = 0;
		f.scope = tree.scope;
		f.caller = null;
		f.level = tree.scope.level;
		f.size = tree.scope.size;
		XFrame old = frames.pushFrame(vlv.frame);
		vlv.frame.caller = ntree;
		execBody(tree,true,value.ctxEval);
		stack.pop();
		frames.popFrame(false,old);
		vlv.frame.caller = null;
	    }
	    stack.push(vlv,0);
	}
    }

    private void execView(XDOMElement tree,boolean isType) {
	int asize = tree.attrSize();
	boolean ok = false;
	if (asize > 0 && tree.getAttr(1) instanceof XDOMName) {
	    XDOMName ntree = (XDOMName) tree.getAttr(1);
	    XDefView def = (XDefView) ntree.def;
	    int level = def.scope.level;
	    VLView vlv;
	    if (isType) {
		vlv = new VLType();
	    } else {
		vlv = new VLView();
	    }
	    XFrame context = frames.getTopFrame();
	    vlv.def = def;
	    vlv.frame = null;
	    vlv.uid = context.uid;
	    vlv.context = context;
	    vlv.lang = xt.currentLang;
	    stack.push(vlv,0);
	    frames.setDefVal(def);
	    stack.pop();
	    if (isType && def.hasExt) {
		// set value for name without ext
		XDefView def1 = (XDefView)(def.next);
		VLView vlv1 = new VLView();
		vlv1.def = def1;
		vlv1.frame = null;
		vlv1.uid = context.uid;
		vlv1.context = context;
		vlv1.lang = xt.currentLang;
		stack.push(vlv1,0);
		frames.setDefVal(def1);
		stack.pop();
	    }
	    ok = true;
	}
	if (! ok) {
	    if (isType) {
		errors.error(Errors.EXEC,tree.pos,"type element has wrong form");
	    } else {
		errors.error(Errors.EXEC,tree.pos,"view element has wrong form");
	    }
	}
	stack.pushNull();
    }

    private void execLang1(XDOMElement tree) {
	stack.pushNull();
	int size = tree.attrSize();
	String script = null;
	String lang = null;
	if (size != 2) return;
	XDOM arg2 = tree.getAttr(2);
	XDOMName name2 = null;
	if (arg2 instanceof XDOMName) {
	    name2 = (XDOMName) arg2;
	    if (! name2.hasExt()) {
		lang = name2.getName();
	    }
	}
	if (lang == null) return;
	execExp(name2,value.ctxEval);
	Object oval1 = stack.getTopOval();
	long ival1 = stack.getTopIval();
	stack.pop();
	if (oval1 instanceof VLLxact || oval1 instanceof VLLrender || oval1 instanceof VLLerror ||
	      oval1 instanceof VLObj) {
	    xt.currentLang = oval1;
	} else {
	    errors.error(Errors.BIND,arg2.pos,"not a language object");
	}
    }

    private void execExp1(XDOMElement tree) {
	int asize = tree.attrSize();
	if (asize > 0) {
	    execExp(tree.getAttr(1),value.ctxEval);
	} else {
	    errors.error(Errors.EXEC,tree.pos,"exp element has wrong form");
	    stack.pushError();
	}
    }

    private void execLang(XDOMElement tree) {
	int asize = tree.attrSize();
	if (asize > 0) {
	    execExp(tree.getAttr(1),value.ctxEval);
	    Object oval = stack.getTopOval();
	    long ival = stack.getTopIval();
	    stack.pop();
	    if (oval instanceof VLLxact || oval instanceof VLLrender || oval instanceof VLLerror ||
		  oval instanceof VLObj) {
		xt.currentLang = oval;
	    } else if (! value.isError(oval,ival)) {
		errors.error(Errors.EXEC,tree.getAttr(1).pos,"not a language object");
	    }
	    stack.pushNull();
	} else {
	    errors.error(Errors.EXEC,tree.pos,"lang element has wrong form");
	    stack.pushError();
	}
    }

    private void execFor(XDOMElement tree,Ctx ctx,VLLxact langx) {
	int args = 1;
	XDOMCall spec = (XDOMCall) tree.getAttr(1);
	XDOM ids = spec.getArg(1);
	if (ids instanceof XDOMCall) {
	    XDOMCall idsc = (XDOMCall) ids;
	    args = idsc.argSize();
	}
	XDOM iter = spec.getArg(2);
	XFrame f = frames.getTopFrame();
	XDefFunc xdf = new XDefFunc();
	VLFunc vlf = new VLFunc();
	XDefVal def = new XDefVal();
	XDOMName name = new XDOMName("sys:Body");
	XDOMCall call = new XDOMCall(iter,1);
	call.insertArg(-1,name);
	name.pos = tree.pos.copy();
	call.pos = tree.pos.copy();
	name.def = def;
	def.oval = vlf;
	def.ival = 0;
	def.name = "sys:Body";
	def.hasExt = false;
	def.readOnly = true;
	def.visible = false;
	def.local = false;
	def.scope = null;
	def.next = null;
	vlf.def = xdf;
	vlf.context = f;
	vlf.uid = f.getUid();
	vlf.lang = xt.currentLang;
	vlf.called = false;
	xdf.ftree = tree;
	xdf.hasArgs = true;
	xdf.varArgs = false;
	xdf.args = args;
	xdf.varExts = false;
	xdf.exts = 0;
	xdf.assert1 = -1;
	callExec.execCall(call,ctx);
	langx.doElse = ! vlf.called;
    }

    private void execWhile(XDOMElement tree,Ctx ctx) {
	join.start(ctx);
	boolean save = xt.doBreak;
	xt.doBreak = false;
	while (! xt.doBreak) {
	    evalue.getB(tree.getAttr(1));
	    Object oval = stack.getTopOval();
	    long ival = stack.getTopIval();
	    stack.pop();
	    if (value.isError(oval,ival)) {
		break;
	    } else {
		if (ival == 0) {
		    break;
		} else {
		    execBody(tree,false,ctx);
		    join.add(tree.pos); 
		}
	    }
	}
	join.pop();
	xt.doBreak = save;
    }

    private void execDefault(String space,String base,XDOMElement tree,XDOMElement bodyTree,Ctx ctx) {
	boolean render = false;
//	int kind = bodyTree.getKind();
	int kind = tree.getKind();
	if (kind == XDOMElement.EXML) {
	    render = true;
	}
	join.start(ctx);
	if (render) join.addSpace(tree.getSpaceBefore(),tree.pos);
	int size = tree.attrSize();
	if (tree.getKind() == XDOMElement.EXMLDECL) {
	    stack.push("<?",0);
	} else {
	    stack.push("<",0);
	}
	join.add(tree.pos);
	String tag = base;
	if (space != "") tag = (space + ":" + base).intern();
	stack.push(tag,0);
	join.add(tree.pos);
	for (int i = 1; i <= size; i++) {
	    XDOM t = tree.getAttr(i);
	    if (t instanceof XDOMCall) {
		XDOMCall ctree = (XDOMCall) t;
		if (ctree.argSize() == 2 && ctree.getArg(1) instanceof XDOMName) {
		    XDOMName ntree = (XDOMName) ctree.getArg(1);
		    XDOM arg2 = ctree.getArg(2);
		    evalue.getS(arg2);
		    Object oval = stack.getTopOval();
		    long ival = stack.getTopIval();
		    stack.pop();
		    if (XDOMValue.isString(oval,ival)) {
			String s = XDOMValue.getString(oval,ival);
			boolean single = false;
			if (arg2 instanceof XDOMString) {
			    XDOMString xs = (XDOMString) arg2;
			    if (xs.getKind() == XDOMString.SSINGLE) single = true;
			} else if (arg2 instanceof XDOMCall) {
			    XDOMCall xc = (XDOMCall) arg2;
			    if (xc.getKind() == XDOMCall.CSINGLE) single = true;
			}
			stack.push(" ",0);
			join.add(t.pos);
			stack.push(ntree.getName(),0);
			join.add(t.pos);
			if (single) {
			    stack.push("='",0);
			    join.add(t.pos);
			    stack.push(value.escapeString(s,'\''),0);
			    join.add(t.pos);
			    stack.push("'",0);
			    join.add(t.pos);
			} else {
			    stack.push("=\"",0);
			    join.add(t.pos);
			    stack.push(value.escapeString(s,'"'),0);
			    join.add(t.pos);
			    stack.push("\"",0);
			    join.add(t.pos);
			}
		    }
		}
	    }
	}
	if (tree.getKind() == XDOMElement.EXMLDECL) {
	    stack.push("?>",0);
	    join.add(tree.pos);
	} else  if (bodyTree.bodySize() == 0) {
	    stack.push("/>",0);
	    join.add(tree.pos);
	} else {
	    stack.push(">",0);
	    join.add(tree.pos);
	    if (render) join.addSpace(tree.getSpaceBeforeBody(),tree.pos);
	    execBody1(bodyTree,false,ctx,render);
	    join.add(bodyTree.pos);
	    if (render) join.addSpace(tree.getSpaceAfterBody(),tree.pos);
	    stack.push("</",0);
	    join.add(tree.pos);
	    stack.push(tree.getTag(),0);
	    join.add(tree.pos);
	    stack.push(">",0);
	    join.add(tree.pos);
	}
	if (render) join.addSpace(tree.getSpaceAfter(),tree.pos);
	join.pop();
    }

    private void execTag(XDOMElement tree,Ctx ctx) {
	int size = tree.attrSize();
	if (size > 0) {
	    XDOM attr1 = tree.getAttr(1);
	    execExp(attr1,value.ctxEval);
	    Object oval = stack.getTopOval();
	    long ival = stack.getTopIval();
	    stack.pop();
	    if (oval instanceof XDOMElement) {
		XDOMElement elem = (XDOMElement) oval;
		execDefault(elem.getSpace(),elem.getBase(),elem,tree,ctx);
	    } else if (value.isNull(oval,ival)) {
		execBody(tree,false,ctx);
	    } else if (value.isError(oval,ival)) {
	    } else {
		errors.error(Errors.EXEC,attr1.pos,"not an element tree");
		stack.pushError();
	    }
	} else {
	    errors.error(Errors.EXEC,tree.pos,"missing attribute");
	    stack.pushError();
	}
    }

    private boolean debugExecCmd(XDOMElement tree,Ctx ctx,VLLxact langx) {
	boolean debugMe = true;
	String tag = tree.getTag();
	if (tree.getNoStep()) return false;
	if (tag == "x:comment") {
	    return false;
	} else if (tag == "x:func") {
	    return false;
	} else if (tag == "x:type") {
	    return false;
	} else if (tag == "x:view") {
	    return false;
	} else if (tag == "x:self") {
	    return false;
	} else if (tag == "x:else" && ! langx.doElse) {
	    return false;
	} else if (tag == "x:orif" && ! langx.doElse) {
	    return false;
	} else if (tag == "x:use" && ! langx.doElse) {
	    return false;
	}
	return true;
    }

    private void execxact(String space,String base,
			  XDOMElement etree,Ctx ctx,VLLxact langx) {
	if (space == "x") {
	    if (base == "func") { execFunc(etree); }
	    else if (base == "type") { execView(etree,true); }
	    else if (base == "view") { execView(etree,false); }
	    else if (base == "self") { stack.pushNull(); }
	    else if (base == "module") { execBody(etree,false,ctx); }
	    else if (base == "thread") { execThread(etree);}
	    else {
		boolean push = false;
		XFrame old = null;
		if (etree.scope != null) {
		    if (etree.scope.top) {
			Load ld = xt.cmd.loads.getLoad(etree.scope.level);
			old = frames.pushFrame(ld.frame);
		    } else {
			frames.push(etree.scope);
		    }
		    push = true;
		}
		if (base == "block") { execBody(etree,false,ctx); }
		else if (base == "func") { execFunc(etree); }
		else if (base == "if") { execIf(etree,ctx,langx); }
		else if (base == "orif") { execOrif(etree,ctx,langx); }
		else if (base == "else") { execElse(etree,ctx,langx); }
		else if (base == "for") { execFor(etree,ctx,langx); }
		else if (base == "for1") { execFor(etree,ctx,langx); }
		else if (base == "var") { execVar(etree,"var"); }
		else if (base == "const") { execVar(etree,"const"); }
		else if (base == "comment") { stack.pushNull(); }
		else if (base == "use") { stack.pushNull(); }
		else if (base == "exp") { execExp1(etree); }
		else if (base == "lang") { execLang1(etree); }
		else if (base == "while") { execWhile(etree,ctx); }
		else if (base == "break") { xt.doBreak = true; stack.pushNull();}
		else if (base == "fork") { execFork(etree);}
		else if (base == "exclude") { execExclude(etree,ctx);}
		else if (base == "tag") { execTag(etree,ctx);}
		else {
		    errors.error(Errors.EXEC,etree.pos,"don't know how to evaluate this tag");
		    stack.pushError();
		}
		if (push) {
		    if (old != null) {
			frames.popFrame(true,old);
		    } else {
			frames.pop(true);
		    }
		}
	    }
	} else {
	    execCmdLang(space,base,etree,ctx,langx.underLang);
	}
    }

    private void execrender(String space,String base,
			     XDOMElement etree,Ctx ctx,VLLrender langr) {
	execDefault(space,base,etree,etree,ctx);
    }

    public void  execCmdLang(String space,String base,XDOMElement tree,Ctx ctx,Object lang) {
	if (lang instanceof VLLxact) {
	    VLLxact langx = (VLLxact) lang;
	    while (true) {
		if (debug) {
		    if (debugExecCmd(tree,ctx,langx)) {
			debugBeforeAct(tree.pos,ctx);
		    }
		}
		boolean ok = false;
		execxact(space,base,tree,ctx,langx);
		if (debug) {
		    if (debugExecCmd(tree,ctx,langx)) {
			boolean assigned = false;
			String tag = tree.getTag();
			if (tag == "x:var" || tag == "x:const") assigned = true;
			if (! debugAfterAct(tree.pos,ctx,assigned)) break;
		    } else {
			break;
		    }
		} else {
		    break;
		}
	    }
	} else if (lang instanceof VLLrender) {
	    VLLrender langr = (VLLrender) lang;
	    boolean push = false;
	    XFrame old = null;
	    if (tree.scope != null) {
		if (tree.scope.top) {
		    Load ld = xt.cmd.loads.getLoad(tree.scope.level);
		    old = frames.pushFrame(ld.frame);
		} else {
		    frames.push(tree.scope);
		}
		push = true;
	    }
	    execrender(space,base,tree,ctx,langr);
	    if (push) {
		if (old != null) {
		    frames.popFrame(true,old);
		} else {
		    frames.pop(true);
		}
	    }
	} else if (lang instanceof VLLerror) {
	    errors.error(Errors.BIND,tree.pos,"don't know how to exec this element");
	    if (ctx != value.ctxAssign) stack.pushError();
	} else if (lang instanceof VLObj) {
	    VLObj vlo = (VLObj) lang;
	    boolean push = false;
	    XFrame old = null;
	    if (tree.scope != null) {
		if (tree.scope.top) {
		    Load ld = xt.cmd.loads.getLoad(tree.scope.level);
		    old = frames.pushFrame(ld.frame);
		} else {
		    frames.push(tree.scope);
		}
		push = true;
	    }
	    byte oldPass = xt.exec.interExec.pass;
	    interExec.pass = InterExec.PEXEC;
	    stack.push("exec",0);
	    stack.push(space,0);
	    stack.push(base,0);
	    stack.push(tree,0);
	    stack.push(lang,0);
	    stack.pushNull(); // info
	    XDef walk = callExec.findCallObject(vlo.type,
		"Walk",false,null);
	    if (walk != null) {
		Layer layer = layerStack.top();
		layer.slink = frames.getTopFrame();
//		System.out.println("walk="+layer.slink+" "+tree.getTag()+frames.getTopFrame()+" "+frames.getTop());
		callExec.execCallObject(walk,vlo.type,vlo,0,true,0,6,tree,
					xt.cmd.value.ctxEval);
		frames.setSlink(layer.slink);
//		System.out.println("afterwalk "+tree.getTag());
//		System.out.println("link1="+layer.slink);
//		System.out.println("link2="+frames.getTopFrame()+" "+frames.getTop());
//		for (int ii = 0; ii <= frames.getTop(); ii++) {
//		    System.out.println(ii+"=>"+frames.getFrame(ii));
//		}
	    } else {
		errors.error(Errors.BIND,tree.pos,"no language object Walk function");
		if (ctx != value.ctxAssign) stack.pushError();
	    }
	    interExec.pass = oldPass;
	    if (push) {
		if (old != null) {
		    frames.popFrame(true,old);
		} else {
		    frames.pop(true);
		}
	    }
	} else {
	    errors.error(Errors.BIND,tree.pos,"not a language object");
	    if (ctx != value.ctxAssign) stack.pushError();
	}
    }

    private void execCmd(XDOMElement tree,Ctx ctx) {
	int oldRunLevel = runLevel;
	runLevel ++;
	String space = tree.getSpace();
	String base = tree.getBase();
	execCmdLang(space,base,tree,ctx,xt.currentLang);
	runLevel = oldRunLevel;
    }

    private void execTree1(XDOM tree,Ctx ctx) {
	if (tree instanceof XDOMElement) {
	    if (ctx == value.ctxAssign) {
		errors.error(Errors.EXEC,tree.pos,"can't assign to a tree element");
	    } else {
		execCmd((XDOMElement) tree,ctx);
	    }
	} else {
	    execExp(tree,ctx);
	}
    }

    private void execTree(XDOM tree,Ctx ctx) {
	int i1 = stack.getTop();
	if (ctx != value.ctxAssign) {
	    long count = 0;
	    if (ctx instanceof CtxStream) {
		CtxStream cts = (CtxStream) ctx;
		count = cts.w.count;
	    }
	    join.start(ctx);
	    execTree1(tree,ctx);
	    Object oval = stack.getTopOval();
	    long ival = stack.getTopIval();
	    if (! value.isNull(oval,ival)) {
		join.add(tree.pos);
	    } else if (ctx instanceof CtxStream) {
		CtxStream cts = (CtxStream) ctx;
		if (cts.w.count > count) {
		    join.add(tree.pos);
		}
	    }
	    join.pop();
	} else {
	    execTree1(tree,ctx);
	}
	int i2 = stack.getTop();
	if (i2 != i1+1) {
	    System.out.println("execTree failed:"+i1+":"+i2);
	    errors.error(Errors.INTERNAL,tree.pos,"execTree sync error");
//	    new Throwable("execTree syncError").printStackTrace();
	}
    }

    public void exec(XDOM tree,Ctx ctx,Object lang) {
	Object oldLang = xt.currentLang;
	xt.currentLang = lang;
	int top = stack.getTop();
	runLevel = 0;
	Layer layer = new Layer(null,InterExec.PEXEC);
	xt.exec.layerStack.push(layer);
	execTree(tree,ctx);
	xt.exec.layerStack.pop();
	int top1 = stack.getTop();
	if (top1 != top+1) {
	    errors.error(Errors.INTERNAL,tree.pos,"stack sync error (top="+top+")");
	}
	xt.currentLang = oldLang;
    }

    public Object getOval() {
	return stack.getTopOval();
    }

    public long getIval() {
	return stack.getTopIval();
    }
}
