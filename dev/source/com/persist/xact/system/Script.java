/*******************************************************************************
*
* Copyright (c) 2007. John R. Nestor. All rights reserved.
*
* See the file XLICENSE.txt for use and redistribution information.
*
*******************************************************************************/

package com.persist.xact.system;

import java.util.*;
import com.persist.xdom.*;

/**
 * Each Script object represents an XACT script
 * that has been read, parsed and bound.
 */
public final class Script {
    /** The script path to the script **/
    public String scriptPath;
    /** The file name to the script **/
    public String fileName;
    /** The parse tree for the script **/
    public XDOMElement tree;
    /** A use cnt. **/
    public int useCnt;
    /** True if reading, parsing, and binding the script **/
    public boolean loading;
    /** Thread that loaded this script **/
    public XThread xt;
    /** index of this script in the scripts array **/
    public int idx;
    /** is stack frame shared among all cmds **/
    public boolean shared;

    /**
     ** Next free script (or null if last)
     ** Set to null if useCnt > 0
     */
    public Script nextFree;
    /**
     ** Previous free script (or null if first)
     ** Set to null if useCnt > 0
     */
    public Script prevFree;

    /** when true, free as soon as useCnt hits 0 **/
    public boolean flush;
	    
    /**
     ** Initialize a new Script object.
     */
    public Script(XThread xt) {
	useCnt = 1;
	this.xt = xt;
	loading = true;
	flush = false;
	shared = false;
    }
}
