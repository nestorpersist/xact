/*******************************************************************************
*
* Copyright (c) 2002-2007. John R. Nestor. All rights reserved.
*
* See the file XLICENSE.txt for use and redistribution information.
*
*******************************************************************************/

package com.persist.xact.bind;

import com.persist.xact.system.*;
import com.persist.xdom.*;
import com.persist.xact.exec.*;
import com.persist.xact.value.*;

public final class Bind {
    public XThread xt;
    private BIBind biBind;
    public SymTab symtab;

    private Errors errors;

    public XDOMElement parent;
    public XDOM prev = null;

    public String myPath = "";

    private XDOMElement info;
    public Bind(XThread xt) {
	this.xt = xt;
	symtab = new SymTab(this);
    }

    public void init() {
	biBind = xt.cmd.biBind;
	errors = xt.errors;
	symtab.init();
	info = new XDOMElement("info");
    }

    private boolean lhs;
    private boolean rhs;
    private boolean visible;
    private boolean local;
    private int assert1;

    private void callableOptions(XDOMElement etree,boolean isFunc) {
	lhs = false;
	rhs = false;
	visible = false;
	local = false;
	assert1 = -1;
	int asize = etree.attrSize();
	for (int i = 2; i <= asize; i++) {
	    XDOM elem = etree.getAttr(i);
	    boolean ok = false;
	    if (elem instanceof XDOMName) {
		XDOMName ntree = (XDOMName) elem;
		String name = ntree.getName();
		ok = true;
		if (isFunc && name == "lhs") {
		    if (lhs) {
			errors.error(Errors.BIND,elem.pos,"lhs specified more than once");
		    } else {
			lhs = true;
		    }
		} else if (isFunc && name == "rhs") {
		    if (rhs) {
			errors.error(Errors.BIND,elem.pos,"rhs specified more than once");
		    } else {
			rhs = true;
		    }
		} else if (name == "visible") {
		    if (visible) {
			errors.error(Errors.BIND,elem.pos,"visible specified more than once");
		    } else {
			visible = true;
		    }
		} else if (name == "local") {
		    if (local) {
			errors.error(Errors.BIND,elem.pos,"local specified more than once");
		    } else {
			local = true;
		    }
		} else {
		    ok = false;
		}
	    } else if (elem instanceof XDOMCall) {
		XDOMCall xc = (XDOMCall) elem;
		XDOM func = xc.getFunc();
		if (func instanceof XDOMName && xc.argSize() == 1 && xc.getKind() == XDOMCall.COP) {
		    XDOMName xn = (XDOMName) func;
		    ok = true;
		    if (assert1 != -1) {
			errors.error(Errors.BIND,elem.pos,"only one assertion permitted");
		    } else {
			assert1 = i;
		    }
		}
	    }
	    if (! ok) {
		errors.error(Errors.BIND,elem.pos,"unrecognized attribute");
	    }
	}
    }

    private void bind1funcname(XDOMName ntree,XDOMElement ftree,
			       int formals,boolean hasFormals,boolean varFormals) {
	int size = 0;
	int variable = 0;
	if (ntree.hasExt()) {
	    size = ntree.extSize();
	    for (int i = 0; i < size; i++) {
		XDOM e1 = ntree.getExt(i+1);
		XDOM e2 = TreeUtil.stripPercent(e1);
		XDOM tree2 = TreeUtil.stripView(e2);
		if (e1 != e2) {
		    variable ++;
		    if (variable == 2) {
			errors.error(Errors.BIND,ntree.pos,"when variable, only 1 formal ext permitted");
		    }
		}
		if (tree2 instanceof XDOMName) {
		    XDOMName ntree2 = (XDOMName) tree2;
		    symtab.define(ntree2,ftree,true,false,false,false);
		} else {
		    errors.error(Errors.BIND,tree2.pos,"illegal extended name spec");
		}
	    }
	}
	if (lhs || rhs) {
	    symtab.defineFunc2(ntree,ftree,parent,formals,hasFormals,varFormals,
			       size,variable > 0,lhs,visible,local,assert1);
	} else {
	    symtab.defineFunc(ntree,ftree,parent,formals,hasFormals,varFormals,
			      size,variable > 0,visible,local,assert1);
	}
    }

    private void bind1func(XDOMElement etree) {
	int asize = etree.attrSize();
	XScope.add(etree);
	if (asize == 0) {
	    errors.error(Errors.BIND,etree.pos,"missing function name");
	} else {
	    boolean ok = false;
	    callableOptions(etree,true);
	    XDOM tree1 = TreeUtil.stripView(etree.getAttr(1));
	    if (lhs && rhs) {
		errors.error(Errors.BIND,etree.pos,"can not be both lhs and rhs");
	    }
	    if (tree1 instanceof XDOMName) {
		XDOMName ntree = (XDOMName) tree1;
		bind1funcname(ntree,etree,0,false,false);
		ok = true;
	    } else if (tree1 instanceof XDOMCall) {
		XDOMCall ctree = (XDOMCall) tree1;
		int i1;
		int size = ctree.argSize();
		int variable = 0;
		for (i1 = 0; i1 < size; i1++) {
		    XDOM arg = ctree.getArg(i1+1);
		    if (TreeUtil.hasPercent(arg)) {
			variable ++;
			if (variable == 2) {
			    errors.error(Errors.BIND,ctree.pos,"when variable, only one formal permitted");
			}
		    }
		}
		if (ctree.getFunc() instanceof XDOMName) {
		    XDOMName ntree = (XDOMName) ctree.getFunc();
		    bind1funcname(ntree,etree,size,true,variable > 0);
		    ok = true;
		}
		for (i1 = 0; i1 < size; i1++) {
		    XDOM tree2 = TreeUtil.stripView(ctree.getArg(i1+1));
		    if (tree2 instanceof XDOMName) {
			XDOMName ntree2 = (XDOMName) tree2;
			symtab.define(ntree2,etree,true,false,false,false);
		    } else {
			errors.error(Errors.BIND,tree2.pos,"illegal formal parameter spec");
		    }
		}
	    } 
	    if (! ok) {
		errors.error(Errors.BIND,tree1.pos,"illegal func spec");
	    }
	}
	bind1body(etree);
    }

    private void bind1viewname(XDOMName ntree,XDOMElement ftree,boolean isType) {
	int size = 0;
	int variable = 0;
	if (ntree.hasExt()) {
	    size = ntree.extSize();
	    for (int i = 0; i < size; i++) {
		XDOM e1 = ntree.getExt(i+1);
		XDOM e2 = TreeUtil.stripPercent(e1);
		XDOM tree2 = TreeUtil.stripView(e2);
		if (e1 != e2) {
		    variable ++;
		    if (variable == 2) {
			errors.error(Errors.BIND,ntree.pos,"when variable, only 1 formal ext permitted");
		    }
		}
		if (tree2 instanceof XDOMName) {
		    XDOMName ntree2 = (XDOMName) tree2;
		    symtab.define(ntree2,ftree,true,false,false,false);
		} else {
		    errors.error(Errors.BIND,tree2.pos,"illegal extended name spec");
		}
	    }
	}
	if (isType) {
	    if (ntree.hasExt()) {
		// define base name (no ext)
		XDOMName ntree1 = new XDOMName(ntree.getName());
		ntree1.pos = ntree.pos;
		symtab.defineView(ntree1,ftree,parent,0,false,visible,local,assert1);
	    }
	    symtab.defineType(ntree,ftree,parent,size,variable > 0,visible,local,assert1);
	} else {
	    symtab.defineView(ntree,ftree,parent,size,variable > 0,visible,local,assert1);
	}
    }

