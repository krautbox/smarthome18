package de.hsb.smarthome.alternative;

import java.io.PrintWriter;
import java.util.Arrays;

import com.fazecast.jSerialComm.SerialPort;

import de.hsb.smarthome.server.FritzBoxConnection;
import de.hsb.smarthome.util.json.Device;
import de.hsb.smarthome.util.log.Logger;
import de.hsb.smarthome.util.log.Logger.LoggerMode;

/**
 * Manages phone calls and communicates with the GSM shield using the AT command
 * set (alternative). The external library "jSerialComm" is required.
 * 
 * @author Fabian Mangels
 *
 */
public class ATCallHandler {

	/**
	 * @param commPort
	 *            Serial interface ("/dev/ttyS0")
	 * @param phoneNumbers
	 *            Phone numbers as string array ("+4915000000000", ...)
	 * @param timeout
	 *            time in ms
	 * @param pinCode
	 *            Pin code to get access via DTMF input
	 * @param mode
	 *            Modes of alternative solution
	 */
	public ATCallHandler(String commPort, String[] phoneNumbers, int timeout, String pinCode, int mode) {
		COMM_PORT = commPort;
		PHONE_NUMBERS = phoneNumbers;
		TIMEOUT = timeout;
		PIN_CODE = pinCode;
		MODE = mode;
		mLogger = Logger.getLogger();
	}

	/**
	 * Called in the {@link RemoteController} to use the phone function of the GSM
	 * module. The call application logic is here placed.
	 * 
	 * @throws GSMShieldException
	 */
	public void handleCall() throws GSMShieldException {
		setup();
		loop();
		closePort();
	}

	/**
	 * Starts the initialization process of the GSM module. Opens the serial port
	 * and controls the connection to the mobile network.
	 * 
	 * @throws GSMShieldException
	 */
	private void setup() throws GSMShieldException {
		openPort();

		// Is the GSM module started?
		boolean result = false;
		for (int i = 0; i < TEST_COUNT && !result; i++) {
			result = sendATcommand(COMMAND_POWER_ON, ANSWER_OK, 2000);
		}

		if (result) {
			mLogger.write(this, "The GSM module is started!", LoggerMode.INFO);
			mLogger.write(this, "Connect to the mobile network ...", LoggerMode.INFO);
		} else {
			mLogger.write(this, "GSM module is not active!", LoggerMode.ERROR);
			throw new GSMShieldException("GSM module is not active!");
		}

		// Check whether the GSM module has registered in the mobile network or not.
		result = false;
		for (int i = 0; i < TEST_COUNT && !result; i++) {
			result = (sendATcommand(COMMAND_NETWORK_ACTIVE, ANSWER_NETWORK_ACTIVE_1, 500)
					|| sendATcommand(COMMAND_NETWORK_ACTIVE, ANSWER_NETWORK_ACTIVE_2, 500));
		}

		if (result) {
			mLogger.write(this, "Connected to the mobile network!", LoggerMode.INFO);
		} else {
			mLogger.write(this, "GSM module could not dial into the mobile network!", LoggerMode.ERROR);
			throw new GSMShieldException("GSM module could not dial into the mobile network!");
		}

		// Activate DTMF (dual-tone multi-frequency) Detection Control
		result = false;
		for (int i = 0; i < TEST_COUNT && !result; i++) {
			result = sendATcommand(COMMAND_DTMF_ON, ANSWER_OK, 2000);
		}

		if (result) {
			mLogger.write(this, "DTMF Detection Control has been activated ... Waiting for a call", LoggerMode.INFO);
		} else {
			mLogger.write(this, "DTMF Detection Control could not be activated!", LoggerMode.ERROR);
			throw new GSMShieldException("DTMF Detection Control could not be activated!");
		}
	}

