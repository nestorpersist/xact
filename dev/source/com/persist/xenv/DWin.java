/*******************************************************************************
*
* Copyright (c) 2002-2007. John R. Nestor. All rights reserved.
*
* See the file XLICENSE.txt for use and redistribution information.
*
*******************************************************************************/

package com.persist.xenv;

import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import javax.swing.event.*;
import javax.swing.text.*;
import javax.swing.tree.*;
// import sun.security.krb5.internal.bl;

import com.persist.xdom.*;
import com.persist.xact.exec.*;
import com.persist.xact.system.*;
import com.persist.xact.value.*;
import com.persist.xact.bind.*;

public class DWin extends JFrame {
   private JTextArea ta2;
   private JLabel t3;
   private Object lock = new Object();
   private Sfiles sfiles;
   private Sfile currentsf;
   private ErrorAct errorAct;

   private XThread xt;
   private Exec exec;
   private Value value;
   private VariableStack stack;
   private Frames frames;

   private final JTree stackTree = makeStackTree("Calls",true);
   private final JTree stackTree1 = makeStackTree("Nesting",false);

   private final int ERR = 0;
   private final int BEFORE = 1;
   private final int AFTER = 2;
   private final int DONE = 3;
   private int kind;
   private XPos pos;
   private String errorKind = "";
   private String msg = "";
   private Highlighter.HighlightPainter pt;
   private boolean hasVal = false;
   private Object oval = null;
   private long ival = 0;
   private boolean assigned = false;
   private boolean redo = false;
   private int buttonKind = 0;

   private void stackTreeSelect(XFrame which) {
      if (which == null) {
	  /*
	  stackTree.clearSelection();
	  stackTree.addSelectionRow(1);
	  */
      } else {
	 XPos pos = this.pos;
	 Highlighter.HighlightPainter pt = this.pt;
	 XThread xt1 = xt;
	 Frames frames1 = exec.frames;
	 int top = frames1.getTop();
	 while (true) {
	     for (int i = top; i >= 0; i--) {
		 XFrame f = frames1.getFrame(i);
		 if (f == which) break;
		 if (f.caller != null) {
		     pos = f.caller.pos;
		     pt = ptstack;
		 }
	     }
	     top = xt1.parentFrame;
	     xt1 = xt1.parent;

	     if (xt1 == null) break;
	     frames1 = xt1.exec.frames;
	 }
	 show(pos,pt);
      }
   }

   private JTree makeStackTree(String name,final boolean dynamic) {
      DefaultMutableTreeNode root = new DefaultMutableTreeNode(name);
      DefaultTreeModel model = new DefaultTreeModel(root);
      JTree tree = new JTree(model);
      tree.setShowsRootHandles(true);
      tree.getSelectionModel().setSelectionMode
	    (TreeSelectionModel.SINGLE_TREE_SELECTION);
      tree.addTreeSelectionListener(new TreeSelectionListener() {
	 public void valueChanged(TreeSelectionEvent e) {
	    DefaultMutableTreeNode node = (DefaultMutableTreeNode)
					  stackTree.getLastSelectedPathComponent();
	    if (! dynamic || node == null) return;

	    Object nodeInfo = node.getUserObject();
	    if (nodeInfo instanceof XFrame) {
		stackTreeSelect((XFrame)nodeInfo);
	    } else {
		stackTree.clearSelection();
		stackTree.addSelectionPath(e.getOldLeadSelectionPath());
		stackTreeSelect((XFrame)null);
	    }
	 }
      });

      return tree;
   }

