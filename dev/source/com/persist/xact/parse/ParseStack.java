/*******************************************************************************
*
* Copyright (c) 2002-2006. John R. Nestor. All rights reserved.
*
* See the file XLICENSE.txt for use and redistribution information.
*
*******************************************************************************/

package com.persist.xact.parse;

import java.util.*;
import com.persist.xdom.*;

/**
<p>
This is the main stack used by the XACT parser.
It holds XDOM parse tree nodes.
Stack element indexes are
in the range 0..size()-1
</p>
**/
public final class ParseStack {

    /**
     ** The size of stack.
     ** This should always be equal to vals.size()
     ** but will be faster to access.
     */
    private int next;

    /**
     ** This Java Vector holds stack values.
     */
    private Vector<XDOM> vals;

    /**
     ** Creates a new parse stack.
     ** @param capacity an initial capacity
     ** @param incr a capacity increment.
     */
    public ParseStack(int capacity,int incr) {
	next = 0;
	vals = new Vector<XDOM>(capacity,incr);
    }

    /**
     ** Initializes the lexical analyzer.
     ** Must be called before any other methods.
     */
    public void init() {
    }

    /**
     ** Returns the size of the stack.
     ** @return the index of the element after the top of the stack.
     */
    public int start() {
	return next;
    }

    /**
     ** Pushes a tree node unto the stack.
     ** @param val an tree node to push.
     */
    public void push(XDOM val) {
	vals.addElement(val);
	next ++;
    }

    /**
     ** Pops off all stack elements between start and the top of stack
     ** and puts them in the attribute list of xe.
     ** @param start index of first element to pop.
     ** @param xe place to put result.
     ** @param clear if true, any previous attributes in xe are removed.
     */
    public void popAttr(int start,XDOMElement xe,boolean clear) {
	int size = next-start;
	if (clear) xe.clearAttr(size);
	if (size != 0) {
	    int i;
	    for (i = 0; i < size; i++) {
		xe.insertAttr(-1,vals.elementAt(i+start));
	    }
	}
	next = start;
	vals.setSize(start);
    }

    /**
     ** Pops off all stack elements between start and the top of stack
     ** and puts them in the body of xe.
     ** @param start index of first element to pop.
     ** @param xe place to put result.
     ** @param clear if true, any previous body contents xe are removed.
     */
    public void popBody(int start,XDOMElement xe,boolean clear) {
	int size = next-start;
	if (clear) xe.clearBody(size);
	if (size != 0) {
	    int i;
	    for (i = 0; i < size; i++) {
		xe.insertBody(-1,vals.elementAt(i+start));
	    }
	}
	next = start;
	vals.setSize(start);
    }

    /**
     ** Pops off all stack elements between start and the top of stack
     ** and puts them in the parameter list of xc.
     ** @param start index of first element to pop.
     ** @param xc place to put result.
     ** @param clear if true, any previous parameters in xc are removed.
     */
    public void pop(int start,XDOMCall xc,boolean clear) {
	int size = next-start;
	if (clear) xc.clearArg(size);
	if (size != 0) {
	    int i;
	    for (i = 0; i < size; i++) {
		xc.insertArg(-1,vals.elementAt(i+start));
	    }
	}
	next = start;
	vals.setSize(start);
    }

    /**
     ** Pops off all stack elements between start and the top of stack
     ** and puts them in the name list of xn.
     ** @param start index of first element to pop.
     ** @param xn place to put result.
     ** @param clear if true, any previous name list contents in xn are removed.
     */
    public void pop(int start,XDOMName xn,boolean clear) {
	int size = next-start;
	if (clear) xn.clearExt(size);
	if (size != 0) {
	    int i;
	    for (i = 0; i < size; i++) {
		xn.insertExt(-1,vals.elementAt(i+start));
	    }
	}
	next = start;
	vals.setSize(start);
    }
}
