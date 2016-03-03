/*******************************************************************************
*
* Copyright (c) 2002. John R. Nestor. All rights reserved.
*
* See the file XLICENSE.txt for use and redistribution information.
*
*******************************************************************************/

package com.persist.xact.bind;

import com.persist.xdom.*;

public class XDefFunc extends XDefName {
    public XDOMElement ftree;
    
    public boolean hasArgs;
    public boolean varArgs;
    public int args;
    
    public boolean varExts;
    public int exts;
    public int assert1;
}