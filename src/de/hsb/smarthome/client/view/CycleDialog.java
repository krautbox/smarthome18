package de.hsb.smarthome.client.view;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.awt.Toolkit;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.LinkedList;
import java.util.List;

import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextField;

import de.hsb.smarthome.util.json.Device;
import de.hsb.smarthome.util.json.Device.Cycle;
import de.hsb.smarthome.util.json.UnsetCycleException;
import de.hsb.smarthome.util.json.Weekday;
import de.hsb.smarthome.util.log.Logger;
import de.hsb.smarthome.util.log.Logger.LoggerMode;

public class CycleDialog extends JDialog{
	//
	public CycleDialog(String title, Device device, String cycleType){
		setTitle(title);
		setModal(true);
		setLayout(new BorderLayout());
		mDevice = device;
		mCycleType = cycleType;
		mTmpCycle = device.new Cycle(cycleType);

		
		mTextFieldName = new JTextField("Neuer Cycle");
		
		if(cycleType.equals(Device.Cycle.CYCLETYPE_TIME)) {
			mTextFieldStart = new JTextField("00:00:00");
			mTextFieldEnd = new JTextField("00:00:00");
		}else if(cycleType.equals(Device.Cycle.CYCLETYPE_TEMPERATURE)){
			
			mTextFieldStart = new JTextField("0.0");
			mTextFieldEnd = new JTextField("0.0");
		}
		
		mCheckBoxMon = new JCheckBox("Mo");
		mCheckBoxMon.setSelected(true);
		mCheckBoxTue = new JCheckBox("Di");
		mCheckBoxTue.setSelected(true);
		mCheckBoxWed = new JCheckBox("Mi");
		mCheckBoxWed.setSelected(true);
		mCheckBoxThu = new JCheckBox("Do");
		mCheckBoxThu.setSelected(true);
		mCheckBoxFri = new JCheckBox("Fr");
		mCheckBoxFri.setSelected(true);
		mCheckBoxSat = new JCheckBox("Sa");
		mCheckBoxSat.setSelected(true);
		mCheckBoxSun = new JCheckBox("So");
		mCheckBoxSun.setSelected(true);
		mCheckBoxEveryWeek = new JCheckBox("jede Woche");
		mCheckBoxEveryWeek.setEnabled(false);
		mCheckBoxEveryTwoWeeks = new JCheckBox("jede 2. Woche");
		mCheckBoxEveryTwoWeeks.setEnabled(false);
		mCheckBoxEveryThreeWeeks = new JCheckBox("jede 3. Woche");
		mCheckBoxEveryThreeWeeks.setEnabled(false);
		mCheckBoxEveryFourWeeks = new JCheckBox("jede 4. Woche");
		mCheckBoxEveryFourWeeks.setEnabled(false);
		mTextFieldStartAt = new JTextField("01.01.2018");
		mTextFieldStartAt.setEnabled(false);
		mTextFieldEndAt = new JTextField("01.01.2018");
		mTextFieldEndAt.setEnabled(false);
		
		JPanel mainPanel = new JPanel(new BorderLayout());
		JPanel labelPanel = new JPanel(new GridLayout(9,1));
		
		labelPanel.add(new JLabel("Name: "));
		if(cycleType.equals(Device.Cycle.CYCLETYPE_TIME)) {
			labelPanel.add(new JLabel("Startuhrzeit: "));
			labelPanel.add(new JLabel("Enduhrzeit: "));
		}else if(cycleType.equals(Device.Cycle.CYCLETYPE_TEMPERATURE)){
			labelPanel.add(new JLabel("Starttemperatur: "));
			labelPanel.add(new JLabel("Endtemperatur: "));
		}
		
		labelPanel.add(new JLabel("Ausfuehren am: "));
		labelPanel.add(new JLabel(""));
		labelPanel.add(new JLabel("Beginn am: "));
		labelPanel.add(new JLabel("Ende am: "));
		labelPanel.add(new JLabel("Wiederholen:"));
		labelPanel.add(new JLabel(""));
		mainPanel.add(labelPanel, BorderLayout.WEST);
		
		JPanel inputPanel = new JPanel(new GridLayout(9, 1));
		inputPanel.add(mTextFieldName);
		inputPanel.add(mTextFieldStart);
		inputPanel.add(mTextFieldEnd);
		
		JPanel dayPicker1 = new JPanel(new GridLayout(1, 4));
		dayPicker1.add(mCheckBoxMon);
		dayPicker1.add(mCheckBoxTue);
		dayPicker1.add(mCheckBoxWed);
		dayPicker1.add(mCheckBoxThu);
		inputPanel.add(dayPicker1);
		
		JPanel dayPicker2 = new JPanel(new GridLayout(1, 4));
		dayPicker2.add(mCheckBoxFri);
		dayPicker2.add(mCheckBoxSat);
		dayPicker2.add(mCheckBoxSun);
		dayPicker2.add(new JLabel(""));
		inputPanel.add(dayPicker2);
		
		inputPanel.add(mTextFieldStartAt);
		inputPanel.add(mTextFieldEndAt);

		JPanel weekPicker1 = new JPanel(new GridLayout(1,2));
		weekPicker1.add(mCheckBoxEveryWeek);
		weekPicker1.add(mCheckBoxEveryTwoWeeks);
		inputPanel.add(weekPicker1);
		
		JPanel weekPicker2 = new JPanel(new GridLayout(1,2));
		weekPicker2.add(mCheckBoxEveryThreeWeeks);
		weekPicker2.add(mCheckBoxEveryFourWeeks);
		inputPanel.add(weekPicker2);
		mainPanel.add(inputPanel, BorderLayout.CENTER);
		
		add(mainPanel, BorderLayout.CENTER);
		
			
		FlowLayout layout = new FlowLayout(FlowLayout.CENTER);
		layout.setHgap(20);
		JPanel footer = new JPanel(layout);
		JButton buttonOK = new JButton("OK");
		JButton buttonAbort = new JButton("Abbruch");
		footer.add(buttonOK);
		footer.add(buttonAbort);
		add(footer, BorderLayout.SOUTH);

		pack();
		//Dieser Abschnitt muss nach pack() stehen.  
		Toolkit tk = Toolkit.getDefaultToolkit();
		Dimension d = tk.getScreenSize();
		this.setLocation((int) (d.getWidth() / 2 - this.getSize().getWidth() / 2),
				(int) (d.getHeight() / 2 - this.getSize().getHeight() / 2));
		
		addWindowListener(new WindowAdapter() {
			@Override
			public void windowClosing(WindowEvent e) {
				abort();
			}
		});
		
		buttonOK.addActionListener(e->{
			confirm();
		});
		
		buttonAbort.addActionListener(e->{
			abort();
		});
		
		
	}
	
