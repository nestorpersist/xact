/*******************************************************************************
*
* Copyright (c) 2002-2006. John R. Nestor. All rights reserved.
*
* See the file XLICENSE.txt for use and redistribution information.
*
*******************************************************************************/

package com.persist.xact.value;

public abstract class VL {

    public final static byte VBOOL = 0;
    public final static byte VINT = 1;
    public final static byte VFLOAT = 2;
    public final static byte VDATE = 3;
    public final static byte VCAT = 4;
    public final static byte VCHAR = 5;

    public final static byte VBIF = 6;
    public final static byte VBIV = 7;
    public final static byte VBIT = 8;
    public final static byte VEXT = 9;
    public final static byte VDOT = 10;
    public final static byte VFUNC = 11;
    public final static byte VTYPE = 12;
    public final static byte VVIEW = 13;

    public final static byte VTHREAD = 14;
    public final static byte VQUEUE = 15;
    public final static byte VSTREAM = 16;
    public final static byte VLOCK = 17;

    public final static byte VJAVA = 18;
    public final static byte VOBJ = 19;
    public final static byte VJMETHOD = 20;

    public final static byte VLXACT = 21;
    public final static byte VLRENDER = 22;
    public final static byte VLERROR = 23;

    public final static String[] XKind = {
	"bool", "int", "float", "date", "cat", "char",
	"BIfunc", "BIview", "BItype", "ext", "dot",
	"func", "view", "type",
	"thread", "queue", "stream", "lock",
	"java", "obj", "jmethod",
	"langtype:xact", "langtype:render", "langtype:error"
    };
    
    protected byte vKind;

    public byte getVKind() {
	return vKind;
    }
}
