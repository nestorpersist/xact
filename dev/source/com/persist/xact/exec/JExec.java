/*******************************************************************************
*
* Copyright (c) 2002-2007 John R. Nestor. All rights reserved.
*
* See the file XLICENSE.txt for use and redistribution information.
*
*******************************************************************************/

package com.persist.xact.exec;

import java.lang.reflect.*;
import com.persist.xact.system.*;
import com.persist.xdom.*;
import com.persist.xact.value.*;

public final class JExec {
    private XThread xt;
    private Exec exec;
    private EValue evalue;
    private VariableStack stack;
    private Value value;
    private Errors errors;

    public JExec(Exec exec) {
	this.exec = exec;
    }

    public void init() {
	xt = exec.xt;
	stack = exec.stack;
	evalue = exec.evalue;
	value = xt.cmd.value;
	errors = xt.errors;
    }

    private void dumpStack(String msg,Exception e1,XPos pos) {
	Throwable e = e1;
	if (e instanceof InvocationTargetException) {
	    Throwable e2 =((InvocationTargetException) e).getCause();
	    if (e2 != null) e = e2;
	}
	errors.error(Errors.EXEC,pos,msg+": "+e);
	StackTraceElement elem[] = e.getStackTrace();
	for (int i = 0; i < elem.length; i++) {
	    String s = elem[i].toString();
	    if (s.startsWith("com.persist.xact")) break;
	    errors.error(Errors.EXEC,pos,"Stack: "+elem[i].toString());
	}
    }

    private int prefer(Class[] parms1,Class[] parms2,int argCnt) {
	int argIdx = stack.getTop() - argCnt + 1; /* first arg */
	boolean ok1 = true;
	boolean ok2 = true;
	int score1 = 0;
	int score2 = 0;
	int i;
	for (i = 0; i < argCnt; i++) {
	    Class c1 = parms1[i];
	    Class c2 = parms2[i];
	    Object oval = stack.getOval(argIdx+i);
	    long ival = stack.getIval(argIdx+i);
	    if (XDOMValue.isString(oval,ival)) {
		if (c1 == String.class) score1 ++;
		if (c2 == String.class) score2 ++;
	    } else if (value.isJObject(oval,ival)) {
		if (! c1.isInstance(oval)) ok1 = false;
		if (! c2.isInstance(oval)) ok2 = false;
	    }
	}
	if (! ok2) return 1;
	if (! ok1) return 2;
	if (score1 > score2) return 1;
	if (score2 > score1) return 2;
	return 0;
    }
    
    private Constructor findConstructor(Class c,int argCnt,XPos pos) {
	Constructor result = null;
	Constructor[] constructs = c.getConstructors();
	int size = constructs.length;
	int i;
	for (i = 0; i < size; i++) {
	    Constructor construct = constructs[i];
	    Class[] parms = construct.getParameterTypes();
	    int psize = parms.length;
	    if (psize == argCnt) {
		if (result != null) {
		    int pref = prefer(result.getParameterTypes(),parms,argCnt);
		    if (pref == 0) {
//			errors.error(Errors.EXEC,pos,
//				     "Java Class "+c.getName()+
//				     " has more than 1 matching constructor ");
//			return null;
		    } else if (pref == 2) {
			result = construct;
		    }
		} else {
		    result = construct;
		}
	    }
	}
    	if (result == null) {
	    errors.error(Errors.EXEC,pos,
			 "Java Class "+c.getName()+
			 " has no constructor "+
			 " with "+argCnt+" formal(s)");
	}
	return result;
    }

    private boolean hasMethod(Class c,String op) {
	Method[] methods = c.getMethods();
	int size = methods.length;
	int i;
	for (i = 0; i < size; i++) {
	    Method m = methods[i];
	    if (m.getName().equals(op)) {
		return true;
	    } 
	}
	return false;
    }
    
    private Method findMethod(Class c,String op,int argCnt,boolean isStatic,XPos pos) {
	String kind = "dynamic";
	if (isStatic) kind = "static";
	Method result = null;
	Method[] methods = c.getMethods();
	int size = methods.length;
	int i;
	for (i = 0; i < size; i++) {
	    Method m = methods[i];
	    if (m.getName().equals(op)) {
		Class[] parms = m.getParameterTypes();
		int mod = m.getModifiers();
		boolean isStatic1 = Modifier.isStatic(mod);
		int psize = parms.length;
		if (psize == argCnt && isStatic == isStatic1) {
		    if (result != null) {
			int pref = prefer(result.getParameterTypes(),parms,argCnt);
			if (pref == 0) {
//			    errors.error(Errors.EXEC,pos,
//					 "Java Class "+c.getName()+
//					 " has more than 1 "+kind+" matching method named "+op);
//			    return null;
			} else if (pref == 2) {
			    result = m;
			}
		    } else {
			result = m;
		    }
		}
	    }
	}
	if (result == null) {
	    errors.error(Errors.EXEC,pos,
			 "Java Class "+c.getName()+
			 " has no "+kind+" method named "+op+
			 " with "+argCnt+" formal(s)");
	} else {
	    result.setAccessible(true);
	}
	return result;
    }

