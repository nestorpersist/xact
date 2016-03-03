/*******************************************************************************
*
* Copyright (c) 2002. John R. Nestor. All rights reserved.
*
* See the file XLICENSE.txt for use and redistribution information.
*
*******************************************************************************/

package com.persist.xact.exec;

import com.persist.xact.value.*;

public final class FixedStack extends Stack {
    private long[] ivals;
    public Object[] ovals;

    public FixedStack(int size) {
	ivals = new long[size];
	ovals = new Object[size];
	for (int i = 0; i < size; i++) {
	    ivals[i] = Value.VUNINIT;
	}
    }

    public long getIval(int pos) {
	return ivals[pos];
    }

    public Object getOval(int pos) {
	return ovals[pos];
    }

    public void setVal(int pos,Object oval,long ival) {
	ovals[pos] = oval;
	ivals[pos] = ival;
    }

}
