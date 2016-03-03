/*******************************************************************************
*
* Copyright (c) 2002-2006. John R. Nestor. All rights reserved.
*
* See the file XLICENSE.txt for use and redistribution information.
*
*******************************************************************************/

package com.persist.xact.system;

import java.io.*;

public final class XRCFile extends XRFile {
    private Reader r = null;
    private String charSet;

    public XRCFile() {
	this.charSet = "ISO-8859-1";
    }

    public XRCFile(String charSet) {
	this.charSet = charSet;
    }

    public boolean open() {
	boolean ok = true;
	try {
	    InputStreamReader isr = new InputStreamReader(System.in,charSet); 
	    r = new BufferedReader(isr);
	} catch(Exception e) {
	    ok = false;
	}
	return ok;
    }

    public boolean open(String name) {
	boolean ok = true;
	try {
	    FileInputStream fs = new FileInputStream(name);
	    InputStreamReader isr = new InputStreamReader(fs,charSet); 
	    r = new BufferedReader(isr);
	} catch(Exception e) {
	    ok = false;
	}
	return ok;
    }

    public int read() {
	int result = -1;
	try {
	    result = r.read();
	} catch(Exception e) { }
	return result;
    }

    public int read(char buff[]) {
	int result = 0;
	try {
	    result = r.read(buff);
	} catch(Exception e) {}
	return result;
    }

    public void close() {
	try {
	    r.close();
	} catch(Exception e) { }
    }
}
