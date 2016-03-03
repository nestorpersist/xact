/*******************************************************************************
*
* Copyright (c) 2002-2007. John R. Nestor. All rights reserved.
*
* See the file XLICENSE.txt for use and redistribution information.
*
*******************************************************************************/
package com.persist.xact.exec;

import com.persist.xact.system.*;
import com.persist.xact.bind.*;
import com.persist.xdom.*;
import com.persist.xact.value.*;

public final class CallExec {
    private XThread xt;
    private Exec exec;
    private JExec jExec;
    private BIExec biExec;
    private VariableStack stack;
    private Value value;
    private Errors errors;
    private Frames frames;
    private XOption option;
    
    public CallExec(Exec exec) {
	this.exec = exec;
    }

    public void init() {
	jExec = exec.jExec;
	xt = exec.xt;
	stack = exec.stack;
	value = xt.cmd.value;
	errors = xt.errors;
	frames = exec.frames;
	biExec = exec.biExec;
	option = xt.cmd.option;
    }

    private final static int NOTVAR = 0;
    private final static int POSVAR = 1;
    private final static int NAMEVAR = 2;
    
    private int varTreeKind(Object oval) {
	if (oval instanceof XDOMElement) {
	    int result = POSVAR;
	    int pos = 1;
	    XDOMElement tree = (XDOMElement) oval;
	    if (tree.bodySize() != 0) return NOTVAR;
	    int size = tree.attrSize();
	    for (int i = 1; i <= size; i++) {
		XDOM attr = tree.getAttr(i);
		if (TreeUtil.isEqual(attr)) result = NAMEVAR;
	    }
	    return result;
	}
	return NOTVAR;
    }

    private void nameVal(XDOMName ntree) {
	XDef def = ntree.def;
	if (def != null) def = def.getReal();
	if (def instanceof XDefVal) {
	    XDefVal vdef = (XDefVal) def;
	    stack.push(vdef.oval,vdef.ival);
	} else if (def instanceof XDefName) {
	    XDefName ndef = (XDefName) def;
	    frames.getDefVal(ndef,ntree.pos);
	} else {
	    errors.error(Errors.INTERNAL,ntree.pos,"unrecognized name def"+def);
	    stack.pushError();
	}
    }

    public int pushExt(XDOMName extTree) {
	int cnt = 0;
	if (extTree.hasExt()) {
	    cnt = extTree.extSize();
	    for (int i = 0; i < cnt; i++) {
		XDOM ext = extTree.getExt(i+1);
		XDOM ext1 = TreeUtil.stripPercent(ext);
		exec.execExp(ext1,value.ctxEval);
		if (ext != ext1) {
		    Object oval = stack.getTopOval();
		    stack.pop();
		    cnt --;
		    if (varTreeKind(oval) == POSVAR) {
			XDOMElement xe = (XDOMElement) oval;
			int size1 = xe.attrSize();
			int j;
			for (j = 1; j <= size1; j++) {
			    xe.getAttr(j);
			    cnt ++;
			}
		    } else {
			errors.error(Errors.EXEC,ext1.pos,"illegal var tree");
		    }
		}
	    }
	}
	return cnt;
    }

    public int pushVarExt(XDOMName extTree) {
	XDOMElement tree = null;
	int cnt = 0;
	if (extTree.hasExt()) {
	    cnt = extTree.extSize();
	    if (cnt == 1) {
		XDOM ext = extTree.getExt(1);
		XDOM ext1 = TreeUtil.stripPercent(ext);
		if (ext != ext1) {
		    // [%ACTUAL] => [%FORMAL]
		    exec.execExp(ext1,value.ctxEval);
		    Object oval = stack.getTopOval();
		    if (varTreeKind(oval) != NOTVAR) return 1;
		    stack.pop();
		}
	    }
	    tree = new XDOMElement("extension");
	    //tree.setKind(XDOMElement.EXML);
	    for (int i = 0; i < cnt; i++) {
		XDOM ext = extTree.getExt(i+1);
		XDOM ext1 = TreeUtil.stripPercent(ext);
		if (ext != ext1) {
		    exec.execExp(ext1,value.ctxEval);
		    Object oval = stack.getTopOval();
		    stack.pop();
		    if (varTreeKind(oval) != NOTVAR) {
			XDOMElement tree1 = (XDOMElement) oval;
			int size = tree1.attrSize();
			int j;
			for (j = 1; j <= size; j++) {
			    XDOM attr = tree1.getAttr(j);
			    if (TreeUtil.isEqual(attr)) {
				String name = TreeUtil.getEqualName(attr);
				XDOM val = TreeUtil.stripEqual(attr);
				exec.execExp(val,value.ctxEval);
				Object oval1 = stack.getTopOval();
				long ival1 = stack.getTopIval();
				stack.pop();
				tree.setERec(name,oval1,ival1);
			    } else {
				exec.execExp(attr,value.ctxEval);
				Object oval1 = stack.getTopOval();
				long ival1 = stack.getTopIval();
				stack.pop();
				tree.insertEArray(-1,oval1,ival1);
			    }
			}
		    } else {
			errors.error(Errors.EXEC,ext1.pos,"illegal var tree");
		    }
		} else {
		    if (TreeUtil.isEqual(ext1)) {
			XDOM ext2 = TreeUtil.stripEqual(ext1);
			String name = TreeUtil.getEqualName(ext1);
			exec.execExp(ext2,value.ctxEval);
			tree.setERec(name,stack.getTopOval(),stack.getTopIval());
		    } else {
			exec.execExp(ext1,value.ctxEval);
			tree.insertEArray(-1,stack.getTopOval(),stack.getTopIval());
		    }
		    stack.pop();
		}
	    }
	} else {
	    tree = new XDOMElement("extension");
	    //tree.setKind(XDOMElement.EXML);
	}
	stack.push(tree,0);
	return 1;
    }

