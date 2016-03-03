/*******************************************************************************
*
* Copyright (c) 2002-2007. John R. Nestor. All rights reserved.
*
* See the file XLICENSE.txt for use and redistribution information.
*
* This Class contains functions for processing query and option strings.
* These functions are called by x.java (for cgi and command line invocations)
* and by xs.java (for servlet invocations).
*
*******************************************************************************/

package com.persist.xact.system;

import com.persist.xdom.*;

public final class XArg {
    private XCmd cmd;
    private XThread xt;
    private XOption option;
    
    public XArg(XThread xt) {
	this.xt = xt;
	cmd = xt.cmd;
	option = cmd.option;
    }

    private Object argVal(XDOM x) {
	if (x instanceof XDOMString) {
	    XDOMString xs = (XDOMString) x;
	    return xs.getVal();
	} else if (x instanceof XDOMCall) {
	    return x;
	}
	return null;
    }

    private void setOption(String name,Object oval,long ival) {
	boolean ok = option.setOption(name,oval,ival);
	if (! ok) xt.errors.error(Errors.CONTROL,null,"can't set option: "+name);
    }

    private void setRec(boolean passed,XDOM x) {
	if (x instanceof XDOMCall) {
	    XDOMCall xc = (XDOMCall) x;
	    int size = xc.argSize();
	    if (xc.getKind() == XDOMCall.COP && size == 2) {
		XDOM arg1 = xc.getArg(1);
		XDOM arg2 = xc.getArg(2);
		Object val = argVal(arg2);
		if (arg1 instanceof XDOMName && val != null) {
		    XDOMName xn = (XDOMName) arg1;
		    String name = xn.getName();
		    if (passed) {
			option.setPassed(name,val,0);
		    } else {
			setOption(name,val,0);
		    }
		}
	    }
	} else if (x instanceof XDOMName) {
	    XDOMName xn = (XDOMName) x;
	    String name = xn.getName();
	    if (passed) {
		option.setPassed(name,cmd.value.vlBool,1);
	    } else {
		setOption(name,cmd.value.vlBool,1);
	    }
	}
    }

    public void doQuery(String s,String cmd) {
        setOption("query",s,0);
        XDOMCall qt = xt.xurl.URLParseQuery(s);
        int size1 = qt.argSize();
        if (cmd != "") {
            XDOMCall qt1 = xt.xurl.URLParseQuery(cmd);
            scriptTree = qt1.getArg(1);
        } else {
            if (size1 > 0) {
                scriptTree = qt.getArg(1);
            }
        }
	for (int j = 2; j <= size1; j++) {
	    setRec(true,qt.getArg(j));
	}
    }

    public void doOption(String s) {
	XDOMCall xc = xt.xurl.URLParseQuery(s);
	int size1 = xc.argSize();
	for (int j = 1; j <= size1; j++) {
	    setRec(false,xc.getArg(j));
	}
    }

    public void doInput(String s) {
        if (s.equals("")) {
            cmd.biBind.define("requestData","",0);
        } else if (s.substring(0,1).equals("<")) {
            cmd.biBind.define("requestData",s,0);
        } else {
            cmd.biBind.define("requestData","",0);
            XDOMCall xc = xt.xurl.URLParseQuery(s);
            int size1 = xc.argSize();
            for (int j = 1; j <= size1; j++) {
                setRec(true,xc.getArg(j));
            }
        }
    }

    // Script Information
    public String script = "";
    public XDOM scriptTree = null;

    public void setScript() {
	// Finalizes setup of: script, func, scriptTree
	if (scriptTree != null) {
	    if (scriptTree instanceof XDOMName) {
		XDOMName xn = (XDOMName) scriptTree;
		script = xn.getName();
	    } else if (scriptTree instanceof XDOMCall) {
		XDOMCall xc = (XDOMCall) scriptTree;
		XDOM func = xc.getFunc();
		if (func instanceof XDOMName) {
		    XDOMName xn = (XDOMName) func;
		    script = xn.getName();
		}
	    }
	}
	if (script == null | script == "") { script = option.defaultScript; }
	setOption("script",script,0);
    }
}
