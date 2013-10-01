/*
 * Copyright 2013 The SCAPE Project Consortium
 * Author: William Palmer (William.Palmer@bl.uk)
 *
 *   Licensed under the Apache License, Version 2.0 (the "License");
 *   you may not use this file except in compliance with the License.
 *   You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing, software
 *   distributed under the License is distributed on an "AS IS" BASIS,
 *   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *   See the License for the specific language governing permissions and
 *   limitations under the License.
 */

package uk.bl.dpt.fclib;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.Authenticator;
import java.net.HttpURLConnection;
import java.net.PasswordAuthentication;
import java.net.URL;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.HashMap;
import java.util.Properties;

import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.xml.sax.SAXException;

import uk.bl.dpt.fclib.https.AbsoluteTrustManager;
import uk.bl.dpt.fclib.https.TotalHostnameVerifier;

/**
 * This class is used to retrieve remote files from a remote webdav repository
 * @author wpalmer
 */
public class FedoraDataConnector {

	private static boolean gDebug = true;
	
	static {
		InputStream props = Tools.getResource(FedoraDataConnector.class, "fedoraserver.properties");
		if(props!=null) {
			loadServerSettings(props);
		} else {
			//no properties file loaded
		}
	}

	//some default settings
	private static class FedoraSettings {
		public static String SERVER = "";
		public static int PORT = -42;
		public static String USER = "";
		public static String PASSWORD = "";
		public static String ROOT = "";
		public static String TRANSPORT = "http";
		private FedoraSettings() {}		
	}
	
	private static final class FedoraKeys {
		public static final String LABEL = "dsLabel";
		public static final String CHECKSUM = "dsChecksum";
		public static final String CHECKSUMTYPE = "dsChecksumType";
		public static final String SIZE = "dsSize";
		public static final String CONTROLGROUP = "dsControlGroup";
		
		private FedoraKeys() {}
	}
	
	private static class Auth extends Authenticator {
		public PasswordAuthentication getPasswordAuthentication() {
			return new PasswordAuthentication(FedoraSettings.USER,FedoraSettings.PASSWORD.toCharArray());
		}
	}
	
