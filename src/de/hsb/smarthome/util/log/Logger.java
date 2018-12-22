package de.hsb.smarthome.util.log;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * Herewith it can be written in a central log.
 * 
 * @author Fabian Mangels
 *
 */
public class Logger {

	/**
	 * Static method to get a logger instance. There is only one central instance.
	 * 
	 * @return Central logger instance
	 */
	public static Logger getLogger() {
		if (LOGGER == null) {
			LOGGER = new Logger();

			FileWriter fileWriter = null;
			try {
				fileWriter = new FileWriter(FILENAME, true);
			} catch (IOException e) {
				e.printStackTrace();
			}
			BufferedWriter bufferedWriter = new BufferedWriter(fileWriter);

			LOGGER.mBufferedWriter = bufferedWriter;
		}
		return LOGGER;
	}

	/**
	 * Method to write in the central log.
	 * 
	 * @see write(Object obj, String msg, LoggerMode logMode)
	 * @param str
	 *            Message
	 * @throws IOException
	 */
	@Deprecated
	public synchronized void write(String msg) {
		SimpleDateFormat simpleDateFormat = new SimpleDateFormat("dd.MM.yyyy HH:mm:ss");
		Date date = new Date();

		try {
			if (OUTPUT == OUTPUT_CONSOLE || OUTPUT == (OUTPUT_CONSOLE + OUTPUT_FILE)) {
				System.out.println(simpleDateFormat.format(date) + " #> " + msg);
			}
			if (OUTPUT == OUTPUT_FILE || OUTPUT == (OUTPUT_CONSOLE + OUTPUT_FILE)) {
				mBufferedWriter.write(simpleDateFormat.format(date) + " #> " + msg);
				mBufferedWriter.newLine();

				mBufferedWriter.flush();
			}
		} catch (IOException e) {
			e.printStackTrace();
			try {
				mBufferedWriter.close();
			} catch (Exception e1) {
			}
		}
	}

	/**
	 * Method to write in the central log.
	 * 
	 * @param obj
	 *            Object from which the log is written
	 * @param msg
	 *            Message
	 * @param logMode
	 */
	public synchronized void write(Object obj, String msg, LoggerMode logMode) {
		SimpleDateFormat simpleDateFormat = new SimpleDateFormat("dd.MM.yyyy HH:mm:ss");
		Date date = new Date();

		try {
			if (OUTPUT == OUTPUT_CONSOLE || OUTPUT == (OUTPUT_CONSOLE + OUTPUT_FILE)) {
				System.out.println(simpleDateFormat.format(date) + " #> " + obj.getClass().getName() + " -- " + logMode
						+ " --> " + msg);
			}
			if (OUTPUT == OUTPUT_FILE || OUTPUT == (OUTPUT_CONSOLE + OUTPUT_FILE)) {
				mBufferedWriter.write(simpleDateFormat.format(date) + " #> " + obj.getClass().getName() + " -- "
						+ logMode + " --> " + msg);
				mBufferedWriter.newLine();

				mBufferedWriter.flush();
			}
		} catch (IOException e) {
			e.printStackTrace();
			try {
				mBufferedWriter.close();
			} catch (Exception e1) {
			}
		}
	}

	public enum LoggerMode {
		TRACE("TRACE"), INFO("INFO"), WARN("WARN"), ERROR("ERROR");

		private final String label;

		private LoggerMode(String label) {
			this.label = label;
		}

		public String toString() {
			return this.label;
		}
	}
	
	public String getLogFilePath() {
		return FILENAME;
	}

	public static final int OUTPUT_CONSOLE = 1;
	public static final int OUTPUT_FILE = 2;
	private static final String FILENAME = "safer_smart_home.log";

	private static Logger LOGGER = null;
	private static int OUTPUT = 3;

	private BufferedWriter mBufferedWriter;
}