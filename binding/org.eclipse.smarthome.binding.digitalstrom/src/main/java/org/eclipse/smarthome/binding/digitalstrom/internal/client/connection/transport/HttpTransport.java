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
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.SocketTimeoutException;
import java.net.URL;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.Security;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSession;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

import org.eclipse.smarthome.binding.digitalstrom.DigitalSTROMBindingConstants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Alexander Betker
 * @author Alex Maier
 * @since 1.3.0
 */
public class HttpTransport {
	
	//Exceptions:
	//Messages
	private final String NO_CERT_AT_PATH = "Missing input stream";
	private final String CERT_EXCEPTION = "java.security.cert.CertificateException";
	//sun.security.validator.ValidatorException: PKIX path building failed: sun.security.provider.certpath.SunCertPathBuilderException: unable to find valid certification path to requested target
	private final String PKIX_PATH_FAILD = "PKIX path building failed";
	private final String HOSTNAME_VERIFIER = "No name matching";
	private final String UNKOWN_HOST = "unknownHost";
	
	private static final Logger logger = LoggerFactory.getLogger(HttpTransport.class);
	
	private final String uri;
	private String trustedCertPath;
	private X509Certificate	trustedCert;
	private InputStream certInputStream;
	
	private final int connectTimeout;
	private final int readTimeout;

		
	public HttpTransport(String uri, int connectTimeout, int readTimeout) {
		if(!uri.startsWith("https://")) uri = "https://" + uri;
		if(!uri.endsWith(":8080")) uri = uri + ":8080";
		this.uri = uri;
		this.connectTimeout = connectTimeout;
		this.readTimeout = readTimeout;
		
		//Check SSL Certificate
		if(DigitalSTROMBindingConstants.TRUST_CERT_PATH != null){			
			logger.info("Certification path is set, generate the certification and accept it.");
			trustedCertPath = DigitalSTROMBindingConstants.TRUST_CERT_PATH;
			
			File dssCert = new File(trustedCertPath);
			if(dssCert.isAbsolute()){
				try {
					certInputStream = new FileInputStream(dssCert);
				} catch (FileNotFoundException e) {
					// TODO Auto-generated catch block
					logger.error("Can't find a certificationfile at the certificationpath: " + trustedCertPath + 
							"\nPlease check the path!");
				}
			} else{
				certInputStream = HttpTransport.class.getClassLoader().getResourceAsStream(trustedCertPath);
			}
			setupWithCertPath();
			String conCheck = this.checkConnection();
			if(conCheck != null && conCheck.contains(CERT_EXCEPTION)){
				logger.error("Invalid certification at path " + this.trustedCertPath);
				
				
			}
			
		}
		
		checkSSLCert();
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
	
	
	/****SSL Check****/
	
	private void checkSSLCert(){
		logger.info("Check SSL Certificate");
		String conCheck = checkConnection();
		if(conCheck != null){
			switch(conCheck){
			case CERT_EXCEPTION:
				logger.info("No certification installated");
				trustedCertPath = DigitalSTROMBindingConstants.TRUST_CERT_PATH; 
				if(trustedCertPath != null && !trustedCertPath.isEmpty()){
					logger.info("Certification path is set, generate the certification and accept it.");
					setupWithCertPath();
					conCheck = checkConnection();
					if(conCheck != null && conCheck.contains(CERT_EXCEPTION)){
						logger.error("Invalid certification at path " + this.trustedCertPath);
						return;
					}
					//checkSSLCert();
				} else{
					//check SSL certificate is installated
					logger.info("No certification path is set, accept all certifications.");
					setupAcceptAllSSLCertificats();
					conCheck = checkConnection();
					if(conCheck != null && conCheck.contains(CERT_EXCEPTION)){
						System.err.println("Invalid certification at parth " + this.trustedCertPath);
						return;
					}
					checkSSLCert();
				}
				break;
			case HOSTNAME_VERIFIER:
				logger.info("Can't verifi hostname, accept hostname: dss.local.");
				this.setupHostnameVerifierForDssLocal();
				conCheck = checkConnection();
				if(conCheck != null && conCheck.contains(HOSTNAME_VERIFIER)){
					logger.info("Can't verifi hostname, accept all hostnames");
					this.setupHostnameVerifier();
				}
				checkSSLCert();
				break;
			case UNKOWN_HOST:
				return;
			default: return;
			}
		}else{
			logger.info("All right, SSL Certificate is installeted");
		}
		
	}
	
	private String checkConnection(){
		try {
			URL url = new URL(uri);
			HttpsURLConnection connection = (HttpsURLConnection) url.openConnection();
			connection.connect();
			connection.disconnect();
		} catch (MalformedURLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			String msg = e.getMessage();
			
			if(e instanceof javax.net.ssl.SSLHandshakeException){
				logger.info(e.getMessage() );
				if(msg.contains(CERT_EXCEPTION) || msg.contains(PKIX_PATH_FAILD)){
					if(msg.contains(HOSTNAME_VERIFIER)){
						//logger.info("Hostname");
						return HOSTNAME_VERIFIER;
					}
					return CERT_EXCEPTION;
				}
				
			}
			 
			if(e instanceof java.net.UnknownHostException){
				logger.error("Can't find host: " + msg);
				return UNKOWN_HOST;
			}
			e.printStackTrace();			
		}
		return null;
	}
	
	private void setupAcceptAllSSLCertificats(){
		Security.addProvider(Security.getProvider("SunJCE"));
        TrustManager[] trustAllCerts = new TrustManager[]{
            new X509TrustManager() {

				@Override
				public java.security.cert.X509Certificate[] getAcceptedIssuers() {
					// TODO Auto-generated method stub
					return null;
				}

				@Override
				public void checkClientTrusted(X509Certificate[] arg0,
						String arg1) throws CertificateException {
					
					
				}

				@Override
				public void checkServerTrusted(X509Certificate[] arg0,
						String arg1) throws CertificateException {
					
					
				}
            }
        };

       		
		try {
			SSLContext sslContext = SSLContext.getInstance("SSL");			
      
			sslContext.init(null, trustAllCerts, new SecureRandom());
			
			HttpsURLConnection.setDefaultSSLSocketFactory(sslContext.getSocketFactory());
		} catch (KeyManagementException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (NoSuchAlgorithmException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	private void setupWithCertPath() {
		 
		CertificateFactory certificateFactory;
		try {
			certificateFactory = CertificateFactory.getInstance("X.509");
		
			trustedCert = (X509Certificate) certificateFactory.generateCertificate(this.certInputStream);
		} catch (CertificateException e) {
			// TODO Auto-generated catch block
			if(e.getMessage().contains(NO_CERT_AT_PATH)){
				logger.error("Can't find a certificationfile at the certificationpath: " + trustedCertPath + 
						"\nPlease check the path!");
				return;
			} else {
				e.printStackTrace();
			}
		}
				
 
 
		final TrustManager[] trustManager = new TrustManager[] { new X509TrustManager() {
 
			public java.security.cert.X509Certificate[] getAcceptedIssuers() {
 
				return null;
 
			}
 
 
			public void checkClientTrusted(java.security.cert.X509Certificate[] certs, String authType) throws CertificateException { 
				if (!certs[0].equals(trustedCert))
					throw new CertificateException();
			}
 
 
			public void checkServerTrusted(java.security.cert.X509Certificate[] certs, String authType) throws CertificateException {
 
				if (!certs[0].equals(trustedCert))
					throw new CertificateException();
 
			}
 
		} };
 
		SSLContext sslContext;
		
		try {
			sslContext = SSLContext.getInstance("SSL");
			sslContext.init(null, trustManager, new java.security.SecureRandom());
			HttpsURLConnection.setDefaultSSLSocketFactory(sslContext.getSocketFactory());
		} catch (NoSuchAlgorithmException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (KeyManagementException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} 
	}
	
	private void setupHostnameVerifier(){
		 HostnameVerifier allHostValid = new HostnameVerifier(){

				@Override
				public boolean verify(String arg0, SSLSession arg1) {
					// TODO Auto-generated method stub
					return true;
				}
				
		};
			
		HttpsURLConnection.setDefaultHostnameVerifier(allHostValid);
	}
	
	private void setupHostnameVerifierForDssLocal(){
		 HostnameVerifier allHostValid = new HostnameVerifier(){

				@Override
				public boolean verify(String arg0, SSLSession arg1) {
					// TODO Auto-generated method stub
					return arg0.contains("dss.local.");
				}
				
		};
			
		HttpsURLConnection.setDefaultHostnameVerifier(allHostValid);
	}
	
}