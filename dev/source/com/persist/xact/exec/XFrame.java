/*******************************************************************************
*
* Copyright (c) 2002. John R. Nestor. All rights reserved.
*
* See the file XLICENSE.txt for use and redistribution information.
*
*******************************************************************************/

package com.persist.xact.exec;

import com.persist.xact.system.*;
import com.persist.xdom.*;
import com.persist.xact.bind.*;

public final class XFrame {
    public int level;
    public XFrame slink; 
    public int base;  /* stack index */
    public int size;  /* number of locals */
    public long uid;
    public XScope scope;
    public XDOM caller;
    public Stack stack;
    //public XFrame save;

    public long getUid() {
	XFrame f = this;
	while (f.uid == -1) {
	    f = f.slink;
	}
	return f.uid;
    }

    public String toString1() {
	XDOMElement t = scope.tree;
	String result;
	String tag = t.getTag();
	if (tag == "x:func") {
	    XDOMName ntree = TreeUtil.getName(t);
	    result = "function "+ntree.getName();
	} else if (tag == "x:self") {
	    XDOMElement t1 = scope.parent.tree;
	    XDOMName ntree = TreeUtil.getName(t1);
	    result = "self "+ntree.getName();
	} else if (tag == "x:type") {
	    XDOMName ntree = TreeUtil.getName(t);
	    result = "type "+ntree.getName();
	} else if (tag == "x:view") {
	    result = "view";
	} else if (tag == "x:fork") {
	    result = "fork";
	} else if (tag == "x:thread") {
	    XDOMName ntree = TreeUtil.getName(t);
	    result = "thread "+ntree.getName();
	} else {
	    result = "block";
	}
	if (t.scope != null) {
	    result = result + "(" + t.scope.level +")";
	}
	return result;
    }
}  