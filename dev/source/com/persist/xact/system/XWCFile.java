/*******************************************************************************
*
* Copyright (c) 2002-2006. John R. Nestor. All rights reserved.
*
* See the file XLICENSE.txt for use and redistribution information.
*
*******************************************************************************/

package com.persist.xact.system;

import java.io.*;

public final class XWCFile extends XWFile {
    private Writer w = null;
    private String charSet;

    public XWCFile() {
	this.charSet = "ISO-8859-1";
    }

    public XWCFile(String charSet) {
	this.charSet = charSet;
    }

    public XWCFile(OutputStream os,String charSet) {
	try {
	    OutputStreamWriter osw = new OutputStreamWriter(os,charSet);
	    w = new BufferedWriter(osw);
	} catch(Exception e) {
	}
    }

    public boolean open() {
	boolean ok = true;
	try {
	    OutputStreamWriter osw = new OutputStreamWriter(System.out,charSet);
	    w = new BufferedWriter(osw);
	} catch(Exception e) {
	    ok = false;
	}
	return ok;
    }

    public boolean open(String name,boolean append) {
	boolean ok = true;
	try {
	    FileOutputStream fs = new FileOutputStream(name,append);
	    OutputStreamWriter osw = new OutputStreamWriter(fs,charSet);
	    w = new BufferedWriter(osw);
	} catch(Exception e) {
	    ok = false;
	}
	return ok;
    }

    public void write(char ch) {
	try {
	    w.write(ch);
	} catch(Exception e) { }
    }

    public void write(String s) {
	int size = s.length();
	try {
	    w.write(s);
	} catch(Exception e) {}
    }

    public void flush() {
	try {
	    w.flush();
	} catch(Exception e) { }
    }

    public void close() {
	try {
	    w.close();
	} catch(Exception e) { }
    }
}
