/*******************************************************************************
*
* Copyright (c) 2002-2006. John R. Nestor. All rights reserved.
*
* See the file XLICENSE.txt for use and redistribution information.
*
*******************************************************************************/

package com.persist.xact.system;

import com.persist.xdom.*;

public final class XURL {

    private final char eChar = '!';
    private XThread xt;

    public XURL(XThread xt) {
	this.xt = xt;
    }

    private int hex(char ch) {
	if ('0' <= ch && ch <= '9') return ch-'0';
	if ('A' <= ch && ch <= 'F') return ch-'A'+10;
	if ('a' <= ch && ch <= 'f') return ch-'a'+10;
	return 0;
    }

    private char hex(String s) {
	int size = s.length();
	int val = 0;
	for (int i = 0; i < size; i++) {
	    char ch = s.charAt(i);
	    val = val * 16 + hex(ch);
	}
	return (char) val;
    }

    private int dec(char ch) {
	if ('0' <= ch && ch <= '9') return ch-'0';
	return 0;
    }

    private char dec(String s) {
	int size = s.length();
	int val = 0;
	for (int i = 0; i < size; i++) {
	    char ch = s.charAt(i);
	    val = val * 10 + dec(ch);
	}
	return (char) val;
    }

    private void decodeEscape(String s) {
	int size = s.length();
	if (size > 0) {
	    if (s.charAt(0) == '=') {
		if (size > 1) {
		    char ch = s.charAt(1);
		    if (ch == 'x' || ch == 'X') {
			xt.sbuff.append(hex(s.substring(2)));
			return;
		    }
		}
		xt.sbuff.append(dec(s.substring(1)));
	    } else {
		if (s.equals("amp")) {
		    xt.sbuff.append('&');
		} else if (s.equals("lt")) {
		    xt.sbuff.append('<');
		} else if (s.equals("gt")) {
		    xt.sbuff.append('>');
		} else if (s.equals("quot")) {
		    xt.sbuff.append('"');
		} else {
		    // unknown escape name
		}
	    }
	} else {
	    // missing escape
	}
    }
    
    private String decode(String s) {
	int size = s.length();
	xt.sbuff.setLength(0);
	int i = 0;
	while (i < size) {
	    char ch = s.charAt(i);
	    if (ch == '+') {
		xt.sbuff.append(' ');
	    } else if (ch == eChar) {
		if (i+1 < size) {
		    i ++;
		    if (s.charAt(i) == '(') {
			i++;
			int first = i;
			int last = -1;
			while (i < size) {
			    ch = s.charAt(i);
			    if (ch == ')') {
				last = i-1;
				break;
			    }
			    i++;
			}
			if (last >= first) {
			    decodeEscape(s.substring(first,last+1));
			} else {
			    // missing escape
			}
			
		    } else {
			// missing (
		    }
		}
	    } else if (ch == '%') {
		i++;
		if (i+1 < size) {
		    xt.sbuff.append(hex(s.substring(i,i+2)));
		    i = i + 1;
		} else {
		    // missing escape
		}
	    } else {
		xt.sbuff.append(ch);
	    }
	    i++;
	}
	return xt.sbuff.toString().intern();
  }

    private void encode(String s) {
	int size = s.length();
	int i = 0;
	while (i < size) {
	    char ch = s.charAt(i);
	    if (ch == ' ') {
		xt.sbuff.append('+');
	    } else if ('a' <= ch && ch <= 'z') {
		xt.sbuff.append(ch);
	    } else if ('A' <= ch && ch <= 'Z') {
		xt.sbuff.append(ch);
	    } else if ('0' <= ch && ch <= '9') {
		xt.sbuff.append(ch);
	    } else if (ch == '.' || ch == '-' || ch == '_' || ch == ':') {
		xt.sbuff.append(ch);
	    } else if (ch == '&') {
		xt.sbuff.append("!(amp)");
	    } else if (ch == '<') {
		xt.sbuff.append("!(lt)");
	    } else if (ch == '>') {
		xt.sbuff.append("!(gt)");
	    } else if (ch == '"') {
		xt.sbuff.append("!(quot)");
	    } else {
		xt.sbuff.append("!(="+((int)ch)+")");
	    }
	    i++;
	}
    }

    private boolean hasEqualLeft(String s) {
	int size = s.length();
	char prev = ' ';
	for (int i = 0; i < size; i++) {
	    char ch = s.charAt(i);
	    if (ch == '=' && prev != '(') return true;
	    if (ch == '(' && prev != eChar) return true;
	    prev = ch;
	}
	return false;
    }

    private XDOM parseVal(String val) {
	int size = val.length();
	if (hasEqualLeft(val)) {
	    return parseExp(val);
	} else {
	    return new XDOMString(decode(val));
	}	
    }

