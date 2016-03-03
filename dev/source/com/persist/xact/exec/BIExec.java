/*******************************************************************************
*
* Copyright (c) 2002-2007. John R. Nestor. All rights reserved.
*
* See the file XLICENSE.txt for use and redistribution information.
*
*******************************************************************************/

package com.persist.xact.exec;

import com.persist.xact.system.*;
import com.persist.xact.bind.*;
import com.persist.xdom.*;
import com.persist.xact.value.*;

public final class BIExec {

    private XThread xt;
    private Exec exec;
    private JExec jExec;
    private CallExec callExec;
    private VariableStack stack;
    private Value value;
    private Errors errors;
    private Join join;
    private Frames frames;
    private EValue evalue;
    private TreeExec treeExec;
    private OpExec opExec;
    private InterExec interExec;
    private BIBind biBind;
   
   
    public BIExec(Exec exec) {
	this.exec = exec;
    }

    public void init() {
	xt = exec.xt;
	stack = exec.stack;
	jExec = exec.jExec;
	evalue = exec.evalue;
	treeExec = exec.treeExec;
	opExec = exec.opExec;
	interExec = exec.interExec;
	value = xt.cmd.value;
	biBind = xt.cmd.biBind;
	errors = xt.errors;
	join = exec.join;
	frames = exec.frames;
	callExec = exec.callExec;
    }
    
    public int hasView(Object oval,long ival,Object voval,long vival,XDOM caller) {
	XPos pos = null;
	if (caller != null) pos = caller.pos;
	boolean result = false;
	boolean ok = true;
	if (value.isError(oval,ival) || value.isError(voval,vival)) {
	    ok = false;
	} else if (voval instanceof VLBIV) {
	    switch ((int) vival) {
		case VLBIV.Tstring:{ result = XDOMValue.isString(oval,ival); break;}
		case VLBIV.Vfold:{ result = XDOMValue.isString(oval,ival); break;}
		case VLBIV.Vdate:{ result = value.isDate(oval,ival); break; }
		case VLBIV.Tint:{ result = value.isInt(oval,ival); break; }
		case VLBIV.Tfloat:{ result = value.isFloat(oval,ival); break; }
		case VLBIV.Vboolean:{ result = value.isBoolean(oval,ival); break;}
		case VLBIV.Tvoid: { result = value.isNull(oval,ival); break; }
		case VLBIV.Vtree: { result = oval instanceof XDOM; break; }
		case VLBIV.TtreeElement: { result = oval instanceof XDOMElement; break; }
		case VLBIV.Varray: { result = oval instanceof XDOMElement; break; }
		case VLBIV.Vlist: { result = oval instanceof XDOMElement; break; }
		case VLBIV.Vrec: { result = oval instanceof XDOMElement; break; }
		case VLBIV.Vattr: { result = oval instanceof XDOMElement; break; }
		case VLBIV.Vbody: { result = oval instanceof XDOMElement; break; }
		case VLBIV.Vbodyrec: { result = oval instanceof XDOMElement; break; }
		case VLBIV.TtreeString: { result = oval instanceof XDOMString; break; }
		case VLBIV.TtreeName: { result = oval instanceof XDOMName; break; }
		case VLBIV.TtreeInt: { result = oval instanceof XDOMInt; break; }
		case VLBIV.TtreeFloat: { result = oval instanceof XDOMFloat; break; }
		case VLBIV.TtreeCall: { result = oval instanceof XDOMCall; break; }
		case VLBIV.TtreeValue: { result = oval instanceof XDOMValue; break; }
		case VLBIV.Tfunc: { result = value.isFunc(oval,ival); break; }
		case VLBIV.Tthread: { result = oval instanceof VLThread; break; }
		case VLBIV.Tqueue: { result = oval instanceof VLQueue; break; }
		case VLBIV.Tstream: { result = oval instanceof VLStream; break; }
		case VLBIV.Tlock: { result = oval instanceof VLLock; break; }
		case VLBIV.Ttype: { result = value.isType(oval,ival); break; }
		case VLBIV.Tview: { result = value.isView(oval,ival); break; }
		case VLBIV.Vview:
		{
		    result = value.isType(oval,ival) || value.isView(oval,ival);
		    break;
		}
		default:
		{
		    errors.error(Errors.EXEC,pos,"unrecognized type or view");
		    ok = false;
		}
	    }
	} else if (voval instanceof VLType) {
	    VLType vlt = (VLType) voval;
	    if (oval instanceof VLObj) {
		VLObj vlo = (VLObj) oval;
		result = vlo.type == vlt;
	    }
	} else if (voval instanceof VLView) {
	    VLView vlv = (VLView) voval;
	    if (vlv.def.vtree.getTag() == "x:type") {
		if (oval instanceof VLObj) {
		    VLObj vlo = (VLObj) oval;
		    if (vlo.type.def.vtree == vlv.def.vtree) return 1; // same x:type decl
		    return 0;
		}
	    } 
	    stack.push(oval,ival); /* push the arg to sys:Is" */
	    XDef def = callExec.findCallView(vlv,"sys:Is",false,null);
	    ok = false;
	    if (def instanceof XDefFunc) {
		XDefFunc fdef = (XDefFunc) def;
		if (fdef.hasArgs) {
		    callExec.execCallView(def,vlv,true,
					  0,1,caller,value.ctxEval);
		    boolean isB = evalue.getB((XPos) null);
		    Object oval1 = stack.getTopOval();
		    long ival1 = stack.getTopIval();
		    stack.pop();
		    if (isB) {
			if (ival1 == 1) result = true;
			ok = true;
		    } else if (value.isError(oval1,ival1)) {
			result = true;
		    } else {
			errors.error(Errors.EXEC,pos,"result of sys:Is is not a boolean");
			result = true;
		    }
		}
	    }
	    if (! ok) {
		errors.error(Errors.EXEC,pos,"missing sys:Is func");
	    }
	} else {
	    errors.error(Errors.EXEC,pos,"not a type or view");
	    ok = false;
	}
	if (!ok) return -1;
	if (result) return 1;
	return 0;
    }

