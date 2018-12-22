package de.hsb.smarthome.server;

import de.hsb.smarthome.server.RemoteController.ProcessException;

/**
 * Contains the main method for the server part.
 * 
 * @author Fabian Mangels
 *
 */
public class Main {

	public static void main(String[] args) throws ProcessException {
		new RemoteController("ssh_remote.conf");
	}
}