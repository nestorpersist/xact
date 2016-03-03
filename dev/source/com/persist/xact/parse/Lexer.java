/*******************************************************************************
*
* Copyright (c) 2002-2006. John R. Nestor. All rights reserved.
*
* See the file XLICENSE.txt for use and redistribution information.
*
*******************************************************************************/

package com.persist.xact.parse;

import com.persist.xact.system.*;
import com.persist.xdom.*;

/**
 <p>
 This is the XACT lexical analyzer.
 It contains the following parts.
 </p>
 <ul>
    <li> <b>Overall Initialization.</b>
    The lexer is is initalized by calling 
    {@link #init}.
    </li>
    <li> <b>Start Analysis.</b>
    Analysis is started by calling 
    {@link #start} and passing it
    an input stream of characters to be analyzed.
    </li>
    <li> <b>Token Get.</b>
    A sequence of calls to {@link #getToken} get all
    the tokens for the input stream.
    After calling {@link #getToken} the token values
    will be in {@link #kind}, {@link #pos}, {@link #val}, {@link #more},
    {@link #spaceBefore} and {@link #singleQuote}.
    The are named constants for each {@link #kind} of token.
    The final token will have kind {@link #EOF}.
    </li>
    <li> <b>Mode Switching.</b>
    The lexer support both XML and XACT lexical rules.
    The calls {@link #setXML} and {@link #setCmd} switch between
    these rules and affect following calls to {@link #getToken}.
    Note the lexer starts out in XACT mode.
    </li>
    <li> <b>End Analysis.</b>
    After all tokens in the input stream are processed {@link
    #finish} should be called. After this another input stream
    can be processed by calling {@link #start}.
    </li>
 </ul>
 <p>
     Tokens are built incrementally into a token buffer
     stored in the thread specific {@link #xt}.sbuff string buffer.
 </p>
*/

public final class Lexer {

    /**
     ** Turn this on to see all the tokens produced by the lexer.
     */
    private boolean debugLexer = false;

    /**
     ** The token kind (set by {@link #getToken}).
     ** Possible values are
     ** {@link #EOF},
     ** {@link #NAME},
     ** {@link #NUMBER},
     ** {@link #CNAME},
     ** {@link #ONAME},
     ** {@link #STRING},
     ** {@link #LPAREN},
     ** {@link #RPAREN},
     ** {@link #LSQUARE},
     ** {@link #LBRACE},
     ** {@link #RBRACE},
     ** {@link #DOT},
     ** {@link #HASH},
     ** {@link #COMMA},
     ** {@link #SEMI},
     ** {@link #SNAME},
     ** {@link #XSTART},
     ** {@link #XRIGHT},
     ** {@link #XEND},
     ** {@link #XRIGHTE},
     ** {@link #XMLSTART},
     ** {@link #XMLRIGHT},
     ** {@link #SHORT},
     ** {@link #LONG},
     ** {@link #XMLSTART},
     ** {@link #CMTNAME},
     ** and {@link #XCOMMENT}.
     */
    public int kind;
    /**
     ** The token position (set by {@link #getToken}).
     */
    public XPos pos;
    /**
     ** The token string value (if any) (set by {@link #getToken}).
     */
    public String val;
    /**
     ** The token is set to true for string tokens that have following
     ** parts (set by {@link #getToken}).
     ** For example the string "a&b;c" has three parts "a", &b; and "c".
     ** Here the lexer returns three tokens with more being true after
     ** the first two.
     */
    public boolean more;
    /**
     ** The amount of space before the token (set by {@link #getToken}).
     ** The value will be {@link XDOM#EMPTY}, {@link XDOM#SPACE},
     ** {@link XDOM#LINE}, or {@link XDOM#LINES}.
     */
    public byte spaceBefore;
    /**
     ** The token will be true for string tokens that use ' rather than
     ** " (set by {@link #getToken}).
     */
    public boolean singleQuote;

    public static byte addSpace(byte space1,byte space2) {
	if (space1 > space2) return space1;
	return space2;
    }

