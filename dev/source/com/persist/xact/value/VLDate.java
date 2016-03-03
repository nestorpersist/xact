/*******************************************************************************
*
* Copyright (c) 2002-2005. John R. Nestor. All rights reserved.
*
* See the file XLICENSE.txt for use and redistribution information.
*
*******************************************************************************/

package com.persist.xact.value;

import java.util.*;

public final class VLDate extends VL {

    static public TimeZone gmtTZ = TimeZone.getTimeZone("GMT");
    static public TimeZone localTZ = TimeZone.getDefault();

    public TimeZone tz;
    public VLDate(TimeZone tz) {
	vKind = VL.VDATE;
	this.tz = tz;
    }

    public static Calendar toCalendar(String s1) {
	Calendar cal = Calendar.getInstance();
	String s = s1;
	int pos = s1.lastIndexOf(' ');
	int size = s1.length();
	TimeZone tz = localTZ;
	boolean dst = false;
	int dstOff = 0;
	if (0<= pos && pos+1 < size) {
	    char ch = s1.charAt(pos+1);
	    if ('0' <= ch && ch <= '9') {
	    } else {
		s1 = s.substring(0,pos);
		size = s1.length();
		String tzs = s.substring(pos+1);
		if (tzs.length() == 3) {
		    if (tzs.substring(1,3).equals("DT")) {
			tzs = tzs.substring(0,1) + "ST";
			dst = true;
			cal.set(2001,Calendar.JUNE,1);
			dstOff = cal.get(Calendar.DST_OFFSET);
		    }
		}
		tzs = tzs.intern();
		if (tzs == "GMT") {
		    tz = gmtTZ;
		} else {
		    tz = TimeZone.getTimeZone(tzs);
		    if (tz.getDisplayName(false,TimeZone.SHORT).equals("GMT")) {
			/* unrecognized timezone name */
			return null;
		    }
		}
		
	    }
	}
	int year = 0;
	int month = 0;
	int day = 0;
	int hour = 0;
	int minute = 0;
	int second = 0;
	int millisecond = 0;
	int i = 0;
	boolean haveYear = false;
	boolean haveMonth = false;
	boolean haveDay = false;
	while (i < size) {
	    char ch = s.charAt(i);
	    if ('0' <= ch && ch <= '9') {
		year = year * 10 + (ch - '0');
		haveYear = true;
		i ++;
	    } else {
		break;
	    } 
	}
	if (! haveYear) return null;
	if (i < size) {
	    char ch = s.charAt(i);
	    if (ch != '/') return null;
	    i++;
	}
	while (i < size) {
	    char ch = s.charAt(i);
	    if ('0' <= ch && ch <= '9') {
		month = month * 10 + (ch - '0');
		haveMonth = true;
		i ++;
	    } else {
		break;
	    } 
	}
	if (! haveMonth) {
	    month = 0;
	} else {
	    month = month - 1;
	}
	if (i < size) {
	    char ch = s.charAt(i);
	    if (ch != '/') return null;
	    i++;
	}
	while (i < size) {
	    char ch = s.charAt(i);
	    if ('0' <= ch && ch <= '9') {
		day = day * 10 + (ch - '0');
		haveDay = true;
		i ++;
	    } else {
		break;
	    } 
	}
	if (! haveDay) day = 1;
	if (i < size) {
	    char ch = s.charAt(i);
	    if (ch != ' ') return null;
	    i++;
	}
	while (i < size) {
	    char ch = s.charAt(i);
	    if ('0' <= ch && ch <= '9') {
		hour = hour * 10 + (ch - '0');
		i ++;
	    } else {
		break;
	    } 
	}
	if (i < size) {
	    char ch = s.charAt(i);
	    if (ch != ':') return null;
	    i++;
	}
	while (i < size) {
	    char ch = s.charAt(i);
	    if ('0' <= ch && ch <= '9') {
		minute = minute * 10 + (ch - '0');
		i ++;
	    } else {
		break;
	    } 
	}
	if (i < size) {
	    char ch = s.charAt(i);
	    if (ch != ':') return null;
	    i++;
	}
	while (i < size) {
	    char ch = s.charAt(i);
	    if ('0' <= ch && ch <= '9') {
		second = second * 10 + (ch - '0');
		i ++;
	    } else {
		break;
	    } 
	}
	if (i < size) {
	    char ch = s.charAt(i);
	    if (ch != '.') return null;
	    i++;
	}
	while (i < size) {
	    char ch = s.charAt(i);
	    if ('0' <= ch && ch <= '9') {
		millisecond = millisecond * 10 + (ch - '0');
		i ++;
	    } else {
		break;
	    } 
	}
	if (i != size) return null;
	cal.setTimeZone(tz);
	cal.clear();
	cal.setLenient(true);
	cal.set(year,month,day,hour,minute,second);
	cal.set(Calendar.MILLISECOND,millisecond);
	if (cal.get(Calendar.YEAR) != year) return null;
	if (cal.get(Calendar.MONTH) != month) return null;
	if (cal.get(Calendar.DAY_OF_MONTH) != day) return null;
	if (cal.get(Calendar.HOUR) != hour) return null;
	if (cal.get(Calendar.MINUTE) != minute) return null;
	if (cal.get(Calendar.SECOND) != second) return null;
	if (cal.get(Calendar.MILLISECOND) != millisecond) return null;
	if (dst) {
	    cal.set(Calendar.ZONE_OFFSET,tz.getRawOffset());
	    cal.set(Calendar.DST_OFFSET,dstOff);
	}
	return cal;
    }

    public static boolean isDate(String s) {
	if (toCalendar(s) == null) return false;
	return true;
    }

    private static String s(int cnt,int val) {
	String s = ""+ val;
	String zeros ="0000";
	int size = s.length();
	if (size >= cnt) return s;
	return zeros.substring(0,cnt-size) + s;
    }

    public static String toString(VLDate vld,long ival) {
	Calendar cal = Calendar.getInstance();
	Date d = new Date(ival);
	cal.setTimeZone(VLDate.gmtTZ);
	cal.setTime(d);
	cal.setTimeZone(vld.tz);
	
	return (s(4,cal.get(Calendar.YEAR))+"/"+
		s(2,cal.get(Calendar.MONTH)+1)+"/"+
		s(2,cal.get(Calendar.DAY_OF_MONTH))+" "+
		s(2,cal.get(Calendar.HOUR_OF_DAY))+":"+
		s(2,cal.get(Calendar.MINUTE))+":"+
		s(2,cal.get(Calendar.SECOND))+"."+
		s(3,cal.get(Calendar.MILLISECOND)) + " " +
		vld.tz.getDisplayName(vld.tz.inDaylightTime(cal.getTime()),TimeZone.SHORT)).intern();
    }
}
