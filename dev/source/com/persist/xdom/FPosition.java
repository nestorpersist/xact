/*******************************************************************************
*
* Copyright (c) 2002-2006. John R. Nestor. All rights reserved.
*
* See the file XLICENSE.txt for use and redistribution information.
*
*******************************************************************************/

package com.persist.xdom;

public final class FPosition {
    public final static byte FSCRIPT  = 0; // script path
    public final static byte FFILE = 1; // file path
    public final static byte FOTHER = 2; // other descriptive string

    public byte kind;
    public String name;

    public FPosition(byte kind, String name) {
	this.kind = kind;
	this.name = name;
    }
}