    public int pushExt(XDef def,XDOMName extTree) {
	if (def instanceof XDefFunc) {
	    XDefFunc fdef = (XDefFunc) def;
	    if (fdef.varExts) return pushVarExt(extTree);
	} else if (def instanceof XDefView) {
	    XDefView vdef = (XDefView) def;
	    if (vdef.varExts) return pushVarExt(extTree);
	}
	return pushExt(extTree);
    }

    public int pushArg(XDOMCall argTree) {
	int cnt = 0;
	if (argTree != null) {
	    int size = argTree.argSize();
	    for (int i = 1; i <= size; i++) {
		XDOM arg = argTree.getArg(i);
		XDOM arg1 = TreeUtil.stripPercent(arg);
		exec.execExp(arg1,value.ctxEval);
		cnt ++;
		if (arg != arg1) {
		    Object oval = stack.getTopOval();
		    stack.pop();
		    cnt --;
		    if (varTreeKind(oval) == POSVAR) {
			XDOMElement xe = (XDOMElement) oval;
			int size1 = xe.arraySize();
			int j;
			for (j = 1; j <= size1; j++) {
			    XDOM val = xe.getEArray(j);
			    int kind = val.getXKind();
			    if (kind == XDOM.XSTRING) {
				XDOMString xs = (XDOMString) val;
				stack.push(xs.getOval(),xs.getIval());
			    } else if (kind == XDOM.XINT) {
				XDOMInt xi = (XDOMInt) val;
				stack.push(value.vlInt,xi.getInt());
			    } else if (kind == XDOM.XFLOAT) {
				XDOMFloat xf = (XDOMFloat) val;
				double d = xf.getFloat();
				long id = Double.doubleToLongBits(d);
				stack.push(value.vlFloat,id);
			    }else if (kind == XDOM.XVALUE) {
				XDOMValue xv = (XDOMValue) val;
				stack.push(xv.getOval(),xv.getIval());
			    } else {
				errors.error(Errors.INTERNAL,arg1.pos,"bad array value");
				stack.pushError();
			    }
			    cnt ++;
			}
		    } else {
			errors.error(Errors.EXEC,arg1.pos,"illegal var tree");
		    }
		}
	    }
	}
	return cnt;
    }

    public int pushVarArg(XDOMCall argTree) {
	XDOMElement tree = null;
	int pos = 1;
	int cnt = 0;
	if (argTree == null) {
	    tree = new XDOMElement("parameters");
	    //tree.setKind(XDOMElement.EXML);
	} else {
	    cnt = argTree.argSize();
	    if (cnt == 1) {
		XDOM arg = argTree.getArg(1);
		XDOM arg1 = TreeUtil.stripPercent(arg);
		if (arg != arg1) {
		    //(%ACTUAL) => (%FORMAL) 
		    exec.execExp(arg1,value.ctxEval);
		    Object oval = stack.getTopOval();
		    if (varTreeKind(oval) != NOTVAR) return 1;
		    stack.pop();
		}
	    }
	    tree = new XDOMElement("parameters");
	    //tree.setKind(XDOMElement.EXML);
	    for (int i = 1; i <= cnt; i++) {
		XDOM arg = argTree.getArg(i);
		XDOM arg1 = TreeUtil.stripPercent(arg);
		if (arg != arg1) {
		    exec.execExp(arg1,value.ctxEval);
		    Object oval = stack.getTopOval();
		    stack.pop();
		    if (varTreeKind(oval) != NOTVAR) {
			XDOMElement tree1 = (XDOMElement) oval;
			int size = tree1.attrSize();
			int j;
			for (j = 1; j <= size; j++) {
			    XDOM attr = tree1.getAttr(j);
			    if (TreeUtil.isEqual(attr)) {
				String name = TreeUtil.getEqualName(attr);
				XDOM val = TreeUtil.stripEqual(attr);
				exec.execExp(val,value.ctxEval);
				Object oval1 = stack.getTopOval();
				long ival1 = stack.getTopIval();
				stack.pop();
				tree.setERec(name,oval1,ival1);
			    } else {
				exec.execExp(attr,value.ctxEval);
				Object oval1 = stack.getTopOval();
				long ival1 = stack.getTopIval();
				stack.pop();
				tree.insertEArray(-1,oval1,ival1);
			    }
			}
		    } else {
			errors.error(Errors.EXEC,arg1.pos,"illegal var tree");
		    }
		} else {
		    if (TreeUtil.isEqual(arg1)) {
			XDOM arg2 = TreeUtil.stripEqual(arg1);
			String name = TreeUtil.getEqualName(arg1);
			exec.execExp(arg2,value.ctxEval);
			tree.setERec(name,stack.getTopOval(),stack.getTopIval());
		    } else {
			exec.execExp(arg1,value.ctxEval);
			tree.insertEArray(-1,stack.getTopOval(),stack.getTopIval());
		    }
		    stack.pop();
		}
	    }
	}
	stack.push(tree,0);
	return 1;
    }

    public int pushArg(XDefFunc def,XDOMCall argTree) {
	if (def.varArgs) return pushVarArg(argTree);
	return pushArg(argTree);
    }

