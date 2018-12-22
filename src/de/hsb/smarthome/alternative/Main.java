package de.hsb.smarthome.alternative;

import de.hsb.smarthome.alternative.RemoteController.ProcessException;

/**
 * Contains the main method for the alternative part.
 * 
 * @author Fabian Mangels
 *
 */
public class Main {

	public static void main(String[] args) throws ProcessException {
		new RemoteController("ssh_remote.conf");
	}
}