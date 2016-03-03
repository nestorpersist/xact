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
 * Each ExecScript represents the top-level
 * execution of an XACT script.
 * In the normal case, there is one top-leve execution
 * for each using command.
 * However, if the script is shared, then there is
 * one top-level execution for all scripts.
 */
public final class ExecScript {
    public Script script;
    public XFrame frame;
    public Object oval;
    public long ival;
    /** True if executing the top-level script **/
    public boolean loading;
    public ExecScript() {
	loading = true;
    }
}
