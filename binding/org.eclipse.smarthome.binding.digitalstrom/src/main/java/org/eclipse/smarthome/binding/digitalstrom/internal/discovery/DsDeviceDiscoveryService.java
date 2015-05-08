package org.eclipse.smarthome.binding.digitalstrom.internal.discovery;

import static org.eclipse.smarthome.binding.digitalstrom.DigitalSTROMBindingConstants.BINDING_ID;
import static org.eclipse.smarthome.binding.digitalstrom.DigitalSTROMBindingConstants.DEVICE_NAME;
import static org.eclipse.smarthome.binding.digitalstrom.DigitalSTROMBindingConstants.DEVICE_UID;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.eclipse.smarthome.binding.digitalstrom.DigitalSTROMBindingConstants;
import org.eclipse.smarthome.binding.digitalstrom.handler.DeviceStatusListener;
import org.eclipse.smarthome.binding.digitalstrom.handler.DsDeviceHandler;
import org.eclipse.smarthome.binding.digitalstrom.handler.DssBridgeHandler;
import org.eclipse.smarthome.binding.digitalstrom.internal.client.entity.Device;
import org.eclipse.smarthome.config.discovery.AbstractDiscoveryService;
import org.eclipse.smarthome.config.discovery.DiscoveryResult;
import org.eclipse.smarthome.config.discovery.DiscoveryResultBuilder;
import org.eclipse.smarthome.core.thing.ThingTypeUID;
import org.eclipse.smarthome.core.thing.ThingUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DsDeviceDiscoveryService extends AbstractDiscoveryService implements DeviceStatusListener{

	private final static Logger logger = LoggerFactory.getLogger(DsDeviceDiscoveryService.class);
			
	private DssBridgeHandler digitalSTROMBridgeHandler;
	
	public DsDeviceDiscoveryService(DssBridgeHandler digitalSTROMBridgeHandler)
			throws IllegalArgumentException {
		super(5);
		this.digitalSTROMBridgeHandler = digitalSTROMBridgeHandler;
	}

	public void activate() {
		digitalSTROMBridgeHandler.registerDeviceStatusListener(DeviceStatusListener.DEVICE_DESCOVERY, this);
		//this.startScan();
    }

    public void deactivate() {
    	digitalSTROMBridgeHandler.unregisterDeviceStatusListener(DeviceStatusListener.DEVICE_DESCOVERY);
    }

	//später ändern auf Struktur mit group-things
	@Override
	protected void startScan() {
		for(Device device : digitalSTROMBridgeHandler.getDevices()){
			onDeviceAddedInternal(device);
		}	
	}

	@Override
	public Set<ThingTypeUID> getSupportedThingTypes() {
		return DsDeviceHandler.SUPPORTED_THING_TYPES;//union auf alle!
	}
	
    private void onDeviceAddedInternal(Device device) {
        ThingUID thingUID = getThingUID(device);
		if(thingUID!=null) {
			ThingUID bridgeUID = digitalSTROMBridgeHandler.getThing().getUID();
	        Map<String, Object> properties = new HashMap<>(7);
	        properties.put(DEVICE_UID, device.getDSUID());
	        properties.put(DigitalSTROMBindingConstants.DEVICE_DSID, device.getDSID().getValue());
	        properties.put(DigitalSTROMBindingConstants.DEVICE_HW_INFO, device.getHWinfo());
	        properties.put(DigitalSTROMBindingConstants.DEVICE_GROUPS, device.getGroups().toString());
	        properties.put(DigitalSTROMBindingConstants.DEVICE_OUTPUT_MODE, device.getOutputMode());
	        properties.put(DigitalSTROMBindingConstants.DEVICE_ZONE_ID, device.getZoneId());
	        if(device.getName() != null){
	        	properties.put(DEVICE_NAME, device.getName());
	        } else{
	        	properties.put(DEVICE_NAME, device.getDSID().getValue());
	        }
	        DiscoveryResult discoveryResult = DiscoveryResultBuilder.create(thingUID)
	        		.withProperties(properties)
	        		.withBridge(bridgeUID)
	        		.withLabel(device.getName())
	        		.build();
	        
	        thingDiscovered(discoveryResult);
		} else {
			logger.debug("discovered unsupported device hardware type '{}' with uid {}", device.getHWinfo(), device.getDSUID());
		}
    }
    
	private ThingUID getThingUID(Device device) {
        ThingUID bridgeUID = digitalSTROMBridgeHandler.getThing().getUID();
        //TODO: ggf. outputmode beachten
		ThingTypeUID thingTypeUID = new ThingTypeUID(BINDING_ID, device.getHWinfo());
				
		if(getSupportedThingTypes().contains(thingTypeUID)) {
		    String thingDeviceId = device.getDSUID();
		    ThingUID thingUID = new ThingUID(thingTypeUID, bridgeUID, thingDeviceId);
			return thingUID;
		} else {
			return null;
		}
	}


	@Override
	public void onDeviceStateChanged(Device device) {
		//nothing to do
	}
	
	
	@Override
	public void onDeviceRemoved(Device device) {
		ThingUID thingUID = getThingUID(device);
	    
		if(thingUID!=null) {
			thingRemoved(thingUID);
		}
	}


	@Override
	public void onDeviceAdded(Device device) {
		onDeviceAddedInternal(device);		
	}

	@Override
	public void onSceneConfigAdded(short sceneId, Device device) {
		//nothing to do
		
	}

	
}
