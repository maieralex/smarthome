package org.eclipse.smarthome.binding.digitalstrom.handler;

import static org.eclipse.smarthome.binding.digitalstrom.DigitalSTROMBindingConstants.CHANNEL_SHADE;

import java.util.Map;
import java.util.Set;

import org.eclipse.smarthome.binding.digitalstrom.DigitalSTROMBindingConstants;
import org.eclipse.smarthome.binding.digitalstrom.internal.client.entity.Device;
import org.eclipse.smarthome.binding.digitalstrom.internal.client.entity.DeviceSceneSpec;
import org.eclipse.smarthome.binding.digitalstrom.internal.client.entity.DeviceStateUpdate;
import org.eclipse.smarthome.binding.digitalstrom.internal.client.entity.impl.JSONDeviceSceneSpecImpl;
import org.eclipse.smarthome.core.library.types.IncreaseDecreaseType;
import org.eclipse.smarthome.core.library.types.OnOffType;
import org.eclipse.smarthome.core.library.types.PercentType;
import org.eclipse.smarthome.core.library.types.StopMoveType;
import org.eclipse.smarthome.core.library.types.UpDownType;
import org.eclipse.smarthome.core.thing.Bridge;
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

public class DsGrayHandler extends BaseThingHandler implements
		DeviceStatusListener {

	public DsGrayHandler(Thing thing) {
		super(thing);
	}

	 private Logger logger = LoggerFactory.getLogger(DsGrayHandler.class);

	    public final static Set<ThingTypeUID> SUPPORTED_THING_TYPES = Sets.newHashSet();
	    
	    private String dSUID = null;

		private DssBridgeHandler dssBridgeHandler;
		
		private Command lastComand = null; 
		
		
	    @Override
	    public void initialize() {
	    	logger.debug("Initializing DigitalSTROM Yellow (light) handler.");
	    	String configDSUId = getConfig().get(DigitalSTROMBindingConstants.DEVICE_UID).toString();
	    	
	    	if (!configDSUId.isEmpty()) {
	            dSUID = configDSUId;
	    	}
	    }

	    @Override
	    protected void bridgeHandlerInitialized(ThingHandler thingHandler, Bridge bridge) {
	    	if (dSUID != null) { //kann das überhaupt null werden?
	            
	            if (thingHandler instanceof DssBridgeHandler) {
		        	this.dssBridgeHandler =  (DssBridgeHandler) thingHandler;
		        	this.dssBridgeHandler.registerDeviceStatusListener(dSUID, this);
		        	
		        	// note: this call implicitly registers our handler as a listener on the bridge
		            getThing().setStatus(bridge.getStatus());
		        	logger.debug("Set status on {}", getThing().getStatus());
		        	
		        	saveConfigSceneSpecificationIntoDevice(getDevice());
		        	logger.debug("Load saved scene specification into device");
		        }
	        }
	    }
	    
	    @Override
	    public void dispose() {
	        logger.debug("Handler disposes. Unregistering listener.");
	        if (dSUID != null) {
	            DssBridgeHandler dssBridgeHandler = getDssBridgeHandler();
	        	if(dssBridgeHandler != null) {
	        		getDssBridgeHandler().unregisterDeviceStatusListener(dSUID);
	        	}
	            dSUID = null;
	        }
	    }

	    private Device getDevice() {
	    	DssBridgeHandler dssBridgeHandler = getDssBridgeHandler();
	    	if(dssBridgeHandler != null) {
	    		//logger.debug("get DssBridgeHandler");
	    		return dssBridgeHandler.getDeviceByDSUID(dSUID);
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
				
			if(channelUID.getId().equals(DigitalSTROMBindingConstants.CHANNEL_SHADE)) {
				if (command instanceof PercentType) {
					device.setOutputValue(fromPercentToValue(((PercentType) command).intValue(), device.getMaxOutPutValue()));
					this.lastComand = command;
				} else if (command instanceof StopMoveType) {
					if(StopMoveType.MOVE.equals((OnOffType) command)){
						handleCommand(channelUID, this.lastComand);
					} else{
						dssBridgeHandler.stopOutputValue(device);
					}
				} else if(command instanceof UpDownType) {
					if(UpDownType.UP.equals((IncreaseDecreaseType) command)){
						device.setIsOpen(true);
						this.lastComand = command;
					} else{
						device.setIsOpen(false);
						this.lastComand = command;
					}
				}
			} else {
				logger.warn("Command send to an unknown channel id: " + channelUID);
			}
			//TODO: hinzufügen zum testen
			//logger.debug("Inform DssBridgeHandler about command {}", command.toString());
			//dssBridgeHandler.sendComandsToDSS(device);
			
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
		            logger.debug("cant find Bridge");
		        	return null;
		        }
		        ThingHandler handler = bridge.getHandler();
		        
		        if (handler instanceof DssBridgeHandler) {
		        	this.dssBridgeHandler =  (DssBridgeHandler) handler;
		        	this.dssBridgeHandler.registerDeviceStatusListener(dSUID, this);
		        } else{
		        	return null;
		        }
	    	}
	        return this.dssBridgeHandler;
	    }

		@Override
		public synchronized void onDeviceStateChanged(Device device) {
			if(device != null){
				if(!device.isESHThingUpToDate()){
					logger.debug("Update ESH State");
					DeviceStateUpdate stateUpdate = device.getNextESHThingUpdateStates();
					if(stateUpdate != null){
						switch(stateUpdate.getType()){
							case DeviceStateUpdate.UPDATE_SLATPOSITION: 
									updateState(new ChannelUID(getThing().getUID(),  CHANNEL_SHADE), 
										new PercentType(fromValueToPercent(stateUpdate.getValue(), device.getMaxOutPutValue())));
								break;
							case DeviceStateUpdate.UPDATE_OPEN_CLOSE: 
								if(stateUpdate.getValue() > 0) {
									updateState(new ChannelUID(getThing().getUID(),  CHANNEL_SHADE), UpDownType.UP);
									updateState(new ChannelUID(getThing().getUID(),  CHANNEL_SHADE), new PercentType(100));
								} else {
									updateState(new ChannelUID(getThing().getUID(),  CHANNEL_SHADE), UpDownType.DOWN);
									updateState(new ChannelUID(getThing().getUID(),  CHANNEL_SHADE), new PercentType(0));
								}
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
	        	getThing().setStatus(ThingStatus.OFFLINE);
		}

		@Override
		public void onDeviceAdded(Device device) {
		       if(device.isPresent()){
		    	   getThing().setStatus(ThingStatus.ONLINE);
		    	   onDeviceStateInitial(device);
		       } else{
		    	   onDeviceRemoved(device);
		       }
		        	     		
		}
		
		private void onDeviceStateInitial(Device device){
			if(device != null){
				logger.debug("initial channel update");
				updateState(new ChannelUID(getThing().getUID(),  CHANNEL_SHADE), 
						new PercentType(fromValueToPercent(device.getOutputValue(), device.getMaxOutPutValue())));

				//nötig oder passiert das von selbst
				if(device.isOpen()) {
					updateState(new ChannelUID(getThing().getUID(),  CHANNEL_SHADE), UpDownType.UP); 
				} else {
					updateState(new ChannelUID(getThing().getUID(),  CHANNEL_SHADE), UpDownType.DOWN);
				}
			}
		}

		@Override
		public synchronized void onSceneConfigAdded(short sceneId, Device device){
			//TODO: save DeviceSceneSpec persistent to Thing
			String saveScene = "";
			DeviceSceneSpec sceneSpec = device.getSceneConfig(sceneId);
			if(sceneSpec != null){
				saveScene = sceneSpec.toString();
			}
			
			int sceneValue = device.getSceneOutputValue(sceneId);
			if(sceneValue != -1){
				saveScene = saveScene + ", sceneValue: " +sceneValue;
			}
			if(!saveScene.isEmpty()){
				logger.debug("Save scene configuration: [{}] to thing with UID {}",saveScene,this.getThing().getUID());
				this.getThing().setProperty(DigitalSTROMBindingConstants.DEVICE_SCENE+sceneId, saveScene);
			}
		}
		
		private void saveConfigSceneSpecificationIntoDevice(Device device){
			//TODO: get persistence saved DeviceSceneSpec from Thing and save it in the Device, must call after Bride is added to ThingHandler
			Map<String, String> propertries = this.getThing().getProperties();
			String sceneSave;
			for(short i = 0; i < 128 ; i++){
				sceneSave = propertries.get(DigitalSTROMBindingConstants.DEVICE_SCENE + i);
				if(sceneSave != null && !sceneSave.isEmpty()){
					logger.debug("Find saved scene configuration for scene id " + i);
					String[] sceneParm = sceneSave.replace(" ", "").split(",");
					JSONDeviceSceneSpecImpl sceneSpecNew = null;
					for(int j = 0 ; j < sceneParm.length ; j++){
						System.out.println(sceneParm[j]);
						String[] sceneParmSplit = sceneParm[j].split(":");
						switch(sceneParmSplit[0]){
							case "Scene":
								sceneSpecNew = new JSONDeviceSceneSpecImpl(sceneParmSplit[1]);
								break;
							case "dontcare":
								sceneSpecNew.setDontcare(Boolean.parseBoolean(sceneParmSplit[1]));
								break;
							case "localPrio":
								sceneSpecNew.setLocalPrio(Boolean.parseBoolean(sceneParmSplit[1]));
								break;
							case "specialMode":
								sceneSpecNew.setSpecialMode(Boolean.parseBoolean(sceneParmSplit[1]));
								break;
							case "sceneValue":
								logger.debug("Saved sceneValue {} for scene id {} into device with dsid {}",sceneParmSplit[1], i, device.getDSID().getValue());;
								device.setSceneOutputValue(i, Integer.parseInt(sceneParmSplit[1]));
								break;
						}
					}
					if(sceneSpecNew != null){
						logger.debug("Saved sceneConfig: [{}] for scene id {} into device with dsid {}",sceneSpecNew.toString(), i, device.getDSID().getValue());;
						device.addSceneConfig(i, sceneSpecNew);
					}
				}
				
			}
			
		}


	

}
