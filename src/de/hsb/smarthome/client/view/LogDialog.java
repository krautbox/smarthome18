package de.hsb.smarthome.client.view;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Toolkit;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;

import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;

import de.hsb.smarthome.util.log.Logger;
import de.hsb.smarthome.util.log.Logger.LoggerMode;

public class LogDialog extends JDialog {

	public LogDialog(JFrame parent, String title, String logFilePath){
		super(parent, title);//setTitle(title);
		
		setLayout(new BorderLayout());
		

		
		JTextArea textArea = new JTextArea();
		JScrollPane scrollPane = new JScrollPane(textArea, JScrollPane.VERTICAL_SCROLLBAR_ALWAYS, JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
		scrollPane.setPreferredSize(new Dimension(750, 400));
		add(scrollPane, BorderLayout.CENTER);
		
		File file = new File(logFilePath);
		if(file.exists()) {
			try {
				BufferedReader bufferedReader = new BufferedReader(new FileReader(file));
				String line = "";
				while((line = bufferedReader.readLine()) != null) {
					textArea.append(line + "\n");
				}
				bufferedReader.close();
			} catch (FileNotFoundException e1) {
				mLogger.write(this, "Can't open file stream", LoggerMode.ERROR);
				mLogger.write(this, e1.getMessage() , LoggerMode.ERROR);
			} catch (IOException e1) {
				mLogger.write(this, "Error while readLine from Logfile", LoggerMode.ERROR);
				mLogger.write(this, e1.getMessage(), LoggerMode.ERROR);
			}
			
		}else {
			mLogger.write(this, "Logfile " + logFilePath + " not exists", LoggerMode.WARN);
		}
		
		
		JButton buttonClose = new JButton("close");
		JPanel footer = new JPanel(new FlowLayout(FlowLayout.CENTER));
		footer.add(buttonClose);
		add(footer, BorderLayout.SOUTH);
		
		buttonClose.addActionListener(e->{
			dispose();
		});
		
		addWindowListener(new WindowAdapter() {
			@Override
			public void windowClosing(WindowEvent e) {
				dispose();
			}
		});
		
		pack();
		
		Toolkit tk = Toolkit.getDefaultToolkit();
		Dimension d = tk.getScreenSize();
		
		this.setLocation((int) (d.getWidth() / 2 - this.getSize().getWidth() / 2),
				(int) (d.getHeight() / 2 - this.getSize().getHeight() / 2));
		
		
		setModal(true);
		
	}
	private static final long serialVersionUID = 1L;
	private Logger mLogger = Logger.getLogger();

}
