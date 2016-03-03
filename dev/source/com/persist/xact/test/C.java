package com.persist.xact.test;

import com.sleepycat.db.*;
import java.io.*;

public class C {
    public final static int CODE = 23;
    public static int y = 200;
    public int x = 100;

    public C(int i) {
	x = i;
    }

    public static String bar(String s) {
	return "bar:"+s;
    }

    public String foo(boolean b,int i) {
	if (b) {
	    return "T:"+i;
	} else {
	    return "F:"+i;
	}
    }
    public static void insert(Database table,String k,String v) throws UnsupportedEncodingException {
//	StringDbt key = new StringDbt(k);
//	StringDbt data = new StringDbt(v);
	DatabaseEntry key = new DatabaseEntry(k.getBytes("UTF-8"));
	DatabaseEntry data = new DatabaseEntry(v.getBytes("UTF-8"));
	OperationStatus err;
	try {
//	    if ((err = table.put(null,
//				 key, data, Db.DB_NOOVERWRITE)) == Db.DB_KEYEXIST) {
	    if ((err = table.putNoOverwrite(null,
				 key, data)) == OperationStatus.KEYEXIST) {
		System.out.println("Key already exists.");
	    }
	} catch (Exception e) {
	    System.out.println("insert failed");
	}
    }

    public static Database open() {
	String name="test.db";
	try {
	    new File(name).delete();
	    DatabaseConfig config = new DatabaseConfig();
	    config.setAllowCreate(true);
	    config.setType(DatabaseType.BTREE);
	    config.setMode(0644);
//	    Database table = new Database(null, 0);
	    Database table = new Database(name, null, config);
	    /*
	    table.set_error_stream(System.err);
	    table.set_errpfx("AccessExample");
	    */
//	    table.open(name, null, Db.DB_BTREE, Db.DB_CREATE, 0644);
	    return table;
	} catch(Exception e) {
	    return null;
	}
    }

    public static void close(Database table) {
	try {
//	    StringDbt key = new StringDbt();
//	    StringDbt data = new StringDbt();
	    DatabaseEntry key = new DatabaseEntry();
	    DatabaseEntry data = new DatabaseEntry();
//	    Dbc iterator = table.cursor(null, 0);
	    Cursor iterator = table.openCursor(null, null);
//	    while (iterator.get(key, data, Db.DB_NEXT) == 0){
	    while (iterator.getNext(key, data, LockMode.DEFAULT) == OperationStatus.SUCCESS){
		String k = new String(key.getData());
		String v = new String(data.getData());
		System.out.println(k + " : " +v);
	    }
	    iterator.close();
//	    table.close(0);
	    table.close();
	} catch(Exception e) {
	}
    }

}
