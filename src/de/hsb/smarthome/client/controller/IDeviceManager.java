package de.hsb.smarthome.client.controller;

import com.kilo52.common.io.ConfigurationFile;

import de.hsb.smarthome.util.json.Device;

public interface IDeviceManager {
	
	/**
	 * Commit the given device to the model and try to commit with the server.
	 * @param device
	 */
	void commitDevice(Device device);
	
	/**
	 * shuts down the whole program
	 */
	void shutdown();
	
	public ConfigurationFile getConfig();
	
	public void writeConfig(ConfigurationFile config);
}