   public void execSubscript(Object oval,long ival,XDOMCall tree,Ctx ctx) {
      int size = tree.argSize();
      if (size != 1) {
	 errors.error(Errors.EXEC,tree.pos,"must be exactly 1 subscript");
      } else {
	    if (XDOMValue.isString(oval,ival)){
		opExec.execSSubscript(oval,ival,tree,ctx);
		return;
	    }
	    if (oval instanceof XDOM){
		XDOM x = (XDOM) oval;
		int kind = x.getXKind();
		if (kind == XDOM.XELEMENT) {
		    XDOMElement xe = (XDOMElement) x;
		    treeExec.arraySubscript(xe,tree,ctx);
		    return;
		} else if (kind == XDOM.XNAME) {
		    XDOMName xn = (XDOMName) x;
		    treeExec.extSubscript(xn,tree,ctx);
		    return;
		} else if (kind == XDOM.XCALL) {
		    XDOMCall xc = (XDOMCall) x;
		    treeExec.argSubscript(xc,tree,ctx);
		    return;
		}
	    }
	    opExec.execType(oval,ival);
	    Object ovalt = stack.getTopOval();
	    long ivalt = stack.getTopIval();
	    stack.pop();
	    if (ovalt instanceof VLType) {
		VLType vlt = (VLType) ovalt;
		XDef def = callExec.findCallObject(vlt,"sys:Subscript",false,null);
		if (def != null && def instanceof XDefFunc) {
		    XDefFunc fdef = (XDefFunc) def;
		    int cnt = callExec.pushArg(fdef,tree);
		    callExec.execCallObject(def,vlt,oval,ival,true,0,cnt,tree,value.ctxEval);
		    return;
		}
	    }
	    errors.error(Errors.EXEC,tree.pos,"subscript not available for this type");
      }
      if (ctx != value.ctxAssign) {
	 stack.pushError();
      }
   }