    private void bind1type(XDOMElement etree) {
	int asize = etree.attrSize();
	XScope.add(etree);
	if (asize == 0) {
	    errors.error(Errors.BIND,etree.pos,"missing type name");
	} else {
	    boolean ok = false;
	    callableOptions(etree,false);
	    XDOM tree1 = etree.getAttr(1);
	    if (tree1 instanceof XDOMName) {
		XDOMName ntree = (XDOMName) tree1;
		bind1viewname(ntree,etree,true);
		ok = true;
	    } 
	    if (! ok) {
		errors.error(Errors.BIND,tree1.pos,"illegal type spec");
	    }
	}
	bind1body(etree);
    }

    private void bind1view(XDOMElement etree) {
	int asize = etree.attrSize();
	XScope.add(etree);
	boolean visible = TreeUtil.hasProp(etree,"visible");
	boolean local = TreeUtil.hasProp(etree,"local");
	if (asize == 0) {
	    errors.error(Errors.BIND,etree.pos,"missing view name");
	} else {
	    boolean ok = false;
	    callableOptions(etree,false);
	    XDOM tree1 = etree.getAttr(1);
	    if (tree1 instanceof XDOMName) {
		XDOMName ntree = (XDOMName) tree1;
		bind1viewname(ntree,etree,false);
		ok = true;
	    } 
	    if (! ok) {
		errors.error(Errors.BIND,tree1.pos,"illegal view spec");
	    }
	}
	bind1body(etree);
    }

    private void bind1for1(XDOMElement etree) {
	int asize = etree.attrSize();
	if (asize == 0) {
	    errors.error(Errors.BIND,etree.pos,"missing for spec");
	} else {
	    XDOM tree1 = etree.getAttr(1);
	    boolean ok = false;
	    if (tree1 instanceof XDOMCall) {
		XDOMCall ctree = (XDOMCall) tree1;
		if (ctree.getFunc() instanceof XDOMName && ctree.argSize() == 2) {
		    XDOMName ntree = (XDOMName) ctree.getFunc();
		    bind1exp(ctree.getArg(2));
		    if (ntree.getName() == "Assign") {
			XDOM ids = ctree.getArg(1);
			if (ids instanceof XDOMName) {
			    XDOMName ntree1 = (XDOMName) ctree.getArg(1);
			    symtab.define(ntree1,etree,true,false,false,false);
			    ok = true;
			} else if (ids instanceof XDOMCall) {
			    XDOMCall idctree = (XDOMCall) ids;
			    if (idctree.getFunc() instanceof XDOMName) {
				XDOMName idntree = (XDOMName) idctree.getFunc();
				if (idntree.getName() == "list") {
				    ok = true;
				    for (int i = 1; i <= idctree.argSize(); i++) {
					if (idctree.getArg(i) instanceof XDOMName) {
					    XDOMName n = (XDOMName) idctree.getArg(i);
					    symtab.define(n,etree,true,false,false,false);
					} else {
					    ok = false;
					}
				    }
				}
			    }
			}
		    }
		}
	    }
	    if (! ok) {
		errors.error(Errors.BIND,tree1.pos,"illegal for spec");
	    }
	}
	bind1body(etree);
    }

    private void bind1for(XDOMElement etree) {
	int asize = etree.attrSize();
	boolean ok = true;
	if (asize == 0) {
	    errors.error(Errors.BIND,etree.pos,"missing for spec");
	    ok = false;
	} else {
	    boolean ok1 = false;
	    XDOM last = etree.getAttr(asize);
	    for (int i = 1; i <= asize-1; i++) {
		XDOM attr = etree.getAttr(i);
		if (attr instanceof XDOMName) {
		    XDOMName ntree = (XDOMName) attr;
		    if (ntree.hasExt()) {
			errors.error(Errors.BIND,attr.pos,"not an identifier");
			ok = false;
		    }
		} else {
		    errors.error(Errors.BIND,attr.pos,"not an identifier");
		    ok = false;
		}
	    }
	    if (last instanceof XDOMCall) {
		XDOMCall ctree = (XDOMCall) last;
		if (ctree.getFunc() instanceof XDOMName && ctree.argSize() == 2) {
		    XDOMName ntree = (XDOMName) ctree.getFunc();
		    if (ntree.getName() == "Assign" && ctree.getArg(1) instanceof XDOMName) {
			ok1 = true;
		    }
		}
	    }
	    if (! ok1) {
		errors.error(Errors.BIND,last.pos,"illegal for spec");
		ok = false;
	    }
	}
	if (ok) {
	    XDOMCall spec = (XDOMCall) etree.getAttr(asize);
	    XDOMName name1 = new XDOMName("sys:Body");
	    XDOMName name2 = new XDOMName("sys:Body");
	    XDOMElement etree1 = new XDOMElement("x:func",1,etree.bodySize());
	    name1.pos = etree.pos.copy();
	    name2.pos = etree.pos.copy();
	    name2.setNoStep(true);
	    etree1.setSpaceBefore(XDOM.LINE);
	    etree1.setSpaceAfter(XDOM.LINE);
	    etree1.setSpaceBeforeBody(etree.getSpaceBeforeBody());
	    etree1.setSpaceAfterBody(etree.getSpaceAfterBody());
	    int bsize = etree.bodySize();
	    if (bsize <= 1) {
		etree1.pos = etree.pos.copy();
		etree1.setNoStep(true);
	    } else {
		XDOM first = etree.getBody(1);
		XDOM last = etree.getBody(bsize);
		etree1.pos = new XPos(first.pos.firstLine,first.pos.firstChar,
				      last.pos.lastLine,last.pos.lastChar,first.pos.fpos);
	    }
	    for (int i = 1; i <= bsize; i++) {
		etree1.insertBody(-1,etree.getBody(i));
	    }
	    XDOMCall call1 = new XDOMCall(name1,asize);
	    call1.pos = etree.pos.copy();
	    call1.setNoStep(true);
	    for (int i = 1; i <= asize-1; i++) {
		call1.insertArg(-1,etree.getAttr(i));
	    }
	    call1.insertArg(-1,spec.getArg(1));
	    etree1.insertAttr(-1,call1);
	    XDOMCall call2 = new XDOMCall(spec.getArg(2),1);
	    call2.setNoStep(true);
	    call2.pos = etree.pos.copy();
	    call2.insertArg(-1,name2);
	    etree.setTag("x:block");
	    etree.setKind(XDOMElement.EITERATOR);
	    etree.clearAttr();
	    etree.clearBody(2);
	    etree.insertBody(-1,etree1);
	    etree.insertBody(-1,call2);
	    etree.setSpaceBeforeBody(XDOM.LINE);
	    etree.setSpaceAfterBody(XDOM.LINE);
	} else {
	    etree.setTag("x:comment");
	}
	bind1(etree);
    }

    private void bind1thread(XDOMElement etree) {
	int asize = etree.attrSize();
	XScope.add(etree);
	if (parent.getTag() != "x:fork") {
	    errors.error(Errors.BIND,etree.pos,"not directly inside a x:fork element");
	} else {
	    if (asize > 0) {
		XDOM attr1 = etree.getAttr(1);
		if (attr1 instanceof XDOMName) {
		    XDOMName xn = (XDOMName) attr1;
		    if (! xn.hasExt()) {
			symtab.defineThread(xn,etree,parent);
			return;
		    }
		}
	    }
	    errors.error(Errors.BIND,etree.pos,"bad thread spec");
	}
    }

