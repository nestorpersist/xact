/*******************************************************************************
*
* Copyright (c) 2007. John R. Nestor. All rights reserved.
*
* See the file XLICENSE.txt for use and redistribution information.
*
*******************************************************************************/

package com.persist.xact.system;

import com.persist.xact.value.*;
/**
 * This class is the top level data for
 * an XACT interpreter invocation.
 * There is exacly one instance of this class.
 */
public final class XInter {
    /** The Value object for the intepreter */
    public Value value;
    /** The Loads object for the interpreter */
    public Loads loads = null;
    /** The Scripts object for the interpreter */
    public Scripts scripts = null;

    public boolean newLoader = false;

    /**
     ** Initializes a new XInter object.
     */
    public XInter() {
	value = new Value();
	if (newLoader) {
	    scripts = new Scripts(this);
	} else {
	    loads = new Loads(this);
	}
    }
}