	private void abort() {
		mLogger.write(this, "cycle discarded", LoggerMode.INFO);
		dispose();
	}
	
	private void confirm() {
		
		if(mTextFieldName.getText() != null && mTextFieldName.getText() != "") {
			mTmpCycle.setName(mTextFieldName.getText());
		}else {
			JOptionPane.showMessageDialog(this,"Der Name darf nicht leer sein.\n",
					"Name fehlt",JOptionPane.ERROR_MESSAGE);
			return;
		}
		
		
		if(mCycleType.equals(Device.Cycle.CYCLETYPE_TIME)) {
			try {
				mTmpCycle.setStartTime(mTextFieldStart.getText());
				mTmpCycle.setStopTime(mTextFieldEnd.getText());
			}catch(IllegalArgumentException | UnsetCycleException e) {
				mLogger.write(this, e.getMessage(), LoggerMode.ERROR);
				JOptionPane.showMessageDialog(this,"Das angegebene Zeitformat ist nicht korrekt.\n"
						+ "Schreiben sie die Uhrzeit wie folgt 00:00:00 im 24h Format.\n"
						+ "Beispiel: \"23:58:00\"",
						"Falsches Format",JOptionPane.ERROR_MESSAGE);
				return;
			}
		}else if (mCycleType.equals(Device.Cycle.CYCLETYPE_TEMPERATURE)) {
			try {
				mTmpCycle.setStartTemperature(Float.valueOf(mTextFieldStart.getText()));
				mTmpCycle.setStopTemperature(Float.valueOf(mTextFieldEnd.getText()));
			}catch(NumberFormatException | NullPointerException | UnsetCycleException e) {
				mLogger.write(this, e.getMessage(), LoggerMode.ERROR);
				JOptionPane.showMessageDialog(this,"Das angegebene Temperaturformat ist nicht korrekt.\n"
						+ "Schreiben sie die Temperatur wie folgt \"00.0\". Die Angabe erfolgt in Grad Celsius.\n"
						+ "Beispiel: \"23,7\"",
						"Falsches Format",JOptionPane.ERROR_MESSAGE);
				return;
			}
			
		}
		
		if(mCheckBoxMon.isSelected()) {
			mTmpCycle.addWeekday(Weekday.MONDAY);
		}else {
			mTmpCycle.removeWeekday(Weekday.MONDAY);
		}
		
		if(mCheckBoxTue.isSelected()) {
			mTmpCycle.addWeekday(Weekday.TUESDAY);
		}else {
			mTmpCycle.removeWeekday(Weekday.TUESDAY);
		}
		
		if(mCheckBoxWed.isSelected()) {
			mTmpCycle.addWeekday(Weekday.WEDNESDAY);
		}else {
			mTmpCycle.removeWeekday(Weekday.WEDNESDAY);
		}
		
		if(mCheckBoxThu.isSelected()) {
			mTmpCycle.addWeekday(Weekday.THURSDAY);
		}else {
			mTmpCycle.removeWeekday(Weekday.THURSDAY);
		}
		
		if(mCheckBoxFri.isSelected()) {
			mTmpCycle.addWeekday(Weekday.FRIDAY);
		}else {
			mTmpCycle.removeWeekday(Weekday.FRIDAY);
		}
		
		if(mCheckBoxSat.isSelected()) {
			mTmpCycle.addWeekday(Weekday.SATURDAY);
		}else {
			mTmpCycle.removeWeekday(Weekday.SATURDAY);
		}
		
		if(mCheckBoxSun.isSelected()) {
			mTmpCycle.addWeekday(Weekday.SUNDAY);
		}else {
			mTmpCycle.removeWeekday(Weekday.SUNDAY);
		}
		
		List<Cycle> tmpList = mDevice.getCycles();
		if(tmpList == null) {
			tmpList = new LinkedList<Cycle>();
		}
		
		if(!isValidCycle(tmpList, mTmpCycle)) {
			mLogger.write(this, "Invalid cycle.", LoggerMode.ERROR);
			JOptionPane.showMessageDialog(this,"Der Zyklus ueberschneidet sich mit einem anderen Zyklus oder \n "
					+ "keine Tage sind gesetzt.",
					"Nicht gueltiger Zyklus",JOptionPane.ERROR_MESSAGE);
			return;
		}
		
		tmpList.add(mTmpCycle);
		mDevice.setCycles(tmpList);
		mLogger.write(this, "new cycle created", LoggerMode.INFO);
		dispose();
	}
		
