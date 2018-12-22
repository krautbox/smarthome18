package de.hsb.smarthome.client.view;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.awt.Toolkit;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextField;

import com.kilo52.common.io.ConfigurationFile;

import de.hsb.smarthome.client.controller.IDeviceManager;

public class ConfigDialog extends JDialog{

	private static final long serialVersionUID = 1L;
	
	public ConfigDialog(JFrame parent, String title, IDeviceManager deviceManager) {
		super(parent, title);
		setModal(true);
		mDeviceManager = deviceManager;
		mConfig = mDeviceManager.getConfig();
		this.setLayout(new BorderLayout());
		JPanel mainPanel = new JPanel(new GridLayout(2,2));
		JLabel labelIP = new JLabel("IP:");
		JLabel labelPort = new JLabel("Port:");
		mTextFieldIP = new JTextField();
		mTextFieldIP.setText(mConfig.getSection("Global").valueOf("RemoteIP"));
		mTextFieldPort = new JTextField();
		mTextFieldPort.setText(mConfig.getSection("Global").valueOf("RemotePort"));
		mainPanel.add(labelIP);
		mainPanel.add(mTextFieldIP);
		mainPanel.add(labelPort);	
		mainPanel.add(mTextFieldPort);
		this.add(mainPanel, BorderLayout.CENTER);
		
		JPanel bottomPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
		JButton buttonOK = new JButton("OK");
		JButton buttonAbort = new JButton("Abbrechen");
		bottomPanel.add(buttonOK);
		bottomPanel.add(buttonAbort);
		this.add(bottomPanel, BorderLayout.SOUTH);
		
		buttonAbort.addActionListener(e->{
			dispose();
		});
		
		buttonOK.addActionListener(e->{
			if(updateConfig()) {
				dispose();
			}
		});
		
		addWindowListener(new WindowAdapter() {
			@Override
			public void windowClosing(WindowEvent e) {
				dispose();
			}
		});
		pack();
		//Setze Fenster beim start in die Mitte
		int screenMiddleWidth = Toolkit.getDefaultToolkit()
	            .getScreenSize().width/2;
		int screenMiddleHeight = Toolkit.getDefaultToolkit()
	            .getScreenSize().height/2;
setLocation(screenMiddleWidth-this.getWidth()/2, screenMiddleHeight-this.getHeight()/2);
	}
	
	private boolean updateConfig() {
		String newIP = mTextFieldIP.getText();
		boolean correctIP = false;
		if(validIP(newIP)) {
			correctIP = true;
		}else {
			JOptionPane.showMessageDialog(this,"Keine korrekte IP","IP Fehler", JOptionPane.ERROR_MESSAGE);
		}
		
		String newPort = mTextFieldPort.getText();
		boolean correctPort = false;
		try {
			Integer.parseInt(newPort);
			correctPort = true;
		}catch(Exception x){
			JOptionPane.showMessageDialog(this,"Keine korrekter Port","Port Fehler", JOptionPane.ERROR_MESSAGE);
		}
		
		if(correctPort && correctIP) {
			mConfig.getSection("Global").set("RemoteIP", newIP);
			mConfig.getSection("Global").set("RemotePort", newPort);
			mDeviceManager.writeConfig(mConfig);
			return true;
		}else {
			return false;
		}
	}
	
	private boolean validIP (String ip) {
	    try {
	        if ( ip == null || ip.isEmpty() ) {
	            return false;
	        }

	        String[] parts = ip.split( "\\." );
	        if ( parts.length != 4 ) {
	            return false;
	        }

	        for ( String s : parts ) {
	            int i = Integer.parseInt( s );
	            if ( (i < 0) || (i > 255) ) {
	                return false;
	            }
	        }
	        if ( ip.endsWith(".") ) {
	            return false;
	        }

	        return true;
	    } catch (NumberFormatException nfe) {
	        return false;
	    }
	}
	
	
	private ConfigurationFile mConfig = null;
	private IDeviceManager mDeviceManager = null;
	private JTextField mTextFieldIP;
	private JTextField mTextFieldPort;

}
