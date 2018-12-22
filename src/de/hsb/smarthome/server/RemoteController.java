package de.hsb.smarthome.server;

import java.io.IOException;
import java.net.InetAddress;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import javax.xml.parsers.ParserConfigurationException;

import org.xml.sax.SAXException;

import com.kilo52.common.io.ConfigurationFile;
import com.kilo52.common.io.ConfigurationFileHandler;

import de.hsb.smarthome.server.Scheduler.CycleListener;
import de.hsb.smarthome.util.json.Device;
import de.hsb.smarthome.util.json.Device.Type;
import de.hsb.smarthome.util.log.Logger;
import de.hsb.smarthome.util.log.Logger.LoggerMode;

/**
 * Controls the program flow on the remote side of the Safer Smart Home project.
 * Provides the server service {@link RemoteSocket} for the client program.
 * 
 * @author Fabian Mangels
 *
 */
public class RemoteController implements CycleListener {

	/**
	 * @param home_ip
	 *            IP address in the home network
	 * @param remote_gateway
	 *            IP address of the remote gateway (FRITZ!Box)
	 * @param ip_camera
	 *            IP address of the ip camera
	 * @param fb_username
	 *            Username for the FRITZ!Box
	 * @param fb_password
	 *            Password for the FRITZ!Box
	 * @param commPort
	 *            Serial interface ("/dev/ttyS0")
	 * @param phoneNumber
	 *            Phone number ("+4915000000000")
	 * @param port
	 *            Port, for connecting to the client program (50001)
	 * @param timeout
	 *            Timeout for the {@link RemoteSocket} (time in ms)
	 */
	public RemoteController(String home_ip, String remote_gateway, String ip_camera, String fb_username,
			String fb_password, String commPort, String[] phoneNumbers, int port, int timeout) {
		HOME_IP = home_ip;
		REMOTE_GATEWAY = remote_gateway;
		IP_CAMERA = ip_camera;
		FB_USERNAME = fb_username;
		FB_PASSWORD = fb_password;
		AT_CALL_HANDLER = new ATCallHandler(commPort, phoneNumbers);
		REM_SOCKET = new RemoteSocket(this, port, timeout);

		loop();
	}

	/**
	 * @param confFilePath
	 *            Configuration file using the library claymore
	 * @throws ProcessException
	 */
	public RemoteController(String confFilePath) throws ProcessException {
		try {
			LOGGER.write(this, "Read from the conf file: " + confFilePath, LoggerMode.INFO);

			ConfigurationFileHandler confFileHandler = new ConfigurationFileHandler(confFilePath);
			ConfigurationFile confFile = confFileHandler.read();

			HOME_IP = confFile.getSection("general").valueOf("homeIp");
			REMOTE_GATEWAY = confFile.getSection("general").valueOf("remoteGateway");
			IP_CAMERA = confFile.getSection("general").valueOf("ipCamera");
			FB_USERNAME = confFile.getSection("general").valueOf("fbUserName");
			FB_PASSWORD = confFile.getSection("general").valueOf("fbPassword");

			String commPort = confFile.getSection("general").valueOf("commPort");
			String[] phoneNumbers = confFile.getSection("general").valueOf("phoneNumbers").split("([,])");
			int timeout = Integer.valueOf(confFile.getSection("general").valueOf("timeout"));
			int port = Integer.valueOf(confFile.getSection("general").valueOf("port"));

			AT_CALL_HANDLER = new ATCallHandler(commPort, phoneNumbers);
			REM_SOCKET = new RemoteSocket(this, port, timeout);
		} catch (IOException e) {
			LOGGER.write(this, "Failed to read the conf file: " + confFilePath, LoggerMode.ERROR);
			throw new ProcessException("Failed to read the conf file: " + confFilePath);
		}

		loop();
	}

	private void loop() {
		mDevices = getDevices();

		boolean isAlright = true;
		while (isAlright) {
			try {
				AT_CALL_HANDLER.handleCall();

				startPPP();
				startVPNC();

				startRSock();

				endVPNC();
				endPPP();
			} catch (Exception e) {
				isAlright = false;
				e.printStackTrace();
			}
		}
	}