    private void bind1var(XDOMElement etree,boolean isConst) {
	int asize = etree.attrSize();
	if (asize == 0) {
	    if (isConst) {
		errors.error(Errors.BIND,etree.pos,"missing const spec");
	    } else {
		errors.error(Errors.BIND,etree.pos,"missing var spec");
	    }
	} else {
	    boolean ok = false;
	    XDOM tree1 = etree.getAttr(1);
	    XDOM tree1x = TreeUtil.stripView(tree1);
	    int bsize = etree.bodySize();
	    boolean visible = false;
	    boolean local = false;
	    boolean hasInit = false;
	    for (int i = 2; i <= asize; i++) {
		XDOM elem = etree.getAttr(i);
		if (elem instanceof XDOMName) {
		    XDOMName ntree = (XDOMName) elem;
		    String name = ntree.getName();
		    if (name == "visible") {
			if (visible) {
			    errors.error(Errors.BIND,elem.pos,"visible specified more than once");
			} else {
			    visible = true;
			}
		    } else if (name == "local") {
			if (local) {
			    errors.error(Errors.BIND,elem.pos,"local specified more than once");
			} else {
			    local = true;
			}
		    } else {
			errors.error(Errors.BIND,elem.pos,"unrecognized attribute");
		    }
		}
	    }

	    if (tree1x instanceof XDOMName) {
		XDOMName ntree = (XDOMName) tree1x;
		XDOM view = TreeUtil.getView(tree1);
		symtab.define(ntree,parent,false,view != null,visible,local);
		ok = true;
	    } else if (tree1 instanceof XDOMCall) {
		XDOMCall ctree = (XDOMCall) tree1;
		if (ctree.getFunc() instanceof XDOMName && ctree.argSize() == 2) {
		    XDOMName ntree = (XDOMName) ctree.getFunc();
		    XDOM tree2 = TreeUtil.stripView(ctree.getArg(1));
		    XDOM view = TreeUtil.getView(ctree.getArg(1));
		    if (! ntree.hasExt() &&
			ntree.getName() == "Assign" &&
			tree2 instanceof XDOMName) {
			hasInit = true;
			XDOMName ntree1 = (XDOMName) tree2;
			symtab.define(ntree1,parent,false,view != null,visible,local);
			bind1exp(ctree.getArg(2));
			ok = true;
		    }
		}
	    }
	    if (bsize != 0) {
		errors.error(Errors.BIND,etree.pos,"body not permitted");
	    }
/*		if (bsize != 0) {
		boolean bodyOk = false;
		if (bsize == 1) {
		    XDOM x = etree.getBody(1);
		    if (x instanceof XDOMElement) {
			bodyOk = true;
		    }
		}
		if (hasInit) {
		    errors.error(Errors.BIND,etree.pos,"can't have both initial value and body");
		}
		if (! bodyOk) {
		    errors.error(Errors.BIND,etree.pos,"body must constain exactly one element");
		}
	    }
*/	    
	    if (! ok) {
		if (isConst) {
		    errors.error(Errors.BIND,tree1.pos,"illegal const spec");
		} else {
		    errors.error(Errors.BIND,tree1.pos,"illegal var spec");
		}
//	    } else {
//		if (isConst && ! hasInit && bsize == 0) {
//		    errors.error(Errors.BIND,etree.pos,"missing const initial value");
//		}
	    }
	}
    }

    private void checkName(XDOM arg) {
	if (arg instanceof XDOMName) {
	    XDOMName xn = (XDOMName) arg;
	    if (xn.extSize() == 0) return;
	} else if (arg instanceof XDOMCall) {
	    XDOMCall xc = (XDOMCall) arg;
	    XDOM f = xc.getFunc();
	    if (f instanceof XDOMName && xc.argSize() == 2) {
		XDOMName xn = (XDOMName) f;
		XDOM arg1 = xc.getArg(1);
		XDOM arg2 = xc.getArg(2);
		if (xn.getName() == "Greater" && arg1 instanceof XDOMName && arg2 instanceof XDOMName) {
		    XDOMName xn1 = (XDOMName) arg1;
		    XDOMName xn2 = (XDOMName) arg2;
		    if (xn1.hasExt() == xn2.hasExt() && xn1.extSize() == 0 && xn2.extSize() == 0) return;
		}
	    }
	}
	errors.error(Errors.BIND,arg.pos,"illegal name form");
    }

    private XDOMName getOldName(XDOM arg) {
	if (arg instanceof XDOMName) {
	    XDOMName xn = (XDOMName) arg;
	    return xn;
	} else if (arg instanceof XDOMCall) {
	    XDOMCall xc = (XDOMCall) arg;
	    XDOM f = xc.getFunc();
	    if (f instanceof XDOMName && xc.argSize() == 2) {
		XDOMName xn = (XDOMName) f;
		XDOM arg1 = xc.getArg(1);
		XDOM arg2 = xc.getArg(2);
		if (xn.getName() == "Greater" && arg1 instanceof XDOMName && arg2 instanceof XDOMName) {
		    XDOMName xn1 = (XDOMName) arg1;
		    XDOMName xn2 = (XDOMName) arg2;
		    if (xn1.extSize() == 0 && xn2.extSize() == 0) {
			return xn1;
		    }
		}
	    }
	}
	return null;
    }

    private XDOMName getNewName(XDOM arg) {
	if (arg instanceof XDOMName) {
	    XDOMName xn = (XDOMName) arg;
	    return xn;
	} else if (arg instanceof XDOMCall) {
	    XDOMCall xc = (XDOMCall) arg;
	    XDOM f = xc.getFunc();
	    if (f instanceof XDOMName && xc.argSize() == 2) {
		XDOMName xn = (XDOMName) f;
		XDOM arg1 = xc.getArg(1);
		XDOM arg2 = xc.getArg(2);
		if (xn.getName() == "Greater" && arg1 instanceof XDOMName && arg2 instanceof XDOMName) {
		    XDOMName xn1 = (XDOMName) arg1;
		    XDOMName xn2 = (XDOMName) arg2;
		    if (xn1.extSize() == 0 && xn2.extSize() == 0) {
			return xn2;
		    }
		}
	    }
	}
	return null;
    }

    private void checkList(XDOMCall list) {
	int size = list.argSize();
	for (int i = 1; i <= size; i++) {
	    XDOM arg = list.getArg(i);
	    checkName(arg);
	    XDOMName old = getOldName(arg);
	    if (old != null) {
		for (int j = 1; j < i; j++) {
		    XDOM arg1 = list.getArg(j);
		    XDOMName old1 = getOldName(arg1);
		    if (old1 != null) {
			if (old.getName() == old1.getName() && old.hasExt() == old1.hasExt()) {
			    errors.error(Errors.BIND,arg.pos,"duplicate name");
			}
		    }
		}
	    }
	}
    }
    private String inList(XDOMCall list,String name,boolean hasExt) {
	int size = list.argSize();
	for (int i = 1; i <= size; i++) {
	    XDOM arg = list.getArg(i);
	    XDOMName xn = getOldName(arg);
	    if (xn != null) {
		if (xn.getName() == name && xn.hasExt() == hasExt) {
		    XDOMName xnnew = getNewName(arg);
		    if (xnnew != null) return xnnew.getName();
		    return null;
		}

	    }
	}
	return null;
    }

