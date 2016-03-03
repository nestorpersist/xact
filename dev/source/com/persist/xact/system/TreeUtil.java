/*******************************************************************************
*
* Copyright (c) 2002-2006. John R. Nestor. All rights reserved.
*
* See the file XLICENSE.txt for use and redistribution information.
*
*******************************************************************************/

package com.persist.xact.system;

import com.persist.xdom.*;

public final class TreeUtil {

   public static boolean hasProp(XDOMElement etree,String name) {
       int asize = etree.attrSize();
       int i;
       for (i = 2; i <= asize; i++) {
	   XDOM elem = etree.getAttr(i);
	   if (elem instanceof XDOMName) {
	       XDOMName ntree = (XDOMName) elem;
	       if (ntree.getName() == name) return true;
	   }
       }
       return false;
   }

   public static XDOMName getName(XDOMElement etree) {
       int size = etree.attrSize();
       if (size > 0) {
	   XDOM view = stripView(etree.getAttr(1));
	   if (view instanceof XDOMName) {
	       XDOMName ntree = (XDOMName) view;
	       return ntree;
	   } else if (view instanceof XDOMCall) {
	       XDOMCall ctree = (XDOMCall) view;
	       if (ctree.getFunc() instanceof XDOMName) {
		   XDOMName ntree = (XDOMName) ctree.getFunc();
		   return ntree;
	       }
	   }
       }
       return null;
   }

   public static boolean hasPercent(XDOM x) {
       if (x instanceof XDOMCall) {
	   XDOMCall ctree = (XDOMCall) x;
	   if (ctree.getFunc() instanceof XDOMName) {
	       XDOMName ntree = (XDOMName) ctree.getFunc();
	       if (ntree.getName() == "Percent" && ctree.argSize() == 1) {
		   return true;
	       }
	   }
       }
       return false;
   }

   public static XDOM stripPercent(XDOM x) {
       if (hasPercent(x)) {
	   XDOMCall ctree = (XDOMCall) x;
	   return ctree.getArg(1);
       }
       return x;
   }

   private static boolean hasView(XDOM x) {
       if (x instanceof XDOMCall) {
	   XDOMCall ctree = (XDOMCall) x;
	   if (ctree.getFunc() instanceof XDOMName) {
	       XDOMName ntree = (XDOMName) ctree.getFunc();
	       if (ntree.getName() == "Tilde" && ctree.argSize() == 2) {
		   return true;
	       }
	   }
       }
       return false;
   }

   public static XDOM stripView(XDOM x) {
       XDOM t1 = stripPercent(x);
       if (hasView(t1)) {
	   XDOMCall ctree = (XDOMCall) t1;
	   return ctree.getArg(1);
       } else {
	   return t1;
       }
   }

   public static XDOM getView(XDOM x) {
       XDOM t1 = stripPercent(x);
       if (hasView(t1)) {
	   XDOMCall ctree = (XDOMCall) t1;
	   return ctree.getArg(2);
       } else {
	   return null;
       }
   }

   public static boolean isEqual(XDOM x) {
       if (x instanceof XDOMCall) {
	   XDOMCall ctree = (XDOMCall) x;
	   if (ctree.getFunc() instanceof XDOMName && ctree.argSize() == 2) {
	       XDOMName ntree = (XDOMName) ctree.getFunc();
	       if (ntree.getName() == "Equal" && ctree.getArg(1) instanceof XDOMName) {
		   return true;
	       }
	   }
       }
       return false;
   }

   public static XDOM stripEqual(XDOM x) {
       XDOMCall ctree = (XDOMCall) x;
       return ctree.getArg(2);
   }

   public static String getEqualName(XDOM x) {
       XDOMCall ctree = (XDOMCall) x;
       XDOMName ntree = (XDOMName)(ctree.getArg(1));
       return ntree.getName();
   }
/* no longer used ???
   public static boolean isFileRead(XDOM x) {
       if (! (x instanceof XDOMCall)) return false;
       XDOMCall xc = (XDOMCall) x;
       if (xc.argSize() != 0) return false;
       XDOM func = xc.getFunc();
       if (! (func instanceof XDOMCall)) return false;
       XDOMCall xc1 = (XDOMCall) func;
       if (xc1.argSize() != 3) return false;
       XDOM func1 = xc1.getFunc();
       XDOM arg2 = xc1.getArg(2);
       XDOM arg3 = xc1.getArg(3);
       if (! (func1 instanceof XDOMName)) return false;
       if (! (arg2 instanceof XDOMString)) return false;
       if (! (arg3 instanceof XDOMName)) return false;
       XDOMName fn = (XDOMName) func1;
       XDOMString xs = (XDOMString) arg2;
       XDOMName xn = (XDOMName) arg3;
       if (fn.getName() != "Dot") return false;
       if (xn.getName() != "file") return false;
       if (xs.getVal() != "read") return false;
       return true;
   }


   public static XDOM fileReadName(XDOM x) {
       XDOMCall xc = (XDOMCall) x;
       XDOM func = xc.getFunc();
       XDOMCall xc1 = (XDOMCall) func;
       return xc1.getArg(1);
   }
*/   
}