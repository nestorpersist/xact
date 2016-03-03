/*******************************************************************************
*
* Copyright (c) 2002-2006. John R. Nestor. All rights reserved.
*
* See the file XLICENSE.txt for use and redistribution information.
*
*******************************************************************************/

package com.persist.xact.value;

public class VLBIV extends VL {

    public VLBIV() {
	vKind = VL.VBIV;
    }

    public final static int Tvoid = 0;

    public final static int Tstring = 1;
    public final static int Vboolean = 2;
    public final static int Tint = 3;
    public final static int Tfloat = 4;

    public final static int Vtree = 5;
    public final static int Vrec = 6;
    public final static int Vattr = 7;
    public final static int Vbody = 8;
    public final static int Vbodyrec = 9;
    public final static int TtreeElement = 10;
    public final static int TtreeString = 11;
    public final static int TtreeInt = 12;
    public final static int TtreeFloat = 13;
    public final static int TtreeName = 14;
    public final static int TtreeCall = 15;
    public final static int TtreeValue = 16;

    public final static int Tfunc = 17;
    public final static int Tview = 18;
    public final static int Vview = 19;
    public final static int Ttype = 20;
    public final static int Tjava = 21;
    public final static int Varray = 22;
    public final static int Vlist = 23;
    public final static int Vfile = 24;
    public final static int Tthread = 25;
    public final static int Tqueue = 26;
    public final static int Tlock = 27;
    public final static int Tstream = 28;
    public final static int Vfold = 29;
    public final static int Vdate = 30;
    public final static int TLxact = 31;
    public final static int TLrender = 32;
    public final static int TLerror = 33;

    public final static String[] name = {
	"void", "string", "boolean", "int", "float",
	"xdom", "rec", "attr", "body", "bodyRec",
	"xdom:element", "xdom:string", "xdom:int", "xdom:float",
	"xdom:name", "xdom:call", "xdom:value",
	"func", "view", "View", "type", "Type[java]",
	"array", "list", "file", "thread", "queue", "lock",
	"stream", "fold", "date",
	"langtype:xact", "langtype:render", "langtype:error"
    };
}