   private int addLocals(XFrame f,DefaultTreeModel model,DefaultMutableTreeNode node,XDef def) {
       if (def != null) {
	   int j = addLocals(f,model,node,def.next);
	   if (def instanceof XDefName &&
	       ! (def instanceof XDefFunc) &&
	       ! (def instanceof XDefType)) {
	       XDefName ndef = (XDefName) def;
	       frames.getDefVal(f,ndef,null);
	       Object oval = stack.getTopOval();
	       long ival = stack.getTopIval();
	       stack.pop();
	       if (value.isError(oval,ival)) { ival = 2; }
	       DefaultMutableTreeNode node1 = new DefaultMutableTreeNode(def.name+":"+valtype(oval,ival,true));
	       model.insertNodeInto(node1,node,j);
	       j ++;
	   }
	   return j;
       }
       return 0;
   }

   private void updateStackTree(JTree jtree,boolean dynamic) {
      DefaultTreeModel model = (DefaultTreeModel) jtree.getModel();
      DefaultMutableTreeNode root = (DefaultMutableTreeNode) model.getRoot();
      int size = root.getChildCount();
      for (int i = 0; i < size; i++) {
	 model.removeNodeFromParent((MutableTreeNode)root.getChildAt(0));
      }
      /*
      root.removeAllChildren();
      */
      XThread xt1 = xt;
      Frames frames1 = exec.frames;
      int top = frames1.getTop();
      /*
      System.out.println("frames debug dump");
      */
      int j = 0;
      if (dynamic) {
	  while (true) {
	      for (int i = top; i >= 0; i--) {
		  XFrame f = frames1.getFrame(i);
		  DefaultMutableTreeNode node = new DefaultMutableTreeNode(f);
		  model.insertNodeInto(node,root,j);
		  addLocals(f,model,node,f.scope.defs);
		  jtree.expandPath(new TreePath(node.getPath()));
		  j++;
	      }
	      top = xt1.parentFrame;
	      xt1 = xt1.parent;

	      if (xt1 == null) break;
	      frames1 = xt1.exec.frames;
	  }
      } else {
	  XFrame f = frames.getFrame(top);
	  while (f != null) {
	      DefaultMutableTreeNode node = new DefaultMutableTreeNode(f);
	      model.insertNodeInto(node,root,j);
	      addLocals(f,model,node,f.scope.defs);
	      jtree.expandPath(new TreePath(node.getPath()));
	      j++;
	      f = f.slink;
	  }
      }
      jtree.setRootVisible(true);
      jtree.expandRow(0);
      jtree.addSelectionRow(1);
      /*
      jtree.setRootVisible(false);
      */
   }

   private int getPos(Sfile sf, int ch, int line) {
       try {
	   return sf.ta.getLineStartOffset(line-1)+ch-1;
       } catch(Exception e) {
	   return 0;
       }
       /*
       String txt = sf.text;
       int size = txt.length();
       int line1 = 1;
       int ch1 = 1;
       for (int i = 0; i < size; i++) {
	   char c = txt.charAt(i);
	   if (line == line1 && ch == ch1) return i;
	   if (line < line1) return i;
	   if (c == '\n') {
	       line1 ++;
	       ch1 = 0;
	   }
	   ch1 ++;
       }
       return size;
       */
   }

   private void show(XPos pos,Highlighter.HighlightPainter pt) {
       if (pos == null) return;
       Sfile newsf = sfiles.get(pos.fpos);
       if (newsf == null) return;
      final int pos1 = getPos(newsf, pos.firstChar, pos.firstLine);
      int pos2;
      if (pos.lastLine <= 0) {
	 pos2 = pos1 + 1;
      } else {
	  pos2 = getPos(newsf, pos.lastChar, pos.lastLine)+1;
      }
      try {
	 if (currentsf != null) {
	    currentsf.h.removeAllHighlights();
	 }
	 newsf.h.addHighlight(pos1,pos2,pt);
	 final int pos3 = getPos(newsf, pos.lastChar, pos.lastLine+2)+1;
	 final JTextArea ta = newsf.ta;
	 SwingUtilities.invokeLater(new Runnable() {
	     public void run() {
		 try {
		     Rectangle r1 = ta.modelToView(pos1);
		     Rectangle r2 = ta.modelToView(pos3);
		     if (r1 != null && r2 != null) {
			 r1.add(r2);
			 ta.scrollRectToVisible(r1);
		     }
		 } catch(Exception e) {}
	     }
	 });
      } catch(Exception e) {
	 System.out.println("can't set highlight"+e);
	 xt.errors.fail(e,"main");
      }
      currentsf = newsf;
   }