    /**
     ** The {@link #kind} value for end of file.
     */
    public final static int EOF = 0;
    /**
     ** The {@link #kind} value for a name.
     */
    public final static int NAME = 1;
    /**
     ** The {@link #kind} value for a number.
     */
    public final static int NUMBER = 2;
    /**
     ** The {@link #kind} value for command (tag) name of form
     ** <b>@xxx</b>
     */
    public final static int CNAME = 3;     
    /**
     ** The {@link #kind} value for a operator.
     */
    public final static int ONAME = 4;    
    /**
     ** The {@link #kind} value for a string.
     */
    public final static int STRING = 5;
    /**
     ** The {@link #kind} value for a <b>(</b>
     */
    public final static int LPAREN = 6;
    /**
     ** The {@link #kind} value for a <b>)</b>
     */
    public final static int RPAREN = 7;
    /**
     ** The {@link #kind} value for a <b>[</b>
     */
    public final static int LSQUARE = 8;
    /**
     ** The {@link #kind} value for a <b>]</b>
     */
    public final static int RSQUARE = 9;
    /**
     ** The {@link #kind} value for a <b>{</b>
     */
    public final static int LBRACE = 10;
    /**
     ** The {@link #kind} value for a <b>}</b>
     */
    public final static int RBRACE = 11;
    /**
     ** The {@link #kind} value for <b>.</b>
     */
    public final static int DOT = 12;
    /**
     ** The {@link #kind} value for a <b>#</b>
     */
    public final static int HASH = 13;
    /**
     ** The {@link #kind} value for a <b>,</b>
     */
    public final static int COMMA = 14;
    /**
     ** The {@link #kind} value for a <b>;</b>
     */
    public final static int SEMI = 15;
    /**
     ** The {@link #kind} value for an entity name of the form
     ** <b>&amp;foo;</b>
     */
    public final static int SNAME = 16; 
    /**
     ** The {@link #kind} value for a start of an XML start tag of the
     ** form <b><</b><b>foo</b>
     */
    public final static int XSTART = 17;   
    /**
     ** The {@link #kind} value for the end of an XML start tag of the
     ** form <b>></b>
     */
    public final static int XRIGHT = 18;   
    /**
     ** The {@link #kind} value for an XML end tag of the form
     ** <b>&lt;</b><b>/foo></b>
     */
    public final static int XEND = 19;
    /**
     ** The {@link #kind} value for an XML tag close <b>/</b><b>></b>
     */
    public final static int XRIGHTE = 20;  
    /**
     ** The {@link #kind} value for <b><</b><b>?xml</b>
     */
    public final static int XMLSTART = 21; 
    /**
     ** The {@link #kind} value for <b>?</b><b>&gt;</b>
     */
    public final static int XMLRIGHT = 22;
    /**
     ** The {@link #kind} value for an XACT comment of the
     ** form <b>// comment</b>
     */
    public final static int SHORT = 23;  
    /**
     ** The {@link #kind} value for an XACT comment of the
     ** form <b>/* comment *</b><b>/</b>
     ** 
     */
    public final static int LONG = 24;    
    /**
     ** The {@link #kind} value for a comment name of the form
     ** <b>name:</b>
     */
    public final static int CMTNAME = 25; 
    /**
     ** The {@link #kind} value for an XML comment of
     ** form <b><</b><b>!-- comment --></b>
     */
    public final static int XCOMMENT = 26; 

    /**
     ** String forms of all token {@link #kind} values.
     */
    public final static String[] kname= {
	"EOF","NAME","NUMBER","CNAME",
	"ONAME","STRING","LPAREN","RPAREN",
	"LSQUARE","RSQUARE","LBRACE","RBRACE",
	"DOT","HASH",
	"COMMA", "SEMI", "SNAME",
	"XSTART", "XRIGHT", "XEND", "XRIGHTE",
	"XMLSTART", "XMLRIGHT", "SHORT", "LONG"
    };

    /**
     ** The thread in which the Lexer is running.
     */
    private XThread xt;
    /**
     ** The stream of characters being broken into tokens.
     */
    private XR r = null;
    /**
     ** The handler for lexical errors.
     */
    private Errors errors;

    /**
     ** The line number of the first character of the current token.
     */
    private int openLine;

    /**
     ** The character position of the first character of the current token
     ** within the {@link #openLine} line.
     */
    private int openChar;

    /**
     ** The line number of the initial &lt; character of an XML element.
     */
    private int headLine;

    /**
     ** The character position of the initial &lt; character of an XML element
     ** within {@link #headLine}.
     */
    private int headChar;

    /**
     ** Processing mode.
     ** Legal values are {@link #NORMAL}, {@link #XML},
     ** {@link #CMD}, {@link #XMLHEAD},
     ** {@link #CSTRING}, {@link #CSTRINGA}, {@link #XSTRING},
     ** and {@link #XSTRINGA}.
     */
    private int mode;
    /**
     ** XACT {@link #mode} (&lt; is XML).
     */
    private final static int NORMAL = 0;    
    /**
     ** XACT " string continue {@link #mode}.
     ** For "ab&amp;bc" (after a and b).
     */
    private final static int CSTRING = 1;
    /**
     ** XML {@link #mode}.
     */
    private final static int XML = 2;
    /**
     ** XML {@link #mode} for inside a tag.
     */
    private final static int XMLHEAD = 3;
    /**
     ** XML string continue {@link #mode}.
     ** For "ab&amp;bc" (after a and b).
     */
    private final static int XSTRING = 4;
    /**
     ** XACT command {@link #mode} (&lt; is an op).
     */
    private final static int CMD = 5;
    /**
     ** XACT ' string continue {@link #mode}.
     ** For 'ab&amp;bc' (after a and b).
     */
    private final static int CSTRINGA = 6;
    /**
     ** XML string continue {@link #mode}.
     ** For 'ab&amp;bc' (after a and b).
     */
    private final static int XSTRINGA = 7;

    /**
     ** Print strings for {@link #mode}.
     */
    private final static String[] mname = {
	"NORMAL", "CSTRING", "XML", "XMLHEAD", "XSTRING",
	"CMD", "CSTRINGA", "XSTRINGA"
    };

    /**
     ** The current character from the input stream.
     */
    private char ch;
    /**
     ** True if an end of input stream.
     */
    private boolean eof;
    /**
     ** The current character {@link #ch} position.
     */
    private XPos chpos;
    /**
     ** The index of the current character {@link #ch} on the
     ** current line. First character has index 1.
     */
    private int charCnt;
    /**
     ** The amount of space after the token (set by {@link #getToken}).
     ** The value will be {@link XDOM#EMPTY}, {@link XDOM#SPACE},
     ** {@link XDOM#LINE}, or {@link XDOM#LINES}.
     ** This is used to initialized {@link #spaceBefore} on
     ** the next call to {@link #getToken}.
     */
    private byte spaceAfter;

