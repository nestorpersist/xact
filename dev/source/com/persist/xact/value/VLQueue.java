/*******************************************************************************
*
* Copyright (c) 2002. John R. Nestor. All rights reserved.
*
* See the file XLICENSE.txt for use and redistribution information.
*
*******************************************************************************/

package com.persist.xact.value;

import com.persist.xact.exec.*;

public final class VLQueue extends VL {

    private Object notFull = new Object();
    private Object notEmpty = new Object();
    private Object lock = new Object();
    private int cnt;
    private int size;
    private int first;
    private int last;
    public Object[] ovals;
    public long[] ivals;
    
    public VLQueue(int size) {
	vKind = VL.VQUEUE;
	ovals = new Object[size];
	ivals = new long[size];
	this.size = size;
	cnt = 0;
	first = 0;
	last = size - 1;
    }

    public void send(Object oval,long ival) {
	try {
	    synchronized(notFull) {
		while (cnt == size) notFull.wait();
		synchronized(lock) {
		    cnt ++;
		}
		last ++;
		if (last == size) last = 0;
		ovals[last] = oval;
		ivals[last] = ival;
	    }
	    synchronized(notEmpty) {
		notEmpty.notify();
	    }
	}
	catch (Exception e) {
	}
    }

    public Object receive(VariableStack stack) {
	Object result = null;
	try {
	    synchronized(notEmpty) {
		while (cnt == 0) notEmpty.wait();
		if (stack != null) {
		    stack.push(ovals[first],ivals[first]);
		} else {
		    result = ovals[first];
		}
		ovals[first] = null;
		first ++;
		if (first == size) first = 0;
		synchronized(lock) {
		    cnt --;
		}
	    }
	    synchronized(notFull) {
		notFull.notify();
	    }
	}
	catch (Exception e) {
	    stack.pushError();
	}
	return result;
    }
}