   private String val(Object oval,long ival,boolean debug) {
       try {
	   exec.opExec.makeString(oval,ival,value.ctxEval,debug);
       } catch(Exception e) {
	   return "eee";
       }
       Object oval1 = stack.getTopOval();
       long ival1 = stack.getTopIval();
       stack.pop();
       if (XDOMValue.isString(oval1,ival1)) {
	   return XDOMValue.getString(oval1,ival1);
       }
       return "???";
   }

   private String type(Object oval,long ival,boolean debug) {
       exec.opExec.execType(oval,ival);
       Object oval1 = stack.getTopOval();
       long ival1 = stack.getTopIval();
       stack.pop();
       return val(oval1,ival1,debug);
   }

   private String valtype(Object oval,long ival,boolean debug) {
       return val(oval,ival,debug)+"~"+type(oval,ival,debug);
   }
   
   private class ErrorAct implements Runnable {

      public void run() {
	 try {
	    updateStackTree(stackTree,true);
	    updateStackTree(stackTree1,false);
	    show(pos,pt);
	    if (kind == ERR) {
		activate(stepClass);
		ta2.setText(errorKind+": "+msg);
	    } else if (kind == BEFORE) {
		String s = "Before";
		activate(stepClass);
		if (hasVal) {
		    s = s + " Assign " + valtype(oval,ival,false);
		}
		ta2.setText(s);
	    } else if (kind == AFTER) {
		String s = "After";
		activate(afterClass);
		if (hasVal) {
		    s = s + " " + valtype(oval,ival,false);
		} else {
		    s = s + " Assign";
		}
		if (assigned) {
		    s = s + " [Assigned "+valtype(xt.assignOval,xt.assignIval,false)+"]";
		}
		ta2.setText(s);
	    } else {
	       activate(doneClass);
	       ta2.setText("Done");
	       if (currentsf != null) {
		  currentsf.h.removeAllHighlights();
	       }
	    }
	    t3.setText("Thread: "+xt.name);
	 } catch (Exception e) {
	    System.out.println("DWin run failed");
	    e.printStackTrace();
	 }
      }
   }

   private final static Color cerror = new Color(255,100,100);
   private final static Color cbefore = Color.yellow;
   private final static Color cafter = Color.green;
   private final static Color cstack = new Color(0,204,255);
   private final static Highlighter.HighlightPainter pterror = new DefaultHighlighter.DefaultHighlightPainter(cerror);
   private final static Highlighter.HighlightPainter ptbefore = new DefaultHighlighter.DefaultHighlightPainter(cbefore);
   private final static Highlighter.HighlightPainter ptafter = new DefaultHighlighter.DefaultHighlightPainter(cafter);
   private final static Highlighter.HighlightPainter ptstack = new DefaultHighlighter.DefaultHighlightPainter(cstack);

   private final static int BBeforeOut = 0;
   private final static int BBeforeAfterOut = 1;
   private final static int BAfterOut = 2;
   private final static int BBefore = 3;
   private final static int BBeforeAfter = 4;
   private final static int BAfter = 5;
   private final static int BBeforeIn = 6;
   private final static int BBeforeAfterIn = 7;
   private final static int BAfterIn = 8;
   private final static int BRestart = 9;
   private final static int BContinue = 10;
   private final static int BQuit = 11;
   private final static int BRedo = 12;
   private final static int BHalt = 13;
   private JButton[] allButtons = new JButton[14];
   
