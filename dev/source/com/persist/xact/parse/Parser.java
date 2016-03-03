/*******************************************************************************
*
* Copyright (c) 2002-2007. John R. Nestor. All rights reserved.
*
* See the file XLICENSE.txt for use and redistribution information.
*
*******************************************************************************/

package com.persist.xact.parse;

import com.persist.xact.system.*;
import com.persist.xdom.*;
import com.persist.xact.exec.*;
import com.persist.xact.value.*;
import com.persist.xact.bind.*;

/**
<p>
This is the XACT parser.
</p>
 <ul>
    <li> <b>Overall Initialization.</b>
    The parser is is initalized by calling 
    {@link #init}.
    </li>
    <li><b>Parsing.</b>
    The {@link #parse} method is passed an input
    stream of characters to parse and returns
    the result {@link XDOM} parse tree.
    </li>
</ul>
<p>
 The parser calls the {@link Lexer} class to preform
 lexical analysis.
</p>
**/
public final class Parser {
    /**
     ** The thread in which the parser is running.
     */
    private XThread xt;
    /**
     ** The handler for parse errors.
     */
    private Errors errors;
    /**
     ** The underlying lexical analyzer.
     */
    private Lexer lex;
    /**
     ** The parse stack.
     */
    private ParseStack ps;
    /**
     ** This is used to call parse action routines.
     */
    private CallExec callExec;
    /**
     ** Standard values.
     */
    private Value value;
    /**
     ** Execution stack.
     ** Used to pass parameters and results
     ** for action routines.
     */
    private VariableStack stack;
    /**
     ** An XACT caller for any called actions.
     */
    private XDOM caller;
    /**
     ** True if a ; separator (or equivalent)
     ** is present imediately prior to current
     ** parse position.
     */
    private boolean hasSep;
    /**
     ** True if positions are to be included in the
     ** result tree.
     */
    private boolean includePos;
    /**
     ** The file part of position information for the
     ** result tree and for error messages.
     */
    private FPosition fpos;
    /**
     ** True if parsing is incremental.
     ** This occurs when an action handler is specified.
     */
    private boolean incr;

    /**
     ** The startElement action routine (or null).
     */
    private XDef startElement;
    /**
     ** The startBody action routine (or null).
     */
    private XDef startBody;
    /**
     ** The endElement action routine (or null).
     */
    private XDef endElement;
    /**
     ** The endElement action routine (or null).
     */
    private XDef endAttr;
    /**
     ** The endChild action routine (or null).
     */
    private XDef endChild;

    /**
    **	The parse action handler (or null is none is
    ** specified). This is specified in the handler parameter of
    ** the XACT Parse function.
    */
    private VLObj vlo;

    /**
     ** True during the time {@link #parse} is being called.
     ** This is used to detect recursion where
     ** a call to XACT function Parse call an action routine
     ** that itself calls Parse.
     ** When this occurs a second instance of the Parse
     ** class is created.
     */
    public boolean inUse = false;

    /**
     ** Creates a new parser for a specified thread.
     ** @param xt the thread.
     */
    public Parser(XThread xt) {
	this.xt = xt;
	lex = new Lexer(xt);
    }

    /**
     ** Initializes the parser.
     ** Must be called before any other methods.
     */
    public void init() {
	errors = xt.errors;
	ps = xt.ps;
	callExec = xt.exec.callExec;
	value = xt.cmd.value;
	stack = xt.exec.stack;
	lex.init();
    }

    /**
     ** Parses a string.
     ** @return the parse tree for that string.
     */
    private XDOM parseString() {
	XDOM result;
	int firstLine = lex.pos.firstLine;
	int firstChar = lex.pos.firstChar;
	int lastLine = lex.pos.lastLine;
	int lastChar = lex.pos.lastChar;
	if (lex.kind == Lexer.STRING) {
	    XDOMString stree = new XDOMString(lex.val,0);
	    if (lex.singleQuote) {
		stree.setKind(XDOMString.SSINGLE);
	    }
	    stree.setSpaceBefore(lex.spaceBefore);
	    if (includePos) {
		stree.pos = lex.pos.copy();
	    }
	    result = stree;
	} else {
	    XDOMName ntree = new XDOMName(lex.val);
	    ntree.setSpaceBefore(lex.spaceBefore);
	    if (includePos) {
		ntree.pos = lex.pos.copy();
	    }
	    result = ntree;
	}
	if (lex.more || lex.kind == Lexer.SNAME) {
	    byte before = result.getSpaceBefore();
	    result.setSpaceBefore(XDOM.EMPTY);
	    boolean singleQuote = lex.singleQuote;
	    int start = ps.start();
	    ps.push(result);
	    XDOMName ntree = new XDOMName("Cat");
	    if (includePos) {
	     /* The position of the initial " */
		ntree.pos = new XPos(lex.pos.firstLine,lex.pos.firstChar,firstLine,lex.pos.firstChar,fpos);
	    }
	    while (lex.more) {
		lex.getToken();
		lastLine = lex.pos.lastLine;
		lastChar = lex.pos.lastChar;
		if (lex.kind == Lexer.STRING) {
		    XDOMString stree = new XDOMString(lex.val,0);
		    if (includePos) {
			stree.pos = lex.pos.copy();
		    }
		    ps.push(stree);
		} else {
		    XDOMName ntree1 = new XDOMName(lex.val);
		    if (includePos) {
			ntree1.pos = lex.pos.copy();
		    }
		    lastLine = lex.pos.lastLine;
		    lastChar = lex.pos.lastChar;
		    ps.push(ntree1);
		}
	    }
	    XDOMCall ctree = new XDOMCall(ntree);
	    ctree.setSpaceBefore(before);
	    if (singleQuote) {
		ctree.setKind(XDOMCall.CSINGLE);
	    } else {
		ctree.setKind(XDOMCall.CDOUBLE);
	    }
	    ps.pop(start,ctree,true);
	    if (includePos) {
		ctree.pos = new XPos(firstLine,firstChar,lex.pos.lastLine,lex.pos.lastChar,fpos);
	    }
	    result = ctree;
	}
	lex.getToken();
	if (includePos) {
	    result.pos = new XPos(firstLine,firstChar,lastLine,lastChar,fpos);
	}
	result.setSpaceAfter(lex.spaceBefore);
	return result;
    }

