package de.hsb.smarthome.client;

import de.hsb.smarthome.client.controller.SSH_Control;

public class Main {

	public static void main(String[] args) {
		//SSH_Control ctrl = new SSH_Control("192.168.178.111", 50001);
		//SSH_Control ctrl = new SSH_Control("192.168.178.111", 50001, true);
		//SSH_Control ctrl = new SSH_Control("safer_smart_home.config");
		SSH_Control ctrl = new SSH_Control("safer_smart_home.config", true);
		ctrl.startGUI();
	}
}