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
import com.persist.xact.exec.*;
import com.persist.xact.value.*;

public final class Loads {
    XInter inter;

    private Vector<Load> loads;

    static final boolean debugLoads = false;

    private int loadFails = 0;
    
    public Loads(XInter inter) {
	this.inter = inter;
	loads = new Vector<Load>(100);
    }

    public void reset() {
	synchronized(this) {
	    if (debugLoads) System.out.println("loads:reset");
	    loads.setSize(0);
	}
    }

    public Load getLoad(int i) {
	Load ld = null;
	synchronized(this) {
	    ld = loads.elementAt(i);
	}
	return ld;
    }

    public String convertPath(XThread xt,String name) {
	int size = name.length();
	xt.sbuff.setLength(0);
	int lastColon = -1;
	for (int i = 0; i < size; i++) {
	    char ch = name.charAt(i);
	    if ('a' <= ch && ch <= 'z') {
		xt.sbuff.append(ch);
	    } else if ('A' <= ch && ch <= 'Z') {
		xt.sbuff.append(ch);
	    } else if ('0' <= ch && ch <= '9') {
		xt.sbuff.append(ch);
	    } else if (ch == ':') {
		if (i == 0) return null;
		if (i == size-1) return null;
		if (lastColon+1 == i) return null;
		xt.sbuff.append(xt.cmd.option.fileSep.charAt(0));
		lastColon = i;
	    } else {
		return null;
	    }
	}
	return xt.sbuff.toString().intern();
    }

    private String processPath(XThread xt,String name,String path) {
	if (path != "") {
	    String rel = path + ":" + name;
	    String fpath = convertPath(xt,rel);
	    if (fpath != null) {
		String fname = xt.cmd.option.scriptDir+xt.cmd.option.fileSep+fpath+".xact";
		File f = new File(fname);
		if (f.exists()) {
		    return fpath;
		}
	    }
	}
	return convertPath(xt,name);
    }
    
    public Load getUse(XThread xt,String name,XPos pos,String myPath) {
	Errors errors = xt.errors;
	String filePath = processPath(xt,name,myPath);
	String fullPath = filePath.replace(xt.cmd.option.fileSep.charAt(0),':').intern();
	if (fullPath == null) {
	    errors.error(Errors.CONTROL,pos,"illegal script path");
	    return null;
	}
	String fname = xt.cmd.option.scriptDir+xt.cmd.option.fileSep+filePath+".xact";
	String path = "";
	int cpos = fullPath.lastIndexOf(':');
	if (cpos != -1) {
	    path = fullPath.substring(0,cpos).intern();
	}
	Load load = null;
	Load result = null;
	boolean found = false;
	int last = 0;
	synchronized(this) {
	    int size = loads.size();
	    for (int i = 0; i < size; i++) {
		Load ld = loads.elementAt(i);
		if (ld.name == fullPath) {
		    if (debugLoads) System.out.println("loads:found:"+fullPath+i);
		    if (ld.status == Load.PARSE && load.xtLoad == xt) {
			errors.error(Errors.BIND,pos,"use cycle detected");
		    } else {
			load = ld;
		    } 
		    found = true;
		    if (load.hasErrors) loadFails++;
		    break;
		}
	    }
	    if (! found) {
		if (debugLoads) System.out.println("loads:notfound:"+fullPath+loads.size());
		load = new Load();
		load.status = Load.ERROR;
		load.name = fullPath;
		load.fname = fname;
		load.xtLoad = xt;
		loads.addElement(load);
		last = loads.size()-1;
	    }
	}
	if (found) {
	    if (load != null && load.status != Load.DONE && load.xtLoad != xt) {
		try {
		    synchronized(load) {
			while (load.status != Load.DONE) load.wait();
		    }
		} catch(Exception e) {
		}
	    }
	    return load;
	}
	XRFile r = new XRCFile("UTF-8");
	if (r.open(fname)) {
	    FPosition fpos = new FPosition(FPosition.FSCRIPT,fullPath);
	    int errorsBefore = xt.cmd.errorCnt;
	    int loadFailsBefore = loadFails;
	    XDOMElement t = xt.parser.parse(r,true,fpos,null,null);
	    load.tree = t;
	    load.status = Load.PARSE;
	    xt.bind.bind(t,last,path,xt.currentLang);
	    load.status = Load.BIND;
	    if (xt.cmd.errorCnt > errorsBefore ||
	        loadFails > loadFailsBefore) {
		load.hasErrors = true;
	    }
	    XFrame f = new XFrame();
	    load.frame = f;
	    f.uid = 0;
	    f.slink = null;
	    f.stack = new FixedStack(t.scope.size);
	    f.base = 0;
	    f.scope = t.scope;
	    f.level = last;
	    f.size = t.scope.size;
	    result = load;
	} else {
	    errors.error(Errors.CONTROL,null,"can't open "+fname);
	}
	return result;
    }

    private void execUse1(XThread xt,Load load,Ctx ctx) {
	if (load.status != Load.BIND) return;
	if (debugLoads) System.out.println("loads:exec:"+load.name);
	Errors errors = xt.errors;
	VariableStack stack = xt.exec.stack;
	try {
	    if (xt.cmd.option.debugAll) xt.exec.setDebug(true,true,false,-1);
	    xt.exec.exec(load.tree,ctx,xt.currentLang);
	} catch(Exception e) {
	    errors.fail(e,"exec exception");
	}
	load.oval = stack.getTopOval();
	load.ival = stack.getTopIval();
	stack.pop();
	synchronized(load) {
	    load.status = Load.DONE;
	    load.notifyAll();
	}
    }

    public void execUse(XThread xt,Load load,Ctx ctx) {
	if (! load.hasErrors) {
	    execUse1(xt,load,ctx);
	}
    }
}
