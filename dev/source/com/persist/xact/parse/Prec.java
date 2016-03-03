/*******************************************************************************
*
* Copyright (c) 2002-2006. John R. Nestor. All rights reserved.
*
* See the file XLICENSE.txt for use and redistribution information.
*
*******************************************************************************/

package com.persist.xact.parse;

/**
<p>
This class provides information about XACT prefix and
infix operators including there precedence.
</p>
**/
public final class Prec {
    /**
     ** The precedence to be used to process top
     ** level expressions. This is the minimum
     ** precedence value.
     */
    static public int topPrec = 0;

    /**
     ** Finds the index of an operator name.
     ** @param s the operator name
     ** @return its index (or -1 if not found).
     */
    static public int findName(String s) {
        int size = name.length;
	for (int i = 0; i < size; i++) {
	    if (s == name[i]) return i;
	}
	System.out.println("Prec findName fail: "+s);
	return -1;
    }

    /**
     ** Table of all operator names.
     */
    static public String[] name = {
	"Assert",
	"Equal",
	"Assign",
	"Or",
	"Xor",
	"And",
	"Eq", "Ne",
	"Less", "LessEq", "Greater", "GreaterEq",
	"Range",
	"Add", "Sub", 
	"Mult", "Div", "Rem",
	"Cat",
	"Minus", "Not", "Percent", "XDOMValue",
	"Tilde", "Question", "Dot"
    };

    /**
     ** Table of all operators.
     */
    static public String[] op = {
	"~",
	"=",
	":=",
	"|",
	"^",
	"&",
	"==", "!=",
	"<", "<=", ">", ">=",
	"..",
	"+", "-", 
	"*", "/", "/+",
	"++",
	"-", "!", "%", "**",
	"~", "?", "."
    };

    /**
     ** Table of operator precedences.
     */
    static public int[] pr = {
	1,
	2,
	3,
	4,
	5,
	6,
	7,7,
	8,8,8,8,
	9,
	10,10,
	11,11,11,
	12,
	13,13,13,13,
	14,14,14
    };

    /**
     ** Table that indicates whether an operator
     ** is prefix or infix.
     */
    static public boolean[] infix = {
	false,
	true,
	true,
	true,
	true,
	true,
	true,true,
	true,true,true,true,
	true,
	true,true,
	true,true,true,
	true,
	false,false,false,false,
	true,true,true
    };
}
