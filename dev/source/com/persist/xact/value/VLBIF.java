/*******************************************************************************
*
* Copyright (c) 2002-2006. John R. Nestor. All rights reserved.
*
* See the file XLICENSE.txt for use and redistribution information.
*
*******************************************************************************/

package com.persist.xact.value;

public final class VLBIF extends VL{

    public int kind;
    public boolean hasArgs;
    public boolean varArgs;
    public boolean lhs;
    public int args;		

    public VLBIF(int kind,boolean hasArgs,boolean varArgs,int args,boolean lhs) {
	vKind = VL.VBIF;
	this.kind = kind;
	this.hasArgs = hasArgs;	
	this.varArgs = varArgs;
	this.args = args;
	this.lhs = lhs;
    }

    public final static int Iadd = 0;
    public final static int Isub = 1;
    public final static int Imult = 2;
    public final static int Idiv = 3;
    public final static int assign = 4;
    public final static int cat = 5;
    public final static int Xeq = 6;
    public final static int Xne = 7;
    public final static int Sls = 8;
    public final static int Sle = 9;
    public final static int Sgt = 10;
    public final static int Sge = 11;
    public final static int eq = 12;
    public final static int ne = 13;
    public final static int ls = 14;
    public final static int le = 15;
    public final static int gt = 16;
    public final static int ge = 17;
    public final static int Ieq = 18;
    public final static int Ine = 19;
    public final static int Ils = 20;
    public final static int Ile = 21;
    public final static int Igt = 22;
    public final static int Ige = 23;
    public final static int Band = 24;
    public final static int Bor = 25;
    public final static int Bxor = 26;
    public final static int Bnot = 27;
    public final static int tilde = 28;
    public final static int question = 29;
    public final static int Irem = 30;
    public final static int Assert = 31;
    public final static int Iminus = 32;
    public final static int dot = 33;
    public final static int Fminus = 34;
    /*
    public final static int Bsize = 35;
    */
    public final static int unparse = 36;
    public final static int error = 37;

    public final static int Fadd = 38;
    public final static int Fsub = 39;
    public final static int Fmult = 40;
    public final static int Fdiv = 41;
    public final static int Feq = 42;
    public final static int Fne = 43;
    public final static int Fls = 44;
    public final static int Fle = 45;
    public final static int Fgt = 46;
    public final static int Fge = 47;
    public final static int type = 48;
    public final static int BodySubscript = 49;
    public final static int BodyInsert = 50;
    public final static int BodyDelete = 51;
    public final static int AttrSubscript = 52;
    public final static int AttrInsert = 53;
    public final static int AttrDelete = 54;
    public final static int percent = 55;
    public final static int equal = 56;
    public final static int ArgInsert = 57;
    public final static int ArgDelete = 58;
    public final static int ExtInsert = 59;
    public final static int ExtDelete = 60;
    public final static int ArrayInsert = 61;
    public final static int ArrayDelete = 62;
    public final static int ArrayAll = 63;
    public final static int AttrAll = 64;
    public final static int BodyAll = 65;
    public final static int ExtAll = 66;
    public final static int ArgAll = 67;
    public final static int read = 68;
    public final static int write = 69;
    public final static int append = 70;
    public final static int exists = 71;
    public final static int create = 72;
    public final static int delete = 73;
    public final static int isDirectory = 74;
    public final static int createDirectory = 75;
    public final static int files = 76;
    public final static int rename = 77;
    public final static int parse = 78;
    public final static int QueueSend = 79;
    public final static int QueueReceive = 80;
    public final static int LockWait = 81;
    public final static int LockNotify = 82;
    public final static int LockNotifyAll = 83;
    public final static int range = 84;
    public final static int rangeE = 85;
    public final static int StreamSend = 86;
    public final static int StreamClose = 87;
    public final static int StreamReceive = 88;
    public final static int chars = 89;
    public final static int lines = 90;
    public final static int SFeq = 91;
    public final static int SFne = 92;
    public final static int SFls = 93;
    public final static int SFle = 94;
    public final static int SFgt = 95;
    public final static int SFge = 96;
    public final static int DTeq = 97;
    public final static int DTne = 98;
    public final static int DTls = 99;
    public final static int DTle = 100;
    public final static int DTgt = 101;
    public final static int DTge = 102;
    public final static int DTnow = 103;
    public final static int DTgmt = 104;
    public final static int DTadd = 105;
    public final static int DTsub = 106;
    public final static int comment = 107;
    public final static int URLParseQuery = 108;
    public final static int URLUnparseQuery = 109;
    public final static int warn = 110;
    public final static int log = 111;
    public final static int XDOMValue = 112;
    public final static int bind = 113;
    public final static int exec = 114;
    public final static int Wexp = 115;
    public final static int Wbody = 116;
    public final static int Wother = 117;
    public final static int Wassign = 118;
    public final static int Wdefine = 119;
    public final static int add = 120;
    public final static int sub = 121;
    public final static int mult = 122;
    public final static int div = 123;
    public final static int rem = 124;
    public final static int minus = 125;
    public final static int Scat = 126;
    public final static int and = 127;
    public final static int or = 128;
    public final static int xor = 129;
    public final static int not = 130;
    public final static int cover = 131;
    public final static int dobreak = 132;
    public final static int langcurrent = 133;
    