	/**
	 * Starts a point-to-point connection. Building a mobile internet connection
	 * with the Deamon pppd. Deamon uses the GSM module.
	 * 
	 * @throws IOException
	 * @throws InterruptedException
	 * @throws ProcessException
	 */
	private void startPPP() throws IOException, InterruptedException, ProcessException {
		LOGGER.write(this, "Mobile Internet connection is being set up ...", LoggerMode.INFO);
		Process processPPP = PROCESS_BUILDER_PPP.start();
		processPPP.waitFor(TIMEOUT_IN_MS, TimeUnit.MILLISECONDS);

		if (processPPP.isAlive()) {
			LOGGER.write(this, "pppd could be started!", LoggerMode.INFO);
		} else {
			LOGGER.write(this, "pppd could not be started!", LoggerMode.ERROR);
			throw new ProcessException("pppd could not be started!");
		}
	}

	/**
	 * Establishes a VPN connection (IPSec) using the program vpnc. Test whether the
	 * home gateway is reachable.
	 * 
	 * @throws IOException
	 * @throws InterruptedException
	 * @throws ProcessException
	 */
	private void startVPNC() throws IOException, InterruptedException, ProcessException {
		LOGGER.write(this, "VPN connection is established ...", LoggerMode.INFO);
		PROCESS_BUILDER_VPNC.start();
		boolean reachable = InetAddress.getByName(HOME_IP).isReachable(TIMEOUT_IN_MS);

		if (reachable) {
			LOGGER.write(this, "vpnc could be started!", LoggerMode.INFO);
		} else {
			LOGGER.write(this, "vpnc could not be started!", LoggerMode.ERROR);
			throw new ProcessException("vpnc could not be started!");
		}
	}

	/**
	 * Starts the {@link RemoteSocket} as a stand-alone thread.
	 * 
	 * @throws InterruptedException
	 */
	private void startRSock() throws InterruptedException {
		LOGGER.write(this, "RemoteSocket is started ...", LoggerMode.INFO);

		REM_SOCKET.start();
	}

	/**
	 * Ends the VPN connection. The vpnc Deamon is terminated.
	 * 
	 * @throws IOException
	 * @throws InterruptedException
	 * @throws ProcessException
	 */
	private void endVPNC() throws IOException, InterruptedException, ProcessException {
		LOGGER.write(this, "VPN connection is disconnected ...", LoggerMode.INFO);
		Process processVPNC = PROCESS_BUILDER_VPNC_END.start();
		boolean result = processVPNC.waitFor(TIMEOUT_IN_MS, TimeUnit.MILLISECONDS);

		if (!result) {
			LOGGER.write(this, "The VPN connection could not be finished in the given time!", LoggerMode.ERROR);
			throw new ProcessException("The VPN connection could not be finished in the given time!");
		}
	}

	/**
	 * Ends the point-to-point connection. The Deamon pppd is terminated.
	 * 
	 * @throws InterruptedException
	 * @throws ProcessException
	 * @throws IOException
	 */
	private void endPPP() throws InterruptedException, ProcessException, IOException {
		LOGGER.write(this, "Mobile Internet connection is disconnected ...", LoggerMode.INFO);
		Process processPPP = PROCESS_BUILDER_PPP_END.start();
		// mProcessPPP.destroy();
		// mProcessPPP.destroyForcibly();
		boolean result = processPPP.waitFor(TIMEOUT_IN_MS, TimeUnit.MILLISECONDS);

		if (!result) {
			LOGGER.write(this, "The mobile internet connection could not be finished in the given time!",
					LoggerMode.ERROR);
			throw new ProcessException("The mobile internet connection could not be finished in the given time!");
		}
	}

	/**
	 * Returns all devices that are available in the network (Fritz!Box).
	 */
	public static List<Device> getDevices() {
		List<Device> listDevices = new ArrayList<Device>();

		try {
			String sessionId = "0000000000000000";
			for (int i = 0; i < TEST_COUNT && sessionId.equals("0000000000000000"); i++) {
				sessionId = FritzBoxConnection.getSessionId(REMOTE_GATEWAY, FB_USERNAME, FB_PASSWORD);
			}

			listDevices = FritzBoxConnection.getDeviceListInfos(REMOTE_GATEWAY, sessionId);
			FritzBoxConnection.logout(REMOTE_GATEWAY, sessionId);

		} catch (KeyManagementException | NoSuchAlgorithmException | IOException | ParserConfigurationException
				| SAXException e) {
			e.printStackTrace();
		}

		int counter = listDevices.size();

		Device device2 = new Device();
		device2.setId(++counter);
		device2.setType(Type.CAMERA);
		device2.setName("IPCamera");

		boolean reachable = false;
		try {
			reachable = InetAddress.getByName(IP_CAMERA).isReachable(10000);
		} catch (IOException e) {
			e.printStackTrace();
		}

		if (reachable) {
			device2.setConnected(true);
			device2.setStatus(1);
		} else {
			device2.setConnected(false);
			device2.setStatus(0);
		}

		listDevices.add(device2);

		return listDevices;
	}

