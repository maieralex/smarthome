/**
 * Copyright (c) 2014-2015 openHAB UG (haftungsbeschraenkt) and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.smarthome.binding.digitalstrom.internal.discovery;

import java.util.HashMap;
import java.util.Map;

import org.eclipse.smarthome.binding.digitalstrom.DigitalSTROMBindingConstants;
import org.eclipse.smarthome.binding.digitalstrom.internal.client.DigitalSTROMAPI;
import org.eclipse.smarthome.binding.digitalstrom.internal.client.impl.DigitalSTROMJSONImpl;
import org.eclipse.smarthome.config.discovery.AbstractDiscoveryService;
import org.eclipse.smarthome.config.discovery.DiscoveryResult;
import org.eclipse.smarthome.config.discovery.DiscoveryResultBuilder;
import org.eclipse.smarthome.core.thing.ThingUID;
import org.slf4j.LoggerFactory;

/**
 * The {@link DssBridgeAbstractDiscoveryParticipant} is responsible for discovering new and
 * removed DigitalSTROM bridges. It uses the central {@link MDNSDiscoveryService}.
 *
 * @author Michael Ochel, Matthias Siegele 
 *
 */
public class DssBridgeAbstractDiscoveryService extends AbstractDiscoveryService {

	public DssBridgeAbstractDiscoveryService(){
		super(DigitalSTROMBindingConstants.SUPPORTED_THING_TYPES_UIDS, 10);
	}


	private org.slf4j.Logger logger = LoggerFactory.getLogger(DssBridgeAbstractDiscoveryService.class);
	
	private String hostAdress = "dss.local."; 
	private String dsid = null;
	
	public void createResult() {
		ThingUID uid = getThingUID();
        if (uid != null) {
            Map<String, Object> properties = new HashMap<>(2);
            properties.put(DigitalSTROMBindingConstants.HOST, hostAdress);
            properties.put(DigitalSTROMBindingConstants.DS_ID, dsid);

            DiscoveryResult result = DiscoveryResultBuilder.create(uid).withProperties(properties).withLabel("DigitalSTROM-Server").build();
            thingDiscovered(result);
        }
      
	}

	
	public ThingUID getThingUID() {
		//logger.debug("URL: " + service.getName() + "."+service.getDomain() + ".");
		//if(service.getApplication().contains("dssweb")){
			//hostAdress = service.getName() + "."+service.getDomain() + ".";
		DigitalSTROMAPI digitalSTROMClient = new DigitalSTROMJSONImpl(hostAdress, 
			DigitalSTROMBindingConstants.DEFAULT_CONNECTION_TIMEOUT, 
			DigitalSTROMBindingConstants.DEFAULT_READ_TIMEOUT);
		dsid = digitalSTROMClient.getDSID("123");
		logger.debug("test connection, dsid = {}",dsid);
		if(dsid != null){
				return new ThingUID(DigitalSTROMBindingConstants.THING_TYPE_DSS_BRIDGE,
				dsid);
		}
		return null;
	}

	@Override
	protected void startScan() {
		createResult();
		
	}
	
	@Override
    protected void startBackgroundDiscovery() {
		new Thread(new Runnable(){

			@Override
			public void run() {
				createResult();
			}
			
		}).start();
    }

}
