/*******************************************************************************
*
* Copyright (c) 2002. John R. Nestor. All rights reserved.
*
* See the file XLICENSE.txt for use and redistribution information.
*
*******************************************************************************/

package com.persist.xact.value;

public final class VLJMethod extends VL {
    public Object obj;
    public String name;

    public VLJMethod() {
	vKind = VL.VJMETHOD;
    }
}