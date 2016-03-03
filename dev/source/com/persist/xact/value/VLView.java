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

public class VLView extends VL {
    public XFrame frame;
    public long uid;
    public XFrame context;
    public VLView next;
    public XDefView def;
    public Object lang;

    public VLView() {
	vKind = VL.VVIEW;
    }

}