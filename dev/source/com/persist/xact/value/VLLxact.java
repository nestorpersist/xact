/*******************************************************************************
*
* Copyright (c) 2002-2006. John R. Nestor. All rights reserved.
*
* See the file XLICENSE.txt for use and redistribution information.
*
*******************************************************************************/

package com.persist.xact.value;

public final class VLLxact extends VLL {
    public Object underLang;
    public boolean doElse = false;
    public VLLxact(Object under) {
	vKind = VL.VLXACT;
	underLang = under;
    }
}
