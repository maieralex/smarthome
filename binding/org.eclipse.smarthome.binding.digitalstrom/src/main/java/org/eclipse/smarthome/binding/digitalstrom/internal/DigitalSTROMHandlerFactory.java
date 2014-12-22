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
import static org.eclipse.smarthome.binding.digitalstrom.DigitalSTROMBindingConstants.DEVICE_ID;
import static org.eclipse.smarthome.binding.digitalstrom.DigitalSTROMBindingConstants.DS_ID;
import static org.eclipse.smarthome.binding.digitalstrom.DigitalSTROMBindingConstants.HOST;
import static org.eclipse.smarthome.binding.digitalstrom.DigitalSTROMBindingConstants.PASSWORD;
import static org.eclipse.smarthome.binding.digitalstrom.DigitalSTROMBindingConstants.USER_NAME;

import java.util.HashMap;
import java.util.Hashtable;
import java.util.Map;
import java.util.Set;

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

import com.google.common.collect.Sets;

/**
 * The {@link DigitalSTROMHandlerFactory} is responsible for creating things and thing 
 * handlers.
 * 
 * @author Alex Maier - Initial contribution
 */
public class DigitalSTROMHandlerFactory extends BaseThingHandlerFactory {
    
	private Map<ThingUID, ServiceRegistration<?>> discoveryServiceRegs = new HashMap<>();
	
	//Vervollständigen auf alle SupportetThingsTypes
	public final static Set<ThingTypeUID> SUPPORTED_THING_TYPES = Sets.union(
			DssBridgeHandler.SUPPORTED_THING_TYPES,
			DsYellowHandler.SUPPORTED_THING_TYPES);

	private DigitalSTROMAPI digitalSTROMClient = null;
	private String applicationName = "EclipseSmartHome";


    @Override
    public boolean supportsThingType(ThingTypeUID thingTypeUID) {
        return SUPPORTED_THING_TYPES.contains(thingTypeUID);
    }

    //Vervollständigen auf alle Things
    @Override
    public Thing createThing(ThingTypeUID thingTypeUID, Configuration configuration,
            ThingUID thingUID, ThingUID bridgeUID) {
        if (DssBridgeHandler.SUPPORTED_THING_TYPES.equals(thingTypeUID)) {
            ThingUID digitalStromUID = getBridgeThingUID(thingTypeUID, thingUID, configuration);
            return super.createThing(thingTypeUID, configuration, digitalStromUID, null);
        }
        if (DsYellowHandler.SUPPORTED_THING_TYPES.equals(thingTypeUID)) {
            ThingUID dssLightUID = getLightUID(thingTypeUID, thingUID, configuration, bridgeUID);
            return super.createThing(thingTypeUID, configuration, dssLightUID, bridgeUID);
        }
        throw new IllegalArgumentException("The thing type " + thingTypeUID
                + " is not supported by the DigitalStrom binding.");
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
		String lightId = configuration.get(DEVICE_ID).toString();
		if (thingUID == null) {
	          thingUID = new ThingUID(thingTypeUID, lightId, bridgeUID.getId());
	    }
	    return thingUID;
	}

	private ThingUID getBridgeThingUID(ThingTypeUID thingTypeUID,
			ThingUID thingUID, Configuration configuration) {
		digitalSTROMClient = new DigitalSTROMJSONImpl((String) configuration.get(HOST), DEFAULT_CONNECTION_TIMEOUT, DEFAULT_READ_TIMEOUT);
		
		if (thingUID == null) {
			String dSID;
			
			if(configuration.get(DS_ID) == null){
				//verbinde zu DSS (Host...)
				dSID = getDssID(configuration);//test = hole dSID vom DSS
				configuration.put(DS_ID, dSID);
			}
			else dSID = configuration.get(DS_ID).toString();
            
			thingUID = new ThingUID(thingTypeUID, dSID);
        }
        return thingUID;
	}
	
	private String getDssID(Configuration configuration){
		String dsID = null;
		
		if(configuration.get(APPLICATION_TOKEN) != null && configuration.get(APPLICATION_TOKEN).toString() != ""){
			dsID =  digitalSTROMClient.getDSID(configuration.get(APPLICATION_TOKEN).toString());
		}
		//final ändern in Konsoleneingabe oder Konsoleneingabe hinzufügen
		if(checkUserPassword(configuration)){
			if(dsID == null){
				//Fehlerüberprüfung einbauen ... wenn z.B. Host, User oder Passwort falsch sind
				
				//generate applicationToken
				String applicationToken = this.digitalSTROMClient.requestAppplicationToken(applicationName);
							
				if(applicationToken != null && applicationToken != ""){
					//enable applicationToken
					if(this.digitalSTROMClient.enableApplicationToken(applicationToken, getSessionToken(configuration))){
						configuration.put(APPLICATION_TOKEN, applicationToken);
		
						dsID = digitalSTROMClient.getDSID(applicationToken);
					}
				}
			}
			
			//remove password and username, to don't store them persistently   
			if(dsID != null){
				configuration.remove(PASSWORD);
				configuration.remove(USER_NAME);
			}
		}
				
		return dsID;
	}
	
	private String getSessionToken(Configuration configuration){
		return this.digitalSTROMClient.login(
				configuration.get(USER_NAME).toString(), 
				configuration.get(PASSWORD).toString());
	}
	
	private boolean checkUserPassword(Configuration configuration){
		if((configuration.get(USER_NAME) != null && configuration.get(PASSWORD) != null) &&
			(configuration.get(USER_NAME).toString() != "" && configuration.get(PASSWORD).toString() != ""))//notwendig? 
			return true;
		return false;
	}
}

