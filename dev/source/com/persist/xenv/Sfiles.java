/*******************************************************************************
*
* Copyright (c) 2002-2007. John R. Nestor. All rights reserved.
*
* See the file XLICENSE.txt for use and redistribution information.
*
*******************************************************************************/

package com.persist.xenv;

import java.io.*;
import java.awt.*;
import javax.swing.*;
import javax.swing.text.*;
import javax.swing.border.*;
import com.persist.xact.system.*;
import com.persist.xdom.*;

public class Sfiles {
    public JTabbedPane jtp;

    private Sfile[] sfiles;
    private int top;
    private int current;

    private XThread xt;

    public Sfiles() {
//	this.xt = xt;
	top = -1;
	current = -1;
	jtp = new JTabbedPane(JTabbedPane.BOTTOM);
	sfiles = new Sfile[200];
    }

    public void setXT(XThread xt) {
	this.xt = xt;
    }
    
    public Sfile get(FPosition fpos) {
	int i;
	Sfile sfile;
	String name = fpos.name;
	String fname;
	if (fpos.kind == FPosition.FSCRIPT) {
	    String filePath = xt.cmd.loads.convertPath(xt,name);
	    fname = xt.cmd.option.scriptDir+xt.cmd.option.fileSep+filePath+".xact";
	} else if (fpos.kind == FPosition.FFILE) {
	    fname = name;
	} else {
	    return null;
	}
	for (i = 0; i <= top; i++) {
	    sfile = sfiles[i];
	    if (sfile.name.equals(name)) {
		jtp.setSelectedIndex(i);
		return sfile;
	    }
	}
	top ++;
	sfile = new Sfile();
	sfiles[top] = sfile;
	current = top;
	sfile.name = name;
	sfile.ta = new JTextArea();
	sfile.ta.setFont(new Font("COURIER NEW",Font.BOLD,16));
	sfile.ta.setEditable(false);
	sfile.h = new DefaultHighlighter();
	sfile.ta.setHighlighter(sfile.h);
	sfile.ta.setBorder(new EmptyBorder(3,5,3,5));
	try {
	    FileReader r = new FileReader(fname);

	    sfile.ta.read(r,null);
	    r.close();
	    sfile.text = sfile.ta.getText();
	} catch (Exception e) {
	    System.out.println("can't read "+fname);
	}
	JScrollPane sp = new JScrollPane(sfile.ta);
	sp.setBorder(new EmptyBorder(5,5,10,5));
	jtp.addTab(name,sp);
	jtp.setSelectedIndex(top);
	return sfile;
    }
}