	@Override
	public void onCycleStart(Device device) {
		try {
			if (device.getType().equals(Type.SOCKET)) {
				String sessionId = "0000000000000000";
				for (int i = 0; i < TEST_COUNT && sessionId.equals("0000000000000000"); i++) {
					sessionId = FritzBoxConnection.getSessionId(REMOTE_GATEWAY, FB_USERNAME, FB_PASSWORD);
				}

				FritzBoxConnection.sendDeviceCommand(REMOTE_GATEWAY, device.getAid(), CMD_SWITCH_ON, sessionId);

				FritzBoxConnection.logout(REMOTE_GATEWAY, sessionId);
				device.setStatus(1);
			}

			LOGGER.write(this, "The device (" + device.getName() + ") with ID " + device.getId()
					+ " could be switched on in the time interval!", LoggerMode.INFO);
		} catch (Exception e) {
			LOGGER.write(this, "The device (" + device.getName() + ") with ID " + device.getId()
					+ " could not be switched on in the time interval!", LoggerMode.WARN);
		}
	}

	@Override
	public void onCycleStop(Device device) {
		try {
			if (device.getType().equals(Type.SOCKET)) {
				String sessionId = "0000000000000000";
				for (int i = 0; i < TEST_COUNT && sessionId.equals("0000000000000000"); i++) {
					sessionId = FritzBoxConnection.getSessionId(REMOTE_GATEWAY, FB_USERNAME, FB_PASSWORD);
				}

				FritzBoxConnection.sendDeviceCommand(REMOTE_GATEWAY, device.getAid(), CMD_SWITCH_OFF, sessionId);

				FritzBoxConnection.logout(REMOTE_GATEWAY, sessionId);
				device.setStatus(0);
			}

			LOGGER.write(this, "The device (" + device.getName() + ") with ID " + device.getId()
					+ " could be switched off in the time interval!", LoggerMode.INFO);
		} catch (Exception e) {
			LOGGER.write(this, "The device (" + device.getName() + ") with ID " + device.getId()
					+ " could not be switched off in the time interval!", LoggerMode.WARN);
		}
	}

	private final ProcessBuilder PROCESS_BUILDER_PPP = new ProcessBuilder("sudo", "pppd", "call", "gprs");
	private final ProcessBuilder PROCESS_BUILDER_PPP_END = new ProcessBuilder("sudo", "killall", "pppd");
	private final ProcessBuilder PROCESS_BUILDER_VPNC = new ProcessBuilder("sudo", "vpnc-connect",
			"/etc/vpnc/fritzbox.conf");
	private final ProcessBuilder PROCESS_BUILDER_VPNC_END = new ProcessBuilder("sudo", "vpnc-disconnect");

	// Timeout is 2 minutes in ms
	private final int TIMEOUT_IN_MS = 1000 * 60 * 2;
	private final Logger LOGGER = Logger.getLogger();
	private final ATCallHandler AT_CALL_HANDLER;
	private final RemoteSocket REM_SOCKET;

	static final String CMD_SWITCH_OFF = "setswitchoff";
	static final String CMD_SWITCH_ON = "setswitchon";
	static final int TEST_COUNT = 5;
	static final Scheduler SCHEDULER = Scheduler.getInstance();

	private static String HOME_IP;
	private static String IP_CAMERA;
	static String REMOTE_GATEWAY;
	static String FB_USERNAME;
	static String FB_PASSWORD;
	static List<Device> mDevices = new ArrayList<Device>();

	/**
	 * Exception thrown when errors occur in the {@link RemoteController}.
	 * 
	 * @author Fabian Mangels
	 *
	 */
	class ProcessException extends Exception {

		public ProcessException(String msg) {
			super(msg);
		}

		private static final long serialVersionUID = 1L;
	}
}