    public void execCallFunc(VLFunc vlf,int extCnt,int argCnt,XDOM caller,Ctx ctx) {
	XPos pos = null;
	if (caller != null) pos = caller.pos;
	boolean ok = true;
	boolean lhs = false;
	int argIdx = stack.getTop() - argCnt + 1; // first arg
	int extIdx = argIdx - extCnt;             // first ext 
	int top = extIdx - 1;			  // top before call 

	vlf.called = true;
//	if (vlf == null) {
//	    // ??? temp hack when debugger calls toString on self before frame initialized
//	    stack.pushError();
//	    return;
//	}
	XFrame cframe = vlf.context;
	long uid = cframe.getUid();
	if (uid != vlf.uid) {
	    errors.error(Errors.EXEC,pos,"function context no longer exists"+cframe.uid+","+vlf.uid);
	    ok = false;
	}

	XDOMElement funcTree = vlf.def.ftree;
	int formals = vlf.def.args;
	int assert1 = vlf.def.assert1;
	int extraArg = 0;
	if (ctx == value.ctxAssign) {
	    lhs = true;
	    if (vlf.def instanceof XDefFunc2) {
		XDefFunc2 def2 = (XDefFunc2) vlf.def;
		assert1 = def2.lhsAssert1;
		funcTree = def2.lhsFtree;
		formals = def2.lhsArgs;
		Object oval = stack.getOval(top);
		long ival = stack.getIval(top);
		stack.push(oval,ival); // push rhs val
		if (def2.lhsVarArgs) {
		    Object oval1 = stack.getOval(argIdx);
		    XDOMElement vtree = (XDOMElement) oval1;
		    vtree.setEArray(vtree.arraySize()+1,stack.getTopOval(),stack.getTopIval());
		    stack.pop();
		} else {
		    extraArg = 1;
		}
	    } else {
		errors.error(Errors.EXEC,pos,"can't assign to this expression");
	    }
	}

	if (extCnt != vlf.def.exts) {
	    errors.error(Errors.EXEC,pos,"call has wrong number of exts:"+
			 extCnt+" (expect "+vlf.def.exts+")");
	    ok = false;
	}

	if (argCnt + extraArg != formals) {
	    errors.error(Errors.EXEC,pos,
			 "call to "+vlf.def.name+
			 " has wrong number of actuals:"+
			 (argCnt+extraArg)+" (expect "+formals+")");
	    ok = false;
	}

	if (ok) {
	    boolean fail = false;
	    frames.push(vlf,extCnt+argCnt+extraArg,funcTree,caller);
	    if (option.check) {
		for (int i = 0; i < extCnt; i++) {
		    Object oval1 = stack.getOval(extIdx+i);
		    long ival1 = stack.getIval(extIdx+i);
		    if (! exec.checkExt(oval1,ival1,i,funcTree,pos)) {
			fail = true;
		    }
		}
		for (int i = 0; i < argCnt+extraArg; i++) {
		    Object oval1 = stack.getOval(argIdx+i);
		    long ival1 = stack.getIval(argIdx+i);
		    if (! exec.checkArg(oval1,ival1,i,funcTree,pos)) {
			fail = true;
		    }
		}
		if (! fail && assert1 != -1) {
		    XDOMCall xc = (XDOMCall) funcTree.getAttr(assert1);
		    if (! exec.opExec.execAssert(xc,pos)) {
			fail = true;
		    }
		    stack.pop();
		}
	    }
	    if (fail) {
		if (ctx != value.ctxAssign) stack.pushError();
	    } else if (exec.debug) {
		exec.execBodyLang(funcTree,! funcTree.getNoStep(),ctx,vlf.lang);
	    } else {
		exec.execBodyLang(funcTree,true,ctx,vlf.lang);
	    }
	    if (! fail & ! (ctx instanceof CtxStream) && option.check) {
		// when streaming there is no result left to check! 
		if (! exec.checkResult(funcTree)) {
		    if (ctx != value.ctxAssign) {
			stack.pop();
			stack.pushError();
		    }
		}
	    }
	    frames.pop(true);
	    if (lhs) {
		stack.pop(); // discard lhs func result 
	    }
	} else {
	    stack.setTop(top);
	    if (! lhs) stack.pushError();
	}
    }

    public XDef findCallObject(VLView vlv,String name,boolean hasExt,XDOM caller) {
	XPos pos = null;
	if (caller != null) pos = caller.pos;
	XScope scope = vlv.def.self.scope;
	XDef def = scope.find(name,hasExt);
	if (def == null) {
	    XScope scope1 = vlv.def.vtree.scope;
	    def = scope1.find(name,hasExt);
	}
	if (def == null) {
	    def = scope.find("Dot",true);
	}
	if (def != null) {
	    if (def.local) {
		XScope hideScope = vlv.def.scope;
		XScope sc = frames.getTopFrame().scope;
		while (sc != null && sc.level > hideScope.level) {
		    sc = sc.parent;
		}
		if (sc != hideScope) {
		    if (caller != null) {
			String ext = "";
			if (hasExt) ext="[...]";
			errors.error(Errors.EXEC,pos,"self name "+name+ext+" is local");
		    }
		    return null;
		}
	    } else if (! def.visible) {
		XScope hideScope1 = vlv.def.vtree.scope;
		int level = 0;
		if (hideScope1 != null) level = hideScope1.level;
		XScope sc = frames.getTopFrame().scope;
		while (sc != null && sc.level > level) {
		    sc = sc.parent;
		}
		if (sc != hideScope1) {
		    if (caller != null) {
			String ext = "";
			if (hasExt) ext="[...]";
			errors.error(Errors.EXEC,pos,"self name "+name+ext+" is not visible");
		    }
		    return null;
		}
	    }
	}
	if (def == null && caller != null) {
	    String ext = "";
	    if (hasExt) ext="[...]";
	    errors.error(Errors.EXEC,pos,"self name "+name+ext+" not found");
	}
	return def;
    }

