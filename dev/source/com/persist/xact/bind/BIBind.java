/*******************************************************************************
*
* Copyright (c) 2002-2006. John R. Nestor. All rights reserved.
*
* See the file XLICENSE.txt for use and redistribution information.
*
*******************************************************************************/

package com.persist.xact.bind;

import com.persist.xact.value.*;

public final class BIBind {
    public XScope biscope;

    private Value value;

    public void define(String s,Object oval,long ival) {
	XDefVal def = new XDefVal();
	def.name = s;
	def.oval = oval;
	def.ival = ival;
	biscope.add(def);
    }

    private void defineE(String s,Object oval,long ival) {
	XDefVal def = new XDefVal();
	def.name = s;
	def.oval = oval;
	def.ival = ival;
	def.hasExt = true;
	biscope.add(def);
    }

    public VLBIF Band = new VLBIF(VLBIF.Band,true,true,2,false);
    public VLBIF Bor = new VLBIF(VLBIF.Bor,true,true,2,false);
    public VLBIF Bxor = new VLBIF(VLBIF.Bxor,true,true,2,false);
    public VLBIF Bnot = new VLBIF(VLBIF.Bnot,true,false,1,false);

    public VLBIF DTeq = new VLBIF(VLBIF.DTeq,true,false,2,false);
    public VLBIF DTne = new VLBIF(VLBIF.DTne,true,false,2,false);
    public VLBIF DTls = new VLBIF(VLBIF.DTls,true,false,2,false);
    public VLBIF DTle = new VLBIF(VLBIF.DTle,true,false,2,false);
    public VLBIF DTge = new VLBIF(VLBIF.DTge,true,false,2,false);
    public VLBIF DTgt = new VLBIF(VLBIF.DTgt,true,false,2,false);
    public VLBIF DTsub = new VLBIF(VLBIF.DTsub,true,false,2,false);
    public VLBIF DTnow = new VLBIF(VLBIF.DTnow,true,false,0,false);
    public VLBIF DTgmt = new VLBIF(VLBIF.DTgmt,true,false,0,false);
    public VLBIF DTadd = new VLBIF(VLBIF.DTadd,true,true,0,false);

    public VLBIF Iadd = new VLBIF(VLBIF.Iadd,true,true,2,false);
    public VLBIF Isub = new VLBIF(VLBIF.Isub,true,false,2,false);
    public VLBIF Iminus = new VLBIF(VLBIF.Iminus,true,false,1,false);
    public VLBIF Imult = new VLBIF(VLBIF.Imult,true,true,2,false);
    public VLBIF Idiv = new VLBIF(VLBIF.Idiv,true,false,2,false);
    public VLBIF Irem = new VLBIF(VLBIF.Irem,true,false,2,false);
    public VLBIF Ieq = new VLBIF(VLBIF.Ieq,true,false,2,false);
    public VLBIF Ine = new VLBIF(VLBIF.Ine,true,false,2,false);
    public VLBIF Ils = new VLBIF(VLBIF.Ils,true,false,2,false);
    public VLBIF Ile = new VLBIF(VLBIF.Ile,true,false,2,false);
    public VLBIF Ige = new VLBIF(VLBIF.Ige,true,false,2,false);
    public VLBIF Igt = new VLBIF(VLBIF.Igt,true,false,2,false);

    public VLBIF Fadd = new VLBIF(VLBIF.Fadd,true,true,2,false);
    public VLBIF Fsub = new VLBIF(VLBIF.Fsub,true,false,2,false);
    public VLBIF Fminus = new VLBIF(VLBIF.Fminus,true,false,1,false);
    public VLBIF Fmult = new VLBIF(VLBIF.Fmult,true,true,2,false);
    public VLBIF Fdiv = new VLBIF(VLBIF.Fdiv,true,false,2,false);
    public VLBIF Feq = new VLBIF(VLBIF.Feq,true,false,2,false);
    public VLBIF Fne = new VLBIF(VLBIF.Fne,true,false,2,false);
    public VLBIF Fls = new VLBIF(VLBIF.Fls,true,false,2,false);
    public VLBIF Fle = new VLBIF(VLBIF.Fle,true,false,2,false);
    public VLBIF Fge = new VLBIF(VLBIF.Fge,true,false,2,false);
    public VLBIF Fgt = new VLBIF(VLBIF.Fgt,true,false,2,false);

