package de.hsb.smarthome.server;

import java.io.IOException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;

import de.hsb.smarthome.server.Scheduler.CycleListener;
import de.hsb.smarthome.util.image.IPCamera;
import de.hsb.smarthome.util.info.SystemInfo;
import de.hsb.smarthome.util.json.Control;
import de.hsb.smarthome.util.json.Control.Memory;
import de.hsb.smarthome.util.json.Control.Processor;
import de.hsb.smarthome.util.json.Device;
import de.hsb.smarthome.util.json.Image;
import de.hsb.smarthome.util.json.Transmission;
import de.hsb.smarthome.util.json.Transmission.Action;
import de.hsb.smarthome.util.log.Logger;
import de.hsb.smarthome.util.log.Logger.LoggerMode;
import de.hsb.smarthome.util.transmission.TCPConnection;
import de.hsb.smarthome.util.transmission.TCPConnection.ConnectionState;
import de.hsb.smarthome.util.transmission.TCPConnection.IConnectionCallback;
import de.hsb.smarthome.util.transmission.TCPConnection.IDataOnTcpPortObserver;

/**
 * Provides a socket to the client program ({@link TCPConnection}). Command
 * interpreter to query the smart home components.
 * 
 * @author Fabian Mangels
 *
 */
public class RemoteSocket implements IConnectionCallback, IDataOnTcpPortObserver {

	/**
	 * @param port
	 *            Port
	 * @param timeout
	 *            Time in ms
	 */
	public RemoteSocket(CycleListener cycleListener, int port, int timeout) {
		mCycleListener = cycleListener;
		PORT = port;
		TIMEOUT = timeout;
	}

	/**
	 * Starts a new server instance of TCPConnection. Waits until the socket
	 * connection is over.
	 */
	public void start() {
		mTcpConnection = TCPConnection.getServerInstance(PORT, TIMEOUT, this);
		mWaiting = true;

		waiting();
		mTcpConnection.close();
	}

