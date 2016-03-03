/*******************************************************************************
*
* Copyright (c) 2002-2007. John R. Nestor. All rights reserved.
*
* See the file XLICENSE.txt for use and redistribution information.
*
*******************************************************************************/

package com.persist.xact.exec;

import com.persist.xdom.*;
import com.persist.xact.system.*;
import com.persist.xact.parse.*;
import com.persist.xact.value.*;

public final class TreeExec {
    private XThread xt;
    private Exec exec;
    private VariableStack stack;
    private Errors errors;
    private ParseStack ps;
    private Value value;
    private EValue evalue;
    private Join join;
    private CallExec callExec;
    private XOption option;

    public TreeExec(Exec exec) {
	this.exec = exec;
    }

    public void init() {
	xt = exec.xt;
	stack = exec.stack;
	errors = exec.xt.errors;
	ps = exec.xt.ps;
	value = exec.xt.cmd.value;
	evalue = exec.evalue;
	join = exec.join;
	callExec = exec.callExec;
	option = xt.cmd.option;
    }

    private byte spaceByte(String s,XPos pos) {
	int size = XDOM.spaceKind.length;
	byte i;
	for (i = 0; i < size; i++) {
	    if (s == XDOM.spaceKind[i]) {
		return i;
	    }
	}
	errors.error(Errors.EXEC,pos,"illegal space value");
	return -1;
    }

    private void spacePushString(byte i,XPos pos) {
	int size = XDOM.spaceKind.length;
	if (0 <= i && i < size) {
	    stack.push(XDOM.spaceKind[i],0);
	} else {
	    errors.error(Errors.EXEC,pos,"illegal internal space code");
	    stack.pushError();
	}
    }

    public boolean doAttr(XDOM x,String name,Ctx ctx,XPos pos) {
	if (ctx == value.ctxAssign) {
	    if (name == "spaceBefore") {
		if (evalue.getS(pos)) {
		    Object oval = stack.getTopOval();
		    long ival = stack.getTopIval();
		    byte code = spaceByte(XDOMValue.getString(oval,ival),pos);
		    if (code >= 0) {
			x.setSpaceBefore(code);
		    }
		}
		return true;
	    }
	    if (name == "spaceAfter") {
		if (evalue.getS(pos)) {
		    Object oval = stack.getTopOval();
		    long ival = stack.getTopIval();
		    byte code = spaceByte(XDOMValue.getString(oval,ival),pos);
		    if (code >= 0) {
			x.setSpaceAfter(code);
		    }
		}
		return true;
	    }
	    if (name == "breakBefore") {
		if (evalue.getB(pos)) {
		    long ival = stack.getTopIval();
		    boolean val = false;
		    if (ival == 1) val = true;
		    x.setBreakBefore(val);
		}
		return true;
	    }
	    if (name == "breakAfter") {
		if (evalue.getB(pos)) {
		    long ival = stack.getTopIval();
		    boolean val = false;
		    if (ival == 1) val = true;
		    x.setBreakAfter(val);
		}
		return true;
	    }
	    if (name == "noStep") {
		if (evalue.getB(pos)) {
		    long ival = stack.getTopIval();
		    boolean val = false;
		    if (ival == 1) val = true;
		    x.setNoStep(val);
		}
		return true;
	    }
	    if (name == "mark1") {
		if (evalue.getB(pos)) {
		    long ival = stack.getTopIval();
		    boolean val = false;
		    if (ival == 1) val = true;
		    x.mark1 = val;
		}
		return true;
	    }
	    if (name == "mark2") {
		if (evalue.getB(pos)) {
		    long ival = stack.getTopIval();
		    boolean val = false;
		    if (ival == 1) val = true;
		    x.mark2 = val;
		}
		return true;
	    }
	    if (name == "mark3") {
		if (evalue.getB(pos)) {
		    long ival = stack.getTopIval();
		    boolean val = false;
		    if (ival == 1) val = true;
		    x.mark3 = val;
		}
		return true;
	    }
	    if (name == "label") {
		if (evalue.getS(pos)) {
		    Object oval = stack.getTopOval();
		    long ival = stack.getTopIval();
		    x.label = XDOMValue.getString(oval,ival);
		}
		return true;
	    }
	} else {
	    if (name == "spaceBefore") {
		byte code = x.getSpaceBefore();
		spacePushString(code,pos);
		return true;
	    }
	    if (name == "spaceAfter") {
		byte code = x.getSpaceAfter();
		spacePushString(code,pos);
		return true;
	    }
	    if (name == "breakBefore") {
		boolean b = x.getBreakBefore();
		long ival = 0;
		if (b) ival = 1;
		stack.push(value.vlBool,ival);
		return true;
	    }
	    if (name == "breakAfter") {
		boolean b = x.getBreakAfter();
		long ival = 0;
		if (b) ival = 1;
		stack.push(value.vlBool,ival);
		return true;
	    }
	    if (name == "noStep") {
		boolean b = x.getNoStep();
		long ival = 0;
		if (b) ival = 1;
		stack.push(value.vlBool,ival);
		return true;
	    }
	    if (name == "mark1") {
		boolean b = x.mark1;
		long ival = 0;
		if (b) ival = 1;
		stack.push(value.vlBool,ival);
		return true;
	    }
	    if (name == "mark2") {
		boolean b = x.mark2;
		long ival = 0;
		if (b) ival = 1;
		stack.push(value.vlBool,ival);
		return true;
	    }
	    if (name == "mark3") {
		boolean b = x.mark3;
		long ival = 0;
		if (b) ival = 1;
		stack.push(value.vlBool,ival);
		return true;
	    }
	    if (name == "label") {
		stack.push(x.label,0);
		return true;
	    }
	}
	return false;
    }

    private XDOM makeAttr(Object oval,long ival,boolean allowTree) {
	if (allowTree && oval instanceof XDOM) {
	    return (XDOM) oval;
	} if (XDOMValue.isString(oval,ival)) {
	    XDOMString stree = new XDOMString(oval,ival);
	    return stree;
	} else if (XDOMValue.isInt(oval,ival)) {
	    XDOMInt itree = new XDOMInt(ival);
	    return itree;
	} else if (XDOMValue.isFloat(oval,ival)) {
	    double f1 = Double.longBitsToDouble(ival);
	    XDOMFloat ftree = new XDOMFloat(f1);
	    return ftree;
	} else {
	    XDOMValue vtree = new XDOMValue(oval,ival);
	    return vtree;
	}
    }

    private XDOM makeAttr(String name,Object oval,long ival,boolean allowTree) {
	XDOMName equalTree = new XDOMName("Equal");
	XDOMCall ctree = new  XDOMCall(equalTree,2);
	XDOMName ntree = new XDOMName(name);
	ctree.setKind(XDOMCall.COP);
	ctree.insertArg(-1,ntree);
	ctree.insertArg(-1,makeAttr(oval,ival,allowTree));
	return ctree;
    }

    /*************************************************************************
    *
    *   TreeElement
    *
    *************************************************************************/

    private byte elemKindByte(String s,XPos pos) {
	int size = XDOMElement.EKind.length;
	byte i;
	for (i = 0; i < size; i++) {
	    if (s == XDOMElement.EKind[i]) {
		return i;
	    }
	}
	errors.error(Errors.EXEC,pos,"illegal element kind value");
	return -1;
    }

    private void elemKindPushString(byte i,XPos pos) {
	int size = XDOMElement.EKind.length;
	if (0 <= i && i < size) {
	    stack.push(XDOMElement.EKind[i],0);
	} else {
	    errors.error(Errors.EXEC,pos,"illegal internal element kind code");
	    stack.pushError();
	}
    }

