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

import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;

import javax.net.ssl.X509TrustManager;

/**
 * An empty AbsoluteTrustManager that trusts anything
 * @author wpalmer
 *
 */
public class AbsoluteTrustManager implements X509TrustManager {

	private boolean gDebug = false;
	
	/**
	 * Initialise
	 * @param pDebug whether or not to print debug messages
	 */
	public AbsoluteTrustManager(boolean pDebug) {
		// TODO Auto-generated constructor stub
		gDebug = pDebug;
		if(gDebug) System.out.println("AbsoluteTrustManager()");
	}

	@Override
	public void checkClientTrusted(X509Certificate[] arg0, String arg1)
			throws CertificateException {
		// TODO Auto-generated method stub
		if(gDebug) System.out.println("checkClientTrusted() "+arg1);
	}

	@Override
	public void checkServerTrusted(X509Certificate[] arg0, String arg1)
			throws CertificateException {
		// TODO Auto-generated method stub
		if(gDebug) System.out.println("checkServerTrusted() "+arg1);
	}

	@Override
	public X509Certificate[] getAcceptedIssuers() {
		// TODO Auto-generated method stub
		if(gDebug) System.out.println("getAcceptedIssuers()");

		return null;
	}

}