   private final static String bName[] = {
      "^", "<>", "^", "<", "<>",  ">", "V", "<>", "V",
      "S", "=>", "Q",
      "<=", "X"
   };
   private final static String bDesc[] = {
      "Before Out", "Before/After Out", "After Out",
      "Before", "Before/After", "After",
      "Before In", "Before/After In", "After In",
      "Restart", "Continue", "Quit",
      "Redo", "Halt"
   };
   private final static Color bColor[] = {
      cbefore, cstack, cafter,
      cbefore, cstack, cafter,
      cbefore, cstack, cafter,
      cstack, cstack, cstack,
      cstack, cstack
   };
   private final static boolean bDebug[] = {
      true, true, true, true, true, true, true, true, true,
      false, false, false,
      false, false
   };
   private final static boolean bBefore[] = {
       true, true, false, true, true, false, true, true, false,
       false, false, false,
       false, false
   };
   private final static boolean bAfter[] = {
       false, true, true, false, true, true, false, true, true,
       false, false, false,
       false, false
   };
   private final static int bWhich[] = {
      -1, -1, -1, 0, 0, 0, 1, 1, 1,
      0, 0, 0,
      0, 0
   };
   private final static boolean[] stepClass = {
      true, true, true, true, true, true, true, true, true,
      true, true, true,
      false , false 
   };
   private final static boolean[] afterClass = {
      true, true, true, true, true, true, true, true, true,
      true, true, true,
      true , false 
   };
   private final static boolean[] doneClass = {
      false, false, false, false, false, false, false, false, false,
      true, false, true,
      false , false 
   };
   private final static boolean[] runClass = {
      false, false, false, false, false, false, false, false, false,
      false, false, false,
      false , true 
   };

   private void activate(boolean[] cl) {
      int size = allButtons.length;
      for (int i = 0; i < size; i++) {
	 allButtons[i].setEnabled(cl[i]);
      }
   }

   private JButton dbutton(final int bkind) {
      JButton b = new JButton(bName[bkind]);
      allButtons[bkind] = b;
      b.setToolTipText(bDesc[bkind]);
      b.setBackground(bColor[bkind]);
      b.addActionListener(new ActionListener() {
	 public void actionPerformed(ActionEvent ae) {
	    activate(runClass);
	    ta2.setText("Running");
	    if (currentsf != null) {
	       currentsf.h.removeAllHighlights();
	    }
	    if (bkind == BHalt) {
	       xt.exec.haltDebug();
	       return;
	    }
	    buttonKind = bkind;
	    if (bkind == BRestart) {
	       xt.exec.setDebug(true,true,false,-1);
	    } else if (bkind == BQuit) {
	    } else if (bkind == BRedo) {
	       redo = true;
	       xt.exec.setDebug(true,true,false,0);
	    } else if (kind == ERR && bkind == BContinue) {
	       /* leave step breakpoint asis */
	    } else {
	       xt.exec.setDebug(bDebug[bkind],bBefore[bkind],bAfter[bkind],bWhich[bkind]);
	    }
	    synchronized(lock) {
	       lock.notify();
	    }
	 }
      });
      return b;
   }
   
