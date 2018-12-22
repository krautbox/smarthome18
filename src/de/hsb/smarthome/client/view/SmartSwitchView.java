package de.hsb.smarthome.client.view;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Graphics;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.List;

import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JToggleButton;

import de.hsb.smarthome.client.controller.IDeviceManager;
import de.hsb.smarthome.util.json.Device;
import de.hsb.smarthome.util.json.Device.Cycle;
import de.hsb.smarthome.util.json.Image;
import de.hsb.smarthome.util.json.Weekday;
import de.hsb.smarthome.util.log.Logger;
import de.hsb.smarthome.util.log.Logger.LoggerMode;

public class SmartSwitchView extends JPanel implements IComponentView{
	
	
	public SmartSwitchView(IDeviceManager deviceManager, Device smartObject) {
		super();
		mDeviceManager = deviceManager;
		mSmartDevice = smartObject;
		GridLayout mainLayout = new GridLayout(2, 1);
		this.setLayout(mainLayout);
		
		JPanel topElements = new JPanel(new GridLayout(2, 1));
		buildFirstPanel(topElements);
		buildSecondPanel(topElements);
		this.add(topElements);
		
		JPanel bottomElements  = new JPanel(new BorderLayout());
		buildThirdPanel(bottomElements);
		this.add(bottomElements);
		setActionListeners();
		
		update(mSmartDevice);
	}

	
	private void buildFirstPanel(JPanel toThis) {
		//TopPanel
		JPanel firstPanel = new JPanel(new GridLayout(4,3));
		
		firstPanel.add(new JLabel("Temperatur:"));
		mTemperature = new JLabel( String.valueOf(mSmartDevice.getTemperature()) + " Grad");
		firstPanel.add(mTemperature);
		
		firstPanel.add(new JLabel("Status:"));
			JPanel onOff = new JPanel(new FlowLayout(FlowLayout.LEFT));
			mToggleButtonSetSwitchState = new JToggleButton("Platzhalter");
			mToggleButtonSetSwitchState.setSelected(mSmartDevice.getStatus() == 0 ? false : true);
			String text = mSmartDevice.getStatus() == 0 ? "Aus" : "Ein";
			mToggleButtonSetSwitchState.setText(text);
			
			onOff.add(mToggleButtonSetSwitchState);
		firstPanel.add(onOff);
		
		firstPanel.add(new JLabel("Stromverbrauch:"));
		mEnergy = new JLabel("default" +" W");
		firstPanel.add(mEnergy);
		
		firstPanel.add(new JLabel("Gesamtverbraucht: "));
		mPower = new JLabel("default");
		firstPanel.add(mPower);
		
		toThis.add(firstPanel);
	}
	
