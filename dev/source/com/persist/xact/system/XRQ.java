/*******************************************************************************
*
* Copyright (c) 2002. John R. Nestor. All rights reserved.
*
* See the file XLICENSE.txt for use and redistribution information.
*
*******************************************************************************/

package com.persist.xact.system;

import com.persist.xact.value.*;

public final class XRQ extends XR {
    VLQueue vlq;
    String val;
    int size;
    int next;
    boolean eof;

    public XRQ(VLQueue vlq) {
	this.vlq = vlq;
	val = "";
	next = 0;
	size = 0;
	eof = false;
    }

    public int read() {
	if (eof) return -1;
	if (next >= size) {
	    Object oval = vlq.receive(null);
	    if (oval == null) {
		eof = true;
		return -1;
	    }
	    val = (String) oval;
	    next = 0;
	    size = val.length();
	}
	int result = val.charAt(next);
	next++;
	return result;
    }

    public int read(char buff[]) {
	int count = buff.length;
	if (eof) return -1;
	if (next >= size) {
	    Object oval = vlq.receive(null);
	    if (oval == null) {
		eof = true;
		return -1;
	    }
	    val = (String) oval;
	    next = 0;
	    size = val.length();
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
