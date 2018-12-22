package de.hsb.smarthome.server;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import de.hsb.smarthome.util.json.Device;
import de.hsb.smarthome.util.json.Device.Cycle;
import de.hsb.smarthome.util.json.Weekday;
import de.hsb.smarthome.util.log.Logger;
import de.hsb.smarthome.util.log.Logger.LoggerMode;

/**
 * Takes care of scheduling and keeping track of recurring tasks.
 * A caller can register itself for a listener interface of this class
 * to get notified about any events that may occur in the future. 
 * You cannot instantiate this class directly, use <code>Scheduler.getInstance()</code>
 * instead. This class is entirely thread-safe and supports multiple callers.
 *
 */
public class Scheduler {
	
	/**
	 * Listener interface users of the Scheduler class may implement in order
	 * to receive cycle events. Once a device has been registered you will receive
	 * the appropriate cycle event with the Device instance that triggered the event
	 * passed in as a parameter.
	 *
	 */
	public interface CycleListener {
		/**
		 * Will be called when a registered device has moved into the active cycle state.
		 * The device itself gets passed as a parameter. Please note that the class registered
		 * for this event merely gets notified about a cycle state transition. It is that class'
		 * responsibility to perform any meaningful actions on the referring device, like changing
		 * its status etc. 
		 * All changes to the Device instance are backed by the listeners behaviour, so if any 
		 * devices' cycles change, the listener will notice
		 * 
		 * @param device the Device instance which triggered the event
		 */
		void onCycleStart(Device device);
		/**
		 * Will be called when a registered device has moved from active- to the inactive
		 * cycle state. The device itself gets passed as a parameter. Please note that 
		 * the class registered for this event merely gets notified about a cycle state transition.
		 * It is that class' responsibility to perform any meaningful actions on the referring device,
		 * like changing its status etc. 
		 * All changes to the Device instance are backed by the listeners behaviour, so if any 
		 * devices' cycles change, the listener will notice
		 * 
		 * @param device the Device instance which triggered the event
		 */
		void onCycleStop(Device device);
	}
	
	private Scheduler() {
		this.dlMap = new ConcurrentHashMap<Device, List<CycleListener>>(20);
		this.log = Logger.getLogger();
	}
	
	/**
	 * Returns a Scheduler instance that can be used to register listeners for smarthome devices
	 * 
	 * @return A Scheduler instance to work with
	 */
	public synchronized static Scheduler getInstance() {
		if(scheduler != null) {
			return scheduler;
		}
		scheduler = new Scheduler();
		return scheduler;
	}
	
	/**
	 * Registers a smarthome device to receive callbacks for cycle events. The devices cycles which
	 * should be monitored must be passed as an argument. Changes to that devices state are reflected
	 * by the listeners behaviour. Make sure you call either <code>deregisterDeviceCycles()</code>
	 * or <code>deregisterListener()</code> when you no longer want to receive cycle events in order
	 * to free up resources
	 * 
	 * @param listener the Scheduler.CycleListener instance to receive any cycle events
	 * @param device the Device instance which should be monitored
	 */
	public synchronized void registerDeviceCycles(final CycleListener listener, final Device device) {
		if(dlMap.containsKey(device)) {
			final List<CycleListener> ls = dlMap.get(device);
			ls.add(listener);
			this.dlMap.put(device, ls);
		}else {
			final List<CycleListener> ls = new LinkedList<CycleListener>();
			ls.add(listener);
			this.dlMap.put(device, ls);
		}
		if(observer == null) {
			observer = new CycleObserver();
			new Thread(observer).start();
		}
	}
	
	/**
	 * Deregisters a given smarthome device from being monitored. Any class which is registered as
	 * such a listener and wants to no longer receive cycle events for a particular device should
	 * call this method. Alternatively, if a class implementing the CycleListener interface no longer
	 * wants to receive any cycle events, you can call <code>deregisterListener()</code>
	 * 
	 * @param listener the Scheduler.CycleListener instance to deregister the device
	 * @param device the Device instance to deregister
	 */
	public synchronized void deregisterDeviceCycles(final CycleListener listener, final Device device) {
		final List<CycleListener> ls = this.dlMap.get(device);
		if(ls != null) {
			if(ls.size() == 1) {
				this.dlMap.remove(device);
			}else {
				ls.remove(listener);
				this.dlMap.put(device, ls);
			}
		}
		if(dlMap.isEmpty() && observer != null) {
			this.observer.terminate();
			this.observer = null;
		}
	}
	
