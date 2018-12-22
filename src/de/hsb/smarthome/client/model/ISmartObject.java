package de.hsb.smarthome.client.model;

import java.awt.Image;

public interface ISmartObject{
	
 

	int getID();
	String getName();
	void setName(String name);
	boolean getAccessableState();
	int whoAmI();
	
	String getIP();
	//anderes Objekt? AWT alt??
	Image getImage();
	String getConnectionMode();
	
	
	//Functions for a Smart Switch
	//Die Funktionsnamen sind der AHA-HTTP-Interface Spezifikation nachbenannt.
	void setSwitchOn();
	void setSwitchOff();
	boolean getSwitchState();
	void setSwitchToggle();
	int getSwitchPower();
	int getSwitchEngery();
	int getTemperature();
	
	//eigene Namen (Zusatzfunktionen)
	long getAID();
	void setTimeSchedule(String name, int beginHour, int endHour, int beginMinute, int endMinute);
	void deleteTimeSchedule(String name);
	String[] getTimeSchedules();
	void setTemperatureSchedule(String name, double beginTemperature, double endTemperature);
	void deleteTemperatureSchedule(String name);
	String[] getTemperatureSchedules();
	String[] getPossibleCommands();
	
	public static final int GENERIC_OBJECT = 0;
	public static final int SMART_SWITCH = 1;
	public static final int SMART_CAMERA = 2;
	
}