	/**
	 * Helper method. The main thread waits until it is awakened.
	 */
	private synchronized void waiting() {
		while (mWaiting) {
			try {
				wait();
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
		notifyAll();
	}

	/**
	 * This method is executed when a new status is reached in the TCPConnection.
	 * Important are the states "CONNECTED" and "CLOSED".
	 */
	@Override
	public void tcpConnectionCallback(ConnectionState state) {
		LOGGER.write(this, "tcpConnectionCallback " + state, LoggerMode.TRACE);

		switch (state) {
		case CONNECTED:
			mTcpConnection.register(this);
			break;
		case CLOSED:
		case TIMEOUT:
			mWaiting = false;
			waiting();
			break;
		default:
			break;
		}
	}

	/**
	 * The remote command logic is stored here and the respective answer is created.
	 */
	@Override
	public void receiveTransmission(Transmission transmissionIn) {
		Transmission transmissionOut = new Transmission();
		Control controlOut = new Control();
		controlOut.setInfo(mSystemInfo.getOsInfo());

		Processor cpu = controlOut.new Processor();
		cpu.setCore(mSystemInfo.getAvailableProcessors());
		cpu.setWork(mSystemInfo.getSystemCpuLoad());
		ArrayList<Processor> listProcessor = new ArrayList<Processor>();
		listProcessor.add(cpu);
		controlOut.setCpu(listProcessor);

		Memory mem = controlOut.new Memory();
		mem.setFree(mSystemInfo.getFreePhysicalMemorySize());
		mem.setUsed(mSystemInfo.getTotalPhysicalMemorySize() - mSystemInfo.getFreePhysicalMemorySize());
		controlOut.setMem(mem);

		transmissionOut.setControl(controlOut);

		switch (transmissionIn.getAction()) {

		case REPORT:
			try {
				transmissionOut.setAction(Action.REPORT);
				transmissionOut.setMessage("Report is submitted!");

				if (transmissionIn.getDevices() != null) {
					Device deviceIn = transmissionIn.getDevices().get(0);
					List<Device> tmpDevices = RemoteController.getDevices();

					Device refDevice = null;
					for (Device device : tmpDevices) {
						if (device.getId() == deviceIn.getId()) {
							refDevice = device;
							break;
						}
					}

					for (Device device : RemoteController.mDevices) {
						if (device.getId() == deviceIn.getId()) {
							device.setConnected(refDevice.isConnected());
							device.setEnergy(refDevice.getEnergy());
							device.setPower(refDevice.getPower());
							device.setStatus(refDevice.getStatus());
							device.setTemperature(refDevice.getTemperature());

							List<Device> deviceOut = new ArrayList<Device>();
							deviceOut.add(device);
							transmissionOut.setDevices(deviceOut);
							break;
						}
					}

				} else {
					transmissionOut.setDevices(RemoteController.mDevices);
				}

			} catch (Exception e) {
				transmissionOut.setAction(Action.ERROR);
				transmissionOut.setMessage("Report is not submitted!");
			}

			break;

		case COMMIT:
			Device deviceIn = transmissionIn.getDevices().get(0);

			switch (deviceIn.getType()) {

			case SOCKET:
				try {
					String aid = "";

					for (Device device : RemoteController.mDevices) {
						if (device.getId() == deviceIn.getId()) {
							aid = device.getAid();
							break;
						}
					}

					String sessionId = "0000000000000000";
					for (int i = 0; i < RemoteController.TEST_COUNT && sessionId.equals("0000000000000000"); i++) {
						sessionId = FritzBoxConnection.getSessionId(RemoteController.REMOTE_GATEWAY,
								RemoteController.FB_USERNAME, RemoteController.FB_PASSWORD);
					}

					if (deviceIn.getStatus() == 0) {
						FritzBoxConnection.sendDeviceCommand(RemoteController.REMOTE_GATEWAY, aid,
								RemoteController.CMD_SWITCH_OFF, sessionId);
					} else if (deviceIn.getStatus() == 1) {
						FritzBoxConnection.sendDeviceCommand(RemoteController.REMOTE_GATEWAY, aid,
								RemoteController.CMD_SWITCH_ON, sessionId);
					}

					FritzBoxConnection.logout(RemoteController.REMOTE_GATEWAY, sessionId);

					if (deviceIn.getCycles() != null) {
						for (Device device : RemoteController.mDevices) {
							if (device.getId() == deviceIn.getId()) {
								device.setCycles(deviceIn.getCycles());

								// first remove all cycles
								RemoteController.SCHEDULER.deregisterDeviceCycles(mCycleListener, device);
								// then add all cycles
								RemoteController.SCHEDULER.registerDeviceCycles(mCycleListener, device);
								break;
							}
						}
					} else {
						for (Device device : RemoteController.mDevices) {
							if (device.getId() == deviceIn.getId()) {
								RemoteController.SCHEDULER.deregisterDeviceCycles(mCycleListener, device);
							}
						}
					}

					transmissionOut.setAction(Action.SUCCESS);
					transmissionOut.setMessage("Process was successful!");

				} catch (Exception e) {
					transmissionOut.setAction(Action.ERROR);
					transmissionOut.setMessage("Process was not successful!");
					e.printStackTrace();
				}

				break;

			case CAMERA:
				transmissionOut.setDevices(transmissionIn.getDevices());

				try {
					String imageInBase64 = IPCamera.encodeImage();

					transmissionOut.setAction(Action.SUCCESS);
					transmissionOut.setMessage("Process was successful!");

					Image imageOut = new Image();
					imageOut.setData(imageInBase64);
					imageOut.setFormat(Image.Format.JPG);
					imageOut.setTimestamp(new Timestamp(System.currentTimeMillis()));
					imageOut.setTitle("Image");
					imageOut.setComment("Picture is Base64 encoded.");
					ArrayList<Image> listImage = new ArrayList<Image>();
					listImage.add(imageOut);

					transmissionOut.setImages(listImage);

				} catch (Exception e) {
					transmissionOut.setAction(Action.ERROR);
					transmissionOut.setMessage("Process was not successful!");
				}

				break;

			default:
				transmissionOut.setAction(Action.ERROR);
				transmissionOut.setMessage("No device found!");
				break;
			}

			break;

		case CLOSE:
			mTcpConnection.close();
			break;

		default:
			transmissionOut.setAction(Action.ERROR);
			transmissionOut.setMessage("Command was not recognized!");
			break;
		}

		try {
			mTcpConnection.sendTransmission(transmissionOut);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	private final int PORT;
	private final int TIMEOUT;
	private final Logger LOGGER = Logger.getLogger();

	private TCPConnection mTcpConnection;
	private boolean mWaiting;
	private SystemInfo mSystemInfo = new SystemInfo();
	private CycleListener mCycleListener;
}
