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

public final class XDOMElement extends XDOM {

    public final static int hashSize = 20;  // min number of attrs to set up hashmap
    public final static byte ENORMAL = 0;
    public final static byte EXML = 1;       
    public final static byte EITERATOR = 2;  // @x:for ... { ... }
    public final static byte EXMLDECL = 3;   // <?xml ... ?>
    public final static byte ESHORT = 4;     // name://...
    public final static byte ELONG = 5;      // name:/* ... */

    public final static String[] EKind = {
	"normal" , "xml", "iterator", "xmldecl", "short", "long"
    };

    private String tag;
    private String space;
    private String base;
    private byte kind;
    private HashMap<String,XDOM> nattr;
    private ArrayList<XDOM> attr;
    private ArrayList<XDOM> body;

    public XScope scope;
    private byte spaceBeforeBody;
    private byte spaceAfterBody;

    private void setSpaceBase() {
	int size = tag.length();
	int i = tag.indexOf(':');
	if (i == -1) {
	    space = "";
	    base = tag;
	} else {
	    space = tag.substring(0,i).intern();
	    base = tag.substring(i+1).intern();
	}
    }

    private static String getString(XDOM x) {
	int kind = x.xKind;
	Object oval;
	long ival;
	if (kind == XSTRING) {
	    XDOMString xs = (XDOMString) x;
	    oval = xs.getOval();
	    ival = xs.getIval();
	} else if (kind == XVALUE) {
	    XDOMValue xv = (XDOMValue) x;
	    oval = xv.getOval();
	    ival = xv.getIval();
	} else {
	    return null;
	}
	if (XDOMValue.isString(oval,ival)) {
	    return XDOMValue.getString(oval,ival);
	}
	return null;

    }

    private static String attrName(XDOM x) {
	if (x.xKind == XDOM.XCALL) {
	    XDOMCall xc = (XDOMCall) x;
	    XDOM func = xc.getFunc();
	    if (func.xKind == XDOM.XNAME) {
		XDOMName fn = (XDOMName) func;
		if (fn.getName() == "Equal" && xc.argSize() == 2) {
		    XDOM arg1 = xc.getArg(1);
		    if (arg1.xKind == XDOM.XNAME) {
			XDOMName xn = (XDOMName) arg1;
			return xn.getName();
		    }
		}
	    }
	}
	return null;
    }
    
    private static XDOM nameVal(XDOM x) {
	XDOMCall xc = (XDOMCall) x;
	return xc.getArg(2);
    }

    private static XDOM setVal(XDOM x,Object oval,long ival) {
	if (XDOMValue.isString(oval,ival)) {
	    if (x.xKind == XDOM.XSTRING) {
		XDOMString xs = (XDOMString) x;
		xs.setVal(oval,ival);
		return x;
	    } else {
		XDOMString xs = new XDOMString(oval,ival);
		return xs;
	    }
	} else if (XDOMValue.isInt(oval,ival)) {
	    long val = XDOMValue.getInt(oval,ival);
	    if (x.xKind == XDOM.XINT) {
		XDOMInt xi = (XDOMInt) x;
		xi.setVal(val);
		return x;
	    } else {
		XDOMInt xi = new XDOMInt(val);
		return xi;
	    }
	} else if (XDOMValue.isFloat(oval,ival)) {
	    double val = XDOMValue.getFloat(oval,ival);
	    if (x.xKind == XDOM.XFLOAT) {
		XDOMFloat xf = (XDOMFloat) x;
		xf.setVal(val);
		return x;
	    } else {
		XDOMFloat xf = new XDOMFloat(val);
		return xf;
	    }
	} else {
	    if (x.xKind == XDOM.XVALUE) {
		XDOMValue xv = (XDOMValue) x;
		xv.setVal(oval,ival);
		return xv;
	    } else {
		XDOMValue xv = new XDOMValue(oval,ival);
		return xv;
	    }
	}
    }

    private static void setNameVal(XDOM x,Object oval,long ival) {
	if (x.xKind == XDOM.XCALL) {
	    XDOMCall xc = (XDOMCall) x;
	    XDOM func = xc.getFunc();
	    if (func.xKind == XDOM.XNAME) {
		XDOMName fn = (XDOMName) func;
		if (fn.getName() == "Equal" && xc.argSize() == 2) {
		    XDOM arg1 = xc.getArg(2);
		    XDOM x1 = setVal(arg1,oval,ival);
		    if (x1 != arg1) {
			xc.setArg(2,x1);
		    }
		    return;
		}
	    }
	}
    }

