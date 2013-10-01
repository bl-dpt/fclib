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

package uk.bl.dpt.fclib.https;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.SSLSession;

/**
 * Implement a TotalHostnameVerified that trusts anything
 * @author wpalmer
 *
 */
public class TotalHostnameVerifier implements HostnameVerifier {

	boolean gDebug = false;
	
	/**
	 * Initialise
	 * @param pDebug whether or not to print debug messages
	 */
	public TotalHostnameVerifier(boolean pDebug) {
		// TODO Auto-generated constructor stub
		gDebug = pDebug;
		if(gDebug) System.out.println("TotalHostnameVerifier()");
	}

	@Override
	public boolean verify(String arg0, SSLSession arg1) {
		// TODO Auto-generated method stub
		
		if(gDebug) System.out.println("TotalHostnameVerifier.verify("+arg0+", "+arg1+")");
		//always return true - we just want to ensure SSL
		return true;
	}

}
