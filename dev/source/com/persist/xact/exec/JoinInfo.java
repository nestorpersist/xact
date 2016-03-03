/*******************************************************************************
*
* Copyright (c) 2002-2006. John R. Nestor. All rights reserved.
*
* See the file XLICENSE.txt for use and redistribution information.
*
*******************************************************************************/

package com.persist.xact.exec;

import com.persist.xact.value.*;

public final class JoinInfo {
    public int stackFirst;
    public int bufferFirst;
    public Ctx ctx;
    public boolean onlyString;
    public boolean isError;
    public byte spaceBefore;
    public byte spaceAfter;
}