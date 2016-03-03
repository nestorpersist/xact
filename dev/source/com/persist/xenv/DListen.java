/*******************************************************************************
*
* Copyright (c) 2002. John R. Nestor. All rights reserved.
*
* See the file XLICENSE.txt for use and redistribution information.
*
*******************************************************************************/

package com.persist.xenv;	

import java.awt.*;
import java.awt.event.*;

public class DListen extends WindowAdapter {
   public void windowClosing(WindowEvent e) {
      Window w = e.getWindow();
      w.setVisible(false);
      w.dispose();
      System.exit(0);
   }
}
