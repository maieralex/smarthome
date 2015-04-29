package org.eclipse.smarthome.binding.digitalstrom.handler;

import static org.eclipse.smarthome.binding.digitalstrom.DigitalSTROMBindingConstants.THING_TYPE_GE_KL200;
import static org.eclipse.smarthome.binding.digitalstrom.DigitalSTROMBindingConstants.THING_TYPE_GE_KM200;

import java.util.List;
import java.util.Map;
import java.util.Set;

import org.eclipse.smarthome.binding.digitalstrom.DigitalSTROMBindingConstants;
import org.eclipse.smarthome.binding.digitalstrom.internal.client.constants.SceneEnum;
import org.eclipse.smarthome.binding.digitalstrom.internal.client.entity.DetailedGroupInfo;
import org.eclipse.smarthome.binding.digitalstrom.internal.client.entity.Scene;
import org.eclipse.smarthome.binding.digitalstrom.internal.client.entity.Zone;
import org.eclipse.smarthome.core.library.types.OnOffType;
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

public class DsSceneHandler extends BaseThingHandler  {

	private Logger logger = LoggerFactory.getLogger(DsSceneHandler.class);

    public final static Set<ThingTypeUID> SUPPORTED_THING_TYPES = Sets.newHashSet(THING_TYPE_GE_KM200, THING_TYPE_GE_KL200);

    DssBridgeHandler dssBridgeHandler = null;
    Short sceneId = null;
    Integer zoneID = null ;
    Short groupID = null;
    
    String sceneThingID = null;
    
	public DsSceneHandler(Thing thing) {
		super(thing);
	}

	@Override
    protected void bridgeHandlerInitialized(ThingHandler thingHandler, Bridge bridge) {
    	String configZoneID = getConfig().get(DigitalSTROMBindingConstants.SCENE_ZONE_ID).toString().toLowerCase();
    	String configGroupID = getConfig().get(DigitalSTROMBindingConstants.SCENE_GROUP_ID).toString().toLowerCase();
    	String configSceneID = getConfig().get(DigitalSTROMBindingConstants.SCENE_ID).toString().toLowerCase();
    			
    	if (!configSceneID.isEmpty()) {
    		this.sceneId = Short.parseShort(configSceneID);
            
    		if (thingHandler instanceof DssBridgeHandler) {
	        	this.dssBridgeHandler =  (DssBridgeHandler) thingHandler;
	        	//this.dssBridgeHandler.registerDeviceStatusListener(dSUID, this);
	        	
	        	// note: this call implicitly registers our handler as a listener on the bridge
	            getThing().setStatus(bridge.getStatus());
	        	logger.debug("Set status on {}", getThing().getStatus());
	        	
	        }
    		
    		Map<Integer, Zone> zoneMap = this.dssBridgeHandler.getApartment().getZoneMap();
    		
    		if(configZoneID.isEmpty()){
    			zoneID = 0;
    		} else{
    			
	    		try{
	    			zoneID = Integer.getInteger(configZoneID);
	    			if(!zoneMap.containsKey(zoneID)){
	    				zoneID = null;
	    			}
	    		} catch(NumberFormatException e){
	    			for(Integer zoneID : zoneMap.keySet()){
	    				if(zoneMap.get(zoneID).getName().toLowerCase().equals(configZoneID)){
	    					this.zoneID = zoneID;
	    					break;
	    				}
	    			}
	    			
	    			if(this.zoneID == null){
	    				logger.error("Can not found zone id or zone name {}!", configZoneID);
	    				return;
	    			}
	    		}
    		}
    		
    		if(configGroupID.isEmpty()){
    			groupID = 0;
    			
    		} else {
    			List<DetailedGroupInfo> groupList = zoneMap.get(zoneID).getGroups();
	    		try{
	    			Short tempGroupID = Short.parseShort(configGroupID);
	    			
	    			for(DetailedGroupInfo group : groupList){
	    				if(group.getGroupID() == tempGroupID){
	    					this.groupID = tempGroupID;
	    					break;
	    				}
	    			}
	    		} catch(NumberFormatException e){
	    			for(DetailedGroupInfo group : groupList){
	    				if(group.getGroupName().toLowerCase().equals(configGroupID)){
	    					this.groupID = group.getGroupID();
	    					break;
	    				}
	    			}	    			
	    		}
	    		
	    		if(this.groupID == null){
    				logger.error("Can not found group id or group name {}!", configZoneID);
    				return;
    			}
	    		
	    		this.sceneThingID = this.zoneID + ":" + this.groupID + ":" + this.sceneId;
    		}
        }
    }
	
	@Override
	public void handleCommand(ChannelUID channelUID, Command command) {
		DssBridgeHandler dssBridgeHandler = getDssBridgeHandler();
		if (dssBridgeHandler == null) {
            logger.warn("DigitalSTROM bridge handler not found. Cannot handle command without bridge.");
            return;
        }
			
		if(channelUID.getId().equals(DigitalSTROMBindingConstants.CHANNEL_SCENE)) {
			if (command instanceof OnOffType) {
				if(OnOffType.ON.equals((OnOffType) command)){
					this.dssBridgeHandler.sendSeneComandToDSS(zoneID, groupID, sceneId, true);
				} else{
					this.dssBridgeHandler.sendSeneComandToDSS(zoneID, groupID, sceneId, false);
				}
			}
		} else {
			logger.warn("Command send to an unknown channel id: " + channelUID);
		}

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
	        	//TODO: register on dsBridge
	        	//this.dssBridgeHandler.registerDeviceStatusListener(dSUID, this);
	        } else{
	        	return null;
	        }
    	}
        return this.dssBridgeHandler;
    }

}
