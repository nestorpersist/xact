/*******************************************************************************
*
* Copyright (c) 2002. John R. Nestor. All rights reserved.
*
* See the file XLICENSE.txt for use and redistribution information.
*
*******************************************************************************/

package com.persist.xact.system;

public final class XWEncode extends XW {
    private final static int SIZE = 200;
    private XW w = null;
    private StringBuffer sbuff = new StringBuffer(SIZE);
    private int size = 0;

    public XWEncode(XW w) {
	this.w = w;
    }

    public void flush() {
	w.write(sbuff.toString());
	size = 0;
	sbuff.setLength(0);
	w.flush();
    }

    public void write(char ch1) {
	char ch = ch1;
	String rest = null;
	count ++;
	if (size == SIZE) {
	    flush();
	}
	if (ch == '\240') {
	    ch = '&';
	    rest = "nbsp";
	} else if (ch < 32 & ch != 10 & ch != 13) {
	    ch = '&';
	    rest = "#" + ch;
	} else if (ch >= 127) {
	    ch = '&';
	    rest = "#" + ch;
	}
	sbuff.append(ch);
	size ++;
	if (rest != null) {
	    write(rest);
	    write(';');
	}
    }
    
    public void write(String s) {
	int size1 = s.length();
	count += size1;
	for (int i = 0; i < size1; i++) {
	    write(s.charAt(i));
	}
    }

    public void close() {
	flush();
	w.close();
    }
}