    private void checkUsed(XDOMCall list,XDef defs) {
	int size = list.argSize();
	for (int i = 1; i <= size; i++) {
	    XDOM arg = list.getArg(i);
	    XDOMName xn = getOldName(arg);
	    if (xn != null) {
		boolean found = false;
		XDef def = defs;
		while (def != null) {
		    if (def.visible) {
			if (def.name == xn.getName() && def.hasExt == xn.hasExt()) {
			    found = true;
			    break;
			}
		    }
		    def = def.next;
		}
		if (! found) {
		    errors.error(Errors.BIND,arg.pos,"name not found");
		}
	    }
	}
    }

    private void bind1lang(XDOMElement etree) {
	int size = etree.attrSize();
	String script = null;
	String lang = null;
	if (size != 2) {
	    errors.error(Errors.BIND,etree.pos,"does not have two attributes");
	    return;
	}
	XDOM arg1 = etree.getAttr(1);
	if (arg1 instanceof XDOMName) {
	    XDOMName name1 = (XDOMName) arg1;
	    if (! name1.hasExt()) {
		script = name1.getName();
	    } else {
		errors.error(Errors.BIND,arg1.pos,"not an id");
		return;
	    }
	} else {
	    errors.error(Errors.BIND,arg1.pos,"not a name");
	    return;
	}
	if (script == null) return;

	XDOM arg2 = etree.getAttr(2);
	XDOMName name2 = null;
	if (arg2 instanceof XDOMName) {
	    name2 = (XDOMName) arg2;
	    if (! name2.hasExt()) {
		lang = name2.getName();
	    } else {
		errors.error(Errors.BIND,arg2.pos,"not an id");
		return;
	    }
	} else {
	    errors.error(Errors.BIND,arg1.pos,"not a name");
	    return;
	}
	if (lang == null) return;

	Load ld = xt.cmd.loads.getUse(xt,script,etree.pos,myPath);
	if (ld == null) {
	    return;
	}
	xt.cmd.loads.execUse(xt,ld,xt.cmd.value.ctxEval);
	XScope scope = ld.tree.scope;
	XDef def = scope.defs;
	XDef found = null;
	while (def != null) {
	    if (def.visible && ! def.hasExt) {
		if (def.name == lang) {
		    found = def;
		    break;
		}
	    }
	    def = def.next;
	}
	if (found == null) {
	    errors.error(Errors.BIND,arg2.pos,"can't find name");
	    return;
	}
	name2.def = found.getReal();
	xt.exec.execExp(name2,xt.cmd.value.ctxEval);
	Object oval1 = xt.exec.stack.getTopOval();
	long ival1 = xt.exec.stack.getTopIval();
	xt.exec.stack.pop();
	if (oval1 instanceof VLLxact || oval1 instanceof VLLrender || oval1 instanceof VLLerror ||
	      oval1 instanceof VLObj) {
	    xt.currentLang = oval1;
	} else {
	    errors.error(Errors.BIND,arg2.pos,"not a language object");
	}
    }

    private void bind1use(XDOMElement etree) {
	int size = etree.attrSize();
	boolean visible = false;
	boolean hidden = false;
	boolean dynamic = false;
	String script = null;
	String language = null;
	XDOMCall visibleList = null;
	XDOMCall hiddenList = null;
	for (int i = 1; i <= size; i++) {
	    XDOM arg = etree.getAttr(i);
	    boolean ok = false;
	    if (arg instanceof XDOMName) {
		XDOMName xn = (XDOMName) arg;
		String n = xn.getName();
		if (! xn.hasExt()) {
		    if (i == 1) {
			script = n;
			ok = true;
		    } else if (n == "visible") {
			if (visible) {
			    errors.error(Errors.BIND,arg.pos,"visible specified more than once");
			} else {
			    visible = true;
			}
			ok = true;
		    } else if (n == "hidden") {
			if (hidden) {
			    errors.error(Errors.BIND,arg.pos,"hidden specified more than once");
			} else {
			    hidden = true;
			}
			ok = true;
		    } else if (n == "dynamic") {
			if (dynamic) {
			    errors.error(Errors.BIND,arg.pos,"dynamic specified more than once");
			}
			dynamic = true;
			ok = true;
		    } 
		}
	    } else if (TreeUtil.isEqual(arg)) {
		String n = TreeUtil.getEqualName(arg);
		XDOM x = TreeUtil.stripEqual(arg);
		String val = null;
		if (x instanceof XDOMName) {
		    XDOMName xn = (XDOMName) x;
		    val = xn.getName();
		} else if (x instanceof XDOMString) {
		    XDOMString xs = (XDOMString) x;
		    val = xs.getVal();
		}
		if (val != null) {
		    if (n == "script") {
			if (script != null) {
			    errors.error(Errors.BIND,arg.pos,"script specified more than once");
			} else {
			    script = val;
			}
			ok = true;
		    } else if (n == "language") {
			if (language != null) {
			    errors.error(Errors.BIND,arg.pos,"language specified more than once");
			} else {
			    language = val;
			}
			ok = true;
		    }
		}
	    } else if (arg instanceof XDOMCall) {
		XDOMCall xc = (XDOMCall) arg;
		XDOM f = xc.getFunc();
		if (f instanceof XDOMName) {
		    XDOMName xn = (XDOMName) f;
		    String n = xn.getName();
		    if (n == "visible") {
			if (visibleList != null) {
			    errors.error(Errors.BIND,arg.pos,"visible specified more than once");
			} else {
			    visibleList = xc;
			    checkList(visibleList);
			}
			ok = true;
		    } else if (n == "hidden") {
			if (hiddenList != null) {
			    errors.error(Errors.BIND,arg.pos,"hidden specified more than once");
			} else {
			    hiddenList = xc;
			    checkList(hiddenList);
			}
			ok = true;
		    }
		}
	    }
	    if (! ok) {
		errors.error(Errors.BIND,arg.pos,"illegal x:use attribute");
	    }
	}
	if (script == null) {
	    errors.error(Errors.BIND,etree.pos,"missing script");
	    return;
	}
	if (dynamic) {
	    if (visible || hidden || (visibleList == null && hiddenList == null)) {
		errors.error(Errors.BIND,etree.pos,
			     "dynamic use must have explicit list with no defaults");
		return;
	    }
	}
	if (hidden && visible) {
	    errors.error(Errors.BIND,etree.pos,
			 "use default can not be both hidden and visible");
	    return;
	}
	Load ld = xt.cmd.loads.getUse(xt,script,etree.pos,myPath);
	if (ld != null) {
	    xt.cmd.loads.execUse(xt,ld,xt.cmd.value.ctxEval);
	    XScope scope = ld.tree.scope;
	    XDef def = scope.defs;
	    while (def != null) {
		if (def.visible) {
		    if (visibleList == null && hiddenList == null) {
			symtab.defineVirtual(def.name,def,parent,etree.pos,visible);
		    } else {
			String v = null;
			String h = null;
			if (visibleList != null) {
			    v = inList(visibleList,def.name,def.hasExt);
			}
			if (hiddenList != null) {
			    h = inList(hiddenList,def.name,def.hasExt);
			}
			if (h != null && v != null) {
			    String s = def.name;
			    if (def.hasExt) s = s + "[]";
			    errors.error(Errors.BIND,etree.pos,"name "+s+" is both visible and hidden");
			    symtab.defineVirtual(v,def,parent,etree.pos,true);
			} else if (h != null) {
			    symtab.defineVirtual(h,def,parent,etree.pos,false);
			} else if (v != null) {
			    symtab.defineVirtual(v,def,parent,etree.pos,true);
			} else if (visible || hidden) {
			    symtab.defineVirtual(def.name,def,parent,etree.pos,visible);
			}
		    }
		}
		def = def.next;
	    }
	    if (visibleList != null) {
		checkUsed(visibleList,scope.defs);
	    }
	    if (hiddenList != null) {
		checkUsed(hiddenList,scope.defs);
	    }
	}
    }

