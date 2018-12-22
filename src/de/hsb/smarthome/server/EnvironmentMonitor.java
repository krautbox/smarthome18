package de.hsb.smarthome.server;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.net.NoRouteToHostException;
import java.net.Socket;

import com.google.gson.GsonBuilder;

import de.hsb.smarthome.util.json.env.TempHumSensor;
import de.hsb.smarthome.util.log.Logger;
import de.hsb.smarthome.util.log.Logger.LoggerMode;

/**
 * Allows the monitoring of environment variables in the physical world.
 * Simply acquire an instance of this class by calling <code>getInstance()</code>
 * and query the desired variable by calling the appropriate method, for example
 * <code>getTemperature()</code> will get you the current room temperature.
 * However, make sure your EnvironmentMonitor is enabled by calling <code>enable()</code>
 * first. Please note, that a suitable device capable of performing the necessary 
 * measurements must be available within the local network and that device must have
 * registered its service to the service register. This class merely provides an easy
 * to use API to query things like room temperature etc. and to frequently update 
 * those values in the background.
 *
 */
public class EnvironmentMonitor {

	private EnvironmentMonitor() {
		this.log = Logger.getLogger();
		//private no-arg constructor
		//init is done through enable()
	}

	/**
	 * Gets an EnvironmentMonitor instance
	 * 
	 * @return An EnvironmentMonitor instance to work with
	 */
	public static EnvironmentMonitor getInstance() {
		if(em == null) {
			em = new EnvironmentMonitor();
		}
		return em;
	}

	/**
	 * Enables the EnvironmentMonitor to continuously update its variables in
	 * the background within a certain time interval. Subsequent calls to this method
	 * have no effect
	 */
	public void enable() {
		if(dsr == null) {
			dsr = DeviceServiceRegister.getInstance();
			if(!dsr.isEnabled()) {
				dsr.enable();
			}
		}
		if(updater == null) {
			updater = new Updater();
			new Thread(updater).start();
		}
	}

	/**
	 * Disables the EnvironmentMonitor from updating its variables. It should only
	 * be disabled if its services are no longer needed globally, because any 
	 * subsequent calls to the get*value*() methods will return null right after 
	 * <code>disable()</code> returns
	 */
	public void disable() {
		if(updater != null) {
			updater.terminate = true;
			updater = null;
			currentTemp = null;
			currentHum = null;
		}
	}

	/**
	 * Indicates whether the EvironmentMonitor is currently enabled
	 * 
	 * @return True if the EnvironmentMonitor is enabled and can update its
	 * 		   variables. Returns false if it's currently disabled
	 */
	public boolean isEnabled() {
		return (updater != null);
	}

	/**
	 * Indicates whether the EnvironmentMonitor has the capability to monitor
	 * changes in temperature
	 * 
	 * @return True if temperature can be monitored, false otherwise
	 */
	public boolean canMonitorTemperature() {
		return (currentTemp != null);
	}

	/**
	 * Indicates whether the EnvironmentMonitor has the capability to monitor
	 * changes in air humidity
	 * 
	 * @return True if air humidity can be monitored, false otherwise
	 */
	public boolean canMonitorHumidity() {
		return (currentHum != null);
	}

	/**
	 * Gets the current temperature provided by this EnvironmentMonitor. Please note 
	 * that this value might be slightly obsolete due to the update frequency. This 
	 * method simply returns the temperature since the last update
	 * 
	 * @return The current monitored temperature, or null if no temperature value can
	 * 		   be provided
	 */
	public Float getTemperature() {
		return this.currentTemp;
	}

	/**
	 * Gets the current relative air humidity provided by this EnvironmentMonitor. 
	 * Please note that this value might be slightly obsolete due to the update frequency.
	 * This method simply returns the air humidity since the last update
	 * 
	 * @return The current monitored air humidity, or null if no humidity value can
	 * 		   be provided
	 */
	public Byte getHumidity() {
		return this.currentHum;
	}

	//frequency to update measurements, in seconds
	private static final long UPDATE_FREQUENCY = 30;

	private static EnvironmentMonitor em;
	private DeviceServiceRegister dsr;
	private Updater updater;

	private volatile Float currentTemp;
	private volatile Byte currentHum;
	
	private Logger log;

	/**
	 * Implements all background work necessary to update monitored variables
	 *
	 */
	private class Updater implements Runnable{

		Socket socket;
		private boolean terminate;

		@Override
		public void run() {
			while(!terminate) {
				try {
					Thread.sleep(UPDATE_FREQUENCY*1000);
					if(dsr != null) {
						if(dsr.hasService(DeviceServiceRegister.SERVICE_GAUGE_TEMPERATURE)) {
							final String address = dsr.getServiceIP(DeviceServiceRegister.SERVICE_GAUGE_TEMPERATURE);
							socket = new Socket(InetAddress.getByName(address), 80);
							PrintWriter pw = new PrintWriter(socket.getOutputStream());
							pw.print("GET / HTTP/1.1");
							pw.flush();
							BufferedReader	br = new BufferedReader(new InputStreamReader(socket.getInputStream()));
							StringBuilder sb = new StringBuilder();
							String line;
							while ((line = br.readLine()) != null) {
								sb.append(line);
							}
							final TempHumSensor sensor = new GsonBuilder()
									.create()
									.fromJson(sb.toString(), TempHumSensor.class);
							
							if(sensor != null) {
								currentTemp = sensor.getTemperature();
								currentHum = sensor.getHumidity();
								log.write(this, "Sensor values updated", LoggerMode.TRACE);
								log.write(this, "Temp="+currentTemp, LoggerMode.TRACE);
								log.write(this, "Hum="+currentHum, LoggerMode.TRACE);
							}
						}
					}
				}catch(InterruptedException ex) {
					terminate = true;
				}catch(NoRouteToHostException ex){//Connection lost
					//invalidate variables to avoid returning obsolete values
					currentTemp = null;
					currentHum = null;
					log.write(this, String.format("Connection to service device lost. Trying again in %s seconds",
							UPDATE_FREQUENCY), LoggerMode.TRACE);
					
				}catch(Exception ex){
					//ex.printStackTrace();
				}finally {
					try {
						if(socket != null){
//							log.write(this, "Closing socket", LoggerMode.TRACE);//TODO: check removal
							socket.close();
						}
					} catch (IOException ex) {
						log.write(this, ex.getMessage(), LoggerMode.TRACE);
					}
				}
			}
		}
	}

}