    /**
     ** True if all space is to be kept in XML comments and strings.
     ** Now always set to false (support may be added in a future
     ** version of the Lexer).
     ** If false space is removed at the beginning and end and
     ** noted in {@link #spaceBefore} and {@link #spaceAfter}.
     ** Internal space is also reduced to one of a single
     ** blank, a single end of line or two end of lines.
     */
    private boolean keepSpace;

    /**
     ** The next character after {@link #ch} in the input stream.
     */
    /**
     ** The next character {@link #nextCh} position.
     */
    private char nextCh;
    /**
     ** True if end of file has occured one character after
     ** the current character.
     */
    private boolean nextEof;

    /**
     ** Creates a new lexical analyzer for a specified thread.
     ** 
     ** @param xt the thread.
     */
    public Lexer(XThread xt) {
	this.xt = xt;
    }


    /**
     ** Initializes the lexical analyzer.
     ** Must be called before any other methods.
     */
    public void init() {
	chpos = new XPos();
	errors = xt.errors;
    }

    /**
     ** Gets the next character in the input stream {@link #r}.
     ** Sets {@link #ch}to the next character
     ** At end of file sets {@link #eof} to true.
     ** Also sets {@link #chpos}, {@link #charCnt}, {@link #nextCh}, and {@link #nextEof}.
     */
    private void getChar() {
	try {
	    if (ch == '\n') {
		chpos.firstLine ++;
		chpos.firstChar = 0;
		charCnt = 0;
	    }
	    ch = nextCh;
	    eof = nextEof;
	    if (! eof) {
		chpos.firstChar ++;
		charCnt ++;
	    }
	    if (! nextEof) {
		int ci = r.read();
		if (ci == -1) {
		    nextEof = true;
		} else {
		    nextCh = (char) ci;
		}
	    }
	} catch(Exception e) {
	    errors.fail(e,"getChar");
	}
    }

    /**
     ** Starts the lexical analysis of a new stream.
     ** Must be called before calling {@link #getToken}.
     ** Can be called only after a call to {@link #init} or
     ** {@link #finish}.
     ** 
     ** @param r a stream of characters to break into tokens.
     ** @param fpos a file position for error reporing.
     */
    public void start(XR r,FPosition fpos) {
	this.r = r;
	ch = '\n';
	mode = NORMAL;
	eof = false;
	spaceAfter = XDOM.EMPTY;
	keepSpace = false;
	chpos.firstLine = 0;
	chpos.firstChar = 0;
	chpos.lastLine = -1;
	chpos.lastChar = 0;
	chpos.fpos = fpos;
	charCnt = 0;
	pos = new XPos(0,0,-1,0,fpos);
	nextEof = false;
	int ci = -1;
	try {
	    ci = r.read();
	} catch(Exception e) {
	}
	if (ci == -1) {
	    nextEof = true;
	} else {
	    nextCh = (char) ci;
	}
	getChar();
    }

    /**
     ** Replaces a tab character in xt.sbuff with
     ** the appropriate number of spaces.
     ** It assumes tabs are 8 characters apart.
     */
    private void expandTab() {
	int size = xt.sbuff.length();
	xt.sbuff.setLength(size-1);
	charCnt --;
	int cnt = 8-(charCnt % 8);
	for (int i = 0; i < cnt; i++) {
	    xt.sbuff.append(' ');
	}
	charCnt += cnt;
    }

    /**
     ** Get and XML name into {@link #val}.
     ** @return true if a name is found
     */
    private boolean getName(String prefix) {
	xt.sbuff.setLength(0);
	xt.sbuff.append(prefix);
	if (!(('a' <= ch && ch  <= 'z') || ('A' <= ch && ch <= 'Z') ||
	      ch == '.' || ch == '_' || ch == ':')) {
	    return false;
	}
	while (true) {
	    xt.sbuff.append(ch);
	    getChar();

	    pos.lastChar = chpos.firstChar - 1;
	    if (eof || ! (('a' <= ch && ch  <= 'z') || ('A' <= ch && ch <= 'Z') ||
			  ('0' <= ch && ch <= '9') || ch == '-' ||
			  ch == '.' || ch == '_' || ch == ':')) {
		break;
	    }
	}
	val = xt.sbuff.toString().intern();
	return true;
    }

    /**
     ** Get and XACT name into {@link #val}.
     ** @return true if a name is found
     */
    private boolean getXName(String prefix) {
	if (ch == '$') {
	    openLine = chpos.firstLine;
	    openChar = chpos.firstChar;
	    getChar();
	    if (! getName(prefix)) {
		errors.error(Errors.PARSE,chpos,"missing name after $");
		return false;
	    }
	    if (ch == '$') {
		getChar();
	    } else {
		XPos openPos = new XPos(openLine,openChar,
					chpos.firstLine,chpos.firstChar,chpos.fpos);
		errors.error(Errors.PARSE,openPos,"no closing $");
	    }
	    return true;
	}
	xt.sbuff.setLength(0);
	xt.sbuff.append(prefix);
	if (!(('a' <= ch && ch  <= 'z') || ('A' <= ch && ch <= 'Z') ||
	      ch == '_' || ch == ':')) {
	    return false;
	}
	while (true) {
	    if (ch == ':' && (nextCh == '=' || nextCh == '/')) {
		break;
	    } else {
		xt.sbuff.append(ch);
		getChar();
	    }
	    pos.lastChar = chpos.firstChar - 1;
	    if (eof || ! (('a' <= ch && ch  <= 'z') || ('A' <= ch && ch <= 'Z') ||
			  ('0' <= ch && ch <= '9') ||
			  ch == '_' || ch == ':')) {
		break;
	    }
	}
	if (xt.sbuff.charAt(xt.sbuff.length()-1) == ':') {
	    errors.error(Errors.PARSE,pos,"name may not end in :");
	}
	val = xt.sbuff.toString().intern();
	return true;
    }