   private void execDot2(String name,XDOMCall tree,Ctx ctx) {
       XDOMName extTree = null;
       boolean hasExt = false;
       if (tree.getFunc() instanceof XDOMName) {
	   extTree = (XDOMName) tree.getFunc();
	   if (extTree.hasExt()) hasExt = true;
       }
       XDOM arg1 = tree.getArg(1);
       XDOM arg2 = tree.getArg(2);
       exec.execExp(arg1,value.ctxEval);
       Object oval = stack.getTopOval();
       long ival = stack.getTopIval();
       stack.pop();
       if (! hasExt) {
	   VLBIF vlf1  = findObjOp(name,oval,ival,null);
	   if (vlf1 != null) {
	       if (ctx == value.ctxAssign) {
		   errors.error(Errors.EXEC,tree.pos,"can't assign to this expression");
	       } else {
		   VLDot vlc = new VLDot();
		   vlc.voval = value.vlBIV;
		   vlc.vival = 0; /* never used !! */
		   vlc.f = vlf1;
		   vlc.selfoval = oval;
		   vlc.selfival = ival;
		   vlc.hasSelf = true;
		   stack.push(vlc,0);
	       }
	       return;
	   }
	   if (oval instanceof XDOM) {
	       XDOM x = (XDOM) oval;
	       int kind = x.getXKind();
	       switch (kind) {
		   case XDOM.XELEMENT:
		   {
		       XDOMElement xe = (XDOMElement) x;
		       if (treeExec.doAttr(xe,name,ctx,tree.pos)) return;
		       break;
		   }
		   case XDOM.XCALL:
		   {
		       XDOMCall xc = (XDOMCall) x;
		       if (treeExec.doAttr(xc,name,ctx,tree.pos)) return;
		       break;
		   }
		   case XDOM.XNAME:
		   {
		       XDOMName xn = (XDOMName) x;
		       if (treeExec.doAttr(xn,name,ctx,tree.pos)) return;
		       break;
		   }
		   case XDOM.XSTRING:
		   {
		       XDOMString xs = (XDOMString) x;
		       if (treeExec.doAttr(xs,name,ctx,tree.pos)) return;
		       break;
		   }
		   case XDOM.XINT:
		   {
		       XDOMInt xn = (XDOMInt) x;
		       if (treeExec.doAttr(xn,name,ctx,tree.pos)) return;
		       break;
		   }
		   case XDOM.XFLOAT:
		   {
		       XDOMFloat xn = (XDOMFloat) x;
		       if (treeExec.doAttr(xn,name,ctx,tree.pos)) return;
		       break;
		   }
		   case XDOM.XVALUE:
		   {
		       XDOMValue xv = (XDOMValue) x;
		       if (treeExec.doAttr(xv,name,ctx,tree.pos)) return;
		       break;
		   }
	       }
	   } else if (oval instanceof VLBIV) {
	       if (ctx == value.ctxAssign) {
		   errors.error(Errors.EXEC,tree.pos,"can't assign to this expression");
	       } else {
		   VLBIF vlf = findOp((int) ival,name);
		   if (vlf != null) {
		       stack.push(vlf,0);
		   } else {
		       errors.error(Errors.EXEC,tree.pos,"unrecognized operator");
		       stack.pushError();
		   }
	       }
	       return;
	   } else if (oval instanceof VLJava) {
	       if (ctx == value.ctxAssign) {
		   errors.error(Errors.EXEC,arg2.pos,"can't assign to Java names");
	       } else {
		   jExec.getClass(name,(VLJava) oval,arg2.pos);
		   return;
	       }
	   } else if (oval instanceof Class) {
	       Class c = (Class) oval;
	       jExec.doVar(name,null,c,ctx,arg2.pos);
	       return;
	   } else if (value.isJObject(oval,ival)) {
	       jExec.doVar(name,oval,oval.getClass(),ctx,arg2.pos);
	       return;
	   } else if (XDOMValue.isString(oval,ival)) {
	       if (ctx == value.ctxAssign) {
		   errors.error(Errors.EXEC,tree.pos,"can't assign to this expression");
	       } else if (name != "size") {
		   errors.error(Errors.EXEC,tree.pos,"not size");
	       } else {
		   stack.push(oval,ival);
		   evalue.getS((XPos) null);
		   oval = stack.getTopOval();
		   ival = stack.getTopIval();
		   stack.pop();
		   stack.push(value.vlInt,Value.stringSize(oval));
	       }
	       return;
	   } else if (oval instanceof VLThread) {
	       VLThread vlth = (VLThread) oval;
	       if (name == "parent") {
		   if (ctx == value.ctxAssign) {
		       errors.error(Errors.EXEC,tree.pos,"can't assign to this expression");
		   } else if (exec.interExec.pass != InterExec.PBIND1) {
		       errors.error(Errors.EXEC,tree.pos,
				    "parent only available during intepretation bind1");
		       stack.pushError();
		   } else {
		       stack.push(vlth.xt.bind.parent,0);
		   }
		   return;
	       } else if (name == "prev") {
		   if (ctx == value.ctxAssign) {
		       errors.error(Errors.EXEC,tree.pos,"can't assign to this expression");
		   } else if (exec.interExec.pass != InterExec.PBIND1) {
		       errors.error(Errors.EXEC,tree.pos,
				    "prev only available during intepretation bind1");
		       stack.pushError();
		   } else {
		       stack.push(vlth.xt.bind.prev,0);
		   }
		   return;
/*		   
	       } else if (name == "notHandled") {
		   if (exec.interExec.pass == InterExec.PNONE) {
		       errors.error(Errors.EXEC,tree.pos,
		       "notHandled only available during intepretation");
		       if (ctx != value.ctxAssign) stack.pushError();
		   } else if (ctx == value.ctxAssign) {
		       if (evalue.getB(tree.pos)) {
			   long ival1 = stack.getTopIval();
			   if (ival1 == 1) {
			       xt.exec.interExec.notHandled = true;
			   } else {
			       xt.exec.interExec.notHandled = false;
			   }
		       }
		   } else {
		       if (xt.exec.interExec.notHandled) {
			   stack.push(value.vlBool,1);
		       } else {
			   stack.push(value.vlBool,0);
		       }
		   }
		   return;
*/		   
	       }
	   }
       }

       if (oval instanceof VLView) {
	   VLView vlv = (VLView) oval;
	   XDef def = callExec.findCallView(vlv,name,hasExt,tree);
	   if (def != null) {
	       int ecnt = callExec.dotExt(def,name);
	       if (hasExt) ecnt += callExec.pushExt(def,extTree);
	       callExec.execCallView(def,vlv,false,ecnt,0,tree,ctx);
	       return;
	   }
       } else if (oval instanceof VLObj) {
	   VLObj vlo = (VLObj) oval;
	   XDef def = callExec.findCallObject(vlo.type,name,hasExt,tree);
	   if (def != null) {
	       int ecnt = 0;
	       if (hasExt) ecnt = callExec.pushExt(def,extTree);
	       callExec.execCallObject(def,vlo.type,vlo,0,false,
				       ecnt,0,tree,ctx);
	       return;
	   }
       } else if (value.isError(oval,ival)) {
       } else {
	   errors.error(Errors.EXEC,arg2.pos,"unrecognized dot expression");
       }
       if (ctx != value.ctxAssign) {
	   stack.pushError();
       }
   }

   private void execDot3(String name,XDOMCall tree,Ctx ctx) {
       XDOMName extTree = null;
       boolean hasExt = false;
       if (tree.getFunc() instanceof XDOMName) {
	   extTree = (XDOMName) tree.getFunc();
	   if (extTree.hasExt()) hasExt = true;
       }
       XDOM arg1 = tree.getArg(1);
       XDOM arg2 = tree.getArg(2);
       XDOM arg3 = tree.getArg(3);
       exec.execExp(arg3,value.ctxEval);
       Object oval = stack.getTopOval();
       long ival = stack.getTopIval();
       stack.pop();
       boolean ok = true;
       if (! hasExt) {
	   ok = false;
	   if (oval instanceof VLBIV) {
	       switch ((int) ival) {
		   case VLBIV.Vattr:
		   {
		       if (ctx == value.ctxAssign) {
			   errors.error(Errors.EXEC,tree.pos,"assignment to this expression not permitted");
		       } else {
			   if(name=="size") {
			       treeExec.attrSize(tree);
			       return;
			   }
		       }
		       break;
		   }
		   case VLBIV.Vbody:
		   {
		       if (ctx == value.ctxAssign) {
			   errors.error(Errors.EXEC,tree.pos,"assignment to this expression not permitted");
		       } else {
			   if(name=="size") {
			       treeExec.bodySize(tree);
			       return;
			   }
		       }
		       break;
		   }
		   case VLBIV.Vrec:
		   {
		       treeExec.recordSelect(arg1,name,tree.pos,ctx);
		       return;
		   }
		   case VLBIV.Vbodyrec:
		   {
		       treeExec.bodyRecordSelect(arg1,name,tree.pos,ctx);
		       return;
		   }
		   case VLBIV.Vdate:
		   {
		       if (opExec.DTfield(arg1,name,tree.pos,ctx)) return;
		   }
	       }
	       exec.execExp(arg1,value.ctxEval);
	       Object selfoval = stack.getTopOval();
	       long selfival = stack.getTopIval();
	       stack.pop();
	       VLBIF vlf  = findObjOp((int) ival,name,selfoval,selfival,tree);
	       if (vlf == null) {
	       } else if (ctx == value.ctxAssign) {
		   errors.error(Errors.EXEC,tree.pos,"can't assign to this expression");
	       } else {
		   VLDot vlc = new VLDot();
		   vlc.voval = value.vlBIV;
		   vlc.vival = ival;
		   vlc.f = vlf;
		   vlc.selfoval = selfoval;
		   vlc.selfival = selfival;
		   vlc.hasSelf = true;
		   stack.push(vlc,0);
		   return;
	       }
	   }
       }
       if (oval instanceof VLView) {
	   VLView vlv = (VLView) oval;
	   exec.execExp(arg1,value.ctxEval);
	   Object oval1 = stack.getTopOval();
	   long ival1 = stack.getTopIval();
	   stack.pop();
	   XDef def = callExec.findCallObject(vlv,name,hasExt,tree);
	   if (def != null) {
	       int ecnt = 0;
	       if (hasExt) ecnt = callExec.pushExt(def,extTree);
	       callExec.execCallObject(def,vlv,oval1,ival1,false,
				       ecnt,0,tree,ctx);
	       return;
	   }
       } else if (! ok || value.isError(oval,ival)) {
       } else if (ok) {
	   errors.error(Errors.EXEC,arg3.pos,"not a view");
       } else {
	   errors.error(Errors.EXEC,arg2.pos,"unrecognized dot expression");
       }
       if (ctx != value.ctxAssign) {
	   stack.pushError();
       }
   }

