/*******************************************************************************
*
* Copyright (c) 2002-2007. John R. Nestor. All rights reserved.
*
* See the file XLICENSE.txt for use and redistribution information.
*
*******************************************************************************/

package com.persist.xact.system;

import java.io.*;
import com.persist.xdom.*;
import com.persist.xact.value.*;
import com.persist.xact.bind.*;
import com.persist.xact.exec.*;

public final class XRun {
    private XCmd cmd;
    private XThread xt;
    private XOption option;
    private Exec exec;
    private Frames frames;
    private VariableStack stack;

    private Object resultOval = null;
    private long resultIval = 0;

    public XRun(XThread xt) {
	this.xt = xt;
	cmd = xt.cmd;
	option = cmd.option;
	exec = xt.exec;
	frames = exec.frames;
	stack = exec.stack;
    }

    private static Object argVal(XDOM x) {
	if (x instanceof XDOMString) {
	    XDOMString xs = (XDOMString) x;
	    return xs.getVal();
	} else if (x instanceof XDOMCall) {
	    return x;
	}
	return null;
    }

    private static String scriptTail(String name) {
	int last = name.lastIndexOf(':');
	if (last == -1) {
	    return name;
	} else {
	    return name.substring(last+1).intern();
	}
    }

    private static XDOMElement varArg(XDOMCall callTree) {
	XDOMElement xe = new XDOMElement("parameters");
	int size = 0;
	if (callTree != null) {
	    size = callTree.argSize();
	}
	for (int i = 1; i <= size; i++) {
	    XDOM arg = callTree.getArg(i);
	    if (TreeUtil.isEqual(arg)) {
		String name = TreeUtil.getEqualName(arg);
		XDOM val = TreeUtil.stripEqual(arg);
		xe.setERec(name,argVal(val),0);
	    } else {
		xe.insertAttr(-1,arg);
	    }
	}
	return xe;
    }

    private void callx(XDef def,XW w,XDOM scriptTree,Ctx ctx) {
	if (def instanceof XDefName) {
	    XDefName ndef = (XDefName) def;
	    frames.getDefVal(ndef,null);
	    Object oval = stack.getTopOval();
	    stack.pop();
	    if (oval instanceof VLFunc) {
		VLFunc vlf = (VLFunc) oval;
		int formals = vlf.def.args;
		int actuals = 0;
		XDOMCall callTree = null;
		if (scriptTree instanceof XDOMCall) {
		    callTree = (XDOMCall) scriptTree;
		    actuals = callTree.argSize();
		}
		int argIdx = stack.getTop()+1;
		if (vlf.def.varArgs) {
		    stack.push(varArg(callTree),0);
		    actuals = 1;
		} else {
		    if (formals == 0 && actuals == 1) {
			Object o = argVal(callTree.getArg(1));
			if (o instanceof String) {
			    String s = (String) o;
			    if (s == "") {
				actuals = 0;
			    }
			}
		    }
		    for (int i = 1; i <= actuals; i++) {
			stack.push(argVal(callTree.getArg(i)),0);
		    }
		}
		exec.callExec.execCallFunc(vlf,0,actuals,null,ctx);
		resultOval = stack.getTopOval();
		resultIval = stack.getTopIval();
		stack.pop();
	    }
	}
    }