    private Field findField(Class c,String name,boolean isStatic,XPos pos) {
	/*
	String kind = "dynamic";
	if (isStatic) kind = "static";
	*/
	try {
	    Field f = c.getField(name);
	    if (Modifier.isStatic(f.getModifiers())== isStatic) {
		return f;
	    }
	} catch(Exception e) {
	}
	/*
	errors.error(Errors.EXEC,pos,"Java "+kind+" field "+name+" not found in class "+c.getName());
	*/
	return null;
    }

    public void getClass(String s,VLJava vlj,XPos pos) {
	String s1 = s;
	if (vlj.pkg != "") {
	    s1 = vlj.pkg + "." + s;
	}
	try {
	    Class c = Class.forName(s1);
	    stack.push(c,-1);
	} catch(Exception e) {
//	    errors.error(Errors.EXEC,pos,"can't find Java class "+s);
//	    stack.pushError();
	    VLJava vlj1 = new VLJava();
	    vlj1.pkg = s1;
	    stack.push(vlj1,0);
	}
    }

    final static int OK = 0;
    final static int BADFROM = 1;
    final static int BADTO = 2;
    final static int BADRANGE = 3;

    private int fromJava(Object val,Class c) {
	if (val == null || c == void.class) {
	    stack.pushNull();
	    return OK;
	} else if (c == boolean.class) {
	    if (val instanceof Boolean) {
		Boolean b = (Boolean) val;
		stack.push(value.vlBool,VLBool.toI(b.booleanValue()));
		return OK;
	    } 
	} else if (c == int.class) {
	    if (val instanceof Integer) {
		Integer i = (Integer) val;
		stack.push(value.vlInt,i.longValue());
		return OK;
	    } else if (val instanceof Long) {
		Long i = (Long) val;
		stack.push(value.vlInt,i.longValue());
		return OK;
	    }
	} else if (c == double.class) {
	    if (val instanceof Double) {
		Double d = (Double) val;
		long i = Double.doubleToLongBits(d.doubleValue());
		stack.push(value.vlFloat,i);
		return OK;
	    }
	} else if (c == String.class) {
	    if (val instanceof String) {
		String s = (String) val;
		stack.push(s.intern(),0);
		return OK;
	    }
	} else {
	    stack.push(val,-1);
	    return OK;
	}
	return BADFROM;
    }

    private int toJava(Object oval,long ival,Class c) {
	if (c == boolean.class) {
	    stack.push(oval,ival);
	    if (evalue.getB((XPos) null)) {
		long ival1 = stack.getTopIval();
		boolean b = false;
		if (ival1 != 0) b = true;
		stack.pop();
		stack.push(new Boolean(b),-1);
		return OK;
	    } else {
		stack.pop();
		return BADFROM;
	    }
	} else if (c == int.class) {
	    stack.push(oval,ival);
	    if (evalue.getI((XPos) null)) {
		long ival1 = stack.getTopIval();
		stack.pop();
		if (Integer.MIN_VALUE <= ival1 && ival1 <= Integer.MAX_VALUE) {
		    stack.push(new Integer((int) ival1),-1);
		    return OK;
		} else {
		    return BADRANGE;
		}
	    } else {
		stack.pop();
		return BADFROM;
	    }
	} else if (c == double.class) {
	    stack.push(oval,ival);
	    if (evalue.getF((XPos) null)) {
		double fval = Double.longBitsToDouble(stack.getTopIval());
		stack.pop();
		stack.push(new Double(fval),-1);
		return OK;
	    } else {
		stack.pop();
		return BADFROM;
	    }
	} else if (c == String.class) {
	    if (XDOMValue.isString(oval,ival)) {
		stack.push(XDOMValue.getString(oval,ival),-1);
		return OK;
	    } else if (value.isNull(oval,ival)) {
		stack.push(null,-1);
		return OK;
	    } else {
		return BADFROM;
	    }
	} else if (value.isJObject(oval,ival)) {
	    if (c.isInstance(oval)) {
		stack.push(oval,-1);
		return OK;
	    } else {
		return BADFROM;
	    }
	} else if (value.isNull(oval,ival)) {
	    stack.push(null,-1);
	    return OK;
	} else if (c == Object.class) {
	    if (XDOMValue.isString(oval,ival)) {
		return toJava(oval,ival,String.class);
	    } else if (XDOMValue.isInt(oval,ival)) {
		return toJava(oval,ival,int.class);
	    } else if (XDOMValue.isFloat(oval,ival)) {
		return toJava(oval,ival,double.class);
	    } else {
		return BADTO;
	    }
	} else {

	    return BADTO;
	}
    }
    
    private boolean setActual(int i,Object[] actuals,Class c,
			      Object oval,long ival,XPos pos) {
	int ok = toJava(oval,ival,c);
	if (ok == OK) {
	    actuals[i] = stack.getTopOval();
	    stack.pop();
	    return true;
	}
	if (ok == BADFROM) {
	    errors.error(Errors.EXEC,pos,"Actual "+(i+1)+" does not match "+c.getName());
	} else if (ok == BADRANGE) {
	    errors.error(Errors.EXEC,pos,"Actual "+(i+1)+" out of range for "+c.getName());
	} else {
	    errors.error(Errors.EXEC,pos,"Java formal "+(i+1)+" class "+c.getName()+" not supported");
	}
	return false;
    }


