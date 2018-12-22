package de.hsb.smarthome.util.image;

import java.awt.image.*;
import java.io.*;
import java.net.*;
import java.text.SimpleDateFormat;
import java.util.*;

import javax.imageio.*;

public class IPCamera {

	private final static String LINK = "http://192.168.188.25/snapshot.cgi?user=admin&pwd=Admin18!";
	private final static String NOW = new SimpleDateFormat("yyyy.MM.dd_HH-mm-ss").format(new Date());
	private final static String NAME = "snapshot_" + NOW + ".jpg";
	private final static String FOLDER = "snapshots/";

	/**
	 * This method make a snapshot from ip_camera and save this local
	 */
	public static void snapshotSave(String LINK) {
		File dir = new File(FOLDER);
		if (!dir.exists())
			dir.mkdir();
		File file = new File(FOLDER + NAME);
		try {
			URLConnection conn = new URL(LINK).openConnection();
			conn.connect();
			InputStream in = conn.getInputStream();
			OutputStream out = new FileOutputStream(file);
			int b = 0;
			while (b != -1) {
				b = in.read();
				if (b != -1)
					out.write(b);
			}
			out.close();
			in.close();
		} catch (MalformedURLException e) {
			System.out.println("url: " + LINK + " invalid");
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	/**
	 * Encodes the byte array into base64 string
	 *
	 * @return String a {@link java.lang.String}
	 * @throws IOException
	 * @throws MalformedURLException
	 */
	public static String encodeImage() throws MalformedURLException, IOException {
		// Open URL connection

		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		URLConnection connection;
		connection = new URL(LINK).openConnection();
		connection.connect();
		// Reading a Image from URL
		InputStream imageInFile = new BufferedInputStream(connection.getInputStream());
		int read = imageInFile.read();

		while (read != -1) {
			baos.write(read);
			read = imageInFile.read();
		}

		imageInFile.close();
		baos.close();
		return Base64.getEncoder().encodeToString(baos.toByteArray());
	}

	/**
	 * Decodes the base64 string into byte array and then into image
	 *
	 * @param imageDataString
	 *            - a {@link java.lang.String}
	 * @return image
	 * @throws IOException
	 */
	public static BufferedImage decodeImage(String imageDataString) throws IOException {
		byte[] imageByteArray = Base64.getDecoder().decode(imageDataString);
		ByteArrayInputStream bais = new ByteArrayInputStream(imageByteArray);
		return ImageIO.read(bais);
	}
}