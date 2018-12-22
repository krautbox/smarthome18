package de.hsb.smarthome.client.view;

import de.hsb.smarthome.util.json.Device;
import de.hsb.smarthome.util.json.Image;

public interface IComponentView {
	public enum ViewType{
		RASPBERRY,
		SMARTSWITCH,
		SMARTCAMERA
	}
	
	public void update(Device device);
	public void update(Device device, Image image);
	public ViewType getViewType();
	public void close();
}
