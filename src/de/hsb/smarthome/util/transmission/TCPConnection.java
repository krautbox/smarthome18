package de.hsb.smarthome.util.transmission;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Vector;

import de.hsb.smarthome.util.json.Transmission;
import de.hsb.smarthome.util.json.Transmission.Action;
import de.hsb.smarthome.util.log.Logger;
import de.hsb.smarthome.util.log.Logger.LoggerMode;

/**
 * This class gives the ability to build up a socket connection and to send and
 * receive string data between {Socket}s.
 * 
 * @author Jules
 * 
 */
public class TCPConnection extends Thread {

	/**
	 * Have to implemented by classes who wants to create a TCPConnection.
	 */
	public interface IConnectionCallback {

		/**
		 * These function get the callback from the TCP-Connection in every case. It is necessary that the 
		 * Object which implements these method is registered at a TCP-Connection.
		 * 
		 * @param state
		 *            - possible states ConnectionState.UNCONNECTED
		 *            ConnectionState.CONNECTED ConnectionState.ERROR
		 *            ConnectionState.CLOSED ConnectionState.TIMEOUT
		 *            ConnectionState.THREAD_END
		 */
		void tcpConnectionCallback(ConnectionState state);
	}

	/**
	 * Must be implemented by classes who are interested to get the inputs. The
	 * class which implements this interface have to register to this class to get
	 * the event if a message is on the input stream.
	 */
	public interface IDataOnTcpPortObserver {

		/**
		 * This method will be called everytime the TCP-Connection received valid data. It's necessary that the
		 * Object which implements these Method is registere at a TCP-Connection. 
		 * 
		 * @param obj
		 */
		void receiveTransmission(Transmission obj);
	}

	/**
	 * Constructor for a client socket. Creates a new thread for input and a new
	 * thread for output. Both will start implicit. Use the Interface
	 * "IConnectionCallback" to get information if a connection is created.
	 * (Interface is in this class) Use the Interface "IDataOnTcpPortObserver" and
	 * register to be a observer from the input stream. (Interface is in this class)
	 * 
	 * @param ipAdress
	 *            - The IP-address of an other socket.
	 * @param portNumber
	 *            - The TCP port for the communication
	 * @param timeout
	 *            (ms) - If no connection is possible to the given IP-address the
	 *            process will end after this timeout. It's possible, that this
	 *            timeout not work correctly. This happens in case of configuration
	 *            problems with the operating system (e.g. Windows).
	 * @param delegate
	 *            - The object which get the information when the connection is
	 *            ready
	 */
	private TCPConnection(String ipAdress, int portNumber, int timeout, final IConnectionCallback delegate) {
		mIpAdress = ipAdress;
		mPortNumber = portNumber;
		mTimeout = timeout;
		mDelegate = delegate;
		mMode = CLIENT_MODE;
		mConnectionState = ConnectionState.UNCONNECTED;
	}

	/**
	 * Constructor for the a server socket. Creates a new thread for input and a new
	 * thread for output. Both will start implicit. Use the Interface
	 * "IConnectionCallback" to get information if a connection is created.
	 * (Interface is in this class) Use the Interface "IDataOnTcpPortObserver" and
	 * register to be a observer from the input stream. (Interface is in this class)
	 * 
	 * @param portNumber
	 *            - The TCP port for the communication
	 * @param timeout
	 *            (ms) - If nobody connects to the given port or no message is send
	 *            till the timeout the port will be closed
	 * @param delegate
	 *            - The object which get the information when the connection is
	 *            ready
	 */
	private TCPConnection(int portNumber, int timeout, final IConnectionCallback delegate) {
		mPortNumber = portNumber;
		mTimeout = timeout;
		mDelegate = delegate;
		mMode = SERVER_MODE;
		mConnectionState = ConnectionState.UNCONNECTED;
	}

	/**
	 * Constructor for a client socket. Creates a new thread for input and a new
	 * thread for output. Both will start implicit. Use the Interface
	 * "IConnectionCallback" to get information if a connection is created.
	 * (Interface is in this class) Use the Interface "IDataOnTcpPortObserver" and
	 * register to be a observer from the input stream. (Interface is in this class)
	 * 
	 * @param ipAdress
	 *            - The IP-address of an other socket.
	 * @param portNumber
	 *            - The TCP port for the communication
	 * @param timeout
	 *            (ms) - If no connection is possible to the given IP-address the
	 *            process will end after this timeout. It's possible, that this
	 *            timeout not work correctly. This happens in case of configuration
	 *            problems with the operating system (e.g. Windows).
	 * @param delegate
	 *            - The object which get the information when the connection is
	 *            ready
	 */
	public static TCPConnection getClientInstance(String ipAdress, int portNumber, int timeout,
			final IConnectionCallback delegate) {
		mInstance = new TCPConnection(ipAdress, portNumber, timeout, delegate);
		mInstance.start();
		return mInstance;
	}