    public boolean doAttr(XDOMElement xe,String name,Ctx ctx,XPos pos) {
	if (doAttr((XDOM) xe,name,ctx,pos)) return true;
	if (ctx == value.ctxAssign) {
	    if (name == "spaceBeforeBody") {
		if (evalue.getS(pos)) {
		    Object oval = stack.getTopOval();
		    long ival = stack.getTopIval();
		    byte code = spaceByte(XDOMValue.getString(oval,ival),pos);
		    if (code >= 0) {
			xe.setSpaceBeforeBody(code);
		    }
		}
		return true;
	    }
	    if (name == "spaceAfterBody") {
		if (evalue.getS(pos)) {
		    Object oval = stack.getTopOval();
		    long ival = stack.getTopIval();
		    byte code = spaceByte(XDOMValue.getString(oval,ival),pos);
		    if (code >= 0) {
			xe.setSpaceAfterBody(code);
		    }
		}
		return true;
	    }
	    if (name == "kind") {
		if (evalue.getS(pos)) {
		    Object oval = stack.getTopOval();
		    long ival = stack.getTopIval();
		    byte code = elemKindByte(XDOMValue.getString(oval,ival),pos);
		    if (code >= 0) {
			xe.setKind(code);
		    }
		}
		return true;
	    }
	    if (name == "tag") {
		if (evalue.getS(pos)) {
		    Object oval = stack.getTopOval();
		    long ival = stack.getTopIval();
		    xe.setTag(XDOMValue.getString(oval,ival));
		}
		return true;
	    }
	    if (name == "space") {
		if (evalue.getS(pos)) {
		    Object oval = stack.getTopOval();
		    long ival = stack.getTopIval();
		    xe.setSpace(XDOMValue.getString(oval,ival));
		}
		return true;
	    }
	    if (name == "base") {
		if (evalue.getS(pos)) {
		    Object oval = stack.getTopOval();
		    long ival = stack.getTopIval();
		    xe.setBase(XDOMValue.getString(oval,ival));
		}
		return true;
	    }
	    if (name == "size") {
		errors.error(Errors.EXEC,pos,"can't assign to size field");
		return true;
	    }
	} else {
	    if (name == "spaceBeforeBody") {
		byte code = xe.getSpaceBeforeBody();
		spacePushString(code,pos);
		return true;
	    }
	    if (name == "spaceAfterBody") {
		byte code = xe.getSpaceAfterBody();
		spacePushString(code,pos);
		return true;
	    }
	    if (name == "kind") {
		byte code = xe.getKind();
		elemKindPushString(code,pos);
		return true;
	    }
	    if (name == "tag") {
		String s = xe.getTag();
		stack.push(s,0);
		return true;
	    }
	    if (name == "space") {
		String s = xe.getSpace();
		stack.push(s,0);
		return true;
	    }
	    if (name == "base") {
		String s = xe.getBase();
		stack.push(s,0);
		return true;
	    }
	    if (name == "size") {
		stack.push(value.vlInt,xe.arraySize());
		return true;
	    }
	}
	return false;
    }

    private void pushSeqCall(XDOM tree) {
	if (option.optimize && tree instanceof XDOMCall) {
	    XDOMCall xc = (XDOMCall) tree;
	    XDOM func = xc.getFunc();
	    if (func instanceof XDOMName) {
		XDOMName xn = (XDOMName) func;
		String s = xn.getName();
		if (s == "rec") {
		    pushRecord(xc);
		    return;
		} else if (s == "array") {
		    pushArray(xc);
		    return;
		} else if (s == "list") {
		    pushList(xc);
		    return;
		}
	    }
	} 
	exec.execExp(tree,value.ctxEval);
	Object oval = stack.getTopOval();
	long ival = stack.getTopIval();
	stack.pop();
	if (oval instanceof XDOMElement) {
	    pushAttrs((XDOMElement) oval);
	} else {
	    errors.error(Errors.EXEC,tree.pos,"not an xdom element");
	}
    }

    public void makeTreeElement(XDOMCall ctree) {
	int size = ctree.argSize();
	if (size >= 1)  {
	    XDOM arg1 = ctree.getArg(1);
	    if (evalue.getS(arg1)) {
		Object oval = stack.getTopOval();
		long ival = stack.getTopIval();
		stack.pop();
		String name = XDOMValue.getString(oval,ival);
		XDOMElement newTree = new XDOMElement(name);
		newTree.setSpaceBefore(XDOM.LINE);
		newTree.setSpaceBeforeBody(XDOM.LINE);
		newTree.setSpaceAfterBody(XDOM.LINE);
		newTree.setSpaceAfter(XDOM.LINE);
		if (size > 1) {
		    boolean haveAttr = false;
		    boolean haveBody = false;
		    for (int i = 2; i <= size; i++) {
			XDOM atree = ctree.getArg(i);
			if (TreeUtil.isEqual(atree)) {
			    String aname = TreeUtil.getEqualName(atree);
			    XDOM atree1 = TreeUtil.stripEqual(atree);
			    if (aname == "attr") {
				if (haveAttr) {
				    errors.error(Errors.EXEC,atree.pos,"duplicate attr= attribute");
				} else {
				    haveAttr = true;
				    int start = ps.start();
				    pushSeqCall(atree1);
				    ps.popAttr(start,newTree,true);
				}
			    } else if (aname == "body") {
				if (haveBody) {
				    errors.error(Errors.EXEC,atree.pos,"duplicate body= attribute");
				} else {
				    haveBody = true;
				    int start = ps.start();
				    pushSeqCall(atree1);
				    ps.popBody(start,newTree,true);
				}
			    } else {
				exec.execExp(atree1,value.ctxEval);
				if (! doAttr(newTree,aname,value.ctxAssign,atree.pos)) {
				    errors.error(Errors.EXEC,atree.pos,"unrecognized attribute:"+name);
				}
				stack.pop();
			    }
			} else {
			    errors.error(Errors.EXEC,atree.pos,"missing name =");
			}
		    }
		}
		stack.push(newTree,0);
		return;
	    }
	    stack.pop();
	} else {
	    errors.error(Errors.EXEC,ctree.pos,"no tag specified");
	}
	stack.pushError();
    }

    /*************************************************************************
    *
    *   TreeElement : Attr
    *
    *************************************************************************/

    public void attrSize(XDOMCall tree) {
	XDOM arg1 = tree.getArg(1);
	exec.execExp(arg1,value.ctxEval);
	Object oval = stack.getTopOval();
	long ival = stack.getTopIval();
	stack.pop();
	if (oval instanceof XDOMElement) {
	    XDOMElement xe = (XDOMElement) oval;
	    stack.push(value.vlInt,xe.attrSize());
	} else {
	    if (! value.isError(oval,ival)) {
		errors.error(Errors.EXEC,arg1.pos,"not an xdom element");
	    }
	    stack.pushError();
	}
    }

    public void attrInsert(XDOMElement xe,XDOMCall tree) {
	int args = tree.argSize();
	if (args != 1 && args != 2) {
	    errors.error(Errors.EXEC,tree.pos,"wrong number of actual parameters");
	    stack.pushError();
	    return;
	}
	XDOM arg1 = tree.getArg(1);
	XDOM argv = arg1;
	int size = xe.attrSize();
	boolean ok = true;

	int index = -1;
	if (args == 2) {
	    argv = tree.getArg(2);
	    if (evalue.getI(arg1)) {
		index =  (int) stack.getTopIval();
		if (index < -1 || index > size) {
		    errors.error(Errors.EXEC,arg1.pos,"insert index out of range");
		    ok = false;
		}
	    } else {
		ok = false;
	    } 
	    stack.pop();
	}

	XDOM newElem = null;
	exec.execExp(argv,value.ctxEval);
	Object oval2 = stack.getTopOval();
	long ival2 = stack.getTopIval();
	stack.pop();
	if (oval2 instanceof XDOM) {
	    newElem = (XDOM) oval2;
	} else {
	    if (! value.isError(oval2,ival2)) {
		errors.error(Errors.EXEC,argv.pos,"not an xdom");
	    }
	    ok = false;
	}

	if (ok) {
	    xe.insertAttr(index,newElem);
	}
	stack.pushNull();
    }

    public void attrDelete(XDOMElement xe,XDOMCall tree) {
	XDOM arg1 = tree.getArg(1);
	int size = xe.attrSize();
	boolean ok = true;
	int index = 0;
	if (evalue.getI(arg1)) {
	    index = (int) stack.getTopIval();
	    if (index < 1 || index > size) {
		errors.error(Errors.EXEC,arg1.pos,"delete index out of range");
		ok = false;
	    }
	} else {
	    ok = false;
	}
	stack.pop();
	if (ok) {
	    xe.deleteAttr(index);
	}
	stack.pushNull();
    }

