/*******************************************************************************
*
* Copyright (c) 2002-2006. John R. Nestor. All rights reserved.
*
* See the file XLICENSE.txt for use and redistribution information.
*
*******************************************************************************/

package com.persist.xdom;

import java.util.*;

public final class XDOMCall extends XDOM {

    public final static byte CNORMAL = 0;
    public final static byte CDOT = 1;        // a.b
    public final static byte COP = 2;	      // a + b
    public final static byte CDOUBLE = 3;     // "..."
    public final static byte CSINGLE = 4;     // '...'
    public final static byte CLONG = 5;	      // /*...*/
    public final static byte CSHORT = 6;      // //...
    public final static byte CSUBSCRIPT = 7;  // a(...)#v
    public final static byte CLIST = 8;       // [...]

    public final static String[] CKind = {
	"normal", "dot", "op",
	"double" , "single",
	"long", "short", "subscript", "list"
    };
    
    private byte kind;
    private XDOM func;
    private ArrayList<XDOM> arg;

    /* Java Interfaces */

    public XDOMCall(XDOM func) {
	xKind = XDOM.XCALL;
	this.func = func;
    }

    public XDOMCall(XDOM func,int capacity) {
	xKind = XDOM.XCALL;
	this.func = func;
	if (capacity != 0) arg = new ArrayList<XDOM>(capacity);
    }

    public XDOM getFunc() {
	return func;
    }

    public void setFunc(XDOM func) {
	this.func = func;
    }

    public byte getKind() {
	return kind;
    }

    public void setKind(byte kind) {
	this.kind = kind;
    }

    public void clearArg() {
	arg = null;
    }

    public void clearArg(int capacity) {
	if (capacity == 0) {
	    arg = null;
	} else {
	    arg = new ArrayList<XDOM>(capacity);
	}
    }

    public int argSize() {
	if (arg == null) return 0;
	return arg.size();
    }

    public XDOM getArg(int pos) {
	return arg.get(pos-1);
    }

    public void setArg(int pos,XDOM val) {
	arg.set(pos-1,val);
    }

    public void insertArg(int pos,XDOM val) {
	if (arg == null) arg = new ArrayList<XDOM>();
	if (pos == -1) {
	    arg.add(val);
	} else {
	    arg.add(pos,val);
	}
    }

    public void deleteArg(int pos) {
	arg.remove(pos-1);
    }

    /* Special xact Interfaces */
}