    public void execCallObject(XDef def,
			       VLView vlv,Object oval,long ival,
			       boolean hasArg,
			       int extCnt,int argCnt,
			       XDOM caller,Ctx ctx) {
	XPos pos = null;
	if (caller != null) pos = caller.pos;
	if (def instanceof XDefFunc) {
	    XDefFunc fdef = (XDefFunc) def;
	    if (fdef.hasArgs && ! hasArg) {
		if (ctx == value.ctxAssign) {
		    errors.error(Errors.EXEC,pos,"can't assign to this expression");
		} else {
		    VLDot vlc = new VLDot();
		    int extIdx = stack.getTop() - extCnt + 1; // first ext 
		    int top = extIdx - 1;	  	      // top before call
		    vlc.voval = vlv;
		    vlc.vival = 0;
		    vlc.f = def;
		    vlc.selfoval = oval;
		    vlc.selfival = ival;
		    vlc.hasSelf = true;
		    if (extCnt != 0) {
			vlc.eovals = new Object[extCnt];
			vlc.eivals = new long[extCnt];
			for (int i = 0; i < extCnt; i++) {
			    vlc.eovals[i] = stack.getOval(extIdx+i);
			    vlc.eivals[i] = stack.getIval(extIdx+i);
			}
		    }
		    stack.setTop(top);
		    stack.push(vlc,0);
		}
	    } else if (vlv instanceof VLType) {
		VLType vlt = (VLType) vlv;
		VLObj vlo = (VLObj) oval;
		if (vlo.type == vlt) {
		    XDefName ndef = (XDefName) def;
		    XFrame old = frames.pushFrame(vlo.frame);
		    frames.getDefVal(ndef,pos);
		    Object oval1 = stack.getTopOval();
		    long ival1 = stack.getTopIval();
		    VLFunc vlf = (VLFunc) oval1;
		    stack.pop();
		    execCallFunc(vlf,extCnt,argCnt,caller,ctx);
		    frames.popFrame(false,old);
		} else {
		    errors.error(Errors.INTERNAL,pos,
				 "execCallObject object does not have specified type");
		    stack.setTop(stack.getTop()-extCnt-argCnt);
		    if (ctx != value.ctxAssign) {
			stack.pushError();
		    }
		}
	    } else {
		frames.pushSelf(vlv);
		XFrame f = frames.getTopFrame();
		// set self value
		f.stack.setVal(f.base,oval,ival);
		Object saveLang = xt.currentLang;
		exec.execBodyLang(vlv.def.self,true,ctx,vlv.lang);
		stack.pop(); // ignore self result 
		frames.getDefVal((XDefName) def,pos);
		VLFunc vlf = (VLFunc) stack.getTopOval();
		stack.pop();
		execCallFunc(vlf,extCnt,argCnt,caller,ctx);
		frames.pop(false);
	    }
	} else if (def instanceof XDefName) {
	    XDefName ndef = (XDefName) def;
	    if (vlv instanceof VLType) {
		VLObj vlo = (VLObj) oval;
		exec.execNameDef(vlo.frame,ndef,caller,ctx);
	    } else {
		frames.pushSelf(vlv);
		XFrame f = frames.getTopFrame();
		// set self value
		f.stack.setVal(f.base,oval,ival);
		exec.execBodyLang(vlv.def.self,true,ctx,vlv.lang);
		stack.pop(); // ignore self result 
		exec.execNameDef(f,ndef,caller,ctx);
		frames.pop(false);
	    }
	} else {
	    if (ctx != value.ctxAssign) stack.pushError();
	    errors.error(Errors.INTERNAL,pos,"execCallObject: bad def");
	}
    }

    public XDef findCallView(VLView vlv,String name,boolean hasExt,XDOM caller) {
	XPos pos = null;
	if (caller != null) pos = caller.pos;
	XScope scope = vlv.def.vtree.scope;
	XDef def = scope.find(name,hasExt);
	if (def == null) {
	    def = scope.find("Dot",true);
	}
	if (def != null) {
	    if (def.local) {
		XScope hideScope = vlv.def.scope;
		XScope sc = frames.getTopFrame().scope;
		while (sc != null && sc.level > hideScope.level) {
		    sc = sc.parent;
		}
		if (sc != hideScope) {
		    if (caller != null) {
			String ext = "";
			if (hasExt) ext="[...]";
			if (vlv instanceof VLType) {
			    errors.error(Errors.EXEC,pos,"type name "+name+ext+" is local");
			} else {
			    errors.error(Errors.EXEC,pos,"view name "+name+ext+" is local");
			}
		    }
		    return null;
		}
	    } else if (! def.visible) {
		XScope hideScope1 = vlv.def.vtree.scope;
		int level = 0;
		if (hideScope1 != null) level = hideScope1.level;
		XScope sc = frames.getTopFrame().scope;
		while (sc != null && sc.level > level) {
		    sc = sc.parent;
		}
		if (sc != hideScope1) {
		    if (caller != null) {
			String ext = "";
			if (hasExt) ext="[...]";
			if (vlv instanceof VLType) {
			    errors.error(Errors.EXEC,pos,"type name "+name+ext+" is not visible");
			} else {
			    errors.error(Errors.EXEC,pos,"view name "+name+ext+" is not visible");
			}
		    }
		    return null;
		}
	    }
	}
	if (def == null && caller != null) {
	    if (vlv instanceof VLType) {
		errors.error(Errors.EXEC,pos,"type name "+name+" not found");
	    } else {
		errors.error(Errors.EXEC,pos,"view name "+name+" not found");
	    }
	}
	return def;
    }

