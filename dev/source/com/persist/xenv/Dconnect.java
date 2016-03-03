/*******************************************************************************
*
* Copyright (c) 2002-2007. John R. Nestor. All rights reserved.
*
* See the file XLICENSE.txt for use and redistribution information.
*
*******************************************************************************/

package com.persist.xenv;

import com.persist.xdom.*;
import com.persist.xact.system.*;

public class Dconnect {
   private static DWin dwin = null;

   public Dconnect() {
   }

   private void initDebug() {
       if (dwin == null) {
	   dwin = new DWin();
	   dwin.setVisible(true);
       }
   }

   public void setError(XThread xt,XPos pos,String kind,String msg) {
       if (! xt.cmd.debug) {
	   xt.cmd.debug = true;
	   initDebug();
	   dwin.setError(xt,pos,kind,msg);
	   dwin.dwait();
	   xt.cmd.debug = false;
       }
   }

   public void before(XThread xt,XPos pos,boolean hasVal,Object oval,long ival) {
       if (! xt.cmd.debug) {
	   xt.cmd.debug = true;
	   initDebug();
	   dwin.before(xt,pos,hasVal,oval,ival);
	   dwin.dwait();
	   xt.cmd.debug = false;
       }	   
   }

   public boolean after(XThread xt,XPos pos,boolean hasVal,Object oval,long ival,boolean assigned) {
       boolean redo = false;
       if (! xt.cmd.debug) {
	   xt.cmd.debug = true;
	   initDebug();
	   dwin.after(xt,pos,hasVal,oval,ival,assigned);
	   dwin.dwait();
	   redo = dwin.getRedo();
	   xt.cmd.debug = false;
       }
       return redo;
   }

   public void assign(XThread xt,Object oval,long ival) {
       if (! xt.cmd.debug) {
	   xt.assignOval = oval;
	   xt.assignIval = ival;
       }
   }

   public void done(XThread xt) {
      if (dwin != null) {
	  dwin.done(xt);
	  dwin.dwait();
      }
   }

   public void quit(XThread xt) {
      if (dwin != null) {
	  dwin.quit(xt);
      }
   }
}