    /**
     ** Parses a name extension.
     ** @param name the name before the exrension.
     ** @param firstLine the line where the name starts.
     ** @param firstChar the character position when the name starts.
     ** @return the parse tree for the name.
     */
    private XDOMName parseExt(String name,int firstLine,int firstChar) {
	int lastLine = firstLine;
	int lastChar = firstChar;
	int start = ps.start();
	byte before = lex.spaceBefore;
	int openLine = lex.pos.firstLine;
	int openChar = lex.pos.firstChar;
	lex.getToken();
	if (lex.kind != Lexer.RSQUARE) {
	    while (true) {
		XDOM tree1 = parseExp(Prec.topPrec);
		if (tree1.pos != null) {
		    lastLine = tree1.pos.lastLine;
		    lastChar = tree1.pos.lastChar;
		}
		ps.push(tree1);
		if (lex.kind == Lexer.RSQUARE) break;
		if (lex.kind == Lexer.EOF) break;
		if (lex.kind == Lexer.COMMA) {
		    lex.getToken();
		} else {
		    XPos pos = new XPos(openLine,openChar,lastLine,lastChar,fpos);
		    errors.error(Errors.PARSE,pos,"no closing ]");
		}
	    }
	}
	if (lex.kind == Lexer.RSQUARE) {
	    lastLine = lex.pos.lastLine;
	    lastChar = lex.pos.lastChar;
	    lex.getToken();
	} else {
	    errors.error(Errors.PARSE,lex.pos,"missing ]");
	}
	XDOMName tree = new XDOMName(name);
	tree.setSpaceBefore(before);
	tree.setSpaceAfter(lex.spaceBefore);
	ps.pop(start,tree,true);
	if (includePos) {
	    tree.pos = new XPos(firstLine,firstChar,lastLine,lastChar,fpos);
	}
	return tree;
    }

    /**
     ** Parses a primary expression.
     ** @return its parse tree.
     */
    private XDOM parseExp1() {
	XDOM result = null;
	int firstLine = lex.pos.firstLine;
	int firstChar = lex.pos.firstChar;
	int lastLine = lex.pos.lastLine;
	int lastChar = lex.pos.lastChar;
	if (lex.kind == Lexer.NAME) {
	    String name = lex.val;
	    byte before = lex.spaceBefore;
	    lex.getToken();
	    if (lex.kind != Lexer.LSQUARE) {
		XDOMName tree = new XDOMName(name);
		tree.setSpaceBefore(before);
		tree.setSpaceAfter(lex.spaceBefore);
		result = tree;
		if (includePos) {
		    result.pos = new XPos(firstLine,firstChar,lastLine,lastChar,fpos);
		}
	    } else {
		XDOMName tree = parseExt(name,firstLine,firstChar);
		tree.setSpaceBefore(before);
		result = tree;
	    }
	    return result;
	} else if (lex.kind == Lexer.NUMBER) {
	    if (XDOMInt.isInt(lex.val)) {
		XDOMInt tree = new XDOMInt(lex.val);
		tree.setSpaceBefore(lex.spaceBefore);
		if (includePos) {
		    tree.pos = lex.pos.copy();
		}
		result = tree;
		lex.getToken();
		result.setSpaceAfter(lex.spaceBefore);
		return result;
	    } else if (XDOMFloat.isFloat(lex.val)) {
		XDOMFloat tree = new XDOMFloat(lex.val);
		tree.setSpaceBefore(lex.spaceBefore);
		if (includePos) {
		    tree.pos = lex.pos.copy();
		}
		result = tree;
		lex.getToken();
		result.setSpaceAfter(lex.spaceBefore);
		return result;
	    } else {
		errors.error(Errors.INTERNAL,lex.pos,"bad lexical number");
		lex.getToken();
	    }
	} else if (lex.kind == Lexer.STRING || lex.kind == Lexer.SNAME) {
	    result = parseString();
	    return result;
	} else if (lex.kind == Lexer.LPAREN) {
	    byte before = lex.spaceBefore;
	    lex.getToken();
	    result = parseExp(Prec.topPrec);
	    result.setSpaceBefore(before);
	    if (lex.kind == Lexer.RPAREN) {
		lex.getToken();
		result.setSpaceAfter(lex.spaceBefore);
	    } else {
		errors.error(Errors.PARSE,lex.pos,"missing )");
	    }
	    return result;
	} else if (lex.kind == Lexer.LSQUARE) {
	    int start = ps.start();
	    byte before = lex.spaceBefore;
	    int openLine = lex.pos.firstLine;
	    int openChar = lex.pos.firstChar;
	    lex.getToken();
	    if (lex.kind != Lexer.RSQUARE) {
		while (true) {
		    XDOM tree1 = parseExp(Prec.topPrec);
		    if (tree1.pos != null) {
			lastLine = tree1.pos.lastLine;
			lastChar = tree1.pos.lastChar;
		    }
		    ps.push(tree1);
		    if (lex.kind == Lexer.RSQUARE) break;
		    if (lex.kind == Lexer.EOF) break;
		    if (lex.kind == Lexer.COMMA) {
			lex.getToken();
		    } else {
			XPos pos = new XPos(openLine,openChar,lastLine,lastChar,fpos);
			errors.error(Errors.PARSE,pos,"no closing ]");
		    }
		}
	    }
	    if (lex.kind == Lexer.RSQUARE) {
		lastLine = lex.pos.lastLine;
		lastChar = lex.pos.lastChar;
		lex.getToken();
	    } else {
		errors.error(Errors.PARSE,lex.pos,"missing ]");
	    }
	    XDOMName listName = new XDOMName("list");
	    XDOMCall tree = new XDOMCall(listName);
	    tree.setKind(XDOMCall.CLIST);
	    tree.setSpaceBefore(before);
	    tree.setSpaceAfter(lex.spaceBefore);
	    ps.pop(start,tree,true);
	    if (includePos) {
		tree.pos = new XPos(firstLine,firstChar,lastLine,lastChar,fpos);
		listName.pos = tree.pos;
	    }
	    return tree;
	} else {
	    errors.error(Errors.PARSE,lex.pos,"missing name or number");
	    lex.getToken();
	}
	XDOMName badtree = new XDOMName("UNKNOWN");
	result = badtree;
	if (includePos) {
	    result.pos = new XPos(firstLine,firstChar,lastLine,lastChar,fpos);
	}
	return result;
    }

