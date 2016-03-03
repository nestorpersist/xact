/*******************************************************************************
*
* Copyright (c) 2002-2006. John R. Nestor. All rights reserved.
*
* See the file XLICENSE.txt for use and redistribution information.
*
*******************************************************************************/

package com.persist.xact.value;

import com.persist.xact.bind.*;
import com.persist.xact.exec.*;
import com.persist.xact.system.*;

public class VLFunc extends VL {
    public XDefFunc def;
    public XFrame context;
    public long uid;
    public Object lang;
    public boolean called;

    public VLFunc() {
	vKind = VL.VFUNC;
    }
}
