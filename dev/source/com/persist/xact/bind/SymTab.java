/*******************************************************************************
*
* Copyright (c) 2002. John R. Nestor. All rights reserved.
*
* See the file XLICENSE.txt for use and redistribution information.
*
*******************************************************************************/

package com.persist.xact.bind;

import com.persist.xact.system.*;
import com.persist.xdom.*;

public final class SymTab {

   private Errors errors;
   private Bind bind;
   private XScope scope;
   
   private boolean debugSymTab = false;

   public SymTab(Bind bind) {
      this.bind = bind;
   }

   public void init() {
       errors = bind.xt.errors;
   }

   private String show(String name,boolean hasExt) {
      if (hasExt) {
	 return name + "[...]";
      } else {
	 return name;
      }
   }

   public void defineVirtual(String name,XDef def1,XDOMElement etree,XPos pos,boolean visible) {
      boolean hasExt = def1.hasExt;
      XScope scope = etree.scope;
      XDefVirtual def = new XDefVirtual();
      if (scope == null) {
	 scope = XScope.add(etree);
      } 
      def.name = name;
      def.hasExt = def1.hasExt;
      def.readOnly = def1.readOnly;
      def.def = def1;
      def.visible = visible;
      XDef old = scope.find(name,hasExt);
      if (old != null) {
	 errors.error(Errors.BIND,pos,"duplicate definition of name "+show(name,hasExt));
      } else {
	 scope.add(def);
	 if (debugSymTab) {
	    System.out.println("Defining virtual "+show(name,hasExt)+" in "+etree.getTag());
	 }
      }
   }
   
   public void defineSpecial(String name,XDOMElement etree) {
      XScope scope = etree.scope;
      XDefName ndef = new XDefName();
      if (scope == null) {
	 scope = XScope.add(etree);
      } 
      ndef.name = name;
      ndef.readOnly = true;
      XDef old = scope.find(name,false);
      if (old != null) {
	 errors.error(Errors.BIND,etree.pos,"duplicate definition of name "+name);
      } else {
	 scope.add(ndef);
	 if (debugSymTab) {
	    System.out.println("Defining "+name+" in "+etree.getTag());
	 }
      }
   }

   public void define(XDOMName ntree,XDOMElement etree,boolean readOnly,
		      boolean hasView,boolean visible,boolean local) {
      String name = ntree.getName();
      boolean hasExt = ntree.hasExt();
      XScope scope = etree.scope;
      XDefName ndef = new XDefName();
      if (scope == null) {
	 scope = XScope.add(etree);
      } 
      ndef.name = name;
      ndef.readOnly = readOnly;
      ndef.hasView = hasView;
      ndef.visible = visible;
      ndef.local = local;
      ndef.tree = ntree;
      ntree.def = ndef;
      XDef old = scope.find(name,hasExt);
      if (old != null) {
	 errors.error(Errors.BIND,ntree.pos,"duplicate definition of name "+show(name,hasExt));
      } else {
	 scope.add(ndef);
	 if (debugSymTab) {
	    System.out.println("Defining "+show(name,hasExt)+" in "+etree.getTag());
	 }
      }
   }

   public void defineType(XDOMName ntree,XDOMElement vtree,XDOMElement etree,
			  int exts,boolean varExts,boolean visible,boolean local,int assert1) {
       String name = ntree.getName();
       boolean hasExt = ntree.hasExt();
       XScope scope = etree.scope;
       XDefType tdef = new XDefType();
       if (scope == null) {
	   scope = XScope.add(etree);
       } 
       tdef.name = name;
       tdef.hasExt = hasExt;
       tdef.exts = exts;
       tdef.varExts = varExts;
       tdef.readOnly = true;
       tdef.tree = ntree;
       tdef.vtree = vtree;
       tdef.visible = visible;
       tdef.local = local;
       tdef.assert1 = assert1;
       ntree.def = tdef;
       XDef old = scope.find(name,hasExt);
       if (old != null) {
	   errors.error(Errors.BIND,ntree.pos,"duplicate definition of name "+show(name,hasExt));
       } else {
	   scope.add(tdef);
	   if (debugSymTab) {
	       System.out.println("Defining "+show(name,hasExt)+" in "+etree.getTag());
	   }
       }
   }

   public void defineView(XDOMName ntree,XDOMElement vtree,XDOMElement etree,
			  int exts,boolean varExts,boolean visible,boolean local,int assert1) {
      String name = ntree.getName();
      boolean hasExt = ntree.hasExt();
      XScope scope = etree.scope;
      XDefView vdef = new XDefView();
      if (scope == null) {
	 scope = XScope.add(etree);
      } 
      vdef.name = name;
      vdef.hasExt = hasExt;
      vdef.exts = exts;
      vdef.varExts = varExts;
      vdef.readOnly = true;
      vdef.tree = ntree;
      vdef.vtree = vtree;
      vdef.visible = visible;
      vdef.assert1 = assert1;

      vdef.local = local;
      ntree.def = vdef;
      XDef old = scope.find(name,hasExt);
      if (old != null) {
	 errors.error(Errors.BIND,ntree.pos,"duplicate definition of name "+show(name,hasExt));
      } else {
	 scope.add(vdef);
	 if (debugSymTab) {
	    System.out.println("Defining "+show(name,hasExt)+" in "+etree.getTag());
	 }
      }
   }
   public void defineThread(XDOMName ntree,XDOMElement ttree,XDOMElement etree) {
       String name = ntree.getName();
       XScope scope = etree.scope;
       boolean hasExt = false;
       XDefThread tdef = new XDefThread();
       if (scope == null) {
	   scope = XScope.add(etree);
       } 
       tdef.name = name;
       tdef.hasExt = hasExt;
       tdef.readOnly = true;
       tdef.tree = ntree;
       tdef.ttree = ttree;
       ntree.def = tdef;
       XDef old = scope.find(name,hasExt);
       if (old != null) {
	   errors.error(Errors.BIND,ntree.pos,"duplicate definition of name "+show(name,hasExt));
       } else {
	   scope.add(tdef);
	   if (debugSymTab) {
	       System.out.println("Defining "+show(name,hasExt)+" in "+etree.getTag());
	   }
       }
   }

