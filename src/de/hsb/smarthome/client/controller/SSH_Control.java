package de.hsb.smarthome.client.controller;

import java.io.IOException;
import java.util.List;

import javax.swing.JOptionPane;

import com.kilo52.common.io.ConfigurationFile;
import com.kilo52.common.io.ConfigurationFileHandler;

import de.hsb.smarthome.client.controller.ConnectionCheck.Status;
import de.hsb.smarthome.client.model.Model;
import de.hsb.smarthome.client.view.LoadingDialog;
import de.hsb.smarthome.client.view.SSH_MainFrame;
import de.hsb.smarthome.util.json.Control;
import de.hsb.smarthome.util.json.Device;
import de.hsb.smarthome.util.json.Image;
import de.hsb.smarthome.util.json.Transmission;
import de.hsb.smarthome.util.json.Transmission.Action;
import de.hsb.smarthome.util.log.Logger;
import de.hsb.smarthome.util.log.Logger.LoggerMode;
import de.hsb.smarthome.util.transmission.TCPConnection;
import de.hsb.smarthome.util.transmission.TCPConnection.ConnectionState;

public class SSH_Control implements TCPConnection.IConnectionCallback, TCPConnection.IDataOnTcpPortObserver, IDeviceManager, ConnectionCheck.IConnectionState{

	public SSH_Control(String ip, int port) {

		model = new Model();
		mIP = ip;
		mPort = port;
		
		
	}
	
	public SSH_Control(String configFilePath) {

		model = new Model();
		try {
			mConfigFile = new ConfigurationFileHandler(configFilePath);
			mConfig = mConfigFile.read();
			mIP = mConfig.getSection("Global").valueOf("RemoteIP");
			mPort = Integer.valueOf(mConfig.getSection("Global").valueOf("RemotePort"));
			mLogger.write(this,"Read IP from config file: " +  mIP, LoggerMode.INFO);
			mLogger.write(this, "Read port from config file: " + mPort, LoggerMode.INFO);
			
		} catch (IOException e) {
			mLogger.write(this, "Failed to read config file: " + configFilePath, LoggerMode.ERROR);
			mLogger.write(this, "Try to set config infos with paramters.", LoggerMode.INFO);
			e.printStackTrace();
		}
	}
	
	public SSH_Control(String ip, int port, boolean debug) {
		this(ip, port);
		this.mDebug = debug;
	}
	
	public SSH_Control(String configFilePath, boolean debug) {
		this(configFilePath);
		this.mDebug = debug;
	}

	/**
	 * Initial method for startup
	 */
	public void startGUI() {
		LoadingDialog myDialog = new LoadingDialog(null, this);
		myDialog.setTitle("Versuche Verbindung herzustellen");
		myDialog.setVisible(true);

		if(!mDebug) {
			createTCPConnection(mIP, mPort);
		}
		myDialog.dispose();

		if (this.mGotInitialData || mDebug) {
			mMainFrame = new SSH_MainFrame("Safer-Smarter-Home", model, this);
			mMainFrame.setVisible(true);
			if(!mDebug) {
				mConnectionCheck = new ConnectionCheck(mIP, 500, 1000, 5, this);
			}
		} else {
			mLogger.write(this, "No connection Possible", LoggerMode.ERROR);
			
		}
	}