    private void bind1module(XDOMElement etree) {
	XScope.add(etree);
	if (parent.scope == null) XScope.add(parent);
	XScope scope = etree.scope;
	scope.parent = parent.scope;
	bind1body(etree);

	int size = etree.attrSize();
	boolean visible = false;
	boolean hidden = false;
	XDOMCall visibleList = null;
	XDOMCall hiddenList = null;
	for (int i = 1; i <= size; i++) {
	    XDOM arg = etree.getAttr(i);
	    boolean ok = false;
	    if (arg instanceof XDOMName) {
		XDOMName xn = (XDOMName) arg;
		String n = xn.getName();
		if (! xn.hasExt()) {
		    if (n == "visible") {
			if (visible) {
			    errors.error(Errors.BIND,arg.pos,"visible specified more than once");
			} else {
			    visible = true;
			}
			ok = true;
		    } else if (n == "hidden") {
			if (hidden) {
			    errors.error(Errors.BIND,arg.pos,"hidden specified more than once");
			} else {
			    hidden = true;
			}
			ok = true;
		    } 
		}
	    } else if (arg instanceof XDOMCall) {
		XDOMCall xc = (XDOMCall) arg;
		XDOM f = xc.getFunc();
		if (f instanceof XDOMName) {
		    XDOMName xn = (XDOMName) f;
		    String n = xn.getName();
		    if (n == "visible") {
			if (visibleList != null) {
			    errors.error(Errors.BIND,arg.pos,"visible specified more than once");
			} else {
			    visibleList = xc;
			    checkList(visibleList);
			}
			ok = true;
		    } else if (n == "hidden") {
			if (hiddenList != null) {
			    errors.error(Errors.BIND,arg.pos,"hidden specified more than once");
			} else {
			    hiddenList = xc;
			    checkList(hiddenList);
			}
			ok = true;
		    }
		}
	    }
	    if (! ok) {
		errors.error(Errors.BIND,arg.pos,"illegal x:module attribute");
	    }
	}
	if (hidden && visible) {
	    errors.error(Errors.BIND,etree.pos,
			 "module default can not be both hidden and visible");
	    return;
	}
	XDef def = scope.defs;
	while (def != null) {
	   // if (def.visible) symtab.defineVirtual(def.name,def,parent,etree.pos,false);
	    if (def.visible) {
		if (visibleList == null && hiddenList == null) {
		    symtab.defineVirtual(def.name,def,parent,etree.pos,visible);
		} else {
		    String v = null;
		    String h = null;
		    if (visibleList != null) {
			v = inList(visibleList,def.name,def.hasExt);
		    }
		    if (hiddenList != null) {
			h = inList(hiddenList,def.name,def.hasExt);
		    }
		    if (h != null && v != null) {
			String s = def.name;
			if (def.hasExt) s = s + "[]";
			errors.error(Errors.BIND,etree.pos,"name "+s+" is both visible and hidden");
			symtab.defineVirtual(v,def,parent,etree.pos,true);
		    } else if (h != null) {
			symtab.defineVirtual(h,def,parent,etree.pos,false);
		    } else if (v != null) {
			symtab.defineVirtual(v,def,parent,etree.pos,true);
		    } else if (visible || hidden) {
			symtab.defineVirtual(def.name,def,parent,etree.pos,visible);
		    }
		}
	    }
	    def = def.next;
	}
	if (visibleList != null) {
	    checkUsed(visibleList,scope.defs);
	}
	if (hiddenList != null) {
	    checkUsed(hiddenList,scope.defs);
	}
    }

    public void bind1exp(XDOM tree) {
	byte kind = tree.getXKind();
	switch (kind) {
	    case XDOM.XNAME:
	    {
		XDOMName ntree = (XDOMName) tree;
		if (ntree.hasExt()) {
		    int size = ntree.extSize();
		    for (int i = 0; i < size; i++) {
			bind1exp(ntree.getExt(i+1));
		    }
		}
		break;
	    }
	    case XDOM.XCALL:
	    {
		XDOMCall ctree = (XDOMCall) tree;
		int size = ctree.argSize();
		bind1exp(ctree.getFunc());
		for (int i = 1; i <= size; i++) {
		    bind1exp(ctree.getArg(i));
		}
		break;
	    }
	    case XDOM.XELEMENT:
	    {
		errors.error(Errors.INTERNAL,tree.pos,"bind1exp: not an exp"+tree);
		break;
	    }
	}
    }

    public void bind1body(XDOMElement etree) {
	int bsize = etree.bodySize();
	if (bsize != 0) {
	    Object oldLang = xt.currentLang;
	    XDOMElement oldParent = parent;
	    XDOM oldPrev = prev;
	    parent = etree;
	    prev = null;
	    for (int i = 1; i <= bsize; i++) {
		XDOM elem = etree.getBody(i);
		bind1(elem);
		prev = elem;
	    }
	    parent = oldParent;
	    prev = oldPrev;
	    xt.currentLang = oldLang;
	}
    }

    private void noAttr(XDOMElement etree) {
	int asize = etree.attrSize();
	if (asize != 0) {
	    errors.error(Errors.BIND,etree.pos,"attributes not permitted");
	}
    }

    private void singleAttr(XDOMElement etree) {
	int asize = etree.attrSize();
	if (asize == 0) {
	    errors.error(Errors.BIND,etree.pos,"missing attribute");
	} else {
	    bind1exp(etree.getAttr(1));
	    if (asize > 1) {
		errors.error(Errors.BIND,etree.pos,"only a single attribute is permitted");
	    }
	}
    }

    private void noBody(XDOMElement etree) {
	int bsize = etree.bodySize();

	if (bsize != 0) {
	    errors.error(Errors.BIND,etree.pos,"body not permitted");
	}
    }
    
