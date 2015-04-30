/**
 * Copyright (c) 2014 openHAB UG (haftungsbeschraenkt) and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.smarthome.binding.digitalstrom.internal;

import static org.eclipse.smarthome.binding.digitalstrom.DigitalSTROMBindingConstants.APPLICATION_TOKEN;
import static org.eclipse.smarthome.binding.digitalstrom.DigitalSTROMBindingConstants.DEFAULT_CONNECTION_TIMEOUT;
import static org.eclipse.smarthome.binding.digitalstrom.DigitalSTROMBindingConstants.DEFAULT_READ_TIMEOUT;
import static org.eclipse.smarthome.binding.digitalstrom.DigitalSTROMBindingConstants.DS_ID;
import static org.eclipse.smarthome.binding.digitalstrom.DigitalSTROMBindingConstants.DEVICE_UID;
import static org.eclipse.smarthome.binding.digitalstrom.DigitalSTROMBindingConstants.HOST;
import static org.eclipse.smarthome.binding.digitalstrom.DigitalSTROMBindingConstants.PASSWORD;
import static org.eclipse.smarthome.binding.digitalstrom.DigitalSTROMBindingConstants.USER_NAME;

import java.net.HttpURLConnection;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Map;
import java.util.Set;

import org.eclipse.smarthome.binding.digitalstrom.DigitalSTROMBindingConstants;
import org.eclipse.smarthome.binding.digitalstrom.handler.DsSceneHandler;
import org.eclipse.smarthome.binding.digitalstrom.handler.DsYellowHandler;
import org.eclipse.smarthome.binding.digitalstrom.handler.DssBridgeHandler;
import org.eclipse.smarthome.binding.digitalstrom.internal.client.DigitalSTROMAPI;
import org.eclipse.smarthome.binding.digitalstrom.internal.client.impl.DigitalSTROMJSONImpl;
import org.eclipse.smarthome.binding.digitalstrom.internal.discovery.DsDeviceDiscoveryService;
import org.eclipse.smarthome.config.core.Configuration;
import org.eclipse.smarthome.config.discovery.DiscoveryService;
import org.eclipse.smarthome.core.thing.Bridge;
import org.eclipse.smarthome.core.thing.Thing;
import org.eclipse.smarthome.core.thing.ThingTypeUID;
import org.eclipse.smarthome.core.thing.ThingUID;
import org.eclipse.smarthome.core.thing.binding.BaseThingHandlerFactory;
import org.eclipse.smarthome.core.thing.binding.ThingHandler;
import org.osgi.framework.ServiceRegistration;
import org.slf4j.LoggerFactory;

import com.google.common.collect.Sets;

/**
 * The {@link DigitalSTROMHandlerFactory} is responsible for creating things and thing 
 * handlers.
 * 
 * @author Alex Maier - Initial contribution
 */
public class DigitalSTROMHandlerFactory extends BaseThingHandlerFactory {
    
	private Map<ThingUID, ServiceRegistration<?>> discoveryServiceRegs = new HashMap<>();
	
	private org.slf4j.Logger logger = LoggerFactory.getLogger(DigitalSTROMHandlerFactory.class);

	//Vervollständigen auf alle SupportetThingsTypes
	public final static Set<ThingTypeUID> SUPPORTED_THING_TYPES = Sets.union(DsSceneHandler.SUPPORTED_THING_TYPES,
			Sets.union(DssBridgeHandler.SUPPORTED_THING_TYPES,
					DsYellowHandler.SUPPORTED_THING_TYPES));

	private DigitalSTROMAPI digitalSTROMClient = null;

    @Override
    public boolean supportsThingType(ThingTypeUID thingTypeUID) {
        return SUPPORTED_THING_TYPES.contains(thingTypeUID);
    }