	/**
	 * Deregisters a Scheduler.CycleListener instance from receiving any cycle events for any device it
	 * has registered in the past. If only one particular device should be deregistered, you should call
	 * <code>deregisterDeviceCycles()</code> instead. Please note that other Scheduler.CycleListener 
	 * instances registered for particular devices will still receive cycle events
	 * 
	 * @param listener the Scheduler.CycleListener instance to deregister
	 */
	public synchronized void deregisterListener(final CycleListener listener) {
		for(final List<CycleListener> ls : dlMap.values()) {
			ls.remove(listener);
		}
		for(final Device d : dlMap.keySet()) {//clean up internal map
			if(dlMap.get(d).isEmpty()) {
				dlMap.remove(d);
			}
		}
		if(dlMap.isEmpty() && observer != null) {
			this.observer.terminate();
			this.observer = null;
		}
	}
	
	//observer lookup frequency in seconds
	private static final long DELAY_SECONDS = 5;
	
	private static Scheduler scheduler;
	
	private Map<Device, List<CycleListener>> dlMap;
	private CycleObserver observer;
	
	private Logger log;
	
	/**
	 * Represents a thread which can be launched to observe any cycle state transitions
	 * of smarthome devices. Unless interrupted or gracefully terminated, the thread will
	 * keep executing indefinitely. It can be terminated by calling the <code>terminate()</code>
	 * method directily.
	 *
	 */
	private class CycleObserver implements Runnable {
		
		private boolean terminate;

		@Override
		public void run() {
			while(!terminate) {
				try {
					for(final Device d : dlMap.keySet()) {
						final byte change = determineCycleState(d);
						if(change == 1) {
							for(final CycleListener delegate : dlMap.get(d)) {
								delegate.onCycleStart(d);
							}
						}else if(change == -1) {
							for(final CycleListener delegate : dlMap.get(d)) {
								delegate.onCycleStop(d);
							}
						}
					}
					Thread.sleep(DELAY_SECONDS*1000);
				}catch(InterruptedException ex) {
					terminate = true;
				}
			}
		}
		
		/**
		 * Determines if a cycle state transition for a given device has occurred at the time 
		 * of the method invocation. A byte is returned indicating the result of that computation.
		 * More precisely, 1 is returned if a device has changed to an ACTIVE cycle state, -1 if
		 * it has changed to an INACTIVE cycle state. 0 (zero) is returned if the devices cycle has
		 * not changed its state
		 * 
		 * @param d the Device instance to check against cycle state transitions
		 * @return 1 (switched to active), -1 (switched to inactive), or 0 (unchanged)
		 */
		private byte determineCycleState(final Device d) {
			final List<Cycle> cycles = d.getCycles();
			for(final Cycle c: cycles) {
				final String now = new SimpleDateFormat("EEEE-HH:mm:ss", Locale.ENGLISH).format(new Date());
				final String time = now.substring(now.indexOf("-")+1);
				if(c.getCycletype().equals(Cycle.CYCLETYPE_TIME)) {
					if(c.getDays().contains(Weekday.valueOf(now.substring(0, now.indexOf("-")).toUpperCase()))) {
						final boolean isInInterval = ((time.compareTo(c.getStart()) >= 0) && (time.compareTo(c.getStop()) < 0));
						if(isInInterval && (d.getStatus() == 0)) {
							return 1;
						}
						if(!isInInterval && (d.getStatus() == 1)) {
							return -1;
						}
					}

				}else if(c.getCycletype().equals(Cycle.CYCLETYPE_TEMPERATURE)) {
					EnvironmentMonitor em = EnvironmentMonitor.getInstance();
					if(em.isEnabled() && em.canMonitorTemperature()) {
						final float temp = em.getTemperature();
						final float start = Float.valueOf(c.getStart());
						final float stop = Float.valueOf(c.getStop());
						if((temp >= start) && (temp < stop) && (d.getStatus() == 0)) {
							return 1;
						}
						if(((temp < start) || (temp > stop)) && (d.getStatus() == 1)) {
							return -1;
						}
					}else {
						log.write(this, String.format("Device %s has a temperature based cycle "
								+ "but temperature could not be determined", d.toString()), LoggerMode.TRACE);
					}
				}else if(c.getCycletype().equals(Cycle.CYCLETYPE_HUMIDITY)) {
					EnvironmentMonitor em = EnvironmentMonitor.getInstance();
					if(em.isEnabled() && em.canMonitorHumidity()) {
						final int hum = em.getHumidity();
						final int start = Integer.valueOf(c.getStart());
						final int stop = Integer.valueOf(c.getStop());
						if((hum >= start) && (hum <= stop) && (d.getStatus() == 0)) {
							return 1;
						}
						if(((hum < start) || (hum > stop)) && (d.getStatus() == 1)) {
							return -1;
						}
					}else {
						log.write(this, String.format("Device %s has a humidity based cycle "
								+ "but humidity could not be determined", d.toString()), LoggerMode.TRACE);
					}
				}
			}
			return 0;
		}
		
		/**
		 * Initiates the thread termination
		 */
		private void terminate() {
			terminate = true;
		}
	}

}