	private void buildSecondPanel(JPanel toThis) {
		// secondPanel
		GridLayout grid = new GridLayout(6,4);
		grid.setHgap(8);
		JPanel secondPanel = new JPanel(grid);
		
		secondPanel.add(new JLabel("Zeitplan"));
		//String[] arr = {"---"};
		mComboBoxTimeScheduling = new JComboBox<Device.Cycle>() ;
		Device.Cycle tmp = mSmartDevice.new Cycle();
		tmp.setName("---");
		mComboBoxTimeScheduling.addItem(tmp);
		secondPanel.add(mComboBoxTimeScheduling);
		
		mButtonNewTimeScheduling = new JButton("Neuer Zeitplan");
		secondPanel.add(mButtonNewTimeScheduling);
		
		mButtonDeleteTimeScheduling = new JButton("Zeitplan loeschen");
		secondPanel.add(mButtonDeleteTimeScheduling);
		
		
		secondPanel.add(new JLabel(""));
		
		JPanel panelTimeSchedulingOn = new JPanel(new FlowLayout(FlowLayout.LEFT));
			panelTimeSchedulingOn.add(new JLabel("Ein um: "));
			
			mLabelTimeSchedulingOn = new JLabel("- Uhr");
			panelTimeSchedulingOn.add(mLabelTimeSchedulingOn);
		secondPanel.add(panelTimeSchedulingOn);	
			
		JPanel panelTimeSchedulingOff = new JPanel(new FlowLayout(FlowLayout.LEFT));
			panelTimeSchedulingOff.add(new JLabel("Aus um: "));
			mLabelTimeSchedulingOff = new JLabel("- Uhr");
			panelTimeSchedulingOff.add(mLabelTimeSchedulingOff);
		secondPanel.add(panelTimeSchedulingOff);
		
		secondPanel.add(new JLabel(""));
		secondPanel.add(new JLabel(""));
		secondPanel.add(new JLabel("Wiederholen am: "));
		mLabelRepeatTimeScheduling = new JLabel("-");
		secondPanel.add(mLabelRepeatTimeScheduling);
		secondPanel.add(new JLabel(""));
		
		secondPanel.add(new JLabel("Temperaturplan"));
		mComboBoxTemperatureScheduling = new JComboBox<Device.Cycle>();
		tmp = mSmartDevice.new Cycle();
		tmp.setName("---");
		mComboBoxTemperatureScheduling.addItem(tmp);
		secondPanel.add(mComboBoxTemperatureScheduling);
		
		mButtonNewTemperatureScheduling = new JButton("Neuer Temperaturplan");
		secondPanel.add(mButtonNewTemperatureScheduling);
		
		mButtonDeleteTemperatureScheduling = new JButton("Temperaturplan loeschen");
		secondPanel.add(mButtonDeleteTemperatureScheduling);
		
		secondPanel.add(new JLabel(""));
		
		JPanel panelTemperatureSchedulingOn = new JPanel();
			panelTemperatureSchedulingOn.setLayout(new FlowLayout(FlowLayout.LEFT));
			panelTemperatureSchedulingOn.add(new JLabel("Ein bei: "));
			mLabelTemperatureSchedulingOn = new JLabel("- Grad");
			panelTemperatureSchedulingOn.add(mLabelTemperatureSchedulingOn);
		secondPanel.add(panelTemperatureSchedulingOn);	
			
		JPanel panelTemperatureSchedulingOff = new JPanel();
			panelTemperatureSchedulingOff.setLayout(new FlowLayout(FlowLayout.LEFT));
			panelTemperatureSchedulingOff.add(new JLabel("Aus bei: "));
			mLabelTemperatureSchedulingOff = new JLabel("- Grad");
			panelTemperatureSchedulingOff.add(mLabelTemperatureSchedulingOff);
		secondPanel.add(panelTemperatureSchedulingOff);
		
		secondPanel.add(new JLabel(""));
		
		secondPanel.add(new JLabel(""));
		secondPanel.add(new JLabel("Wiederholen am: "));
		mLabelRepeatTemperatureScheduling = new JLabel("-");
		secondPanel.add(mLabelRepeatTemperatureScheduling);
		secondPanel.add(new JLabel(""));
		
		toThis.add(secondPanel);
	}
	
