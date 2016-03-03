/*******************************************************************************
*
* Copyright (c) 2002-2006. John R. Nestor. All rights reserved.
*
* See the file XLICENSE.txt for use and redistribution information.
*
*******************************************************************************/

package com.persist.xdom;

/** This is the common parent class
 ** for all XDOM node types.
 */
public abstract class XDOM {

    /** The {@link #xKind} value for {@link XDOMElement} nodes **/
    public final static byte XELEMENT = 0;
    /** The {@link #xKind} value for {@link XDOMCall} nodes **/
    public final static byte XCALL = 1;
    /** The {@link #xKind} value for {@link XDOMString} nodes **/
    public final static byte XSTRING = 2;
    /** The {@link #xKind} value for {@link XDOMName} nodes **/
    public final static byte XNAME = 3;
    /** The {@link #xKind} value for {@link XDOMInt} nodes **/
    public final static byte XINT = 4;
    /** The {@link #xKind} value for {@link XDOMFloat} nodes **/
    public final static byte XFLOAT = 5;
    /** The {@link #xKind} value for {@link XDOMValue} nodes **/
    public final static byte XVALUE = 6;
    /** The kind of XDOM nodes.
     ** Equality tests based on xKind are faster
     ** than instanceof tests and can be used in
     ** switch statements.
     */
    protected byte xKind;

    /** Maps {@link xkind values to print strings **/
    public final static String[] XKind = {
	"element", "call", "string", "name",
	"int", "float", "value" 
    };

    /** The empty spacer value **/
    public final static byte EMPTY = 0;
    /** The single space spacer value **/
    public final static byte SPACE = 1;
    /** The single end-of-line spacer value **/
    public final static byte LINE = 2;
    /** The doubl end-of-line spacer value (blank line) **/
    public final static byte LINES = 3;
    /** The erase spacer value **/
    public final static byte ERASE = 4;

    /** Maps spacer values to print strings **/
    public final static String[] spaceKind = {
	"empty", "space", "line", "lines", "erase"
    };

    /** Maps spacer values to their strings for their space **/
    public final static String[] spaceString = {
	"", " ", "\n", "\n\n", ""
    };

    /** The spacer for space before the node **/
    private byte spaceBefore;
    /** The spacer for space after the node **/
    private byte spaceAfter;

    /**
     ** Joins two spacer values.
     ** @param s1 the first spacer
     ** @param s2 the second spacer
     ** @return the join of s1 and s2
     */
    public static byte spaceJoin(byte s1,byte s2) {
	if (s1 > s2) return s1;
	return s2;
    }

    /** The first mark bit for XDOM graph traversal **/
    public boolean mark1;
    /** The second mark bit for XDOM graph traversal **/
    public boolean mark2;
    /** The third mark bit for XDOM graph traversal **/
    public boolean mark3;
    /** The label (used for XDOM graph output) **/
    public String label;

    /** Should debugger break before executing this node **/
    private boolean breakBefore;
    /** Should debugger break after executing this node **/
    private boolean breakAfter;
    /** Debugger should skip over execution of the node when single
     ** stepping **/
    private boolean noStep;

    /** The position of this node **/
    public XPos pos;

    /**
     ** Gets the node kind.
     ** @return the node kind.
     */
    public byte getXKind() {
	return xKind;
    }

    /**
     ** Gets the spaceBefore value.
     ** @return the spaceBefore spacer value.
     */
    public byte getSpaceBefore() {
	return spaceBefore;
    }

    /**
     ** Sets the spaceBefore value.
     ** @param val the new spaceBefore spacer value.
     */
    public void setSpaceBefore(byte val) {
	spaceBefore = val;
    }

    /**
     ** Gets the spaceAfter value.
     ** @return the spaceAfter spacer value.
     */
    public byte getSpaceAfter() {
	return spaceAfter;
    }

    /**
     ** Sets the spaceAfter value.
     ** @param val the new spaceAfter spacer value.
     */
    public void setSpaceAfter(byte val) {
	spaceAfter = val;
    }

    /**
     ** Gets the breakBefore value.
     ** @return the breakBefore value.
     */
    public boolean getBreakBefore() {
	return breakBefore;
    }

    /**
     ** Sets the breakBefore value.
     ** @param val the new breakBefore value.
     */
    public void setBreakBefore(boolean val) {
	breakBefore = val;
    }

    /**
     ** Gets the breakAfter value.
     ** @return the breakAfter value.
     */
    public boolean getBreakAfter() {
	return breakAfter;
    }

    /**
     ** Sets the breakAfter value.
     ** @param val the new breakAfter value.
     */
    public void setBreakAfter(boolean val) {
	breakAfter = val;
    }

    /**
     ** Gets the noStep value.
     ** @return the noStep value.
     */
    public boolean getNoStep() {
	return noStep;
    }

    /**
     ** Sets the noStep value.
     ** @param val the new noStep value.
     */
    public void setNoStep(boolean val) {
	noStep = val;
    }
}