	/**
	 * Creates a TCP-Connection to the given IP and Port. 
	 * 
	 * @param IP have to be in the form \"192.168.128.111\"
	 * @param port - a number between 0 and 65535
	 */
	private void createTCPConnection(String IP, int port) {
		mTcpConnection = TCPConnection.getClientInstance(IP, port, 10000, this);
		//Warten bis TCP-Verbindung aufgebaut ist.
		try {
			mTcpConnection.join();
		} catch (InterruptedException e2) {
			mLogger.write(this, "Cant", LoggerMode.ERROR);
			e2.printStackTrace();
		}
		
		//Als zurhoerer bei der Verbindung registrieren.
		//null-pointer check, falles was schief gegangen ist wird hier benoetigt, 
		//weil bei einem CLOSED signal die Variable auf null gesetzt wird (siehe tcpConnectionCallback)
		if(mTcpConnection != null && !mTcpConnection.register(this)) {
			mLogger.write(this, "controller registration in TCP-connection failed", LoggerMode.ERROR);
		}else {
			return;
		}
		
		//initiale Abfrage fuer die Elemente
		Transmission trans = new Transmission();
		trans.setAction(Action.REPORT);
		
		//Bekomme die Initialen Daten
		try {
			mLogger.write(this, "Try to send initial request.", LoggerMode.INFO);
			//null-pointer check, falls was schief gegangen ist wird hier benoetigt, 
			//weil bei einem CLOSED signal die Variable auf null gesetzt wird (siehe tcpConnectionCallback)
			if(mTcpConnection != null && !mTcpConnection.sendTransmission(trans)) {
				mLogger.write(this, "Initial request failed.", LoggerMode.ERROR);
			}else {
				return;
			}
		} catch (IOException e) {
			mLogger.write(this, "Error while sending the initial request", LoggerMode.ERROR);
			mLogger.write(this, e.getMessage(), LoggerMode.ERROR);
			
		}
		
		//Warte bis die initialen Daten da sind.
		synchronized(this) {
			try {
				wait();
			} catch (InterruptedException e) {
				mLogger.write(this, "Error while waiting for notify", LoggerMode.ERROR);
				e.printStackTrace();
			}
		}
		
	}
	

	public TCPConnection getTCPConnection() {
		return mTcpConnection;
	}
	
	
	@Override
	public void tcpConnectionCallback(ConnectionState state) {
		mConnectionState = state;
		mLogger.write(this, "tcpConnectionCallback " + state, LoggerMode.TRACE);
		
		switch(mConnectionState){
		case TIMEOUT: 
			this.shutdown();
			synchronized (this) {
				notify();
			}
			break;
		case ERROR:
			break;
		case CLOSED:
			mTcpConnection = null;
			break;
		default: 
			break;
		}
		
	}
	
	@Override
	public void receiveTransmission(Transmission obj) {
		mLogger.write(this, "Receive Data", LoggerMode.TRACE);
		if(!mGotInitialData){
			
			if(obj.getControl() != null) {
				model.setControl(obj.getControl());
			}
			
			if(obj.getDevices() != null) {
				model.setSmartElements(obj.getDevices());
			}
			
			mLogger.write(this, obj.getMessage(), LoggerMode.INFO);
			synchronized (this) {
				notify();
			}
			mGotInitialData = true;
		}else {
			switch(obj.getAction()) {
			case REPORT:
				if(obj.getControl() != null) {
					updateControl(obj.getControl());
				}
				
				if(obj.getDevices() != null) {
					updateViews(obj.getDevices());
				}
				
				break;
			case COMMIT:
				break;
			case SUCCESS:
				
				if(obj.getImages() != null) {
					updateViews(obj.getDevices(), obj.getImages());
					
				}else {
					updateViews(obj.getDevices());
				}
				break;
			case ERROR:
				mLogger.write(this, "ERROR in receive", LoggerMode.ERROR);
				break;
			case CLOSE:
				mTcpConnection.close();
				break;
			}
	
		}
	}
	
	public void updateControl(Control control) {
		mMainFrame.updateControl(control);
	}
	
	/**
	 * Get a Device List and updates all Views on in the GUI
	 * @param list
	 */
	public void updateViews(List<Device> list) {
		if(list != null) {
			for(int i = 0; i < list.size(); ++i) {
				mMainFrame.updateView(list.get(i));
			}
		}else {
			mLogger.write(this, "JSON action was succes, devicelist is null", LoggerMode.WARN);
		}
	}
	
	/**
	 * Get a Device List and an image list. Then updates all Views on in the GUI  including the pictures.
	 * @param list
	 */
	public void updateViews(List<Device> deviceList, List<Image> imageList) {
		if(deviceList != null && imageList != null) {
			
			if(deviceList.get(0).getType() == Device.Type.CAMERA) {
				mMainFrame.updateView(deviceList.get(0), imageList.get(0));
			}else {
				mLogger.write(this, "JSON devicetype is not camera", LoggerMode.WARN);
			}
			
		}else {
			mLogger.write(this, "JSON action was succes, device and imagelist is null", LoggerMode.WARN);
		}
	}
	