    /**
     ** Added a sequence of digits to the token buffer.
     */
    private boolean getDigits() {
	boolean found = false;
	while (true) {
	    if (eof || !('0' <= ch && ch <= '9')) return found;
	    xt.sbuff.append(ch);
	    pos.lastChar = chpos.firstChar;
	    getChar();
	    found = true;
	}
    }

    /**
     ** Gets the next number token.
     ** The number can optionally include
     ** a decimal point and exponent.
     ** The result is left as a string.
     */
    private boolean getNumber() {
	xt.sbuff.setLength(0);
	if (! ('0' <= ch && ch <= '9')) {
	    return false;
	}
	getDigits();
	if (ch == '.' && '0' <= nextCh && nextCh <= '9') {
	    xt.sbuff.append(ch);
	    getChar();
	    if (! getDigits()) {
		errors.error(Errors.PARSE,chpos,"digits expected after . in number");
	    }
	}
	if (ch == 'e' || ch == 'E') {
	    xt.sbuff.append(ch);
	    getChar();
	    if (! getDigits()) {
		errors.error(Errors.PARSE,chpos,"digits expected after E in number");
	    }
	}
	val = xt.sbuff.toString().intern();
	return true;
    }

    /**
     ** Get the decimal or hexidecimal number of an entity.
     ** Also consumes the following semicolon.
     ** @return the int value.
     */
    private int getCharNum() {
	int val = 0;
	boolean ok = false;
	if (ch == 'x' || ch == 'X') {
	    getChar();
	    while (true) {
		if ('0' <= ch && ch <= '9') {
		    val = val * 16 + (ch - '0');
		    ok = true;
		} else if ('a' <= ch && ch <= 'z') {
		    val = val * 16 + (ch -'a' + 10);
		    ok = true;
		} else if ('A' <= ch && ch <= 'Z') {
		    val = val * 16 + (ch -'A' + 10);
		    ok = true;
		} else {
		    break;
		}
		getChar();
	    }
	} else {
	    while ('0' <= ch && ch <= '9') {
		val = val * 10 + (ch-'0');
		getChar();
		ok = true;
	    }
	}
	if (! ok) {
	    errors.error(Errors.PARSE,chpos,"missing number");
	}
	if (ch == ';') {
	    getChar();
	} else {
	    ok = false;
	    errors.error(Errors.PARSE,chpos,"missing ;");
	}
	if (! ok) return -1;
	return val;
    }

    /**
     ** Called by {@link #getToken} when {@link #mode} is
     ** {@link #CSTRING} {@link #CSTRINGA}, {@link #XSTRING} or {@link
     ** #XSTRINGA}.
     ** @param xml true for {@link #XSTRING} and {@link #XSTRINGA}.
     ** @param quot '\'' for A forms '"' for nonA forms.
     */
    private void getString(boolean xml,char quot) {
	int mode1 = NORMAL;
	int mode2 = CSTRING;
	if (quot == '\'') {
	    mode2 = CSTRINGA;
	    singleQuote = true;
	}
	if (xml) {
	    mode1 = XMLHEAD;
	    mode2 = XSTRING;
	    if (quot == '\'') mode2 = XSTRINGA;
	}
	xt.sbuff.setLength(0);
	if (ch == '&' && nextCh != '#') {
	    getChar();
	    if (getName("")) {
		kind = SNAME;
		if (ch != ';') {
		    errors.error(Errors.PARSE,chpos,"missing ; for entity");
		} else {
		    pos.lastLine = chpos.firstLine;
		    pos.lastChar = chpos.firstChar;
		    getChar();
		}
		if (ch == quot) {
		    pos.lastLine = chpos.firstLine;
		    pos.lastChar = chpos.firstChar;
		    mode = mode1;
		    more = false;
		    getChar();
		} else {
		    mode = mode2;
		    more = true;
		}
	    } else {
		errors.error(Errors.PARSE,chpos,"missing entity name");
		getChar();
	    }
	} else {
	    while (true) {
		if (eof || ch == quot) break;
		if (ch == '&') {
		    if (nextCh != '#') break;
		    getChar();
		    getChar();
		    int val = getCharNum();
		    if (val != -1) {
			xt.sbuff.append((char) val);
			pos.lastLine = chpos.firstLine;
			pos.lastChar = chpos.firstChar-1;
		    }
		} else {
		    xt.sbuff.append(ch);
		    pos.lastLine = chpos.firstLine;
		    pos.lastChar = chpos.firstChar;
		    getChar();
		}
	    }
	    if (eof) {
		XPos openPos = new XPos(openLine,openChar,
					chpos.firstLine,chpos.firstChar,chpos.fpos);
		errors.error(Errors.PARSE,openPos,"no closing "+quot);
	    } else if (ch == '&') {
		val = xt.sbuff.toString().intern();
		kind = STRING;
		mode = mode2;
		more = true;
	    } else {
		pos.lastLine = chpos.firstLine;
		pos.lastChar = chpos.firstChar;
		val = xt.sbuff.toString().intern();
		kind = STRING;
		mode = mode1;
		more = false;
		getChar();
	    }
	}
    }