    private XDOM newVal(Object oval,long ival) {
	if (XDOMValue.isString(oval,ival)) {
	    XDOMString xs = new XDOMString(oval,ival);
	    return xs;
	} else if (XDOMValue.isInt(oval,ival)) {
	    XDOMInt xi = new XDOMInt(ival);
	    return xi;
	} else if (XDOMValue.isFloat(oval,ival)) {
	    double val = XDOMValue.getFloat(oval,ival);
	    XDOMFloat xf = new XDOMFloat(val);
	    return xf;
	} else {
	    XDOMValue xv = new XDOMValue(oval,ival);
	    return xv;
	}
	
    }
    private XDOM newName(String name,Object oval,long ival) {
	XDOMName xn = new XDOMName(name);
	XDOMName xequal = new XDOMName("Equal");
	XDOMCall xc = new XDOMCall(xequal,2);
	xc.setKind(XDOMCall.COP);
	xc.insertArg(-1,xn);
	xc.insertArg(-1,newVal(oval,ival));
	return xc;
    }
    
    /* Java Interfaces */

    public XDOMElement(String tag) {
	xKind = XDOM.XELEMENT;
	this.tag = tag.intern();
	setSpaceBase();
    }

    public XDOMElement(String tag,int acapacity,int bcapacity) {
	xKind = XDOM.XELEMENT;
	this.tag = tag.intern();
	setSpaceBase();
	if (acapacity != 0) attr = new ArrayList<XDOM>(acapacity);
	if (bcapacity != 0) body = new ArrayList<XDOM>(bcapacity);
    }

    public String getTag() {
	return tag;
    }

    public void setTag(String tag) {
	this.tag = tag.intern();
	setSpaceBase();
    }

    public String getSpace() {
	return space;
    }

    public void setSpace(String space) {
	this.space = space.intern();
	tag = (space + ":" + base).intern();
    }

    public String getBase() {
	return base;
    }

    public void setBase(String base) {
	this.base = base.intern();
	tag = (space + ":" + base).intern();
    }
    
    public byte getKind() {
	return kind;
    }

    public void setKind(byte kind) {
	this.kind = kind;
    }

    public byte getSpaceBeforeBody() {
	return spaceBeforeBody;
    }

    public void setSpaceBeforeBody(byte val) {
	spaceBeforeBody = val;
    }

    public byte getSpaceAfterBody() {
	return spaceAfterBody;
    }

    public void setSpaceAfterBody(byte val) {
	spaceAfterBody = val;
    }

    /* attributes operations */

    public void clearAttr() {
	attr = null;
    }

    public void clearAttr(int capacity) {
	if (capacity == 0) {
	    attr = null;
	} else {
	    attr = new ArrayList<XDOM>(capacity);
	}
    }

    public int attrSize() {
	if (attr == null) return 0;
	return attr.size();
    }

    public XDOM getAttr(int pos) {
	return attr.get(pos-1);

    }

    public void setAttr(int pos,XDOM val) {
	attr.set(pos-1,val);
    }

    public void insertAttr(int pos,XDOM val) {
	if (attr == null) attr = new ArrayList<XDOM>();
	if (pos == -1) {
	    attr.add(val);
	} else {
	    attr.add(pos,val);
	}
    }

    public void deleteAttr(int pos) {
	attr.remove(pos-1);
    }

    /* body operations */

    public void clearBody() {
	body = null;
    }

    public void clearBody(int capacity) {
	if (capacity == 0) {
	    body = null;
	} else {
	    body = new ArrayList<XDOM>(capacity);
	}
    }

    public int bodySize() {
	if (body == null) return 0;
	return body.size();
    }

    public XDOM getBody(int pos) {
	return body.get(pos-1);
    }

    public void setBody(int pos,XDOM val) {
	body.set(pos-1,val);
    }

    public void insertBody(int pos,XDOM val) {
	if (body == null) body = new ArrayList<XDOM>();
	if (pos == -1) {
	    body.add(val);
	} else {
	    body.add(pos,val);
	}
    }

    public void deleteBody(int pos) {
	body.remove(pos-1);
    }

    /* record operations */

    public String getRec(String name) {
	XDOM x = getERec(name.intern());
	if (x == null) return null;
	return getString(x);
    }

    public void setRec(String name,String val) {
	String val1 = val;
	if (val1 != null) val1 = val.intern();
	setERec(name.intern(),val1,0);
    }
	    
    /* array operations */

    public String getArray(int i) {
	XDOM x = getEArray(i);
	return getString(x);
    }

    public void setArray(int i,String val) {
	String val1 = val;
	if (val1 != null) val1 = val.intern();
	setEArray(i,val1,0);
    }

    public int arraySize() {
	int result = 0;
	int size = attrSize();
	for (int i = 1; i <= size; i++) {
	    XDOM elem = getAttr(i);
	    if (attrName(elem) == null) result ++;
	}
	return result;
    }