	/**
	 * Constructor for the a server socket. Creates a new thread for input and a new
	 * thread for output. Both will start implicit. Use the Interface
	 * "IConnectionCallback" to get information if a connection is created.
	 * (Interface is in this class) Use the Interface "IDataOnTcpPortObserver" and
	 * register to be a observer from the input stream. (Interface is in this class)
	 * 
	 * @param portNumber
	 *            - The TCP port for the communication
	 * @param timeout
	 *            (ms) - If nobody connects to the given port or no message is send
	 *            till the timeout the port will be closed
	 * @param delegate
	 *            - The object which get the information when the connection is
	 *            ready
	 */
	public static TCPConnection getServerInstance(int portNumber, int timeout, final IConnectionCallback delegate) {
		mInstance = new TCPConnection(portNumber, timeout, delegate);
		mInstance.start();
		return mInstance;
	}

	/**
	 * During creation of an Instance of this object you will take a choice if you
	 * want to get a server or a client socket.
	 * 
	 * @author Jules-Marc Siemssen & Lars Kiegeland
	 * 
	 */
	@Override
	public void run() {
		mRunning = true;
		if (this.mMode == CLIENT_MODE) {
			mLogger.write(this, "client mode", LoggerMode.TRACE);
			createClientSocket();
		} else {
			mLogger.write(this, "server mode", LoggerMode.TRACE);
			createServerSocket();
		}
	}

