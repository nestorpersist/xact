/*******************************************************************************
*
* Copyright (c) 2002. John R. Nestor. All rights reserved.
*
* See the file XLICENSE.txt for use and redistribution information.
*
*******************************************************************************/

package com.persist.xact.system;

public abstract class XW {
    public long count = 0;
    public abstract void write(char ch);
    public abstract void write(String s);
    public abstract void flush();
    public abstract void close();
}