	/**
	 * Waits until a call is received, if successful, checks the phone number that
	 * was called. If the phone number is correct, the transmitted codes (DTMF) are
	 * evaluated. If the telephone number is wrong, the method is called again.
	 * 
	 * AT code being handled:<br />
	 * RING<br />
	 * <br />
	 * +CLIP: "+4915000000000",145,"",0,"",0<br />
	 * OK<br />
	 * <br />
	 * +DTMF: ([0-9+#*])
	 * 
	 * @throws GSMShieldException
	 */
	private void loop() throws GSMShieldException {
		boolean isWrongNum = true;

		while (isWrongNum) {
			// Waits until a call is received ...
			while (!sendATcommand(COMMAND_WAIT, ANSWER_RING, 1000))
				;

			// Is it the right phone number?
			String phoneNumber = "";
			boolean isRightNum = false;
			for (int i = 0; i < TEST_COUNT && !isRightNum; i++) {
				String res;
				if ((res = getATcommandAnswer(ANSWER_RING_NUMBER, REGEX_RING_NUMBER, 2000)) != "") {
					res = res.replaceAll("\"", "");
					mLogger.write(this, "Received number: " + res, LoggerMode.INFO);
					isRightNum = Arrays.stream(PHONE_NUMBERS).anyMatch(res::equals);
					phoneNumber = res;
				}
			}

			boolean result = false;
			if (isRightNum) {
				mLogger.write(this, "It is called with the right number!", LoggerMode.INFO);
				isWrongNum = false;

				if (MODE == 1) {
					// Hang-up the call
					result = false;
					for (int i = 0; i < TEST_COUNT && !result; i++) {
						result = sendATcommand(COMMAND_CALL_HANG_UP, ANSWER_OK, 1000);
					}
					mLogger.write(this, "The call was hung-up? " + result, LoggerMode.INFO);

					// Call the number again
					mLogger.write(this, "Call the following number: " + phoneNumber, LoggerMode.INFO);

					result = false;
					for (int i = 0; i < TEST_COUNT && !result; i++) {
						result = sendATcommand(COMMAND_VOICE_CALL + phoneNumber + ";", ANSWER_OK, 1000);
					}
					mLogger.write(this, "The call was successful? " + result, LoggerMode.INFO);
				} else {
					// Answer the incoming call
					for (int i = 0; i < TEST_COUNT && !result; i++) {
						result = sendATcommand(COMMAND_ANSWER_CALL, ANSWER_OK, 1000);
					}
					mLogger.write(this, "The call was accepted? " + result, LoggerMode.INFO);
				}

				// Sound transmission -- welcome
				playSound(SOUND_WELCOME);

				String pinCode = "";
				if (!(MODE == 1)) {
					// Sound transmission -- pin
					playSound(SOUND_PIN);

					// Waits until the complete pin code is received ...
					long last = System.currentTimeMillis();
					while (!pinCode.contains("#") && (System.currentTimeMillis() - last) < TIMEOUT) {
						String res;
						if ((res = getATcommandAnswer(ANSWER_DTMF, REGEX_DTMF, 2000)) != "") {
							pinCode += res;
						}
					}

					mLogger.write(this, "Received Pin Code: " + pinCode, LoggerMode.INFO);
				}

				if (pinCode.equals(PIN_CODE) || MODE == 1) {
					if (!(MODE == 1)) {
						mLogger.write(this, "Right Pin Code!", LoggerMode.INFO);

						// Sound transmission -- pin_ok
						playSound(SOUND_PIN_OK);
					}

					boolean action_mode = true;
					while (action_mode) {

						// Sound transmission -- action
						playSound(SOUND_ACTION);

						// Waits until the complete action code is received ...
						String actionCode = "";
						long last = System.currentTimeMillis();
						while (!actionCode.contains("#") && (System.currentTimeMillis() - last) < TIMEOUT) {
							String res;
							if ((res = getATcommandAnswer(ANSWER_DTMF, REGEX_DTMF, 2000)) != "") {
								actionCode += res;
							}
						}
						mLogger.write(this, "Received Action Code: " + actionCode, LoggerMode.INFO);

						boolean actionResult = false;
						if (actionCode.matches(REGEX_ACTION_CODE)) {
							mLogger.write(this, "Right Action Code!", LoggerMode.INFO);
							String[] actionCodeSplit = actionCode.split(REGEX_SPLIT_ACTION_CODE);
							actionResult = switchDevice(actionCodeSplit[0], actionCodeSplit[1]);
						} else {
							mLogger.write(this, "Wrong Action Code!", LoggerMode.WARN);
						}

						if (actionResult) {
							// Sound transmission -- action_ok
							playSound(SOUND_ACTION_OK);
						} else {
							// Sound transmission -- action_error
							playSound(SOUND_ACTION_ERROR);
						}

						// Sound transmission -- action_next
						playSound(SOUND_ACTION_NEXT);

						String inputCode = "";
						last = System.currentTimeMillis();
						while (!(inputCode.contains("#") || inputCode.contains("*"))
								&& (System.currentTimeMillis() - last) < TIMEOUT) {
							String res;
							if ((res = getATcommandAnswer(ANSWER_DTMF, REGEX_DTMF, 2000)) != "") {
								inputCode += res;
							}
						}
						mLogger.write(this, "Received Input Code: " + inputCode, LoggerMode.INFO);

						if (!inputCode.contains("#")) {
							action_mode = false;
						}
					}
				} else {
					mLogger.write(this, "Wrong Pin Code!", LoggerMode.WARN);
					// Sound transmission -- pin_error
					playSound(SOUND_PIN_ERROR);
				}

			} else {
				mLogger.write(this, "It is called with the wrong number!", LoggerMode.WARN);
			}

			// Sound transmission -- goodbye
			playSound(SOUND_GOODBYE);

			// Hang-up the call
			result = false;
			for (int i = 0; i < TEST_COUNT && !result; i++) {
				result = sendATcommand(COMMAND_CALL_HANG_UP, ANSWER_OK, 1000);
			}
			mLogger.write(this, "The call was hung-up? " + result, LoggerMode.INFO);
		}
	}