   private void execDot(XDOMCall tree,Ctx ctx) {
       int size = tree.argSize();
       if (size < 2 || size > 3) {
	   errors.error(Errors.EXEC,tree.pos,"wrong number of actual parameters");	 
       } else {
	   XDOM arg2 = tree.getArg(2);
	   Object oval;
	   long ival;
	   if (arg2 instanceof XDOMString) {
	       /* avoid eval and extra debug step */
	       XDOMString stree = (XDOMString) arg2;
	       oval = stree.getOval();
	       ival = stree.getIval();
	   } else {
	       evalue.getS(arg2);
	       oval = stack.getTopOval();
	       ival = stack.getTopIval();
	       stack.pop();
	   }
	   if (XDOMValue.isString(oval,ival)) {
	       String name = XDOMValue.getString(oval,ival);
	       if (size == 2) {
		   execDot2(name,tree,ctx);
	       } else {
		   execDot3(name,tree,ctx);
	       }
	       return;
	   } else {
	       errors.error(Errors.EXEC,arg2.pos,"not a string");
	   }
       }
       if (ctx != value.ctxAssign) {
	   stack.pushError();
       }
   }
   
   public void execMake(long view,XDOMCall tree,Ctx ctx) {
      switch ((int) view) {
	 case VLBIV.TtreeElement: {treeExec.makeTreeElement(tree);return;}
	 case VLBIV.TtreeString: {treeExec.makeTreeString(tree);return;}
	 case VLBIV.TtreeInt: {treeExec.makeTreeInt(tree);return;}
	 case VLBIV.TtreeFloat: {treeExec.makeTreeFloat(tree);return;}
	 case VLBIV.TtreeName: {treeExec.makeTreeName(tree);return;}
	 case VLBIV.TtreeValue: {treeExec.makeTreeValue(tree);return;}
	 case VLBIV.TtreeCall: {treeExec.makeTreeCall(tree);return;}
	 case VLBIV.Vrec: {treeExec.makeRec(tree);return;}
	 case VLBIV.Vlist: {treeExec.makeList(tree);return;}
	 case VLBIV.Varray: {treeExec.makeArray(tree);return;}
	 case VLBIV.Tstring: {opExec.makeString(tree,ctx);return;}
	 case VLBIV.Vdate: {opExec.makeDate(tree,ctx);return;}
	 case VLBIV.Tint: {opExec.makeInt(tree);return;}
	 case VLBIV.Tfloat: {opExec.makeFloat(tree);return;}
	 case VLBIV.Vfold: {opExec.makeFold(tree,ctx);return;}
	 case VLBIV.Tqueue: {opExec.makeQueue(tree);return;}
	 case VLBIV.Tstream: {opExec.makeStream(tree);return;}
	 case VLBIV.Tlock: {opExec.makeLock(tree);return;}
	 case VLBIV.TLxact: {interExec.makeXact(tree);return;}
	 case VLBIV.TLerror: {interExec.makeError(tree);return;}
	 case VLBIV.TLrender: {interExec.makeRender(tree);return;}
      }
      errors.error(Errors.EXEC,tree.pos,"don't know how to make this type or view");
      stack.pushError();
   }

   public void execBIName(VLBIF vlf,XDOMName tree,Ctx ctx) {
       if (ctx == value.ctxAssign) {
	   errors.error(Errors.EXEC,tree.pos,"can't assign to this expression");
	   return;
       }
       int code = vlf.kind;
       switch (code) {
	   case VLBIF.type:{opExec.execType(tree);return;}
	   case VLBIF.dobreak:{opExec.execDoBreak(tree);return;}
	   case VLBIF.langcurrent:{opExec.execLangCurrent(tree);return;}
	   case VLBIF.rangeE:{opExec.execRangeE(tree);return;}
	   case VLBIF.chars:{opExec.execChars(tree);return;}
	   case VLBIF.lines:{opExec.execLines(tree);return;}
	   case VLBIF.dot:
	   {
	       errors.error(Errors.EXEC,tree.pos,"Dot call must have parameters");
	       break;
	   }
	   default:
	   {
	       errors.error(Errors.EXEC,tree.pos,"unrecognized function:"+code);
	       break;
	   }
       }
       stack.pushError();
   }