    private XDOM parseCall(String name,String args) {
	XDOMCall xc = new XDOMCall(new XDOMName(decode(name)));
	int size = args.length();
	int first = 0;
	int pcnt = 0;
	for (int i = 0; i < size; i++) {
	    char ch = args.charAt(i);
	    if (ch == ',' && pcnt == 0) {
		XDOM x1 = parseVal(args.substring(first,i));
		if (x1 != null) xc.insertArg(-1,x1);
		first = i+1;
	    } else if (ch == '(') {
		pcnt ++;
	    } else if (ch == ')') {
		pcnt --;
	    }
	}
	XDOM x2 = parseVal(args.substring(first,size));
	if (x2 != null) xc.insertArg(-1,x2);
	return xc;
    }
    
    private XDOM parseExp(String s) {
	int size = s.length();
	for (int i = 0; i < size; i++) {
	    char ch = s.charAt(i);
	    if (ch == '=') {
		return parseDef(s.substring(0,i),s.substring(i+1,size));
	    } else if (ch == '(') {
		if (s.charAt(size-1) == ')') {
		    return parseCall(s.substring(0,i),s.substring(i+1,size-1));
		} else {
		    //missing )
		    return parseCall(s.substring(0,i),s.substring(i+1,size));
		}
					      
	    }
	}
	return new XDOMName(decode(s));
    }

    private XDOM parseDef(String name,String val) {
	XDOM x1 = parseVal(val);
	if (x1 != null) {
	    XDOMCall xc = new XDOMCall(new XDOMName("Equal"),2);
	    xc.setKind(XDOMCall.COP);
	    xc.insertArg(-1,new XDOMName(decode(name)));
	    xc.insertArg(-1,x1);
	    return xc;
	} else {
	    return null;
	}
    }
    
    private XDOM parseItem(String s,boolean first) {
	int size = s.length();
	for (int i = 0; i < size; i++) {
	    char ch = s.charAt(i);
	    if (ch == '(') {
		return parseExp(s);
	    } else if (ch == '=') {
		return parseDef(s.substring(0,i),s.substring(i+1,size));
	    }
	}
	if (first) {
	    return parseExp(s);
	} else {
	    XDOMName xn = new XDOMName(s);
	    return xn;
	}
    }

    public XDOMCall URLParseQuery(String s) {
	XDOMCall xc = new XDOMCall(new XDOMName("query"));
	// split by &
	int first = 0;
	int size = s.length();
	for (int i = 0; i < size; i++) {
	    char ch = s.charAt(i);
	    if (ch == '&') {
		XDOM x1 = parseItem(s.substring(first,i),i == 0);
		if (x1 != null) {
		    xc.insertArg(-1,x1);
		}
		first = i+1;
	    }
	}
	XDOM x2 = parseItem(s.substring(first,size),first == 0);
	if (x2 != null) {
	    xc.insertArg(-1,x2);
	}
	return xc;
    }

    private void unparseItem(XDOM x,boolean needEscape) {
	if (x instanceof XDOMString) {
	    XDOMString xs = (XDOMString) x;
	    encode(xs.getVal());
	} else if (x instanceof XDOMName) {
	    XDOMName xn = (XDOMName) x;
	    encode(xn.getName());
	} else if (x instanceof XDOMCall) {
	    XDOMCall xc = (XDOMCall) x;
	    XDOM func = xc.getFunc();
	    int size = xc.argSize();
	    if (func instanceof XDOMName) {
		XDOMName xn = (XDOMName) func;
		String name = xn.getName();
		if (xc.getKind() == XDOMCall.COP && size == 2 && name == "Equal" &&
		    xc.getArg(1) instanceof XDOMName) {
		    XDOMName name1 = (XDOMName) xc.getArg(1);
		    encode(name1.getName());
		    xt.sbuff.append("=");
		    unparseItem(xc.getArg(2),true);
		} else {
		    encode(name);
		    String sep = "";
		    xt.sbuff.append('(');
		    for (int i = 1; i <= size; i++) {
			xt.sbuff.append(sep);
			unparseItem(xc.getArg(i),true);
			sep = ",";
		    }
		    xt.sbuff.append(')');
		}
	    }
	}
    }
    
    public String URLUnparseQuery(XDOMCall xc) {
	xt.sbuff.setLength(0);
	int size = xc.argSize();
	String sep = "";
	for (int i = 1; i <= size; i++) {
	    xt.sbuff.append(sep);
	    sep = "&";
	    unparseItem(xc.getArg(i),false);
	}
	return xt.sbuff.toString().intern();
    }
}
