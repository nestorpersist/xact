/*******************************************************************************
*
* Copyright (c) 2002-2010. John R. Nestor. All rights reserved.
*
* See the file XLICENSE.txt for use and redistribution information.
*
*******************************************************************************/

package com.persist.xact.system;

import com.persist.xdom.*;
import com.persist.xact.value.*;
import java.util.*;

public final class XOption {

    private XCmd cmd;

    public XDOMElement options = new XDOMElement("options");
    public XDOMElement passed = new XDOMElement("passed");

    public boolean optimize = true;         // syntax shortcuts, streaming
    public boolean check = true;	    // check assertions
    public boolean streamTop = false;       // top level streaming
    public boolean flushDebugAfter = true;  // flush stream at debug points
    public boolean debug = false;	    // break at start of execution of main script
    public boolean debugAll = false;	    // break at start of execution or each part
    public boolean debugError = true;       // enter debugger when error 
    public boolean debugWarn = false;       // enter debugger when warn
    public boolean scriptOnly = false;      // run only script (not named func)
    public boolean funcOnly = false;        // run only func (never script)
    public boolean asciiXML = false;        // output escaped to ASCII

    public long errorMax = 200;		    // maximum number of errors to log per run

    public String charSet = "ISO-8859-1";
    public String scriptDir = "c:\\xact\\scripts";  // location of scripts
    public String logDir = "c:\\xact\\logs";	    // location of logs
    public String scriptPrefix = "";		    // allow only scripts with this prefix
    public String mime="";		            // generate mime line
    public final String version="0.10";
    public String script = "";
    public String defaultScript="default";
    public String errorScript="error";
    public String inter="xact.bat";
    public final String fileSep=System.getProperty("file.separator");
    public final String os=System.getProperty("os.name");

    public final long startTime = System.currentTimeMillis()+
				 TimeZone.getDefault().getRawOffset();
    public String id="";

    public XOption(XCmd cmd) {
	this.cmd = cmd;
	cmd.biBind.define("passed",passed,0);
	cmd.biBind.define("options",options,0);
	id = cmd.toString();
	int cpos = id.indexOf("@");
	if (cpos != -1) {
	    id = id.substring(cpos+1);
	}
	options.setERec("check",cmd.value.vlBool,VLBool.toI(check));
	options.setERec("currentDir",System.getProperty("user.dir"),0);
	options.setERec("debug",cmd.value.vlBool,VLBool.toI(debug));
	options.setERec("debugAll",cmd.value.vlBool,VLBool.toI(debugAll));
	options.setERec("debugError",cmd.value.vlBool,VLBool.toI(debugError));
	options.setERec("debugWarn",cmd.value.vlBool,VLBool.toI(debugWarn));
	options.setERec("defaultScript",defaultScript,0);
	options.setERec("errorMax",cmd.value.vlInt,errorMax);
	options.setERec("errorScript",errorScript,0);
	options.setERec("fileSep",fileSep,0);
	options.setERec("flushDebugAfter",cmd.value.vlBool,VLBool.toI(flushDebugAfter));
	options.setERec("funcOnly",cmd.value.vlBool,VLBool.toI(funcOnly));
	options.setERec("asciiXML",cmd.value.vlBool,VLBool.toI(asciiXML));
	options.setERec("id",id,0);
	options.setERec("inter",inter,0);
	options.setERec("logDir",logDir,0);
	options.setERec("mime",mime,0);
	options.setERec("optimize",cmd.value.vlBool,VLBool.toI(optimize));
	options.setERec("os",os,0);
	options.setERec("scriptOnly",cmd.value.vlBool,VLBool.toI(scriptOnly));
	options.setERec("startTime",cmd.value.localDate,startTime);
	options.setERec("streamTop",cmd.value.vlBool,VLBool.toI(streamTop));
	options.setERec("scriptDir",scriptDir,0);
	options.setERec("charSet",charSet,0);
	options.setERec("scriptPrefix",scriptPrefix,0);
	options.setERec("version",version,0);
    }

    public boolean setOption(String name,Object oval,long ival) {
	String s = XDOMValue.getString(oval,ival);
	boolean ok = true;
	if (name == "optimize") {
	    if (s == "true") optimize = true;
	    else if (s == "false") optimize = false;
	    else ok = false;
	} else if (name == "streamTop") {
	    if (s == "true") streamTop = true;
	    else if (s == "false") streamTop = false;
	    else ok = false;
	} else if (name == "flushDebugAfter") {
	    if (s == "true") flushDebugAfter = true;
	    else if (s == "false") flushDebugAfter = false;
	    else ok = false;
	} else if (name == "debug") {
	    if (s == "true") debug = true;
	    else if (s == "false") debug = false;
	    else ok = false;
	} else if (name == "debugAll") {
	    if (s == "true") debugAll = true;
	    else if (s == "false") debugAll = false;
	    else ok = false;
	} else if (name == "debugError") {
	    if (s == "true") debugError = true;
	    else if (s == "false") debugError = false;
	    else ok = false;
	} else if (name == "debugWarn") {
	    if (s == "true") debugWarn = true;
	    else if (s == "false") debugWarn = false;
	    else ok = false;
	} else if (name == "check") {
	    if (s == "true") check = true;
	    else if (s == "false") check = false;
	    else ok = false;
	} else if (name == "scriptOnly") {
	    if (s == "true") scriptOnly = true;
	    else if (s == "false") scriptOnly = false;
	    else ok = false;
	} else if (name == "funcOnly") {
	    if (s == "true") funcOnly = true;
	    else if (s == "false") funcOnly = false;
	    else ok = false;
	} else if (name == "asciiXML") {
	    if (s == "true") asciiXML = true;
	    else if (s == "false") asciiXML = false;
	    else ok = false;
	} else if (name == "errorMax") {
	    if (XDOMInt.isInt(s)) {
		errorMax = XDOMInt.toInt(s);
	    } else {
		ok = false;
	    }
	} else if (name == "scriptDir") {
	    scriptDir = s;
	} else if (name == "charSet") {
	    charSet = s;
	} else if (name == "logDir") {
	    logDir = s;
	} else if (name == "scriptPrefix") {
	    scriptPrefix = s;
	} else if (name == "mime") {
	    mime = s;
        } else if (name == "script") {
            script = s;
        } else if (name == "defaultScript") {
            defaultScript = s;
	} else if (name == "errorScript") {
	    errorScript = s;
	} else if (name == "inter") {
	    inter = s;
	} else if (name == "fileSep") {
	    ok = false;
	} else if (name == "os") {
	    ok = false;
	} else if (name == "startTime") {
	    ok = false;
	} else if (name == "id") {
	    ok = false;
	} else if (name == "version") {
	    ok = false;
	} else if (name == "currentDir") {
	    ok = false;
	}
	if (ok) options.setERec(name,oval,ival);
	return ok;
    }

    public void setPassed(String name,Object oval,long ival) {
	passed.setERec(name,oval,ival);
    }
}
