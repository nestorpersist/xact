/*******************************************************************************
*
* Copyright (c) 2002-2006. John R. Nestor. All rights reserved.
*
* See the file XLICENSE.txt for use and redistribution information.
*
*******************************************************************************/

package com.persist.xact.value;

public final class VLJava extends VL {
    public String pkg;
    public VLJava() {
	vKind = VL.VJAVA;
	pkg = "";
    }
}
