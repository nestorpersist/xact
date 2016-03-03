/*******************************************************************************
*
* Copyright (c) 2002-2006. John R. Nestor. All rights reserved.
*
* See the file XLICENSE.txt for use and redistribution information.
*
*******************************************************************************/

package com.persist.xact.system;

import java.io.*;

public final class XWBFile extends XWFile {
    private OutputStream st = null;

    public XWBFile() {
    }

    public boolean open() {
	boolean ok = true;
	try {
	    st = new BufferedOutputStream(System.out);
	} catch(Exception e) {
	    ok = false;
	}
	return ok;
    }

    public XWBFile(OutputStream os) {
	try {
	    st = new BufferedOutputStream(System.out);
	} catch(Exception e) {
	}
    }

    public boolean open(String name,boolean append) {
	boolean ok = true;
	try {
	    FileOutputStream fs = new FileOutputStream(name,append);
	    st = new BufferedOutputStream(fs);
	} catch(Exception e) {
	    ok = false;
	}
	return ok;
    }

    public void write(char ch) {
	try {
	    st.write(ch);
	} catch(Exception e) { }
    }

    public void write(String s) {
	int size = s.length();
	for (int i = 0; i < s.length(); i++) {
	    write(s.charAt(i));
	}
    }

    public void flush() {
	try {
	    st.flush();
	} catch(Exception e) { }
    }

    public void close() {
	try {
	    st.close();
	} catch(Exception e) { }
    }
}
