/**
 * Copyright (c) 2014 openHAB UG (haftungsbeschraenkt) and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.smarthome.binding.digitalstrom.handler;

import static org.eclipse.smarthome.binding.digitalstrom.DigitalSTROMBindingConstants.CHANNEL_BRIGHTNESS;
import static org.eclipse.smarthome.binding.digitalstrom.DigitalSTROMBindingConstants.DS_ID;
import static org.eclipse.smarthome.binding.digitalstrom.DigitalSTROMBindingConstants.THING_TYPE_GE_KL200;
import static org.eclipse.smarthome.binding.digitalstrom.DigitalSTROMBindingConstants.THING_TYPE_GE_KM200;

import java.util.Set;

import org.eclipse.smarthome.binding.digitalstrom.internal.client.entity.Device;
import org.eclipse.smarthome.core.library.types.IncreaseDecreaseType;
import org.eclipse.smarthome.core.library.types.OnOffType;
import org.eclipse.smarthome.core.library.types.PercentType;
import org.eclipse.smarthome.core.thing.Bridge;
import org.eclipse.smarthome.core.thing.ChannelUID;
import org.eclipse.smarthome.core.thing.Thing;
import org.eclipse.smarthome.core.thing.ThingTypeUID;
import org.eclipse.smarthome.core.thing.binding.BaseThingHandler;
import org.eclipse.smarthome.core.thing.binding.ThingHandler;
import org.eclipse.smarthome.core.types.Command;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.Sets;

/**
 * The {@link DsYellowHandler} is responsible for handling commands, which are
 * sent to one of the channels of an yellow (light) DigitalStrom device.
 * 
 * @author Michael
 *
 */
public class DsYellowHandler extends BaseThingHandler implements DeviceStatusListener{

    private Logger logger = LoggerFactory.getLogger(DigitalSTROMHandler.class);

    public final static Set<ThingTypeUID> SUPPORTED_THING_TYPES = Sets.newHashSet(THING_TYPE_GE_KM200, THING_TYPE_GE_KL200);
    
    private String dsID = null;

	private DssBridgeHandler dssBridgeHandler;
	
	public DsYellowHandler(Thing thing) {
		super(thing);
		// TODO Auto-generated constructor stub
	}
	
    @Override
    public void initialize() {
    	logger.debug("Initializing digitalSTROM yellow device handler.");
        final String configDSId = (String) getConfig().get(DS_ID);
        if (configDSId != null) {
            dsID = configDSId;
        	// note: this call implicitly registers our handler as a listener on the bridge
            if(getDssBridgeHandler()!=null) { 
            	getThing().setStatus(getBridge().getStatus());
            }
        }
    }

    @Override
    public void dispose() {
        logger.debug("Handler disposes. Unregistering listener.");
        if (dsID != null) {
            DssBridgeHandler dssBridgeHandler = getDssBridgeHandler();
        	if(dssBridgeHandler != null) {
        		getDssBridgeHandler().unregisterDeviceStatusListener(this);
        	}
            dsID = null;
        }
    }

    private Device getDevice() {
    	DssBridgeHandler dssBridgeHandler = getDssBridgeHandler();
    	if(dssBridgeHandler != null) {
    		return dssBridgeHandler.getDeviceByDSID(dsID);
    	}
        return null;
    }

	@Override
	public void handleCommand(ChannelUID channelUID, Command command) {
		
		if(channelUID.getId().equals(CHANNEL_BRIGHTNESS)) {
			if (command instanceof PercentType) {
			
            } else if (command instanceof OnOffType) {
            
            } else if(command instanceof IncreaseDecreaseType) {
            
            }
        }	else {
            logger.warn("Command send to an unknown channel id: " + channelUID);
        }	
		
	}
	
	private synchronized DssBridgeHandler getDssBridgeHandler() {
    	if(this.dssBridgeHandler==null) {
	    	Bridge bridge = getBridge();
	        if (bridge == null) {
	            return null;
	        }
	        ThingHandler handler = bridge.getHandler();
	        if (handler instanceof DssBridgeHandler) {
	        	this.dssBridgeHandler =  (DssBridgeHandler) handler;
	        	this.dssBridgeHandler.registerDeviceStatusListener(this);
	        } else {
	            return null;
	        }
    	}
        return this.dssBridgeHandler;
    }

	@Override
	public void onDeviceStateChanged(Device device) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void onDeviceRemoved(Device device) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void onDeviceAdded(Device device) {
		// TODO Auto-generated method stub
		
	}

}
