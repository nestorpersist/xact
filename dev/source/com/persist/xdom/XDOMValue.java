/*******************************************************************************
*
* Copyright (c) 2002-2005. John R. Nestor. All rights reserved.
*
* See the file XLICENSE.txt for use and redistribution information.
*
*******************************************************************************/

package com.persist.xdom;

import com.persist.xact.value.*;

public final class XDOMValue extends XDOM {
   private Object oval;
   private long ival;

   /* Java Interfaces */

   public boolean isString() {
       return isString(oval,ival);
   }

   public String getString() {
       return getString(oval,ival);
   }

   /* Special xact Interfaces */

   public XDOMValue(Object oval,long ival) {
       xKind = XDOM.XVALUE;
       this.oval = oval;
       this.ival = ival;
   }

   public Object getOval() {
       return oval;
   }

   public long getIval() {
       return ival;
   }

   public void setVal(Object oval,long ival) {
       this.oval = oval;
       this.ival = ival;
   }

   public static boolean isString(Object oval,long ival) {
       if (oval instanceof String) {
	   return true;
       } else if (oval instanceof VL) {
	   VL vl = (VL) oval;
	   byte kind = vl.getVKind();
	   switch (kind) {
	       case VL.VCAT: return true; 
	       case VL.VCHAR: return true;
	       case VL.VDATE: return true;
	       case VL.VBOOL: return true;
	   }
       }
       return false;
   }

   private static String getString1(Object oval,long ival,boolean includeSpacer) {
       if (oval instanceof String) {
	   String s = (String) oval;
	   return s;
       } else if (oval instanceof VL) {
	   VL vl = (VL) oval;
	   byte kind = vl.getVKind();
	   switch (kind) {
	       case VL.VBOOL:
	       {
		   if (ival == 0) {
		       return "false";
		   } else {
		       return "true";
		   }
	       }
	       case VL.VCHAR:
	       {
		   char[] chars = new char[1];
		   chars[0] = (char) ival;
		   return new String(chars).intern();
	       }
	       case VL.VDATE:
	       {
		   VLDate vld = (VLDate) oval;
		   return VLDate.toString(vld,ival);
	       }
	       case VL.VCAT:
	       {
		   VLCat vlc = (VLCat) oval;
		   String result = "";
		   int size = vlc.ovals.length;
		   if (includeSpacer && vlc.spaceBefore != XDOM.EMPTY) {
		       result = result + XDOM.spaceString[vlc.spaceBefore];
		   }
		   for (int i = 0; i < size; i++) {
		       Object oval1 = vlc.ovals[i];
		       result = result + getString1(oval1,0,false);
		   }
		   if (includeSpacer && vlc.spaceAfter != XDOM.EMPTY) {
		       result = result + XDOM.spaceString[vlc.spaceAfter];
		   }
		   return result.intern();
	       }
	   }
       }
       return "";
   }

   public static String getString(Object oval,long ival) {
       return getString1(oval,ival,true);
   }


   public static boolean isInt(Object oval,long ival) {
       if (oval instanceof VL) {
	   VL vl = (VL) oval;
	   if (vl.getVKind() == VL.VINT) {
	       return true;
	   }
       }
       return false;
   }

   public static long getInt(Object oval,long ival) {
       if (oval instanceof VL) {
	   VL vl = (VL) oval;
	   if (vl.getVKind() == VL.VINT) {
	       return ival;
	   }
       }
       return 0;
   }

   public static boolean isFloat(Object oval,long ival) {
       if (oval instanceof VL) {
	   VL vl = (VL) oval;
	   if (vl.getVKind() == VL.VFLOAT) {
	       return true;
	   }
       }
       return false;
   }

   public static double getFloat(Object oval,long ival) {
       if (oval instanceof VL) {
	   VL vl = (VL) oval;
	   if (vl.getVKind() == VL.VFLOAT) {
	       return Double.longBitsToDouble(ival);
	   }
       }
       return 0.0;
   }

}