	private boolean isValidCycle(List<Cycle> list, Cycle cycle) {
		boolean isValid = true;
		boolean checkDetails = false;
		
		if(cycle.getDays().isEmpty()) {
			isValid = false;
			return isValid;
		}
		
		
		for(int i = 0; i < list.size(); ++i) {
			Cycle tmp = list.get(i);

			for(Weekday day: tmp.getDays()) {
				if(cycle.getDays().contains(day)) {
					checkDetails = true;
				}
			}
			
			if(!checkDetails) {
				return isValid;
			}
			
			if(tmp.getCycletype().compareTo(cycle.getCycletype()) == 0 ) {
			
		
				if(tmp.getStart().compareTo(tmp.getStop()) < 0) { // ist der start kleiner als das ende und stop groesser 
					if(tmp.getStart().compareTo(cycle.getStart()) <= 0 && tmp.getStop().compareTo(cycle.getStart()) >= 0) {
						isValid = false;
					}
					
					if(tmp.getStart().compareTo(cycle.getStop()) <= 0  && tmp.getStop().compareTo(cycle.getStop()) >= 0 ) {
						isValid = false;
					}
				}else {
					if(tmp.getStart().compareTo(cycle.getStart()) <= 0 && tmp.getStop().compareTo(cycle.getStart()) <= 0) {
						isValid = false;
					}
					
					if(tmp.getStart().compareTo(cycle.getStop()) <= 0 && tmp.getStop().compareTo(cycle.getStop()) <= 0  ) {
						isValid = false;
					}
				}
	
			}else {
				mLogger.write(this, "Unknown cycletype.", LoggerMode.ERROR);
			}
			
		}
		
		return isValid;
	}
	
	private static final long serialVersionUID = 1L;
	private Logger mLogger = Logger.getLogger();
	private Cycle mTmpCycle;
	private Device mDevice;
	private JTextField mTextFieldName;
	private JTextField mTextFieldStart;
	private JTextField mTextFieldEnd;
	private JCheckBox mCheckBoxMon;
	private JCheckBox mCheckBoxTue;
	private JCheckBox mCheckBoxWed;
	private JCheckBox mCheckBoxThu;
	private JCheckBox mCheckBoxFri;
	private JCheckBox mCheckBoxSat;
	private JCheckBox mCheckBoxSun;
	private JCheckBox mCheckBoxEveryWeek;
	private JCheckBox mCheckBoxEveryTwoWeeks;
	private JCheckBox mCheckBoxEveryThreeWeeks;
	private JCheckBox mCheckBoxEveryFourWeeks;
	private JTextField mTextFieldStartAt;
	private JTextField mTextFieldEndAt;
	private String mCycleType;

}