    public void attrAll(XDOMElement xe,XDOMCall tree,Ctx ctx) {
	XDOM arg1 = tree.getArg(1);
	exec.execExp(arg1,value.ctxEval);
	Object oval = stack.getTopOval();
	long ival = stack.getTopIval();
	stack.pop();
	if (oval instanceof VLFunc) {
	    VLFunc vlf = (VLFunc) oval;
	    join.start(ctx);
	    int size = xe.attrSize();
	    boolean save = xt.doBreak;
	    xt.doBreak = false;
	    for (int i = 1; i <= size; i++) {
		XDOM x = xe.getAttr(i);
		stack.push(x,0);
		callExec.execCallFunc(vlf,0,1,tree,value.ctxEval);
		join.add(x.pos);
		if (xt.doBreak) break;
	    }
	    join.pop();
	    xt.doBreak = save;
	} else {
	    if (! value.isError(oval,ival)) {
		errors.error(Errors.EXEC,arg1.pos,"iterator parameter not a function");
	    }
	    stack.pushError();
	}
    }

    public void attrSubscript(XDOMElement xe,XDOMCall tree,Ctx ctx) {
	XDOM arg1 = tree.getArg(1);
	int size = xe.attrSize();
	boolean ok = true;
	int index = 0;
	if (evalue.getI(arg1)) {
	    index = (int) stack.getTopIval();
	    if (index < 1 || index > size) {
		errors.error(Errors.EXEC,arg1.pos,"subscript out of range");
		ok = false;
	    }
	} else {
	    ok = false;
	}
	stack.pop();
	if (ok) {
	    if (ctx == value.ctxAssign) {
		Object oval = stack.getTopOval();
		if (oval instanceof XDOM) {
		    xe.setAttr(index,(XDOM) oval);
		} else {
		    errors.error(Errors.EXEC,tree.pos,"value to be assigned is not an xdom");
		}
	    } else {
		XDOM x = xe.getAttr(index);
		stack.push(x,0);
		return;
	    }
	}
	if (ctx != value.ctxAssign) {
	    stack.pushError();
	}
    }

    /*************************************************************************
    *
    *   TreeElement : Body
    *
    *************************************************************************/

    public void bodySize(XDOMCall tree) {
	XDOM arg1 = tree.getArg(1);
	exec.execExp(arg1,value.ctxEval);
	Object oval = stack.getTopOval();
	long ival = stack.getTopIval();
	stack.pop();
	if (oval instanceof XDOMElement) {
	    XDOMElement xe = (XDOMElement) oval;
	    stack.push(value.vlInt,xe.bodySize());
	} else {
	    if (! value.isError(oval,ival)) {
		errors.error(Errors.EXEC,arg1.pos,"not an xdom element");
	    }
	    stack.pushError();
	}
    }

    public void bodyInsert(XDOMElement xe,XDOMCall tree) {
	int args = tree.argSize();
	if (args != 1 && args != 2) {
	    errors.error(Errors.EXEC,tree.pos,"wrong number of actual parameters");
	    stack.pushError();
	    return;
	}
	XDOM arg1 = tree.getArg(1);
	XDOM argv = arg1;
	int size = xe.bodySize();
	boolean ok = true;

	int index = -1;
	if (args == 2) {
	    argv = tree.getArg(2);
	    if (evalue.getI(arg1)) {
		index = (int) stack.getTopIval();
		if (index < -1 || index > size) {
		    errors.error(Errors.EXEC,arg1.pos,"insert index out of range");
		    ok = false;
		}
	    } else {
		ok = false;
	    }
	    stack.pop();
	}

	XDOM newElem = null;
	exec.execExp(argv,value.ctxEval);
	Object oval2 = stack.getTopOval();
	long ival2 = stack.getTopIval();
	stack.pop();
	if (oval2 instanceof XDOM) {
	    newElem = (XDOM) oval2;
	} else {
	    if (! value.isError(oval2,ival2)) {
		errors.error(Errors.EXEC,argv.pos,"not an xdom");
	    }
	    ok = false;
	}

	if (ok) {
	    xe.insertBody(index,newElem);
	}
	stack.pushNull();
    }

    public void bodyDelete(XDOMElement xe,XDOMCall tree) {
	XDOM arg1 = tree.getArg(1);
	int size = xe.bodySize();
	boolean ok = true;
	int index = 0;
	if (evalue.getI(arg1)) {
	    index = (int) stack.getTopIval();
	    if (index < 1 || index > size) {
		errors.error(Errors.EXEC,arg1.pos,"delete index out of range");
		ok = false;
	    }
	} else {
	    ok = false;
	}
	stack.pop();
	if (ok) {
	    xe.deleteBody(index);
	}
	stack.pushNull();
    }

    public void bodyAll(XDOMElement xe,XDOMCall tree,Ctx ctx) {
	XDOM arg1 = tree.getArg(1);
	exec.execExp(arg1,value.ctxEval);
	Object oval = stack.getTopOval();
	long ival = stack.getTopIval();
	stack.pop();
	if (oval instanceof VLFunc) {
	    VLFunc vlf = (VLFunc) oval;
	    join.start(ctx);
	    int size = xe.bodySize();
	    boolean save = xt.doBreak;
	    xt.doBreak = false;
	    for (int i = 1; i <= size; i++) {
		XDOM x = xe.getBody(i);
		stack.push(x,0);
		callExec.execCallFunc(vlf,0,1,tree,value.ctxEval);
		join.add(x.pos);
		if (xt.doBreak) break;
	    }
	    join.pop();
	    xt.doBreak = save;
	} else {
	    if (! value.isError(oval,ival)) {
		errors.error(Errors.EXEC,arg1.pos,"iterator parameter not a function");
	    }
	    stack.pushError();
	}
    }

    public void bodySubscript(XDOMElement xe,XDOMCall tree,Ctx ctx) {
	XDOM arg1 = tree.getArg(1);
	int size = xe.bodySize();
	boolean ok = true;
	int index = 0;
	if (evalue.getI(arg1)) {
	    index = (int) stack.getTopIval();
	    if (index < 1 || index > size) {
		errors.error(Errors.EXEC,arg1.pos,"subscript out of range");
		ok = false;
	    }
	} else {
	    ok = false;
	}
	stack.pop();
	if (ok) {
	    if (ctx == value.ctxAssign) {
		Object oval = stack.getTopOval();
		if (oval instanceof XDOM) {
		    xe.setBody(index,(XDOM) oval);
		} else {
		    errors.error(Errors.EXEC,tree.pos,"value to be assigned is not an xdom");
		}
	    } else {
		XDOM x = xe.getBody(index);
		stack.push(x,0);
		return;
	    }
	}
	if (ctx != value.ctxAssign) {
	    stack.pushError();
	}
    }

    /*************************************************************************
    *
    *   TreeElement : list
    *
    *************************************************************************/

    private void pushList(XDOMCall ctree) {
	int size = ctree.argSize();
	for (int i = 1; i <= size; i++) {
	    XDOM arg = ctree.getArg(i);
	    if (TreeUtil.hasPercent(arg)) {
		XDOM arg1 = TreeUtil.stripPercent(arg);
		exec.execExp(arg1,value.ctxEval);
		Object oval = stack.getTopOval();
		long ival = stack.getTopIval();
		stack.pop();
		if (oval instanceof XDOMElement) {
		    pushAttrs((XDOMElement) oval);
		} else if (! value.isError(oval,ival)) {
		    errors.error(Errors.EXEC,arg1.pos,"not an xdom element");
		}
	    } else if (TreeUtil.isEqual(arg)) {
		String name = TreeUtil.getEqualName(arg);
		XDOM val = TreeUtil.stripEqual(arg);
		exec.execExp(val,value.ctxEval);
		Object oval = stack.getTopOval();
		long ival = stack.getTopIval();
		stack.pop();
		if (! value.isError(oval,ival)) {
		    ps.push(makeAttr(name,oval,ival,true));
		}
	    } else {
		exec.execExp(arg,value.ctxEval);
		Object oval = stack.getTopOval();
		long ival = stack.getTopIval();
		stack.pop();
		if (! value.isError(oval,ival)) {
		    ps.push(makeAttr(oval,ival,true));
		}
	    }
	}
    }

    public void makeList(XDOMCall ctree) {
	XDOMElement xe = new XDOMElement("list");
	xe.setSpaceBefore(XDOM.LINE);
	xe.setSpaceBeforeBody(XDOM.LINE);
	xe.setSpaceAfterBody(XDOM.LINE);
	xe.setSpaceAfter(XDOM.LINE);
	int start = ps.start();
	pushList(ctree);
	ps.popAttr(start,xe,true);
	stack.push(xe,0);
    }

    /*************************************************************************
    *
    *   TreeElement : array
    *
    *************************************************************************/

