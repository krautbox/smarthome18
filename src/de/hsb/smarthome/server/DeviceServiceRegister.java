package de.hsb.smarthome.server;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import com.google.gson.GsonBuilder;
import com.google.gson.JsonParseException;

import de.hsb.smarthome.util.json.env.ServiceBroadcast;
import de.hsb.smarthome.util.log.Logger;
import de.hsb.smarthome.util.log.Logger.LoggerMode;

/**
 * Class implementing a service register for smarthome devices. Using such a 
 * register will allow smarthome devices to register their services to the
 * smarthome control unit. The service type a device may provide is encoded as
 * a byte value. See SERVICE_*-constants for all currently supported services.
 *
 */
public class DeviceServiceRegister {

	/** Indicates a device's capability to measure temperature values **/
	public static final byte SERVICE_GAUGE_TEMPERATURE = 1;
	/** Indicates a device's capability to measure humidity values **/
	public static final byte SERVICE_GAUGE_HUMIDITY = 2;

	private DeviceServiceRegister() {
		this.registeredServices = new ConcurrentHashMap<Byte, String>();
		this.log = Logger.getLogger();
	}

	/**
	 * Returns a DeviceServiceRegister instance to control the service register and
	 * to querry available services offered by devices. You must call <code>enable()</code>
	 * before smarthome devices can register their service
	 * 
	 * @return A DeviceServiceRegister instance to work with
	 */
	public static DeviceServiceRegister getInstance() {
		if(dsr == null) {
			dsr = new DeviceServiceRegister();
		}
		return dsr;
	}

	/**
	 * Opens a port on which smarthome devices can send UDP packets to register their
	 * service. You can querry on which port to send packets to by calling 
	 * <code>getServiceRegisterPort()</code>. Making subsequent calls to this method
	 * has no effect
	 */
	public void enable() {
		if(so == null) {
			so = new ServiceObserver();
			new Thread(so).start();
		}
	}

	/**
	 * Disables this DeviceServiceRegister. Subsequent calls to this method have no effect
	 */
	public void disable() {
		if(so != null) {
			so.terminate = true;
			so = null;
		}
	}
	
	/**
	 * Indicates whether the service register is enabled and active, i.e. devices can send 
	 * packets to register their service
	 * 
	 * @return True if the background service is enabled and ready to receive packets,
	 *  	   false otherwise
	 */
	public boolean isEnabled() {
		return (so != null);
	}
	
	/**
	 * Returns the IP address on which any device has registerd its service
	 * 
	 * @param SERVICE the service code of the requested service. 
	 * 		  Must be one of the SERVICE_*-constants
	 * @return the IP address of the device providing the requested service, or null if no
	 * 		   device is currently registerd for that particular service
	 */
	public String getServiceIP(final byte SERVICE) {
		return registeredServices.get(SERVICE);
	}
	
	/**
	 * Manually registers a device's service. You should not call this method directly. A 
	 * device should perform this action automatically by sending the properly encoded
	 * packets to the DeviceServiceRegister's UDP port
	 * 
	 * @param SERVICE the service code of the service to register
	 * @param serviceIP the IP address of the device to register
	 */
	public void registerService(final byte SERVICE, final String serviceIP) {
		registeredServices.put(SERVICE, serviceIP);
	}
	
	/**
	 * Manually deregisters a device's service so subsequent calls to 
	 * <code>getServiceIP()</code> will return null for that particular service
	 * 
	 * @param SERVICE the service code of the service to deregister
	 */
	public void deregisterService(final byte SERVICE) {
		registeredServices.remove(SERVICE);
	}

	/**
	 * Indicates whether a particular service is currently being provided by some device
	 * 
	 * @param SERVICE the service code of the service to look up.
	 * 		  Must be one of the SERVICE_*-constants
	 * @return True if some device has registered itself and provides above 
	 * 		   mentioned service. Returns false if the service is currently not provided
	 */
	public boolean hasService(final byte SERVICE) {
		return (registeredServices.get(SERVICE) != null);
	}
	
	/**
	 * Gets the port on which this service register operates on
	 * 
	 * @return The port number on which this service register receives packets
	 */
	public int getServiceRegisterPort() {
		return ServiceObserver.SERVICE_REGISTER_PORT;
	}
	
	/**
	 * Adds all services provided by a particular device to the known services
	 * 
	 * @param sb the ServiceBroadcast object of the device which made the request
	 */
	private void addServices(final ServiceBroadcast sb) {
		for(final byte service : sb.getServices()) {
			switch(service) {
			case SERVICE_GAUGE_TEMPERATURE:
				registeredServices.put(SERVICE_GAUGE_TEMPERATURE, sb.getLocalIP());
				log.write(this, "Added temperature service from " + sb.getLocalIP(), LoggerMode.INFO);
				break;
			case SERVICE_GAUGE_HUMIDITY:
				registeredServices.put(SERVICE_GAUGE_HUMIDITY, sb.getLocalIP());
				log.write(this, "Added humidity service from " + sb.getLocalIP(), LoggerMode.INFO);
				break;
			default:
				log.write(this, "Service not recognised", LoggerMode.WARN);
			}
		}
	}

	private static DeviceServiceRegister dsr;
	private static ServiceObserver so;
	private Map<Byte, String> registeredServices;
	
	private Logger log;

	/**
	 * Implements the background work for opening resources and listening for 
	 * incoming packets.
	 *
	 */
	private class ServiceObserver implements Runnable{
		
		private static final int SERVICE_REGISTER_PORT = 50053;

		private DatagramSocket socket;
		private boolean terminate;

		ServiceObserver(){
			try {
				socket = new DatagramSocket(SERVICE_REGISTER_PORT, InetAddress.getByName("0.0.0.0"));
				socket.setBroadcast(true);
			} catch (SocketException ex) {
				log.write(this, ex.getMessage(), LoggerMode.ERROR);
			} catch (UnknownHostException ex) {
				log.write(this, ex.getMessage(), LoggerMode.ERROR);
			}
		}

		@Override
		public void run() {
			try {
				log.write(this, "ServiceObserver [RDY]", LoggerMode.INFO);
				while(!terminate) {
					byte[] buffer = new byte[2048];
					final DatagramPacket datagram = new DatagramPacket(buffer, buffer.length);
					socket.receive(datagram);//blocking receive

					final String msg = new String(datagram.getData());
					log.write(this, ("UDP packet received from " 
							+ datagram.getAddress().getHostAddress()), LoggerMode.TRACE);
					log.write(this, ("UDP payload: " + msg), LoggerMode.TRACE);

					byte[] responseSig = null;
					final ServiceBroadcast sb = deserializePayload(msg.trim());
					if(sb == null) {
						responseSig = "NACK\r\n".getBytes();
					}else{
						responseSig = "ACK\r\n".getBytes();
						addServices(sb);
					}

					socket.send(new DatagramPacket(responseSig, responseSig.length, 
							datagram.getAddress(),
							datagram.getPort()));

					log.write(this, ("Responding " 
							+ new String(responseSig).trim() 
							+ " to " + datagram.getSocketAddress()),
							LoggerMode.TRACE);
				}
			}catch(IOException ex) {
				log.write(this, ex.getMessage(), LoggerMode.ERROR);
			}	    	
		}
	}

	private ServiceBroadcast deserializePayload(final String payload) {
		ServiceBroadcast sb = null;
		try {
			sb = new GsonBuilder().create().fromJson(payload, ServiceBroadcast.class);
		} catch(JsonParseException ex) {
			return null;
		}
		return sb;
	}

}
