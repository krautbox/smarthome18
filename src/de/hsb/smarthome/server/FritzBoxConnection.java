package de.hsb.smarthome.server;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.StringReader;
import java.net.URL;
import java.security.KeyManagementException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.List;

import javax.net.ssl.HttpsURLConnection;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import de.hsb.smarthome.util.json.Device;
import de.hsb.smarthome.util.json.Device.Type;
import de.hsb.smarthome.util.log.Logger;
import de.hsb.smarthome.util.log.Logger.LoggerMode;

/**
 * This class tries an HTTPS_CONNection to the FRITZ!Box and various operations
 * can be performed, such as turn the Socket ON, OFF etc.
 * 
 * @author Coulibaly, Ben Inza
 *
 */
public class FritzBoxConnection {

	/**
	 * Get a valid Session-ID to prove your Identity and use that to command the
	 * FRITZ!Box components.
	 * 
	 * @param remote_gateway
	 * @param username
	 * @param password
	 * @return session id (FRITZ!Box)
	 * @throws IOException
	 * @throws ParserConfigurationException
	 * @throws SAXException
	 * @throws NoSuchAlgorithmException
	 * @throws KeyManagementException
	 */
	public static String getSessionId(String remote_gateway, String username, String password) throws IOException,
			ParserConfigurationException, SAXException, NoSuchAlgorithmException, KeyManagementException {
		url = new URL("https", remote_gateway, "/login_sid.lua");
		HTTPS_CONN = TrustCertificate.trustCertificate(url);
		HTTPS_CONN = (HttpsURLConnection) url.openConnection();
		String webSite = getWebsiteInString();
		doc = convertStringToDoc(webSite);
		String sid = getValue(doc, "SID");

		if ("0000000000000000".equals(sid)) {
			String challenge = getValue(doc, "Challenge");
			String response = getResponse(challenge, password);
			url = new URL("https", remote_gateway, "/login_sid.lua?username=" + username + "&response=" + response);
			webSite = getWebsiteInString();
			doc = convertStringToDoc(webSite);
			sid = getValue(doc, "SID");
			logger.write(new FritzBoxConnection(), "SID = " + sid, LoggerMode.INFO);
		}

		return sid;
	}

	/**
	 * Reads any web page, saves it to a string and returns it.
	 * 
	 * @return web page as String
	 * @throws IOException
	 */
	private static String getWebsiteInString() throws IOException {
		HTTPS_CONN = (HttpsURLConnection) url.openConnection();
		BufferedReader in = new BufferedReader(new InputStreamReader(HTTPS_CONN.getInputStream()));
		String inputLine;
		StringBuffer respon = new StringBuffer();

		while ((inputLine = in.readLine()) != null) {
			respon.append(inputLine);
		}
		in.close();
		inputLine = respon.toString();
		return inputLine;
	}

	/**
	 * Build a string into a DOM document and return it.
	 * 
	 * @param str
	 * @return DOM document
	 * @throws ParserConfigurationException
	 * @throws SAXException
	 * @throws IOException
	 */
	private static Document convertStringToDoc(String str)
			throws ParserConfigurationException, SAXException, IOException {

		DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
		DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
		doc = dBuilder.parse(new InputSource(new StringReader(str)));
		doc.normalizeDocument();

		return doc;
	}

	/**
	 * This method returns a HASH-CODE>250 in string.
	 * 
	 * @param input
	 * @return MD5 hash
	 * @throws NoSuchAlgorithmException
	 * @throws IOException
	 */
	private static String getMD5Hash(String input) throws NoSuchAlgorithmException, IOException {
		MessageDigest md5 = MessageDigest.getInstance("MD5");
		byte[] data = md5.digest(input.getBytes("UTF-16LE"));

		StringBuffer sb = new StringBuffer();
		for (int i = 0; i < data.length; i++) {
			sb.append(Integer.toHexString((data[i] & 0xFF) | 0x100).substring(1, 3));
		}
		return sb.toString();
	}

	/**
	 * Get the value of any element in a DOM document
	 * 
	 * @param doc
	 * @param name
	 * @return element value
	 */
	private static String getValue(Document doc, String name) {
		String info;
		info = doc.getElementsByTagName(name).item(0).getTextContent();
		return info;
	}

