/*******************************************************************************
*
* Copyright (c) 2002. John R. Nestor. All rights reserved.
*
* See the file XLICENSE.txt for use and redistribution information.
*
*******************************************************************************/

package com.persist.xact.system;

public final class XRString extends XR {
    String val;
    int size;
    int next;
    boolean eof;

    public XRString(String val) {
	this.val = val;
	next = 0;
	size = val.length();
	eof = false;
    }

    public int read() {
	if (eof) return -1;
	if (next >= size) {
	    return -1;
	}
	int result = val.charAt(next);
	next++;
	return result;
    }

    public int read(char buff[]) {
	int count = buff.length;
	if (eof) return -1;
	if (next >= size) {
	    return -1;
	}
	int result = size - next;
	if (count < result) result = count;
	for (int i = 0; i < result; i++) {
	    buff[i] = val.charAt(next+i);
	}
	next += result;
	return result;
    }

    public void close() {
    }
}