    public void insertArray(int pos,String val) {
	String val1 = val;
	if (val1 != null) val1 = val.intern();
	insertEArray(pos,val1,0);
    }

    public void deleteArray(int pos) {
	int pos1 = 0;
	int size = attrSize();
	for (int i = 1; i <= size; i++) {
	    XDOM elem = getAttr(i);
	    if (attrName(elem) == null) {
		pos1 ++;
		if (pos == pos1) {
		    deleteAttr(i);
		    return;
		}
	    }
	}
    }
    
    /* body record operations */

    public XDOMElement getBodyRec(String name) {
	String iname = name.intern();
	int size = bodySize();
	for (int i = 1; i <= size; i++) {
	    XDOM x = getBody(i);
	    if (x.xKind == XDOM.XELEMENT) {
		XDOMElement xe = (XDOMElement) x;
		if (xe.getTag() == iname) {
		    return xe;
		}
	    }
	}
	return null;
    }

    public void setBodyRec(String name,XDOMElement elem) {
	String iname = name.intern();
	int size = bodySize();
	for (int i = 1; i <= size; i++) {
	    XDOM x = getBody(i);
	    if (x.xKind == XDOM.XELEMENT) {
		XDOMElement xe = (XDOMElement) x;
		if (xe.getTag() == iname) {
		    if (elem == null) {
			deleteBody(i);
		    } else {
			setBody(i,elem);
		    }
		    return;
		}
	    }
	}
	insertBody(-1,elem);
	return;
    }
    
    /* Special xact Interfaces */

    private void nattrSetup() {
	int size = attrSize();
	nattr = new HashMap<String,XDOM>(size);
	for (int i = 1; i <= size; i++) {
	    XDOM elem = getAttr(i);
	    String name = attrName(elem);
	    if (name != null) {
		nattr.put(name,elem);
	    }
	}
    }

    public XDOM getERec(String name) {
	int size = attrSize();
	if (nattr != null) {
	    XDOM elem1 = nattr.get(name);
	    if (elem1 == null) return null;
	    return nameVal(elem1);
	}
	for (int i = 1; i <= size; i++) {
	    XDOM elem = getAttr(i);
	    if (attrName(elem) == name) {
		return nameVal(elem);
	    }
	}
	return null;
    }

    public void setERec(String name,Object oval,long ival) {
	int size = attrSize();
	if (nattr == null && size > hashSize) nattrSetup();
	if (nattr != null) {
	    XDOM elem1 = nattr.get(name);
	    if (oval == null) {
		// delete attr
		if (elem1 != null) {
		    nattr.remove(name);
		    attr.remove(elem1);
		}
	    } else if (elem1 != null) {
		// change attr 
		setNameVal(elem1,oval,ival);
	    } else {
		// add attr
		elem1 = newName(name,oval,ival);
		insertAttr(-1,elem1);
		nattr.put(name,elem1);
	    }
	    return;
	}
	for (int i = 1; i <= size; i++) {
	    XDOM elem = getAttr(i);
	    if (attrName(elem) == name) {
		if (oval == null) {
		    deleteAttr(i);
		} else {
		    setNameVal(elem,oval,ival);
		}
		return;
	    }
	}
	if (oval != null) {
	    XDOM elem1 = newName(name,oval,ival);
	    insertAttr(-1,elem1);
	}
    }

    public XDOM getEArray(int pos) {
	int pos1 = 0;
	int size = attrSize();
	for (int i = 1; i <= size; i++) {
	    XDOM elem = getAttr(i);
	    if (attrName(elem) == null) {
		pos1 ++;
		if (pos == pos1) return elem;
	    }
	}
	return null;
    }

    public void setEArray(int pos,Object oval,long ival) {
	int pos1 = 0;
	int size = attrSize();
	for (int i = 1; i <= size; i++) {
	    XDOM elem = getAttr(i);
	    if (attrName(elem) == null) {
		pos1 ++;
		if (pos1 == pos) {
		    XDOM elem1 = setVal(elem,oval,ival);
		    if (elem1 != elem) {
			setAttr(i,elem1);
		    }
		    return;
		}
	    }
	}
    }

    public void insertEArray(int pos,Object oval,long ival) {
	XDOM val = newVal(oval,ival);
	if (pos != -1) {
	    int pos1 = 0;
	    int size = attrSize();
	    for (int i = 1; i <= size; i++) {
		XDOM elem = getAttr(i);
		if (attrName(elem) == null) {
		    pos1 ++;
		    if (pos == pos1) {
			insertAttr(i,val);
			return;
		    }
		}
	    }
	}
	insertAttr(-1,val);
    }
}
