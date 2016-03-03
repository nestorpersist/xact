/*******************************************************************************
*
* Copyright (c) 2007. John R. Nestor. All rights reserved.
*
* See the file XLICENSE.txt for use and redistribution information.
*
*******************************************************************************/

// 2. errors during get

package com.persist.xact.system;

import java.util.*;
import java.io.*;
import com.persist.xdom.*;

/**
 * This class maintains the list of all scripts
 * loaded by the intepreter.
 * These is exactly one instance of this class.
 * Each script is represented by a {@link Script} object.
 * If multiple servlet commands are executing,
 * they will share a single Script object for any
 * script they have in common.
 * 
 * Every script is identified by a small integer
 * index that remains fixed while the script is in use.
 * This index will be the index of the script in
 * the scripts array.
 * 
 * Since the scripts data structure can be accessed
 * from multiple threads, synchronization is used.
 * Scripts that are loaded but not being used are
 * placed on a doubly-linked free list. When too many
 * scripts are present, the least-recently used script
 * is removed (and must be reloaded if used again).
 */
public final class Scripts {
    /** the xact interpreter object */
    private XInter inter;

    /** Array of all scripts.
     ** The index in this array identifies
     ** a specific script.
     ** Unused indexes will have value null.
     */
    private ArrayList<Script> scripts;

    /** Number of non-null elements in scripts array **/
    private int numScripts;
    /**
     ** If there are more than maxScripts,
     ** scripts with useCnt == 0 are freed
     */
    private int maxScripts = 20;

    /**
     ** First free script (or null if none free).
     ** firstFree is least recently used script.
     */
    Script firstFree;
    /**
     ** Last free script (or null if none free).
     ** lastFree is most recently used script.
     */
    Script lastFree;
    /**
     ** Intialize a new Scripts object.
     ** @param inter the XACT interpreter object
     */
    public Scripts(XInter inter) {
	this.inter = inter;
	scripts = new ArrayList<Script>(100);
	numScripts = 0;
    }

    public Script getScript(int i) {
	return scripts.get(i);
    }
    
    private void addFirstFree(Script s) {
	if (firstFree != null) firstFree.prevFree = s;
	s.nextFree = firstFree;
	firstFree = s;
    }

    private void addLastFree(Script s) {
	if (lastFree != null) lastFree.nextFree = s;
	s.prevFree = lastFree;
	lastFree = s;
    }

    private void removeFree(Script s) {
	if (s.prevFree == null) {
	    firstFree = s.nextFree;
	} else {
	    s.prevFree.nextFree = s.nextFree;
	}
	if (s.nextFree == null) {
	    lastFree = s.prevFree;
	} else {
	    s.nextFree.prevFree = s.prevFree;
	}
	s.nextFree = null;
	s.prevFree = null;
    }

    /**
     ** Checks a script path for syntactic correctness.
     ** @param name the script path
     ** @return true if the script path is syntactically correct
     */
    private boolean checkPath(String name) {
	int size = name.length();
	int lastColon = -1;
	for (int i = 0; i < size; i++) {
	    char ch = name.charAt(i);
	    if ('a' <= ch && ch <= 'z') {
	    } else if ('A' <= ch && ch <= 'Z') {
	    } else if ('0' <= ch && ch <= '9') {
	    } else if (ch == ':') {
		if (i == 0) return false;
		if (i == size-1) return false;
		if (lastColon+1 == i) return false;
		lastColon = i;
	    } else {
		return false;
	    }
	}
	if (lastColon == size - 1) return false;
	return true;
    }

    /**
     ** Converts a script path to a fully qualified file name.
     ** @param xt the current thread (for getting options).
     ** @param name the script path
     ** @return the file name.
     */
    private String scriptToFile(XThread xt,String name) {
 	String name1 = name.replace(':',xt.cmd.option.fileSep.charAt(0));
	return (xt.cmd.option.scriptDir+xt.cmd.option.fileSep+name1+".xact").intern();
    }

    /**
     ** Test if a file exists.
     ** @param fname the file name
     ** @return true if the file exists.
     */
    private boolean exists(String fname) {
	File f = new File(fname);
	return f.exists();
    }

    /**
     ** Adds a script to the scripts array.
     ** First looks for a free slot.
     ** Otherwise adds it to the end.
     ** @param the script.
     ** @return the index of that script in the scripts
     ** array.
     */
    private int add(Script s) {
	int size = scripts.size();
	// look for a free script index
	for (int i = 0; i < size; i++) {
	    if (scripts.get(i) == null) {
		scripts.set(i,s);
		return i;
	    }
	}
	// add it to the end
	scripts.add(s);
	numScripts ++;
	return size;
    }

