/*******************************************************************************
*
* Copyright (c) 2002-2007. John R. Nestor. All rights reserved.
*
* See the file XLICENSE.txt for use and redistribution information.
*
*******************************************************************************/

package com.persist.xact.value;

import java.util.*;
import com.persist.xact.system.*;
import com.persist.xdom.*;

public final class Value {
    /*
      FORM		OVAL	IVAL				TYPE

      novalue		null	VNOVAL				error   
      null		null	VNULL				void
      error		null	VERROR				error
      uninit		null    VUNINIT				uninit
     integer		VLInt	val (long 64 bit int)		int
      float		VLFloat	val (double as 64 bit int)	float
      string		String	0				string
         boolean	VLBool	0 or 1				string
         date           VLDate  java date val (gmt)		string
 	 cat		VLCat	0				string
 	 1 char		VLChar	code				string
      tree		XDOM	0				xdom:*	
      bi func		VLBIF   0				func
      bi view           VLBIV   index				view
      bi type           VLBIT   index				type
      func		VLFunc	0				func
      type		VLType  0				type
      view		VLView  0				view
      thread		VLThread0				thread
      queue		VLQueue 0				queue
      stream		VLStream0				stream
      lock		VLLock  0				lock
      xact lang	obj	VLLxact 0			        langtype:xact
      render lang obj	VLLrender 0			        langtype:render
      error lang obj	VLLerror 0			        langtype:error
      obj		VLObj	0				t
      f[...]		VLExt   0				func
      x.f		VLDot	0				func
      Java		VLJava  0				Type[java]
      Java Obj		Obj	-1				Java Class
      Java Method       VLJMethod 0				func
    */

    public final static int VNOVAL = -1;
    public final static int VNULL = 0;
    public final static int VERROR = 1;
    public final static int VUNINIT = 2;
    
    public Object vlChar;
    public Object vlBool;
    public VLDate gmtDate;
    public VLDate localDate;
    public Object vlInt;
    public Object vlFloat;
    public Object vlBIV;
    public Object vlBIT;
    public Object vlJava;

    public Ctx ctxEval;
    public Ctx ctxAssign;

    private Vector dates;

    public VLDate makeVLDate(TimeZone tz) {
	if (tz == VLDate.gmtTZ) return gmtDate;
	if (tz == VLDate.localTZ) return localDate;
	VLDate vld = null;
	synchronized(this) {
	    int size = dates.size();
	    for (int i = 0; i < size; i++) {
		VLDate vld1 = (VLDate) (dates.elementAt(i));
		if (vld1.tz == tz) {
		    vld = vld1;
		    break;
		}
	    }
	    if (vld == null) {
		vld = new VLDate(tz);
		dates.addElement(vld);
	    }
	}
	return vld;
    }

    public Value() {
	vlChar = new VLChar();
	vlBool = new VLBool();
	vlInt = new VLInt();
	gmtDate = new VLDate(VLDate.gmtTZ);
	localDate = new VLDate(VLDate.localTZ);
	vlFloat = new VLFloat();
	vlBIV = new VLBIV();
	vlBIT = new VLBIT();
	vlJava = new VLJava();
	ctxEval = new CtxEval();
	ctxAssign = new CtxAssign();
	dates = new Vector();
    }

    public String escapeXMLTxt(String s) {
	int size = s.length();
	int first = 0;
	String result = "";
	for (int i = 0; i < size; i++) {
	    char ch = s.charAt(i);
	    String alt = "";
	    if (ch == '&') {
		alt = "&amp;";
	    } else if (ch == '<') {
		alt = "&lt;";
	    } else if (ch == '>') {
		alt = "&gt;";
	    }
	    if (alt != "") {
		if (i > first) {
		    result = result + s.substring(first,i);
		}
		result = result + alt;
		first = i+1;
	    }
	}
	if (first == 0) {
	    return s;
	} else {
	    if (first < size) {
		result = result + s.substring(first,size);
	    }
	    return result.intern();
	}
    }

