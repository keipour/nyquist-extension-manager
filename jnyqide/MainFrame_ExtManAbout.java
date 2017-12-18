package jnyqide;


import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import javax.swing.border.*;


public class MainFrame_ExtManAbout extends JDialog implements ActionListener {

	  JPanel panel1 = new JPanel();
	  JPanel panel2 = new JPanel();
	  JPanel insetsPanel1 = new JPanel();
	  JPanel insetsPanel2 = new JPanel();
	  JPanel insetsPanel3 = new JPanel();
	  JButton button1 = new JButton();
	  JLabel imageLabel = new JLabel();
	  JLabel label1 = new JLabel();
	  JLabel label2 = new JLabel();
	  JLabel label3 = new JLabel();
	  JLabel label4 = new JLabel();
	  BorderLayout borderLayout1 = new BorderLayout();
	  BorderLayout borderLayout2 = new BorderLayout();
	  FlowLayout flowLayout1 = new FlowLayout();
	  FlowLayout flowLayout2 = new FlowLayout();
	  GridLayout gridLayout1 = new GridLayout();
	  String product = "Nyquist IDE Extension Manager";
	  String version = "Version 0.2 - December 2017";
	  String author = "Implemented by: Azarakhsh Keipour";
	  String comments = "Developed as final project for 15-622 course in Fall 2017";

	public MainFrame_ExtManAbout(Frame parent) {
    super(parent);
    enableEvents(AWTEvent.WINDOW_EVENT_MASK);
    try {
      jbInit();
    }
    catch(Exception e) {
      e.printStackTrace();
    }
    pack();
  }
  //Component initialization
  private void jbInit() throws Exception  {
    this.setTitle("About Extension Manager");
    setResizable(false);
    panel1.setLayout(borderLayout1);
    panel2.setLayout(borderLayout2);
    insetsPanel1.setLayout(flowLayout1);
    insetsPanel2.setLayout(flowLayout2);
    insetsPanel2.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
    button1.setText("Ok");
    button1.addActionListener(this);
    gridLayout1.setRows(4);
    gridLayout1.setColumns(1);
    label1.setText(product);
    label2.setText(version);
    label3.setText(author);
    label4.setText(comments);
    insetsPanel2.add(insetsPanel3);
    insetsPanel3.setLayout(gridLayout1);
    insetsPanel3.setBorder(BorderFactory.createEmptyBorder(10, 60, 10, 10));
    insetsPanel3.add(label1, null);
    insetsPanel3.add(label2, null);
    insetsPanel3.add(label3, null);
    insetsPanel3.add(label4, null);
    insetsPanel2.add(imageLabel, null);
    panel2.add(insetsPanel2, BorderLayout.WEST);
    this.getContentPane().add(panel1, null);
    insetsPanel1.add(button1, null);
    panel1.add(insetsPanel1, BorderLayout.SOUTH);
    panel1.add(panel2, BorderLayout.NORTH);
  }
  //Overridden so we can exit when window is closed
  protected void processWindowEvent(WindowEvent e) {
    if (e.getID() == WindowEvent.WINDOW_CLOSING) {
      cancel();
    }
    super.processWindowEvent(e);
  }
  //Close the dialog
  void cancel() {
    dispose();
  }
  //Close the dialog on a button event
  public void actionPerformed(ActionEvent e) {
	    if (e.getSource() == button1) {
	        cancel();
    }
  }
}