    /**
     ** Called by {@link #getToken} when {@link #mode} is
     ** {@link #XMLHEAD}.
     */
    private void getTokenXMLHEAD() {
	while (true) {
	    pos.firstLine = chpos.firstLine;
	    pos.firstChar = chpos.firstChar;
	    pos.lastLine = chpos.firstLine;
	    pos.lastChar = chpos.firstChar;
	    if (eof) {
		kind = EOF;
		return;
	    }
	    switch (ch) {
		case '>' :
		{
		    kind = XRIGHT;
		    mode = XML;
		    getChar();
		    return;
		}
		case '?':
		{
		    getChar();
		    if (ch == '>') {
			pos.lastChar = chpos.firstChar;
			getChar();
		    } else {
			errors.error(Errors.PARSE,chpos,"Missing > after ?");
		    }
		    kind = XMLRIGHT;
		    mode = XML;
		    return;
		}
		case '/':
		{
		    getChar();
		    if (ch == '>') {
			pos.lastChar = chpos.firstChar;
			getChar();
		    } else {
			errors.error(Errors.PARSE,chpos,"missing >");
		    }
		    kind = XRIGHTE;
		    mode = XML;
		    return;
		}
		case '=' :
		{
		    kind = ONAME;
		    val = "Equal";
		    getChar();
		    return;
		}
		case ' ': case '\t':
		{
		    if (spaceBefore == XDOM.EMPTY) {
			spaceBefore = XDOM.SPACE;
		    }
		    getChar();
		    break;
		}
		case '\r':
		{
		    if (nextCh != '\n') errors.error(Errors.PARSE,chpos,"CR Not followed bf LF");
		    getChar();
		    break;
		}
		case '\n':
		{
		    if (spaceBefore == XDOM.LINES) {
		    } else if (spaceBefore == XDOM.LINE) {
			spaceBefore = XDOM.LINES;
		    } else {
			spaceBefore = XDOM.LINE;
		    }
		    getChar();
		    break;
		}
		case '"' :
		{
		    openLine = chpos.firstLine;
		    openChar = chpos.firstChar;
		    getChar();
		    getString(true,'"');
		    return;
		}
		case '\'' :
		{
		    openLine = chpos.firstLine;
		    openChar = chpos.firstChar;
		    getChar();
		    getString(true,'\'');
		    return;
		}
		default:
		{
		    if (getName("")) {
			kind = NAME;
			return;
		    }
		    XPos headPos = new XPos(headLine,headChar,
					    chpos.firstLine,chpos.firstChar-1,chpos.fpos);
		    errors.error(Errors.PARSE,headPos,"missing closing >");
		    getChar();
		}
	    }
	}
    }

    /**
     ** Get a string in the body of an XML tag.
     ** Named entities separate strings.
     ** Numberic entities are expanded here.
     ** @return true if a string is found.
     **/
    private boolean bodyString() {
	boolean found = false;
	xt.sbuff.setLength(0);
	boolean onlySpace = true;
	int last = -1;
	int lastEOL = -1;
	while (true) {
	    if (eof || ch == '<') break;
	    if (ch == '&') {
		if (nextCh != '#') break;
		if (! keepSpace) {
		    if (lastEOL > 0) {
			xt.sbuff.setLength(lastEOL+1);
		    }
		    onlySpace = false;
		    spaceAfter = XDOM.EMPTY;
		    last = xt.sbuff.length();
		    lastEOL = -1;
		}
		getChar();
		getChar();
		int val = getCharNum();
		if (val != -1) {
		    xt.sbuff.append((char) val);
		    pos.lastLine = chpos.firstLine;
		    pos.lastChar = chpos.firstChar-1;
		}
	    }
	    if (keepSpace) {
		xt.sbuff.append(ch);
		if (ch == ' ' || ch == '\t') {
		    if (spaceBefore == XDOM.EMPTY) spaceBefore = XDOM.SPACE;
		} else if (ch == '\n') {
		    if (spaceBefore == XDOM.LINES) {
		    } else if (spaceBefore == XDOM.LINE) {
			spaceBefore = XDOM.LINES;
		    } else {
			spaceBefore = XDOM.LINE;
		    }
		}
	    } else {
		if (ch == ' ' || ch == '\t') {
		    if (onlySpace) {
			if (spaceBefore == XDOM.EMPTY) spaceBefore = XDOM.SPACE;
		    } else {
			if (spaceAfter == XDOM.EMPTY) spaceAfter = XDOM.SPACE;
		    }
		} else if (ch == '\n') {
		    if (onlySpace) {
			if (spaceBefore == XDOM.LINES) {
			} else if (spaceBefore == XDOM.LINE) {
			    spaceBefore = XDOM.LINES;
			} else {
			    spaceBefore = XDOM.LINE;
			}
		    } else {
			if (spaceAfter == XDOM.LINES) {
			} else if (spaceAfter == XDOM.LINE) {
			    spaceAfter = XDOM.LINES;
			} else {
			    spaceAfter = XDOM.LINE;
			}
		    }
		    lastEOL = xt.sbuff.length();
		} else if (ch == '\r') {
		    if (nextCh != '\n') errors.error(Errors.PARSE,chpos,"CR Not followed bf LF");
		} else {
		    if (lastEOL > 0) {
			xt.sbuff.setLength(lastEOL+1);
		    }
		    onlySpace = false;
		    spaceAfter = XDOM.EMPTY;
		    last = xt.sbuff.length();
		    lastEOL = -1;
		}
		if (! onlySpace) {
		    xt.sbuff.append(ch);
		    if (ch == '\t') expandTab();
		}
	    }
	    pos.lastLine = chpos.firstLine;
	    pos.lastChar = chpos.firstChar;
	    getChar();
	}
	if (keepSpace) {
	    last = xt.sbuff.length();
	    spaceAfter = XDOM.EMPTY;
	}
	if (last >= 0) {
	    xt.sbuff.setLength(last+1);
	    val = xt.sbuff.toString().intern();
	    kind = STRING;
	    return true;
	}
	return false;
    }

