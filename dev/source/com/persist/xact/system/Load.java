/*******************************************************************************
*
* Copyright (c) 2002. John R. Nestor. All rights reserved.
*
* See the file XLICENSE.txt for use and redistribution information.
*
*******************************************************************************/

package com.persist.xact.system;

import com.persist.xdom.*;
import com.persist.xact.exec.*;

public final class Load {
    public String name;
    public String fname;
    public XDOMElement tree;
    public XFrame frame;
    public Object oval;
    public long ival;
    public int status;
    public XThread xtLoad;
    public boolean hasErrors;

    public final static int ERROR = 0;
    public final static int PARSE = 1;
    public final static int BIND = 2;
    public final static int DONE = 3;
}