	@Override
	public void commitDevice(Device device) {
		mLogger.write(this, "Commit Device", LoggerMode.TRACE);
		mConnectionCheck.setCommitSend(true);
		if(mTcpConnection != null) {
			Transmission trans = new Transmission();
			trans.setAction(Action.COMMIT);
			trans.setMessage("test test test");
			trans.addDevice(device);
			try {
				mTcpConnection.sendTransmission(trans);
			} catch (IOException e) {
				mLogger.write(this, e.getMessage(), LoggerMode.ERROR);
				mLogger.write(this, "Error while sending", LoggerMode.ERROR);
			}
		}else {
			mLogger.write(this,"TCPConnection not initialized", LoggerMode.WARN);
		}
	}
	
	private void closeTCPConnection() {
		if(mTcpConnection != null) {
			mTcpConnection.close();
		}
	}
	
	@Override
	public void shutdown() {
		mLogger.write(this, "Shutdown programm", LoggerMode.INFO);
		closeTCPConnection();
		if(mConnectionCheck != null) {
			mConnectionCheck.stopConnectionCheck();
		}
	}
	
	@Override
	public void connectionStateChanged(Status status) {
		if(status == Status.OK) {
			mMainFrame.mFooterInfo.setInfo("Verbindung OK");
			mLogger.write(this, "Connection state changed to \"Connection OK\"", LoggerMode.INFO);
			if(mTcpConnection == null) {
				mLogger.write(this, "Try to recreate a TCP-Connection", LoggerMode.INFO);
				createTCPConnection(mIP, mPort);
				
			}
		}else if (status == Status.UNCLEAR) {
			mMainFrame.mFooterInfo.setInfo("Verbindung unbekannt");
			mLogger.write(this, "Connection state changed to \"Connection UNCLEAR\"", LoggerMode.INFO);
		}else if (status == Status.LOST){
			mMainFrame.mFooterInfo.setInfo("Verbindung verloren");
			mLogger.write(this, "Connection state changed to \"Connection LOST\"", LoggerMode.INFO);
			if(JOptionPane.YES_OPTION == JOptionPane.showConfirmDialog(null,"Die Verbindung zum Server wurde verloren. Eventuell kann eine Verbindung erneut"
					+ "hergestellt werden. Das Programm wird bis zu 5 min versuchen erneut eine Verbindung herzustellen."
					+ "Wollen Sie warten?\n"
					+ "Wenn Sie nicht warten wird die Anwendung beendet .","Verbindung verloren",
					JOptionPane.YES_NO_CANCEL_OPTION, JOptionPane.WARNING_MESSAGE)) {
				
				
			}else {// TIMEOUT
				shutdown();
			}
		}
		
	}
	
	@Override
	public ConfigurationFile getConfig() {
		if(mConfig == null) {
			mLogger.write(this, "Create tmp config", LoggerMode.INFO);
			mConfig = new ConfigurationFile();
			mConfig.addSection(new ConfigurationFile.Section("Global"));
			mConfig.getSection("Global").set("RemoteIP", mIP);
			mConfig.getSection("Global").set("RemotePort", String.valueOf(mPort));
			mTmpConfig = true;
		}
		return mConfig;
	}
	
	@Override
	public void writeConfig(ConfigurationFile config) {
		if(mTmpConfig) {
			return;
		}else {
			if(mConfigFile != null) {
				try {
					mLogger.write(this, "Write config file with current configs.", LoggerMode.INFO);
					mConfigFile.write(config);
				} catch (IOException e) {
					mLogger.write(this, "Failed to write config file.", LoggerMode.ERROR);
					e.printStackTrace();
				}
			}
		}
	}
	
	private Model model = null;
	private Logger mLogger = Logger.getLogger();
	private TCPConnection mTcpConnection;
	private ConnectionState mConnectionState = ConnectionState.UNCONNECTED;
	private ConnectionCheck mConnectionCheck;
	private boolean mGotInitialData = false;
	private SSH_MainFrame mMainFrame;
	private String mIP = "";
	private int mPort = 0;
	private ConfigurationFileHandler mConfigFile = null;
	private ConfigurationFile mConfig = null;
	private boolean mDebug = false; //without TCPConnection
	private boolean mTmpConfig = false;
}
