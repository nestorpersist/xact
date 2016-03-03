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
import com.persist.xact.exec.*;

/**
 * Each CmdScript represents the use
 * of an XACT script by a specific comand.
 */
public final class CmdScript {
    public Script script;
    public ExecScript execscript;
    public boolean shared;
    public CmdScript(Script script, ExecScript execscript) {
	this.script = script;
	this.execscript = execscript;
	shared = false;
    }
}