    /**
     ** Called by {@link #getToken} when {@link #mode} is
     ** {@link #XML}.
     */
    private void getTokenXML() {
	while (true) {
	    pos.firstLine = chpos.firstLine;
	    pos.firstChar = chpos.firstChar;
	    pos.lastLine = chpos.firstLine;
	    pos.lastChar = chpos.firstChar;
	    if (eof) {
		kind = EOF;
		return;
	    }
	    switch (ch) {
		case '<' :
		{
		    openLine = chpos.firstLine;
		    openChar = chpos.firstChar;
		    headLine = openLine;
		    headChar = openChar;
		    getChar();
		    if (ch == '/') {
			getChar();
			if (getName("")) {
			    kind = XEND;
			    if (ch != '>') {
				errors.error(Errors.PARSE,chpos,"missing >");
			    } else {
				pos.lastChar = chpos.firstChar;
				getChar();
			    }
			    return;
			} else {
			    errors.error(Errors.PARSE,chpos,"missing name after </");
			}
		    } else if (ch == '@') {
			getChar();
			if (getXName("")) {
			    mode = NORMAL;
			    kind = CNAME;
			    return;
			}
			errors.error(Errors.PARSE,chpos,"missing name after <!");
		    } else if (ch == '?') {
			getChar();
			if (getName("")) {
			    mode = XMLHEAD;
			    kind = XMLSTART;
			    return;
			}
			errors.error(Errors.PARSE,chpos,"missing name after <?");
		    } else if (ch == '!') {
			getChar();
			getXComment();
			return;
		    } else {
			if (getName("")) {
			    mode = XMLHEAD;
			    kind = XSTART;
			    return;
			}
			errors.error(Errors.PARSE,chpos,"missing name after <");
		    }
		    break;
		}
		default:
		{
		    if (ch == '&' && nextCh != '#') {
			getChar();
			if (getName("")) {
			    kind = NAME;
			    if (ch != ';') {
				errors.error(Errors.PARSE,chpos,"missing ;");
			    } else {
				pos.lastChar = chpos.firstChar;
				getChar();
			    }
			    return;
			} else {
			    errors.error(Errors.PARSE,chpos,"missing name after &");
			}
			break;
		    } else {
			if (bodyString()) return;
		    }
		}
	    }
	}
    }

    /**
     ** Gets an XACT short comment.
     ** The starting // has already been consumed before the call.
     */
    private void getShort() {
	int last = 0;
	xt.sbuff.setLength(0);
	while (true) {
	    if (ch == '\r') {
		if (nextCh != '\n') errors.error(Errors.PARSE,chpos,"CR Not followed bf LF");
	    } else if (ch == '\n' || eof) {
		break;
	    } else {
		xt.sbuff.append(ch);
		if (ch == ' ' || ch == '\t') {
		    if (ch == '\t') expandTab();
		} else {
		    last = xt.sbuff.length();
		    pos.lastLine = chpos.firstLine;
		    pos.lastChar = chpos.firstChar;
		}
	    }
	    getChar();
	}
	xt.sbuff.setLength(last);
	val = xt.sbuff.toString().intern();
	kind = SHORT;
    }

    /**
     ** Gets an XACT long comment.
     ** The starting /* has already been consumed before the call.
     */
    private void getLong() {
	int last = 0;
	int start = -1;
	xt.sbuff.setLength(0);
	while (true) {
	    if (eof) {
		XPos openPos = new XPos(openLine,openChar,
					chpos.firstLine,chpos.firstChar,chpos.fpos);
		errors.error(Errors.PARSE,openPos,"no closing */");
		break;
	    } else if (ch == '*' && nextCh == '/') {
		getChar();
		getChar();
		break;
	    } else if (ch == '\r') {
		if (nextCh != '\n') errors.error(Errors.PARSE,chpos,"CR Not followed bf LF");
	    } else {
		if (ch == '\n') {
		    xt.sbuff.setLength(last);
		    start = xt.sbuff.length()+1;
		    last = start;
		} else if (ch == ' ' || ch == '\t') {
		} else {
		    if (start != -1) xt.sbuff.setLength(start);
		    last = xt.sbuff.length()+1;
		    start = -1;
		}
		pos.lastLine = chpos.firstLine;
		pos.lastChar = chpos.firstChar;
		xt.sbuff.append(ch);
		if (ch == '\t') expandTab();
	    }
	    getChar();
	}
	if (last == start) xt.sbuff.setLength(last);
	val = xt.sbuff.toString().intern();
	kind = LONG;
    }

