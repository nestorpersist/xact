/*******************************************************************************
*
* Copyright (c) 2002-2007. John R. Nestor. All rights reserved.
*
* See the file XLICENSE.txt for use and redistribution information.
*
*******************************************************************************/

package com.persist.xact.system;

import java.io.*;
import java.util.*;
import com.persist.xdom.*;
import com.persist.xact.value.*;

public final class Errors {
    private XThread xt;
    private XCmd cmd;

    private long startTime = 0;

    public final static int PARSE = 0;
    public final static int BIND = 1;
    public final static int EXEC = 2;
    public final static int INTERNAL = 3;
    public final static int CONTROL = 4;

    public final static String[] ekind = {
	"Parse", "Bind", "Exec", "Internal", "Control"
    };

    public final static int ERROR = 0;
    public final static int WARN = 1;
    public final static int LOG = 2;

    public final static String[] skind = {
	"error", "warn", "log"
    };
    
    public Errors(XThread xt) {
	this.xt = xt;
	cmd = xt.cmd;
    }

    public void fail(Throwable e,String where) {
	System.out.println(where+" failed");
	e.printStackTrace();
    }

    private Writer commonOut(String tag,long time) {
	Writer w = null;
	String date = VLDate.toString(cmd.value.localDate,time);
	try {
	    String file = date.substring(0,10).replace('/','.');
	    String fname = cmd.option.logDir+cmd.option.fileSep+file+".log";
 	    w = new FileWriter(fname,true);
	    w.write("<");
	    w.write(tag);
	    w.write(" time=\"");
	    w.write(date);
	    w.write("\"");
	    w.write(" id=\"");
	    w.write(cmd.option.id);
	    w.write("\"");

	    if (xt != cmd.xtMain) {
		w.write(" thread=\"");
		w.write(xt.name);
		w.write("\"");
		String tobj=xt.toString();
		w.write(" threadId=\"");
		int pos = tobj.indexOf("@");
		if (pos != -1) {
		    tobj = tobj.substring(pos+1);
		}
		w.write(tobj);
		w.write("\"");
	    }
	} catch (Exception e) {
	    System.out.println("commonOut failed"+e);
	}
	return w;
    }

    public void start() {
//	startTime = System.currentTimeMillis()+ TimeZone.getDefault().getRawOffset();
	startTime = System.currentTimeMillis();
    }

    private void start1() {
	try {
	    Writer w = commonOut("start",startTime);
	    String query = cmd.option.options.getRec("query");
	    if (query != null) {
		w.write(" query=\"");
		w.write(query);
		w.write("\"");
	    }
	    String ra = cmd.option.options.getRec("REMOTE_ADDR");
	    if (ra != null) {
		w.write(" REMOTE_ADDR=\"");
		w.write(ra);
		w.write("\"");
	    }
	    w.write("/>\n");
	    w.close();
	} catch(Exception e) {
	}
    }

    public void end() {
	if (cmd.msgCnt == 0) return;
//	long time = System.currentTimeMillis()+ TimeZone.getDefault().getRawOffset();
	long time = System.currentTimeMillis();
	try {
	    Writer w = commonOut("end",time);
	    w.write("/>\n");
	    w.close();
	} catch(Exception e) {
	}
    }

    private void error1(int severity,int kind,XPos pos,String what,long time) {
	String kinds = ekind[kind];
	try {
	    Writer w = commonOut(skind[severity],time);
	    w.write(" kind=\"");
	    w.write(kinds);
	    w.write("\"");
	    if (pos != null) {
		FPosition fpos = pos.fpos;
		if (fpos != null) {
		    if (fpos.kind == FPosition.FSCRIPT) {
			w.write(" script=\"");
		    } else if (fpos.kind == FPosition.FFILE) {
			w.write(" file=\"");
		    } else {
			w.write(" desc=\"");
		    }
		    w.write(fpos.name);
		    w.write("\"");
		}
		w.write(" firstLine=\""+pos.firstLine);
		w.write("\"");
		w.write(" firstChar=\""+pos.firstChar);
		w.write("\"");
		if (pos.lastLine > 0) {
		    w.write(" lastLine=\""+pos.lastLine);
		    w.write("\"");
		    w.write(" lastChar=\""+pos.lastChar);
		    w.write("\"");
		}
	    }
	    w.write(" message=\"");
	    w.write(cmd.value.escapeString(what,'"'));
	    w.write("\"");
	    w.write("/>\n");
	    w.close();
	} catch (Exception e) {
	}
    }

    private void msg(int severity,int kind,XPos pos,String what,long time) {
	synchronized(cmd) {
	    if (cmd.msgCnt == 0) start1();
	    cmd.msgCnt ++;
	    if (cmd.msgCnt > cmd.option.errorMax) {
		if (cmd.msgCnt == cmd.option.errorMax + 1) {
		    error1(ERROR,CONTROL,null,"too many messages",time);
		}
	    } else {
		error1(severity,kind,pos,what,time);
	    }
	}
    }

    public void error(int kind,XPos pos,XPos debugPos,String what) {
	if (cmd.debug) return;
	long time = System.currentTimeMillis();
	synchronized(cmd) {
	    cmd.errorCnt ++;
	    msg(ERROR,kind,pos,what,time);
	    if (cmd.option.debugError) {
		cmd.dconnect.setError(xt,debugPos,ekind[kind]+" error",what);
	    }
	}
    }


    public void error(int kind,XPos pos,String what) {
	error(kind,pos,pos,what);
    }

    public void warn(int kind,XPos pos,String what) {
	if (cmd.debug) return;
	long time = System.currentTimeMillis();
	synchronized(cmd) {
	    cmd.warnCnt ++;
	    msg(WARN,kind,pos,what,time);
	    if (cmd.option.debugWarn) {
		cmd.dconnect.setError(xt,pos,ekind[kind]+" warning",what);
	    }
	}
    }

    public void log(int kind,XPos pos,String what) {
	if (cmd.debug) return;
	long time = System.currentTimeMillis();
	synchronized(cmd) {
	    cmd.logCnt ++;
	    msg(LOG,kind,pos,what,time);
	}
    }
}