   public void execBICall(VLBIF vlf,XDOMCall tree,Ctx ctx) {
       int i1 = stack.getTop();
       int incr = 1;
       int code = vlf.kind;
       int size = tree.argSize();
       if (! vlf.varArgs) {
	   if (size != vlf.args) {
	       errors.error(Errors.EXEC,tree.pos,"wrong number of actual parameters");
	       if (ctx != value.ctxAssign) {
		   stack.pushError();
	       }
	       return;
	   }
       }
       if (ctx == value.ctxAssign) {
	   if (! vlf.lhs) {
	       errors.error(Errors.EXEC,tree.pos,"assignment to this expression not permitted");
	       return;
	   }
	   incr = 0;
       }
       switch (code) {
	   case VLBIF.dot:{execDot(tree,ctx);break;}
	   case VLBIF.assign:{opExec.execAssign(tree);break;}
	   case VLBIF.range:{opExec.execRange(tree);break;}
	   case VLBIF.rangeE:{opExec.execRangeE(tree,ctx);break;}
	   case VLBIF.chars:{opExec.execChars(tree,ctx);break;}
	   case VLBIF.lines:{opExec.execLines(tree,ctx);break;}

	   case VLBIF.Band:{opExec.execBand(tree);break;}
	   case VLBIF.Bor:{opExec.execBor(tree);break;}
	   case VLBIF.Bxor:{opExec.execBxor(tree);break;}
	   case VLBIF.Bnot:{opExec.execBnot(tree);break;}

	   case VLBIF.cat:{opExec.execCat(tree,ctx);break;}
	   case VLBIF.cover:{opExec.execCover(tree,ctx);break;}
	   case VLBIF.add:{opExec.execAdd(tree,ctx);break;}
	   case VLBIF.sub:{opExec.execSub(tree,ctx);break;}
	   case VLBIF.mult:{opExec.execMult(tree,ctx);break;}
	   case VLBIF.div:{opExec.execDiv(tree,ctx);break;}
	   case VLBIF.rem:{opExec.execRem(tree,ctx);break;}
	   case VLBIF.minus:{opExec.execMinus(tree,ctx);break;}
	   case VLBIF.and:{opExec.execAnd(tree,ctx);break;}
	   case VLBIF.or:{opExec.execOr(tree,ctx);break;}
	   case VLBIF.xor:{opExec.execXor(tree,ctx);break;}
	   case VLBIF.not:{opExec.execNot(tree,ctx);break;}
	   case VLBIF.eq:{opExec.execEq(tree,ctx);break;}
	   case VLBIF.ne:{opExec.execNe(tree,ctx);break;}
	   case VLBIF.ls:{opExec.execLs(tree,ctx);break;}
	   case VLBIF.le:{opExec.execLe(tree,ctx);break;}
	   case VLBIF.gt:{opExec.execGt(tree,ctx);break;}
	   case VLBIF.ge:{opExec.execGe(tree,ctx);break;}

	   case VLBIF.tilde:{opExec.execTilde(tree);break;}
	   case VLBIF.Assert:{opExec.execAssert(tree,null);break;}
	   case VLBIF.question:{opExec.execQuestion(tree);break;}
	   case VLBIF.equal:{opExec.execEqual(tree);break;}
	   case VLBIF.XDOMValue:{opExec.execTreeValue(tree);break;}
	   case VLBIF.percent:{opExec.execPercent(tree);break;}
	   case VLBIF.comment:{stack.pushNull(); break;}
	   case VLBIF.URLParseQuery:{opExec.URLParseQuery(tree);break;}
	   case VLBIF.URLUnparseQuery:{opExec.URLUnparseQuery(tree);break;}
			      
	   case VLBIF.parse:{interExec.execParse(tree);break;}
	   case VLBIF.bind:{interExec.execBind(tree);break;}
	   case VLBIF.exec:{interExec.execExec(tree,ctx);break;}
	   case VLBIF.Wexp:{interExec.walkExp(tree,ctx);break;}
	   case VLBIF.Wbody:{interExec.walkBody(tree,ctx);break;}
	   case VLBIF.Wother:{interExec.walkOther(tree,ctx);break;}
	   case VLBIF.Wassign:{interExec.walkAssign(tree);break;}
	   case VLBIF.Wdefine:{interExec.walkDefine(tree);break;}
	   case VLBIF.error:{opExec.execError(tree);break;}
	   case VLBIF.warn:{opExec.execWarn(tree);break;}
	   case VLBIF.log:{opExec.execLog(tree);break;}

	   case VLBIF.Xeq:{opExec.execXcomp(tree,true);break;}
	   case VLBIF.Xne:{opExec.execXcomp(tree,false);break;}
	   case VLBIF.Sls:{opExec.execScomp(tree,true,false,false);break;}
	   case VLBIF.Sle:{opExec.execScomp(tree,true,true,false);break;}
	   case VLBIF.Sgt:{opExec.execScomp(tree,false,false,true);break;}
	   case VLBIF.Sge:{opExec.execScomp(tree,false,true,true);break;}
	   case VLBIF.Scat:{opExec.execScat(tree,ctx);break;}
	   case VLBIF.SFeq:{opExec.execSFcomp(tree,false,true,false);break;}
	   case VLBIF.SFne:{opExec.execSFcomp(tree,true,false,true);break;}
	   case VLBIF.SFls:{opExec.execSFcomp(tree,true,false,false);break;}
	   case VLBIF.SFle:{opExec.execSFcomp(tree,true,true,false);break;}
	   case VLBIF.SFgt:{opExec.execSFcomp(tree,false,false,true);break;}
	   case VLBIF.SFge:{opExec.execSFcomp(tree,false,true,true);break;}

	   case VLBIF.DTeq:{opExec.execDTeq(tree);break;}
	   case VLBIF.DTne:{opExec.execDTne(tree);break;}
	   case VLBIF.DTls:{opExec.execDTls(tree);break;}
	   case VLBIF.DTle:{opExec.execDTle(tree);break;}
	   case VLBIF.DTgt:{opExec.execDTgt(tree);break;}
	   case VLBIF.DTge:{opExec.execDTge(tree);break;}
	   case VLBIF.DTsub:{opExec.execDTsub(tree);break;}
	   case VLBIF.DTnow:{opExec.execDTnow(tree);break;}
	   case VLBIF.DTgmt:{opExec.execDTgmt(tree);break;}

	   case VLBIF.Iadd:{opExec.execIadd(tree);break;}
	   case VLBIF.Isub:{opExec.execIsub(tree);break;}
	   case VLBIF.Iminus:{opExec.execIminus(tree);break;}
	   case VLBIF.Imult:{opExec.execImult(tree);break;}
	   case VLBIF.Idiv:{opExec.execIdiv(tree);break;}
	   case VLBIF.Ieq:{opExec.execIeq(tree);break;}
	   case VLBIF.Ine:{opExec.execIne(tree);break;}
	   case VLBIF.Ils:{opExec.execIls(tree);break;}
	   case VLBIF.Ile:{opExec.execIle(tree);break;}
	   case VLBIF.Igt:{opExec.execIgt(tree);break;}
	   case VLBIF.Ige:{opExec.execIge(tree);break;}

	   case VLBIF.Fadd:{opExec.execFadd(tree);break;}
	   case VLBIF.Fsub:{opExec.execFsub(tree);break;}
	   case VLBIF.Fminus:{opExec.execFminus(tree);break;}
	   case VLBIF.Fmult:{opExec.execFmult(tree);break;}
	   case VLBIF.Fdiv:{opExec.execFdiv(tree);break;}
	   case VLBIF.Feq:{opExec.execFcomp(tree,false,true,false);break;}
	   case VLBIF.Fne:{opExec.execFcomp(tree,true,false,true);break;}
	   case VLBIF.Fls:{opExec.execFcomp(tree,true,false,false);break;}
	   case VLBIF.Fle:{opExec.execFcomp(tree,true,true,false);break;}
	   case VLBIF.Fgt:{opExec.execFcomp(tree,false,false,true);break;}
	   case VLBIF.Fge:{opExec.execFcomp(tree,false,true,true);break;}

	   default:
	   {
	       errors.error(Errors.INTERNAL,tree.pos,"unrecognized function:"+code);
	       stack.pushError();
	       break;
	   }
       }
       int i2 = stack.getTop();
       if (i2 != i1+incr) {
	   System.out.println("execBiCall(code="+code+") failed:"+i1+":"+i2);
	   errors.error(Errors.INTERNAL,tree.pos,"execTree sync error");
//	    new Throwable("execTree syncError").printStackTrace();
       }
   }