    private void pushArray(XDOMCall ctree) {
	int size = ctree.argSize();
	for (int i = 1; i <= size; i++) {
	    XDOM arg = ctree.getArg(i);
	    if (TreeUtil.hasPercent(arg)) {
		XDOM arg1 = TreeUtil.stripPercent(arg);
		exec.execExp(arg1,value.ctxEval);
		Object oval = stack.getTopOval();
		long ival = stack.getTopIval();
		stack.pop();
		if (oval instanceof XDOMElement) {
		    pushAttrs((XDOMElement) oval);
		} else if (! value.isError(oval,ival)) {
		    errors.error(Errors.EXEC,arg1.pos,"not an xdom element");
		}
	    } else {
		exec.execExp(arg,value.ctxEval);
		Object oval = stack.getTopOval();
		long ival = stack.getTopIval();
		stack.pop();
		if (! value.isError(oval,ival)) {
		    ps.push(makeAttr(oval,ival,false));
		}
	    }
	}
    }

    public void makeArray(XDOMCall ctree) {
	int args = ctree.argSize();
	XDOMElement xe = new XDOMElement("array");
	xe.setSpaceBefore(XDOM.LINE);
	xe.setSpaceBeforeBody(XDOM.LINE);
	xe.setSpaceAfterBody(XDOM.LINE);
	xe.setSpaceAfter(XDOM.LINE);
	if (args == 2) {
	    XDOM arg1 = ctree.getArg(1);
	    XDOM arg2 = ctree.getArg(2);
	    if (TreeUtil.isEqual(arg1) && TreeUtil.getEqualName(arg1) == "size") {
		if (evalue.getI(TreeUtil.stripEqual(arg1))) {
		    int size = (int) stack.getTopIval();
		    stack.pop();
		    exec.execExp(arg2,value.ctxEval);
		    Object oval = stack.getTopOval();
		    long ival = stack.getTopIval();
		    if (! value.isError(oval,ival)) {
			stack.pop();
			xe.clearAttr(size);
			for(int i = 0; i < size; i++) {
			    xe.insertEArray(-1,oval,ival);
			}
			stack.push(xe,0);
		    }
		}
		return;
	    }
	}
	int start = ps.start();
	pushArray(ctree);
	ps.popAttr(start,xe,true);
	stack.push(xe,0);
    }

    public void arraySubscript(XDOMElement xe,XDOMCall tree,Ctx ctx) {
	XDOM arg1 = tree.getArg(1);
	int size = xe.arraySize();
	boolean ok = true;
	int index = 0;
	if (evalue.getI(arg1)) {
	    index = (int) stack.getTopIval();
	    if (index < 1 || index > size) {
		errors.error(Errors.EXEC,arg1.pos,"subscript out of range");
		ok = false;
	    }
	} else {
	    ok = false;
	}
	stack.pop();
	if (ok) {
	    if (ctx == value.ctxAssign) {
		Object oval = stack.getTopOval();
		long ival = stack.getTopIval();
		xe.setEArray(index,oval,ival);
	    } else {
		XDOM x = xe.getEArray(index);
		int kind = x.getXKind();
		if (kind == XDOM.XSTRING) {
		    XDOMString xs = (XDOMString) x;
		    stack.push(xs.getOval(),xs.getIval());
		} else if (kind == XDOM.XINT) {
		    XDOMInt xi = (XDOMInt) x;
		    stack.push(value.vlInt,xi.getInt());
		} else if (kind == XDOM.XFLOAT) {
		    XDOMFloat xf = (XDOMFloat) x;
		    double d = xf.getFloat();
		    long i = Double.doubleToLongBits(d);
		    stack.push(value.vlFloat,i);
		} else if (kind == XDOM.XVALUE) {
		    XDOMValue xv = (XDOMValue) x;
		    stack.push(xv.getOval(),xv.getIval());
		} else {
		    errors.error(Errors.INTERNAL,tree.pos,"bad array value: "+kind);
		    stack.pushError();
		}
		return;
	    }
	}
	if (ctx != value.ctxAssign) {
	    stack.pushError();
	}
    }

    public void arrayInsert(XDOMElement xe,XDOMCall tree) {
	int args = tree.argSize();
	if (args != 1 && args != 2) {
	    errors.error(Errors.EXEC,tree.pos,"wrong number of actual parameters");
	    stack.pushError();
	    return;
	}
	XDOM arg1 = tree.getArg(1);
	XDOM argv = arg1;
	int size = xe.arraySize();
	boolean ok = true;

	int index = -1;
	if (args == 2) {
	    argv = tree.getArg(2);
	    if (evalue.getI(arg1)) {
		index =  (int) stack.getTopIval();
		if (index < -1 || index > size) {
		    errors.error(Errors.EXEC,arg1.pos,"insert index out of range");
		    ok = false;
		}
	    } else {
		ok = false;
	    } 
	    stack.pop();
	}

	XDOM newElem = null;
	exec.execExp(argv,value.ctxEval);
	Object oval2 = stack.getTopOval();
	long ival2 = stack.getTopIval();
	stack.pop();
	if (value.isError(oval2,ival2)) {
	    ok = false;
	}

	if (ok) {
	    xe.insertEArray(index,oval2,ival2);
	}
	stack.pushNull();
    }

    public void arrayDelete(XDOMElement xe,XDOMCall tree) {
	XDOM arg1 = tree.getArg(1);
	int size = xe.arraySize();
	boolean ok = true;
	int index = 0;
	if (evalue.getI(arg1)) {
	    index = (int) stack.getTopIval();
	    if (index < 1 || index > size) {
		errors.error(Errors.EXEC,arg1.pos,"delete index out of range");
		ok = false;
	    }
	} else {
	    ok = false;
	}
	stack.pop();
	if (ok) {
	    xe.deleteArray(index);
	}
	stack.pushNull();
    }

    public void arrayAll(XDOMElement xe,XDOMCall tree,Ctx ctx) {
	XDOM arg1 = tree.getArg(1);
	exec.execExp(arg1,value.ctxEval);
	Object oval = stack.getTopOval();
	long ival = stack.getTopIval();
	stack.pop();
	if (oval instanceof VLFunc) {
	    VLFunc vlf = (VLFunc) oval;
	    join.start(ctx);
	    int size = xe.arraySize();
	    boolean save = xt.doBreak;
	    xt.doBreak = false;
	    for (int i = 1; i <= size; i++) {
		XDOM x = xe.getEArray(i);
		int kind = x.getXKind();
		if (kind == XDOM.XSTRING) {
		    XDOMString xs = (XDOMString) x;
		    stack.push(xs.getOval(),xs.getIval());
		} else if (kind == XDOM.XINT) {
		    XDOMInt xi = (XDOMInt) x;
		    stack.push(value.vlInt,xi.getInt());
		} else if (kind == XDOM.XFLOAT) {
		    XDOMFloat xf = (XDOMFloat) x;
		    double d = xf.getFloat();
		    long i1 = Double.doubleToLongBits(d);
		    stack.push(value.vlFloat,i1);
		} else if (kind == XDOM.XVALUE) {
		    XDOMValue xv = (XDOMValue) x;
		    stack.push(xv.getOval(),xv.getIval());
		} else {
		    errors.error(Errors.INTERNAL,x.pos,"bad array value: "+kind);
		    stack.pushError();
		}
		callExec.execCallFunc(vlf,0,1,tree,value.ctxEval);
		join.add(x.pos);
		if (xt.doBreak) break;
	    }
	    join.pop();
	    xt.doBreak = save;
	} else {
	    if (! value.isError(oval,ival)) {
		errors.error(Errors.EXEC,arg1.pos,"iterator parameter not a function");
	    }
	    stack.pushError();
	}
    }

    /*************************************************************************
    *
    *   TreeElement : Record
    *
    *************************************************************************/

    private void pushAttrs(XDOMElement xe) {
	int size = xe.attrSize();
	for (int i = 1; i <= size; i++) {
	    ps.push(xe.getAttr(i));
	}
    }
    
    private void pushRecord(XDOMCall ctree) {
	int size = ctree.argSize();
	for (int i = 1; i <= size; i++) {
	    XDOM arg = ctree.getArg(i);
	    if (TreeUtil.hasPercent(arg)) {
		XDOM arg1 = TreeUtil.stripPercent(arg);
		exec.execExp(arg1,value.ctxEval);
		Object oval = stack.getTopOval();
		long ival = stack.getTopIval();
		stack.pop();
		if (oval instanceof XDOMElement) {
		    pushAttrs((XDOMElement) oval);
		} else if (! value.isError(oval,ival)) {
		    errors.error(Errors.EXEC,arg1.pos,"not an xdom element");
		}
	    } else if (TreeUtil.isEqual(arg)) {
		String name = TreeUtil.getEqualName(arg);
		XDOM val = TreeUtil.stripEqual(arg);
		exec.execExp(val,value.ctxEval);
		Object oval = stack.getTopOval();
		long ival = stack.getTopIval();
		stack.pop();
		if (! value.isError(oval,ival)) {
		    ps.push(makeAttr(name,oval,ival,false));
		}
	    } else {
		errors.error(Errors.EXEC,arg.pos,"not an = expression");
	    }
	}
    }