    public VLBIF Xeq = new VLBIF(VLBIF.Xeq,true,false,2,false);
    public VLBIF Xne = new VLBIF(VLBIF.Xne,true,false,2,false);
    public VLBIF Sls = new VLBIF(VLBIF.Sls,true,false,2,false);
    public VLBIF Sle = new VLBIF(VLBIF.Sle,true,false,2,false);
    public VLBIF Sge = new VLBIF(VLBIF.Sge,true,false,2,false);
    public VLBIF Sgt = new VLBIF(VLBIF.Sgt,true,false,2,false);
    public VLBIF Scat = new VLBIF(VLBIF.Scat,true,false,2,false);
    public VLBIF SFeq = new VLBIF(VLBIF.SFeq,true,false,2,false);
    public VLBIF SFne = new VLBIF(VLBIF.SFne,true,false,2,false);
    public VLBIF SFls = new VLBIF(VLBIF.SFls,true,false,2,false);
    public VLBIF SFle = new VLBIF(VLBIF.SFle,true,false,2,false);
    public VLBIF SFge = new VLBIF(VLBIF.SFge,true,false,2,false);
    public VLBIF SFgt = new VLBIF(VLBIF.SFgt,true,false,2,false);

    public VLBIF add = new VLBIF(VLBIF.add,true,true,0,false);
    public VLBIF sub = new VLBIF(VLBIF.sub,true,true,2,false);
    public VLBIF mult = new VLBIF(VLBIF.mult,true,true,0,false);
    public VLBIF div = new VLBIF(VLBIF.div,true,true,2,false);
    public VLBIF rem = new VLBIF(VLBIF.rem,true,true,2,false);
    public VLBIF minus = new VLBIF(VLBIF.minus,true,true,1,false);
    public VLBIF cat = new VLBIF(VLBIF.cat,true,true,0,false);
    public VLBIF and = new VLBIF(VLBIF.and,true,true,0,false);
    public VLBIF or = new VLBIF(VLBIF.or,true,true,0,false);
    public VLBIF xor = new VLBIF(VLBIF.xor,true,true,2,false);
    public VLBIF not = new VLBIF(VLBIF.not,true,true,1,false);
    public VLBIF eq = new VLBIF(VLBIF.eq,true,true,2,false);
    public VLBIF ne = new VLBIF(VLBIF.ne,true,true,2,false);
    public VLBIF ls = new VLBIF(VLBIF.ls,true,true,2,false);
    public VLBIF le = new VLBIF(VLBIF.le,true,true,2,false);
    public VLBIF gt = new VLBIF(VLBIF.gt,true,true,2,false);
    public VLBIF ge = new VLBIF(VLBIF.ge,true,true,2,false);
    public VLBIF cover = new VLBIF(VLBIF.cover,true,true,2,false);

    public VLBIF assign = new VLBIF(VLBIF.assign,true,false,2,false);
    public VLBIF tilde = new VLBIF(VLBIF.tilde,true,false,2,false);
    public VLBIF Assert = new VLBIF(VLBIF.Assert,true,false,1,false);
    public VLBIF question = new VLBIF(VLBIF.question,true,false,2,false);
    public VLBIF dot = new VLBIF(VLBIF.dot,true,true,0,true);
    public VLBIF percent = new VLBIF(VLBIF.percent,true,false,1,false);
    public VLBIF XDOMValue = new VLBIF(VLBIF.XDOMValue,true,false,1,false);
    public VLBIF equal = new VLBIF(VLBIF.equal,true,false,2,false);
    public VLBIF range = new VLBIF(VLBIF.range,true,false,2,false);
    public VLBIF rangeE = new VLBIF(VLBIF.rangeE,true,false,1,false);
    public VLBIF chars = new VLBIF(VLBIF.chars,true,false,1,false);
    public VLBIF lines = new VLBIF(VLBIF.lines,true,false,1,false);
    public VLBIF comment = new VLBIF(VLBIF.comment,true,false,1,false);
    public VLBIF URLParseQuery = new VLBIF(VLBIF.URLParseQuery,true,false,1,false);
    public VLBIF URLUnparseQuery = new VLBIF(VLBIF.URLUnparseQuery,true,false,1,false);

