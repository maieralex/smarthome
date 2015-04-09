/**
 * Copyright (c) 2010-2014, openHAB.org and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.smarthome.binding.digitalstrom.internal.client.connection.transport;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.SocketTimeoutException;


import java.net.URL;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.Security;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSession;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import javax.security.cert.CertificateException;
import javax.security.cert.X509Certificate;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Alexander Betker
 * @author Alex Maier
 * @since 1.3.0
 */
public class HttpTransport {
	
	private static final Logger logger = LoggerFactory.getLogger(HttpTransport.class);
	
	private final String uri;
	
	private final int connectTimeout;
	private final int readTimeout;

	
	public HttpTransport(String uri, int connectTimeout, int readTimeout) {
		if(!uri.startsWith("https://")) uri = "https://" + uri;
		if(!uri.endsWith(":8080")) uri = uri + ":8080";
		this.uri = uri;
		this.connectTimeout = connectTimeout;
		this.readTimeout = readTimeout;
		
		//check SSL certificate is installated
		try {
			URL url = new URL(uri);
			HttpsURLConnection connection = (HttpsURLConnection) url.openConnection();
			connection.connect();
			connection.disconnect();
		} catch (MalformedURLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			setupHttpsConnection();
		}
		
	}
	
	public String execute(String request) {
		return execute(request, this.connectTimeout, this.readTimeout );
	}
	
	public String execute(String request, int connectTimeout, int readTimeout) {
		if (request != null && !request.trim().equals("")) {
			
			HttpsURLConnection connection = null;
			
			StringBuilder response = new StringBuilder();
			BufferedReader in = null;
			try {
				URL url = new URL(this.uri+request);
				
				connection = (HttpsURLConnection) url.openConnection();
				int responseCode =-1;
				if (connection != null) {
					connection.setConnectTimeout(connectTimeout);
					connection.setReadTimeout(readTimeout);
					
				
					try {
						connection.connect();
						responseCode = connection.getResponseCode();
					} catch (SocketTimeoutException e) {
						logger.warn(e.getMessage()+" : "+request);
						return null;
					}
					
					if (responseCode == HttpURLConnection.HTTP_OK) {
						String inputLine = null;
						in = new BufferedReader(new InputStreamReader(connection.getInputStream()));
	        
						while ((inputLine = in.readLine()) != null) {
							response.append(inputLine);
						}
						
						in.close();
					}
					else {
						response = null;
					}
				
				}
				if (response != null) {
					return response.toString();
				}

			} catch (MalformedURLException e) {
				logger.error("MalformedURLException by executing jsonRequest: "
						+ request +" ; "+e.getLocalizedMessage());

			} catch (IOException e) {
				logger.error("IOException by executing jsonRequest: "
						+ request +" ; "+e.getLocalizedMessage());							
			}
			finally{
				if (connection != null)
					connection.disconnect();
			}
		}
		return null;
	}
	
	public int checkConnection(String testRequest) {
		if (testRequest != null && !testRequest.trim().equals("")) {
			
			HttpURLConnection connection = null;
			
			try {
				URL url = new URL(this.uri+testRequest);
				
				connection = (HttpsURLConnection) url.openConnection();

				if (connection != null) {
					connection.setConnectTimeout(connectTimeout);
					connection.setReadTimeout(readTimeout);
				
					try {
						connection.connect();
						return connection.getResponseCode();
					} catch (SocketTimeoutException e) {
						logger.warn(e.getMessage()+" : "+testRequest);
						return -1;
					}			
				}

			} catch (MalformedURLException e) {
				logger.error("MalformedURLException by executing jsonRequest: "
						+ testRequest +" ; "+e.getLocalizedMessage());
				return -2;

			} catch (IOException e) {
				logger.error("IOException by executing jsonRequest: "
						+ testRequest +" ; "+e.getLocalizedMessage());							
			}
			finally{
				if (connection != null)
					connection.disconnect();
			}
		}
		return -1;
	}
	
	private void setupHttpsConnection(){
		Security.addProvider(Security.getProvider("SunJCE"));
        TrustManager[] trustAllCerts = new TrustManager[]{
            new X509TrustManager() {
               
                public void checkServerTrusted(X509Certificate[] certs, String authType) throws CertificateException {
                    return;
                }

                public void checkClientTrusted(X509Certificate[] certs, String authType) throws CertificateException {
                    return;
                }

				@Override
				public void checkClientTrusted(
						java.security.cert.X509Certificate[] chain,
						String authType)
						throws java.security.cert.CertificateException {
					// TODO Auto-generated method stub
					
				}

				@Override
				public void checkServerTrusted(
						java.security.cert.X509Certificate[] chain,
						String authType)
						throws java.security.cert.CertificateException {
					// TODO Auto-generated method stub
					
				}

				@Override
				public java.security.cert.X509Certificate[] getAcceptedIssuers() {
					// TODO Auto-generated method stub
					return null;
				}
            }
        };

        HostnameVerifier allHostValid = new HostnameVerifier(){

			@Override
			public boolean verify(String hostname, SSLSession session) {
				// TODO Auto-generated method stub
				return true;
			}
			
		};
		
		try {
			SSLContext sslContext = SSLContext.getInstance("SSL");			
      
			sslContext.init(null, trustAllCerts, new SecureRandom());
			
			HttpsURLConnection.setDefaultSSLSocketFactory(sslContext.getSocketFactory());
			HttpsURLConnection.setDefaultHostnameVerifier(allHostValid);
		} catch (KeyManagementException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (NoSuchAlgorithmException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
}