    /**
     ** Parses an expression stopping before any operator
     ** with a precedence <= prec.
     ** @param prec the precedence of the preceeding operator
     ** @return the parse tree for the expression.
     */
    private XDOM parseExp(int prec) {
	XDOM result = null;
	int firstLine = lex.pos.firstLine;
	int firstChar = lex.pos.firstChar;
	int lastLine = lex.pos.lastLine;
	int lastChar = lex.pos.lastChar;
	byte before = lex.spaceBefore;
	if (lex.kind == Lexer.ONAME) {
	    String name = lex.val;
	    if (name == "Tilde") {
		name = "Assert";
	    } else if (name == "Sub") {
		name = "Minus";
	    }
	    int opprec = Prec.topPrec;
	    int idx = Prec.findName(name);
	    if (idx >= 0) {
		opprec = Prec.pr[idx];
	    }
	    if (Prec.infix[idx]) {
		errors.error(Errors.PARSE,lex.pos,"illegal prefix op");
	    }
	    XPos npos = null;
	    XDOM tree1;
	    XDOM func = null;
	    int start = ps.start();
	    if (includePos) {
		npos = lex.pos.copy();
	    }
	    lex.getToken();
	    if (lex.kind != Lexer.HASH) {
		XDOMName ntree = new XDOMName(name);
		ntree.pos = npos;
		func = ntree;
	    } else {
		name = ("sys:" + name).intern();
		XDOMString stree = new XDOMString(name,0);
		XDOMName ntree = new XDOMName("Dot");
		XDOMCall tree2 = new XDOMCall(ntree,2);
		stree.pos = npos;
		if (includePos) {
		    ntree.pos = lex.pos.copy();
		}
		tree2.setKind(XDOMCall.CDOT);
		lex.getToken();
		tree2.insertArg(-1,parseExp1());
		tree2.insertArg(-1,stree);
		if (includePos) {
		    tree2.pos = new XPos(npos.firstLine,npos.firstChar,lastLine,lastChar,fpos);
		}
		func = tree2;
	    }
	    tree1 = parseExp(opprec);
	    if (includePos) {
		lastLine = tree1.pos.lastLine;
		lastChar = tree1.pos.lastChar;
	    }
	    boolean done = false;
	    if (name == "Minus") {
		if (tree1 instanceof XDOMInt) {
		    /* constant fold - int */
		    XDOMInt xn = (XDOMInt) tree1;
		    tree1.setSpaceBefore(before);
		    long val = xn.getInt();
		    xn.setVal(-val);
		    done = true;
		    result = tree1;
		} else if (tree1 instanceof XDOMFloat) {
		    /* constant fold - int */
		    XDOMFloat xn = (XDOMFloat) tree1;
		    tree1.setSpaceBefore(before);
		    double val = xn.getFloat();
		    xn.setVal(-val);
		    done = true;
		    result = tree1;
		}
	    }
	    if (! done) {
		ps.push(tree1);
		XDOMCall tree = new XDOMCall(func);
		tree.setSpaceBefore(before);
		tree.setSpaceAfter(lex.spaceBefore);
		tree.setKind(XDOMCall.COP);
		ps.pop(start,tree,true);
		result = tree;
	    }
	    if (includePos) {
		result.pos = new XPos(firstLine,firstChar,lastLine,lastChar,fpos);
	    }
	} else {
	    result = parseExp1();
	}
	while (true) {
	    if (lex.kind == Lexer.LPAREN) {
		int start = ps.start();
		int openLine = lex.pos.firstLine;
		int openChar = lex.pos.firstChar;
		lex.getToken();
		if (lex.kind != Lexer.RPAREN) {
		    while (true) {
			XDOM tree1 = parseExp(Prec.topPrec);
			lastLine = tree1.pos.lastLine;
			lastChar = tree1.pos.lastChar;
			ps.push(tree1);
			if (lex.kind == Lexer.RPAREN) break;
			if (lex.kind == Lexer.EOF) break;
			if (lex.kind == Lexer.COMMA) {
			    lex.getToken();
			} else {
			    XPos pos = new XPos(openLine,openChar,lastLine,lastChar,fpos);
			    errors.error(Errors.PARSE,pos,"no closing )");
			}
		    }
		}
		if (lex.kind == Lexer.RPAREN) {
		    lastLine = lex.pos.lastLine;
		    lastChar = lex.pos.lastChar;
		    lex.getToken();
		} else {
		    errors.error(Errors.PARSE,lex.pos,"missing )");
		}
		XDOMCall tree = new XDOMCall(result);
		tree.setSpaceBefore(before);
		tree.setSpaceAfter(lex.spaceBefore);
		ps.pop(start,tree,true);
		result = tree;
		if (lex.kind == Lexer.HASH) {
		    XDOMName ntree = new XDOMName("Dot");
		    XDOMString stree = new XDOMString("sys:Subscript",0);
		    XDOM view;
		    if (includePos) {
			ntree.pos = lex.pos.copy();
			stree.pos = lex.pos.copy();
		    }
		    lex.getToken();
		    view = parseExp1();
		    tree.setSpaceAfter(lex.spaceBefore);
		    XDOMCall tree1 = new XDOMCall(ntree,3);
		    tree.setKind(XDOMCall.CSUBSCRIPT);
		    tree1.setKind(XDOMCall.COP);
		    if (includePos) {
			lastLine = view.pos.lastLine;
			lastChar = view.pos.lastChar;
			tree1.pos = new XPos(firstLine,firstChar,lastLine,lastChar,fpos);
		    }
		    tree1.insertArg(-1,tree.getFunc());
		    tree1.insertArg(-1,stree);
		    tree1.insertArg(-1,view);
		    tree.setFunc(tree1);
		}
	    } else if (lex.kind == Lexer.DOT) {
		XDOMString stree = new XDOMString("",0);
		int start = ps.start();
		XDOM func = null;
		lastLine = lex.pos.lastLine;
		lastChar = lex.pos.lastChar;
		ps.push(result);
		lex.getToken();
		if (lex.kind == Lexer.NAME) {
		    stree.setVal(lex.val,0);
		    if (includePos) {
			stree.pos = lex.pos.copy();
		    }
		    lastLine = lex.pos.lastLine;
		    lastChar = lex.pos.lastChar;
		    lex.getToken();
		} else {
		    errors.error(Errors.PARSE,lex.pos,"missing name");
		    stree.setVal("?",0);
		}
		if (lex.kind != Lexer.LSQUARE) {
		    XDOMName ntree = new XDOMName("Dot");
		    if (includePos) {
			ntree.pos = new XPos(firstLine,firstChar,lastLine,lastChar,fpos);
		    }
		    func = ntree;
		} else {
		    XDOMName entree = parseExt("Dot",firstLine,firstChar);
		    func = entree;
		    if (includePos) {
			lastLine = entree.pos.lastLine;
			lastChar = entree.pos.lastChar;
		    }
		}
		ps.push(stree);
		if (lex.kind == Lexer.HASH) {
		    XDOM tree1;
		    lex.getToken();
		    tree1 = parseExp1();
		    ps.push(tree1);
		    lastLine = tree1.pos.lastLine;
		    lastChar = tree1.pos.lastChar;
		}
		XDOMCall tree = new XDOMCall(func);
		tree.setSpaceBefore(before);
		tree.setSpaceAfter(lex.spaceBefore);
		tree.setKind(XDOMCall.CDOT);
		ps.pop(start,tree,true);
		result = tree;
	    } else if (lex.kind == Lexer.ONAME) {
		int opprec = Prec.topPrec;
		int idx = Prec.findName(lex.val);
		if (idx >= 0) {
		    opprec = Prec.pr[idx];
		}
		if (! Prec.infix[idx]) {
		    errors.error(Errors.PARSE,lex.pos,"illegal infix op");
		}
		XPos npos = null;
		if (includePos) {
		    npos = lex.pos.copy();
		}

		if (opprec <= prec) break;
		{
		    XDOM func = null;
		    XDOM tree1;
		    String name = lex.val;
		    int start = ps.start();
		    ps.push(result);
		    lex.getToken();
		    if (lex.kind != Lexer.HASH) {
			XDOMName ntree = new XDOMName(name);
			ntree.pos = npos;
			func = ntree;
		    } else {
			name = ("sys:" + name).intern();
			XDOMString stree = new XDOMString(name,0);
			XDOMName ntree = new XDOMName("Dot");
			int start1 = ps.start();
			stree.pos = npos;
			if (includePos) {
			    ntree.pos = lex.pos.copy();
			}
			lex.getToken();
			XDOMCall tree2 = new XDOMCall(ntree,2);
			tree2.setKind(XDOMCall.CDOT);
			XDOM view = parseExp1();
			tree2.insertArg(-1,view);
			tree2.insertArg(-1,stree);
			if (includePos) {
			    tree2.pos = new XPos(npos.firstLine,npos.firstChar,
				view.pos.lastLine,view.pos.lastChar,
				fpos);
			}
			func = tree2;
		    }
		    tree1 = parseExp(opprec);
		    if (tree1.pos != null) {
			lastLine = tree1.pos.lastLine;
			lastChar = tree1.pos.lastChar;
		    }
		    ps.push(tree1);
		    XDOMCall tree = new XDOMCall(func);
		    tree.setSpaceBefore(before);
		    tree.setSpaceAfter(lex.spaceBefore);
		    tree.setKind(XDOMCall.COP);
		    ps.pop(start,tree,true);
		    result = tree;
		}
	    } else {
		break;
	    }
	    if (includePos) {
		result.pos = new XPos(firstLine,firstChar,lastLine,lastChar,fpos);
	    }
	}
	return result;
    }

