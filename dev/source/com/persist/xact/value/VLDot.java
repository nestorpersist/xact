/*******************************************************************************
*
* Copyright (c) 2002. John R. Nestor. All rights reserved.
*
* See the file XLICENSE.txt for use and redistribution information.
*
*******************************************************************************/

package com.persist.xact.value;

public final class VLDot extends VL {
    public Object voval; /* VLView or VLBIV */
    public long vival;
    public Object f;	 /* function XDef or VLBIF */
    public Object selfoval;
    public long selfival;
    public Object[] eovals;
    public long[] eivals;
    public boolean hasSelf;

    public VLDot() {
	vKind = VL.VDOT;
    }
}