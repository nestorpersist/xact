/*******************************************************************************
*
* Copyright (c) 2002. John R. Nestor. All rights reserved.
*
* See the file XLICENSE.txt for use and redistribution information.
*
*******************************************************************************/

package com.persist.xact.value;

import com.persist.xact.exec.*;

public final class VLObj extends VL {
   public VLType type;
   public XFrame frame;

   public VLObj() {
       vKind = VL.VOBJ;
   }

}