    /**
     ** Parses a single XML element.
     ** @return the parse tree for that element.
     */
    private XDOM parseXCmd(boolean endxml,boolean xmlDecl) {
	XDOMElement result = new XDOMElement(lex.val);
	int firstLine = lex.pos.firstLine;
	int firstChar = lex.pos.firstChar;
	int lastLine = lex.pos.lastLine;
	int lastChar = lex.pos.lastChar;
	result.setSpaceBefore(lex.spaceBefore);
	result.setSpaceBeforeBody(XDOM.LINE);
	result.setSpaceAfterBody(XDOM.LINE);
	if (xmlDecl) {
	    if (lex.val != "xml") {
		errors.error(Errors.PARSE,lex.pos,"not xml");
	    }
	    result.setKind(XDOMElement.EXMLDECL);
	} else {
	    result.setKind(XDOMElement.EXML);
	}
	lex.getToken();
	if (incr && startElement != null) {
	    int hasAttr = 0;
	    if (lex.kind != Lexer.EOF && lex.kind != Lexer.XRIGHT &&
		lex.kind != Lexer.XRIGHTE && lex.kind != Lexer.XMLRIGHT ) {
		hasAttr = 1;
	    } 
	    stack.push(result,0);
	    stack.push(value.vlBool,hasAttr);
	    callExec.execCallObject(startElement,vlo.type,vlo,0,true,0,2,caller,value.ctxEval);
	    stack.pop();
	}
	if (lex.kind != Lexer.EOF && lex.kind != Lexer.XRIGHT &&
	    lex.kind != Lexer.XRIGHTE && lex.kind != Lexer.XMLRIGHT) {
	    int start = ps.start();
	    while (true) {
		if (lex.kind == Lexer.EOF) {
		    break;
		} else if (lex.kind == Lexer.XRIGHT) {
		    break;
		} else if (lex.kind == Lexer.XRIGHTE) {
		    break;
		} else if (lex.kind == Lexer.XMLRIGHT) {
		    break;
		} else if (lex.kind == Lexer.NAME) {
		    XDOMName ntree1 = new XDOMName("Equal");
		    XDOMName ntree2 = new XDOMName(lex.val);
		    ntree2.setSpaceBefore(lex.spaceBefore);
		    XDOM x = null;
		    boolean ok = true;
		    if (includePos) {
			ntree2.pos = lex.pos.copy();
		    }
		    lex.getToken();
		    ntree2.setSpaceAfter(lex.spaceBefore);
		    if (lex.kind == Lexer.ONAME && lex.val == "Equal") {
			if (includePos) {
			    ntree1.pos = lex.pos.copy();
			}
			lex.getToken();
		    } else {
			errors.error(Errors.PARSE,lex.pos,"missing =");
			ok = false;
		    }
		    if (lex.kind == Lexer.STRING || lex.kind == Lexer.SNAME) {
			x = parseString();
			lastLine = lex.pos.lastLine;
			lastChar = lex.pos.lastChar;
		    } else {
			errors.error(Errors.PARSE,lex.pos,"missing string");
			ok = false;
		    }
		    if (ok && x != null) {
			XDOMCall ctree = new XDOMCall(ntree1);
			ctree.setKind(XDOMCall.COP);
			int start1 = ps.start();
			ctree.setSpaceBefore(ntree2.getSpaceBefore());
			ctree.setSpaceAfter(x.getSpaceAfter());
			x.setSpaceAfter(XDOM.EMPTY);
			ps.push(ntree2);
			ps.push(x);
			ps.pop(start1,ctree,true);
			if (includePos) {
			    result.pos = new XPos(firstLine,firstChar,lastLine,lastChar,fpos);
			}
			if (incr && endAttr != null) {
			    stack.push(result,0);
			    stack.push(ctree,0);
			    callExec.execCallObject(endAttr,vlo.type,vlo,0,true,0,2,caller,value.ctxEval);
			    Object oval = stack.getTopOval();
			    long ival = stack.getTopIval();
			    if (value.isNull(oval,ival)) {
			    } else if (oval instanceof XDOM) {
				XDOM x1 = (XDOM) oval;
//				ps.push(x1);
				result.insertAttr(-1,x1);
			    } else {
//				ps.push(ctree);
				result.insertAttr(-1,ctree);
			    }
			    stack.pop();
			} else {
			    ps.push(ctree);
			}
		    }
		} else {
		    errors.error(Errors.PARSE,lex.pos,"missing name or >");
		    lex.getToken();
		}
	    }
//	    ps.popAttr(start,result,true);
	    if (! incr || endAttr == null) ps.popAttr(start,result,true);
	}
	if (incr && startBody != null) {
	    int hasBody = 0;
	    if (lex.kind == Lexer.XRIGHT) {
		hasBody = 1;
	    } 
	    stack.push(result,0);
	    stack.push(value.vlBool,hasBody);
	    callExec.execCallObject(startBody,vlo.type,vlo,0,true,0,2,caller,value.ctxEval);
	    stack.pop();
	}
	if (xmlDecl) {
	    if (endxml) lex.setXML(false);
	    if (lex.kind != Lexer.XMLRIGHT) {
		errors.error(Errors.PARSE,lex.pos,"missing ?>");
	    } else {
		if (endxml) lex.setCmd();
		lex.getToken();
	    }
	} else if (lex.kind == Lexer.XRIGHTE) {
	    if (endxml) {
		lex.setXML(false);
	    }
	    if (endxml) lex.setCmd();
	    lex.getToken();
	} else if (lex.kind == Lexer.XRIGHT) {
	    lex.getToken();
	    result.setSpaceBeforeBody(lex.spaceBefore);
	    parseXCmds(result);
	    int bsize = result.bodySize();
	    if (bsize > 0) {
		XDOM tree2 = result.getBody(bsize);
		if (tree2.pos != null) {
		    lastLine = tree2.pos.lastLine;
		    lastChar = tree2.pos.lastChar;
		}
	    }
	    if (endxml) {
		lex.setXML(false);
	    }
	    if (lex.kind == Lexer.XEND) {
		lastLine = lex.pos.lastLine;
		lastChar = lex.pos.lastChar;
		if (lex.val != result.getTag()) {
		    XPos pos = new XPos(firstLine,firstChar,lastLine,lastChar,fpos);
		    errors.error(Errors.PARSE,pos,
				 "opening <"+result.getTag()+
				 "> does not match closing </"+lex.val+">");
		}
		result.setSpaceAfterBody(lex.spaceBefore);
		if (endxml) lex.setCmd();
		lex.getToken();
	    } else {
		errors.error(Errors.PARSE,lex.pos,"missing </"+result.getTag()+">");
	    }
	} else {
	    if (endxml) {
		lex.setXML(false);
	    }
	    errors.error(Errors.PARSE,lex.pos,"missing > or />");
	}
	if (includePos) {
	    result.pos = new XPos(firstLine,firstChar,lastLine,lastChar,fpos);
	}
	result.setSpaceAfter(lex.spaceBefore);
	if (incr && endElement != null) {
	    stack.push(result,0);
	    callExec.execCallObject(endElement,vlo.type,vlo,0,true,0,1,caller,value.ctxEval);
	    stack.pop();
	}
	return result;
    }

