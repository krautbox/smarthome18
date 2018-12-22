package de.hsb.smarthome.alternative;

import java.io.IOException;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.List;

import javax.xml.parsers.ParserConfigurationException;

import org.xml.sax.SAXException;

import com.kilo52.common.io.ConfigurationFile;
import com.kilo52.common.io.ConfigurationFileHandler;

import de.hsb.smarthome.server.FritzBoxConnection;
import de.hsb.smarthome.util.json.Device;
import de.hsb.smarthome.util.log.Logger;
import de.hsb.smarthome.util.log.Logger.LoggerMode;

/**
 * Controls the program flow on the remote side of the Safer Smart Home project
 * (alternative).
 * 
 * @author Fabian Mangels
 *
 */
public class RemoteController {

	/**
	 * @param remote_gateway
	 *            IP address of the remote gateway (FRITZ!Box)
	 * @param fb_username
	 *            Username for the FRITZ!Box
	 * @param fb_password
	 *            Password for the FRITZ!Box
	 * @param commPort
	 *            Serial interface ("/dev/ttyS0")
	 * @param phoneNumbers
	 *            Phone numbers as string array ("+4915000000000", "...")
	 * @param timeout
	 *            Timeout for the {@link ATCallHandler} (time in ms)
	 * @param pinCode
	 *            Pin code to get access via DTMF input
	 */
	public RemoteController(String remote_gateway, String fb_username, String fb_password, String commPort,
			String[] phoneNumbers, int timeout, String pinCode, int mode) {
		REMOTE_GATEWAY = remote_gateway;
		FB_USERNAME = fb_username;
		FB_PASSWORD = fb_password;
		AT_CALL_HANDLER = new ATCallHandler(commPort, phoneNumbers, timeout, pinCode, mode);

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

			REMOTE_GATEWAY = confFile.getSection("general").valueOf("remoteGateway");
			FB_USERNAME = confFile.getSection("general").valueOf("fbUserName");
			FB_PASSWORD = confFile.getSection("general").valueOf("fbPassword");

			String commPort = confFile.getSection("general").valueOf("commPort");
			String[] phoneNumbers = confFile.getSection("general").valueOf("phoneNumbers").split("([,])");

			int timeout = Integer.valueOf(confFile.getSection("alternative").valueOf("timeout"));
			String pinCode = confFile.getSection("alternative").valueOf("pinCode");
			int mode = Integer.valueOf(confFile.getSection("alternative").valueOf("mode"));

			AT_CALL_HANDLER = new ATCallHandler(commPort, phoneNumbers, timeout, pinCode, mode);
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
			} catch (Exception e) {
				isAlright = false;
				e.printStackTrace();
			}
		}
	}

	/**
	 * Returns all devices that are available in the network (FRITZ!Box).
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

		return listDevices;
	}

	private final Logger LOGGER = Logger.getLogger();
	private final ATCallHandler AT_CALL_HANDLER;

	static String REMOTE_GATEWAY;
	static String FB_USERNAME;
	static String FB_PASSWORD;
	static final String CMD_SWITCH_OFF = "setswitchoff";
	static final String CMD_SWITCH_ON = "setswitchon";
	static final int TEST_COUNT = 5;
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