	/**
	 * Get a valid response value for the HTTPS_CONN to FRITZ!Box and get a valid
	 * SID.
	 * 
	 * @param challenge
	 * @param password
	 * @return response value
	 * @throws NoSuchAlgorithmException
	 * @throws IOException
	 */
	private static String getResponse(String challenge, String password) throws NoSuchAlgorithmException, IOException {
		String getMD5 = getMD5Hash(challenge + "-" + password);
		String responseValue = challenge + "-" + getMD5;
		return responseValue;
	}

	/**
	 * To command the FRITZ!Box component, you need the AIN_Number of the Socket.
	 * 
	 * @param remote_gateway
	 * @param ain
	 * @param cmd
	 * @param sid
	 * @return
	 * @throws IOException
	 * @throws ParserConfigurationException
	 * @throws SAXException
	 */
	public static String sendDeviceCommand(String remote_gateway, String ain, String cmd, String sid)
			throws IOException, ParserConfigurationException, SAXException {
		String str = "";
		url = new URL("https", remote_gateway,
				"/webservices/homeautoswitch.lua?ain=" + ain + "&switchcmd=" + cmd + "&sid=" + sid);
		str = getWebsiteInString();
		logger.write(new FritzBoxConnection(), cmd + " = " + str, LoggerMode.INFO);

		return str;
	}

	/**
	 * This methode returns a list of all connected devices with the FRITZ!Box.
	 * 
	 * @param remote_gateway
	 * @param sid
	 * @return
	 * @throws IOException
	 * @throws ParserConfigurationException
	 * @throws SAXException
	 */
	private static List<Device> setDeviceListInfos(String remote_gateway, String sid)
			throws IOException, ParserConfigurationException, SAXException {
		url = new URL("https", remote_gateway,
				"/webservices/homeautoswitch.lua?switchcmd=getdevicelistinfos&sid=" + sid);

		String s = getWebsiteInString();
		doc = convertStringToDoc(s);
		Device tmp = new Device();

		NodeList rootDevices = doc.getElementsByTagName("device");

		for (int i = 0; i < rootDevices.getLength(); ++i) {
			NodeList deviceChilds = rootDevices.item(i).getChildNodes();

			tmp.setAid(((Element) rootDevices.item(i)).getAttribute("identifier").toString());
			tmp.setTemperature(Float.parseFloat(deviceChilds.item(4).getChildNodes().item(0).getTextContent()) / 10);
			tmp.setPower(Float.parseFloat(deviceChilds.item(3).getChildNodes().item(0).getTextContent()));
			tmp.setEnergy(Float.parseFloat(deviceChilds.item(3).getChildNodes().item(1).getTextContent()));
			tmp.setStatus(Integer.parseInt(deviceChilds.item(2).getChildNodes().item(0).getTextContent()));
			tmp.setId(i + 1);
			tmp.setConnected(true);
			tmp.setType(Type.SOCKET);
			tmp.setName(getValue(doc, "name"));
			myList.add(tmp);
			logger.write(new FritzBoxConnection(), myList.toString(), LoggerMode.INFO);
		}

		return myList;
	}

	/**
	 * This methode returns a list of all connected devices with the FRITZ!Box.
	 * 
	 * @param remote_gateway
	 * @param sid
	 * @return
	 * @throws IOException
	 * @throws ParserConfigurationException
	 * @throws SAXException
	 */
	public static List<Device> getDeviceListInfos(String remote_gateway, String sid)
			throws IOException, ParserConfigurationException, SAXException {
		return myList = setDeviceListInfos(remote_gateway, sid);
	}

	/**
	 * Log-out successfull from the FRITZ!Box.
	 * 
	 * @param remote_gateway
	 * @param sid
	 * @throws IOException
	 */
	public static void logout(String remote_gateway, String sid) throws IOException {
		url = new URL("https", remote_gateway, "/login.lua?page=/home/home.lua&logout=1&sid=" + sid);
		HTTPS_CONN = (HttpsURLConnection) url.openConnection();
		int responseCode = HTTPS_CONN.getResponseCode();

		if (responseCode == 200)
			logger.write(new FritzBoxConnection(), "log-out successfull with responseCode = " + responseCode,
					LoggerMode.INFO);
		else
			logger.write(new FritzBoxConnection(), "log-out failled with responseeCode = " + responseCode,
					LoggerMode.INFO);
	}

	private static HttpsURLConnection HTTPS_CONN;
	private static URL url;
	private static List<Device> myList = new ArrayList<>();
	private final static Logger logger = Logger.getLogger();
	private static Document doc;
}