/*******************************************************************************
*
* Copyright (c) 2006. John R. Nestor. All rights reserved.
*
* See the file XLICENSE.txt for use and redistribution information.
*
*******************************************************************************/

package com.persist.xact.system;

public abstract class XRFile extends XR {
    public abstract boolean open();
    public abstract boolean open(String name);
}