    /**
     ** Parses a single XACT command.
     ** @return the parse tree for that command.
     */
    private XDOM parseCmd() {
	XDOMElement result = new XDOMElement(lex.val);
	int firstLine = lex.pos.firstLine;
	int firstChar = lex.pos.firstChar;
	int lastLine = lex.pos.lastLine;
	int lastChar = lex.pos.lastChar;
	result.setSpaceBefore(lex.spaceBefore);
	result.setSpaceBeforeBody(XDOM.LINE);
	result.setSpaceAfterBody(XDOM.LINE);
	lex.getToken();
	if (incr && startElement != null) {
	    int hasAttr = 0;
	    if (lex.kind != Lexer.EOF && lex.kind != Lexer.LBRACE && lex.kind != Lexer.SEMI
		&& lex.kind != Lexer.RBRACE && lex.kind != Lexer.XRIGHTE &&
	       lex.kind != Lexer.XMLRIGHT) {
		hasAttr = 1;
	    } 
	    stack.push(result,0);
	    stack.push(value.vlBool,hasAttr);
	    callExec.execCallObject(startElement,vlo.type,vlo,0,true,0,2,caller,value.ctxEval);
	    stack.pop();
	}
	if (lex.kind != Lexer.EOF && lex.kind != Lexer.LBRACE && lex.kind != Lexer.SEMI
	    && lex.kind != Lexer.RBRACE && lex.kind != Lexer.XRIGHTE &&
	    lex.kind != Lexer.XMLRIGHT) {
	    int start = ps.start();
	    while (true) {
		XDOM tree1 = parseExp(Prec.topPrec);
		if (tree1.pos != null) {
		    lastLine = tree1.pos.lastLine;
		    lastChar = tree1.pos.lastChar;
		}
		if (incr && endAttr != null) {
		    stack.push(result,0);
		    stack.push(tree1,0);
		    callExec.execCallObject(endAttr,vlo.type,vlo,0,true,0,2,caller,value.ctxEval);
		    Object oval = stack.getTopOval();
		    long ival = stack.getTopIval();
		    if (value.isNull(oval,ival)) {
		    } else if (oval instanceof XDOM) {
			XDOM x1 = (XDOM) oval;
//			ps.push(x1);
			result.insertAttr(-1,x1);
		    } else {
//			ps.push(tree1);
			result.insertAttr(-1,tree1);
		    }
		    stack.pop();
		} else {
		    ps.push(tree1);
		}
		if (lex.kind == Lexer.EOF) {
		    break;
		} else if (lex.kind == Lexer.LBRACE) {
		    break;
		} else if (lex.kind == Lexer.SEMI) {
		    break;
		} else if (lex.kind == Lexer.RBRACE) {
		    break;
		} else if (lex.kind == Lexer.XRIGHTE) {
		    break;
		} else if (lex.kind == Lexer.XMLRIGHT) {
		    break;
		} else if (lex.kind == Lexer.COMMA) {
		    lex.getToken();
		} else {
		    errors.error(Errors.PARSE,lex.pos,"missing ,");
		}
	    }
//	    ps.popAttr(start,result,true);
	    if (! incr || endAttr == null) ps.popAttr(start,result,true);
	}
	if (incr && startBody != null) {
	    int hasBody = 0;
	    if (lex.kind == Lexer.LBRACE) {
		hasBody = 1;
	    } 
	    stack.push(result,0);
	    stack.push(value.vlBool,hasBody);
	    callExec.execCallObject(startBody,vlo.type,vlo,0,true,0,2,caller,value.ctxEval);
	    stack.pop();
	}
	if (lex.kind == Lexer.LBRACE) {
	    int openLine = lex.pos.firstLine;
	    int openChar = lex.pos.firstChar;
	    lex.setCmd();
	    lex.getToken();
	    result.setSpaceBeforeBody(lex.spaceBefore);
	    parseCmds(result);
	    if (result.bodySize() > 0) {
		XDOM tree2 = result.getBody(result.bodySize());
		if (tree2.pos != null) {
		    lastLine = tree2.pos.lastLine;
		    lastChar = tree2.pos.lastChar;
		}
	    }
	    if (lex.kind != Lexer.RBRACE) {
		XPos pos = new XPos(openLine,openChar,lastLine,lastChar,fpos);
		errors.error(Errors.PARSE,pos,"no closing }");
	    } else {
		lastLine = lex.pos.lastLine;
		lastChar = lex.pos.lastChar;
		result.setSpaceAfterBody(lex.spaceBefore);
		lex.setCmd();
		lex.getToken();
	    }
	    hasSep = true;
	} else {
	    hasSep = false;
	}
	if (includePos) {
	    result.pos = new XPos(firstLine,firstChar,lastLine,lastChar,fpos);
	}
	result.setSpaceAfter(lex.spaceBefore);
	if (incr && endElement != null) {
	    stack.push(result,0);
	    callExec.execCallObject(endElement,vlo.type,vlo,0,true,0,1,caller,value.ctxEval);
	    stack.pop();
	}
	return result;
    }