    public VLBIF read = new VLBIF(VLBIF.read,true,true,0,false);
    public VLBIF write = new VLBIF(VLBIF.write,true,true,0,false);
    public VLBIF append = new VLBIF(VLBIF.append,true,false,1,false);
    public VLBIF exists = new VLBIF(VLBIF.exists,true,false,0,false);
    public VLBIF create = new VLBIF(VLBIF.create,true,false,0,false);
    public VLBIF delete = new VLBIF(VLBIF.delete,true,false,0,false);
    public VLBIF isDirectory = new VLBIF(VLBIF.isDirectory,true,false,0,false);
    public VLBIF createDirectory = new VLBIF(VLBIF.createDirectory,true,false,0,false);
    public VLBIF files = new VLBIF(VLBIF.files,true,false,1,false);
    public VLBIF rename = new VLBIF(VLBIF.rename,true,false,1,false);
    public VLBIF parse = new VLBIF(VLBIF.parse,true,true,1,false);
    public VLBIF bind = new VLBIF(VLBIF.bind,true,true,1,false);
    public VLBIF exec = new VLBIF(VLBIF.exec,true,true,1,false);
    public VLBIF Wexp = new VLBIF(VLBIF.Wexp,true,true,1,false);
    public VLBIF Wbody = new VLBIF(VLBIF.Wbody,true,true,1,false);
    public VLBIF Wassign = new VLBIF(VLBIF.Wassign,true,true,1,false);
    public VLBIF Wdefine = new VLBIF(VLBIF.Wdefine,true,true,1,false);
    public VLBIF Wother = new VLBIF(VLBIF.Wother,true,true,1,false);
    public VLBIF error = new VLBIF(VLBIF.error,true,true,1,false);
    public VLBIF warn = new VLBIF(VLBIF.warn,true,true,1,false);
    public VLBIF log = new VLBIF(VLBIF.log,true,true,1,false);
    public VLBIF type = new VLBIF(VLBIF.type,false,false,0,false);
    public VLBIF dobreak = new VLBIF(VLBIF.dobreak,false,false,0,false);
    public VLBIF langCurrent = new VLBIF(VLBIF.langcurrent,false,false,0,false);

    public VLBIF ArrayInsert = new VLBIF(VLBIF.ArrayInsert,true,true,0,false);
    public VLBIF ArrayDelete = new VLBIF(VLBIF.ArrayDelete,true,false,1,false);
    public VLBIF ArrayAll = new VLBIF(VLBIF.ArrayAll,true,false,1,false);
    public VLBIF BodySubscript = new VLBIF(VLBIF.BodySubscript,true,false,1,true);
    public VLBIF BodyInsert = new VLBIF(VLBIF.BodyInsert,true,true,0,false);
    public VLBIF BodyDelete = new VLBIF(VLBIF.BodyDelete,true,false,1,false);
    public VLBIF BodyAll = new VLBIF(VLBIF.BodyAll,true,false,1,false);
    public VLBIF AttrSubscript = new VLBIF(VLBIF.AttrSubscript,true,false,1,true);
    public VLBIF AttrInsert = new VLBIF(VLBIF.AttrInsert,true,true,0,false);
    public VLBIF AttrDelete = new VLBIF(VLBIF.AttrDelete,true,false,1,false);
    public VLBIF AttrAll = new VLBIF(VLBIF.AttrAll,true,false,1,false);
    public VLBIF ArgInsert = new VLBIF(VLBIF.ArgInsert,true,true,0,false);
    public VLBIF ArgDelete = new VLBIF(VLBIF.ArgDelete,true,false,1,false);
    public VLBIF ArgAll = new VLBIF(VLBIF.ArgAll,true,false,1,false);
    public VLBIF ExtInsert = new VLBIF(VLBIF.ExtInsert,true,true,0,false);
    public VLBIF ExtDelete = new VLBIF(VLBIF.ExtDelete,true,false,1,false);
    public VLBIF ExtAll = new VLBIF(VLBIF.ExtAll,true,false,1,false);

    public VLBIF QueueSend = new VLBIF(VLBIF.QueueSend,true,false,1,false);
    public VLBIF QueueReceive = new VLBIF(VLBIF.QueueReceive,true,false,0,false);
    public VLBIF LockWait = new VLBIF(VLBIF.LockWait,true,true,0,false);
    public VLBIF LockNotify = new VLBIF(VLBIF.LockNotify,true,false,0,false);
    public VLBIF LockNotifyAll = new VLBIF(VLBIF.LockNotifyAll,true,false,0,false);
    public VLBIF StreamSend = new VLBIF(VLBIF.StreamSend,true,false,1,false);
    public VLBIF StreamClose = new VLBIF(VLBIF.StreamClose,true,false,0,false);
    public VLBIF StreamReceive = new VLBIF(VLBIF.StreamReceive,true,true,0,false);

