/*******************************************************************************
*
* Copyright (c) 2002-2007. John R. Nestor. All rights reserved.
*
* See the file XLICENSE.txt for use and redistribution information.
*
*******************************************************************************/

package com.persist.xact.system;

import java.util.*;
import com.persist.xact.parse.*;
import com.persist.xdom.*;
import com.persist.xact.value.*;
import com.persist.xact.bind.*;
import com.persist.xact.exec.*;

public final class XThread {
   public XCmd cmd;
   public ParseStack ps;

   public Parser parser;
   public Bind bind;
   public Exec exec;
   public Errors errors;
   public XURL xurl;
   public XArg xarg;
   public XRun xrun;

   public StringBuffer sbuff;
   public Calendar cal;
   public Date date;

   public String name;
   public XThread parent = null;
   public int parentFrame = 0;

   public XDOMElement parseResult;
   
   public Object assignOval = null;
   public long assignIval = 0;

   public Object currentLang = null;

   public boolean doBreak;

   public XThread(String name,XCmd cmd) {
       this.name = name;
       this.cmd = cmd;

       sbuff = new StringBuffer(200);
       date = new Date(0);

       currentLang = new VLLxact(new VLLrender(VLLrender.KXML));
       doBreak = false;

       cal = Calendar.getInstance();
       cal.setTimeZone(TimeZone.getTimeZone("GMT"));
       cal.setTime(date);
       cal.setLenient(true);

       ps = new ParseStack(1000,500);
       parser = new Parser(this);
       bind = new Bind(this);
       exec = new Exec(this);
       errors = new Errors(this);
       xurl = new XURL(this);
       xarg = new XArg(this);
       xrun = new XRun(this);
       ps.init();
       parser.init();
       bind.init();
       exec.init();
   }
}	