    private void bind1xact(String space,String base,XDOMElement etree,VLLxact lang) {
	if (space == "x") {
	    if (base == "func") {
		bind1func(etree);
	    } else if (base == "oldfor") {
		bind1for(etree);
	    } else if (base == "for") {
		bind1for1(etree);
	    } else if (base == "var") {
		bind1var(etree,false);
	    } else if (base == "const") {
		bind1var(etree,true);
	    } else if (base == "else") {
		noAttr(etree);
		boolean ok = false;
		if (prev instanceof XDOMElement) {
		    XDOMElement tree1 = (XDOMElement) prev;
		    String base1 = tree1.getBase();
		    if (etree.getSpace() == tree1.getSpace()) {
			if (base1 == "if") { ok = true; }
			else if (base1 == "orif") { ok = true; }
			else if (base1 == "for") { ok = true; }
			else if (base1 == "for1") { ok = true; }
		    }
		}
		if (! ok) errors.error(Errors.BIND,etree.pos,"else not preceeded by if, orif or for");
		bind1body(etree);
	    } else if (base == "orif") {
		singleAttr(etree);
		boolean ok = false;
		if (prev instanceof XDOMElement) {
		    XDOMElement tree1 = (XDOMElement) prev;
		    String base1 = tree1.getBase();
		    if (etree.getSpace() == tree1.getSpace()) {
			if (base1 == "if") { ok = true; }
			else if (base1 == "orif") { ok = true; }
		    }
		}
		if (! ok) errors.error(Errors.BIND,etree.pos,"orif not preceeded by if or orif");
		bind1body(etree);
	    } else if (base == "type") {
		bind1type(etree);
	    } else if (base == "view") {
		bind1view(etree);
	    } else if (base == "self") {
		noAttr(etree);
		boolean ok = false;
		if (prev instanceof XDOMElement) {
		    XDOMElement tree1 = (XDOMElement) prev;
		    String base1 = tree1.getBase();
		    if (etree.getSpace() == tree1.getSpace()) {
			if (base1 == "type" || base1 == "view") {
			    XDOMName ntree = TreeUtil.getName(tree1);
			    if (ntree != null && ntree.def instanceof XDefView) {
				XDefView vdef = (XDefView) ntree.def;
				vdef.self = etree;
			    }
			    ok = true;
			}
		    }
		}
		if (! ok) {
		    errors.error(Errors.BIND,etree.pos,"self not preceeded by type or view");
		}
		symtab.defineSpecial("self",etree);
		bind1body(etree);
	    } else if (base == "comment") {
	    } else if (base == "use") {
		noBody(etree);
		bind1use(etree);
	    } else if (base == "thread") {
		bind1thread(etree);
		bind1body(etree);
	    } else if (base == "module") {
		bind1module(etree);
	    } else if (base == "block") {
		noAttr(etree);
		bind1body(etree);
	    } else if (base == "if") {
		singleAttr(etree);
		bind1body(etree);
	    } else if (base == "while") {
		singleAttr(etree);
		bind1body(etree);
	    } else if (base == "exp") {
		singleAttr(etree);
		noBody(etree);
	    } else if (base == "lang") {
		bind1lang(etree);
		noBody(etree);
	    } else if (base == "break") {
		noAttr(etree);
		noBody(etree);
	    } else if (base =="fork") {
		noAttr(etree);
		bind1body(etree);
	    } else if (base == "exclude") {
		singleAttr(etree);
		bind1body(etree);
	    } else if (base == "tag") {
		singleAttr(etree);
		bind1body(etree);
	    } else {
		errors.error(Errors.BIND,etree.pos,"unrecognized tag "+space+":"+base);
	    }
	    /*
	    int asize = etree.attrSize();
	    for (int i = 1; i <= asize; i++) {
		bind1exp(etree.getAttr(i));
	    }
	    bind1body(etree);
	    if (base == "module") {
		bind1moduleA(etree);
	    }
	    */
	} else {
	    bind1Lang(space,base,etree,lang.underLang);
	}
    }

    private void bind1render(String space,String base,XDOMElement etree,VLLrender lang) {
	int asize = etree.attrSize();
	for (int i = 1; i <= asize; i++) {
	    bind1exp(etree.getAttr(i));
	}
	bind1body(etree);
    }

    public void bind1Lang(String space,String base,XDOMElement etree,Object lang) {
	if (lang instanceof VLLxact) {
	    VLLxact langx = (VLLxact) lang;
	    bind1xact(space,base,etree,langx);
	} else if (lang instanceof VLLrender) {
	    VLLrender langr = (VLLrender) lang;
	    bind1render(space,base,etree,langr);
	} else if (lang instanceof VLLerror) {
	    errors.error(Errors.BIND,etree.pos,"don't know how to bind1 this element");
	} else if (lang instanceof VLObj) {
	    VLObj vlo = (VLObj) lang;
	    byte oldPass = xt.exec.interExec.pass;
	    xt.exec.interExec.pass = InterExec.PBIND1;
	    xt.exec.stack.push("bind1",0);
	    xt.exec.stack.push(space,0);
	    xt.exec.stack.push(base,0);
	    xt.exec.stack.push(etree,0);
	    xt.exec.stack.push(lang,0);
	    xt.exec.treeExec.recordSelect(info,"scope",etree.pos,xt.cmd.value.ctxEval);
	    Object oldOval = xt.exec.stack.getTopOval();
	    long oldIval = xt.exec.stack.getTopIval();
	    xt.exec.stack.pop();
	    info.setERec("scope",parent,0);
	    xt.exec.stack.push(info,0);
	    XDef walk = xt.exec.callExec.findCallObject(vlo.type,
		"Walk",false,null);
	    if (walk != null) {
		xt.exec.callExec.execCallObject(walk,vlo.type,vlo,0,true,0,6,etree,
						xt.cmd.value.ctxEval);
		xt.exec.stack.pop();
	    } else {
		errors.error(Errors.BIND,etree.pos,"no language object Walk function");
	    }
	    info.setERec("scope",oldOval,oldIval);
	    xt.exec.interExec.pass = oldPass;
	} else {
	    errors.error(Errors.BIND,etree.pos,"not a language object");
	}
/*
	    XI xi1 = xi;
	    while (xi1 != null) {
		Object i = xi1.current;
		if (i instanceof VLIxact) {
		    VLIxact ix = (VLIxact) i;
		    if (bind1xact(ix,space,base,etree,xi)) return;
		} else if (i instanceof VLIrender) {
		    VLIrender ir = (VLIrender) i;
		    if (bind1render(ir,space,base,etree,xi)) return;
		} else if (i instanceof VLObj && xi.walk != null) {
		    VLObj vlo = (VLObj) i;
		    byte oldPass = xt.exec.interExec.pass;
		    xt.exec.interExec.notHandled = false;
		    xt.exec.interExec.pass = InterExec.PBIND1;
		    xt.exec.stack.push("bind1",0);
		    xt.exec.stack.push(space,0);
		    xt.exec.stack.push(base,0);
		    xt.exec.stack.push(etree,0);
		    xt.exec.stack.push(xi,0);
		    xt.exec.callExec.execCallObject(xi.walk,vlo.type,vlo,0,true,0,5,etree,
			xt.cmd.value.ctxEval);
		    xt.exec.stack.pop();
		    xt.exec.interExec.pass = oldPass;
		    if (! xt.exec.interExec.notHandled) return;
		}
		xi1 = xi1.parent;
	    }
	    errors.error(Errors.BIND,tree.pos,"don't know how to bind1 this element");
*/	    
    }

    private void bind1(XDOM tree) {
	if (tree instanceof XDOMElement) {
	    XDOMElement etree = (XDOMElement) tree;
	    String space = etree.getSpace();
	    String base = etree.getBase();
	    bind1Lang(space,base,etree,xt.currentLang);
	} else {
	    bind1exp(tree);
	}
    }

    private void bind2prop(XDOM tree) {
	if (tree instanceof XDOMName) {
	} else {
	    bind2exp(tree);
	}
    }

