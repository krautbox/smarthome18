package de.hsb.smarthome.client.view;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridLayout;

import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;

import de.hsb.smarthome.client.controller.IDeviceManager;
import de.hsb.smarthome.util.json.Device;
import de.hsb.smarthome.util.json.Image;
import de.hsb.smarthome.util.log.Logger;
import de.hsb.smarthome.util.log.Logger.LoggerMode;

public class RaspberryView extends JPanel implements IComponentView{


	
	public RaspberryView(IDeviceManager deviceManager, Device smartDevice){
		super();
//		mDeviceManager = deviceManager;
		mSmartDevice = smartDevice;
		this.setLayout(new BorderLayout());
		
		JPanel topPanel = new JPanel(new GridLayout(2,1));
		
		buildFirstPanel(topPanel);
		buildSecondPanel(topPanel);
		this.add(topPanel, BorderLayout.NORTH);
		buildThirdPanel(this);
		
		setActionListeners();
		
//		if(mModel.getTCPConnection() != null) {
//			mModel.getTCPConnection().register(this);
//		}
	}
	
	private void buildFirstPanel(JPanel toThis) {
		//First Panel
		JPanel firstPanel = new JPanel(new GridLayout(2,3));
		firstPanel.add(new JLabel("IP im Fernnetz:"));
		mLabelIpRemotNetwork = new JLabel("192.168.178.23");
		firstPanel.add(mLabelIpRemotNetwork);
		firstPanel.add(new JLabel(""));
		firstPanel.add(new JLabel(""));
		
		firstPanel.add(new JLabel("IP im Heimnetz:"));
		mLabelIpHomeNetzwork = new JLabel("192.168.178.100");
		firstPanel.add(mLabelIpHomeNetzwork);
		firstPanel.add(new JLabel(""));
		firstPanel.add(new JLabel(""));
		
		toThis.add(firstPanel);
	}
	
	private void buildSecondPanel(JPanel toThis) {
		
	
		//Second
		JPanel secondPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
			JPanel leftSide = new JPanel(new GridLayout(2,1));
			mButtonShowLog = new JButton("Zeige Log");
			leftSide.add(mButtonShowLog);
			leftSide.add(new JLabel("Befehl:"));
			
			JPanel rightSide = new JPanel(new GridLayout(2,1));
			rightSide.add(new JLabel(""));
			JPanel rightSideDown = new JPanel(new FlowLayout(FlowLayout.LEFT));
			mTextFieldCommand = new JTextField();
			mTextFieldCommand.setSize(new Dimension(200,50));
			mTextFieldCommand.setPreferredSize(new Dimension(300,20));
			rightSideDown.add(mTextFieldCommand);
			mButtonSendCommand = new JButton("Sende Befehl");
			rightSideDown.add(mButtonSendCommand);
			rightSide.add(rightSideDown);
			
		secondPanel.add(leftSide);
		secondPanel.add(rightSide);
		toThis.add(secondPanel);
	}

	private void buildThirdPanel(JPanel toThis) {
		//FourthPanel
		JPanel fourthPanel = new JPanel(new GridLayout(1,1));
		mTextArea =  new JTextArea(5, 20);
		mTextArea.setEditable(false);
		mTextArea.append("Schalte Steckdose ein...\n");
		mScrollPaneTextArea = new JScrollPane(mTextArea, JScrollPane.VERTICAL_SCROLLBAR_ALWAYS, JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
		
		fourthPanel.add(mScrollPaneTextArea);
		
		toThis.add(fourthPanel, BorderLayout.CENTER);
	}
	
	private void setActionListeners() {
		mButtonSendCommand.addActionListener(e->{
			mLogger.write(this, "Sende Befehl: " + mTextFieldCommand.getText(), LoggerMode.TRACE);
			mTextArea.append("Sende: " + mTextFieldCommand.getText() + "\n");
			
			
		});
		mButtonShowLog.addActionListener(e->{
			
			JOptionPane.showMessageDialog(this,"Log", "Log", JOptionPane.INFORMATION_MESSAGE);
		});
	}
	
	
	public boolean isViewFromDevice(Device dev) {
		return mSmartDevice.equals(dev);
	}
	
	@Override
	public void update(Device device) {
		// TODO Auto-generated method stub
		
	}
	
	@Override
	public void update(Device device, Image image) {
		// TODO Auto-generated method stub
		
	}
	
	@Override
	public ViewType getViewType() {
		return ViewType.RASPBERRY;
	}

	@Override
	public void close() {
		// TODO Auto-generated method stub
		
	}
	
	private static final long serialVersionUID = 1L;

	private Logger mLogger = Logger.getLogger();
	
	private JLabel mLabelIpRemotNetwork;
	private JLabel mLabelIpHomeNetzwork;
	private JButton mButtonSendCommand;
	private JButton mButtonShowLog;
	private JTextField mTextFieldCommand;
	private JTextArea mTextArea;
	private JScrollPane mScrollPaneTextArea;
//	private IDeviceManager mDeviceManager;
	private Device mSmartDevice;





}