	/**
	 * This method can be used to send AT commands. In addition, the associated
	 * answer is checked. Method is interrupted after a timeout.
	 * 
	 * @param command
	 *            AT command
	 * @param answer
	 *            Associated answer
	 * @param timeout
	 *            Time in ms
	 * @return Success of the command
	 * @throws GSMShieldException
	 */
	private boolean sendATcommand(String command, String answer, int timeout) throws GSMShieldException {
		// Send AT command (with line break!)
		mPrintWriter.println(command);

		long last = System.currentTimeMillis();

		// Waiting for an answer ... or timeout
		StringBuilder strBuilder = new StringBuilder();
		boolean result = false;
		do {
			// Are bytes available for reading?
			if (mSerialPort.bytesAvailable() > 0) {
				byte[] readBuffer = new byte[mSerialPort.bytesAvailable()];
				int numRead = mSerialPort.readBytes(readBuffer, readBuffer.length);

				if (numRead == -1) {
					mLogger.write(this, "Error during read access to the serial port!", LoggerMode.ERROR);
					throw new GSMShieldException("Error during read access to the serial port!");
				}

				strBuilder.append(new String(readBuffer));
				// mLogger.write(this, "DEBUG: " + strBuilder.toString(), LoggerMode.INFO);
			}
		} while (!(result = strBuilder.toString().contains(answer)) && (System.currentTimeMillis() - last) < timeout);

		return result;
	}

	/**
	 * This method can be used to get the AT command answer. Method is interrupted
	 * after a timeout.
	 * 
	 * @param answer
	 *            Answer that must be included
	 * @param match
	 *            Regular expression
	 * @param timeout
	 *            Time in ms
	 * @return String without the given answer
	 * @throws GSMShieldException
	 */
	private String getATcommandAnswer(String answer, String match, int timeout) throws GSMShieldException {
		long last = System.currentTimeMillis();

		// Waiting for an answer ... or timeout
		String str = "";
		boolean result = false;
		do {
			// Are bytes available for reading?
			if (mSerialPort.bytesAvailable() > 0) {
				byte[] readBuffer = new byte[mSerialPort.bytesAvailable()];
				int numRead = mSerialPort.readBytes(readBuffer, readBuffer.length);

				if (numRead == -1) {
					mLogger.write(this, "Error during read access to the serial port!", LoggerMode.ERROR);
					throw new GSMShieldException("Error during read access to the serial port!");
				}

				str += new String(readBuffer).replaceAll("([\\r\\n])", "");
				// str += new String(readBuffer).trim();
			}
		} while (!(result = str.matches(match)) && (System.currentTimeMillis() - last) < timeout);

		if (result) {
			return str.substring(answer.length() + 1, str.length());
		} else {
			return "";
		}
	}

	/**
	 * Serial port of the system is opened and a PrintWriter is saved.
	 * 
	 * @throws GSMShieldException
	 */
	private void openPort() throws GSMShieldException {
		mSerialPort = SerialPort.getCommPort(COMM_PORT);

		if (mSerialPort.openPort()) {
			mLogger.write(this, "Serial Port \"" + COMM_PORT + "\" could be opened!", LoggerMode.INFO);
			mLogger.write(this, mSerialPort.getDescriptivePortName() + "\t" + mSerialPort.getPortDescription() + "\t"
					+ mSerialPort.getSystemPortName(), LoggerMode.INFO);

			mPrintWriter = new PrintWriter(mSerialPort.getOutputStream(), true);
		} else {
			mLogger.write(this, "Unable to connect to the serial port \"" + COMM_PORT + "\" .", LoggerMode.ERROR);
			throw new GSMShieldException("Unable to connect to the serial port \"" + COMM_PORT + "\" .");
		}
	}

	/**
	 * Serial port of the system is closed.
	 * 
	 * @throws GSMShieldException
	 */
	private void closePort() throws GSMShieldException {
		if (mSerialPort.closePort()) {
			mLogger.write(this, "Serial Port \"" + COMM_PORT + "\" could be closed!", LoggerMode.INFO);
		} else {
			mLogger.write(this, "The serial port \"" + COMM_PORT + "\" could not be closed.", LoggerMode.ERROR);
			throw new GSMShieldException("The serial port \"" + COMM_PORT + "\" could not be closed.");
		}
	}

