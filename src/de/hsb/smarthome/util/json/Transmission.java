package de.hsb.smarthome.util.json;

import java.util.List;
import java.util.LinkedList;

import com.google.gson.GsonBuilder;
import com.google.gson.JsonSyntaxException;

/**
 * Models a transmission between the control unit of a smarthome and the client application.
 *
 */
public class Transmission {

	/**
	 * Declares all supported actions for a particular transmission.
	 *
	 */
	public enum Action {
		REPORT,
		COMMIT,
		SUCCESS,
		ERROR,
		CLOSE;
	}
	
	public static final transient String TIMESTAMP_FORMAT = "yyyy-MM-dd'T'HH:mm:ssZ";

	public Transmission() {
		//no-arg constructor
	}

	public Action getAction() {
		return action;
	}

	public void setAction(final Action action) {
		this.action = action;
	}

	public Control getControl() {
		return control;
	}

	public void setControl(final Control control) {
		this.control = control;
	}

	public List<Device> getDevices() {
		return devices;
	}

	public void setDevices(final List<Device> devices) {
		this.devices = devices;
	}

	public void addDevice(final Device device) {
		if(this.devices == null){
			this.devices = new LinkedList<Device>();
		}
		this.devices.add(device);
	}

	public List<Image> getImages() {
		return images;
	}

	public void setImages(final List<Image> images) {
		this.images = images;
	}

	public String getMessage() {
		return message;
	}

	public void setMessage(final String message) {
		this.message = message;
	}
	
	/**
	 * Serializes a given Transmission instance to a JSON string
	 * 
	 * @param transmission The transmission to serialize
	 * @return the JSON representation of the transmission
	 */
	public static String serializeToJson(final Transmission transmission) {
		return new GsonBuilder()
				.setDateFormat(TIMESTAMP_FORMAT)
				.create()
				.toJson(transmission);
	}

	/**
	 * Deserializes a JSON representation to a Transmission instance
	 * 
	 * @param json The JSON string to deserialize
	 * @return a Transmission instance represented by the given JSON string
	 * @throws JsonSyntaxException If the given string was invalid JSON
	 */
	public static Transmission deserializeJson(final String json) throws JsonSyntaxException {
		return new GsonBuilder()
				.setDateFormat(TIMESTAMP_FORMAT)
				.create()
				.fromJson(json, Transmission.class);
	}

	private Action action;
	private String message;
	private Control control;
	private List<Device> devices;
	private List<Image> images;
}