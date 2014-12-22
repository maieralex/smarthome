/**
 * Copyright (c) 2014 openHAB UG (haftungsbeschraenkt) and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.smarthome.binding.digitalstrom.handler;

import static org.eclipse.smarthome.binding.digitalstrom.DigitalSTROMBindingConstants.APPLICATION_TOKEN;
import static org.eclipse.smarthome.binding.digitalstrom.DigitalSTROMBindingConstants.DEFAULT_CONNECTION_TIMEOUT;
import static org.eclipse.smarthome.binding.digitalstrom.DigitalSTROMBindingConstants.DEFAULT_READ_TIMEOUT;
import static org.eclipse.smarthome.binding.digitalstrom.DigitalSTROMBindingConstants.HOST;
import static org.eclipse.smarthome.binding.digitalstrom.DigitalSTROMBindingConstants.THING_TYPE_DSS_BRIDGE;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;

import org.eclipse.smarthome.binding.digitalstrom.internal.client.DigitalSTROMAPI;
import org.eclipse.smarthome.binding.digitalstrom.internal.client.DigitalSTROMEventListener;
import org.eclipse.smarthome.binding.digitalstrom.internal.client.entity.Apartment;
import org.eclipse.smarthome.binding.digitalstrom.internal.client.entity.DetailedGroupInfo;
import org.eclipse.smarthome.binding.digitalstrom.internal.client.entity.Device;
import org.eclipse.smarthome.binding.digitalstrom.internal.client.entity.DeviceSceneSpec;
import org.eclipse.smarthome.binding.digitalstrom.internal.client.entity.Zone;
import org.eclipse.smarthome.binding.digitalstrom.internal.client.impl.DigitalSTROMJSONImpl;
import org.eclipse.smarthome.core.thing.Bridge;
import org.eclipse.smarthome.core.thing.ChannelUID;
import org.eclipse.smarthome.core.thing.ThingTypeUID;
import org.eclipse.smarthome.core.thing.binding.BaseBridgeHandler;
import org.eclipse.smarthome.core.types.Command;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The {@link DigitalSTROMHandler} is responsible for handling commands, which are
 * sent to one of the channels.
 * 
 * @author Alex Maier - Initial contribution
 */
public class DssBridgeHandler extends BaseBridgeHandler {


	private Logger logger = LoggerFactory.getLogger(DssBridgeHandler.class);
    
    public final static Set<ThingTypeUID> SUPPORTED_THING_TYPES = Collections.singleton(THING_TYPE_DSS_BRIDGE);

    private DigitalSTROMAPI digitalSTROMClient = null;
    private String applicationToken = null;
    
    private List<DeviceStatusListener> deviceStatusListeners = new CopyOnWriteArrayList<>();
    
    private HashMap<String, Device> deviceMap = new HashMap<String, Device>();
    
	// zoneID - Map < groupID, List<dsid-String>>
	private Map<Integer, Map<Short, List<String>>> digitalSTROMZoneGroupMap = Collections
			.synchronizedMap(new HashMap<Integer, Map<Short, List<String>>>());

	private DigitalSTROMEventListener digitalSTROMEventListener = null;
    
	public DssBridgeHandler(Bridge bridge) {
		super(bridge);
	}
	
    @Override
    public void dispose() {
        logger.debug("Handler disposed.");
        this.digitalSTROMEventListener.shutdown();
        //füllen mit allem rest
    }

    @Override
    public void initialize() {
        logger.debug("Initializing DigitalSTROM Bridge handler.");
        
        if(this.getConfig().get(HOST).toString() != null){
        	this.digitalSTROMClient = new DigitalSTROMJSONImpl(
        			this.getConfig().get(HOST).toString(), 
        			DEFAULT_CONNECTION_TIMEOUT, 
        			DEFAULT_READ_TIMEOUT);
        
        	this.applicationToken = this.getConfig().get(APPLICATION_TOKEN).toString();
		
        	deviceMap.putAll(getDigitalSTROMDeviceHashMap());
        	
        	handleStructure(digitalSTROMClient
        			.getApartmentStructure(applicationToken));
		
        	this.digitalSTROMEventListener = new DigitalSTROMEventListener(
        			this.getConfig().get(HOST).toString(), 
        			(DigitalSTROMJSONImpl) digitalSTROMClient, 
        			this);
        	this.digitalSTROMEventListener.start();
        	
        } else{
            logger.warn("Cannot connect to DigitalSTROMSever. Host address is not set.");
        }
        
    }
    
