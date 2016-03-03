/*******************************************************************************
*
* Copyright (c) 2002-2006. John R. Nestor. All rights reserved.
*
* See the file XLICENSE.txt for use and redistribution information.
*
*******************************************************************************/

package com.persist.xact.value;

import com.persist.xdom.*;

public final class VLCat extends VL {
    public int size;  // number of character in string
    public byte spaceBefore;
    public byte spaceAfter;
    public Object[] ovals; /* String or other VLCat */

    public VLCat(int count) {
	vKind = VL.VCAT;
	size = 0;
	ovals = new Object[count];
	spaceBefore = XDOM.EMPTY;
	spaceAfter = XDOM.EMPTY;
    }
}
