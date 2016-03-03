/*******************************************************************************
*
* Copyright (c) 2002-2006. John R. Nestor. All rights reserved.
*
* See the file XLICENSE.txt for use and redistribution information.
*
*******************************************************************************/

package com.persist.xact.exec;

import com.persist.xact.value.*;

public final class VariableStack extends Stack {

    private final int SHIFT = 10;
    private final int CHUNK = 2 << (SHIFT - 1);
    private final int MASK = CHUNK - 1;

    private final int FIRST = 100;
    private final int INCR = 100;
    
    private FixedStack vals[];
    private int last = -1;
    private int top;

    private boolean haveTop = false;
    private boolean storedTop = false;
    private Object topOval;
    private long topIval;
    
    
    private void addChunk() {
	FixedStack fs = new FixedStack(CHUNK);
	last ++;
	if (last > vals.length) {
	    FixedStack newVals[] = new FixedStack[last+INCR];
	    for (int i = 0; i < last; i++) {
		newVals[i] = vals[i];
	    }
	    vals = newVals;
	}
	vals[last] = fs;
    }
    
    public VariableStack() {
	vals = new FixedStack[FIRST];
	top = -1;
	addChunk();
    }

    public long getIval(int pos) {
	if (pos == top && haveTop) return topIval;
	int high = pos >> SHIFT;
	int low = pos & MASK;
	FixedStack fs = vals[high];
	return fs.getIval(low);
    }

    public Object getOval(int pos) {
	if (pos == top && haveTop) return topOval;
	int high = pos >> SHIFT;
	int low = pos & MASK;
	FixedStack fs = vals[high];
	return fs.getOval(low);
    }

    public void setVal1(int pos,Object oval,long ival) {
	int high = pos >> SHIFT;
	int low = pos & MASK;
	while (last <= high) {
	    addChunk();
	}
	FixedStack fs = vals[high];
	fs.setVal(low,oval,ival);
    }

    public void setVal(int pos,Object oval,long ival) {
	if (pos == top) {
	    topOval = oval;
	    topIval = ival;
	    haveTop = true;
	    storedTop = false;
	    return;
	}
	setVal1(pos,oval,ival);
    }

    public long getTopIval() {
	if (! haveTop) {
	    int high = top >> SHIFT;
	    int low = top & MASK;
	    FixedStack fs = vals[high];
	    topIval = fs.getIval(low);
	    topOval = fs.getOval(low);
	    haveTop = true;
	    storedTop = true;
	}
	return topIval;
    }

    public Object getTopOval() {
	if (! haveTop) {
	    int high = top >> SHIFT;
	    int low = top & MASK;
	    FixedStack fs = vals[high];
	    topIval = fs.getIval(low);
	    topOval = fs.getOval(low);
	    haveTop = true;
	    storedTop = true;
	}
	return topOval;
    }

    public int getTop() {
	return top;
    }

    public void setTop(int pos) {
	if (pos == top) return;
	if (top < pos && haveTop && ! storedTop) {
	    setVal1(top,topOval,topIval); 
	}
	int last = top;
	top = pos;
	for (int i = pos + 1; i <= last; i++) {
	    setVal1(i,null,Value.VUNINIT);
	}
	haveTop = false;
    }

    public void push(Object oval,long ival) {
	if (haveTop && ! storedTop) {
	    setVal1(top,topOval,topIval); 
	}
	top ++;
	topOval = oval;
	topIval = ival;
	haveTop = true;
	storedTop = false;
    }

    public void pop() {
	top --;
	haveTop = false;
    }

    public void pushNull() {
	push(null,Value.VNULL);
    }

    public void pushError() {
	push(null,Value.VERROR);
    }

    public void pushUninit() {
	push(null,Value.VUNINIT);
    }
}
