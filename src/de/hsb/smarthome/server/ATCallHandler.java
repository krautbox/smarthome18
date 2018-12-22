package de.hsb.smarthome.server;

import java.io.PrintWriter;
import java.util.Arrays;

import com.fazecast.jSerialComm.SerialPort;

import de.hsb.smarthome.util.log.Logger;
import de.hsb.smarthome.util.log.Logger.LoggerMode;

/**
 * Manages phone calls and communicates with the GSM shield using the AT command
 * set. The external library "jSerialComm" is required.
 * 
 * @author Fabian Mangels
 *
 */
public class ATCallHandler {

	/**
	 * 
	 * @param commPort
	 *            Serial interface ("/dev/ttyS0")
	 * @param phoneNumbers
	 *            Phone numbers as string array ("+4915000000000", ...)
	 */
	public ATCallHandler(String commPort, String[] phoneNumbers) {
		COMM_PORT = commPort;
		PHONE_NUMBERS = phoneNumbers;
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
			mLogger.write(this, "Connected to the mobile network ... Waiting for a call", LoggerMode.INFO);
		} else {
			mLogger.write(this, "GSM module could not dial into the mobile network!", LoggerMode.ERROR);
			throw new GSMShieldException("GSM module could not dial into the mobile network!");
		}
	}

	/**
	 * Waits until a call is received, if successful, checks the phone number that
	 * was called. If the phone number is correct, the method is exited. If the
	 * telephone number is wrong, the method is called again.
	 * 
	 * AT code being handled:<br />
	 * RING<br />
	 * <br />
	 * +CLIP: "+4915000000000",145,"",0,"",0<br />
	 * OK
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
			boolean isRightNum = false;
			for (int i = 0; i < TEST_COUNT && !isRightNum; i++) {
				String res;
				if ((res = getATcommandAnswer(ANSWER_RING_NUMBER, REGEX_RING_NUMBER, 2000)) != "") {
					res = res.replaceAll("\"", "");
					mLogger.write(this, "Received number: " + res, LoggerMode.INFO);
					isRightNum = Arrays.stream(PHONE_NUMBERS).anyMatch(res::equals);
				}
			}

			// Hang-up the call
			boolean result = false;
			for (int i = 0; i < TEST_COUNT && !result; i++) {
				result = sendATcommand(COMMAND_CALL_HANG_UP, ANSWER_OK, 1000);
			}
			mLogger.write(this, "The call was hung-up? " + result, LoggerMode.INFO);

			if (isRightNum) {
				mLogger.write(this, "It was called with the right number!", LoggerMode.INFO);
				isWrongNum = false;
			} else {
				mLogger.write(this, "It was called with the wrong number!", LoggerMode.WARN);
			}
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

	private final int TEST_COUNT = 5;
	private final String COMM_PORT;
	private final String[] PHONE_NUMBERS;

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