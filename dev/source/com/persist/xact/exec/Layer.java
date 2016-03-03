/*******************************************************************************
*
* Copyright (c) 2006. John R. Nestor. All rights reserved.
*
* See the file XLICENSE.txt for use and redistribution information.
*
*******************************************************************************/

package com.persist.xact.exec;

public final class Layer {
    public XFrame slink;
    public byte pass;

    public Layer(XFrame slink,byte pass) {
	this.slink = slink;
	this.pass = pass;
    }
}