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
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Properties;

import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;

import uk.bl.dpt.fclib.https.AbsoluteTrustManager;
import uk.bl.dpt.fclib.https.TotalHostnameVerifier;

import org.apache.http.HttpHost;
import org.apache.http.HttpRequest;
import org.apache.http.HttpResponse;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.Credentials;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.conn.scheme.Scheme;
import org.apache.http.conn.scheme.SchemeRegistry;
import org.apache.http.conn.ssl.SSLSocketFactory;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.impl.conn.PoolingClientConnectionManager;
import org.apache.http.message.BasicHttpRequest;

/**
 * This class is used to retrieve remote files from a remote webdav repository
 * @author wpalmer
 */
public class WebdavDataConnector {

	private static boolean gDebug = false;

	static {
		InputStream propsGet = Tools.getResource(WebdavDataConnector.class, "webdavserver-get.properties");
		InputStream propsPut = Tools.getResource(WebdavDataConnector.class, "webdavserver-put.properties");
		if(propsGet!=null) { gWebdavSettingsGet = loadServerSettings(propsGet); }
		if(propsPut!=null) { gWebdavSettingsPut = loadServerSettings(propsPut); }
			//no properties file loaded
	}

	/**
	 * Settings for the webdav connection
	 */
	private static class Settings {
		public String SERVER = null;
		public int PORT = -42;
		public String USER = null;
		public String PASSWORD = null;
		public String ROOT = null;
		public String TRANSPORT = null;
		
		public String toString() {
			return TRANSPORT+"://"+USER+":"+PASSWORD+"@"+SERVER+":"+PORT+ROOT;
		}
		public Settings() {}
	}
	
	private static class Auth extends Authenticator {
		private Settings gSettings;
		public Auth(Settings pSettings) {
			gSettings = pSettings;
		}
		public PasswordAuthentication getPasswordAuthentication() {
			return new PasswordAuthentication(gSettings.USER,gSettings.PASSWORD.toCharArray());
		}
	}
	
	private static Settings gWebdavSettingsGet;
	private static Settings gWebdavSettingsPut;	

