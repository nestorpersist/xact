/*******************************************************************************
*
* Copyright (c) 2002. John R. Nestor. All rights reserved.
*
* See the file XLICENSE.txt for use and redistribution information.
*
*******************************************************************************/

package com.persist.xact.system;

public abstract class XR {
    public abstract int read();
    public abstract int read(char buff[]);
    public abstract void close();
}
