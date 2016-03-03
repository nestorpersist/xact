/*******************************************************************************
*
* Copyright (c) 2002. John R. Nestor. All rights reserved.
*
* See the file XLICENSE.txt for use and redistribution information.
*
*******************************************************************************/

package com.persist.xact.bind;

import com.persist.xdom.*;

public final class XScope {
    public XScope parent;
    public XDOMElement tree;
    public XDef defs;
    public int size;
    public boolean top;
    public int level; /* not top = nesting level; top = load index */

    public static XScope add(XDOMElement tree) {
	XScope scope = new XScope();
	scope.tree = tree;
	tree.scope = scope;
	return scope;
    }

    public XDef find(String name,boolean hasExt) {
	XDef def = defs;
	while (def != null) {
	    if (def.name == name && def.hasExt == hasExt) return def;
	    def = def.next;
	}
	return null;
    }

    public void add(XDef def) {
	XScope scope = this;
	def.next = defs;
	defs = def;
	def.scope = scope;
	if (scope.tree != null && scope.tree.getTag() == "x:module") {
	    scope = scope.parent;
	}
	if (def instanceof XDefName) {
	    XDefName ndef = (XDefName) def;
	    ndef.offset = scope.size;
	    scope.size ++;
	    if (ndef.hasView) scope.size ++;
	}
    }
}