    public void makeRec(XDOMCall ctree) {
	XDOMElement xe = new XDOMElement("rec");
	xe.setSpaceBefore(XDOM.LINE);
	xe.setSpaceBeforeBody(XDOM.LINE);
	xe.setSpaceAfterBody(XDOM.LINE);
	xe.setSpaceAfter(XDOM.LINE);
	int start = ps.start();
	pushRecord(ctree);
	ps.popAttr(start,xe,true);
	stack.push(xe,0);
    }

    private boolean isCat(XDOM x) {
	if (x instanceof XDOMCall) {
	    XDOMCall xc = (XDOMCall) x;
	    int args=xc.argSize();
	    XDOM func = xc.getFunc();
	    if (func instanceof XDOMName) {
		XDOMName xn = (XDOMName) func;
		if (xn.getName() != "Cat") return false;
		for (int i = 1; i <= args; i++) {
		    XDOM arg = xc.getArg(i);
		    if (arg instanceof XDOMString) {
		    } else if (arg instanceof XDOMName) {
			XDOMName xn1 = (XDOMName) arg;
			String name = xn1.getName();
			if (name == "quot") {
			} else if (name == "apos") {
			} else if (name == "lt") {
			} else if (name == "gt") {
			} else if (name == "amp") {
			} else if (name == "nbsp") {
			} else {
			    return false;
			}
		    } else {
			return false;
		    }
		}
		return true;
	    }
	}
	return false;
    }

    private void catVal(XDOM x,Ctx ctx) {
	join.start(ctx);
	XDOMCall xc = (XDOMCall) x;
	int args=xc.argSize();
	for (int i = 1; i <= args; i++) {
	    XDOM arg = xc.getArg(i);
	    if (arg instanceof XDOMString) {
		XDOMString xs = (XDOMString) arg;
		stack.push(xs.getOval(),xs.getIval());
		join.add(null);
	    } else if (arg instanceof XDOMName) {
		XDOMName xn1 = (XDOMName) arg;
		String name = xn1.getName();
		if (name == "quot") {
		    stack.push("\"",0);
		    join.add(null);
		} else if (name == "apos") {
		    stack.push("'",0);
		    join.add(null);
		} else if (name == "lt") {
		    stack.push("<",0);
		    join.add(null);
		} else if (name == "gt") {
		    stack.push(">",0);
		    join.add(null);
		} else if (name == "amp") {
		    stack.push("&",0);
		    join.add(null);
		} else if (name == "nbsp") {
		    stack.push("\240",0);
		    join.add(null);
		}
	    }
	}
	join.pop();
    }

    public void recordSelect(XDOM tree,String name,XPos pos,Ctx ctx) {
	Object oval;
	long ival;
	exec.execExp(tree,value.ctxEval);
	oval = stack.getTopOval();
	ival = stack.getTopIval();
	stack.pop();
	if (oval instanceof XDOMElement) {
	    XDOMElement elemtree = (XDOMElement) oval;
	    if (ctx == value.ctxAssign) {
		//evalue.getS((XPos) null);
		oval = stack.getTopOval();
		ival = stack.getTopIval();
		if (elemtree == option.options) {
		    boolean ok = option.setOption(name,oval,ival);
		    if (! ok) {
			errors.error(Errors.EXEC,tree.pos,"can't set option:"+name);
		    }
		} else {
		    elemtree.setERec(name,oval,ival);
		}
	    } else {
		XDOM x = elemtree.getERec(name);
		if (x == null) {
		    stack.pushNull();
		} else {
		    int kind = x.getXKind();
		    if (kind == XDOM.XSTRING) {
			XDOMString xs = (XDOMString) x;
			stack.push(xs.getOval(),xs.getIval());
		    } else if (kind == XDOM.XINT) {
			XDOMInt xi = (XDOMInt) x;
			stack.push(value.vlInt,xi.getInt());
		    } else if (kind == XDOM.XFLOAT) {
			XDOMFloat xf = (XDOMFloat) x;
			stack.push(value.vlFloat,Double.doubleToLongBits(xf.getFloat()));
		    } else if (kind == XDOM.XVALUE) {
			XDOMValue xv = (XDOMValue) x;
			stack.push(xv.getOval(),xv.getIval());
		    } else if (isCat(x)) {
			catVal(x,ctx);
		    } else {
			errors.error(Errors.INTERNAL,tree.pos,"bad record value");
			stack.pushError();
		    }
		}
	    }
	} else {
	    if (! value.isError(oval,ival)) {
		errors.error(Errors.EXEC,tree.pos,"not an xdom element");
	    }
	    if (ctx != value.ctxAssign) {
		stack.pushError();
	    }
	}
    }

    /*************************************************************************
    *
    *   TreeElement : Body Record
    *
    *************************************************************************/

    public void bodyRecordSelect(XDOM tree,String name,XPos pos,Ctx ctx) {
	exec.execExp(tree,value.ctxEval);
	Object oval = stack.getTopOval();
	long ival = stack.getTopIval();
	stack.pop();
	if (oval instanceof XDOMElement) {
	    XDOMElement elemtree = (XDOMElement) oval;
	    if (ctx == value.ctxAssign) {
		Object oval1 = stack.getTopOval();
		if (oval1 instanceof XDOMElement) {
		    elemtree.setBodyRec(name,(XDOMElement) oval1);
		} else {
		    errors.error(Errors.EXEC,pos,"body record value not a xdom element");
		}
	    } else {
		XDOM x = elemtree.getBodyRec(name);
		stack.push(x,0);
	    }
	} else {
	    if (! value.isError(oval,ival)) {
		errors.error(Errors.EXEC,tree.pos,"not an xdom element");
	    }
	    stack.pushError();
	}
    }

    /*************************************************************************
    *
    *   TreeCall
    *
    *************************************************************************/

    private byte callKindByte(String s,XPos pos) {
	int size = XDOMCall.CKind.length;
	byte i;
	for (i = 0; i < size; i++) {
	    if (s == XDOMCall.CKind[i]) {
		return i;
	    }
	}
	errors.error(Errors.EXEC,pos,"illegal call kind value");
	return -1;
    }

    private void callKindPushString(byte i,XPos pos) {
	int size = XDOMCall.CKind.length;
	if (0 <= i && i < size) {
	    stack.push(XDOMCall.CKind[i],0);
	} else {
	    errors.error(Errors.EXEC,pos,"illegal internal call kind code");
	    stack.pushError();
	}
    }

    public boolean doAttr(XDOMCall xc,String name,Ctx ctx,XPos pos) {
	if (doAttr((XDOM) xc,name,ctx,pos)) return true;
	if (ctx == value.ctxAssign) {
	    if (name == "kind") {
		if (evalue.getS(pos)) {
		    Object oval = stack.getTopOval();
		    long ival = stack.getTopIval();
		    byte code = callKindByte(XDOMValue.getString(oval,ival),pos);
		    if (code >= 0) {
			xc.setKind(code);
		    }
		}
		return true;
	    }
	    if (name == "func") {
		Object oval = stack.getTopOval();
		long ival = stack.getTopIval();
		if (oval instanceof XDOM) {
		    xc.setFunc((XDOM) oval);
		} else if (! value.isError(oval,ival)) {
		    errors.error(Errors.EXEC,pos,"not an xdom");
		}
		return true;
	    }
	    if (name == "size") {
		errors.error(Errors.EXEC,pos,"can't assign to size field");
		return true;
	    }
	} else {
	    if (name == "kind") {
		byte code = xc.getKind();
		callKindPushString(code,pos);
		return true;
	    }
	    if (name == "func") {
		XDOM x = xc.getFunc();
		stack.push(x,0);
		return true;
	    }
	    if (name == "size") {
		stack.push(value.vlInt,xc.argSize());
		return true;
	    }
	}
	return false;
    }

