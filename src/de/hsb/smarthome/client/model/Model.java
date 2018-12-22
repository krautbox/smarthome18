package de.hsb.smarthome.client.model;

import java.util.List;
import java.util.Vector;

import de.hsb.smarthome.util.json.Control;
import de.hsb.smarthome.util.json.Device;
import de.hsb.smarthome.util.json.Device.Type;

public class Model {

	public Model() {
		// Testdaten erzeugen
		//generateTestData();
	}

	public Vector<Device> getSmartElements(){
		return mSmartObjects;
	}
	
	public void setSmartElements(List<Device> list) {
		
		if( mSmartObjects == null ) {
			mSmartObjects = new Vector<Device>();
		}
		for(int i = 0; i < list.size(); ++i) {
			mSmartObjects.addElement(list.get(i));
		}
	}

	public void setControl(Control control) {
		mControl = control;
	}
	
	public Control getControl() {
		return mControl;
	}

//	public static Device getEqualDevice(List<Device> list, Device device) {
//		Device tmp = null;
//		if(list != null) {
//			for(int i = 0; i < list.size(); ++i) {
//				if(device.equals(list.get(i))) {
//					tmp = list.get(i);
//				}
//			}
//		}
//		return tmp;
//	}

	Vector<Device> mSmartObjects = null;
	Control mControl = null;

	
	@SuppressWarnings("unused")
	private void generateTestData() {

		mSmartObjects = new Vector<Device>();
		
		Device tmp = new Device();
		tmp.setConnected(true);
		tmp.setId(1);
		tmp.setName("SteckdoseA");
		tmp.setStatus(1);
		tmp.setTemperature(21.3F);
		tmp.setType(Type.SOCKET);
		mSmartObjects.addElement(tmp);
		
		tmp = new Device();
		tmp.setConnected(false);
		tmp.setId(2);
		tmp.setName("SteckdoseB");
		tmp.setStatus(0);
		tmp.setTemperature(20.3F);
		tmp.setType(Type.SOCKET);
		mSmartObjects.addElement(tmp);
		
		tmp = new Device();
		tmp.setConnected(true);
		tmp.setId(3);
		tmp.setName("SteckdoseC");
		tmp.setStatus(2);
		tmp.setTemperature(0F);
		tmp.setType(Type.SOCKET);
		mSmartObjects.addElement(tmp);
		
		tmp = new Device();
		tmp.setConnected(true);
		tmp.setId(4);
		tmp.setName("CameraA");
		tmp.setStatus(1);
		tmp.setTemperature(0F);
		tmp.setType(Type.CAMERA);
		mSmartObjects.addElement(tmp);
		
		tmp = new Device();
		tmp.setConnected(true);
		tmp.setId(5);
		tmp.setName("CameraB");
		tmp.setStatus(0);
		tmp.setTemperature(0F);
		tmp.setType(Type.CAMERA);
		mSmartObjects.addElement(tmp);
		
		tmp = new Device();
		tmp.setId(0);
		tmp.setName("Raspberry");
		tmp.setType(Type.RASPBERRY);
		mSmartObjects.addElement(tmp);
	
	}
}