   public void execBICall1(VLBIF vlf,XDOMCall tree) {
       execBICall(vlf,tree,value.ctxEval);
   }
   
   public VLBIF findOp(int val,String op) {
       switch (val) {
	   case VLBIV.Tstring:
	   {
	       if(op=="sys:Eq") return biBind.Xeq;
	       if(op=="sys:Ne") return biBind.Xne;
	       if(op=="sys:Less") return biBind.Sls;
	       if(op=="sys:LessEq") return biBind.Sle;
	       if(op=="sys:Greater") return biBind.Sgt;
	       if(op=="sys:GreaterEq") return biBind.Sge;
	       break;
	   }
	   case VLBIV.Vfold:
	   {
	       if(op=="sys:Eq") return biBind.SFeq;
	       if(op=="sys:Ne") return biBind.SFne;
	       if(op=="sys:Less") return biBind.SFls;
	       if(op=="sys:LessEq") return biBind.SFle;
	       if(op=="sys:Greater") return biBind.SFgt;
	       if(op=="sys:GreaterEq") return biBind.SFge;
	       break;
	   }
	   case VLBIV.Vdate:
	   {
	       if(op=="sys:Eq") return biBind.DTeq;
	       if(op=="sys:Ne") return biBind.DTne;
	       if(op=="sys:Less") return biBind.DTls;
	       if(op=="sys:LessEq") return biBind.DTle;
	       if(op=="sys:Greater") return biBind.DTgt;
	       if(op=="sys:GreaterEq") return biBind.DTge;
	       if(op=="sys:Sub") return biBind.DTsub;
	       if(op=="now") return biBind.DTnow;
	       if(op=="gmt") return biBind.DTgmt;
	   }
	   case VLBIV.Tint:
	   {
	       if(op=="sys:Eq") return biBind.Ieq;
	       if(op=="sys:Ne") return biBind.Ine;
	       if(op=="sys:Less") return biBind.Ils;
	       if(op=="sys:LessEq") return biBind.Ile;
	       if(op=="sys:Greater") return biBind.Igt;
	       if(op=="sys:GreaterEq") return biBind.Ige;
	       if(op=="sys:Add") return biBind.Iadd;
	       if(op=="sys:Sub") return biBind.Isub;
	       if(op=="sys:Minus") return biBind.Iminus;
	       if(op=="sys:Mult") return biBind.Imult;
	       if(op=="sys:Div") return biBind.Idiv;
	       if(op=="sys:Rem") return biBind.Irem;
	       break;
	   }
	   case VLBIV.Tfloat:
	   {
	       if(op=="sys:Eq") return biBind.Feq;
	       if(op=="sys:Ne") return biBind.Fne;
	       if(op=="sys:Less") return biBind.Fls;
	       if(op=="sys:LessEq") return biBind.Fle;
	       if(op=="sys:Greater") return biBind.Fgt;
	       if(op=="sys:GreaterEq") return biBind.Fge;
	       if(op=="sys:Add") return biBind.Fadd;
	       if(op=="sys:Sub") return biBind.Fsub;
	       if(op=="sys:Minus") return biBind.Fminus;
	       if(op=="sys:Mult") return biBind.Fmult;
	       if(op=="sys:Div") return biBind.Fdiv;
	       break;
	   }
       }
       return null;
   }