    private void bind2Call(XDOMCall ctree) {
	int size = ctree.argSize();
	boolean variable = false;
	if (size == 2 && ctree.getFunc() instanceof XDOMName && ctree.getKind() == XDOMCall.COP) {
	    XDOMName ntree = (XDOMName) ctree.getFunc();
	    String name = ntree.getName();
	    XDOM arg1 = ctree.getArg(1);
	    XDOM arg2 = ctree.getArg(2);
	    if (name == "Equal") {
		if (arg1 instanceof XDOMName) {
		} else {
		    errors.error(Errors.BIND,ctree.pos,"left operand of = not a name");
		}
		bind2exp(ctree.getFunc());
		bind2exp(arg2);
		return;
//	    } else if (name == "Eq" || name == "Ne" || name == "Less" || name == "LessEq" ||
//		       name == "Greater" || name == "GreaterEq") {
//		if (arg1 instanceof XDOMNumber || arg2 instanceof XDOMNumber) {
//		    errors.warn(Errors.BIND,ntree.pos,"use numeric compare");
//		}
	    }
	}
	bind2exp(ctree.getFunc());

	for (int i = 1; i <= size; i++) {
	    XDOM arg = ctree.getArg(i);
	    XDOM arg1 = TreeUtil.stripPercent(arg);
	    if (arg != arg1) variable = true;
	    bind2exp(arg);
	}
	if (ctree.getFunc() instanceof XDOMName) {
	    XDOMName ntree = (XDOMName) ctree.getFunc();
	    if (ntree.def != null && ntree.def instanceof XDefFunc) {
		XDefFunc fdef = (XDefFunc) ntree.def;
		if (! fdef.varArgs && ! variable && fdef.hasArgs) {
		    if (fdef.args != size) {
			errors.error(Errors.BIND,ctree.pos,"wrong number of actual parameters (expect:"
				     +fdef.args+",have:"+size+")");
		    }
		}
	    }
	}
    }

    private void bind2func(XDOMElement etree) {
	XDOMName ntree = TreeUtil.getName(etree);
	int asize = etree.attrSize();
	if (asize > 0) {
	    XDOM def = TreeUtil.stripView(etree.getAttr(1));
	    XDOM view = TreeUtil.getView(etree.getAttr(1));
	    if (def instanceof XDOMCall) {
		XDOMCall ctree = (XDOMCall) def;
		int size = ctree.argSize();
		for (int i = 1; i <= size; i++) {
		    XDOM view1 = TreeUtil.getView(ctree.getArg(i));
		    if (view1 != null) {
			bind2exp(view1);
		    }
		}
	    }
	    if (view != null) bind2exp(view);
	}
	if (ntree != null) {
	    if (ntree.hasExt()) {
		int esize = ntree.extSize();
		for (int i = 0; i < esize; i++) {
		    XDOM view1 = TreeUtil.getView(ntree.getExt(i+1));
		    if (view1 != null) {
			bind2exp(view1);
		    }
		}
	    }
	}
	if (ntree.def != null && ntree.def instanceof XDefFunc2) {
	    XDefFunc2 fdef = (XDefFunc2) ntree.def;
	    if (fdef.tree == null) {
		errors.error(Errors.BIND,etree.pos,"missing rhs definition of "+fdef.name);
	    } else if (fdef.lhsTree == null) {
		errors.error(Errors.BIND,etree.pos,"missing lhs definition of "+fdef.name);
	    } else if (TreeUtil.hasProp(etree,"rhs")) {
		if (fdef.varExts || fdef.lhsVarExts) {
		} else if (fdef.exts != fdef.lhsExts) {
		    errors.error(Errors.BIND,etree.pos,fdef.name+" lhs and rhs name exts have different sizes");
		}
		if (fdef.varArgs || fdef.lhsVarArgs) {
		} else if (fdef.args+1 != fdef.lhsArgs) {
		    errors.error(Errors.BIND,etree.pos,fdef.name+" lhs does not have 1 more argument than rhs");
		}
		if (fdef.visible != fdef.lhsVisible) {
		    errors.error(Errors.BIND,etree.pos,fdef.name+" lhs visible does not match rhs");
		}
		if (fdef.local != fdef.lhsLocal) {
		    errors.error(Errors.BIND,etree.pos,fdef.name+" lhs local does not match rhs");
		}
		if (fdef.assert1 != -1) {
		    bind2exp(etree.getAttr(fdef.assert1));
		}
	    } else {
		if (fdef.lhsAssert1 != -1) {
		    bind2exp(etree.getAttr(fdef.lhsAssert1));
		}
	    }
	} else if (ntree.def != null && ntree.def instanceof XDefFunc) {
	    XDefFunc fdef = (XDefFunc) ntree.def;
	    if (fdef.assert1 != -1) {
		bind2exp(etree.getAttr(fdef.assert1));
	    }
	}
    }

    private void bind2view(XDOMElement etree) {
	XDOMName ntree = TreeUtil.getName(etree);
	int asize = etree.attrSize();
	if (ntree != null) {
	    if (ntree.hasExt()) {
		int esize = ntree.extSize();
		for (int i = 0; i < esize; i++) {
		    XDOM view1 = TreeUtil.getView(ntree.getExt(i+1));
		    if (view1 != null) {
			bind2exp(view1);
		    }
		}
	    }
	}
	if (ntree.def != null && ntree.def instanceof XDefView) {
	    XDefView vdef = (XDefView) ntree.def;
	    if (vdef.assert1 != -1) {
		bind2exp(etree.getAttr(vdef.assert1));
	    }
	}
    }

    private void bind2var(XDOMElement tree) {
	int size = tree.attrSize();
	if (size > 0) {
	    XDOM spec = tree.getAttr(1);
	    XDOM specx = TreeUtil.stripView(spec);
	    if (specx instanceof XDOMName) {
		XDOM view = TreeUtil.getView(spec);
		if (view != null) bind2exp(view);
	    } else if (spec instanceof XDOMCall) {
		XDOMCall ctree = (XDOMCall) spec;
		int size1 = ctree.argSize();
		if (size1 == 2) {
		    XDOM view = TreeUtil.getView(ctree.getArg(1));
		    if (view != null) {
			bind2exp(view);
		    }
		}
		bind2exp(ctree.getArg(2));
	    }
	}
    }

    public void bind2lang(XDOMElement etree) {
	int size = etree.attrSize();
	String script = null;
	String lang = null;
	if (size != 2) return;
	XDOM arg2 = etree.getAttr(2);
	XDOMName name2 = null;
	if (arg2 instanceof XDOMName) {
	    name2 = (XDOMName) arg2;
	    if (! name2.hasExt()) {
		lang = name2.getName();
	    }
	}
	if (lang == null) return;
	xt.exec.execExp(name2,xt.cmd.value.ctxEval);
	Object oval1 = xt.exec.stack.getTopOval();
	long ival1 = xt.exec.stack.getTopIval();
	xt.exec.stack.pop();
	if (oval1 instanceof VLLxact || oval1 instanceof VLLrender || oval1 instanceof VLLerror ||
	      oval1 instanceof VLObj) {
	    xt.currentLang = oval1;
	} else {
	    errors.error(Errors.BIND,arg2.pos,"not a language object");
	}
    }
    
    public void bind2exp(XDOM tree) {
	byte kind = tree.getXKind();
	switch (kind) {
	    case XDOM.XNAME:
	    {
		XDOMName ntree = (XDOMName) tree;
		symtab.lookup(ntree);
		if (ntree.hasExt()) {
		    int size = ntree.extSize();
		    for (int i = 1; i <= size; i++) {
			bind2exp(ntree.getExt(i));
		    }
		}
		break;
	    }
	    case XDOM.XCALL:
	    {
		XDOMCall ctree = (XDOMCall) tree;
		bind2Call(ctree);
		break;
	    }
	    case XDOM.XELEMENT:
	    {
		errors.error(Errors.INTERNAL,tree.pos,"bind2exp: not an exp"+tree);
		break;
	    }
	}
    }

