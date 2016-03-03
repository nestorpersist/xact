/*******************************************************************************
*
* Copyright (c) 2002. John R. Nestor. All rights reserved.
*
* See the file XLICENSE.txt for use and redistribution information.
*
*******************************************************************************/

package com.persist.xdom;

public final class XDOMString extends XDOM {

    public final static byte SDOUBLE = 0;   // "..."
    public final static byte SXML = 1;      // text inside xml tags
    public final static byte SSINGLE = 2;   // '...'
    public final static byte SCOMMENT = 3;  // <!-- ... -->
    
    public final static String[] SKind = {
	"double", "xml", "single", "comment"
    };

    private byte kind;
    private Object oval;
    private long ival;

    /* Java Interfaces */

    public XDOMString(String s) {
	xKind = XDOM.XSTRING;
	oval = s.intern();
	ival = 0;
    }

    public byte getKind() {
	return kind;
    }

    public void setKind(byte kind) {
	this.kind = kind;
    }

    public void setVal(String s) {
	oval = s.intern();
	ival = 0;
    }

    public String getVal() {
	return XDOMValue.getString(oval,ival);
    }

    /* Special xact Interfaces */
    
    public XDOMString(Object oval,long ival) {
	xKind = XDOM.XSTRING;
	this.oval = oval;
	this.ival = ival;
    }

    public void setVal(Object oval,long ival) {
	/* assume XDOMValue.isString(oval,ival) */
	this.oval = oval;
	this.ival = ival;
    }

    public Object getOval() {
	return oval;
    }

    public long getIval() {
	return ival;
    }
}
