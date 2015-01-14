/**
 * Copyright (c) 2014 openHAB UG (haftungsbeschraenkt) and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.smarthome.binding.digitalstrom.handler;

import static org.eclipse.smarthome.binding.digitalstrom.DigitalSTROMBindingConstants.*;

import java.util.Set;

import org.eclipse.smarthome.binding.digitalstrom.DigitalSTROMBindingConstants;
import org.eclipse.smarthome.binding.digitalstrom.internal.client.constants.SensorIndexEnum;
import org.eclipse.smarthome.binding.digitalstrom.internal.client.entity.Device;
import org.eclipse.smarthome.binding.digitalstrom.internal.client.entity.DeviceStateUpdate;
import org.eclipse.smarthome.binding.digitalstrom.internal.client.job.DeviceConsumptionSensorJob;
import org.eclipse.smarthome.core.library.types.DecimalType;
import org.eclipse.smarthome.core.library.types.IncreaseDecreaseType;
import org.eclipse.smarthome.core.library.types.OnOffType;
import org.eclipse.smarthome.core.library.types.PercentType;
import org.eclipse.smarthome.core.thing.Bridge;
import org.eclipse.smarthome.core.thing.Channel;
import org.eclipse.smarthome.core.thing.ChannelUID;
import org.eclipse.smarthome.core.thing.Thing;
import org.eclipse.smarthome.core.thing.ThingStatus;
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
        		getDssBridgeHandler().unregisterDeviceStatusListener(dsID);
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
		DssBridgeHandler dssBridgeHandler = getDssBridgeHandler();
		if (dssBridgeHandler == null) {
            logger.warn("DigitalSTROM bridge handler not found. Cannot handle command without bridge.");
            return;
        }
		
		Device device = getDevice();
		
		if(device == null){
		    logger.debug("DigitalSTROM device not known on bridge. Cannot handle command.");
            return;
        }
			
		if(channelUID.getId().equals(CHANNEL_BRIGHTNESS)) {
			if (command instanceof PercentType) {
				device.setOutputValue(fromPercentToValue(((PercentType) command).intValue(), device.getMaxOutPutValue()));
			} else if (command instanceof OnOffType) {
				if(OnOffType.ON.equals((OnOffType) command)){
					device.setIsOn(true);
				} else{
					device.setIsOn(false);
				}
			} else if(command instanceof IncreaseDecreaseType) {
				if(IncreaseDecreaseType.INCREASE.equals((IncreaseDecreaseType) command)){
					device.increase();
				} else{
					device.decrease();
				}
			}
		} else {
			logger.warn("Command send to an unknown channel id: " + channelUID);
		}
			
		dssBridgeHandler.sendComandsToDSS(device);		
	}
	
	private int fromPercentToValue(int percent, int max) {
		if (percent < 0 || percent == 0) {
			return 0;
		}
		if (max < 0 || max == 0) {
			return 0;
		}
		return (int) (max * (float) ((float) percent / 100));
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
	        	this.dssBridgeHandler.registerDeviceStatusListener(dsID, this);
	        } else {
	            return null;
	        }
    	}
        return this.dssBridgeHandler;
    }

	@Override
	public void onDeviceStateChanged(Device device) {
		if(device != null && device.getDSID().getValue() == dsID){
			while(!device.isESHThingUpToDate()){
				DeviceStateUpdate stateUpdate = device.getNextESHThingUpdateStates();
				if(stateUpdate != null){
					switch(stateUpdate.getType()){
						case DeviceStateUpdate.UPDATE_BRIGHTNESS: 
							updateState(new ChannelUID(getThing().getUID(),  CHANNEL_BRIGHTNESS), 
									new PercentType(fromValueToPercent(stateUpdate.getValue(), device.getMaxOutPutValue())));
							break;
						case DeviceStateUpdate.UPDATE_ON_OFF: 
							if(stateUpdate.getValue() > 0) {
								updateState(new ChannelUID(getThing().getUID(),  CHANNEL_BRIGHTNESS), OnOffType.ON); 
							} else {
								updateState(new ChannelUID(getThing().getUID(),  CHANNEL_BRIGHTNESS), OnOffType.OFF);
							}
							break;
						case DeviceStateUpdate.UPDATE_ELECTRIC_METER_VALUE:
							updateState(new ChannelUID(getThing().getUID(),  CHANNEL_ELECTRIC_METER), new DecimalType(stateUpdate.getValue()));
							break;
						case DeviceStateUpdate.UPDATE_ENERGY_METER_VALUE:
							updateState(new ChannelUID(getThing().getUID(),  CHANNEL_ENERGY_METER), new DecimalType(stateUpdate.getValue()));
							break;
						case DeviceStateUpdate.UPDATE_POWER_CONSUMPTION:
							updateState(new ChannelUID(getThing().getUID(),  CHANNEL_POWER_CONSUMPTION), new DecimalType(stateUpdate.getValue()));
							break;
						default: return;
					}
				}
				
			}
		}		
	}
	
	private int fromValueToPercent(int value, int max) {
		if (value < 0 || value == 0) {
			return 0;
		}
		if (max < 0 || max == 0) {
			return 0;
		}
		return (int) (value * (float) ((float) 100 / max));
	}

	@Override
	public void onDeviceRemoved(Device device) {
		if (device.getDSID().getValue() == dsID) {
        	getThing().setStatus(ThingStatus.OFFLINE);
        }
	}

	@Override
	public void onDeviceAdded(Device device) {
		if (device.getDSID().getValue() == dsID) {
	        	getThing().setStatus(ThingStatus.ONLINE);
	        	onDeviceStateInitial(device);
	     }		
	}
	
	private void onDeviceStateInitial(Device device){
		updateState(new ChannelUID(getThing().getUID(),  CHANNEL_BRIGHTNESS), 
				new PercentType(fromValueToPercent(device.getOutputValue(), device.getMaxOutPutValue())));

		//nötig oder passiert das von selbst
		if(device.isOn()) {
			updateState(new ChannelUID(getThing().getUID(),  CHANNEL_BRIGHTNESS), OnOffType.ON); 
		} else {
			updateState(new ChannelUID(getThing().getUID(),  CHANNEL_BRIGHTNESS), OnOffType.OFF);
		}
		
		updateState(new ChannelUID(getThing().getUID(),  CHANNEL_ELECTRIC_METER), new DecimalType(device.getElectricMeterValue()));
		
		updateState(new ChannelUID(getThing().getUID(),  CHANNEL_ENERGY_METER), new DecimalType(device.getEnergyMeterValue()));
		
		updateState(new ChannelUID(getThing().getUID(),  CHANNEL_POWER_CONSUMPTION), new DecimalType(device.getPowerConsumption()));
		
	}

	@Override
	public void onDeviceNeededSensorDataUpdate(Device device) {
				
		for(Channel channel: this.getThing().getChannels()){
			switch(channel.getUID().getId()){
				case DigitalSTROMBindingConstants.CHANNEL_POWER_CONSUMPTION:
					if(!device.isPowerConsumptionUpToDate()){
						sendUpdateSensorDataToBridge(device, channel, SensorIndexEnum.OUTPUT_CURRENT);
					}
					break;
				case DigitalSTROMBindingConstants.CHANNEL_ENERGY_METER:
					if(!device.isEnergyMeterUpToDate()){
						sendUpdateSensorDataToBridge(device, channel, SensorIndexEnum.OUTPUT_CURRENT);
					}
					break;
				case DigitalSTROMBindingConstants.CHANNEL_ELECTRIC_METER:
					if(!device.isElectricMeterUpToDate()){
						sendUpdateSensorDataToBridge(device, channel, SensorIndexEnum.OUTPUT_CURRENT);
					}
					break;
			}
		}
	}

	private void sendUpdateSensorDataToBridge(Device device, Channel channel, SensorIndexEnum sensorIndex){
		DssBridgeHandler dssBridgeHandler = getDssBridgeHandler();
		
		if (dssBridgeHandler == null) {
            logger.warn("DigitalSTROM bridge handler not found. Cannot handle command without bridge.");
            return;
        }
		
		String priority = channel.getConfiguration().get(DigitalSTROMBindingConstants.CHANNEL_REFRESH_PRIORITY).toString();
		if(priority != DigitalSTROMBindingConstants.REFRESH_PRIORITY_NEVER && priority != null){
			dssBridgeHandler.updateSensorData(new DeviceConsumptionSensorJob(device, sensorIndex), priority);	
		}
	}
}
