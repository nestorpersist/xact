/*******************************************************************************
*
* Copyright (c) 2002-2006. John R. Nestor. All rights reserved.
*
* See the file XLICENSE.txt for use and redistribution information.
*
*******************************************************************************/

package com.persist.xact.value;

import com.persist.xact.system.*;
import com.persist.xdom.*;

public final class CtxStream extends Ctx {
    public XW w;
    public byte spaceAfter;

    public CtxStream(XW w) {
	this.w = w;
	spaceAfter = XDOM.EMPTY;
    }
    public void sendSpace() {
	if (spaceAfter != XDOM.EMPTY) {
	    w.write(XDOM.spaceString[spaceAfter]);
	    spaceAfter = XDOM.EMPTY;
	}
    }
}