   public void callObjOp(VLBIF vlf,Object oval,long ival,XDOMCall tree,Ctx ctx) {
       int code = vlf.kind;
       int size = tree.argSize();
       if (! vlf.varArgs) {
	   if (size != vlf.args) {
	       errors.error(Errors.EXEC,tree.pos,"wrong number of actual parameters");
	       if (ctx != value.ctxAssign) {
		   stack.pushError();
	       }
	       return;
	   }
       }
       if (ctx == value.ctxAssign) {
	   if (! vlf.lhs) {
	       errors.error(Errors.EXEC,tree.pos,"assignment to this expression not permitted");
	       return;
	   }
       }
       switch (code) {
	   case VLBIF.DTadd:{opExec.DTadd(oval,ival,tree);return;}
	   case VLBIF.read:{opExec.execRead(oval,ival,tree,ctx);return;}
	   case VLBIF.write:{opExec.execWrite(oval,ival,tree);return;}
	   case VLBIF.append:{opExec.execAppend(oval,ival,tree);return;}
	   case VLBIF.exists:{opExec.execExists(oval,ival,tree);return;}
	   case VLBIF.create:{opExec.execCreate(oval,ival,tree);return;}
	   case VLBIF.delete:{opExec.execDelete(oval,ival,tree);return;}
	   case VLBIF.isDirectory:{opExec.execIsDirectory(oval,ival,tree);return;}
	   case VLBIF.createDirectory:{opExec.execCreateDirectory(oval,ival,tree);return;}
	   case VLBIF.files:{opExec.execFiles(oval,ival,tree,ctx);return;}
	   case VLBIF.rename:{opExec.execRename(oval,ival,tree);return;}
	   case VLBIF.BodySubscript:{treeExec.bodySubscript((XDOMElement)oval,tree,ctx);return;}
	   case VLBIF.BodyInsert:{treeExec.bodyInsert((XDOMElement)oval,tree);return;}
	   case VLBIF.BodyDelete:{treeExec.bodyDelete((XDOMElement)oval,tree);return;}
	   case VLBIF.BodyAll:{treeExec.bodyAll((XDOMElement)oval,tree,ctx);return;}
	   case VLBIF.AttrSubscript:{treeExec.attrSubscript((XDOMElement)oval,tree,ctx);return;}
	   case VLBIF.AttrInsert:{treeExec.attrInsert((XDOMElement)oval,tree);return;}
	   case VLBIF.AttrDelete:{treeExec.attrDelete((XDOMElement)oval,tree);return;}
	   case VLBIF.AttrAll:{treeExec.attrAll((XDOMElement)oval,tree,ctx);return;}
	   case VLBIF.ExtInsert:{treeExec.extInsert((XDOMName)oval,tree);return;}
	   case VLBIF.ExtDelete:{treeExec.extDelete((XDOMName)oval,tree);return;}
	   case VLBIF.ExtAll:{treeExec.extAll((XDOMName)oval,tree,ctx);return;}
	   case VLBIF.ArgInsert:{treeExec.argInsert((XDOMCall)oval,tree);return;}
	   case VLBIF.ArgDelete:{treeExec.argDelete((XDOMCall)oval,tree);return;}
	   case VLBIF.ArgAll:{treeExec.argAll((XDOMCall)oval,tree,ctx);return;}
	   case VLBIF.ArrayInsert:{treeExec.arrayInsert((XDOMElement)oval,tree);return;}
	   case VLBIF.ArrayDelete:{treeExec.arrayDelete((XDOMElement)oval,tree);return;}
	   case VLBIF.ArrayAll:{treeExec.arrayAll((XDOMElement)oval,tree,ctx);return;}
	   case VLBIF.QueueSend:{opExec.execQueueSend((VLQueue)oval,tree);return;}
	   case VLBIF.QueueReceive:{opExec.execQueueReceive((VLQueue)oval,tree);return;}
	   case VLBIF.StreamSend:{opExec.execStreamSend((VLStream)oval,tree);return;}
	   case VLBIF.StreamClose:{opExec.execStreamClose((VLStream)oval,tree);return;}
	   case VLBIF.StreamReceive:{opExec.execStreamReceive((VLStream)oval,tree,ctx);return;}
	   case VLBIF.LockWait:{opExec.execLockWait((VLLock)oval,tree);return;}
	   case VLBIF.LockNotify:{opExec.execLockNotify((VLLock)oval,tree);return;}
	   case VLBIF.LockNotifyAll:{opExec.execLockNotifyAll((VLLock)oval,tree);return;}
	   default: {errors.error(Errors.INTERNAL,tree.pos,"callObjOp: bad op"); }
       }
       if (ctx != value.ctxAssign) {
	   stack.pushError();
       }
   }