    /**
     ** Gets an XML comment.
     ** The starting &lt;! has already been consumed before the call.
     */
    private void getXComment() {
	if (ch == '-') {
	    getChar();
	    if (ch == '-') {
		getChar();
	    } else {
		errors.error(Errors.PARSE,chpos,"missing -");
	    }
	} else {
	    errors.error(Errors.PARSE,chpos,"missing -");
	}
	int dashCnt = 0;
	int last = 0;
	int start = -1;
	xt.sbuff.setLength(0);
	while (true) {
	    if (eof) {
		XPos openPos = new XPos(openLine,openChar,
					chpos.firstLine,chpos.firstChar,chpos.fpos);
		errors.error(Errors.PARSE,openPos,"no closing -->");
		break;
	    } else if (ch == '>' && dashCnt >= 0) {
		if (dashCnt != 2) {
		    errors.error(Errors.PARSE,chpos,"too many -'s");
		}
		int size = xt.sbuff.length();
		xt.sbuff.setLength(size-2);
		break;
	    } else if (ch == '\r') {
		if (nextCh != '\n') errors.error(Errors.PARSE,chpos,"CR Not followed bf LF");
	    } else {
		if (ch == '-') {
		    dashCnt ++;
		} else {
		    dashCnt = 0;
		}
		if (ch == '\n') {
		    if (! keepSpace) xt.sbuff.setLength(last);
		    start = xt.sbuff.length()+1;
		    last = start;
		} else if (ch == ' ' || ch == '\t') {
		} else {
		    if (! keepSpace && start != -1) xt.sbuff.setLength(start);
		    last = xt.sbuff.length()+1;
		    start = -1;
		}
		xt.sbuff.append(ch);
		if (!keepSpace && ch == '\t') expandTab();
	    }
	    getChar();
	}
	pos.lastLine = chpos.firstLine;
	pos.lastChar = chpos.firstChar;
	if (!keepSpace && last == start) xt.sbuff.setLength(last);
	val = xt.sbuff.toString().intern();
	getChar();
	kind = XCOMMENT;
    }

    /**
     ** Called by {@link #getToken} when {@link #mode} is
     ** {@link #NORMAL} or {@link #CMD}.
     ** @param isCmd true for {@link #CMD} and false for {@link
     ** #NORMAL}.
     */
    private void getTokenNormal(boolean isCmd) {
	mode = NORMAL; /* turn off CMD mode for later tokens */

	while (true) {
	    pos.firstLine = chpos.firstLine;
	    pos.firstChar = chpos.firstChar;
	    pos.lastLine = chpos.firstLine;
	    pos.lastChar = chpos.firstChar;
	    if (eof) {
		kind = EOF;
		return;
	    }
	    switch (ch) {
		case '(' : kind = LPAREN; getChar(); return;
		case ')' : kind = RPAREN; getChar(); return;
		case '[' : kind = LSQUARE; getChar(); return;
		case ']' : kind = RSQUARE; getChar(); return;
		case '{' : kind = LBRACE; getChar(); return;
		case '}' : kind = RBRACE; getChar(); return;
		case '=' :
		{
		    getChar();
		    kind = ONAME;
		    if (ch == '=') {
			pos.lastChar = chpos.firstChar;
			getChar();
			val = "Eq";
		    } else {
			val= "Equal";
		    }
		    return;
		}
		case '#' : kind = HASH; getChar(); return;
		case '~' : kind = ONAME; val="Tilde"; getChar(); return;
		case '?' : kind = ONAME; val = "Question"; getChar(); return;
		case ',' : kind = COMMA; getChar(); return;
		case ';' : kind = SEMI; getChar(); return;
		case '%' : kind = ONAME; val = "Percent"; getChar(); return;
		case '^' : kind = ONAME; val = "Xor"; getChar(); return;
		case '|' : kind = ONAME; val = "Or"; getChar(); return;
		case '&' : kind = ONAME; val = "And"; getChar(); return;
		case '-' : kind = ONAME; val = "Sub"; getChar(); return;
		case '+' :
		{
		    kind = ONAME;
		    getChar();
		    if (ch == '+') {
			pos.lastChar = chpos.firstChar;
			getChar();
			val = "Cat";
		    } else {
			val = "Add";
		    }
		    return;
		}
		case '*' :
		    {
			kind = ONAME;
			getChar();
			if (ch == '*') {
			    getChar();
			    val = "XDOMValue";
			} else {
			    val = "Mult";
			}
			return;
		    }
		case '.' :
		{
		    getChar();
		    if (ch == '.') {
			pos.lastChar = chpos.firstChar;
			getChar();
			kind = ONAME;
			val = "Range";
		    } else {
			kind = DOT;
		    }
		    return;
		}
		case '!' :
		{
		    kind = ONAME;
		    getChar();
		    if (ch == '=') {
			pos.lastChar = chpos.firstChar;
			getChar();
			val = "Ne";
		    } else {
			val = "Not";
		    }
		    return;
		}
		case ':' :
		{
		    getChar();
		    pos.lastChar = chpos.firstChar;
		    kind = ONAME;
		    val = "Assign";
		    if (ch == '=') {
			pos.lastChar = chpos.firstChar;
			getChar();
		    } else {
			errors.error(Errors.PARSE,chpos,"missing =");
		    }
		    return;
		}
		case ' ': case '\t':
		{
		    if (spaceBefore == XDOM.EMPTY) {
			spaceBefore = XDOM.SPACE;
		    }
		    getChar();
		    break;
		}
		case '\r':
		{
		    if (nextCh != '\n') errors.error(Errors.PARSE,chpos,"CR Not followed bf LF");
		    getChar();
		    break;
		}
		case '\n':
		{
		    if (spaceBefore == XDOM.LINES) {
		    } else if (spaceBefore == XDOM.LINE) {
			spaceBefore = XDOM.LINES;
		    } else {
			spaceBefore = XDOM.LINE;
		    }
		    getChar();
		    break;
		}
		case '@' :
		{
		    getChar();
		    if (getXName("")) {
			kind = CNAME;
			return;
		    }
		    errors.error(Errors.PARSE,chpos,"missing name after !");
		}
		case '<' :
		{
		    openLine = chpos.firstLine;
		    openChar = chpos.firstChar;
		    if (isCmd) {
			headLine = openLine;
			headChar = openChar;
			getChar();
			if (ch == '?') {
			    getChar();
			    if (getName("")) {
				mode = XMLHEAD;
				kind = XMLSTART;
				return;
			    }
			    errors.error(Errors.PARSE,chpos,"missing name after <?");
			} else if (ch == '!') {
			    getChar();
			    getXComment();
			} else {
			    if (getName("")) {
				mode = XMLHEAD;
				kind = XSTART;
				return;
			    }
			    errors.error(Errors.PARSE,chpos,"missing name after <");
			}
		    } else {
			kind = ONAME;
			getChar();
			if (ch == '=') {
			    pos.lastChar = chpos.firstChar;
			    getChar();
			    val = "LessEq";
			} else {
			    val = "Less";
			}
			return;
		    }
		}
		case '>' :
		{
		    kind = ONAME;
		    getChar();
		    if (ch == '=') {
			pos.lastChar = chpos.firstChar;
			getChar();
			val = "GreaterEq";
		    } else {
			val = "Greater";
		    }
		    return;
		}
		case '"' :
		{
		    openLine = chpos.firstLine;
		    openChar = chpos.firstChar;
		    getChar();
		    getString(false,'"');
		    return;
		}
		case '\'' :
		{
		    openLine = chpos.firstLine;
		    openChar = chpos.firstChar;
		    getChar();
		    getString(false,'\'');
		    return;
		}
		case '/':
		{
		    openLine = chpos.firstLine;
		    openChar = chpos.firstChar;
		    getChar();
		    if (ch == '>') {
			pos.lastChar = chpos.firstChar;
			getChar();
			kind = XRIGHTE;
			mode = XML;
		    } else if (ch == '+') {
			pos.lastChar = chpos.firstChar;
			kind = ONAME;
			getChar();
			val = "Rem";
		    } else if (ch == '/') {
			getChar();
			getShort();
		    } else if (ch == '*') {
			getChar();
			getLong();
		    } else {
			kind = ONAME;
			val = "Div";
		    }
		    return;
		}
		default:
		{
		    if (getXName("")) {
			if (ch == ':' && nextCh == '/' && isCmd) {
			    kind = CMTNAME;
			    getChar();
			    return;
			}
			kind = NAME;
			return;
		    }
		    if (getNumber()) {
			kind = NUMBER;
			return;
		    }
		    errors.error(Errors.PARSE,chpos,"unexpected character");
		    getChar();
		}
	    }
	}
    }