    //Vervollständigen auf alle Things
    @Override
    public Thing createThing(ThingTypeUID thingTypeUID, Configuration configuration,
            ThingUID thingUID, ThingUID bridgeUID) {
        if (DssBridgeHandler.SUPPORTED_THING_TYPES.contains(thingTypeUID)) {
            ThingUID digitalStromUID = getBridgeThingUID(thingTypeUID, thingUID, configuration);
            return super.createThing(thingTypeUID, configuration, digitalStromUID, null);
        } 
        
        if (DsYellowHandler.SUPPORTED_THING_TYPES.contains(thingTypeUID)) {
            ThingUID dssLightUID = getLightUID(thingTypeUID, thingUID, configuration, bridgeUID);
            return super.createThing(thingTypeUID, configuration, dssLightUID, bridgeUID);
        }  
        
        if (DsSceneHandler.SUPPORTED_THING_TYPES.contains(thingTypeUID)) {
            ThingUID dssSceneUID = getSceneUID(thingTypeUID, thingUID, configuration, bridgeUID);
            return super.createThing(thingTypeUID, configuration, dssSceneUID, bridgeUID);
        }  
        
        throw new IllegalArgumentException("The thing type " + thingTypeUID
        		    + " is not supported by the DigitalSTROM binding.");
            
    }
    
    //Vervollständigen auf alle ThingHandler
	@Override
    protected ThingHandler createHandler(Thing thing) {

        ThingTypeUID thingTypeUID = thing.getThingTypeUID();
        
        if(thingTypeUID == null) return null;
        
        if (DssBridgeHandler.SUPPORTED_THING_TYPES.contains(thing.getThingTypeUID())) {
        	DssBridgeHandler handler = new DssBridgeHandler((Bridge) thing);
			registerDeviceDiscoveryService(handler);
			return handler;
        } 
        
        if (DsYellowHandler.SUPPORTED_THING_TYPES.contains(thing.getThingTypeUID())) {
        	return new DsYellowHandler(thing);
        } 
        
        if (DsSceneHandler.SUPPORTED_THING_TYPES.contains(thing.getThingTypeUID())) {
        	return new DsSceneHandler(thing);
        } 
        
        return null;
    }
	
    private void registerDeviceDiscoveryService(DssBridgeHandler handler) {
    	DsDeviceDiscoveryService discoveryService = new DsDeviceDiscoveryService(handler); 
    	discoveryService.activate(); 
        this.discoveryServiceRegs.put(handler.getThing().getUID(), bundleContext
                .registerService(DiscoveryService.class.getName(), discoveryService,
                        new Hashtable<String, Object>()));
	}

	private ThingUID getLightUID(ThingTypeUID thingTypeUID, ThingUID thingUID,
			Configuration configuration, ThingUID bridgeUID) {
		String lightId = configuration.get(DEVICE_UID).toString();
		if (thingUID == null) {
	          thingUID = new ThingUID(thingTypeUID, lightId, bridgeUID.getId());
	    }
	    return thingUID;
	}
	
	private ThingUID getSceneUID(ThingTypeUID thingTypeUID, ThingUID thingUID,
			Configuration configuration, ThingUID bridgeUID) {
		String sceneId = configuration.get(DigitalSTROMBindingConstants.SCENE_ZONE_ID).toString() + "-" +
				 configuration.get(DigitalSTROMBindingConstants.SCENE_GROUP_ID).toString() + "-" +
				 configuration.get(DigitalSTROMBindingConstants.SCENE_ID).toString();
		if (thingUID == null) {
	          thingUID = new ThingUID(thingTypeUID, sceneId, bridgeUID.getId());
	    }
	    return thingUID;
	}

	private ThingUID getBridgeThingUID(ThingTypeUID thingTypeUID,
			ThingUID thingUID, Configuration configuration) {
		digitalSTROMClient = new DigitalSTROMJSONImpl(configuration.get(HOST).toString(), 
				DEFAULT_CONNECTION_TIMEOUT, 
				DEFAULT_READ_TIMEOUT);
		
		int responseCode = digitalSTROMClient.checkConnection("test"); 
		if( responseCode == HttpURLConnection.HTTP_NOT_FOUND || 
				responseCode == -1 || 
				responseCode == -2){
			if(responseCode == HttpURLConnection.HTTP_NOT_FOUND || 
					responseCode == -1 ){
				logger.error("Server not found! Please check this points:\n"
					+ " - DigitalSTROM-Server turned on?\n"
					+ " - hostadress correct?\n"
					+ " - ethernet cable connection established?");
			} else {
				logger.error("Invalide URL!");
			}
			return null;
		}
		
		if (thingUID == null) {
			String dSID;
			
			if(configuration.get(DS_ID) == null){
				dSID = getDssID(configuration);
				configuration.put(DS_ID, dSID);
			}
			else dSID = configuration.get(DS_ID).toString();
            
			thingUID = new ThingUID(thingTypeUID, dSID);
        }
        return thingUID;
	}
	