    private HashMap<String, Device> getDigitalSTROMDeviceHashMap(){
    	HashMap<String, Device> tempDeviceMap = new HashMap<String, Device>();
    	
    	for(Device device: digitalSTROMClient.getApartmentDevices(applicationToken, false)){
    		tempDeviceMap.put(device.getDSID().getValue(), device);
    	}
    	
    	return tempDeviceMap;
    }
	
	// Here we build up a new hashmap in order to replace it with the old one.
	// This hashmap is used to find the affected items after an event from
	// digitalSTROM.
	public void handleStructure(Apartment apartment) {
		if (apartment != null) {

			Map<Integer, Map<Short, List<String>>> newZoneGroupMap = Collections
					.synchronizedMap(new HashMap<Integer, Map<Short, List<String>>>());
			Map<String, Device> clonedDsidMap = getDsidToDeviceMap();

			for (Zone zone : apartment.getZoneMap().values()) {

				Map<Short, List<String>> groupMap = new HashMap<Short, List<String>>();

				for (DetailedGroupInfo g : zone.getGroups()) {

					List<String> devicesInGroup = new LinkedList<String>();
					for (String dsid : g.getDeviceList()) {
						if (clonedDsidMap.containsKey(dsid)) {
							devicesInGroup.add(dsid);
						}
					}
					groupMap.put(g.getGroupID(), devicesInGroup);
				}
				newZoneGroupMap.put(zone.getZoneId(), groupMap);
			}

			synchronized (digitalSTROMZoneGroupMap) {
				digitalSTROMZoneGroupMap = newZoneGroupMap;
			}
		}
	}

	@Override
	public void handleCommand(ChannelUID channelUID, Command command) {
     /*   if(channelUID.getId().equals(CHANNEL_BRIGHTNESS)) {
            // TODO: handle command
        }*/
	}
	
	public Collection<Device> getDevices(){
		return deviceMap.values();
	}
	
	public Map<String, Device> getDsidToDeviceMap() {
		return new HashMap<String, Device>(deviceMap);
	}
	
	public Device getDeviceByDSID(String dsID){
		return deviceMap.get(dsID);
	}
	
	public Map<Integer, Map<Short, List<String>>> getDigitalSTROMZoneGroupMap() {
		return new HashMap<Integer, Map<Short, List<String>>>(
				digitalSTROMZoneGroupMap);
	}
	
	public String getApplicationToken(){
		return applicationToken;
	}
	
	 public boolean registerDeviceStatusListener(DeviceStatusListener deviceStatusListener) {
	        if (deviceStatusListener == null) {
	            throw new NullPointerException("It's not allowed to pass a null LightStatusListener.");
	        }
	        boolean result = deviceStatusListeners.add(deviceStatusListener);
	        if (result) {
	        	//onUpdate();
	            // inform the listener initially about all lights and their states
	            for (Device device : deviceMap.values()) {
	            	deviceStatusListener.onDeviceAdded(device);
	            }
	        }
	        return result;
	    }

	    public boolean unregisterDeviceStatusListener(DeviceStatusListener deviceStatusListener) {
	        boolean result = deviceStatusListeners.remove(deviceStatusListener);
	        if (result) {
	            //onUpdate();
	        }
	        return result;
	    }

	
	// Here we read the configured and stored (in the chip) output value for a
	// specific scene
	// and we store this value in order to know next time what to do.
	// The first time a scene command is called, it takes some time for the
	// sensor reading,
	// but the next time we react very fast because we learned what to do on
	// this command.
	public void getSceneSpec(Device device, short sceneId) {

		// setSensorReading(true); // no metering in this time
		DeviceSceneSpec spec = digitalSTROMClient.getDeviceSceneMode(
				applicationToken, device.getDSID(), null, sceneId);
		// setSensorReading(false);

		if (spec != null) {
			device.addSceneConfig(sceneId, spec);
			logger.info("UPDATED ignoreList for dsid: " + device.getDSID()
					+ " sceneID: " + sceneId);
		}
	}

	
}