    public void makeTreeCall(XDOMCall ctree) {
	int size = ctree.argSize();
	if (size >= 1) {
	    XDOM arg1 = ctree.getArg(1);
	    exec.execExp(arg1,value.ctxEval);
	    Object oval = stack.getTopOval();
	    long ival = stack.getTopIval();
	    stack.pop();
	    if (oval instanceof XDOM) {
		XDOMCall xc = new XDOMCall((XDOM) oval);
		boolean haveList = false;
		for (int i = 2; i <= size; i++) {
		    XDOM atree = ctree.getArg(i);
		    if (TreeUtil.isEqual(atree)) {
			String aname = TreeUtil.getEqualName(atree);
			XDOM atree1 = TreeUtil.stripEqual(atree);
			if (aname == "list") {
			    if (haveList) {
				errors.error(Errors.EXEC,atree.pos,"duplicate list= attribute");
			    } else {
				haveList = true;
				int start = ps.start();
				pushSeqCall(atree1);
				ps.pop(start,xc,true);
			    }
			} else {
			    exec.execExp(atree1,value.ctxEval);
			    if (! doAttr(xc,aname,value.ctxAssign,atree.pos)) {
				errors.error(Errors.EXEC,atree.pos,"unrecognized attribute:"+aname);
			    }
			    stack.pop();
			}
		    } else {
			errors.error(Errors.EXEC,atree.pos,"missing name =");
		    }
		}
		stack.push(xc,0);
		return;
	    } else if (! value.isError(oval,ival)) {
		errors.error(Errors.EXEC,arg1.pos,"not an xdom");
	    }
	} else {
	    errors.error(Errors.EXEC,ctree.pos,"wrong number of actual parameters");
	}
	stack.pushError();
    }

    public void argSubscript(XDOMCall xc,XDOMCall tree,Ctx ctx) {
	XDOM arg1 = tree.getArg(1);
	int size = xc.argSize();
	boolean ok = true;
	int index = 0;
	if (evalue.getI(arg1)) {
	    index = (int) stack.getTopIval();
	    if (index < 1 || index > size) {
		errors.error(Errors.EXEC,arg1.pos,"subscript out of range");
		ok = false;
	    }
	} else {
	    ok = false;
	}
	stack.pop();
	if (ok) {
	    if (ctx == value.ctxAssign) {
		Object oval = stack.getTopOval();
		if (oval instanceof XDOM) {
		    xc.setArg(index,(XDOM) oval);
		} else {
		    errors.error(Errors.EXEC,tree.pos,"value to be assigned is not an xdom");
		}
	    } else {
		XDOM x = xc.getArg(index);
		stack.push(x,0);
		return;
	    }
	}
	if (ctx != value.ctxAssign) {
	    stack.pushError();
	}
    }

    public void argInsert(XDOMCall xc,XDOMCall tree) {
	int args = tree.argSize();
	if (args != 1 && args != 2) {
	    errors.error(Errors.EXEC,tree.pos,"wront number of actual paramters");
	    stack.pushError();
	    return;
	}
	XDOM arg1 = tree.getArg(1);
	XDOM argv = arg1;
	int size = xc.argSize();
	boolean ok = true;

	int index = -1;
	if (args == 2) {
	    argv = tree.getArg(2);
	    if (evalue.getI(arg1)) {
		index =  (int) stack.getTopIval();
		if (index < -1 || index > size) {
		    errors.error(Errors.EXEC,arg1.pos,"insert index out of range");
		    ok = false;
		}
	    } else {
		ok = false;
	    } 
	    stack.pop();
	}

	XDOM newElem = null;
	exec.execExp(argv,value.ctxEval);
	Object oval2 = stack.getTopOval();
	long ival2 = stack.getTopIval();
	stack.pop();
	if (oval2 instanceof XDOM) {
	    newElem = (XDOM) oval2;
	} else {
	    if (! value.isError(oval2,ival2)) {
		errors.error(Errors.EXEC,argv.pos,"not an xdom");
	    }
	    ok = false;
	}

	if (ok) {
	    xc.insertArg(index,newElem);
	}
	stack.pushNull();
    }

    public void argDelete(XDOMCall xc,XDOMCall tree) {
	XDOM arg1 = tree.getArg(1);
	int size = xc.argSize();
	boolean ok = true;
	int index = 0;
	if (evalue.getI(arg1)) {
	    index = (int) stack.getTopIval();
	    if (index < 1 || index > size) {
		errors.error(Errors.EXEC,arg1.pos,"delete index out of range");
		ok = false;
	    }
	} else {
	    ok = false;
	}
	stack.pop();
	if (ok) {
	    xc.deleteArg(index);
	}
	stack.pushNull();
    }

    public void argAll(XDOMCall xc,XDOMCall tree,Ctx ctx) {
	XDOM arg1 = tree.getArg(1);
	exec.execExp(arg1,value.ctxEval);
	Object oval = stack.getTopOval();
	long ival = stack.getTopIval();
	stack.pop();
	if (oval instanceof VLFunc) {
	    VLFunc vlf = (VLFunc) oval;
	    join.start(ctx);
	    int size = xc.argSize();
	    boolean save = xt.doBreak;
	    xt.doBreak = false;
	    for (int i = 1; i <= size; i++) {
		XDOM x = xc.getArg(i);
		stack.push(x,0);
		callExec.execCallFunc(vlf,0,1,tree,value.ctxEval);
		join.add(x.pos);
		if (xt.doBreak) break;
	    }
	    join.pop();
	    xt.doBreak = save;
	} else {
	    if (! value.isError(oval,ival)) {
		errors.error(Errors.EXEC,arg1.pos,"iterator parameter not a function");
	    }
	    stack.pushError();
	}
    }


    /*************************************************************************
    *
    *   TreeName
    *
    *************************************************************************/

    private byte nameKindByte(String s,XPos pos) {
	int size = XDOMName.NKind.length;
	byte i;
	for (i = 0; i < size; i++) {
	    if (s == XDOMName.NKind[i]) {
		return i;
	    }
	}
	errors.error(Errors.EXEC,pos,"illegal name kind value");
	return -1;
    }

    private void nameKindPushString(byte i,XPos pos) {
	int size = XDOMName.NKind.length;
	if (0 <= i && i < size) {
	    stack.push(XDOMName.NKind[i],0);
	} else {
	    errors.error(Errors.EXEC,pos,"illegal internal name kind code");
	    stack.pushError();
	}
    }

    public boolean doAttr(XDOMName xn,String name,Ctx ctx,XPos pos) {
	if (doAttr((XDOM) xn,name,ctx,pos)) return true;
	if (ctx == value.ctxAssign) {
	    if (name == "val") {
		if (evalue.getS(pos)) {
		    Object oval = stack.getTopOval();
		    long ival = stack.getTopIval();
		    xn.setName(XDOMValue.getString(oval,ival));
		}
		return true;
	    }
	    if (name == "kind") {
		if (evalue.getS(pos)) {
		    Object oval = stack.getTopOval();
		    long ival = stack.getTopIval();
		    byte code = nameKindByte(XDOMValue.getString(oval,ival),pos);
		    if (code >= 0) {
			xn.setKind(code);
		    }
		}
		return true;
	    }
	    if (name == "space") {
		if (evalue.getS(pos)) {
		    Object oval = stack.getTopOval();
		    long ival = stack.getTopIval();
		    xn.setSpace(XDOMValue.getString(oval,ival));
		}
		return true;
	    }
	    if (name == "base") {
		if (evalue.getS(pos)) {
		    Object oval = stack.getTopOval();
		    long ival = stack.getTopIval();
		    xn.setBase(XDOMValue.getString(oval,ival));
		}
		return true;
	    }
	    if (name == "hasExt") {
		if (evalue.getB(pos)) {
		    long ival = stack.getTopIval();
		    boolean val = false;
		    if (ival == 1) {
			if (! xn.hasExt()) {
			    xn.clearExt(0);
			}
		    } else {
			if (xn.hasExt()) {
			    xn.clearExt();
			}
		    }
		}
		return true;
	    }
	    if (name == "size") {
		errors.error(Errors.EXEC,pos,"can't assign to size field");
		return true;
	    }
	} else {
	    if (name == "val") {
		String s = xn.getName();
		stack.push(s,0);
		return true;
	    }
	    if (name == "kind") {
		byte code = xn.getKind();
		nameKindPushString(code,pos);
		return true;
	    }
	    if (name == "space") {
		String s = xn.getSpace();
		stack.push(s,0);
		return true;
	    }
	    if (name == "base") {
		String s = xn.getBase();
		stack.push(s,0);
		return true;
	    }
	    if (name == "hasExt") {
		boolean b = xn.hasExt();
		long ival = 0;
		if (b) ival = 1;
		stack.push(value.vlBool,ival);
		return true;
	    }
	    if (name == "size") {
		stack.push(value.vlInt,xn.extSize());
		return true;
	    }
	}
	return false;
    }

