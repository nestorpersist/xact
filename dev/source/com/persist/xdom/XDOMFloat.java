/*******************************************************************************
*
* Copyright (c) 2002-2005. John R. Nestor. All rights reserved.
*
* See the file XLICENSE.txt for use and redistribution information.
*
*******************************************************************************/

package com.persist.xdom;

public final class XDOMFloat extends XDOM {
    private double num;

    /* Java Interfaces */

    public static boolean isFloat(String s) {
	boolean before = false;
	boolean dot = false;
	boolean after = false;
	boolean e = false;
	boolean exp = false;
	int size = s.length();
	int i = 0;
	if (i < size) {
	    char ch = s.charAt(i);
	    if (ch == '-') i ++;
	}
	while (i < size) {
	    char ch = s.charAt(i);
	    if ('0' <= ch && ch <= '9') {
		before = true;
		i ++;
	    } else {
		break;
	    } 
	}
	if (i < size) {
	    char ch = s.charAt(i);
	    if (ch == '.') {
		dot = true;
		i ++;
	    }
	}
	while (i < size) {
	    char ch = s.charAt(i);
	    if ('0' <= ch && ch <= '9') {
		after = true;
		i ++;
	    } else {
		break;
	    } 
	}
	if (i < size) {
	    char ch = s.charAt(i);
	    if (ch == 'e' || ch == 'E') {
		e = true;
		i ++;
	    }
	}
	if (i < size) {
	    char ch = s.charAt(i);
	    if (ch == '+' || ch == '-') {
		i ++;
	    }
	}
	while (i < size) {
	    char ch = s.charAt(i);
	    if ('0' <= ch && ch <= '9') {
		exp = true;
		i ++;
	    } else {
		break;
	    } 
	}
	if (i != size) return false;
	if (! dot && ! e) return false; 
	if (! before && ! after) return false;
	if (e && ! exp) return false;
	return true;
    }

    public static double toFloat(String s) {
	Double d = new Double(s);
	return d.doubleValue();
    }

    public static String toString(double fval) {
	return "" + fval;
    }

    public XDOMFloat(String s) {
	xKind = XDOM.XFLOAT;
	setVal(s);
    }

    public XDOMFloat(double fval) {
	xKind = XDOM.XFLOAT;
	setVal(fval);
    }

    public void setVal(String s) {
	if (isFloat(s)) {
	    num = toFloat(s);
	} else {
	    num = 0.0;
	}
    }

    public void setVal(double fval) {
	num = fval;
    }

    public String getString() {
	return toString(num);
    }

    public double getFloat() {
	return num;
    }

}
