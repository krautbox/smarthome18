package de.hsb.smarthome.util.json;

/**
 * Exception which should be thrown whenever a device's Cycle has an invalid or incoherent state.
 * That is, a caller tried to set a start or stop point but the cycle type was not the type of the 
 * setter method. For example: if a device's cycle type is TIME, calling the setTemperature() method 
 * should fail by throwing an exception.
 *
 */
public class UnsetCycleException extends Exception {

	private static final long serialVersionUID = 1L;
	
	public UnsetCycleException(final String msg) {
		super(msg);
	}

}
