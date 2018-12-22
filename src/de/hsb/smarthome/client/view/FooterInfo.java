package de.hsb.smarthome.client.view;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Graphics;

import javax.swing.JLabel;
import javax.swing.JPanel;

public class FooterInfo extends JPanel{
	private static final long serialVersionUID = 1L;
	JLabel labelInfo;
	
	
	public FooterInfo(String info) {
		labelInfo = new JLabel(info);
		this.setLayout(new FlowLayout(FlowLayout.RIGHT));
		this.add(labelInfo);
		this.repaint();
	}
	
	public void setInfo(String info) {
		labelInfo.setText(info);
	}
	
	
	@Override 
	public void paint(Graphics g){
        //Get the current size of this component
		super.paint(g);
        Dimension d = this.getSize();

        //draw in black
        g.setColor(Color.BLACK);
        //draw a centered horizontal line
        g.drawLine(0,0,d.width,0);
    }
	
}
