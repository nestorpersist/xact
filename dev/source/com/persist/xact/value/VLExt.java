/*******************************************************************************
*
* Copyright (c) 2002. John R. Nestor. All rights reserved.
*
* See the file XLICENSE.txt for use and redistribution information.
*
*******************************************************************************/

package com.persist.xact.value;

public final class VLExt extends VL {
    public Object foval;
    public long fival;
    public Object[] eovals;
    public long[] eivals;

    public VLExt() {
	vKind = VL.VEXT;
    }
}