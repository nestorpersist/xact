/*******************************************************************************
*
* Copyright (c) 2002. John R. Nestor. All rights reserved.
*
* See the file XLICENSE.txt for use and redistribution information.
*
*******************************************************************************/

package com.persist.xact.exec;

public abstract class Stack {
    public abstract long getIval(int pos);
    public abstract Object getOval(int pos);
    public abstract void setVal(int pos,Object oval,long ival);
}

