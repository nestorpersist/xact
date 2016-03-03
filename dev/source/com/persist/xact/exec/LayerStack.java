/*******************************************************************************
*
* Copyright (c) 2006. John R. Nestor. All rights reserved.
*
* See the file XLICENSE.txt for use and redistribution information.
*
*******************************************************************************/

package com.persist.xact.exec;

import java.util.*;

public final class LayerStack {
    private Vector<Layer> layers;
    public LayerStack() {
	layers = new Vector<Layer>(10,5);
    }
    public void push(Layer layer) {
	layers.addElement(layer);
    }
    public Layer top() {
	int size = layers.size();
	return layers.get(size-1);
    }
    public void pop() {
	int size = layers.size();
	layers.remove(size-1);
    }
}
