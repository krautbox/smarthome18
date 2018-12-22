package de.hsb.smarthome.client.view;

import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Toolkit;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

import javax.swing.ImageIcon;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;

import de.hsb.smarthome.client.controller.IDeviceManager;


public class LoadingDialog extends JDialog {

  public LoadingDialog(JFrame parent, IDeviceManager deviceManager) {
    super(parent, "");

    this.setLayout(new FlowLayout(FlowLayout.LEFT));
    JLabel icon = new JLabel();

    ImageIcon imageIcon = new ImageIcon(
        LoadingDialog.class.getProtectionDomain().getCodeSource().getLocation().getPath()
            + "../images/loading4.gif");
    icon.setIcon(imageIcon);
    // icon.setPreferredSize(new Dimension(100,100));
    this.add(icon);
    JLabel text = new JLabel("Versuche zu Verbinden...");

    text.setFont(new Font("Serif", Font.TYPE1_FONT, 30));
    this.add(text);
       
    pack();

    this.addWindowListener(new WindowAdapter() {

      @Override
      public void windowClosing(WindowEvent e) {
    	  deviceManager.shutdown();
    	  dispose();
      }
    });
  }

	// die groesse (height und width) wird erste gesetzt wenn es visible ist. Damit
	// es
	// an der richtigen Position ist muss es zum geeigenen Zeitpunkt gesetzt werden.
	@Override
	public void setVisible(boolean aState) {
		super.setVisible(aState);

		Toolkit tk = Toolkit.getDefaultToolkit();
		Dimension d = tk.getScreenSize();
		this.setLocation((int) (d.getWidth() / 2 - this.getSize().getWidth() / 2),
				(int) (d.getHeight() / 2 - this.getSize().getHeight() / 2));
		this.pack();
	}
  
  private static final long serialVersionUID = 1L;
//  private Logger mLogger = Logger.getLogger();

}