   public VLBIF findObjOp(String op,Object oval,long ival,XDOMCall ctree) {
       if (oval instanceof XDOM) {
	   XDOM x = (XDOM) oval;
	   int kind = x.getXKind();
	   if (op=="insert") {
	       if (kind == XDOM.XCALL) return biBind.ArgInsert;
	       if (kind == XDOM.XNAME) return biBind.ExtInsert;
	       if (kind == XDOM.XELEMENT) return biBind.ArrayInsert;
	   } else if (op=="delete") {
	       if (kind == XDOM.XCALL) return biBind.ArgDelete;
	       if (kind == XDOM.XNAME) return biBind.ExtDelete;
	       if (kind == XDOM.XELEMENT) return biBind.ArrayDelete;
	   } else if (op=="all") {
	       if (kind == XDOM.XCALL) return biBind.ArgAll;
	       if (kind == XDOM.XNAME) return biBind.ExtAll;
	       if (kind == XDOM.XELEMENT) return biBind.ArrayAll;
	   }
       } else if (oval instanceof VLLock) {
	   if (op=="wait") return biBind.LockWait;
	   if (op=="notify") return biBind.LockNotify;
	   if (op=="notifyAll") return biBind.LockNotifyAll;
       } else if (oval instanceof VLQueue) {
	   if (op=="send") return biBind.QueueSend;
	   if (op=="receive") return biBind.QueueReceive;
       } else if (oval instanceof VLStream) {
	   if (op=="send") return biBind.StreamSend;
	   if (op=="close") return biBind.StreamClose;
	   if (op=="receive") return biBind.StreamReceive;
       }
       if (ctree != null) {
	   errors.error(Errors.EXEC,ctree.pos,"unrecognized dot expression");
       }
       return null;
   }

   public VLBIF findObjOp(int val,String op,Object oval,long ival,XDOMCall ctree) {
       if (value.isError(oval,ival)) return null;
       switch (val) {
	   case VLBIV.Vbody:
	   {
	       if (oval instanceof XDOMElement) {
		   if (op=="sys:Subscript") return biBind.BodySubscript;
		   if (op=="insert") return biBind.BodyInsert;
		   if (op=="delete") return biBind.BodyDelete;
		   if (op=="all") return biBind.BodyAll;
	       }
	       if (ctree != null) {
		   errors.error(Errors.EXEC,ctree.getArg(1).pos,"not an xdom element");
	       }
	       return null;
	   }
	   case VLBIV.Vattr:
	   {
	       if (oval instanceof XDOMElement) {
		   if (op=="sys:Subscript") return biBind.AttrSubscript;
		   if (op=="insert") return biBind.AttrInsert;
		   if (op=="delete") return biBind.AttrDelete;
		   if (op=="all") return biBind.AttrAll;
	       }
	       if (ctree != null) {
		   errors.error(Errors.EXEC,ctree.getArg(1).pos,"not an xdom element");
	       }
	       return null;
	   }
	   case VLBIV.Vfile:
	   {
	       if (op=="read") return biBind.read;
	       if (op=="write") return biBind.write;
	       if (op=="append") return biBind.append;
	       if (op=="exists") return biBind.exists;
	       if (op=="create") return biBind.create;
	       if (op=="delete") return biBind.delete;
	       if (op=="isDirectory") return biBind.isDirectory;
	       if (op=="createDirectory") return biBind.createDirectory;
	       if (op=="files") return biBind.files;
	       if (op=="rename") return biBind.rename;
	       break;
	   }
	   case VLBIV.Vdate:
	   {
	       if (op=="add") return biBind.DTadd;
	   }
       }
       if (ctree != null) {
	   errors.error(Errors.EXEC,ctree.pos,"unrecognized dot expression");
       }
       return null;
   }

   public void execExt(VLBIF vlf,VLExt vle,XDOMCall ctree,Ctx ctx) {
       int args = ctree.argSize();
       if (ctx == value.ctxAssign && ! vlf.lhs) {
	   errors.error(Errors.EXEC,ctree.pos,"can't assign to this expression");
       } else if (vlf.args != args) {
	   errors.error(Errors.EXEC,ctree.pos,"wrong number of actual parameters");
       } else if (vlf.kind == VLBIF.rangeE) {
	   XDOM arg1 = ctree.getArg(1);
	   exec.execExp(arg1,value.ctxEval);
	   Object oval = stack.getTopOval();
	   long ival = stack.getTopIval();
	   stack.pop();
	   if (oval instanceof VLFunc) {
	       VLFunc vlf1 = (VLFunc) oval;
	       opExec.Range(vle.eivals[0],vle.eivals[1],vlf1,ctree,ctx);
	       return;
	   } else {
	       if (! value.isError(oval,ival)) {
		   errors.error(Errors.EXEC,arg1.pos,"iterator parameter not a function");
	       }
	   }
       } else if (vlf.kind == VLBIF.chars) {
	   XDOM arg1 = ctree.getArg(1);
	   exec.execExp(arg1,value.ctxEval);
	   Object oval = stack.getTopOval();
	   long ival = stack.getTopIval();
	   stack.pop();
	   if (oval instanceof VLFunc) {
	       VLFunc vlf1 = (VLFunc) oval;
	       opExec.Chars(vle.eovals[0],vle.eivals[0],vlf1,ctree,ctx);
	       return;
	   } else {
	       if (! value.isError(oval,ival)) {
		   errors.error(Errors.EXEC,arg1.pos,"iterator parameter not a function");
	       }
	   }
       } else if (vlf.kind == VLBIF.lines) {
	   XDOM arg1 = ctree.getArg(1);
	   exec.execExp(arg1,value.ctxEval);
	   Object oval = stack.getTopOval();
	   long ival = stack.getTopIval();
	   stack.pop();
	   if (oval instanceof VLFunc) {
	       VLFunc vlf1 = (VLFunc) oval;
	       opExec.Lines(vle.eovals[0],vle.eivals[0],vlf1,ctree,ctx);
	       return;
	   } else {
	       if (! value.isError(oval,ival)) {
		   errors.error(Errors.EXEC,arg1.pos,"iterator parameter not a function");
	       }
	   }
       } else {
	   errors.error(Errors.INTERNAL,ctree.pos,"bad BIVLExt");
       }
       if (ctx != value.ctxAssign) {
	   stack.pushError();
       }
   }
}