    // ??? obsolete
    public String escapeString(String s,char kind) {
	int size = s.length();
	int first = 0;
	String result = "";
	for (int i = 0; i < size; i++) {
	    char ch = s.charAt(i);
	    String alt = "";
	    if (kind == '"' && ch == '"') {
		alt = "&quot;";
	    } else if (kind != ' ' && ch == '&') {
		alt = "&amp;";
	    } else if (kind == ' ' && ch == '<') {
		alt = "&lt;";
	    } else if (kind == ' ' && ch == '>') {
		alt = "&gt;";
	    } else if (ch == '\240') {
		alt = "&nbsp;";
	    } else if (ch == 10 || ch == 13) {
	    } else if (ch < 32 | 126 < ch) {
		alt = "&#" + (int)(ch) + ";";
	    }
	    if (alt != "") {
		if (i > first) {
		    result = result + s.substring(first,i);
		}
		result = result + alt;
		first = i+1;
	    }
	}
	if (first == 0) {
	    return s;
	} else {
	    if (first < size) {
		result = result + s.substring(first,size);
	    }
	    return result.intern();
	}
    }

    private void printVal1(XW w,Object oval,long ival) {
	if (oval == null) {
	} else if (oval instanceof VLCat) {
	    VLCat vlc = (VLCat) oval;
	    int size = vlc.ovals.length;
	    for (int i = 0; i < size; i++) {
		printVal1(w,vlc.ovals[i],0);
	    }
	} else if (XDOMValue.isString(oval,ival)) {
	    w.write(XDOMValue.getString(oval,ival));
	} else {
	    w.write("?"+oval.toString());
	}
    }

    public void printVal(XW w,Object oval,long ival) {
	if (oval != null && oval instanceof VLCat) {
	    VLCat vlc = (VLCat) oval;
	    w.write(XDOM.spaceString[vlc.spaceBefore]);
	    printVal1(w,oval,ival);
	    w.write(XDOM.spaceString[vlc.spaceAfter]);
	} else {
	    printVal1(w,oval,ival);
	}
    }

    public boolean isError(Object oval,long ival) {
	return oval == null && ival == VERROR;
    }

    public boolean isNull(Object oval,long ival) {
	return oval == null && ival == VNULL;
    }

    public boolean isUninit(Object oval,long ival) {
	return oval == null && ival == VUNINIT;
    }

    public boolean isBoolean(Object oval,long ival) {
	if (oval == vlBool) {
	    return true;
	} else if (oval instanceof String || oval instanceof VLCat) {
	    return VLBool.isBool(oval);
	}
	return false;
    }

    public boolean isDate(Object oval,long ival) {
	if (oval instanceof VLDate) {
	    return true;
	} else if (XDOMValue.isString(oval,ival)) {
	    String s = XDOMValue.getString(oval,ival);
	    return VLDate.isDate(s);
	}
	return false;
    }

    public boolean isInt(Object oval,long ival) {
	if (oval instanceof VLInt) {
	    return true;
	}
	return false;
    }


    public boolean isFloat(Object oval,long ival) {
	if (oval instanceof VLInt) {
	    return true;
	} else if (oval instanceof VLFloat) {
	    return true;
	}
	return false;
    }

    public boolean isFunc(Object oval,long ival) {
	if (oval instanceof VL) {
	    VL vl = (VL) oval;
	    byte vkind = vl.getVKind();
	    if (vkind == VL.VFUNC) return true;
	    if (vkind == VL.VEXT) return true;
	    if (vkind == VL.VDOT) return true;
	    if (vkind == VL.VBIF) return true;
	    if (vkind == VL.VJMETHOD) return true;
	}
	return false;
    }

    public boolean isView(Object oval,long ival) {
	if (oval instanceof VL) {
	    VL vl = (VL) oval;
	    byte kind = vl.getVKind();
	    if (kind == VL.VVIEW) return true;
	    if (kind == VL.VBIV) return true;
	}
	return false;
    }

    public boolean isType(Object oval,long ival) {
	if (oval instanceof VL) {
	    VL vl = (VL) oval;
	    byte kind = vl.getVKind();
	    if (kind == VL.VTYPE) return true;
	    if (kind == VL.VBIT) return true;
	}
	return false;
    }