	/**
	 * Creates a socket for the client connection.
	 * 
	 */
	private void createClientSocket() {
		mSocket = null;
		long time = System.currentTimeMillis();

		while (mConnectionState != ConnectionState.CONNECTED && mRunning) {
			try {
				mLogger.write(this, "Try to connect", LoggerMode.TRACE);

				mSocket = new Socket(mIpAdress, mPortNumber);

				BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(mSocket.getInputStream()));
				mTcpInput = new TCPInput(bufferedReader);
				mTcpInput.start();

				mConnectionState = ConnectionState.CONNECTED;
				tcpConnectionCallback(mConnectionState);

				mLogger.write(this, "Client connected to " + mIpAdress + " on Port " + mPortNumber, LoggerMode.INFO);

			} catch (IOException e) {
				if (mConnectionState != ConnectionState.CLOSED) {
					mSocket = null;
					mLogger.write(this, "while trying to connect " + e.getMessage(), LoggerMode.TRACE);
					if (mTimeout < (System.currentTimeMillis() - time)) {
						mLogger.write(this, "Error - client timeout.", LoggerMode.ERROR);
						mConnectionState = ConnectionState.TIMEOUT;
						tcpConnectionCallback(mConnectionState);
						break;
					}

					try {
						Thread.sleep(100);
					} catch (InterruptedException e1) {
						mLogger.write(this, "while trying to connect " + e1.getMessage(), LoggerMode.TRACE);
					}
				}
			}
		}
	}

	/**
	 * creates the server socket and waits for the connection
	 * 
	 * @return TCPConncetion.CONNECTED or TCPConncetion.TIMEOUT
	 */
	private void createServerSocket() {
		mSocket = null;
		mServerSocket = null;

		if (mConnectionState != ConnectionState.CONNECTED && mRunning) {
			try {
				mServerSocket = new ServerSocket(mPortNumber);
				mServerSocket.setSoTimeout(mTimeout);
				mSocket = mServerSocket.accept();
				mSocket.setSoTimeout(mTimeout);

				BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(mSocket.getInputStream()));
				mTcpInput = new TCPInput(bufferedReader);
				mTcpInput.start();

				mConnectionState = ConnectionState.CONNECTED;
				tcpConnectionCallback(mConnectionState);

				mLogger.write(this, "Connection to server was created.", LoggerMode.INFO);

			} catch (IOException e) {
				if (mConnectionState != ConnectionState.CLOSED) {
					mLogger.write(this, "while waiting for connection " + e.getMessage(), LoggerMode.TRACE);
					mLogger.write(this, "Error - server timeout.", LoggerMode.ERROR);
					mConnectionState = ConnectionState.TIMEOUT;
					tcpConnectionCallback(mConnectionState);
				}
			}
		}
	}

	/**
	 * Sends the given Transmission object to the initialized TCP connection if the
	 * connection will be ready.
	 * 
	 * @param obj
	 * @return true if the TCP connection was ready and the transmission could
	 *         start.
	 * @throws IOException
	 *             - If something goes wrong during the transmission
	 */
	public synchronized boolean sendTransmission(Transmission obj) throws IOException {
		if (mConnectionState == ConnectionState.CONNECTED) {
			String str = Transmission.serializeToJson(obj);
			mLogger.write(this, "Output: " + str, LoggerMode.INFO);
			str = str + "\n";
			mSocket.getOutputStream().write(str.getBytes());
			return true;
		}
		return false;
	}

	/**
	 * With this function you can register a observer to the input stream in case
	 * that the TCP connection is ready. You know that the TCP connection is ready
	 * when you implement the IConnectionCallback interface. When creating this
	 * class (TCPConnection) you have to put in a class which implements this
	 * interface. Then you will get a callback.
	 * 
	 * @param observer
	 * @return Returns true if the registration of the observer is correct otherwise
	 *         false.
	 */
	public synchronized boolean register(IDataOnTcpPortObserver observer) {
		if (mConnectionState == ConnectionState.CONNECTED) {
			mTcpInput.register(observer);
			return true;
		}
		return false;
	}

	/**
	 * With this function you can unregister a observer to the input stream in case
	 * that the TCP connection is ready. You know that the TCP connection is ready
	 * when you implement the IConnectionCallback interface. When creating this
	 * class (TCPConnection) you have to put in a class which implements this
	 * interface. Then you will get a callback.
	 * 
	 * @param observer
	 * @return Returns true if the unregistration is of the observer is correct
	 *         otherwise false.
	 */
	public synchronized boolean unregister(IDataOnTcpPortObserver observer) {
		if (mConnectionState == ConnectionState.CONNECTED) {
			mTcpInput.unregister(observer);
			return true;
		}
		return false;
	}

	/**
	 * Close the current TCP connection. Server or client.
	 */
	public synchronized void close() {
		mRunning = false;

		if (mTcpInput != null) {
			mTcpInput.close();
		}

		if (mSocket != null) {
			try {
				mSocket.close();
			} catch (IOException e) {
				mLogger.write(this, "While closing the socket - " + e.getMessage(), LoggerMode.TRACE);
			}
		}

		if (mServerSocket != null) {
			try {
				mServerSocket.close();
			} catch (IOException e) {
				mLogger.write(this, "While closing the server socket - " + e.getMessage(), LoggerMode.TRACE);
			}
		}

		if (mConnectionState != ConnectionState.CLOSED) {
			mConnectionState = ConnectionState.CLOSED;
			tcpConnectionCallback(mConnectionState);
		}

		mLogger.write(this, "Streams and sockets closed.", LoggerMode.TRACE);
	}

	private void tcpConnectionCallback(ConnectionState state) {
		if (mDelegate != null) {
			mDelegate.tcpConnectionCallback(state);
		}
	}

	public enum ConnectionState {
		UNCONNECTED, ERROR, CONNECTED, TIMEOUT, CLOSED
	}

	private static int CLIENT_MODE = 0;
	private static int SERVER_MODE = 1;

	private static TCPConnection mInstance = null;
	private int mMode;
	private String mIpAdress;
	private int mPortNumber;
	private Socket mSocket;
	private ServerSocket mServerSocket = null;
	private IConnectionCallback mDelegate = null;
	private TCPInput mTcpInput;
	private ConnectionState mConnectionState = ConnectionState.UNCONNECTED;
	private final int mTimeout;
	private Logger mLogger = Logger.getLogger();
	private volatile boolean mRunning = false;

	class TCPInput extends Thread {

		private TCPInput(BufferedReader bufferedReader) {
			mBufferedReader = bufferedReader;
			mInputObservers = new Vector<IDataOnTcpPortObserver>();
		}

		/**
		 * Reads permanently the input stream and calls the observers if data were
		 * received.
		 */
		@Override
		public void run() {
			mRunning = true;
			String str = null;
			try {
				while (mRunning && (str = mBufferedReader.readLine()) != null) {
					mLogger.write(this, "Input: " + str, LoggerMode.INFO);

					if (!mInputObservers.isEmpty()) {
						mLogger.write(this, "Counted observers: " + mInputObservers.size(), LoggerMode.TRACE);
						for (int i = 0; i < mInputObservers.size(); ++i) {
							try {
								Transmission transmission = Transmission.deserializeJson(str);
								mInputObservers.get(i).receiveTransmission(transmission);
							} catch (Exception e) {
								mLogger.write(this, "Not a deserializeable json String.", LoggerMode.ERROR);
								Transmission transmissionError = new Transmission();
								transmissionError.setAction(Action.ERROR);
								mInputObservers.get(i).receiveTransmission(transmissionError);
							}
						}
					}
				}
			} catch (IOException e) {
				mConnectionState = ConnectionState.ERROR;
				tcpConnectionCallback(mConnectionState);

				mLogger.write(this, e.getMessage(), LoggerMode.TRACE);
				mLogger.write(this, "Error while reading the input stream.", LoggerMode.ERROR);
			} finally {
				try {
					mBufferedReader.close();
					if (mConnectionState != ConnectionState.CLOSED) {
						mConnectionState = ConnectionState.CLOSED;
						tcpConnectionCallback(mConnectionState);
					}
				} catch (Exception e) {
					mLogger.write(this, e.getMessage(), LoggerMode.TRACE);
					mLogger.write(this, "Error at closing the input reader.", LoggerMode.ERROR);
				}
			}
		}

		public synchronized void register(IDataOnTcpPortObserver observer) {
			mInputObservers.addElement(observer);
		}

		public synchronized void unregister(IDataOnTcpPortObserver observer) {
			mInputObservers.remove(observer);
		}

		public synchronized void close() {
			mRunning = false;
		}

		private BufferedReader mBufferedReader;
		private Vector<IDataOnTcpPortObserver> mInputObservers;
		private volatile boolean mRunning = false;
	}
}