	private String getDssID(Configuration configuration){
		String dsID = null;
		
		String applicationToken;
		String sessionToken;
		
		/*
		int responseCode = digitalSTROMClient.checkConnection("test"); 
		if( responseCode == HttpURLConnection.HTTP_NOT_FOUND || 
				responseCode == -1 || 
				responseCode == -2){
			if(responseCode == HttpURLConnection.HTTP_NOT_FOUND || 
					responseCode == -1 ){
				logger.info("Server not found! Please check this points:\n"
					+ " - DigitalSTROM-Server turned on?\n"
					+ " - hostadress correct?\n"
					+ " - ethernet cable connection established?");
			} else {
				logger.info("Invalide URL!");
			}
			return dsID;
		}
		*/
		if(configuration.get(APPLICATION_TOKEN) != null && 
				!( applicationToken = configuration.get(APPLICATION_TOKEN).toString()).trim().isEmpty()){
			
			sessionToken = digitalSTROMClient.loginApplication(applicationToken);
			
			if((dsID = digitalSTROMClient.getDSID(sessionToken)) != null) {
				logger.debug("User defined Applicationtoken can be used. Get dsID.");
			} else{
				if(digitalSTROMClient.checkConnection(sessionToken) == HttpURLConnection.HTTP_NOT_FOUND || 
						digitalSTROMClient.checkConnection(sessionToken) == -1){
					logger.info("Server not found! Please check this points:\n"
							+ " - DigitalSTROM-Server turned on?\n"
							+ " - hostadress correct?\n"
							+ " - ethernet cable connection established?");
				} else{
					logger.info("User defined Applicationtoken can't be used.");
				}
			}
		}
		//final ändern in Konsoleneingabe oder Konsoleneingabe hinzufügen
		if(checkUserPassword(configuration)){
			if(dsID == null){
				logger.debug("Generating Applicationtoken with user and password.");
				
				//generate applicationToken and test host is reachable
				applicationToken = this.digitalSTROMClient.requestAppplicationToken(DigitalSTROMBindingConstants.APPLICATION_NAME);
							
				if(applicationToken != null && !applicationToken.isEmpty()){
					//enable applicationToken
					sessionToken = this.digitalSTROMClient.login(
							configuration.get(USER_NAME).toString(), 
							configuration.get(PASSWORD).toString());
					
					if(this.digitalSTROMClient.enableApplicationToken(applicationToken, sessionToken)){
						configuration.put(APPLICATION_TOKEN, applicationToken);
		
						dsID = digitalSTROMClient.getDSID(sessionToken);
						
						logger.debug("Applicationtoken generated and added to the configuration. Get dsID.");
					} else {
						logger.info("Incorrect Username or password. Can't enable Applicationtoken.");
					}
				}else{
					logger.info("Incorrect hostadress or DigitalSTROM sever isn't reachable.");
				}
			}
			
			//remove password and username, to don't store them persistently   
			if(dsID != null){
				configuration.remove(PASSWORD);
				configuration.remove(USER_NAME);
			}
		} else 
			if(dsID == null){
				logger.info("Can't find Username or password to genarate Appicationtoken.");
			}
				
		return dsID;
	}
	
	
	private boolean checkUserPassword(Configuration configuration){
		if((configuration.get(USER_NAME) != null && configuration.get(PASSWORD) != null) &&
			(!configuration.get(USER_NAME).toString().trim().isEmpty() && !configuration.get(PASSWORD).toString().trim().isEmpty()))//notwendig? 
			return true;
		return false;
	}
}