    public boolean isJObject(Object oval,long ival) {
	if (oval == null) return false;
	if (ival != -1) return false;
	if (oval instanceof VL) {
	    VL vl = (VL) oval;
	    byte kind = vl.getVKind();
	    if (kind == VL.VINT) return false;
	    if (kind == VL.VFLOAT) return false;
	    if (kind == VL.VDATE) return false;
	}
	return true;
    }

    public static int stringSize(Object oval) {
	if (oval instanceof String) {
	    String s = (String) oval;
	    return s.length();
	} else if (oval instanceof VL) {
	    VL vl = (VL) oval;
	    byte kind = vl.getVKind();
	    if (kind == VL.VCHAR) return 1;
	    if (kind == VL.VCAT) {
		VLCat vlc = (VLCat) oval;
		int size =  vlc.size;
		size += XDOM.spaceString[vlc.spaceBefore].length();
		size += XDOM.spaceString[vlc.spaceAfter].length();
		return size;
	    }
	}
	return 0;
    }

    private static char getChar1(Object oval,long ival,int pos) {
	if (oval instanceof String) {
	    String s = (String) oval;
	    return s.charAt(pos);
	} else if (oval instanceof VL) {
	    VL vl = (VL) oval;
	    byte kind = vl.getVKind();
	    if (kind == VL.VCHAR) return (char) ival;
	    if (kind == VL.VCAT) {
		VLCat vlc = (VLCat) oval;
		int pos1 = pos;
		int size = vlc.ovals.length;
		for (int i = 0; i < size; i++) {
		    Object oval1 = vlc.ovals[i];
		    int chars = stringSize(oval1);
		    if (pos1 < chars) {
			return getChar1(oval1,0,pos1);
		    }
		    pos1 = pos1 - chars;
		}
	    }
	}
	return ' ';      
    }
    public static char getChar(Object oval,long ival,int pos) {
	if (oval instanceof VLCat) {
	    VLCat vlc = (VLCat) oval;
	    String s1 = XDOM.spaceString[vlc.spaceBefore];
	    String s2 = XDOM.spaceString[vlc.spaceBefore];
	    int size1 = s1.length();
	    int size2 = s2.length();
	    int size = vlc.size;
	    if (pos < size1) {
		return s1.charAt(pos);
	    } else if (pos < size1 + size) {
		return getChar1(oval,ival,pos-size1);
	    } else {
		return s2.charAt(pos-size1-size);
	    }
	} else {
	    return getChar1(oval,ival,pos);
	}
    }

    public static int stringComp(Object oval1,long ival1,Object oval2,long ival2) {
	if (oval1 == oval2) return 0;
	if (oval1 instanceof String && oval2 instanceof String) {
	    String s1 = (String) oval1;
	    String s2 = (String) oval2;
	    return s1.compareTo(s2);
	} else {
	    int size1 = stringSize(oval1);
	    int size2 = stringSize(oval2);
	    int size = size1;
	    if (size2 < size1) size = size2;
	    for (int i = 0; i < size; i++) {
		char c1 = getChar(oval1,ival1,i);
		char c2 = getChar(oval2,ival2,i);
		if (c1 < c2) return -1;
		if (c1 > c2) return 1;
	    }
	    if (size1 < size2) return -1;
	    if (size1 > size2) return 1;
	    return 0;
	}
    }

    public static int stringFComp(Object oval1,long ival1,Object oval2,long ival2) {
	if (oval1 == oval2) return 0;
	int size1 = stringSize(oval1);
	int size2 = stringSize(oval2);
	int size = size1;
	if (size2 < size1) size = size2;
	for (int i = 0; i < size; i++) {
	    char c1 = getChar(oval1,ival1,i);
	    char c2 = getChar(oval2,ival2,i);
	    if ('A' <= c1 && c1 <= 'Z') c1 = (char) (c1 - 'A' + 'a');
	    if ('A' <= c2 && c2 <= 'Z') c2 = (char) (c2 - 'A' + 'a');
	    if (c1 < c2) return -1;
	    if (c1 > c2) return 1;
	}
	if (size1 < size2) return -1;
	if (size1 > size2) return 1;
	return 0;
    }
}