   public void defineFunc(XDOMName ntree,XDOMElement ftree,XDOMElement etree,
			  int args,boolean hasArgs,boolean varFormals,
			  int exts,boolean varExts,boolean visible,boolean local,
			  int assert1) {
      String name = ntree.getName();
      boolean hasExt = ntree.hasExt();
      XScope scope = etree.scope;
      XDefFunc ndef = new XDefFunc();
      if (scope == null) {
	 scope = XScope.add(etree);
      } 
      ndef.name = name;
      ndef.hasExt = hasExt;
      ndef.readOnly = true;
      ndef.tree = ntree;
      ndef.ftree = ftree;
      ndef.hasArgs = hasArgs;
      ndef.varArgs = varFormals;
      ndef.args = args;
      ndef.exts = exts;
      ndef.varExts = varExts;
      ndef.visible = visible;
      ndef.local = local;
      ndef.assert1 = assert1;
      ntree.def = ndef;
      XDef old = scope.find(name,hasExt);
      if (old != null) {
	 errors.error(Errors.BIND,ntree.pos,"duplicate definition of name "+show(name,hasExt));
      } else {
	 scope.add(ndef);
	 if (debugSymTab) {
	    System.out.println("Defining "+show(name,hasExt)+" in "+etree.getTag());
	 }
      }
   }

   public void defineFunc2(XDOMName ntree,XDOMElement ftree,XDOMElement etree,
			   int args,boolean hasArgs,boolean varArgs,
			   int exts,boolean varExts, boolean lhs,
			   boolean visible,boolean local,
			   int assert1) {
      String name = ntree.getName();
      boolean hasExt = ntree.hasExt();
      XScope scope = etree.scope;
      XDefFunc2 ndef = null;
      boolean ok = true;
      if (scope == null) {
	 scope = XScope.add(etree);
      }
      XDef old = scope.find(name,hasExt);
      if (old == null) {
      } else if (old instanceof XDefFunc2) {
	 ndef = (XDefFunc2) old;
	 if (lhs && ndef.lhsTree != null) {
	    errors.error(Errors.BIND,ntree.pos,"duplicate lhs definition of name "+show(name,hasExt));
	    ok = false;
	 }
	 if (! lhs && ndef.tree != null) {
	    errors.error(Errors.BIND,ntree.pos,"duplicate rhs definition of name "+show(name,hasExt));
	    ok = false;
	 }
      } else {
	 errors.error(Errors.BIND,ntree.pos,"duplicate definition of name "+show(name,hasExt));
	 ok = false;
      }
      if (ndef == null || ! ok) {
	 ndef = new XDefFunc2();
	 ndef.name = name;
	 ndef.readOnly = true;
	 ndef.hasExt = hasExt;
      }
      ntree.def = ndef;
      if (lhs) {
	 ndef.lhsTree = ntree;
	 ndef.lhsFtree = ftree;
	 ndef.lhsArgs = args;
	 ndef.lhsVarArgs = varArgs;
	 ndef.lhsExts = exts;
	 ndef.lhsVarExts = varExts;
	 ndef.lhsVisible = visible;
	 ndef.lhsLocal = local;
	 ndef.lhsAssert1 = assert1;
      } else {
	 ndef.tree = ntree;
	 ndef.ftree = ftree;
	 ndef.hasArgs = hasArgs;
	 ndef.args = args;
	 ndef.varArgs = varArgs;
	 ndef.exts = exts;
	 ndef.varExts = varExts;
	 ndef.visible = visible;
	 ndef.local = local;
	 ndef.assert1 = assert1;
      }
      if (old != ndef) scope.add(ndef);
      if (ok) {
	 if (debugSymTab) {
	    if (lhs) {
	       System.out.println("Defining lhs "+show(name,hasExt)+" in "+etree.getTag());
	    } else {
	       System.out.println("Defining rhs "+show(name,hasExt)+" in "+etree.getTag());
	    }
	 }
      }
   }

   public void setScope(XScope scope) {
      this.scope = scope;
   }

   public XScope getScope() {
      return scope;
   }
   
   public void lookup(XDOMName ntree) {
      String name = ntree.getName();
      boolean hasExt = ntree.hasExt();
      String estr = "";
      if (hasExt) {
	 estr= "[...]";
      }
      if (ntree.def == null) {
	 XScope scope1 = scope;
	 while (scope1 != null) {
	    XDef def = scope1.find(name,hasExt);
	    if (def != null) {
	       ntree.def = def.getReal(); // avoid extra steps in execution
	       if (debugSymTab) {
		  System.out.println("Lookup "+ntree.getName()+estr);
	       }
	       return;
	    }
	    scope1 = scope1.parent;
	 }
	 errors.error(Errors.BIND,ntree.pos,"undefined name "+ntree.getName()+estr);
      }
   }

}