    public void execCallView(XDef def,VLView vlv,
			     boolean hasArg,
			     int extCnt,int argCnt,
			     XDOM caller,Ctx ctx) {
	XPos pos = null;
	if (caller != null) pos = caller.pos;
	if (def instanceof XDefFunc) {
	    XDefFunc fdef = (XDefFunc) def;
	    if (fdef.hasArgs && ! hasArg) {
		if (ctx == value.ctxAssign) {
		    errors.error(Errors.EXEC,pos,"can't assign to this expression");
		} else {
		    VLDot vlc = new VLDot();
		    int extIdx = stack.getTop() - extCnt + 1; // first ext
		    int top = extIdx - 1;		      // top before call
		    vlc.voval = vlv;
		    vlc.vival = 0;
		    vlc.f = def;
		    vlc.selfoval = null;
		    vlc.selfival = 0;
		    vlc.hasSelf = false;
		    if (extCnt != 0) {
			vlc.eovals = new Object[extCnt];
			vlc.eivals = new long[extCnt];
			for (int i = 0; i < extCnt; i++) {
			    vlc.eovals[i] = stack.getOval(extIdx+i);
			    vlc.eivals[i] = stack.getIval(extIdx+i);
			}
		    }
		    stack.setTop(top);
		    stack.push(vlc,0);
		}
	    } else {
		XFrame old = frames.pushFrame(vlv.frame);
		frames.getDefVal((XDefName) def,pos);
		VLFunc vlf = (VLFunc) stack.getTopOval();
		stack.pop();
		execCallFunc(vlf,extCnt,argCnt,caller,ctx);
		frames.popFrame(false,old);
	    }
	} else if (def instanceof XDefName) {
	    XDefName ndef = (XDefName) def;
	    exec.execNameDef(vlv.frame,ndef,caller,ctx);
	} else {
	    if (ctx != value.ctxAssign) stack.pushError();
	    errors.error(Errors.INTERNAL,pos,"execCallView: bad def");
	}
    }

    public int dotExt(XDef def,String name) {
	if (def.name != "Dot") return 0;
	if (name == "Dot") return 0;
	stack.push(name,0);
	return 1;
    }
    
    private boolean execDotCall2(XDOMCall ctree,XDOMCall tree,Ctx ctx) {
	XDOMName extTree = null;
	boolean hasExt = false;
	if (ctree.getFunc() instanceof XDOMName) {
	    extTree = (XDOMName) ctree.getFunc();
	    if (extTree.hasExt()) hasExt = true;
	}
	XDOMName ntree = (XDOMName) ctree.getArg(1);
	nameVal(ntree);
	Object oval = stack.getTopOval();
	long ival = stack.getTopIval();
	stack.pop();
	XDOMString stree = (XDOMString) ctree.getArg(2);
	String op = stree.getVal();
	if (oval instanceof VLBIV) {
	    if (ctx == value.ctxAssign) {
		return false;
	    }
	    if (hasExt) {
		return false;
	    }
	    VLBIF vlop = biExec.findOp((int) ival,op);
	    if (vlop != null) {
		biExec.execBICall1(vlop,tree); return true;
	    }
	} else if (oval instanceof VLView) {
	    exec.execExp(ntree,value.ctxEval); // to force instantiate view !
	    oval = stack.getTopOval();
	    stack.pop();
	    VLView vlv = (VLView) oval;
	    XDef def = findCallView(vlv,op,hasExt,null);
	    if (def != null) {
		int ecnt = dotExt(def,op);
		if (def instanceof XDefFunc) {
		    XDefFunc fdef = (XDefFunc) def;
		    if (hasExt) {
			ecnt += pushExt(fdef,extTree);
		    }
		    if (fdef.hasArgs) {
			execCallView(def,vlv,true,
				     ecnt,pushArg(fdef,tree),tree,ctx);
			return true;
		    }
		}
		execCallView(def,vlv,false,
			     ecnt,0,tree,ctx);
		Object oval1 = stack.getTopOval();
		long ival1 = stack.getTopIval();
		stack.pop();
		execCall1(oval1,ival1,tree,ctx);
		return true;
	    }
	} else if (oval instanceof VLObj) {
	    VLObj vlo = (VLObj) oval;
	    int ecnt = 0;
	    XDef def = findCallObject(vlo.type,op,hasExt,null);
	    if (def != null) {
		if (def instanceof XDefFunc) {
		    XDefFunc fdef = (XDefFunc) def;
		    if (hasExt) {
			ecnt = pushExt(fdef,extTree);
		    }
		    if (fdef.hasArgs) {
			execCallObject(def,vlo.type,vlo,0,true,
				       ecnt,pushArg(fdef,tree),
				       tree,ctx);
			return true;
		    }
		} 
		execCallObject(def,vlo.type,vlo,0,false,
			       ecnt,0,
			       tree,value.ctxEval);
		Object oval1 = stack.getTopOval();
		long ival1 = stack.getTopIval();
		stack.pop();
		execCall1(oval1,ival1,tree,ctx);
		return true;
	    }
	} else if (oval == value.vlJava) {
	    if (hasExt) {
	    } else if (ctx != value.ctxEval) {
	    } else {
		jExec.newCall(op,pushArg(tree),tree);
		return true;
	    }
	} else if (oval instanceof Class) {
	    if (hasExt) {
	    } else if (ctx != value.ctxEval) {
	    } else {
		Class c = (Class) oval;
		jExec.jcall(null,c,op,pushArg(tree),tree);
		return true;
	    }
	} else if (value.isJObject(oval,ival)) {
	    if (hasExt) {
	    } else if (ctx != value.ctxEval) {
	    } else {
		jExec.jcall(oval,oval.getClass(),op,pushArg(tree),tree);
		return true;
	    }
	} else {
	    VLBIF vlf = biExec.findObjOp(op,oval,ival,null);
	    if (vlf != null) {
		biExec.callObjOp(vlf,oval,ival,tree,ctx);
		return true;
	    }
	}
	return false;
    }

