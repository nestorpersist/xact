/*******************************************************************************
*
* Copyright (c) 2002. John R. Nestor. All rights reserved.
*
* See the file XLICENSE.txt for use and redistribution information.
*
*******************************************************************************/

package com.persist.xact.bind;

public class XDef {
    public String name;
    public boolean hasExt;
    public boolean readOnly;
    public boolean visible;
    public boolean local;
    public XScope scope;
    public XDef next;

    public XDef getReal() {
	if (this instanceof XDefVirtual) {
	    XDefVirtual vdef = (XDefVirtual) this;
	    return vdef.def.getReal();
	} else {
	    return this;
	}
    }

    public String toString() {
	return name;
    }
}	