/*******************************************************************************
*
* Copyright (c) 2002-2006. John R. Nestor. All rights reserved.
*
* See the file XLICENSE.txt for use and redistribution information.
*
*******************************************************************************/

package com.persist.xdom;

import com.persist.xact.bind.*;
import java.util.*;

public final class XDOMName extends XDOM {

    public final static byte NNORMAL = 0;
    public final static byte NXML = 1;

    public final static String[] NKind = {
	"normal", "xml"
    };

    private byte kind;
    private String name;
    private ArrayList<XDOM> ext;
    public XDef def;

    /* Java Interfaces */

    public XDOMName(String name) {
	xKind = XDOM.XNAME;
	this.name = name.intern();
    }

    public XDOMName(String name,int capacity) {
	xKind = XDOM.XNAME;
	this.name = name.intern();
	ext = new ArrayList<XDOM>(capacity);
    }

    public byte getKind() {
	return kind;
    }

    public void setKind(byte kind) {
	this.kind = kind;
    }

    public String getName() {
	return name;
    }

    public void setName(String name) {
	this.name = name.intern();
    }

    public String getSpace() {
	int i = name.indexOf(':');
	if (i == -1) return "";
	return name.substring(0,i).intern();
    }

    public void setSpace(String space) {
	if (space == "") name = getBase();
	name = (space + ":" + getBase()).intern();
    }

    public String getBase() {
	int i = name.indexOf(':');
	if (i == -1) return name;
	return name.substring(i+1).intern();
    }

    public void setBase(String base) {
	String space = getSpace();
	if (space == "") name = base;
	name = (space + ":" + base).intern();
    }

    public boolean hasExt() {
	return ext != null;
    }

    public void clearExt() {
	ext = null;
    }

    public void clearExt(int capacity) {
	ext = new ArrayList<XDOM>(capacity);
    }

    public int extSize() {
	if (ext == null) return 0;
	return ext.size();
    }

    public XDOM getExt(int pos) {
	return ext.get(pos-1);
    }

    public void setExt(int pos,XDOM val) {
	ext.set(pos-1,val);
    }

    public void insertExt(int pos,XDOM val) {
	if (ext == null) ext = new ArrayList<XDOM>();
	if (pos == -1) {
	    ext.add(val);
	} else {
	    ext.add(pos,val);
	}
    }

    public void deleteExt(int pos) {
	ext.remove(pos-1);
    }

    /* Special xact Interfaces */

}