    private boolean execDotCall3(XDOMCall ctree,XDOMCall tree,Ctx ctx) {
	XDOMName extTree = null;
	boolean hasExt = false;
	if (ctree.getFunc() instanceof XDOMName) {
	    extTree = (XDOMName) ctree.getFunc();
	    if (extTree.hasExt()) hasExt = true;
	}
	XDOM obj = ctree.getArg(1);
	XDOMString stree = (XDOMString) ctree.getArg(2);
	String name = stree.getVal();
	XDOMName ntree = (XDOMName) ctree.getArg(3);
	nameVal(ntree);
	Object oval = stack.getTopOval();
	long ival = stack.getTopIval();
	stack.pop();
	if (oval instanceof VLBIV) {
	    if (hasExt) {
	    } else {
		exec.execExp(obj,value.ctxEval);
		Object oval1 = stack.getTopOval();
		long ival1 = stack.getTopIval();
		stack.pop();
		VLBIF vlf = biExec.findObjOp((int) ival,name,oval1,ival1,null);
		if (vlf != null) {
		    biExec.callObjOp(vlf,oval1,ival1,tree,ctx);
		    return true;
		}
	    }
	} else if (oval instanceof VLView) {
	    exec.execExp(ntree,value.ctxEval); // to force instantiate view !
	    oval = stack.getTopOval();
	    stack.pop();
	    VLView vlv = (VLView) oval;
	    exec.execExp(obj,value.ctxEval);
	    oval = stack.getTopOval();
	    ival = stack.getTopIval();
	    stack.pop();
	    int ecnt = 0;
	    XDef def = findCallObject(vlv,name,hasExt,null);
	    if (def != null) {
		if (def instanceof XDefFunc) {
		    XDefFunc fdef = (XDefFunc) def;
		    if (hasExt) {
			ecnt = pushExt(fdef,extTree);
		    }
		    if (fdef.hasArgs) {
			execCallObject(def,vlv,oval,ival,true,
				       ecnt,pushArg(fdef,tree),
				       tree,ctx);
			return true;
		    }
		}
		execCallObject(def,vlv,oval,ival,false,
			       ecnt,0,
			       tree,value.ctxEval);
		Object oval1 = stack.getTopOval();
		long ival1 = stack.getTopIval();
		stack.pop();
		execCall1(oval1,ival1,tree,ctx);
		return true;
	    }
	}
	return false;
    }

    public void execMake(VLType vlt,XDOMCall ctree,Object oval,long ival) {
	VLObj vlo = new VLObj();
	XFrame f = new XFrame();
	vlo.type = vlt;
	vlo.frame = f;
	f.uid = -1; // use parent 
	f.slink = vlt.frame;
	f.stack = new FixedStack(vlt.def.self.scope.size);
	//f.stack.setTop(0);
	f.base = 0;
	f.scope = vlt.def.self.scope;
	f.caller = null;
	f.level = vlt.def.self.scope.level;
	f.size = vlt.def.self.scope.size;
	XFrame old = frames.pushFrame(f);
	f.caller = ctree;
	// set self
	f.stack.setVal(f.base,vlo,0);
	exec.execBodyLang(vlt.def.self,true,value.ctxEval,vlt.lang);
	stack.pop();	
	frames.popFrame(false,old);
	f.caller = null;
	XDef def = findCallObject(vlt,"sys:Init",false,null);
	boolean ok = false;
	if (oval != null || ctree.argSize() != 0 || def != null) {
	    if (def instanceof XDefFunc) {
		XDefFunc fdef = (XDefFunc) def;
		if (fdef.hasArgs) {
		    int cnt = 1;
		    if (oval != null) {
			stack.push(oval,ival);
		    } else {
			cnt = pushArg(fdef,ctree);
		    }
		    execCallObject(def,vlt,vlo,0,true,
				   0,cnt,ctree,value.ctxEval);
		    stack.pop();
		    ok = true;
		}
	    }
	} else if (ctree.argSize() == 0) {
	    ok = true;
	}
	if (! ok) {
	    errors.error(Errors.EXEC,ctree.pos,"missing sys:Init func");
	}
	stack.push(vlo,0);
    }

