package de.hsb.smarthome.util.json.env;

import com.google.gson.annotations.SerializedName;

/**
 * Models a service broadcast message sent by a smarthome device to 
 * register its service.
 *
 */
public class ServiceBroadcast {
	
	public ServiceBroadcast() {
		//no-arg constructor
	}
	
	public String getLocalIP() {
		return this.localIP;
	}
	
	public void setLocalIP(final String localIP) {
		this.localIP = localIP;
	}
	
	public byte[] getServices() {
		return this.services;
	}
	
	public void setServices(final byte[] services) {
		this.services = services;
	}

	@SerializedName("localIP")
	private String localIP;
	@SerializedName("services")
	private byte[] services;

}