	/**
	 * Method to switch a device over the FritzBox.
	 * 
	 * @param id
	 *            String containing the device id
	 * @param status
	 *            String containing the status of how the device should be switched
	 * @return
	 */
	private boolean switchDevice(String id, String status) {
		try {
			int deviceId = Integer.parseInt(id);
			int deviceStatus = Integer.parseInt(status);

			String aid = "";
			for (Device device : RemoteController.mDevices) {
				if (device.getId() == deviceId) {
					aid = device.getAid();
					break;
				}
			}

			if (aid != "") {
				String sessionId = "0000000000000000";
				for (int i = 0; i < RemoteController.TEST_COUNT && sessionId.equals("0000000000000000"); i++) {
					sessionId = FritzBoxConnection.getSessionId(RemoteController.REMOTE_GATEWAY,
							RemoteController.FB_USERNAME, RemoteController.FB_PASSWORD);
				}

				if (deviceStatus == 0) {
					FritzBoxConnection.sendDeviceCommand(RemoteController.REMOTE_GATEWAY, aid,
							RemoteController.CMD_SWITCH_OFF, sessionId);
				} else if (deviceStatus == 1) {
					FritzBoxConnection.sendDeviceCommand(RemoteController.REMOTE_GATEWAY, aid,
							RemoteController.CMD_SWITCH_ON, sessionId);
				}

				FritzBoxConnection.logout(RemoteController.REMOTE_GATEWAY, sessionId);

				return true;
			} else {
				return false;
			}
		} catch (Exception e) {
			return false;
		}
	}

	/**
	 * This method will play a sound file (AMR) using an AT command.
	 * 
	 * AT code being handled:<br />
	 * AT+CREC=4,"C:\User\welcome.amr",0,100<br />
	 * OK<br />
	 * +CREC: 0
	 * 
	 * @param file_path
	 *            Path to the sound file
	 * @throws GSMShieldException
	 */
	private void playSound(String file_path) throws GSMShieldException {
		boolean result = false;
		for (int i = 0; i < TEST_COUNT && !result; i++) {
			result = sendATcommand("AT+CREC=4,\"" + file_path + "\",0,100", ANSWER_SOUND, 15000);
		}
	}

	private final int TEST_COUNT = 5;
	private final String COMM_PORT;
	private final String[] PHONE_NUMBERS;
	private final int TIMEOUT;
	private final int MODE;

	private final String ANSWER_OK = "OK";

	private final String COMMAND_POWER_ON = "AT";

	private final String COMMAND_NETWORK_ACTIVE = "AT+CREG?";
	private final String ANSWER_NETWORK_ACTIVE_1 = "+CREG: 0,1";
	private final String ANSWER_NETWORK_ACTIVE_2 = "+CREG: 0,5";

	private final String COMMAND_WAIT = "";
	private final String ANSWER_RING = "RING";
	private final String ANSWER_RING_NUMBER = "+CLIP:";
	private final String REGEX_RING_NUMBER = "\\+CLIP: (\"[0-9\\+]*\")";

	private final String COMMAND_CALL_HANG_UP = "ATH";
	private final String COMMAND_ANSWER_CALL = "ATA";
	private final String COMMAND_VOICE_CALL = "ATD";

	private final String COMMAND_DTMF_ON = "AT+DDET=1";
	private final String ANSWER_DTMF = "+DTMF:";
	private final String REGEX_DTMF = "\\+DTMF: ([0-9\\*\\+#])";

	private final String PIN_CODE;
	private final String REGEX_ACTION_CODE = "(\\d*\\*[01]#)";
	private final String REGEX_SPLIT_ACTION_CODE = "([\\*#])";

	private final String ANSWER_SOUND = "+CREC: 0";

	private final String SOUND_WELCOME = "C:\\User\\welcome.amr";
	private final String SOUND_PIN = "C:\\User\\pin.amr";
	private final String SOUND_PIN_OK = "C:\\User\\pin_ok.amr";
	private final String SOUND_PIN_ERROR = "C:\\User\\pin_error.amr";
	private final String SOUND_ACTION = "C:\\User\\action.amr";
	private final String SOUND_ACTION_OK = "C:\\User\\action_ok.amr";
	private final String SOUND_ACTION_ERROR = "C:\\User\\action_error.amr";
	private final String SOUND_ACTION_NEXT = "C:\\User\\action_next.amr";
	private final String SOUND_GOODBYE = "C:\\User\\goodbye.amr";

	private SerialPort mSerialPort;
	private PrintWriter mPrintWriter;
	private Logger mLogger;

	/**
	 * Exception thrown when errors occur in the {@link ATCallHandler}.
	 * 
	 * @author Fabian Mangels
	 *
	 */
	class GSMShieldException extends Exception {

		public GSMShieldException(String msg) {
			super(msg);
		}

		private static final long serialVersionUID = 1L;
	}
}