    public void newCall1(Class c,int argCnt,XDOM caller) {
	Constructor construct = findConstructor(c,argCnt,caller.pos);
	if (construct != null) {
	    Class[] parms = construct.getParameterTypes();
	    Object[] actuals = new Object[argCnt];
	    int argIdx = stack.getTop() - argCnt + 1; /* first arg */
	    int top = argIdx - 1;		          /* top before call */
	    int i;
	    boolean ok = true;
	    for (i = 0; i < argCnt; i++) {
		Object oval1 = stack.getOval(argIdx+i);
		long ival1 = stack.getIval(argIdx+i);
		if (! setActual(i,actuals,parms[i],oval1,ival1,caller.pos)) ok = false;
	    }
	    stack.setTop(top);
	    if (ok) {
		Object result = null;
		try {
		    result = construct.newInstance(actuals);
		} catch (Exception e) {
		    ok = false;
		    dumpStack("Java constructor raised exception",e,caller.pos);
		}
		if (ok) {
		    int ok1 = fromJava(result,c);
		    if (ok1 == OK) {
			return;
		    }
		    errors.error(Errors.EXEC,caller.pos,
				 "Java constructor result type "+c.getName()+
				 " not supported");
		}
	    }
	}
    }

    public void newCall(String s,int argCnt,XDOM caller) {
//	getClass(s,caller.pos);
//	Object oval = stack.getTopOval();
//	stack.pop();
//	if (oval instanceof Class) {
//	    Class c = (Class) oval;
//	    newCall1(c,argCnt,caller);
//	}
	stack.pushError();
    }
    
    public void jcall(Object o,Class c,String s,int argCnt,XDOM caller) {
	boolean isStatic = o == null;
	Method m = findMethod(c,s,argCnt,isStatic,caller.pos);
	if (m != null) {
	    Class[] parms = m.getParameterTypes();
	    Class res = m.getReturnType();
	    Object[] actuals = new Object[argCnt];
	    int argIdx = stack.getTop() - argCnt + 1; /* first arg */
	    int top = argIdx - 1;		      /* top before call */
	    int i;
	    boolean ok = true;
	    for (i = 0; i < argCnt; i++) {
		Object oval = stack.getOval(argIdx+i);
		long ival = stack.getIval(argIdx+i);
		if (! setActual(i,actuals,parms[i],oval,ival,caller.pos)) ok = false;
	    }
	    stack.setTop(top);
	    if (ok) {
		Object result = null;
		try {
		    result = m.invoke(o,actuals);
		} catch (Exception e) {
		    ok = false;
		    dumpStack("Java call raised exception",e,caller.pos);
		}
		if (ok) {
		    int ok1 = fromJava(result,res);
		    if (ok1 == OK) return;
		    errors.error(Errors.EXEC,caller.pos,
				 "Java result type "+res.getName()+" not supported");
		}
	    }
	}
	stack.pushError();
    }

    public void doVar(String name,Object o,Class c,Ctx ctx,XPos pos) {
	boolean isStatic = o == null;
	Field f = findField(c,name,isStatic,pos);
	if (f != null) {
	    Class t = f.getType();
	    if (ctx == value.ctxAssign) {
		Object oval = stack.getTopOval();
		long ival = stack.getTopIval();
		int ok1 = toJava(oval,ival,t);
		if (ok1 == OK) {
		    Object val = stack.getTopOval();
		    stack.pop();
		    try {
			f.set(o,val);
		    } catch(Exception e) {
			System.out.println("field assign failed");
		    }
		    return;
		}
		if (ok1 == BADFROM) {
		    errors.error(Errors.EXEC,pos,"value to be assigned to Java field has bad value");
		} else if (ok1 == BADRANGE) {
		    errors.error(Errors.EXEC,pos,"value to be assigned to Java field is out of range");
		} else {
		    errors.error(Errors.EXEC,pos,"unsupported Java field type "+t.getName());
		}
	    } else {
		Object val = null;
		try {
		    val = f.get(o);
		} catch(Exception e) {
		    System.out.println("access failed");
		}
		int ok1 = fromJava(val,t);
		if (ok1 == OK) return;
		errors.error(Errors.EXEC,pos,
			     "Field type "+t.getName()+" not supported");
		
	    }
	} else if (hasMethod(c,name)) {
	    if (ctx == value.ctxAssign) {
		errors.error(Errors.EXEC,pos,"can't assign to this expression");
	    } else {
		VLJMethod vlm = new VLJMethod();
		if (isStatic) {
		    vlm.obj = c;
		} else {
		    vlm.obj = o;
		}
		vlm.name = name;
		stack.push(vlm,0);
		return;
	    }
	}
	if (ctx != value.ctxAssign) {
	    stack.pushError();
	}
    }
}
