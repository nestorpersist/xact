/*******************************************************************************
*
* Copyright (c) 2002-2006. John R. Nestor. All rights reserved.
*
* See the file XLICENSE.txt for use and redistribution information.
*
*******************************************************************************/

package com.persist.xact.system;

import com.persist.xact.value.*;

public final class XWStream extends XW {
    private VLStream vls;

    public XWStream(VLStream vls) {
	this.vls = vls;
    }

    public void write(char ch) {
	count ++;
	char[] chars = new char[1];
	chars[0] = ch;
	write(new String(chars).intern());
    }

    public void write(String s) {
	int size1 = s.length();
	count += size1;
	if (s != "") {
	    int size = vls.q.length;
	    for (int i = 0; i < size; i++) {
		vls.q[i].send(s,0);
	    }
	}
    }

    public void flush() {
    }

    public void close() {
    }
}