    /**
     ** Reads, parses
     ** and binds the script.
     ** @param s the script.
     ** @param xt the current thread.
     ** @param scriptPath the script path for the script.
     ** @param relPath the script path for the directory containing
     ** the requesting script.
     ** @param pos the position of the request (or null).
     */
    private void load(Script s,XThread xt,String scriptPath,String relPath,XPos pos) {
	// find script file
	if (! checkPath(scriptPath)) {
	    xt.errors.error(Errors.CONTROL,pos,"illegal script path");
	    return;
	}
	// try to find relative to relPath
	s.scriptPath = (relPath + ":" + scriptPath).intern();
	s.fileName = scriptToFile(xt,s.scriptPath);
	if (! exists(s.fileName)) {
	    // not found relative, so use absolute
	    s.scriptPath = scriptPath;
	    s.fileName = scriptToFile(xt,s.scriptPath);
	}

	// open script file
	XRFile r = new XRCFile("UTF-8");
	if (! r.open(s.fileName)) {
	    xt.errors.error(Errors.CONTROL,null,"can't open "+s.fileName);
	}
	
	// parse script
	FPosition fpos = new FPosition(FPosition.FSCRIPT,s.scriptPath);
	XDOMElement t = xt.parser.parse(r,true,fpos,null,null);
	s.tree = t;
 
	// bind script
	String myPath = s.scriptPath;
	int cpos = myPath.lastIndexOf(':');
	if (cpos != -1) {
	    myPath = myPath.substring(0,cpos).intern();
	}
	xt.bind.bind(s.tree,s.idx,myPath,xt.currentLang);
    }

    /**
     ** Gets the index of the Script object
     ** for a script. If not already present, reads, parses
     ** and binds the script.
     ** Also checks for use cycles, and reports an error
     ** when detected.
     ** @param xt the current thread
     ** @param scriptPath the script path for the script.
     ** @param relPath the script path for the directory containing
     ** the requesting script (or null).
     ** @param pos the position of the request (or null).
     ** @return the the loaded script (or null if loading failed).
     */
    public Script get(XThread xt,String scriptPath,String relPath,XPos pos) {
	String sp = scriptPath.intern();
	Script s = null;
	boolean found = false;
	synchronized (this) {
	    // if already there return it
	    int size = scripts.size();
	    for (int i = 0; i < size; i++) {
		s = scripts.get(i);
		if (s != null && s.scriptPath == sp) {
		    if (s.useCnt == 0) removeFree(s);
		    s.useCnt ++;
		    found = true;
		    break;
		}
	    }
	    if (! found) {
		// not there so must add it
		s = new Script(xt);
		s.idx = add(s);
	    }
	}
	if (found) {
	    if (s.loading) {
		if (s.xt == xt) {
		    xt.errors.error(Errors.BIND,pos,"use cycle detected: "+s.scriptPath);
		    return null;
		} else {
		    // Wait until available.
		    try {
			synchronized(s) {
			    while (s.loading) s.wait();
			}
		    } catch(Exception e) {
		    }
		}
	    }
	} else {
	    // Loading will also load child scripts
	    load(s,xt,sp,relPath,pos);
	    s.loading = false;
	    // Let others waiting for this script know its
	    // now available.
	    synchronized(s) {
		s.notifyAll();
	    }
	}
	while (numScripts > maxScripts && firstFree != null) {
	    free(firstFree,false);
	}
	return s;
    }

    /**
     ** Releases a script from a previous get.
     ** @param i the script index.
     */
    public void release(int idx) {
	synchronized (this) {
	    Script s = scripts.get(idx);
	    s.useCnt --;
	    if (s.useCnt == 0) {
		if (s.flush) {
		    free(s,false); // free it now
		} else {
		    addLastFree(s); // can be freed later
		}
	    }
	}
    }

    /**
     ** Frees a script.
     ** @param s the script
     ** @param freeChildren if true, children
     ** that where only used by this script are also
     ** freed.
     */
    private void free(Script s,boolean freeChildren) {
	// remove it from the free list
	removeFree(s);
	// remove it from scripts array
	scripts.set(s.idx,null);
	numScripts --;
    }

    /**
     ** Removes all scripts with useCnt 0.
     ** If mark is true marks all other scripts
     ** for removal when their use cnt hits 0.
     ** @param mark if true mark scripts with non-zero use counts.
     */
    private void flush(boolean mark) {
	synchronized (this) {
	    int size = scripts.size();
	    for (int i = 0; i < size; i++) {
		Script s = scripts.get(i);
		if (s != null) {
		    if (s.useCnt == 0) {
			free(s,true);
		    } else if (mark) {
			s.flush = true;
		    }
		} 
	    }
	}
    }

    /**
     ** Removes all scripts with useCnt 0.
     */
    public void flush() {
	flush(false);
    }

    /**
     ** Marks all scripts to be freed
     ** as soon as their useCnt hits 0
     */
    public void markFlush() {
	flush(true);
	synchronized (this) {
	    int size = scripts.size();
	    for (int i = 0; i < size; i++) {
		Script s = scripts.get(i);
		if (s != null) {
		    s.flush = true;
		}
	    }
	}
    }
	
    /**
     ** Debug routine for dumping information
     ** about currently loaded scripts.
     */
    public void debug() {
	synchronized (this) {
	    int size = scripts.size();
	    for (int i = 0; i < size; i++) {
		Script s = scripts.get(i);
		if (s != null) {
		    System.out.println(i+" => "+s.scriptPath+"("+s.useCnt+")");
		}
	    }
	}
    }
}
