/*******************************************************************************
*
* Copyright (c) 2002. John R. Nestor. All rights reserved.
*
* See the file XLICENSE.txt for use and redistribution information.
*
*******************************************************************************/

package com.persist.xact.system;

import com.persist.xact.value.*;

public final class XWQ extends XW {
    private VLQueue vlq;

    public XWQ(VLQueue vlq) {
	this.vlq = vlq;
    }

    public void write(char ch) {
	count ++;
	char[] chars = new char[1];
	chars[0] = ch;
	vlq.send(new String(chars).intern(),0);
    }

    public void write(String s) {
	int size = s.length();
	count += size;
	if (s != "") vlq.send(s,0);
    }

    public void flush() {
    }

    public void close() {
	vlq.send(null,0);
    }
}
