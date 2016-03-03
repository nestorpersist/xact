/*******************************************************************************
*
* Copyright (c) 2002. John R. Nestor. All rights reserved.
*
* See the file XLICENSE.txt for use and redistribution information.
*
*******************************************************************************/

package com.persist.xact.value;

import com.persist.xact.bind.*;
import com.persist.xact.system.*;
import com.persist.xact.exec.*;

public final class VLThread extends VL {
    public XThread xt;
    public Thread thread;
    public XDefThread def;
    public XFrame context;
    public long uid;

    public VLThread() {
	vKind = VL.VTHREAD;
    }
}