    public final static String[] name = {
	"int.sys:Add", "int.sys:Sub", "int.sys:Mult", "int.sys:Div", "Assign",
	"Cat", "string.sys:Eq", "string.sys:Ne", "string.sys:Less", "string.sys:LessEqual",
	"string.sys:Greater", "string.sys:GreaterEq",
	"Eq", "Ne", "Less", "LessEq", "Greater", "GreaterEq", 
	"int.sys:Eq", "int.sys:Ne", "int.sys:Less", "int.sys:LessEq",
	"int.sys:Greater", "int.sys:GreaterEq",
	"boolean.sys:And", "boolean.sys:Or", "boolean:sys:Xor", "boolean:sys:Not",
	"Tilde", "Question", "int.sys:Rem", "Assert", "int.sys:Minus",
	"Dot", "float.sys:Minus", "", "Unparse", "Error",
	"float.sys:Add", "float.sys:Sub", "float.sys:Mult", "float.sys:Div",
	"float.sys:Eq", "float.sys:Ne", "float.sys:Less", "float.sys:LessEq",
	"float.sys:Greater", "float.sys:GreaterEq",
	"Type",
	"subscript#body", "insert#body", "delete#body",
	"subscript#attr", "insert#attr", "delete#attr",
	"Percent", "Equal", 
	"insert#xdom:call", "delete#xdom:call",
	"insert#xdom:name", "delete#xdom:name",
	"insert#array", "delete#array",
	"all#array", "all#attr", "all#body", "all#xdom:name", "all#xdom:call",
	"read#file", "write#file", "append#file", "exists#file", "create#file", "delete#file",
	"isDirectory#file", "createDirectory#file", "files#file", "rename#file",
	"Parse",
	"send#queue", "receive#queue", "wait#lock", "notify#lock", "notifyAll#lock",
	"Range", "Range",
	"send#stream", "close#stream", "receive#stream",
	"Chars", "Lines",
	"fold.sys:Eq", "fold.sys:Ne", "fold.sys:Less", "fold.sys:LessEq", "fold.sys:Greater", "fold.sys:GreaterEq",
	"date.sys:Eq", "date.sys:Ne", "date.sys:Less", "date.sys:LessEq", "date.sys:Greater", "date.sys:GreaterEq",
	"now#date", "gmt#date", "date.Add", "date:sys:Sub",
	"Comment", "URLParseQuery", "URLUnparseQuery", "Warn", "Log", "XDOMValue",
	"Bind", "Exec", "walk:exp", "walk:body", "walk:other", "walk:assign", "walk:define",
	"Add","Sub","Mult","Div","Rem","Minus", "string:sys:Cat",
	"And","Or","Xor","Not", "Cover", "doBreak", "lang:current"
    };
}