    public BIBind(Value value) {
      this.value = value;
      biscope = new XScope();
      biscope.level = -1;

      define("And",and,0);
      define("Or",or,0);
      define("Xor",xor,0);
      define("Not",not,0);
      
      define("Add",add,0);
      define("Sub",sub,0);
      define("Minus",minus,0);
      define("Mult",mult,0);
      define("Div",div,0);
      define("Rem",rem,0);

      define("Cat",cat,0);
      define("Cover",cover,0);

      define("Assign",assign,0);
      define("Tilde",tilde,0);
      define("Assert",Assert,0);
      define("Question",question,0);
      define("Dot",dot,0);
      defineE("Dot",dot,0);
      define("Percent",percent,0);
      define("XDOMValue",XDOMValue,0);
      define("Equal",equal,0);
      define("Range",range,0);
      defineE("Range",rangeE,0);
      defineE("Chars",chars,0);
      defineE("Lines",lines,0);
      define("Comment",comment,0);
      define("URLParseQuery",URLParseQuery,0);
      define("URLUnparseQuery",URLUnparseQuery,0);

      define("Parse",parse,0);
      define("Bind",bind,0);
      define("Exec",exec,0);
      define("walk:exp",Wexp,0);
      define("walk:body",Wbody,0);
      define("walk:assign",Wassign,0);
      define("walk:other",Wother,0);
      define("walk:define",Wdefine,0);

      define("Error",error,0);
      define("Warn",warn,0);
      define("Log",log,0);
      defineE("Type",type,0);
      define("doBreak",dobreak,0);
      define("lang:current",langCurrent,0);

      define("Eq",eq,0);
      define("Ne",ne,0);
      define("Less",ls,0);
      define("LessEq",le,0);
      define("Greater",gt,0);
      define("GreaterEq",ge,0);

      define("string",value.vlBIT,VLBIV.Tstring);
      define("fold",value.vlBIT,VLBIV.Vfold);
      define("date",value.vlBIT,VLBIV.Vdate);
      define("int",value.vlBIT,VLBIV.Tint);
      define("float",value.vlBIT,VLBIV.Tfloat);
      define("boolean",value.vlBIV,VLBIV.Vboolean);
      define("array",value.vlBIV,VLBIV.Varray);
      define("list",value.vlBIV,VLBIV.Vlist);
      define("file",value.vlBIV,VLBIV.Vfile);
      define("rec",value.vlBIV,VLBIV.Vrec);
      define("bodyrec",value.vlBIV,VLBIV.Vbodyrec);
      define("attr",value.vlBIV,VLBIV.Vattr);
      define("body",value.vlBIV,VLBIV.Vbody);
      define("xdom",value.vlBIV,VLBIV.Vtree);
      define("xdom:element",value.vlBIT,VLBIV.TtreeElement);
      define("xdom:string",value.vlBIT,VLBIV.TtreeString);
      define("xdom:int",value.vlBIT,VLBIV.TtreeInt);
      define("xdom:float",value.vlBIT,VLBIV.TtreeFloat);
      define("xdom:name",value.vlBIT,VLBIV.TtreeName);
      define("xdom:call",value.vlBIT,VLBIV.TtreeCall);
      define("xdom:value",value.vlBIT,VLBIV.TtreeValue);

      define("thread",value.vlBIT,VLBIV.Tthread);
      define("queue",value.vlBIT,VLBIV.Tqueue);
      define("lock",value.vlBIT,VLBIV.Tlock);
      define("stream",value.vlBIT,VLBIV.Tstream);
      define("langtype:xact",value.vlBIT,VLBIV.TLxact);
      define("langtype:render",value.vlBIT,VLBIV.TLrender);
      define("langtype:error",value.vlBIT,VLBIV.TLerror);
      
      define("void",value.vlBIV,VLBIV.Tvoid);
      define("func",value.vlBIV,VLBIV.Tfunc);
      define("type",value.vlBIV,VLBIV.Ttype);
      define("view",value.vlBIV,VLBIV.Tview);
      define("View",value.vlBIV,VLBIV.Vview);

      define("false",value.vlBool,0);
      define("true",value.vlBool,1);
      define("null",null,0);
      define("Java",value.vlJava,0);

      define("eol","\12",0);
      define("nbsp","\u00A0",0);
      define("quot","\"",0);
      define("apos","'",0);
      define("tab","\13",0);
      define("amp","&",0);
      define("lt","<",0);
      define("gt",">",0);
      define("empty","",0);
   }
}