    /**
     ** Parses a sequence of XML commands
     ** and places their parse trees in the body of xe.
     ** @param xe a element 
     */
    private void parseXCmds(XDOMElement xe) {
	int start = ps.start();
	while (true) {
	    XDOM x = null;
	    if (lex.kind == Lexer.EOF) break;
	    if (lex.kind == Lexer.XEND) break;
	    if (lex.kind == Lexer.STRING) {
		XDOMString stree = new XDOMString(lex.val,0);
		stree.setKind(XDOMString.SXML);
		stree.setSpaceBefore(lex.spaceBefore);
		if (includePos) {
		    stree.pos = lex.pos.copy();
		}
		lex.getToken();
		stree.setSpaceAfter(lex.spaceBefore);
		x = stree;
	    } else if (lex.kind == Lexer.NAME) {
		String s = lex.val;
//		if (s == "lt") {
//		    s = "LT";
//		} else if (s == "gt") {
//		    s = "GT";
//		} else if (s == "amp") {
//		    s = "AMP";
//		}
		XDOMName ntree = new XDOMName(s);
		ntree.setKind(XDOMName.NXML);
		ntree.setSpaceBefore(lex.spaceBefore);
		if (includePos) {
		    ntree.pos = lex.pos.copy();
		}
		lex.getToken();
		ntree.setSpaceAfter(lex.spaceBefore);
		x = ntree;
	    } else if (lex.kind == Lexer.XMLSTART) {
		x = parseXCmd(false,true);
	    } else if (lex.kind == Lexer.XSTART) {
		x = parseXCmd(false,false);
	    } else if (lex.kind == Lexer.XCOMMENT) {
		XDOMString xs = new XDOMString(lex.val);
		if (includePos) xs.pos = lex.pos.copy();
		xs.setKind(XDOMString.SCOMMENT);
		xs.setSpaceBefore(lex.spaceBefore);
		x = xs;
		lex.getToken();
		xs.setSpaceAfter(lex.spaceBefore);
	    } else if (lex.kind == Lexer.CNAME) {
		x = parseCmd();
		if (lex.kind == Lexer.XRIGHTE) {
		    lex.getToken();
		    x.setSpaceAfter(lex.spaceBefore);
		} else {
		    errors.error(Errors.PARSE,lex.pos,"missing />");
		}
	    } else {
		errors.error(Errors.INTERNAL,lex.pos,"parse XCmds unrecognized token");
		lex.getToken();
	    }
	    if (x != null) {
		if (incr && endChild != null) {
		    stack.push(xe,0);
		    stack.push(x,0);
		    callExec.execCallObject(endChild,vlo.type,vlo,0,true,0,2,caller,value.ctxEval);
		    Object oval = stack.getTopOval();
		    long ival = stack.getTopIval();
		    if (value.isNull(oval,ival)) {
		    } else if (oval instanceof XDOM) {
			XDOM x1 = (XDOM) oval;
//			ps.push(x1);
			xe.insertBody(-1,x1);
		    } else {
//			ps.push(x);
			xe.insertBody(-1,x);
		    } 
		    stack.pop();
		} else {
		    ps.push(x);
		}
	    }
	}
//	ps.popBody(start,xe,true);
	if (! incr || endChild == null) ps.popBody(start,xe,true);
    }