    public void run(String scriptName,XDOM scriptTree,OutputStream os,boolean doMime) {
	String func = scriptTail(scriptName);
	boolean restart = false;
	if (os == null) {
	    XWFile f;
	    if (option.charSet == "byte") {
		f = new XWBFile();
	    } else {
		f = new XWCFile(option.charSet);
	    }
	    f.open();
	    cmd.w = f;
	} else {
	    XWFile f;
	    if (option.charSet == "byte") {
		f = new XWBFile(os);
	    } else {
		f = new XWCFile(os,option.charSet);
	    }
	    cmd.w = f;
	}
	if (option.asciiXML) {
	    cmd.w = new XWEncode(cmd.w);
	}
	Ctx ctx = cmd.value.ctxEval;
	if (option.scriptPrefix != "") {
	    if (scriptName.indexOf(option.scriptPrefix) != 0) {
		xt.errors.error(Errors.CONTROL,null,"script prohibited by script prefix");
		return;
	    }
	}
	if (option.streamTop) {
	    ctx = new CtxStream(cmd.w);
	}
	do {
	    restart = false;
	    xt.errors.start();
	    try {
		if (cmd.inter.newLoader) {
		    Script script = cmd.inter.scripts.get(xt,scriptName,null,null);
		    if (cmd.errorCnt == 0 && script != null) {
			XScope scope = script.tree.scope;
			XDef def = scope.find(func,false);
			XDef def1 = null;
			if (def != null) { def1 = def.getReal(); }
			if (! option.scriptOnly && def1 != null && def1 instanceof XDefFunc && def.visible) {
			    CmdScript cmdscript = cmd.cmdscripts.get(xt,script,cmd.value.ctxEval);
			    ExecScript execscript = cmdscript.execscript;
			    execscript.oval = null; // so result will be gc'ed
			    execscript.ival = 0;
			    if (option.debug) exec.setDebug(true,true,false,-1);
			    if (option.mime != "" && doMime) {
				cmd.w.write("Content-type: "+option.mime+"\n\n");
			    }
			    callx(def1,cmd.w,scriptTree,ctx);
			} else if (! option.funcOnly) {
			    if (option.debug) exec.setDebug(true,true,false,-1);
			    if (option.mime != "" && doMime) {
				cmd.w.write("Content-type: "+option.mime+"\n\n");
			    }
			    CmdScript cmdscript = cmd.cmdscripts.get(xt,script,ctx);
			    ExecScript execscript = cmdscript.execscript;
			    resultOval = execscript.oval;
			    resultIval = execscript.ival;
			} else {
			    if (def1 == null || !(def1 instanceof XDefFunc) || ! def.visible) {
				xt.errors.error(Errors.CONTROL,null,"can't find function "+func);
			    } else {
				xt.errors.error(Errors.CONTROL,null,"not allowed to invoke script");
			    }
			}
		    }
		    if (cmd.errorCnt > 0) {
			cmd.w.flush();
			String escriptName = option.errorScript;
			Script escript = cmd.inter.scripts.get(xt,escriptName,null,null);
			if (escript != null) {
			    XScope scope = escript.tree.scope;
			    if (scope != null) {
				XDef def = scope.find(scriptTail(escriptName),false);
				XDef def1 = null;
				if (def != null) { def1 = def.getReal(); }
				if (def1 != null && def1 instanceof XDefFunc && def.visible) {
				    XDOMCall escriptTree = new XDOMCall(new XDOMName("error"));
				    escriptTree.insertArg(-1,new XDOMString(resultOval,resultIval));
				    CmdScript ecmdscript = cmd.cmdscripts.get(xt,escript,cmd.value.ctxEval);
				    ExecScript eexecscript = ecmdscript.execscript;
				    eexecscript.oval = null; // so result will be gc'ed
				    eexecscript.ival = 0;
				    callx(def1,cmd.w,escriptTree,ctx);
				}
			    }
			}
		    }
		    cmd.value.printVal(cmd.w,resultOval,resultIval);
		    cmd.w.flush();
		    cmd.dconnect.done(xt);
		} else {
		    Load ld = cmd.loads.getUse(xt,scriptName,null,"");
		    if (option.mime != "" && doMime) {
			cmd.w.write("Content-type: "+option.mime+"\n\n");
		    }
		    if (cmd.errorCnt == 0 && ld != null) {
			XScope scope = ld.tree.scope;
			XDef def = scope.find(func,false);
			XDef def1 = null;
			if (def != null) { def1 = def.getReal(); }
			if (! option.scriptOnly && def1 != null && def1 instanceof XDefFunc && def.visible) {
			    cmd.loads.execUse(xt,ld,cmd.value.ctxEval);
			    ld.oval = null; // so result will be gc'ed
			    ld.ival = 0;
			    if (option.debug) exec.setDebug(true,true,false,-1);
			    callx(def1,cmd.w,scriptTree,ctx);
			} else if (! option.funcOnly) {
			    if (option.debug) exec.setDebug(true,true,false,-1);
			    cmd.loads.execUse(xt,ld,ctx);
			    resultOval = ld.oval;
			    resultIval = ld.ival;
			} else {
			    if (def1 == null || !(def1 instanceof XDefFunc) || ! def.visible) {
				xt.errors.error(Errors.CONTROL,null,"can't find function "+func);
			    } else {
				xt.errors.error(Errors.CONTROL,null,"not allowed to invoke script");
			    }
			}
		    }
		    if (cmd.errorCnt > 0) {
			cmd.w.flush();
			String escript = option.errorScript;
			Load lderr = cmd.loads.getUse(xt,escript,null,"");
			if (lderr != null) {
			    XScope scope = lderr.tree.scope;
			    if (scope != null) {
				XDef def = scope.find(scriptTail(escript),false);
				XDef def1 = null;
				if (def != null) { def1 = def.getReal(); }
				if (def1 != null && def1 instanceof XDefFunc && def.visible) {
				    XDOMCall escriptTree = new XDOMCall(new XDOMName("error"));
				    escriptTree.insertArg(-1,new XDOMString(resultOval,resultIval));
				    cmd.loads.execUse(xt,lderr,cmd.value.ctxEval);
				    lderr.oval = null; // so result will be gc'ed
				    lderr.ival = 0;
				    callx(def1,cmd.w,escriptTree,ctx);
				}
			    }
			}
		    }
		    cmd.value.printVal(cmd.w,resultOval,resultIval);
		    cmd.w.flush();
		    cmd.dconnect.done(xt);
		}
	    } catch(Throwable e) {
		String s = e.getMessage();
		if (e == XCmd.quitException) {
		} else if (e == XCmd.restartException) {
		    restart = true;
		    cmd.loads.reset();
		} else {
		    System.out.println("fail:"+e);
		    xt.errors.fail(e,"main");
		}
	    }
	    cmd.w.flush();
	} while (restart);
	xt.errors.end();
	ctx.sendSpace();
	cmd.w.close();
	cmd.dconnect.quit(xt);
    }
}
