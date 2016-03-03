/*******************************************************************************
*
* Copyright (c) 2002. John R. Nestor. All rights reserved.
*
* See the file XLICENSE.txt for use and redistribution information.
*
*******************************************************************************/

package com.persist.xact.value;

public final class VLStream extends VL {
    public VLQueue[] q;

    public VLStream(int size) {
	vKind = VL.VSTREAM;
	q = new VLQueue[size];
	for (int i = 0; i < size; i++) {
	    q[i] = new VLQueue(1);
	}
    }
}