    /**
     ** Parses a sequence of XACT commands
     ** and places their parse trees in the body of xe.
     ** @param xe a element 
     */
    private void parseCmds(XDOMElement xe) {
	int start = ps.start();
	hasSep = true;
	while (true) {
	    if (lex.kind == Lexer.EOF) break;
	    if (lex.kind == Lexer.RBRACE) break;
	    if (! hasSep) {
		errors.error(Errors.PARSE,lex.pos,"missing ;");
	    }
	    XDOM x = null;
	    if (lex.kind == Lexer.CNAME) {
		XDOM tree =parseCmd();
		x = tree;
		if (! hasSep) {
		    if (lex.kind == Lexer.SEMI) {
			lex.setCmd();
			lex.getToken();
			hasSep = true;
			if (tree instanceof XDOMElement) {
			    XDOMElement etree = (XDOMElement) tree;
			    etree.setSpaceAfter(Lexer.addSpace(etree.getSpaceAfter(),lex.spaceBefore));
			}
		    }
		}
	    } else if (lex.kind == Lexer.XMLSTART) {
		x = parseXCmd(true,true);
		hasSep = true;
	    } else if (lex.kind == Lexer.XSTART) {
		x = parseXCmd(true,false);
		hasSep = true;
	    } else if (lex.kind == Lexer.XCOMMENT) {
		XDOMString xs = new XDOMString(lex.val);
		if (includePos) xs.pos = lex.pos.copy();
		xs.setKind(XDOMString.SCOMMENT);
		xs.setSpaceBefore(lex.spaceBefore);
		x = xs;
		lex.setCmd();
		lex.getToken();
		xs.setSpaceAfter(lex.spaceBefore);
	    } else if (lex.kind == Lexer.CMTNAME) {
		int firstLine = lex.pos.firstLine;
		int firstChar = lex.pos.firstChar;
		int lastLine = lex.pos.lastLine;
		int lastChar = lex.pos.lastChar;
		XDOMElement xe1 = new XDOMElement("comment");
		xe1.setSpace(lex.val);
		xe1.setKind(XDOMElement.ESHORT);
		lex.setCmd();
		lex.getToken();
		if (lex.kind == Lexer.SHORT || lex.kind == Lexer.LONG) {
		    lastLine = lex.pos.lastLine;
		    lastChar = lex.pos.lastChar;
		    if (lex.kind == Lexer.LONG) xe1.setKind(XDOMElement.ELONG);
		    XDOMString xs = new XDOMString(lex.val);
		    if (includePos) {
			xs.pos = lex.pos.copy();
		    }
		    lex.setCmd();
		    xe1.insertAttr(-1,xs);
		    lex.getToken();
		} else {
		    errors.error(Errors.PARSE,lex.pos,"missing // or /*");
		}
		if (includePos) {
		    xe1.pos = new XPos(firstLine,firstChar,lastLine,lastChar,fpos);
		}
		x = xe1;
	    } else if (lex.kind == Lexer.SHORT || lex.kind == Lexer.LONG) {
		XDOMName xn = new XDOMName("Comment");
		XDOMCall xc = new XDOMCall(xn);
		XDOMString xs = new XDOMString(lex.val);
		xc.setSpaceBefore(lex.spaceBefore);
		if (lex.kind == Lexer.SHORT) {
		    xc.setKind(XDOMCall.CSHORT);
		} else {
		    xc.setKind(XDOMCall.CLONG);
		}
		if (includePos) {
		    xc.pos = lex.pos.copy();
		    xs.pos = lex.pos.copy();
		    xn.pos = lex.pos.copy();
		}
		int start1 = ps.start();
		ps.push(xs);
		ps.pop(start1,xc,true);
		lex.setCmd();
		lex.getToken();
		xc.setSpaceAfter(lex.spaceBefore);
		x = xc;
	    } else {
		x = parseExp(Prec.topPrec);
		hasSep = false;
		if (lex.kind == Lexer.SEMI) {
		    lex.setCmd();
		    lex.getToken();
		    hasSep = true;
		}
	    }
	    if (x != null) {
		if (incr && endChild != null) {
		    stack.push(xe,0);
		    stack.push(x,0);
		    callExec.execCallObject(endChild,vlo.type,vlo,0,true,0,2,caller,value.ctxEval);
		    Object oval = stack.getTopOval();
		    long ival = stack.getTopIval();
		    if (value.isNull(oval,ival)) {
		    } else if (oval instanceof XDOM) {
			XDOM x1 = (XDOM) oval;
//			ps.push(x1);
			xe.insertBody(-1,x1);
		    } else {
//			ps.push(x);
			xe.insertBody(-1,x);
		    } 
		    stack.pop();
		} else {
		    ps.push(x);
		}
	    }
	}
//	ps.popBody(start,xe,true);
	if (! incr || endChild == null) ps.popBody(start,xe,true);
    }