	private void buildThirdPanel(JPanel toThis) {

		JPanel thirdTopPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
	
		mButtonCommit = new JButton("Commit");
		thirdTopPanel.add(mButtonCommit);
		toThis.add(thirdTopPanel, BorderLayout.NORTH);
		
		//FourthPanel
		JPanel thirdBottomPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
		mTextArea =  new JTextArea(5, 20);
		mTextArea.setEditable(false);
		
		mScrollPaneTextArea = new JScrollPane(mTextArea, JScrollPane.VERTICAL_SCROLLBAR_ALWAYS, JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
		mScrollPaneTextArea.setMinimumSize(new Dimension(400, 100));
		mScrollPaneTextArea.setPreferredSize(new Dimension(540, 200));
		thirdBottomPanel.add(mScrollPaneTextArea);
		
		
		toThis.add(thirdBottomPanel, BorderLayout.CENTER);
			
	}
	
	@Override 
	public void paint(Graphics g){
		super.paint(g);
        Dimension d = this.getSize();
        mScrollPaneTextArea.setPreferredSize(new Dimension(d.width-30, 200));

    }
	

	/**
	 * sets the ActionListeners for the Components
	 */
	private void setActionListeners() {
		mButtonCommit.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent e) {
				mDeviceManager.commitDevice(mSmartDevice);
			}
		});
		
		mToggleButtonSetSwitchState.addActionListener(e->{
			String text = "Aus";
			if(mToggleButtonSetSwitchState.isSelected()) {
				mSmartDevice.setStatus(1);
				text = "Ein";
			}else {
				mSmartDevice.setStatus(0);
			}
			mToggleButtonSetSwitchState.setText(text);
			setAllEnabled(false);
			mDeviceManager.commitDevice(mSmartDevice);
			
			
		});
		
		mButtonNewTimeScheduling.addActionListener(e->{
			CycleDialog dialog = new CycleDialog("Neuer Zeitintervall", mSmartDevice, Device.Cycle.CYCLETYPE_TIME);
			dialog.setVisible(true);
			update(mSmartDevice);
		});
		
		mButtonNewTemperatureScheduling.addActionListener(e->{
			CycleDialog dialog = new CycleDialog("Neuer Temperaturintervall", mSmartDevice, Device.Cycle.CYCLETYPE_TEMPERATURE);
			dialog.setVisible(true);
			update(mSmartDevice);
		});
		
		mButtonDeleteTimeScheduling.addActionListener(e->{
			Device.Cycle tmp = mComboBoxTimeScheduling.getItemAt(mComboBoxTimeScheduling.getSelectedIndex());
			List<Cycle> tmpList = mSmartDevice.getCycles();
			if(tmpList == null || !tmpList.remove(tmp)) {
				//ERROR
				mLogger.write(this, "No schedulingplan exists.", LoggerMode.ERROR);
			}
			mSmartDevice.setCycles(tmpList);
			update(mSmartDevice);
		});
		
	}
	
	public void setAllEnabled(boolean aState) {
//		mToggleButtonSetSwitchState.setEnabled(aState);
//		mButtonNewTimeScheduling.setEnabled(aState);
//		mButtonDeleteTimeScheduling.setEnabled(aState);
//		mButtonNewTemperatureScheduling.setEnabled(aState);
//		mButtonDeleteTemperatureScheduling.setEnabled(aState);
		
	}
	
	public boolean isViewFromDevice(Device dev) {
		return mSmartDevice.equals(dev);
	}
	
	@Override
	public void update(Device device) {
		mLogger.write(this, "Update view with device ID: " + device.getId(), LoggerMode.TRACE);
		mSmartDevice = device;
		writeDeviceInTextArea();
		
		mTemperature.setText(String.valueOf(device.getTemperature()));
		mToggleButtonSetSwitchState.setText(mSmartDevice.getStatus() == 0 ? "Aus" : "Ein");
		mEnergy.setText("default");
		mPower.setText("default");
		mLabelTimeSchedulingOn.setText("-");
		mLabelTimeSchedulingOff.setText("-");
		mLabelRepeatTimeScheduling.setText("-");
		mLabelTemperatureSchedulingOn.setText("-");
		mLabelTemperatureSchedulingOff.setText("-");
		mLabelRepeatTemperatureScheduling.setText("-");
		
		if(device.getCycles() != null /*&& !device.getCycles().isEmpty()*/) {
			List<Cycle> tmpList = device.getCycles();
			mComboBoxTimeScheduling.removeAllItems();
			mComboBoxTemperatureScheduling.removeAllItems();
			for(int i = 0; i < tmpList.size(); ++i) {
				if(tmpList.get(i).getCycletype() == Device.Cycle.CYCLETYPE_TIME) {
					mComboBoxTimeScheduling.addItem(tmpList.get(i));
				}else if (tmpList.get(i).getCycletype() == Device.Cycle.CYCLETYPE_TEMPERATURE){
					mComboBoxTemperatureScheduling.addItem(tmpList.get(i));
				}
			}
			
			if(mComboBoxTimeScheduling.getItemCount() == 0) {
				mLogger.write(this, "keine Elemente", LoggerMode.TRACE);
				Device.Cycle tmp = mSmartDevice.new Cycle();
				tmp.setName("---");
				mComboBoxTimeScheduling.addItem(tmp);
			}else {
				Device.Cycle tmp = mComboBoxTimeScheduling.getItemAt(0);
				mLabelTimeSchedulingOn.setText(tmp.getStart());
				mLabelTimeSchedulingOff.setText(tmp.getStop());
				String tmpString = "";
				for(final Weekday day : tmp.getDays()) {
					tmpString = tmpString+(day.toString().substring(0, 3))+", ";
				}
				tmpString = tmpString.substring(0, tmpString.length()-2);
				mLabelRepeatTimeScheduling.setText(tmpString);
			}
			
			if(mComboBoxTemperatureScheduling.getItemCount() == 0) {
				Device.Cycle tmp = mSmartDevice.new Cycle();
				tmp.setName("---");
				mComboBoxTemperatureScheduling.addItem(tmp);
			}else {
				Device.Cycle tmp = mComboBoxTemperatureScheduling.getItemAt(0);
				mLabelTemperatureSchedulingOn.setText(tmp.getStart());
				mLabelTemperatureSchedulingOff.setText(tmp.getStop());
				String tmpString = "";
				for(final Weekday day : tmp.getDays()) {
					tmpString = tmpString+(day.toString().substring(0, 3))+", ";
				}
				tmpString = tmpString.substring(0, tmpString.length()-2);
				mLabelRepeatTemperatureScheduling.setText(tmpString);
			}
			
		}
		
		setAllEnabled(true);
		
	}
	
	@Override
	public void update(Device device, Image image) {
		update(device);
		
	}
	
	@Override
	public ViewType getViewType() {
		return ViewType.SMARTSWITCH;
	}
	
	private void writeDeviceInTextArea() {
		
		mTextArea.append("Device:\n");
		mTextArea.append("ID: " + mSmartDevice.getId() + "\n");
		mTextArea.append("Name: " + mSmartDevice.getName() + "\n");
		mTextArea.append("Status: " + mSmartDevice.getStatus() + "\n");
		mTextArea.append("Temperatur: " + mSmartDevice.getTemperature() + "\n");
		mTextArea.append("Type: " + mSmartDevice.getType() + "\n");
		mTextArea.append("Cycles:\n");
		List<Cycle> list = mSmartDevice.getCycles();
		if(list != null) {
			for(int i = 0; i < list.size(); ++i) {
				mTextArea.append("Cycle " + i);
				mTextArea.append("Sartzeit: " + list.get(i).getStart() + "\n");
				mTextArea.append("Endzeit: " + list.get(i).getStop() + "\n");
			}
		}else {
			mTextArea.append("Keine\n");
		}
	}
	
	@Override
	public void close() {
		// TODO Auto-generated method stub
		
	}

	private static final long serialVersionUID = 1L;
	
	private Device mSmartDevice = null;
	private IDeviceManager mDeviceManager;
	private Logger mLogger = Logger.getLogger();
	
	private JLabel mTemperature;
	private JLabel mEnergy;
	private JLabel mPower;
	private JComboBox<Device.Cycle> mComboBoxTimeScheduling;
	private JLabel mLabelTimeSchedulingOn;
	private JLabel mLabelTimeSchedulingOff;
	private JLabel mLabelRepeatTimeScheduling;
	private JComboBox<Device.Cycle> mComboBoxTemperatureScheduling;
	private JLabel mLabelTemperatureSchedulingOn;
	private JLabel mLabelTemperatureSchedulingOff;
	private JLabel mLabelRepeatTemperatureScheduling;
	private JTextArea mTextArea;
	private JScrollPane mScrollPaneTextArea;
	
	private JToggleButton mToggleButtonSetSwitchState;
	private JButton mButtonNewTimeScheduling;
	private JButton mButtonDeleteTimeScheduling;
	private JButton mButtonNewTemperatureScheduling;
	private JButton mButtonDeleteTemperatureScheduling;
	private JButton mButtonCommit;


}


