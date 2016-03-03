/*******************************************************************************
*
* Copyright (c) 2002-2007. John R. Nestor. All rights reserved.
*
* See the file XLICENSE.txt for use and redistribution information.
*
*******************************************************************************/

package com.persist.xact.exec;

import com.persist.xact.system.*;
import com.persist.xdom.*;
import com.persist.xact.value.*;
import java.util.*;

public final class EValue {
   private Exec exec;
   private VariableStack stack;
   private Errors errors;
   private Value value;

   public EValue(Exec exec) {
      this.exec = exec;
   }

   public void init() {
       stack = exec.stack;
       errors = exec.xt.errors;
       value = exec.xt.cmd.value;
   }

   /* GetS returns String, VLCat, or VLChar */
   public boolean getS(XPos pos) {
      Object oval = stack.getTopOval();
      long ival = stack.getTopIval();
      if (oval instanceof String) {
	 return true;
      } else if (oval instanceof VL) {
	  VL vl = (VL) oval;
	  int kind = vl.getVKind();
	  switch (kind) {
	      case VL.VCAT: return true;
	      case VL.VCHAR: return true;
	      case VL.VBOOL:
	      {
		  stack.pop();
		  stack.push(VLBool.toS(ival),0);
		  return true;
	      }
	      case VL.VINT:
	      {
		  stack.pop();
		  stack.push(XDOMInt.toString(ival),0);
		  return true;
	      }
	      case VL.VFLOAT:
	      {
		  stack.pop();
		  stack.push(XDOMFloat.toString(Double.longBitsToDouble(ival)),0);
		  return true;
	      }
	      case VL.VDATE:
	      {
		  VLDate vld = (VLDate) oval;
		  stack.pop();
		  stack.push(VLDate.toString(vld,ival).intern(),0);
		  return true;
	      }
	  }
      } else if (value.isNull(oval,ival)) {
	  stack.pop();
	  stack.push("",0);
	  return true;
      }
      if (! value.isError(oval,ival) && pos != null) {
	 errors.error(Errors.EXEC,pos,"not a string"+oval+":"+ival);
      }
      stack.pop();
      stack.pushError();
      return false;
   }

   public boolean getS(XDOM tree) {
       exec.execExp(tree,value.ctxEval);
       return getS(tree.pos);
   }

   public boolean getS(XDOM tree,Ctx ctx) {
       exec.execExp(tree,ctx);
       return getS(tree.pos);
   }

   public boolean getB(XPos pos) {
      Object oval = stack.getTopOval();
      long ival = stack.getTopIval();
      if (oval == value.vlBool) {
	 return true;
      } else {
	 if (! (oval instanceof String)) {
	    getS(pos);
	    oval = stack.getTopOval();
	    ival = stack.getTopIval();
	 }
	 if (XDOMValue.isString(oval,ival)) {
	    if (VLBool.isBool(oval)) {
	       stack.pop();
	       stack.push(value.vlBool,VLBool.toBool(oval));
	       return true;
	    }
	 }
      }
      if (! value.isError(oval,ival) && pos != null) {
	 errors.error(Errors.EXEC,pos,"not a boolean");
      }
      stack.pop();
      stack.pushError();
      return false;
   }

   public boolean getB(XDOM tree) {
      exec.execExp(tree,value.ctxEval);
      return getB(tree.pos);
   }

   public boolean getI(XPos pos) {
       Object oval = stack.getTopOval();
       long ival = stack.getTopIval();
       if (oval == value.vlInt) {
	   return true;

//       } else {
//	   if (! (oval instanceof String)) {
//	       getS(pos);
//	       oval = stack.getTopOval();
//	       ival = stack.getTopIval();
//	   }
//	   if (XDOMValue.isString(oval,ival)) {
//	       String s = XDOMValue.getString(oval,ival);
//	       if (XDOMInt.isInt(s)) {
//		   stack.pop();
//		   stack.push(value.vlInt,XDOMInt.toInt(s));
//		   return true;
//	       }
//	   }
       }
       if (! value.isError(oval,ival)) {
	   errors.error(Errors.EXEC,pos,"not an integer");
       }
       stack.pop();
       stack.pushError();
       return false;
   }

   public boolean getI(XDOM tree) {
       exec.execExp(tree,value.ctxEval);
       return getI(tree.pos);
   }

   public boolean getDate(XPos pos) {
       Object oval = stack.getTopOval();
       long ival = stack.getTopIval();
       if (oval instanceof VLDate) {
	   return true;
       } else {
	   if (! (oval instanceof String)) {
	       getS(pos);
	       oval = stack.getTopOval();
	       ival = stack.getTopIval();
	   }
	   if (XDOMValue.isString(oval,ival)) {
	       String s = XDOMValue.getString(oval,ival);
	       Calendar c = VLDate.toCalendar(s);
	       if (c != null) {
		   stack.pop();
		   VLDate vld = value.makeVLDate(c.getTimeZone());
		   c.setTimeZone(VLDate.gmtTZ);
		   stack.push(vld,c.getTime().getTime());
		   return true;
	       }
	   }
       }
       if (! value.isError(oval,ival)) {
	   errors.error(Errors.EXEC,pos,"not a date");
       }
       stack.pop();
       stack.pushError();
       return false;
   }

   public boolean getDate(XDOM tree) {
       exec.execExp(tree,value.ctxEval);
       return getDate(tree.pos);
   }

   public boolean getF(XPos pos) {
      Object oval = stack.getTopOval();
      long ival = stack.getTopIval();
      if (oval == value.vlFloat) {
	 return true;
      } else if (oval == value.vlInt) {
	  stack.pop();
	  stack.push(value.vlFloat,Double.doubleToLongBits((double) ival));
	  return true;
//      } else {
//	 if (! (oval instanceof String)) {
//	    getS(pos);
//	    oval = stack.getTopOval();
//	    ival = stack.getTopIval();
//	 }
//	 if (XDOMValue.isString(oval,ival)) {
//	     String s = XDOMValue.getString(oval,ival);
//	     if (XDOMFloat.isFloat(s)) {
//		 stack.pop();
//		 stack.push(value.vlFloat,
//			    Double.doubleToLongBits(XDOMFloat.toFloat(s)));
//		 return true;
//	    }
//	 }
      }
      if (! value.isError(oval,ival)) {
	 errors.error(Errors.EXEC,pos,"not a number");
      }
      stack.pop();
      stack.pushError();
      return false;
   }

   public boolean getF(XDOM tree) {
      exec.execExp(tree,value.ctxEval);
      return getF(tree.pos);
   }
}