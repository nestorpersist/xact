/*******************************************************************************
*
* Copyright (c) 2007. John R. Nestor. All rights reserved.
*
* See the file XLICENSE.txt for use and redistribution information.
*
*******************************************************************************/

// 1. Shared scripts
// 2. deal with errors

package com.persist.xact.system;

import java.util.*;
import java.io.*;
import com.persist.xdom.*;
import com.persist.xact.value.*;
import com.persist.xact.exec.*;

/**
 * This class maintains the list of all scripts
 * used by a single command.
 * There is one instance of this class for each command.
 * Each script is represented by a {@link CmdScript} object.
 * 
 * Every script is identified by a small integer
 * index that remains fixed while the script is in use.
 * This index will be the index of the script in
 * the cmdscripts array.
 * This is also the same index used in the Scripts class scripts array.
 * 
 * Since the scripts data structure can be accessed
 * from multiple threads, synchronization is used.
 */
public final class CmdScripts {

    private XCmd cmd;
    private Scripts scripts;

    private ArrayList<CmdScript> cmdscripts;

    public CmdScripts(XCmd cmd) {
	this.cmd = cmd;
	cmdscripts = new ArrayList<CmdScript>(100);
	scripts = cmd.inter.scripts;
    }

    public CmdScript getScript(int i) {
	return cmdscripts.get(i);
    }

    public CmdScript get(XThread xt,Script script,Ctx ctx) {
	boolean found = false;
	CmdScript cmdscript = null;
	ExecScript execscript = null;
	int idx = script.idx;
	synchronized(this) {
	    // make sure slot exists
	    while (idx >= cmdscripts.size()) {
		cmdscripts.add(null);
	    }
	    cmdscript = cmdscripts.get(idx);
	    if (cmdscript == null) {
		// not there, add
		execscript = new ExecScript();
		cmdscript = new CmdScript(script,execscript);
		cmdscripts.set(idx,cmdscript);
	    } else {
		// already present
		found = true;
		execscript = cmdscript.execscript;
	    }
	}

	if (found) {
	    scripts.release(idx); // already counted before, so decrement use count
	    if (execscript.loading) {
		    // being loaded, wait until available
		try {
		    synchronized(execscript) {
			while (execscript.loading) execscript.wait();
		    }
		} catch(Exception e) {
		}
	    }
	} else {
	    Script s = scripts.getScript(idx);
	    XDOMElement t = s.tree;
		// not present for this command, so exec it
	    XFrame f = new XFrame();
	    execscript.script = s;
	    execscript.frame = f;
	    f.uid = 0;
	    f.slink = null;
	    f.stack = new FixedStack(t.scope.size);
	    f.base = 0;
	    f.scope = t.scope;
	    f.level = idx;
	    f.size = t.scope.size;

	    VariableStack stack = xt.exec.stack;
	    try {
		if (xt.cmd.option.debugAll) xt.exec.setDebug(true,true,false,-1);
		xt.exec.exec(s.tree,ctx,xt.currentLang);
	    } catch(Exception e) {
		xt.errors.fail(e,"exec exception");
	    }
	    execscript.oval = stack.getTopOval();
	    execscript.ival = stack.getTopIval();

	    stack.pop();

	    execscript.loading = false;
	    // Let other threads waiting for this
	    // script know its now available
	    synchronized (execscript) {
		execscript.notifyAll();
	    }
	}
	return cmdscript;
    }

    /**
     ** Release all scripts held by this command.
     */
    public void release() {
	int size = cmdscripts.size();
	for (int i = 0; i < size; i++) {
	    CmdScript cmdscript = cmdscripts.get(i);
	    if (cmdscript != null) {
		scripts.release(i);
	    }
	}
    }

    /**
     ** Debug routine for dumping information
     ** about currently loaded scripts
     ** for a specific command
     */
    public void debug() {
	synchronized (this) {
	    int size = cmdscripts.size();
	    for (int i = 0; i < size; i++) {
		CmdScript cs = cmdscripts.get(i);
		if (cs != null) {
		    ExecScript es = cs.execscript;
		    Script s = es.script;
		    System.out.println(i+" => "+s.scriptPath+"("+es.loading+")");
		}
	    }
	}
    }
}