	/**
	 * Load server settings from an xml properties file in the jar
	 * @param pInputStream InputStream for settings
	 * @return the loaded Settings object
	 */
	public static Settings loadServerSettings(InputStream pInputStream) {
		Settings settings = new Settings();
		Properties props = new Properties();
		try {
			props.loadFromXML(pInputStream);
			String key = "SERVER";
			if(props.containsKey(key)) {
				settings.SERVER = props.getProperty(key);
			}
			key = "PORT";
			if(props.containsKey(key)) {
				settings.PORT = new Integer(props.getProperty(key));
			}
			key = "USER";
			if(props.containsKey(key)) {
				settings.USER = props.getProperty(key);
			}
			key = "PASSWORD";
			if(props.containsKey(key)) {
				settings.PASSWORD = props.getProperty(key);
			}
			key = "ROOT";
			if(props.containsKey(key)) {
				settings.ROOT = props.getProperty(key);
			}
			key = "TRANSPORT";
			if(props.containsKey(key)) {
				settings.TRANSPORT = props.getProperty(key);
			}
			return settings;
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return null;
	}
	
	/**
	 * URI to add to input files so they will be passed to the dataconnector
	 */
	public static final String DC_URI = "webdav://";
	
	private static String getHTTPFileURI(Settings pSettings, String pFile) {
		//http uri: http://host:port[absolute path]
		String baseURI = "";
		baseURI += pSettings.TRANSPORT+"://";
		baseURI += pSettings.SERVER+":"+pSettings.PORT+pSettings.ROOT;
		baseURI += pFile;
		return baseURI;
	}

	private static HttpsURLConnection setupConnection(Settings pSettings, String pRemoteURI, String pMethod) throws IOException {
		
		Authenticator.setDefault(new Auth(pSettings));
		URL url = new URL(pRemoteURI);

		boolean secure = (pSettings.TRANSPORT.toUpperCase().equals("HTTPS"))?true:false;

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
	 * Recover a file from a remote location and copy it into localDir
	 * @param pRemoteFile file to recover (from a webdav repository)
	 * @param pLocalDir directory to copy file to
	 * @return File for the newly copied file, null if not copied
	 */
	public static File recoverFile(String pRemoteFile, String pLocalDir) {
		if(!pLocalDir.endsWith("/")) {
			pLocalDir+="/";
		}		
		String localFile = pLocalDir+new File(pRemoteFile).getName();

		String remoteURI = getHTTPFileURI(gWebdavSettingsGet, pRemoteFile);
		
		HttpsURLConnection conn = null;
		try {
			conn = setupConnection(gWebdavSettingsGet, remoteURI, "GET");
			conn.connect();
		} catch (IOException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}

		long startTime = System.currentTimeMillis();

		System.out.println("Copying: "+conn.getURL()+" -> "+localFile);

		long size = new Long(conn.getHeaderField("Content-Length"));
		String lastModified = conn.getHeaderField("Last-Modified");

		//e.g. Tue, 10 Oct 2006 07:07:02 GMT
		SimpleDateFormat sdf = new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss zzz");
		Date date = null;
		try {
			date = sdf.parse(lastModified);
		} catch (ParseException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		long count = 0;
		try {
			BufferedInputStream fis = new BufferedInputStream(conn.getInputStream());
			FileOutputStream fos = new FileOutputStream(localFile);
			byte[] buffer = new byte[32768];
			int bytesRead = 0;
			while(count<size) {
				bytesRead = fis.read(buffer);
				fos.write(buffer, 0, bytesRead);
				count += bytesRead;
			}
			fis.close();
			fos.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		File outputFile = new File(localFile);
		outputFile.setLastModified(date.getTime());

		System.out.println("Copied ["+count+"] bytes in ["+(System.currentTimeMillis()-startTime)+"] ms");
		return outputFile;

	}	
	
	/**
	 * This method implements MKCOL over Apache HttpClient/HttpCore 
	 * @param pSettings
	 * @param pRemotePath
	 */
	private static void mkdirs(Settings pSettings, String pRemotePath)  {
		System.err.println("mkdirs() is very very slow");

		//http://hc.apache.org/httpcomponents-client-ga/tutorial/html/connmgmt.html
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
		final SSLSocketFactory sslFactory = new SSLSocketFactory(context, SSLSocketFactory.ALLOW_ALL_HOSTNAME_VERIFIER);

		final Scheme httpsScheme = new Scheme(pSettings.TRANSPORT, pSettings.PORT, sslFactory);
		SchemeRegistry schemeRegistry = new SchemeRegistry();
		schemeRegistry.register(httpsScheme);

		PoolingClientConnectionManager connManager = new PoolingClientConnectionManager(schemeRegistry);
		//set this to a silly high value
		int maxConnectionsPerHost = 5000;
		connManager.setMaxTotal(maxConnectionsPerHost);
		connManager.setDefaultMaxPerRoute(maxConnectionsPerHost);

		DefaultHttpClient client = new DefaultHttpClient(connManager);

		Credentials creds = new UsernamePasswordCredentials(pSettings.USER, pSettings.PASSWORD);
		client.getCredentialsProvider().setCredentials(AuthScope.ANY, creds);

		HttpHost host = new HttpHost(pSettings.SERVER, pSettings.PORT, pSettings.TRANSPORT);
		
		String mkdirPath = "";

		long startTime = 0;
		
		for(String dir:pRemotePath.split("/")) {

			if(dir.equals("")) continue;
			
			mkdirPath += dir+"/";

			System.out.print("Creating: "+mkdirPath+" ");
			
			startTime = System.currentTimeMillis();
			
			HttpRequest request = new BasicHttpRequest("MKCOL", pSettings.ROOT+"/"+mkdirPath);

			@SuppressWarnings("unused")
			HttpResponse response = null;
			
			try {
				response = client.execute(host, request);
			} catch (ClientProtocolException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}

			System.out.println("in "+(System.currentTimeMillis()-startTime)+"ms");
		}

	}

	/**
	 * Post a file to the webdav repository (not tested)
	 * @param pLocalFile local file to copy (full path)
	 * @param pRepositoryPath path in webdav repo to post file to
	 * @param pOverwrite whether or not to overwrite an existing file
	 * @return success boolean
	 */
	public static boolean postFile(File pLocalFile, String pRepositoryPath, boolean pOverwrite) {

		Settings pSettings = gWebdavSettingsPut;
		
		boolean success = false;
		
		if(!pRepositoryPath.endsWith("/")) pRepositoryPath += "/";
		
		if(!pLocalFile.exists()) return false;
		
		//WARNING: VERY SLOW
		mkdirs(pSettings, pRepositoryPath);
		
		String remotePath = getHTTPFileURI(pSettings, pRepositoryPath)+pLocalFile.getName(); 
		
		//upload multipart file
		try {
			HttpURLConnection server = setupConnection(pSettings, remotePath, "PUT");
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
			
			server.connect();
					
			System.out.println("Return code: "+server.getResponseCode()+" "+server.getURL());
			System.out.println("Copied ["+count+"] bytes in ["+(System.currentTimeMillis()-startTime)+"] ms");
			
			success = (HttpURLConnection.HTTP_CREATED==server.getResponseCode());


		} catch(IOException e) {
			e.printStackTrace();
			return false;
		}
		
		return success;

	}
	
	/**
	 * Test main method
	 * @param args command line arguments
	 */
	public static void main(String[] args) {

	}

}
