/*******************************************************************************
*
* Copyright (c) 2002-2005. John R. Nestor. All rights reserved.
*
* See the file XLICENSE.txt for use and redistribution information.
*
*******************************************************************************/

package com.persist.xdom;

public final class XDOMInt extends XDOM {
    private long num;

    /* Java Interfaces */

    public static boolean isInt(String s) {
	boolean hasVal = false;
	int size = s.length();
	int i = 0;
	if (i < size) {
	    char ch = s.charAt(i);
	    if (ch == '-') i ++;
	}
	while (i < size) {
	    char ch = s.charAt(i);
	    if ('0' <= ch && ch <= '9') {
		i ++;
		hasVal = true;
	    } else {
		break;
	    }
	}
	if (i != size) return false;
	if (! hasVal) return false;
	return true;
    }

    public static long toInt(String s) {
	int size = s.length();
	boolean neg = false;
	long val = 0;
	int i;
	for (i = 0; i < size; i++) {
	    char ch = s.charAt(i);
	    if (ch == '-') {	 
		neg = true;
	    } if ('0' <= ch && ch <= '9') {
		val = val * 10 + (ch-'0');
	    }
	}
	if (neg) val = - val;
	return val;
    }

    public static String toString(long ival) {
	return Long.toString(ival);
    }

    public XDOMInt(String s) {
	xKind = XDOM.XINT;
	setVal(s);
    }

    public XDOMInt(long ival) {
	xKind = XDOM.XINT;
	setVal(ival);
    }

    public void setVal(String s) {
	if (isInt(s)) {
	    num = toInt(s);
	} else {
	    num = 0;
	}
    }

    public void setVal(long ival) {
	num = ival;
    }

    public String getString() {
	return toString(num);
    }

    public long getInt() {
	return num;
    }
}
