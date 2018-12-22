package de.hsb.smarthome.util.json.env;

import com.google.gson.annotations.SerializedName;

/**
 * Models the response body of a temperature and humidity sensor
 *
 */
public class TempHumSensor {
	
	public TempHumSensor() {
		//no-arg constructor
	}
	
	public float getTemperature() {
		return this.temperature;
	}
	public void setTemperature(final float temperature) {
		this.temperature = temperature;
	}
	public byte getHumidity() {
		return this.humidity;
	}
	public void setHumidity(final byte humidity) {
		this.humidity = humidity;
	}
	
	@SerializedName("Temp")
	private float temperature;
	@SerializedName("Hum")
	private byte humidity;

}
