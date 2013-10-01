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

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;

import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.xml.sax.SAXException;

/**
 * This class collects useful methods to be shared between classes
 * @author wpalmer
 *
 */
public class Tools {

	/**
	 * Generates a checksum for a file 
	 * @param pType type of checksum to run
	 * @param pInFile file to checksum
	 * @return Checksum
	 * @throws IOException file access error
	 */
	public static String generateChecksum(String pType, String pInFile) throws IOException {

		if(!new File(pInFile).exists()) throw new IOException("File not found: "+pInFile);
		
		MessageDigest md;
		try {
			md = MessageDigest.getInstance(pType.toUpperCase());
		} catch (NoSuchAlgorithmException e) {
			return null;
		}
		
		FileInputStream input;
		try {
			input = new FileInputStream(pInFile);
			byte[] readBuffer = new byte[32768];
			int bytesRead = 0;
			while(input.available()>0) {
				bytesRead = input.read(readBuffer);
				md.update(readBuffer, 0, bytesRead);
			}
			input.close();
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}

		String hash = "";
		for(byte b : md.digest()) hash+=String.format("%02x", b);
		return hash;
	}	
	
	/**
	 * Gets an InputStream for a resource from a jar
	 * @param pClass Class reference
	 * @param pRes resource path in jar
	 * @return InputStream for resource
	 */
	public static InputStream getResource(Class<?> pClass, String pRes) {
		return pClass.getClassLoader().getResourceAsStream(pRes);
	}
	
	/**
	 * Recover the value associated with a xpath expression
	 * @param pInputStream xml file
	 * @param pXPath XPath expression to evaluate
	 * @return value value associated with the xpath expression (or null if error)
	 */
	public static String getXpathVal(InputStream pInputStream, String pXPath) {
		try {
			DocumentBuilder docB = DocumentBuilderFactory.newInstance().newDocumentBuilder();
			Document doc = docB.parse(pInputStream);
			Node root = doc.getFirstChild();		
			XPath xpath = XPathFactory.newInstance().newXPath();
			return xpath.evaluate(pXPath, root);
		} catch (ParserConfigurationException pce) {
		} catch (NumberFormatException e) {
		} catch (XPathExpressionException e) {
		} catch (SAXException e) {
		} catch (IOException e) {
		}
		return null;
	}
	
}
