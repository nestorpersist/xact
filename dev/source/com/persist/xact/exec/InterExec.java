/*******************************************************************************
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
import com.persist.xact.parse.*;

public final class InterExec {

    public static final byte PNONE = 0;
    public static final byte PBIND1 = 1;
    public static final byte PBIND2 = 2;
    public static final byte PEXEC = 4;

    public byte pass = PNONE;

    private XThread xt;
    private Exec exec;
    private Parser parser;
    private VariableStack stack;
    private Errors errors;
    private Value value;
    private EValue evalue;
    private CallExec callExec;
    private XOption option;

    public InterExec(Exec exec) {
	this.exec = exec;
    }

    public void init() {
	xt = exec.xt;
	parser = xt.parser;
	stack = exec.stack;
	errors = exec.xt.errors;
	value = exec.xt.cmd.value;
	evalue = exec.evalue;
	callExec = exec.callExec;
	option = xt.cmd.option;
    }

    /*************************************************************************
    *
    *   Built-in Language Object Constructors
    *
    *************************************************************************/

    public void makeXact(XDOMCall tree) {
	int size = tree.argSize();
	if (size == 1) {
	    XDOM arg1 = tree.getArg(1);
	    exec.execExp(arg1,value.ctxEval);
	    Object oval = stack.getTopOval();
	    long ival = stack.getTopIval();
	    stack.pop();
	    if (oval instanceof VLObj || oval instanceof VLL) {
		VLLxact vlx = new VLLxact(oval);
		stack.push(vlx,0);
	    } else {
		errors.error(Errors.EXEC,arg1.pos,"not a language object");
		stack.pushError();
	    }
	} else {
	    errors.error(Errors.EXEC,tree.pos,"wrong number of actual parameters");
	    stack.pushError();
	}
    }

    public void makeRender(XDOMCall tree) {
	int size = tree.argSize();
	if (size == 1) {
	    XDOM arg1 = tree.getArg(1);
	    if (evalue.getS(arg1)) {
		Object oval1 = stack.getTopOval();
		long ival1 = stack.getTopIval();
		stack.pop();
		String kind = XDOMValue.getString(oval1,ival1);
		byte k = VLLrender.findKind(kind);
		if (k != -1) {
		    VLLrender vlr = new VLLrender(k);
		    stack.push(vlr,0);
		} else {
		    errors.error(Errors.EXEC,tree.pos,"bad render kind: "+kind);
		    stack.pushError();
		}
	    } else {
		stack.pushError();
	    }
	} else {
	    errors.error(Errors.EXEC,tree.pos,"wrong number of actual parameters");
	    stack.pushError();
	}
    }

    public void makeError(XDOMCall tree) {
	int size = tree.argSize();
	if (size == 0) {
	    VLLerror vle = new VLLerror();
	    stack.push(vle,0);
	} else {
	    errors.error(Errors.EXEC,tree.pos,"wrong number of actual parameters");
	    stack.pushError();
	}
    }
    
    /*************************************************************************
    *
    *   Parse
    *
    *************************************************************************/

    public void execParse(XDOMCall tree) {
	int args = tree.argSize();
	if (args >= 1) {
	    XDOM arg1 = tree.getArg(1);
	    VLObj vlo = null;
	    FPosition fpos1 = tree.pos.fpos;
	    boolean sawHandler = false;
	    boolean sawName = false;
	    for (int i = 2; i <= args; i++) {
		XDOM arg = tree.getArg(i);
		if (TreeUtil.isEqual(arg)) {
		    String name = TreeUtil.getEqualName(arg);
		    XDOM x = TreeUtil.stripEqual(arg);
		    if (name == "handler") {
			if (sawHandler) {
			    errors.error(Errors.EXEC,tree.pos,"multiple handler= parameters");
			} else {
			    sawHandler = true;
			    exec.execExp(x,value.ctxEval);
			    Object oval = stack.getTopOval();
			    long ival = stack.getTopIval();
			    stack.pop();
			    if (oval instanceof VLObj) {
				vlo = (VLObj) oval;
			    } else {
				errors.error(Errors.EXEC,x.pos,"not an object");
			    }
			}
		    } else if (name == "fileName" || name == "scriptName" || name == "name") {
			if (sawName) {
			    errors.error(Errors.EXEC,tree.pos,"multiple name parameters");
			} else {
			    sawName = true;
			    evalue.getS(x);
			    Object oval1 = stack.getTopOval();
			    long ival1 = stack.getTopIval();
			    stack.pop();
			    String sname = XDOMValue.getString(oval1,ival1);
			    byte kind = FPosition.FOTHER;
			    if (name == "fileName") {
				kind = FPosition.FFILE;
			    } else if (name == "scriptName") {
				kind = FPosition.FSCRIPT;
			    }
			    fpos1 = new FPosition(kind,sname);
			}
		    } else {
			errors.error(Errors.EXEC,tree.pos,"unrecognized parameter name");
		    }
		} else {
		    errors.error(Errors.EXEC,tree.pos,"missing name=");
		}
	    }
	    final FPosition fpos = fpos1;
	    if (option.optimize) {
		// full streamed version
		final XThread xt1 = new XThread("Parse",xt.cmd);
		xt1.parent = xt;
		xt1.parentFrame = exec.frames.getTop();
		xt1.errors = xt.errors;
		VLQueue vlq = new VLQueue(1);
		XDOMElement xe;
		final XR r = new XRQ(vlq);
		final VLObj vlo1 = vlo;
		final XDOMCall tree1 = tree;
		XW w = new XWQ(vlq);
		CtxStream ctxs = new CtxStream(w);

		Thread t = new Thread(new Runnable() {
		    public void run() {
			try {
			    xt1.parseResult = xt1.parser.parse(r,true,fpos,vlo1,tree1);
			} catch(Throwable e) {
			    String s = e.getMessage();
			    System.out.println("parse thread fail:"+xt1.name+"="+e);
			    xt.errors.fail(e,xt1.name);
			}
		    }
		});
		t.start();
		if (evalue.getS(arg1,ctxs)) {
		    Object oval = stack.getTopOval();
		    long ival = stack.getTopIval();
		    String val = XDOMValue.getString(oval,ival);
		    stack.pop();
		    w.write(val);
		} else {
		    stack.pop();
		}
		ctxs.sendSpace();
		w.close();
		try {
		    t.join();
		} catch(Exception e) {
		}
		stack.push(xt1.parseResult,0);
	    } else {
		// basic non-streamed version 
		if (evalue.getS(arg1)) {
		    Object oval = stack.getTopOval();
		    long ival = stack.getTopIval();
		    String val = XDOMValue.getString(oval,ival);
		    stack.pop();
		    XR r = new XRString(val);
		    Parser parser1 = parser;
		    if (parser1.inUse) {
			parser1 = new Parser(xt);
			parser1.init();
		    }
		    XDOMElement xe = parser1.parse(r,true,fpos,vlo,tree);
		    stack.push(xe,0);
		} else {
		    stack.pop();
		}
	    }
	} else {
	    errors.error(Errors.EXEC,tree.pos,"wrong number of actual parameters");
	    stack.pushError();
	}
    }

    /*************************************************************************
    *
    *   Bind
    *
    *************************************************************************/

    public void execBind(XDOMCall tree) {
	int args = tree.argSize();
	if (args > 0) {
	    boolean ok = true;
	    XDOM itree = null;
	    XDOM arg1 = tree.getArg(1);
	    exec.execExp(arg1,value.ctxEval);
	    Object oval = stack.getTopOval();
	    long ival = stack.getTopIval();
	    stack.pop();
	    if (oval instanceof XDOM) {
		itree = (XDOM) oval;
	    } else {
		ok = false;
		if (! value.isError(oval,ival)) {
		    errors.error(Errors.EXEC,arg1.pos,"not a tree");
		}
	    }
	    Object lang = xt.currentLang;
	    for (int i = 2; i <= args; i++) {
		XDOM arg = tree.getArg(i);
		if (TreeUtil.isEqual(arg)) {
		    String name = TreeUtil.getEqualName(arg);
		    XDOM x = TreeUtil.stripEqual(arg);
		    exec.execExp(x,value.ctxEval);
		    Object oval1 = stack.getTopOval();
		    long ival1 = stack.getTopIval();
		    stack.pop();
		    if (name == "language") {
			lang = oval1;
		    } else {
			errors.error(Errors.EXEC,arg.pos,"unrecognized keyword parameter");
		    }
		} else {
		    errors.error(Errors.EXEC,arg.pos,"not a keyword parameter");
		}
	    }
	    if (ok) {
		xt.bind.bind(itree,0,null,lang);
	    }
	} else {
	    errors.error(Errors.EXEC,tree.pos,"wrong number of actual parameters");
	}
	stack.pushNull();
    }

    /*************************************************************************
    *
    *   Exec
    *
    *************************************************************************/

    public void execExec(XDOMCall tree,Ctx ctx) {
	int args = tree.argSize();
	if (args > 0) {
	    boolean ok = true;
	    XDOM itree = null;
	    XDOM arg1 = tree.getArg(1);
	    exec.execExp(arg1,value.ctxEval);
	    Object oval = stack.getTopOval();
	    long ival = stack.getTopIval();
	    stack.pop();
	    if (oval instanceof XDOM) {
		itree = (XDOM) oval;
	    } else {
		ok = false;
		if (! value.isError(oval,ival)) {
		    errors.error(Errors.EXEC,arg1.pos,"not a tree");
		}
	    }
	    Object lang = xt.currentLang;
	    for (int i = 2; i <= args; i++) {
		XDOM arg = tree.getArg(i);
		if (TreeUtil.isEqual(arg)) {
		    String name = TreeUtil.getEqualName(arg);
		    XDOM x = TreeUtil.stripEqual(arg);
		    exec.execExp(x,value.ctxEval);
		    Object oval1 = stack.getTopOval();
		    long ival1 = stack.getTopIval();
		    stack.pop();
		    if (name == "language") {
			lang = oval1;
		    } else {
			errors.error(Errors.EXEC,arg.pos,"unrecognized keyword parameter");
		    }
		} else {
		    errors.error(Errors.EXEC,arg.pos,"not a keyword parameter");
		}
	    }
	    if (ok) {
		xt.exec.exec(itree,ctx,lang);
		return;
	    }
	} else {
	    errors.error(Errors.EXEC,tree.pos,"wrong number of actual parameters");
	}
	if (ctx != value.ctxAssign) {
	    stack.pushNull();
	}
    }

    /*************************************************************************
    *
    *   Walk
    *
    *************************************************************************/

    public void walkOther(XDOMCall tree,Ctx ctx) {
	int args = tree.argSize();
	Layer layer = exec.layerStack.top();
	byte pass = layer.pass;
	if (pass == PNONE) {
	    errors.error(Errors.EXEC,tree.pos,"walk:other only availble during interpretation");
	} if (args == 5)  {
	    boolean ok = true;

	    Object otherLang = null;
	    XDOM arg1 = tree.getArg(1);
	    exec.execExp(arg1,value.ctxEval);
	    Object oval1 = stack.getTopOval();
	    long ival1 = stack.getTopIval();
	    stack.pop();
	    if (oval1 instanceof VLLxact || oval1 instanceof VLLrender || oval1 instanceof VLLerror ||
		  oval1 instanceof VLObj) {
		otherLang = oval1;
	    } else {
		if (! value.isError(oval1,ival1)) {
		    errors.error(Errors.EXEC,arg1.pos,"not a language object");
		}
		ok = false;
	    }
	    
	    String space = "";
	    XDOM arg2 = tree.getArg(2);
	    exec.execExp(arg2,value.ctxEval);
	    Object oval2 = stack.getTopOval();
	    long ival2 = stack.getTopIval();
	    if (evalue.getS(arg2.pos)) {
		space = XDOMValue.getString(oval2,ival2);
	    } else {
		ok = false;
	    }
	    stack.pop();

	    String base = "";
	    XDOM arg3 = tree.getArg(3);
	    exec.execExp(arg3,value.ctxEval);
	    Object oval3 = stack.getTopOval();
	    long ival3 = stack.getTopIval();
	    if (evalue.getS(arg3.pos)) {
		base = XDOMValue.getString(oval3,ival3);
	    } else {
		ok = false;
	    }
	    stack.pop();

	    XDOMElement elem = null;
	    XDOM arg4 = tree.getArg(4);
	    exec.execExp(arg4,value.ctxEval);
	    Object oval4 = stack.getTopOval();
	    long ival4 = stack.getTopIval();
	    stack.pop();
	    if (oval4 instanceof XDOMElement) {
		elem = (XDOMElement) oval4;
	    } else {
		ok = false;
		if (! value.isError(oval4,ival4)) {
		    errors.error(Errors.EXEC,arg1.pos,"not an xdom element");
		}
	    }

	    Object lang = null;
	    XDOM arg5 = tree.getArg(5);
	    exec.execExp(arg5,value.ctxEval);
	    Object oval5 = stack.getTopOval();
	    long ival5 = stack.getTopIval();
	    stack.pop();
	    if (oval5 instanceof VLLxact || oval5 instanceof VLLrender || oval5 instanceof VLLerror
		  || oval5 instanceof VLObj) {
		lang = oval5;
	    } else {
		if (! value.isError(oval1,ival1)) {
		    errors.error(Errors.EXEC,arg5.pos,"not a language object");
		}
		ok = false;
	    }

	    if (ok) {
		Object oldLang = xt.currentLang;
		xt.currentLang = lang;
		byte oldPass = pass;
		if (oldPass == PBIND1) {
		    xt.bind.bind1Lang(space,base,elem,otherLang);
		    stack.pushNull();
		} else if (oldPass == PBIND2) {
		    xt.bind.bind2Lang(space,base,elem,otherLang);
		    stack.pushNull();
		} else if (oldPass == PEXEC) {
		    XFrame oldSlink = exec.frames.getTopFrame();
		    exec.frames.setSlink(layer.slink);
		    exec.execCmdLang(space,base,elem,ctx,otherLang);
		    exec.frames.setSlink(oldSlink);
		}
		xt.currentLang = oldLang;
	    } else {
		stack.pushError();
	    }
	} else {
	    errors.error(Errors.EXEC,tree.pos,"wrong number of actual parameters");
	    stack.pushError();
	}
    }

    public void walkExp(XDOMCall tree,Ctx ctx) {
	int args = tree.argSize();
	Layer layer = exec.layerStack.top();
	byte pass = layer.pass;
	if (pass == PNONE) {
	    errors.error(Errors.EXEC,tree.pos,"walk:exp only availble during interpretation");
	} else if (args == 1)  {
	    XDOM x = null;
	    XDOM arg1 = tree.getArg(1);
	    boolean ok = true;

	    exec.execExp(arg1,value.ctxEval);
	    Object oval1 = stack.getTopOval();
	    long ival1 = stack.getTopIval();
	    stack.pop();
	    if (oval1 instanceof XDOM &&(! (oval1 instanceof XDOMElement))) {
		x = (XDOM) oval1;
	    } else {
		ok = false;
		if (! value.isError(oval1,ival1)) {
		    errors.error(Errors.EXEC,arg1.pos,"not an xdom expression");
		}
	    }
	    if (ok) {
		byte oldPass = pass;
		oldPass = layer.pass;
		if (oldPass == PBIND1) {
		    xt.bind.bind1exp(x);
		    stack.pushNull();
		} else if (oldPass == PBIND2) {
		    xt.bind.bind2exp(x);
		    stack.pushNull();
		} else if (oldPass == PEXEC) {
		    XFrame oldSlink = exec.frames.getTopFrame();
		    exec.frames.setSlink(layer.slink);
		    exec.execExp(x,ctx);
		    exec.frames.setSlink(oldSlink);
		}
	    } else {
		stack.pushError();
	    }
	} else {
	    errors.error(Errors.EXEC,tree.pos,"wrong number of actual parameters");
	    stack.pushError();
	}
    }

    public void walkBody(XDOMCall tree,Ctx ctx) {
	int args = tree.argSize();
	Layer layer = exec.layerStack.top();
	byte pass = layer.pass;
	if (pass == PNONE) {
	    errors.error(Errors.EXEC,tree.pos,"walk:body only availble during interpretation");
	} if (args == 2)  {
	    XDOMElement xe = null;
	    XDOM arg1 = tree.getArg(1);
	    XDOM arg2 = tree.getArg(2);
	    boolean ok = true;
	    exec.execExp(arg1,value.ctxEval);
	    Object oval1 = stack.getTopOval();
	    long ival1 = stack.getTopIval();
	    stack.pop();
	    exec.execExp(arg2,value.ctxEval);
	    Object oval2 = stack.getTopOval();
	    long ival2 = stack.getTopIval();
	    stack.pop();
	    if (oval1 instanceof XDOMElement) {
		xe = (XDOMElement) oval1;
	    } else {
		ok = false;
		if (! value.isError(oval1,ival1)) {
		    errors.error(Errors.EXEC,arg1.pos,"not an xdom element");
		}
	    }
	    if (ok) {
		Object oldLang = xt.currentLang;
		xt.currentLang = oval2;
		byte oldPass = pass;
		if (oldPass == PBIND1) {
		    xt.bind.bind1body(xe);
		    stack.pushNull();
		} else if (oldPass == PBIND2) {
		    xt.bind.bind2body(xe);
		    stack.pushNull();
		} else if (oldPass == PEXEC) {
		    XFrame oldSlink = exec.frames.getTopFrame();
		    exec.frames.setSlink(layer.slink);
		    exec.execBody1(xe,true,ctx,false);
		    exec.frames.setSlink(oldSlink);
		}
		xt.currentLang = oldLang;
	    } else {
		stack.pushError();
	    }
	} else {
	    errors.error(Errors.EXEC,tree.pos,"wrong number of actual parameters");
	    stack.pushError();
	}
    }	

    public void walkAssign(XDOMCall tree) {
	int args = tree.argSize();
	Layer layer = exec.layerStack.top();
	pass = layer.pass;
	if (pass != PEXEC) {
	    errors.error(Errors.EXEC,tree.pos,
			 "walk:assign only availble during interpretation exec");
	} else if (args == 2)  {
	    XDOM x = null;
	    XDOM arg1 = tree.getArg(1);
	    XDOM arg2 = tree.getArg(2);
	    boolean ok = true;
	    exec.execExp(arg1,value.ctxEval);
	    Object oval1 = stack.getTopOval();
	    long ival1 = stack.getTopIval();
	    stack.pop();
	    exec.execExp(arg2,value.ctxEval);
	    Object oval2 = stack.getTopOval();
	    long ival2 = stack.getTopIval();
	    stack.pop();
	    if (oval1 instanceof XDOM &&(! (oval1 instanceof XDOMElement))) {
		x = (XDOM) oval1;
	    } else {
		ok = false;
		if (! value.isError(oval1,ival1)) {
		    errors.error(Errors.EXEC,arg1.pos,"not an xdom expression");
		}
	    }
	    if (ok) {
		byte oldPass = pass;
		XFrame oldSlink = exec.frames.getTopFrame();
		exec.frames.setSlink(layer.slink);
		stack.push(oval2,ival2);
		exec.execExp(x,value.ctxAssign);
		stack.pop();
		exec.frames.setSlink(oldSlink);
	    }
	} else {
	    errors.error(Errors.EXEC,tree.pos,"wrong number of actual parameters");
	}
	stack.pushNull();
    }

    public void walkDefine(XDOMCall tree) {
	int args = tree.argSize();
	Layer layer = exec.layerStack.top();
	byte pass = layer.pass;
	if (pass != PBIND1) {
	    errors.error(Errors.EXEC,tree.pos,
			 "walk:define only available during interpretation bind1");
	} else if (args == 2)  {
	    XDOMName xn = null;
	    XDOMElement xe = null;
	    XDOM arg1 = tree.getArg(1);
	    XDOM arg2 = tree.getArg(2);
	    boolean ok = true;
	    exec.execExp(arg1,value.ctxEval);
	    Object oval1 = stack.getTopOval();
	    long ival1 = stack.getTopIval();
	    stack.pop();
	    exec.execExp(arg2,value.ctxEval);
	    Object oval2 = stack.getTopOval();
	    long ival2 = stack.getTopIval();
	    stack.pop();
	    if (oval1 instanceof XDOMName) {
		xn = (XDOMName) oval1;
	    } else {
		ok = false;
		if (! value.isError(oval1,ival1)) {
		    errors.error(Errors.EXEC,arg1.pos,"not an xdom name");
		}
	    }
	    if (oval2 instanceof XDOMElement) {
		xe = (XDOMElement) oval2;
	    } else {
		ok = false;
		if (! value.isError(oval2,ival2)) {
		    errors.error(Errors.EXEC,arg2.pos,"not an xdom element");
		}
	    }
	    if (ok) {
		// name,elem
		// readOnly, hasView,visible,local
		byte oldPass = pass;
		xt.bind.symtab.define(xn,xe,false,false,false,false);
	    }
	} else {
	    errors.error(Errors.EXEC,tree.pos,"wrong number of actual parameters");
	}
	stack.pushNull();
    }
}