//@Override
//public void receiveTransmission(Transmission obj) {
//	mTextArea.append(Transmission.serializeToJson(obj));
//	setAllEnabled(true);
//	
//	Device tmp = Model.getEqualDevice(obj.getDevices(), mSmartDevice);
//	if( tmp != null ) {
//		
//		mSmartDevice = tmp;
//	}else {
//		//sendError();
//	}
//	
//	updateView();
//}

//private void sendCommit() {
//	Transmission transObj = new Transmission();
//	transObj.setAction(Action.COMMIT);
//	transObj.setMessage("Steckdose Commit");
//	Vector<Device> vec = new Vector<Device>();
//	vec.add(mSmartDevice);
//	transObj.setDevices(vec);
//	
//	mTextArea.append("Daten gesendet!\nWarte auf Antwort...\n");
//	try {
//		mModel.getTCPConnection().sendTransmission(transObj);
//	} catch (IOException e) {
//		mLogger.write(this, "Error while sending data", LoggerMode.ERROR);
//		mLogger.write(this, e.getMessage() ,LoggerMode.ERROR);
//	}
//}

//private void sendError() {
//	Transmission transObj = new Transmission();
//	transObj.setAction(Action.ERROR);
//	transObj.setMessage("Get no Device");
//	
//	try {
//		mModel.getTCPConnection().sendTransmission(transObj);
//	} catch (IOException e) {
//		mLogger.write(this, "Error while sending error :(", LoggerMode.ERROR);
//		mLogger.write(this, e.getMessage() ,LoggerMode.ERROR);
//	}
//}

//private void updateView() {
//
//}

//public void unregisterFromTCPConnection() {
//	mLogger.write(this, "Removed as TCP observer", LoggerMode.TRACE);
//	
//	mModel.getTCPConnection().unregister(this);
//}