    public void bind2body(XDOMElement etree) {
	int bsize = etree.bodySize();
	if (bsize != 0) {
	    Object oldLang = xt.currentLang;
	    XDOMElement oldParent = parent;
	    XDOM oldPrev = prev;
	    parent = etree;
	    prev = null;
	    for (int i = 1; i <= bsize; i++) {
		XDOM elem = etree.getBody(i);
		bind2(elem);
		prev = elem;
	    }
	    parent = oldParent;
	    prev = oldPrev;
	    xt.currentLang = oldLang;
	}
    }

    private void bind2xact(String space,String base,XDOMElement etree,VLLxact lang) {
	if (space == "x") {
	    if (base == "comment") {
	    } else if (base == "var") {
		bind2var(etree);
	    } else if (base == "const") {
		bind2var(etree);
	    } else if (base == "use") {
	    } else if (base == "break") {
	    } else if (base == "lang") {
		bind2lang(etree);
	    } else if (base == "exp") {
		int asize = etree.attrSize();
		if (asize > 0) {
		    bind2exp(etree.getAttr(1));
		}
	    } else {
		if (base == "self") {
		    if (etree.scope != null) {
			if (prev instanceof XDOMElement) {
			    XDOMElement tree1 = (XDOMElement) prev;
			    String base1 = tree1.getBase();
			    if (etree.getSpace() == tree1.getSpace()) {
				if (base1 == "type" || base1 == "view") {
				    etree.scope.parent = tree1.scope;
				    etree.scope.level = tree1.scope.level + 1;
				}
			    }
			}
		    }
		} else if (base == "module") {
		    if (etree.scope != null) {
			etree.scope.level = etree.scope.parent.level;
			etree.scope.top = etree.scope.parent.top;
		    }
		} else if (base == "func") {
		    bind2func(etree);
		} else if (base == "type" || base == "view") {
		    if (base == "type") {
			XDOMName ntree = TreeUtil.getName(etree);
			if (ntree != null && ntree.def instanceof XDefView) {
			    XDefView vdef = (XDefView) ntree.def;
			    if (vdef.self == null) {
				errors.error(Errors.BIND,etree.pos,"type not followed by self");
			    }
			}
		    }
		    bind2view(etree);
		} else {
		    int asize = etree.attrSize();
		    if (asize > 0) {
			bind2exp(etree.getAttr(1));
		    }
		}
		bind2body(etree);
	    }
	} else {
	    bind2Lang(space,base,etree,lang.underLang);
	}
    }

    private void bind2render(String space,String base,XDOMElement etree,VLLrender lang) {
	int asize = etree.attrSize();
	for (int i = 1; i <= asize; i++) {
	    bind2exp(etree.getAttr(i));
	}
	bind2body(etree);
    }

    public void bind2Lang(String space,String base,XDOMElement etree,Object lang) {
	if (lang instanceof VLLxact) {
	    VLLxact langx = (VLLxact) lang;
	    bind2xact(space,base,etree,langx);
	} else if (lang instanceof VLLrender) {
	    VLLrender langr = (VLLrender) lang;
	    bind2render(space,base,etree,langr);
	} else if (lang instanceof VLLerror) {
	    errors.error(Errors.BIND,etree.pos,"don't know how to bind2 this element");
	} else if (lang instanceof VLObj) {
	    VLObj vlo = (VLObj) lang;
	    byte oldPass = xt.exec.interExec.pass;
	    xt.exec.interExec.pass = InterExec.PBIND2;
	    xt.exec.stack.push("bind2",0);
	    xt.exec.stack.push(space,0);
	    xt.exec.stack.push(base,0);
	    xt.exec.stack.push(etree,0);
	    xt.exec.stack.push(lang,0);
	    xt.exec.stack.push(info,0);
	    XDef walk = xt.exec.callExec.findCallObject(vlo.type,
		"Walk",false,null);
	    if (walk != null) {
		xt.exec.callExec.execCallObject(walk,vlo.type,vlo,0,true,0,6,etree,
						xt.cmd.value.ctxEval);
		xt.exec.stack.pop();
	    } else {
		errors.error(Errors.BIND,etree.pos,"no language object Walk function");
	    }
	    xt.exec.interExec.pass = oldPass;
	} else {
	    errors.error(Errors.BIND,etree.pos,"not a language object");
	}
/*	    
	    boolean ok = false;
	    while (xi1 != null) {
		Object i = xi1.current;
		if (i instanceof VLIxact) {
		    VLIxact ix = (VLIxact) i;
		    if (bind2xact(ix,space,base,etree,xi)) {
			ok = true;
			break;
		    }
		} else if (i instanceof VLIrender) {
		    VLIrender ir = (VLIrender) i;
		    if (bind2render(ir,space,base,etree,xi)) {
			ok = true;
			break;
		    }
		} else if (i instanceof VLObj && xi.walk != null) {
		    VLObj vlo = (VLObj) i;
		    byte oldPass = xt.exec.interExec.pass;
		    xt.exec.interExec.notHandled = false;
		    xt.exec.interExec.pass = InterExec.PBIND2;
		    xt.exec.stack.push("bind2",0);
		    xt.exec.stack.push(space,0);
		    xt.exec.stack.push(base,0);
		    xt.exec.stack.push(etree,0);
		    xt.exec.stack.push(xi,0);
		    xt.exec.callExec.execCallObject(xi.walk,vlo.type,vlo,0,true,0,5,etree,
			xt.cmd.value.ctxEval);
		    xt.exec.stack.pop();
		    xt.exec.interExec.pass = oldPass;
		    if (! xt.exec.interExec.notHandled) {
			ok = true;
			break;
		    }
		}
		xi1 = xi1.parent;
	    }
	    if (! ok) {
		errors.error(Errors.BIND,tree.pos,"don't know how to bind2 this element");
	    }
*/	    
    }
    
    private void bind2(XDOM tree) {
	if (tree instanceof XDOMElement) {
	    XDOMElement etree = (XDOMElement) tree;
//	    XI xi1 = xi;
	    XScope oldScope = symtab.getScope();
	    if (etree.scope != null) {
		symtab.setScope(etree.scope);
		etree.scope.parent = oldScope;
		if (! etree.scope.top) {
		    etree.scope.level = etree.scope.parent.level + 1;
		}
	    }
	    String space = etree.getSpace();
	    String base = etree.getBase();
	    bind2Lang(space,base,etree,xt.currentLang);
	    symtab.setScope(oldScope);
	} else {
	    bind2exp(tree);
	}
    }

    public void bind(XDOM tree,int index,String path,Object lang) {
	XDOMElement oldParent = parent;
	XDOM oldPrev = prev;
	String oldPath = myPath;
	Object oldLang = xt.currentLang;

	xt.currentLang = lang;
	parent = null;
	prev = null;
	if (path != null) myPath = path;
	if (tree instanceof XDOMElement) {
	    XDOMElement etree = (XDOMElement) tree;
	    XScope.add(etree);
	    if (path != null) {
		XDefVal def = new XDefVal();
		def.name = "myPath";
		def.oval = path;
		def.ival = 0;
		etree.scope.add(def);
	    }
	    etree.scope.top = true;
	    etree.scope.level = index;
	}
	Layer layer = new Layer(null,InterExec.PBIND1);
	xt.exec.layerStack.push(layer);
	bind1(tree);
	symtab.setScope(biBind.biscope);
	prev = null;
	layer.pass = InterExec.PBIND2;
	bind2(tree);
	xt.exec.layerStack.pop();
	parent = oldParent;
	prev = oldPrev;
	myPath = oldPath;
	xt.currentLang = oldLang;
    }

}