	private static HttpsURLConnection setupConnection(String pAddress, String pMethod) throws IOException {
		
		Authenticator.setDefault(new Auth());
		URL url = new URL(pAddress);

		boolean secure = (FedoraSettings.TRANSPORT.toUpperCase().equals("HTTPS"))?true:false;

		if(secure) {
			//i.e. we are secure
			SSLContext context = null;
			try {
				context = SSLContext.getInstance("SSL");
			} catch (NoSuchAlgorithmException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			try {
				context.init(null, new TrustManager[] { new AbsoluteTrustManager(gDebug) }, new SecureRandom());
			} catch (KeyManagementException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			//WARNING: this sets the static socket factory for ssl connections to accept any secure connection!
			HttpsURLConnection.setDefaultSSLSocketFactory(context.getSocketFactory());
		}
		
		HttpsURLConnection server = (HttpsURLConnection)url.openConnection();
		server.setRequestMethod(pMethod);
		server.setDoOutput(true);
		
		//WARNING: this allows all hostnames!
		server.setHostnameVerifier(new TotalHostnameVerifier(gDebug));
		
		return server;
	}
	
	/**
	 * Load server settings from an xml properties file in the jar
	 * @param pInputStream InputStream for settings
	 */
	public static void loadServerSettings(InputStream pInputStream) {
		Properties props = new Properties();
		try {
			props.loadFromXML(pInputStream);
			String key = "SERVER";
			if(props.containsKey(key)) {
				FedoraSettings.SERVER = props.getProperty(key);
			}
			key = "PORT";
			if(props.containsKey(key)) {
				FedoraSettings.PORT = new Integer(props.getProperty(key));
			}
			key = "USER";
			if(props.containsKey(key)) {
				FedoraSettings.USER = props.getProperty(key);
			}
			key = "PASSWORD";
			if(props.containsKey(key)) {
				FedoraSettings.PASSWORD = props.getProperty(key);
			}
			key = "ROOT";
			if(props.containsKey(key)) {
				FedoraSettings.ROOT = props.getProperty(key);
			}
			key = "TRANSPORT";
			if(props.containsKey(key)) {
				FedoraSettings.TRANSPORT = props.getProperty(key);
			}
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	/**
	 * URI to add to input files so they will be passed to the dataconnector
	 */
	public static final String DC_URI = "dataconnector://";
	
	private static String getFedoraURI(String pPid, String pDatastream) {
		String baseURI = "";
		baseURI += FedoraSettings.TRANSPORT+"://";
		baseURI += FedoraSettings.SERVER+":"+FedoraSettings.PORT+FedoraSettings.ROOT;
		baseURI += "objects/"+pPid+"/datastreams/"+pDatastream;
		return baseURI;
	}

	/**
	 * Parse an xml file to find the datastream label
	 * @param pPid 
	 * @param pDatastream
	 * @return
	 */
	private static HashMap<String,String> getDatastreamProperties(String pPid, String pDatastream) {
		//EXAMPLE: http://host:port/fedora/objects/pid:1/datastreams/DS0?format=xml
		String remoteURI = getFedoraURI(pPid, pDatastream)+"?format=xml";
		
		try {
			if(gDebug) System.out.print("Connecting... ");
			HttpURLConnection server = setupConnection(remoteURI, "GET");
			if(gDebug) System.out.println(server.toString());
			try {
				server.connect();
			} catch(Exception ioe) {
				ioe.printStackTrace();
				if(gDebug) System.out.println("Return code: "+server.getResponseCode()+" "+server.getURL());
			}
			if(gDebug) System.out.println("Return code: "+server.getResponseCode()+" "+server.getURL());
			
			//we should get an xml document with root node datastreamProfile
			//and a series of elements containing data
			
			DocumentBuilder docB = DocumentBuilderFactory.newInstance().newDocumentBuilder();
			Document doc = docB.parse(server.getInputStream());
			Node root = doc.getFirstChild();
			
			HashMap<String, String> values = new HashMap<String, String>();
			
			if(root.getNodeName().equals("datastreamProfile")) {
				Node node = root.getFirstChild();
				while(node!=null) {
					values.put(node.getNodeName(), node.getTextContent());
					node = node.getNextSibling();
				} 
			} else {
				return null;
			}
			
			//for(String key:values.keySet()) {
			//	System.out.println(key+": "+values.get(key));
			//}
			return values;
			
		} catch(ParserConfigurationException pce) {
			return null;
		} catch(IOException ioe) {
			return null;
		} catch(SAXException se) {
			return null;
		} 
	}
	
	/**
	 * Recover a datastream from a Fedora Commons repository
	 * @param pPid pid of object
	 * @param pDatastream datastream to recover
	 * @param pLocalDir directory to put file when recovered
	 * @return File object for recovered file, or null
	 */
	public static File recoverDatastream(String pPid, String pDatastream, String pLocalDir) {
		String remoteURI = getFedoraURI(pPid, pDatastream)+"/content";
		if(!pLocalDir.endsWith("/")) {
			pLocalDir+="/";
		}		
		
		//recover datastream properties
		HashMap<String, String> properties = getDatastreamProperties(pPid, pDatastream);
		if(gDebug) for(String k:properties.keySet()) System.out.println(k+": "+properties.get(k));
		String localFile = pLocalDir+properties.get(FedoraKeys.LABEL);

		try {
			//recover the datastream
			HttpURLConnection server = setupConnection(remoteURI, "GET");
			server.connect();
			System.out.println("Return code: "+server.getResponseCode()+" "+server.getURL());
			
			System.out.println("Copying: "+remoteURI+" -> "+localFile);

			BufferedInputStream fis = new BufferedInputStream(server.getInputStream());
			FileOutputStream fos = new FileOutputStream(localFile);
			byte[] buffer = new byte[32768];
			int bytesRead = 0;
			long count = 0;
			long size = new Long(properties.get(FedoraKeys.SIZE));
			
			String dsType = properties.get(FedoraKeys.CONTROLGROUP).toUpperCase(); 
			
			long startTime = System.currentTimeMillis();
			
			//if this is a managed data stream we get the size
			//it shouldn't be zero, but if it is then try the other copy
			if(dsType.equals("M")&size!=0) {
				if(gDebug) System.out.println("[Managed] Size: "+size);
				//note: fis.isAvailable() does not work here (returns false before end of stream)
				while(count<size) {
					bytesRead = fis.read(buffer);
					fos.write(buffer, 0, bytesRead);
					count+=bytesRead;
				}
				fis.close();
				fos.close();
			} else {
				if(dsType.equals("E")) {
					//note: fis.isAvailable() does not work here (returns false before end of stream)
					do {
						bytesRead = fis.read(buffer);
						if(bytesRead>-1) {
							fos.write(buffer, 0, bytesRead);
							count+=bytesRead;
						}
					} while(bytesRead>-1);
					fis.close();
					fos.close();
					if(gDebug) System.out.println("[Referenced] data transferred: "+count);
				}
			}
			System.out.println("Copied ["+count+"] bytes in ["+(System.currentTimeMillis()-startTime)+"] ms");


			//checksum the file 
			String localChecksum = Tools.generateChecksum(properties.get(FedoraKeys.CHECKSUMTYPE), localFile);
			String remoteChecksum = properties.get(FedoraKeys.CHECKSUM);
			System.out.println("Checksums: remote: "+remoteChecksum);			
			System.out.println("Checksums: local: "+localChecksum);			
			if(localChecksum==null||!localChecksum.equals(remoteChecksum)) {
				System.out.println("WARNING: "+properties.get(FedoraKeys.CHECKSUMTYPE)+" checksum error; remote: "+remoteChecksum+", local: "+localChecksum);
				throw new IOException(properties.get(FedoraKeys.CHECKSUMTYPE)+" checksum error; remote: "+remoteChecksum+", local: "+localChecksum);
			} else {
				System.out.println("Checksums ok");
			}
			
			return new File(localFile);

		} catch(IOException e) {
			e.printStackTrace();
			return null;
		}
	}
	
	/**
	 * Upload a new datastream to Fedora from a local file
	 * @param pPid pid to use
	 * @param pDatastream datastream to upload file to
	 * @param pLocalFile local file containing data to upload
	 * @param pLogMessage log message (no spaces(?))
	 * @param pMimeType mimetype of the local file
	 * @return true or false
	 */
	public static boolean postDatastream(String pPid, String pDatastream, File pLocalFile, String pLogMessage, String pMimeType) {
		//we can use addDatastream or modifyDatastream (both take same arguments?)
		String remoteURI = getFedoraURI(pPid, pDatastream);

		if(pLogMessage.contains(" "))
			pLogMessage = pLogMessage.replaceAll(" ", "");

		String checksumType = "MD5";
		String checksum = null;
		try {
			//maybe pass this to this method as a parameter if already generated?
			checksum = Tools.generateChecksum(checksumType, pLocalFile.getAbsolutePath());
		} catch(IOException e) {
			return false;
		}
		//add parameters to URI
		remoteURI+="?controlGroup=M&logMessage="+pLogMessage+"&mimeType="+pMimeType+"&checksumType="+checksumType+"&checksum="+checksum;

		//we need to see if we are adding the data to a new datastream or not
		HashMap<String, String> dsprop = getDatastreamProperties(pPid, pDatastream);
		if(dsprop==null||dsprop.get(FedoraKeys.LABEL)==null) {
			//i.e. this datastream does not currently exist (or we had some other problem asking the server for info)
			remoteURI+="&dsLabel="+pLocalFile.getName();
		}
		
		System.out.println("Uploading: "+pLocalFile.getAbsolutePath()+" -> "+remoteURI);

		//upload multipart file
		try {
			HttpURLConnection server = setupConnection(remoteURI, "POST");
			BufferedInputStream fis = new BufferedInputStream(new FileInputStream(pLocalFile));
			BufferedOutputStream fos = new BufferedOutputStream(server.getOutputStream());
			byte[] buffer = new byte[32768];
			int bytesRead = 0;
			long count = 0;
			long size = pLocalFile.length();
			
			long startTime = System.currentTimeMillis();
			
			//note: fis.isAvailable() does not work here (returns false before end of stream)
			while(count<size) {
				bytesRead = fis.read(buffer);
				fos.write(buffer, 0, bytesRead);
				count+=bytesRead;
			}
			fis.close();
			fos.close();
			
			System.out.println("Copied ["+count+"] bytes in ["+(System.currentTimeMillis()-startTime)+"] ms");
			
			server.connect();
			System.out.println("Return code: "+server.getResponseCode()+" "+server.getURL());
			
		} catch(IOException e) {
			e.printStackTrace();
			return false;
		}
		
		//check the checksum
		try {
			//recover datastream properties
			HashMap<String, String> properties = getDatastreamProperties(pPid, pDatastream);
			//checksum the file 
			String localChecksum = Tools.generateChecksum(properties.get(FedoraKeys.CHECKSUMTYPE), pLocalFile.getAbsolutePath());
			String remoteChecksum = properties.get(FedoraKeys.CHECKSUM);
			System.out.println("Checksums: remote: "+remoteChecksum);			
			System.out.println("Checksums: local: "+localChecksum);	
			if(!localChecksum.equals(remoteChecksum)) {
				System.out.println("WARNING: "+properties.get(FedoraKeys.CHECKSUMTYPE)+" checksum error; remote: "+remoteChecksum+", local: "+localChecksum);
				throw new IOException(properties.get(FedoraKeys.CHECKSUMTYPE)+" checksum error; remote: "+remoteChecksum+", local: "+localChecksum);
			} else {
				System.out.println("Checksums ok");
			}	
		} catch(IOException e) {
			e.printStackTrace();
			return false;
		}

		return true;

	}
	
	/**
	 * Test main method
	 * @param args command line arguments
	 */
	public static void main(String[] args) {

	}

}
