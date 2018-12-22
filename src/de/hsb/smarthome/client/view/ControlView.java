package de.hsb.smarthome.client.view;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridLayout;

import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;

import de.hsb.smarthome.client.controller.IDeviceManager;
import de.hsb.smarthome.util.json.Control;
import de.hsb.smarthome.util.log.Logger;
import de.hsb.smarthome.util.log.Logger.LoggerMode;

public class ControlView extends JPanel {


	public ControlView(JFrame parent, IDeviceManager deviceManager, Control control){
		
//		mDeviceManager = deviceManager;
		mControl = control;
		setLayout(new BorderLayout());
		
		JLabel labelIPinVPN = new JLabel("IP in VPN:");
		JLabel labelRemoteIP = new JLabel("Remote IP:");
		JLabel labelInfo = new JLabel("Info:");
		
		mLabelIPinVPN = new JLabel("null");
		mLabelRemoteIP = new JLabel("null");
		mLabelInfo = new JLabel("null");
		JPanel panelTop = new JPanel(new GridLayout(3,2));
		panelTop.add(labelIPinVPN);
		panelTop.add(mLabelIPinVPN);
		panelTop.add(labelRemoteIP);
		panelTop.add(mLabelRemoteIP);
		panelTop.add(labelInfo);
		panelTop.add(mLabelInfo);
		add(panelTop, BorderLayout.NORTH);
		

		JLabel labelCPU = new JLabel("CPU");
		JLabel labelTemperature = new JLabel("Temperatur:");
		JLabel labelWork = new JLabel("Auslastung:");
		JLabel labelCore = new JLabel("Kerne:");
		mLabelCPUTemperature = new JLabel("null");
		mLabelCPUWork = new JLabel("null");
		mLabelCores = new JLabel("null");
		
		
		JLabel labelMemory = new JLabel("Speicher");
		JLabel labelMemoryUsed = new JLabel("belegt:");
		JLabel labelMemoryFree = new JLabel("frei:");
		
		mLabelMemoryUsed = new JLabel("null");
		mLabelMemoryFree = new JLabel("null");
		
		JPanel panelMiddle = new JPanel(new GridLayout(4, 4));
		panelMiddle.add(labelCPU);
		panelMiddle.add(new JLabel(" "));
		panelMiddle.add(labelMemory);
		panelMiddle.add(new JLabel(" "));
		panelMiddle.add(labelTemperature);
		panelMiddle.add(mLabelCPUTemperature);
		panelMiddle.add(labelMemoryUsed);
		panelMiddle.add(mLabelMemoryUsed);
		panelMiddle.add(labelWork);
		panelMiddle.add(mLabelCPUWork);
		panelMiddle.add(labelMemoryFree);
		panelMiddle.add(mLabelMemoryFree);
		panelMiddle.add(labelCore);
		panelMiddle.add(mLabelCores);
		panelMiddle.add(new JLabel(" "));
		panelMiddle.add(new JLabel(" "));
		add(panelMiddle, BorderLayout.CENTER);
		
		mButtonShowLogHome = new JButton("Zeige Log");
//		mButtonShowLogRemote = new JButton("Zeige Log Raspi");
//		JPanel panelButtons = new JPanel(new GridLayout(1,2));
		JPanel panelButtons = new JPanel(new FlowLayout(FlowLayout.CENTER));
		panelButtons.add(mButtonShowLogHome);
//		panelButtons.add(mButtonShowLogRemote);
		add(panelButtons, BorderLayout.SOUTH);
		
		
		mButtonShowLogHome.addActionListener(e->{
//			LogDialog dialog = new LogDialog("Home log", 
//					LoadingDialog.class.getProtectionDomain().getCodeSource().getLocation().getPath() + 
//					"../safer_smarter_home.log");
			
			LogDialog dialog = new LogDialog(parent, "Home log", mLogger.getLogFilePath());
			dialog.setVisible(true);
		});
		
		Dimension max = new Dimension(300, 140);
		setMaximumSize(max);
		setPreferredSize(max);
		update(mControl);
	}

	public void update(Control control) {
		if(control != null) {
			if(control.getCpu() != null && !control.getCpu().isEmpty() ) {
				mLabelCores.setText(String.valueOf(control.getCpu().get(0).getCore()));
				mLabelCPUTemperature.setText(String.valueOf(control.getCpu().get(0).getTemperature()));
				mLabelCPUWork.setText(String.valueOf(control.getCpu().get(0).getWork()));
			}else {
				mLogger.write(this, "update controlview - no CPU information found", LoggerMode.WARN);
			}
			
			if(control.getMemory() != null) {
				mLabelMemoryFree.setText(String.valueOf(control.getMemory().getFree()));
				mLabelMemoryUsed.setText(String.valueOf(control.getMemory().getUsed()));
			}else {
				mLogger.write(this, "update controlview - no memory information found", LoggerMode.WARN);
			}
			
			if(control.getInfo() != null) {
				mLabelInfo.setText(control.getInfo());
				
				if(control.getInfo().length() > 25) {
					mRunningInfo = new RunningInfo(mLabelInfo);
					mRunningInfo.start();
				}else {
					mRunningInfo.mRunning = false;
				}
			}else {
				mLogger.write(this, "update controlview - no info string found", LoggerMode.WARN);
			}
			
			
			
		}else {
			mLogger.write(this, "Controlview is not updated. Control object is null.", LoggerMode.WARN);
		}
	}
	
	public void close() {
		if(mRunningInfo != null) {
			mRunningInfo.mRunning = false;
		}
	}
	
	private class RunningInfo extends Thread {
		
		RunningInfo(JLabel label){
			mLabel = label;
		}
		
		@Override
		public void run() {
			if(mLabel != null && mLabel.getText().length() > 25) {
				text = mLabel.getText();
				mRunning = true;
				while (mRunning) {
					String tmp = mLabel.getText();
					while (tmp.length() != 0 && mRunning) {

						tmp = mLabel.getText();
						if(tmp.length() > 0) {
							tmp = tmp.substring(1, tmp.length());
						}
						mLabel.setText(tmp);

						try {
							sleep(200);
						} catch (InterruptedException e) {
							mLogger.write(this, e.getMessage(), LoggerMode.ERROR);
						}
					}

					mLabel.setText(text);
					try {
						sleep(1000);
					} catch (InterruptedException e) {
						mLogger.write(this, e.getMessage(), LoggerMode.ERROR);
					}
					
				}
			}
		}
		
		public volatile boolean mRunning = false;
		private String text = "";
		private JLabel mLabel;	
	}
	
	private static final long serialVersionUID = 1L;
	private Logger mLogger = Logger.getLogger();
//	private Model mModel;
//	private IDeviceManager mDeviceManager;
	private Control mControl = null;
	
	private JLabel mLabelIPinVPN;
	private JLabel mLabelRemoteIP;
	private JLabel mLabelInfo;
	
	private JLabel mLabelCPUTemperature;
	private JLabel mLabelCPUWork;
	private JLabel mLabelCores;
	
	private JLabel mLabelMemoryUsed;
	private JLabel mLabelMemoryFree;
	
	private JButton mButtonShowLogHome;
//	private JButton mButtonShowLogRemote;
	
	private RunningInfo mRunningInfo;



}