    private void execCall1(Object oval,long ival,XDOMCall ctree,Ctx ctx) {
	if (oval instanceof VLBIF) {
	    VLBIF vlf = (VLBIF) oval;
	    biExec.execBICall(vlf,ctree,ctx);
	} else if (oval instanceof VLFunc) {
	    VLFunc vlf = (VLFunc) oval;
	    execCallFunc(vlf,0,pushArg(vlf.def,ctree),ctree,ctx);
	} else if (oval instanceof VLExt) {
	    VLExt vle = (VLExt) oval;
	    Object foval = vle.foval;
	    if (foval instanceof VLFunc) {
		VLFunc vlf = (VLFunc) foval;
		int cnt = vle.eovals.length;
		for (int i =  0; i < cnt; i++) {
		    stack.push(vle.eovals[i],vle.eivals[i]);
		}
		execCallFunc(vlf,cnt,pushArg(vlf.def,ctree),ctree,ctx);
	    } else if (foval instanceof VLBIF) {
		VLBIF vlf = (VLBIF) foval;
		biExec.execExt(vlf,vle,ctree,ctx);
	    }
	} else if (oval instanceof VLDot) {
	    VLDot vlc = (VLDot) oval;
	    if (vlc.hasSelf) {
		if (vlc.voval instanceof VLType) {
		    VLType vlt = (VLType) vlc.voval;
		    VLObj vlo = (VLObj) vlc.selfoval;
		    if (vlo.type == vlt) {
			XDefFunc ndef = (XDefFunc) vlc.f;
			int cnt = 0;
			if (vlc.eovals != null) {
			    cnt = vlc.eovals.length;
			    for (int i =  0; i < cnt; i++) {
				stack.push(vlc.eovals[i],vlc.eivals[i]);
			    }
			}
			int args = pushArg(ndef,ctree);
			XFrame old = frames.pushFrame(vlo.frame);
			frames.getDefVal(ndef,ctree.pos);
			Object oval1 = stack.getTopOval();
			long ival1 = stack.getTopIval();
			VLFunc vlf = (VLFunc) oval1;
			stack.pop();
			execCallFunc(vlf,cnt,args,ctree,ctx);
			frames.popFrame(false,old);
		    } else {
			errors.error(Errors.INTERNAL,ctree.pos,
				     "execCallObject dot object does not have specified type");
			if (ctx != value.ctxAssign) stack.pushError();
		    }
		} else if (vlc.voval instanceof VLView) {
		    VLView vlv = (VLView) vlc.voval;
		    XDefFunc ndef = (XDefFunc) vlc.f;
		    int cnt = 0;
		    if (vlc.eovals != null) {
			cnt = vlc.eovals.length;
			for (int i =  0; i < cnt; i++) {
			    stack.push(vlc.eovals[i],vlc.eivals[i]);
			}
		    }
		    int args = pushArg(ndef,ctree);
		    frames.pushSelf(vlv);
		    XFrame f = frames.getTopFrame();
		    // set self value 
		    f.stack.setVal(f.base,vlc.selfoval,vlc.selfival);
		    exec.execBodyLang(vlv.def.self,true,ctx,vlv.lang);
		    stack.pop(); // ignore self result
		    frames.getDefVal((XDefName) vlc.f,ctree.pos);
		    VLFunc vlf = (VLFunc) stack.getTopOval();
		    stack.pop();
		    execCallFunc(vlf,cnt,args,ctree,ctx);
		    frames.pop(false);
		} else if (vlc.voval == value.vlBIV) {
		    VLBIF vlf = (VLBIF) vlc.f;
		    biExec.callObjOp(vlf,vlc.selfoval,vlc.selfival,ctree,ctx);
		}
	    } else {
		VLView vlv = (VLView) vlc.voval;
		XDefFunc ndef = (XDefFunc) vlc.f;
		int cnt = 0;
		if (vlc.eovals != null) {
		    cnt = vlc.eovals.length;
		    for (int i = 0; i < cnt; i++) {
			stack.push(vlc.eovals[i],vlc.eivals[i]);
		    }
		}
		int args = pushArg(ndef,ctree);
		XFrame old = frames.pushFrame(vlv.frame);
		frames.getDefVal((XDefName) vlc.f,ctree.pos);
		VLFunc vlf = (VLFunc) stack.getTopOval();
		stack.pop();
		execCallFunc(vlf,cnt,args,ctree,ctx);
		frames.popFrame(false,old);
	    }
	} else if (oval instanceof VLBIV) {
	    if (ctx == value.ctxAssign) {
		errors.error(Errors.EXEC,ctree.pos,"assignment to a constructor not permitted");
	    } else {
		biExec.execMake(ival,ctree,ctx);
	    }
	} else if (oval instanceof VLType) {
	    if (ctx == value.ctxAssign) {
		errors.error(Errors.EXEC,ctree.pos,"assignment to a constructor not permitted");
	    } else {
		VLType vlt = (VLType) oval;
		execMake(vlt,ctree,null,0);
	    }
	} else if (oval instanceof VLView) {
	    if (ctx == value.ctxAssign) {
		errors.error(Errors.EXEC,ctree.pos,"assignment to a view constructor not permitted");
	    } else {
		boolean ok = false;
		VLView vlv = (VLView) oval;
		XDef def = findCallView(vlv,"sys:Make",false,null);
		if (def instanceof XDefFunc) {
		    XDefFunc fdef = (XDefFunc) def;
		    if (fdef.hasArgs) {
			execCallView(def,vlv,true,
				     0,pushArg(fdef,ctree),ctree,value.ctxEval);
			ok = true;
		    }
		}
		if (! ok) {
		    errors.error(Errors.EXEC,ctree.pos,"missing sys:Make func");
		    stack.pushError();
		}
	    }
	} else if (value.isJObject(oval,ival) && oval instanceof Class) {
	    Class c = (Class) oval;
	    if (ctx == value.ctxAssign) {
		errors.error(Errors.EXEC,ctree.pos,"can't assign to Java constructor");
	    } else {
		jExec.newCall1(c,pushArg(ctree),ctree);
	    }
	} else if (oval instanceof VLJMethod) {
	    VLJMethod vlm = (VLJMethod) oval;
	    if (ctx == value.ctxAssign) {
		errors.error(Errors.EXEC,ctree.pos,"can't assign to this expression");
	    } else {
		if (vlm.obj instanceof Class) {
		    jExec.jcall(null,(Class) vlm.obj,vlm.name,pushArg(ctree),ctree);
		} else {
		    jExec.jcall(vlm.obj,vlm.obj.getClass(),vlm.name,pushArg(ctree),ctree);
		}
	    }
	} else if (value.isError(oval,ival)) {
	    if (ctx != value.ctxAssign) {
		stack.pushError();
	    }
	} else {

	    biExec.execSubscript(oval,ival,ctree,ctx);
	}
    }