    public void makeTreeName(XDOMCall ctree) {
	int size = ctree.argSize();
	if (size >= 1) {
	    XDOM arg1 = ctree.getArg(1);
	    if (evalue.getS(arg1)) {
		Object oval = stack.getTopOval();
		long ival = stack.getTopIval();
		stack.pop();
		String s = XDOMValue.getString(oval,ival);
		XDOMName xn = new XDOMName(s);
		boolean haveList = false;
		for (int i = 2; i <= size; i++) {
		    XDOM atree = ctree.getArg(i);
		    if (TreeUtil.isEqual(atree)) {
			String aname = TreeUtil.getEqualName(atree);
			XDOM atree1 = TreeUtil.stripEqual(atree);
			if (aname == "list") {
			    if (haveList) {
				errors.error(Errors.EXEC,atree.pos,"duplicate list= attribute");
			    } else {
				haveList = true;
				int start = ps.start();
				pushSeqCall(atree1);
				ps.pop(start,xn,true);
			    }
			} else {
			    exec.execExp(atree1,value.ctxEval);
			    if (! doAttr(xn,aname,value.ctxAssign,atree.pos)) {
				errors.error(Errors.EXEC,atree.pos,"unrecognized attribute:"+aname);
			    }
			    stack.pop();
			}
		    } else {
			errors.error(Errors.EXEC,atree.pos,"missing name =");
		    }
		}
		stack.push(xn,0);
		return;
	    }
	    stack.pop();
	} else {
	    errors.error(Errors.EXEC,ctree.pos,"wrong number of actual parameters");
	}
	stack.pushError();
    }

    public void extSubscript(XDOMName xn,XDOMCall tree,Ctx ctx) {
	XDOM arg1 = tree.getArg(1);
	int size = xn.extSize();
	boolean ok = true;
	int index = 0;
	if (evalue.getI(arg1)) {
	    index = (int) stack.getTopIval();
	    if (index < 1 || index > size) {
		errors.error(Errors.EXEC,arg1.pos,"subscript out of range");
		ok = false;
	    }
	} else {
	    ok = false;
	}
	stack.pop();
	if (ok) {
	    if (ctx == value.ctxAssign) {
		Object oval = stack.getTopOval();
		if (oval instanceof XDOM) {
		    xn.setExt(index,(XDOM) oval);
		} else {
		    errors.error(Errors.EXEC,tree.pos,"value to be assigned is not an xdom");
		}
	    } else {
		XDOM x = xn.getExt(index);
		stack.push(x,0);
		return;
	    }
	}
	if (ctx != value.ctxAssign) {
	    stack.pushError();
	}
    }

    public void extInsert(XDOMName xn,XDOMCall tree) {
	int args = tree.argSize();
	if (args != 1 && args != 2) {
	    errors.error(Errors.EXEC,tree.pos,"wrong number of actual parameters");
	    stack.pushError();
	    return;
	}
	XDOM arg1 = tree.getArg(1);
	XDOM argv = arg1;
	int size = xn.extSize();
	boolean ok = true;

	int index = -1;
	if (args == 2) {
	    argv = tree.getArg(2);
	    if (evalue.getI(arg1)) {
		index =  (int) stack.getTopIval();
		if (index < -1 || index > size) {
		    errors.error(Errors.EXEC,arg1.pos,"insert index out of range");
		    ok = false;
		}
	    } else {
		ok = false;
	    } 
	    stack.pop();
	}

	XDOM newElem = null;
	exec.execExp(argv,value.ctxEval);
	Object oval2 = stack.getTopOval();
	long ival2 = stack.getTopIval();
	stack.pop();
	if (oval2 instanceof XDOM) {
	    newElem = (XDOM) oval2;
	} else {
	    if (! value.isError(oval2,ival2)) {
		errors.error(Errors.EXEC,argv.pos,"not an xdom");
	    }
	    ok = false;
	}

	if (ok) {
	    xn.insertExt(index,newElem);
	}
	stack.pushNull();
    }

    public void extDelete(XDOMName xn,XDOMCall tree) {
	XDOM arg1 = tree.getArg(1);
	int size = xn.extSize();
	boolean ok = true;
	int index = 0;
	if (evalue.getI(arg1)) {
	    index = (int) stack.getTopIval();
	    if (index < 1 || index > size) {
		errors.error(Errors.EXEC,arg1.pos,"delete index out of range");
		ok = false;
	    }
	} else {
	    ok = false;
	}
	stack.pop();
	if (ok) {
	    xn.deleteExt(index);
	}
	stack.pushNull();
    }

    public void extAll(XDOMName xn,XDOMCall tree,Ctx ctx) {
	XDOM arg1 = tree.getArg(1);
	exec.execExp(arg1,value.ctxEval);
	Object oval = stack.getTopOval();
	long ival = stack.getTopIval();
	stack.pop();
	if (oval instanceof VLFunc) {
	    VLFunc vlf = (VLFunc) oval;
	    join.start(ctx);
	    int size = xn.extSize();
	    boolean save = xt.doBreak;
	    xt.doBreak = false;
	    for (int i = 1; i <= size; i++) {
		XDOM x = xn.getExt(i);
		stack.push(x,0);
		callExec.execCallFunc(vlf,0,1,tree,value.ctxEval);
		join.add(x.pos);
		if (xt.doBreak) break;
	    }
	    join.pop();
	    xt.doBreak = save;
	} else {
	    if (! value.isError(oval,ival)) {
		errors.error(Errors.EXEC,arg1.pos,"iterator parameter not a function");
	    }
	    stack.pushError();
	}
    }


    /*************************************************************************
    *
    *   TreeString
    *
    *************************************************************************/

    private byte stringKindByte(String s,XPos pos) {
	int size = XDOMString.SKind.length;
	byte i;
	for (i = 0; i < size; i++) {
	    if (s == XDOMString.SKind[i]) {
		return i;
	    }
	}
	errors.error(Errors.EXEC,pos,"illegal string kind value");
	return -1;
    }

    private void stringKindPushString(byte i,XPos pos) {
	int size = XDOMString.SKind.length;
	if (0 <= i && i < size) {
	    stack.push(XDOMString.SKind[i],0);
	} else {
	    errors.error(Errors.EXEC,pos,"illegal internal string kind code");
	    stack.pushError();
	}
    }

    public boolean doAttr(XDOMString xs,String name,Ctx ctx,XPos pos) {
	if (doAttr((XDOM) xs,name,ctx,pos)) return true;
	if (ctx == value.ctxAssign) {
	    if (name == "val") {
		if (evalue.getS(pos)) {
		    Object oval = stack.getTopOval();
		    long ival = stack.getTopIval();
		    xs.setVal(oval,ival);
		}
		return true;
	    }
	    if (name == "kind") {
		if (evalue.getS(pos)) {
		    Object oval = stack.getTopOval();
		    long ival = stack.getTopIval();
		    byte code = stringKindByte(XDOMValue.getString(oval,ival),pos);
		    if (code >= 0) {
			xs.setKind(code);
		    }
		}
		return true;
	    }
	} else {
	    if (name == "val") {
		stack.push(xs.getOval(),xs.getIval());
		return true;
	    }
	    if (name == "kind") {
		byte code = xs.getKind();
		stringKindPushString(code,pos);
		return true;
	    }
	}
	return false;
    }

    public void makeTreeString(XDOMCall ctree) {
	int size = ctree.argSize();
	if (size >= 1) {
	    XDOM arg1 = ctree.getArg(1);
	    if (evalue.getS(arg1)) {
		Object oval = stack.getTopOval();
		long ival = stack.getTopIval();
		stack.pop();
		XDOMString stree = new XDOMString(oval,ival);
		for (int i = 2; i <= size; i++) {
		    XDOM atree = ctree.getArg(i);
		    if (TreeUtil.isEqual(atree)) {
			String aname = TreeUtil.getEqualName(atree);
			XDOM atree1 = TreeUtil.stripEqual(atree);
			exec.execExp(atree1,value.ctxEval);
			if (! doAttr(stree,aname,value.ctxAssign,atree.pos)) {
			    errors.error(Errors.EXEC,atree.pos,"unrecognized attribute:"+aname);
			}
			stack.pop();
		    } else {
			errors.error(Errors.EXEC,atree.pos,"missing name =");
		    }
		}
		stack.push(stree,0);
		return;
	    }
	    stack.pop();
	} else {
	    errors.error(Errors.EXEC,ctree.pos,"wrong number of actual parameters");
	}
	stack.pushError();
    }

