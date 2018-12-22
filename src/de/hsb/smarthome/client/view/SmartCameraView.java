package de.hsb.smarthome.client.view;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Graphics;
import java.awt.GridLayout;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;

import javax.imageio.ImageIO;
import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.filechooser.FileNameExtensionFilter;

import de.hsb.smarthome.client.controller.IDeviceManager;
import de.hsb.smarthome.util.json.Device;
import de.hsb.smarthome.util.json.Image;
import de.hsb.smarthome.util.log.Logger;
import de.hsb.smarthome.util.log.Logger.LoggerMode;

public class SmartCameraView extends JPanel implements IComponentView{
	

	SmartCameraView(IDeviceManager deviceManager, Device smartObject){
		mDeviceManager = deviceManager;
		mSmartDevice = smartObject;
		BorderLayout borderLayout = new BorderLayout();
		borderLayout.setHgap(20);
		borderLayout.setVgap(20);
		setLayout(borderLayout);
		
		
		
		GridLayout gridLayout = new GridLayout(2, 1);
		gridLayout.setHgap(20);
		JPanel topPanel = new JPanel(gridLayout);
		
		
		FlowLayout layout = new FlowLayout(FlowLayout.LEFT);
		layout.setHgap(20);
		JPanel panelLabel = new JPanel(layout);
		panelLabel.add(new JLabel("Status:"));
		String text = mSmartDevice.isConnected() ? "Verbunden" : "Nicht verbunden";
		mLabelStatus = new JLabel(text);
		panelLabel.add(mLabelStatus);
		topPanel.add(panelLabel);
		
		FlowLayout layout2 = new FlowLayout(FlowLayout.LEFT);
		layout2.setHgap(20);
		JPanel panelButtons = new JPanel(layout2);
		mButtonShowPic = new JButton("Wie schauts aus?");
		mButtonSavePic = new JButton("Bild speichern");
		panelButtons.add(mButtonShowPic);
		panelButtons.add(mButtonSavePic);
		topPanel.add(panelButtons);
		
//		GridLayout gridLayout = new GridLayout(3, 4);
//		gridLayout.setHgap(20);
//		JPanel topPanel = new JPanel(gridLayout);
//			topPanel.add(new JLabel("Verbunden ueber:"));
//			mLabelConnectionMode = new JLabel("default");
//			topPanel.add(mLabelConnectionMode);
//			topPanel.add(new JLabel("Status"));
//			String text = mSmartDevice.isConnected() ? "erreichbar" : "nicht erreichbar";
//			mLabelStatus = new JLabel(text);
//			topPanel.add(mLabelStatus);
//			
//			topPanel.add(new JLabel("IP im Fernnetz"));
//			mLabelIP = new JLabel("default");
//			topPanel.add(mLabelIP);
//			topPanel.add(new JLabel(""));
//			topPanel.add(new JLabel(""));
//			
//			mButtonShowPic = new JButton("Wie schauts aus?");
//			mButtonSavePic = new JButton("Bild speichern");
//			topPanel.add(mButtonShowPic);
//			topPanel.add(mButtonSavePic);
		
		this.add(topPanel, BorderLayout.NORTH);
		mPanelImage = new ImagePanel(SmartCameraView.class.getProtectionDomain().getCodeSource().getLocation().getPath() + "../images/noimage.jpg");
		this.add(mPanelImage, BorderLayout.CENTER);
		
		mButtonShowPic.addActionListener(e->{
			mDeviceManager.commitDevice(mSmartDevice);
		});
		
		mButtonSavePic.addActionListener(e->{
			

			JFileChooser fileChooser = new JFileChooser();
			fileChooser.addChoosableFileFilter(new FileNameExtensionFilter("JPG ","jpg"));
			if (fileChooser.showSaveDialog(this) == JFileChooser.APPROVE_OPTION) {
				File outputfile = fileChooser.getSelectedFile();
				if(!outputfile.getAbsolutePath().endsWith(".jpg") || 
						!outputfile.getAbsolutePath().endsWith(".jpeg") ||
						!outputfile.getAbsolutePath().endsWith(".JPG") ||
						!outputfile.getAbsolutePath().endsWith(".JPEG")) {
					outputfile = new File(outputfile.getPath()+".jpg");
					
				}
				
				if(outputfile.exists()) {
					mLogger.write(this, "Files exists and its possible that the image can't written to this path. " + outputfile.getPath(), LoggerMode.WARN);
				}

				try {
					ImageIO.write(mPanelImage.getCurrentImage(), "jpg", outputfile);
				} catch (IOException e1) {
					mLogger.write(this, "Error while creating ouput file.", LoggerMode.ERROR);
					mLogger.write(this, e1.getMessage(), LoggerMode.ERROR);
				}

			}
			
		});
		
	}
	
	class ImagePanel extends JPanel{
		private static final long serialVersionUID = 1L;
		private BufferedImage mImage;
		
		ImagePanel(String path){
			
			try {                
				mImage = ImageIO.read(new File(path));
			} catch (IOException ex) {
				mLogger.write(this, "Fehler beim laden des Bildes", LoggerMode.ERROR);
			}
		}
		
		public void setNewImage(BufferedImage image) {
			mImage=image;
			repaint();
		}
		
		public BufferedImage getCurrentImage() {
			return mImage;
		}
		
		@Override 
		public void paint(Graphics g){
	        //Get the current size of this component
			super.paint(g);
	        Dimension d = this.getSize();
	        
	        g.drawImage(mImage, 0, 0, d.width, d.height, null);

	    }
	}
	
//	@Override
//	public void receiveTransmission(Transmission obj) {
//		
//	}
//	
//	public void unregisterFromTCPConnection() {
//		mLogger.write(this, "Removed as TCP observer", LoggerMode.TRACE);
//		mModel.getTCPConnection().unregister(this);
//	}
	
	public boolean isViewFromDevice(Device dev) {
		return mSmartDevice.equals(dev);
	}
	
	@Override
	public void update(Device device) {
		if(device != null) {
			mLabelStatus.setText(device.isConnected() ? "Verbunden" : "Nicht verbunden");
		}else {
			mLogger.write(this, "View isn't updated. Device is null.", LoggerMode.WARN);
		}
	}
	
	@Override
	public void update(Device device, Image image) {
		update(device);
		
		try {
			mPanelImage.setNewImage(image.asImage());
		} catch (IOException e) {
			mLogger.write(this, "Can't display image.", LoggerMode.ERROR);
			mLogger.write(this, e.getMessage(), LoggerMode.ERROR);
		}
	}
	
	@Override
	public ViewType getViewType() {
		return ViewType.SMARTCAMERA;
	}
	
	@Override
	public void close() {
		// TODO Auto-generated method stub
		
	}
	
	private static final long serialVersionUID = 1L;
	private Logger mLogger = Logger.getLogger();
	private Device mSmartDevice;
	private IDeviceManager mDeviceManager;
	
//	private JLabel mLabelConnectionMode;
//	private JLabel mLabelIP;
	private JLabel mLabelStatus;
	
	private JButton mButtonShowPic;
	private JButton mButtonSavePic;
	private ImagePanel mPanelImage;





}
