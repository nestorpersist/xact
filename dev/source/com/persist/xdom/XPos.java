/*******************************************************************************
*
* Copyright (c) 2002. John R. Nestor. All rights reserved.
*
* See the file XLICENSE.txt for use and redistribution information.
*
*******************************************************************************/

package com.persist.xdom;

public final class XPos {
   public int firstLine;
   public int firstChar;
   public int lastLine;
   public int lastChar;
   public FPosition fpos;

   public XPos () { }

   public XPos(int firstLine,int firstChar,int lastLine,int lastChar,FPosition fpos) {
      this.firstLine = firstLine;
      this.firstChar = firstChar;
      this.lastLine = lastLine;
      this.lastChar = lastChar;
      this.fpos = fpos;
   }
   public XPos copy() {
      XPos p = new XPos(firstLine,firstChar,lastLine,lastChar,fpos);
      return p;
   }
}