    public void execCall(XDOMCall ctree,Ctx ctx) {
	XDOM func = ctree.getFunc();
	if (func instanceof XDOMName) {
	    XDOMName ntree = (XDOMName) func;
	    nameVal(ntree);
	    Object oval = stack.getTopOval();
	    long ival = stack.getTopIval();
	    stack.pop();
	    if (option.optimize && oval instanceof VLFunc) {
		VLFunc vlf = (VLFunc) oval;
		if (vlf.def.hasArgs) {
		    execCallFunc(vlf,pushExt(vlf.def,ntree),
				 pushArg(vlf.def,ctree),ctree,ctx);
		    return;
		}
	    } else if (oval instanceof VLBIF) {
		VLBIF vlf = (VLBIF) oval;
		if (option.optimize || vlf.kind == VLBIF.dot) {
		    if (vlf.hasArgs) {
			biExec.execBICall(vlf,ctree,ctx);
			return;
		    }
		}
	    }
	} else if (option.optimize && func instanceof XDOMCall) {
	    XDOMCall ctree1 = (XDOMCall) func;
	    int size1 = ctree1.argSize();
	    if (ctree1.getFunc() instanceof XDOMName) {
		XDOMName ntree = (XDOMName) ctree1.getFunc();
		String name = ntree.getName();
		if (name == "Dot") {
		    if (size1 == 2) {
			if (ctree1.getArg(1) instanceof XDOMName &&
			    ctree1.getArg(2) instanceof XDOMString) {
			    if (execDotCall2(ctree1,ctree,ctx)) return;
			}


		    } else if (size1 == 3) {
			if (ctree1.getArg(2) instanceof XDOMString &&
			    ctree1.getArg(3) instanceof XDOMName) {
			    if (execDotCall3(ctree1,ctree,ctx)) return;
			}
		    }
		} else if (name == "Range" && size1 == 2 && ctree.argSize() == 1) {
		    exec.opExec.execRangeOpt(ctree,ctx);
		    return;
		}
	    }
	}

	exec.execExp(func,value.ctxEval);
	Object oval = stack.getTopOval();
	long ival = stack.getTopIval();
	stack.pop();
	execCall1(oval,ival,ctree,ctx);
    }

    public boolean execNameCall(XDOMName ntree,Ctx ctx) {
	Object oval  = stack.getTopOval();
	long ival = stack.getTopIval();
	Object foval = null;
	XDefFunc def = null;
	if (oval instanceof VLBIF) {
	    VLBIF vlf = (VLBIF) oval;
//	    if (ntree.hasExt()) {
	    if (! vlf.hasArgs) {
		stack.pop();
		biExec.execBIName(vlf,ntree,ctx);
		return true;
	    }
	    foval = vlf;
	} else if (oval instanceof VLFunc) {
	    VLFunc vlf = (VLFunc) oval;
	    def = vlf.def;
	    if (! def.hasArgs) {
		stack.pop();
		execCallFunc(vlf,pushExt(vlf.def,ntree),0,ntree,ctx);
		return true;
	    }
	    foval = vlf;
	} else if (oval instanceof VLView) {
	    if (ctx != value.ctxAssign) {
		VLView vlv = (VLView) oval;
		stack.pop();
		Object saveLang = xt.currentLang;
		xt.currentLang = vlv.lang;
		exec.makeView(vlv,ntree);
		xt.currentLang = saveLang;
		return true;
	    }
	} else {
	    return false;
	}
	if (ctx == value.ctxAssign) return false;
	if (ntree.hasExt() && def != null) {
	    stack.pop();
	    VLExt vle = new VLExt();
	    vle.foval = foval;
	    vle.fival = 0;
	    int top = stack.getTop();
	    int cnt = pushExt(def,ntree);
	    vle.eovals = new Object[cnt];
	    vle.eivals = new long[cnt];
	    for (int i = 0; i < cnt; i++) {
		Object oval1 = stack.getOval(top+i+1);
		long ival1 = stack.getIval(top+i+1);
		vle.eovals[i] = oval1;
		vle.eivals[i] = ival1;
	    }
	    stack.setTop(top);
	    stack.push(vle,0);
	}
	return true;
    }

}