   public DWin() {
       super("X:Act Debugger");
      sfiles = new Sfiles();
      errorAct = new ErrorAct();
      currentsf = null;
      try {
	  UIManager.setLookAndFeel("com.sun.java.swing.plaf.windows.WindowsLookAndFeel");
	 SwingUtilities.updateComponentTreeUI(this);
      } catch(Exception e) {
//	  System.out.println("can't set look and feel");
//	  e.printStackTrace();
      }
      setSize(800,900);
      addWindowListener(new DListen());

      Box b1 = Box.createHorizontalBox();
      JPanel buttons = new JPanel();
      buttons.setLayout(new GridLayout(3,5));

      buttons.add(dbutton(BBeforeOut));
      buttons.add(dbutton(BBeforeAfterOut));
      buttons.add(dbutton(BAfterOut));
      buttons.add(dbutton(BRestart));
      buttons.add(dbutton(BHalt));

      buttons.add(dbutton(BBefore));
      buttons.add(dbutton(BBeforeAfter));
      buttons.add(dbutton(BAfter));
      buttons.add(dbutton(BContinue));
      buttons.add(dbutton(BRedo));

      buttons.add(dbutton(BBeforeIn));
      buttons.add(dbutton(BBeforeAfterIn));
      buttons.add(dbutton(BAfterIn));
      buttons.add(dbutton(BQuit));
      buttons.add(new JPanel());

      t3 = new JLabel("");
      t3.setFont(new Font("COURIER NEW",Font.BOLD,16));
      b1.add(buttons);
      b1.add(t3);
      b1.setAlignmentX(0);
      b1.setAlignmentY(0);
      b1.add(Box.createHorizontalGlue());

      JScrollPane sp1 = new JScrollPane(sfiles.jtp);
      
      ta2 = new JTextArea();	
      ta2.setFont(new Font("COURIER NEW",Font.BOLD,16));
      ta2.setEditable(false);
      ta2.setLineWrap(true);
      ta2.setWrapStyleWord(true);
      JScrollPane sp2 = new JScrollPane(ta2);
      sp2.setMinimumSize(new Dimension(20,100));

      JSplitPane split = new JSplitPane(JSplitPane.VERTICAL_SPLIT,sfiles.jtp,sp2);
      split.setBackground(Color.white);
      split.setResizeWeight(1);
      split.setContinuousLayout(true);

      JScrollPane sp3 = new JScrollPane(stackTree);
      JScrollPane sp4 = new JScrollPane(stackTree1);
      JSplitPane split2 = new JSplitPane(JSplitPane.VERTICAL_SPLIT,sp3,sp4);
      split2.setResizeWeight(0.5);
      split2.setContinuousLayout(true);
      split2.setOneTouchExpandable(true);
      JSplitPane split1 = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT,split,split2);
      split1.setResizeWeight(0.7);
      split1.setContinuousLayout(true);
      split1.setOneTouchExpandable(true);

      Box b = Box.createVerticalBox();
      b.add(b1);
      b.add(split1);
      getContentPane().add(b);	
   }

   private void setXT(XThread xt) {
       this.xt = xt;
       sfiles.setXT(xt);
       exec = xt.exec;
       value = xt.cmd.value;
       stack = exec.stack;
       frames = exec.frames;
   }

   public void setError(XThread xt,XPos pos,String kind,String msg) {
       setXT(xt);
       this.kind = ERR;
       this.pos = pos;
       this.errorKind = kind;
       this.msg = msg;
       pt = pterror;
       SwingUtilities.invokeLater(errorAct);
   }

   public void before(XThread xt,XPos pos,boolean hasVal,Object oval,long ival) {
       setXT(xt);
       kind = BEFORE;
       this.pos = pos;
       this.hasVal = hasVal;
       this.oval = oval;
       this.ival = ival;
       pt = ptbefore;
       SwingUtilities.invokeLater(errorAct);
   }

   public void after(XThread xt,XPos pos,boolean hasVal,Object oval,long ival,boolean assigned) {
       setXT(xt);
       redo = false;
       kind = AFTER;
       this.pos = pos;
       this.hasVal = hasVal;
       this.oval = oval;
       this.ival = ival;
       this.assigned = assigned;
       pt = ptafter;
       SwingUtilities.invokeLater(errorAct);
   }

   public boolean getRedo() {
       return redo;
   }

   public void done(XThread xt) {
       setXT(xt);
       kind = DONE;
       SwingUtilities.invokeLater(errorAct);
   }

   public void quit(XThread xt) {
       setXT(xt);
       this.setVisible(false);
       this.dispose();
   }
   
   public void dwait() {
       try {
	   synchronized(lock) {
	       lock.wait();
	   }
       } catch(Exception e) {
       }
       if (buttonKind == BRestart) {
	   throw XCmd.restartException;
       } else if (buttonKind == BQuit) {
	   throw XCmd.quitException;
       }
   }
}
