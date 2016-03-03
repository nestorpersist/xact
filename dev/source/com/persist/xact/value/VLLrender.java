/*******************************************************************************
*
* Copyright (c) 2002-2006. John R. Nestor. All rights reserved.
*
* See the file XLICENSE.txt for use and redistribution information.
*
*******************************************************************************/

package com.persist.xact.value;

public final class VLLrender extends VLL {
    public static final byte KXML = 0;
    public static final byte KXACT = 1;
    public static final byte KASIS = 2;

    public static String kinds[] = { "xml", "xact", "asis"};

    public byte kind;
    public static byte findKind(String kind) {
	for (byte i = 0; i < kinds.length; i++) {
	    if (kind == kinds[i]) return i;
	}
	return -1;
    }

    public VLLrender(byte kind) {
	this.kind = kind;
	vKind = VL.VLRENDER;
    }
}
