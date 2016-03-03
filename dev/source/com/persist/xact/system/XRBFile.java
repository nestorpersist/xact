/*******************************************************************************
*
* Copyright (c) 2006. John R. Nestor. All rights reserved.
*
* See the file XLICENSE.txt for use and redistribution information.
*
*******************************************************************************/

package com.persist.xact.system;

import java.io.*;

public final class XRBFile extends XRFile {
    private InputStream st = null;

    public boolean open() {
	st = System.in;
	return true;
    }

    public boolean open(String name) {
	boolean ok = true;
	try {
	    FileInputStream fs = new FileInputStream(name);
	    st = new BufferedInputStream(fs);
	} catch(Exception e) {
	    ok = false;
	}
	return ok;
    }

    public int read() {
	int result = -1;
	try {
	    result = st.read();
	} catch(Exception e) { }
	return result;
    }

    public int read(char buff[]) {
	int result = -1;
	try {
	    for (int i = 0; i < buff.length; i++) {
		int b = read();
		if (b == -1) {
		    if (i == 0) return -1;
		    return i;
		}	
		buff[i] = (char) b; 
		result = i + 1;
	    }
	} catch(Exception e) {}
	return result;
    }

    public void close() {
	try {
	    st.close();
	} catch(Exception e) { }
    }
}