    /**
     ** Parse an input stream of characters to produce an {@link XDOM}
     ** parse tree.
     ** @param r a stream of characters to be parsed.
     ** @param includePos true if positions are to be included in the
     ** result tree.
     ** @param fpos the file part of position information for the
     ** result tree and for error messages.
     ** @param vlo the parse action handler (or null is none is
     ** specified). This is specified in the handler parameter of
     ** the XACT Parse function.
     ** @param caller an XACT caller for any called actions.
     ** @return an XDOM parse tree. This will always be an inserted
     ** x:block command that wraps all top level commands in the input.
     */
    public XDOMElement parse(XR r,boolean includePos,FPosition fpos,VLObj vlo,XDOM caller) {
	inUse = true;
	if (vlo != null) {
	    incr = true;
	    this.vlo = vlo;
	    this.caller = caller;
	    XDef start = callExec.findCallObject(vlo.type,"start",false,null);
	    startElement = callExec.findCallObject(vlo.type,"startElement",false,null);
	    startBody = callExec.findCallObject(vlo.type,"startBody",false,null);
	    endElement = callExec.findCallObject(vlo.type,"endElement",false,null);
	    endAttr = callExec.findCallObject(vlo.type,"endAttr",false,null);
	    endChild = callExec.findCallObject(vlo.type,"endChild",false,null);
	    if (start != null) {
		callExec.execCallObject(start,vlo.type,vlo,0,true,0,0,caller,value.ctxEval);
		stack.pop();
	    }
	} else {
	    incr = false;
	}
	XDOMElement result = new XDOMElement("x:block");
	result.setSpaceBefore(XDOM.EMPTY);
	result.setSpaceBeforeBody(XDOM.LINE);
	result.setSpaceAfterBody(XDOM.LINE);
	result.setSpaceAfter(XDOM.EMPTY);
	this.includePos = includePos;
	this.fpos = fpos;
	lex.start(r,fpos);
	lex.setCmd();
	lex.getToken();
	if (incr && startElement != null) {
	    stack.push(result,0);
	    stack.push(value.vlBool,1);
	    callExec.execCallObject(startElement,vlo.type,vlo,0,true,0,2,caller,value.ctxEval);
	    stack.pop();
	}
	parseCmds(result);
	int bsize = result.bodySize();
	if (bsize > 0) {
	    XDOM tree1 = result.getBody(1);
	    XDOM tree2 = result.getBody(result.bodySize());
	    if (tree1.pos != null && tree2.pos != null && includePos) {
		result.pos = new XPos(tree1.pos.firstLine,tree1.pos.firstChar,
				      tree2.pos.lastLine,tree2.pos.lastChar,fpos);
	    }
	}
	if (incr) {
	    if (endElement != null) {
		stack.push(result,0);
		callExec.execCallObject(endElement,vlo.type,vlo,0,true,0,1,caller,value.ctxEval);
		stack.pop();
	    }
	    XDef end = callExec.findCallObject(vlo.type,"end",false,caller);
	    if (end != null) {
		callExec.execCallObject(end,vlo.type,vlo,0,true,0,0,caller,value.ctxEval);
		stack.pop();
	    }
	}
	lex.finish();
	inUse = false;
	return result;
    }
}
