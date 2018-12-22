package de.hsb.smarthome.util.json;

import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.sql.Timestamp;
import java.util.Base64;

import javax.imageio.ImageIO;
import javax.xml.bind.DatatypeConverter;

/**
 * Models an image sent by a smarthome to the client application.
 *
 */
public class Image {
	
	/**
	 * Declares all supported image formats.
	 *
	 */
	public enum Format {
		JPG,
		PNG;
	}

	public Image() {
		//no-arg constructor
	}
	
	/**
	 * Returns the BufferedImage instance of this image
	 * 
	 * @return a BufferedImage, or null if this Image instance has no serialized 
	 * 		   image attached to it
	 * @throws IOException If an exception occures during the construction of the 
	 * 		   BufferdImage
	 */
	public BufferedImage asImage() throws IOException {
		if(image != null) {
			return image;
		}
		if(data != null) {
			this.image = ImageIO.read(new ByteArrayInputStream(DatatypeConverter.parseBase64Binary(this.data)));
			return image;
		}
		return null;
	}

	public String getTitle() {
		return title;
	}

	public void setTitle(final String title) {
		this.title = title;
	}

	public String getComment() {
		return comment;
	}

	public void setComment(final String comment) {
		this.comment = comment;
	}

	public Format getFormat() {
		return format;
	}

	public void setFormat(final Format format) {
		this.format = format;
	}

	public Timestamp getTimestamp() {
		return timestamp;
	}

	public void setTimestamp(final Timestamp timestamp) {
		this.timestamp = timestamp;
	}

	public String getData() {
		return data;
	}

	public void setData(final String data) {
		this.data = data;
	}
	
	/**
	 * Serializes a provided BufferedImage to a base64 encoded string
	 * 
	 * @param img The image to serialize
	 * @param format The format of the image. Must be one of the Image.Format.*-constants
	 * @return The base64 representation of the given image
	 * @throws IOException If an exception occures during the serialization of the image
	 */
	public static String asEncodedString(BufferedImage img, Format format) throws IOException {
		if(img != null && format != null) {
			final ByteArrayOutputStream os = new ByteArrayOutputStream();
			ImageIO.write(img, format.toString(), os);
			return Base64.getEncoder().encodeToString(os.toByteArray());
		}
		return null;
	}

	private String title;
	private String comment;
	private Format format;
	private Timestamp timestamp;
	private String data;
	private transient BufferedImage image;
}