    /*************************************************************************
    *
    *   TreeInt
    *
    *************************************************************************/

    public boolean doAttr(XDOMInt xn,String name,Ctx ctx,XPos pos) {
	if (doAttr((XDOM) xn,name,ctx,pos)) return true;
	if (ctx == value.ctxAssign) {
	    if (name == "val") {
		Object oval = stack.getTopOval();
		long ival = stack.getTopIval();
		if (oval == value.vlInt) {
		    xn.setVal(ival);
//		} else if (oval == value.vlFloat) {
//		    xn.setVal(ival);
//		    xn.setIsInt(false);
//		} else if (evalue.getS(pos)) {
//		    oval = stack.getTopOval();
//		    ival = stack.getTopIval();
//		    String s = XDOMValue.getString(oval,ival);
//		    if (XDOMNumber.isNumber(s)) {
//			xn.setVal(s);
//		    } else {
//			errors.error(Errors.EXEC,pos,"not a number");
//		    }
		} else {
		    errors.error(Errors.EXEC,pos,"not an int");
		}
		return true;
	    }
	} else {
	    if (name == "val") {
		stack.push(value.vlInt,xn.getInt());
		return true;
	    }
	}
	return false;
    }

    public void makeTreeInt(XDOMCall ctree) {
	int size = ctree.argSize();
	if (size >= 1) {
	    XDOM arg1 = ctree.getArg(1);
	    exec.execExp(arg1,value.ctxEval);
	    Object oval = stack.getTopOval();
	    long ival = stack.getTopIval();
	    stack.pop();
	    XDOMInt ntree = null;
	    if (oval instanceof VLInt) {
		ntree = new XDOMInt(ival);
//	    } else if (oval instanceof VLFloat) {
//		ntree = new XDOMNumber(ival);
//		ntree.setIsInt(false);
//	    } else if (XDOMValue.isString(oval,ival)) {
//		String s = XDOMValue.getString(oval,ival);
//		if (XDOMNumber.isNumber(s)) {
//		    ntree = new XDOMNumber(s);
//		}
	    }
	    if (ntree != null) {
		for (int i = 2; i <= size; i++) {
		    XDOM atree = ctree.getArg(i);
		    if (TreeUtil.isEqual(atree)) {
			String aname = TreeUtil.getEqualName(atree);
			XDOM atree1 = TreeUtil.stripEqual(atree);
			exec.execExp(atree1,value.ctxEval);
			if (! doAttr(ntree,aname,value.ctxAssign,atree.pos)) {
			    errors.error(Errors.EXEC,atree.pos,"unrecognized attribute:"+aname);
			}
			stack.pop();
		    } else {
			errors.error(Errors.EXEC,atree.pos,"missing name =");
		    }
		}
		stack.push(ntree,0);
		return;
	    }
	    if (! value.isError(oval,ival)) {
		errors.error(Errors.EXEC,arg1.pos,"not an int");
	    }
	} else {
	    errors.error(Errors.EXEC,ctree.pos,"wrong number of actual parameters");
	}
	stack.pushError();
    }

    /*************************************************************************
    *
    *   TreeFloat
    *
    *************************************************************************/

    public boolean doAttr(XDOMFloat xn,String name,Ctx ctx,XPos pos) {
	if (doAttr((XDOM) xn,name,ctx,pos)) return true;
	if (ctx == value.ctxAssign) {
	    if (name == "val") {
		Object oval = stack.getTopOval();
		long ival = stack.getTopIval();
		if (oval == value.vlInt) {
		    xn.setVal(ival);
		} else if (oval == value.vlFloat) {
		    xn.setVal(ival);
//		    xn.setIsInt(false);
//		} else if (evalue.getS(pos)) {
//		    oval = stack.getTopOval();
//		    ival = stack.getTopIval();
//		    String s = XDOMValue.getString(oval,ival);
//		    if (XDOMNumber.isNumber(s)) {
//			xn.setVal(s);
//		    } else {
//			errors.error(Errors.EXEC,pos,"not a number");
//		    }
		} else {
		    errors.error(Errors.EXEC,pos,"not a float or int");
		}
		return true;
	    }
	} else {
	    if (name == "val") {
		double fval = xn.getFloat();
		stack.push(value.vlFloat,Double.doubleToLongBits(fval));
		return true;
	    }
	}
	return false;
    }

    public void makeTreeFloat(XDOMCall ctree) {
	int size = ctree.argSize();
	if (size >= 1) {
	    XDOM arg1 = ctree.getArg(1);
	    exec.execExp(arg1,value.ctxEval);
	    Object oval = stack.getTopOval();
	    long ival = stack.getTopIval();
	    stack.pop();
	    XDOMFloat ntree = null;
	    if (oval instanceof VLInt) {
		ntree = new XDOMFloat(ival);
	    } else if (oval instanceof VLFloat) {
		ntree = new XDOMFloat(Double.longBitsToDouble(ival));
//		ntree.setIsInt(false);
//	    } else if (XDOMValue.isString(oval,ival)) {
//		String s = XDOMValue.getString(oval,ival);
//		if (XDOMNumber.isNumber(s)) {
//		    ntree = new XDOMNumber(s);
//		}
	    }
	    if (ntree != null) {
		for (int i = 2; i <= size; i++) {
		    XDOM atree = ctree.getArg(i);
		    if (TreeUtil.isEqual(atree)) {
			String aname = TreeUtil.getEqualName(atree);
			XDOM atree1 = TreeUtil.stripEqual(atree);
			exec.execExp(atree1,value.ctxEval);
			if (! doAttr(ntree,aname,value.ctxAssign,atree.pos)) {
			    errors.error(Errors.EXEC,atree.pos,"unrecognized attribute:"+aname);
			}
			stack.pop();
		    } else {
			errors.error(Errors.EXEC,atree.pos,"missing name =");
		    }
		}
		stack.push(ntree,0);
		return;
	    }
	    if (! value.isError(oval,ival)) {
		errors.error(Errors.EXEC,arg1.pos,"not a float or int");
	    }
	} else {
	    errors.error(Errors.EXEC,ctree.pos,"wrong number of actual parameters");
	}
	stack.pushError();
    }

    /*************************************************************************
    *
    *   TreeValue
    *
    *************************************************************************/

    public boolean doAttr(XDOMValue xv,String name,Ctx ctx,XPos pos) {
	if (doAttr((XDOM) xv,name,ctx,pos)) return true;
	if (ctx == value.ctxAssign) {
	    if (name == "val") {
		Object oval = stack.getTopOval();
		long ival = stack.getTopIval();
		xv.setVal(oval,ival);
		return true;
	    }
	} else {
	    if (name == "val") {
		Object oval = xv.getOval();
		long ival = xv.getIval();
		stack.push(oval,ival);
		return true;
	    }
	}
	return false;
    }

    public void makeTreeValue(XDOMCall ctree) {
	int size = ctree.argSize();
	if (size >= 1) {
	    XDOM arg1 = ctree.getArg(1);
	    exec.execExp(arg1,value.ctxEval);
	    Object oval = stack.getTopOval();
	    long ival = stack.getTopIval();
	    stack.pop();
	    XDOMValue vtree = new XDOMValue(oval,ival);
	    for (int i = 2; i <= size; i++) {
		XDOM atree = ctree.getArg(i);
		if (TreeUtil.isEqual(atree)) {
		    String aname = TreeUtil.getEqualName(atree);
		    XDOM atree1 = TreeUtil.stripEqual(atree);
		    exec.execExp(atree1,value.ctxEval);
		    if (! doAttr(vtree,aname,value.ctxAssign,atree.pos)) {
			errors.error(Errors.EXEC,atree.pos,"unrecognized attribute:"+aname);
		    }
		    stack.pop();
		} else {
		    errors.error(Errors.EXEC,atree.pos,"missing name =");
		}
	    }
	    stack.push(vtree,0);
	} else {
	    errors.error(Errors.EXEC,ctree.pos,"wrong number of actual parameters");
	    stack.pushError();
	}
    }
}