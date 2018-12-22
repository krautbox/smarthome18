package de.hsb.smarthome.client.view;

import java.awt.BorderLayout;
import java.awt.GridLayout;
import java.awt.Toolkit;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Vector;

import javax.swing.JFrame;
import javax.swing.JList;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

import de.hsb.smarthome.client.controller.IDeviceManager;
import de.hsb.smarthome.client.model.Model;
import de.hsb.smarthome.client.view.IComponentView.ViewType;
import de.hsb.smarthome.util.json.Control;
import de.hsb.smarthome.util.json.Device;
import de.hsb.smarthome.util.json.Image;
import de.hsb.smarthome.util.log.Logger;
import de.hsb.smarthome.util.log.Logger.LoggerMode;

public class SSH_MainFrame extends JFrame {


	
	public SSH_MainFrame(String title, Model model, IDeviceManager deviceManager) {
		super(title);
		mModel = model;
		mParent = this;
		mDeviceManager = deviceManager;
		BorderLayout myLayout = new BorderLayout();
		myLayout.setVgap(20);
		myLayout.setHgap(20);
		setLayout(myLayout);
		this.setBounds(500, 150, 1000, 600);

	    java.awt.Image icon = Toolkit.getDefaultToolkit().getImage("images/ssh_icons/192x192/icon.png");
	    this.setIconImage(icon);

		
		setMenues();
		updateListElements();
		createViews();
		
		
		mControlView = new ControlView(this, mDeviceManager, mModel.getControl());
		mCenter = new JPanel(new GridLayout(1,1));
		
		if(mModel.getSmartElements() != null ) {
			setCurrentPanel(mModel.getSmartElements().get(0));
		}
		
		this.add(mCenter, BorderLayout.CENTER);
		
		JScrollPane smartElementsScrollPane = new JScrollPane(mSmartElementsListView , JScrollPane.VERTICAL_SCROLLBAR_ALWAYS, JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
//		smartElementsScrollPane.setPreferredSize(new Dimension(200, 40));
		
		JPanel panelControl = new JPanel(new BorderLayout());
		panelControl.add(smartElementsScrollPane, BorderLayout.CENTER);
		panelControl.add(mControlView, BorderLayout.SOUTH);
		add(panelControl, BorderLayout.WEST);

		
		mFooterInfo = new FooterInfo("Verbindung unbekannt");
		this.add(mFooterInfo, BorderLayout.SOUTH);
		
		this.addWindowListener(new WindowAdapter() {
			
			@Override
			public void windowClosing(WindowEvent e ) {
				//Schliesst den Controller und alles was damit zu tun hat
				mDeviceManager.shutdown();
				
				//Schliesst das Frame fuer die Anzeige der Systeminformationen des Remote PIs
				if(mControlView != null) {
					mControlView.close();
				}
				
				//Schliesst alle View Komponenten der SmartHome Elemente
				if(mComponentViews != null && !mComponentViews.isEmpty()) {
					
					for( Entry<Integer, IComponentView> view : mComponentViews.entrySet()) {
						view.getValue().close();
					}
				}
				mLogger.write(this, "Window Closing", LoggerMode.TRACE);
				mParent.dispose();
			}	
		});
		
		this.pack();
		
		//Setze Fenster beim start in die Mitte
				int screenMiddleWidth = Toolkit.getDefaultToolkit()
			            .getScreenSize().width/2;
				int screenMiddleHeight = Toolkit.getDefaultToolkit()
			            .getScreenSize().height/2;
		setLocation(screenMiddleWidth-this.getWidth()/2, screenMiddleHeight-this.getHeight()/2);
	}
	
	/**
	 * initialize the menues
	 */
	private void setMenues() {
		
		JMenuBar menubar = new JMenuBar();
		JMenu settings = new JMenu("Einstellungen");
		menubar.add(settings);
		JMenu help = new JMenu("Hilfe");
		menubar.add(help);

		
		// setzte Items fuer Datei
		JMenuItem networkSettings = new JMenuItem("Netzwerkeinstellungen");
		networkSettings.addActionListener(e->{
			mLogger.write(this, "Netzwerkeinstellungen", LoggerMode.TRACE);
			ConfigDialog dialog = new ConfigDialog(this,"Netzwerkeinstellungen", mDeviceManager);
			dialog.setVisible(true);
		});
		settings.add(networkSettings);
		
		JMenuItem info = new JMenuItem("Info");
		info.addActionListener(e->{
			mLogger.write(this, "Info", LoggerMode.TRACE);
			JOptionPane.showMessageDialog(this,
					"Verson:                1.0 \n" + 
					"Institut:                 Hochschule Bremerhaven\n" + 
					"Modul:                   Projektarbeit Semester 6/7\n" + 
					"Veranstalter:       Prof. Dr. Peter Kelb\n" + 
					"Projekttitel:          Safer-Smart-Home\n",
					"Info", JOptionPane.INFORMATION_MESSAGE);
		});
		help.add(info);
		
		JMenuItem helpx = new JMenuItem("Hilfe");
		helpx.addActionListener(e->{
			mLogger.write(this, "Hilfe", LoggerMode.TRACE);
			JOptionPane.showMessageDialog(this,
					"Moechten Sie eine Verbindung abbauen koennen Sie das Fenster einfach Schliessen.\n"
					+ "Moechten Sie die Verbindung aendern, nutzen Sie die Einstellungen oder die Konfigurationsdatei (safer_smart_home.config)",
					"Hilfe", JOptionPane.INFORMATION_MESSAGE);
		});
		help.add(helpx);
		
		
//		JMenuItem disconnect = new JMenuItem("Verbindung abbauen");
//		disconnect.addActionListener(e->{
//			mLogger.write(this, "Verbindung abbauen", LoggerMode.TRACE);
//		});
//		menubar.add(disconnect);
		this.setJMenuBar(menubar);
		
	}
	
	/**
	 * Updates the ListView of SmartElements. Remove all entries and gets fresh data from model. 
	 */
	public void updateListElements() {
		if(mSmartElementsListView != null) {
			mSmartElementsListView.removeAll();
		}else {
			
			mSmartElementsListView = new JList<Device>();
			mSmartElementsListView.addListSelectionListener(new ListSelectionListener() {

				@Override
				public void valueChanged(ListSelectionEvent e) {
					if(e.getValueIsAdjusting() == false) {
						setCurrentPanel(mSmartElementsListView.getSelectedValue());
					}
				}
				
			});
		}
		
		if( mModel.getSmartElements() != null ) {
			mSmartElementsListView.setListData(mModel.getSmartElements());
			createViews();
		}else {
			mLogger.write(this, "No smart elements for listview.", LoggerMode.WARN);
		}
		 
	}
	
	/**
	 * Re-/generates the views of the elements
	 */
	private void createViews() {
		Vector<Device> devices = mModel.getSmartElements();
		if(devices != null ) {
			//das put bei der map ersetzt ein Objekt falls der schluessel doppelt vorhanden ist.
			for(int i = 0; i < devices.size(); ++i) {
				if(devices.get(i).getType() == Device.Type.RASPBERRY) {
					mComponentViews.put(devices.get(i).getId(), new RaspberryView(mDeviceManager, devices.get(i)));
				}else if(devices.get(i).getType() == Device.Type.SOCKET) {
					mComponentViews.put(devices.get(i).getId(), new SmartSwitchView(mDeviceManager, devices.get(i)));
				}else if(devices.get(i).getType() == Device.Type.CAMERA) {
					mComponentViews.put(devices.get(i).getId(), new SmartCameraView(mDeviceManager, devices.get(i)));
				}else {
					mLogger.write(this, "No view exists for this device type.", LoggerMode.ERROR);
				}
				
			}
		}else {
			mLogger.write(this, "No views created. No devices available.", LoggerMode.WARN);
		}

		
	}
	
	/**
	 * Updates a single view with the given data
	 * @param device
	 */
	public void updateView(Device device) {
		if(mComponentViews.get(device.getId()) != null ) {
			mComponentViews.get(device.getId()).update(device);
		}else {
			mLogger.write(this, "No view for device.", LoggerMode.ERROR);
		}
	}
	
	/**
	 * Updates a single view with the given data
	 * @param device
	 */
	public void updateView(Device device, Image image) {
		if(mComponentViews.get(device.getId()) != null ) {
			mComponentViews.get(device.getId()).update(device, image);
		}else {
			mLogger.write(this, "No view for device.", LoggerMode.ERROR);
		}
	}
	
	/**
	 * Update the control view with the given data
	 * @param device
	 */
	public void updateControl(Control control) {
		mControlView.update(control);
	}

	/**
	 * changes the center panel to the view of the given device
	 * @param device
	 */
	private void setCurrentPanel(Device device) {
		
		if(device != null) {
			mLogger.write(this, "setCurrentPanel", LoggerMode.TRACE);
			mCenter.removeAll();
			
			
			IComponentView tmp = mComponentViews.get(device.getId());
			if(tmp != null) {
				if(tmp.getViewType() == ViewType.RASPBERRY) {
					mCenter.add( (RaspberryView)mComponentViews.get(device.getId()) );
				}else if(tmp.getViewType() == ViewType.SMARTCAMERA) {
					mCenter.add( (SmartCameraView)mComponentViews.get(device.getId()) );
				}else if(tmp.getViewType() == ViewType.SMARTSWITCH) {
					mCenter.add( (SmartSwitchView)mComponentViews.get(device.getId()) );
				}else {
					mLogger.write(this, "Device has a unsupported type or the view is not initialized.",LoggerMode.ERROR);
				}
			}
			
			this.validate();
			this.repaint();
		}else {
			mLogger.write(this, "No current panel is set.", LoggerMode.WARN);
		}
		
	}
	
	private static final long serialVersionUID = 1L;
	private Logger mLogger = Logger.getLogger();
	private Model mModel = null;
	private IDeviceManager mDeviceManager;
	private JPanel mCenter;
	private SSH_MainFrame mParent;
	private ControlView mControlView;
	private JList<Device> mSmartElementsListView;
	private Map<Integer, IComponentView> mComponentViews = new HashMap<Integer, IComponentView>();
	public FooterInfo mFooterInfo;
	
	
	
	/*
	 * Raspberry in Devicelist? Wie soll das gehen? 
	 * Was ist mit den Werten IP, Power, Energy?
	 * Der Raspberry hat auch IP adressen
	 * (Connectionmode (IP, DECT))
	 * Actor ID (AID)
	 * 
	 * 
	 * Nur ein Device in ein Transmission obj? wie geht das?
	 */
	
}
