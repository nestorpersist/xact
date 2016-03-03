/*******************************************************************************
*
* Copyright (c) 2002. John R. Nestor. All rights reserved.
*
* See the file XLICENSE.txt for use and redistribution information.
*
*******************************************************************************/

package com.persist.xact.value;

public final class VLBool extends VL {

    public VLBool() {
	vKind = VL.VBOOL;
    }
    
    static public boolean isBool(Object oval) {
	if (Value.stringComp(oval,0,"true",0)==0) return true;
	if (Value.stringComp(oval,0,"false",0)==0) return true;
	return false;
    }

    static public long toBool(Object oval) {
	if (Value.stringComp(oval,0,"true",0)==0) return 1;
	return 0;
    }

    static public String toS(long val) {
	if (val == 0) {
	    return "false";
	} else {
	    return "true";
	}
    }

    static public long toI(boolean b) {
	if (b) {
	    return 1;
	} else {
	    return 0;
	}
    }
}