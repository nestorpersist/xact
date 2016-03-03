/*******************************************************************************
*
* Copyright (c) 2002-2007. John R. Nestor. All rights reserved.
*
* See the file XLICENSE.txt for use and redistribution information.
*
*******************************************************************************/

package com.persist.xact;

import java.io.*;
import com.persist.xact.system.*;

/**
 *  This class is used to run an XACT script.
 *  It can be called from the command line or
 *  as a CGI.
 */
public final class x {

    /**
     ** Main entry point for XACT interpreter.
     ** @param args command line args.
     */
    public static void main(String[] args) throws Exception {
	XInter inter = new XInter();
	XCmd cmd = new XCmd(inter);
	XThread xt = new XThread("main",cmd);
	cmd.xtMain = xt;

	int size = args.length;
	if (size > 0) {
	    xt.xarg.doQuery(args[0]);
	}
	int i;
	for (i = 1; i < size; i++) {
	    xt.xarg.doOption(args[i]);
	}
	xt.xarg.setScript();
	xt.xrun.run(xt.xarg.script,xt.xarg.scriptTree,null,true);
	System.exit(0);
    }
}
