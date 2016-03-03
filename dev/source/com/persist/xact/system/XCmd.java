/*******************************************************************************
*
* Copyright (c) 2002-2007. John R. Nestor. All rights reserved.
*
* See the file XLICENSE.txt for use and redistribution information.
*
*******************************************************************************/

package com.persist.xact.system;

import java.util.*;
import com.persist.xact.value.*;
import com.persist.xact.bind.*;
import com.persist.xenv.*;

public final class XCmd {
    public XInter inter;
    public Value value;
    public Loads loads;
    public CmdScripts cmdscripts;
    public XOption option;
    public BIBind biBind;
    public Dconnect dconnect;

    public XW w;		     // main output stream
    public XThread xtMain = null;   // main thread

    public int msgCnt = 0;
    public int errorCnt = 0;
    public int warnCnt = 0;
    public int logCnt = 0;
    public boolean debug = false;

    public static Error restartException = new Error("restart");
    public static Error quitException = new Error("quit");

    public XCmd(XInter inter) {
	this.inter = inter;
	value = inter.value;
	loads = inter.loads;
	cmdscripts = new CmdScripts(this);
	biBind = new BIBind(value);
	option = new XOption(this);
	dconnect = new Dconnect();
    }
}