    /**
     ** Sets the processing mode for the following tokens in the stream.
     ** 
     ** @param xml true (XML syntax) or false (XACT syntax)
     */
    public void setXML(boolean xml) {
	if (xml) {
	    mode = XML;
	} else {
	    mode = NORMAL;
	}
    }

    /**
     ** Sets the mode to command (where < is an operator not an XML
     ** tag).
     */
    public void setCmd() {
	mode = CMD;
    }

    /**
     ** Gets the next token in the stream.
     ** After a call, the token values
     ** will be in {@link #kind}, {@link #pos}, {@link #val}, {@link #more},
     ** {@link #spaceBefore} and {@link #singleQuote}.
     */
    public void getToken() {
	pos.firstLine = chpos.firstLine;
	pos.firstChar = chpos.firstChar;
	val = null;
	singleQuote = false;
	spaceBefore = spaceAfter;
	spaceAfter = XDOM.EMPTY;
	switch (mode) {
	    case NORMAL: { getTokenNormal(false); break; }
	    case CMD: { getTokenNormal(true); break; }
	    case XML: { getTokenXML(); break; }
	    case XMLHEAD: { getTokenXMLHEAD(); break; }
	    case CSTRING: { getString(false,'"'); break; }
	    case CSTRINGA: { getString(false,'\''); break; }
	    case XSTRING: { getString(true,'"'); break; }
	    case XSTRINGA: { getString(true,'\''); break; }
	}
	if (debugLexer) {
	    System.out.print("token("+mname[mode]+")="+kname[kind]);
	    if (val != null) {
		System.out.print(" '"+val+"'");
	    }
	    System.out.print("["+pos.firstLine+":"+pos.firstChar+"]");
	    System.out.print("["+pos.lastLine+":"+pos.lastChar+"]");
	    System.out.print("    "+XDOM.spaceKind[spaceBefore]);
	    System.out.println("");
	}
    }

    /**
     ** Ends the lexical analysis of a stream.
     ** Must be called after all tokens are processed
     ** ({@link #getToken} finds {@link #EOF}).
     */
    public void finish() {
	try {
	    if (r != null) r.close();
	}
	catch(Exception e) {
	    errors.fail(e,"lexer finish");
	}
    }
}
