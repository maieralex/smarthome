package org.eclipse.smarthome.binding.digitalstrom.internal.discovery;

import static org.eclipse.smarthome.binding.digitalstrom.DigitalSTROMBindingConstants.*;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.eclipse.smarthome.binding.digitalstrom.DigitalSTROMBindingConstants;
import org.eclipse.smarthome.binding.digitalstrom.handler.DsDeviceHandler;
import org.eclipse.smarthome.binding.digitalstrom.handler.DssBridgeHandler;
import org.eclipse.smarthome.binding.digitalstrom.internal.digitalSTROMLibary.digitalSTROMListener.DeviceStatusListener;
import org.eclipse.smarthome.binding.digitalstrom.internal.digitalSTROMLibary.digitalSTROMStructure.digitalSTROMDevices.Device;
import org.eclipse.smarthome.binding.digitalstrom.internal.digitalSTROMLibary.digitalSTROMStructure.digitalSTROMDevices.deviceParameters.ChangeableDeviceConfigEnum;
import org.eclipse.smarthome.binding.digitalstrom.internal.digitalSTROMLibary.digitalSTROMStructure.digitalSTROMDevices.deviceParameters.DeviceStateUpdate;
import org.eclipse.smarthome.config.discovery.AbstractDiscoveryService;
import org.eclipse.smarthome.config.discovery.DiscoveryResult;
import org.eclipse.smarthome.config.discovery.DiscoveryResultBuilder;
import org.eclipse.smarthome.core.thing.ThingTypeUID;
import org.eclipse.smarthome.core.thing.ThingUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The {@link DsDeviceDiscoveryService} discovered all DigitalSTROM-Devices
 * which are be able to add them to the ESH-Inbox.
 *
 * @author Michael Ochel
 * @author Matthias Siegele
 */
public class DsDeviceDiscoveryService extends AbstractDiscoveryService implements DeviceStatusListener {

    private final static Logger logger = LoggerFactory.getLogger(DsDeviceDiscoveryService.class);

    private DssBridgeHandler digitalSTROMBridgeHandler;

    /**
     * Creates a new {@link DsDeviceDiscoveryService}.
     *
     * @param digitalSTROMBridgeHandler
     * @throws IllegalArgumentException
     */
    public DsDeviceDiscoveryService(DssBridgeHandler digitalSTROMBridgeHandler) throws IllegalArgumentException {
        super(5);
        this.digitalSTROMBridgeHandler = digitalSTROMBridgeHandler;
    }

    /**
     * Activate the {@link DsDeviceDiscoveryService}.
     */
    public void activate() {
        if (digitalSTROMBridgeHandler != null) {
            digitalSTROMBridgeHandler.registerDeviceStatusListener(this);
        }
        // this.startScan();
    }

    /**
     * Deactivate the {@link DsDeviceDiscoveryService}.
     */
    @Override
    public void deactivate() {
        if (digitalSTROMBridgeHandler != null) {
            digitalSTROMBridgeHandler.unregisterDeviceStatusListener(this);
        }
    }

    @Override
    protected void startScan() {
        if (digitalSTROMBridgeHandler.getDevices() != null) {
            for (Device device : digitalSTROMBridgeHandler.getDevices()) {
                onDeviceAddedInternal(device);
            }
        }
    }

    @Override
    public Set<ThingTypeUID> getSupportedThingTypes() {
        return DsDeviceHandler.SUPPORTED_THING_TYPES;// union auf alle!
    }

    private void onDeviceAddedInternal(Device device) {
        if (device.isDeviceWithOutput()) {
            ThingUID thingUID = getThingUID(device);
            if (thingUID != null) {
                ThingUID bridgeUID = digitalSTROMBridgeHandler.getThing().getUID();
                Map<String, Object> properties = new HashMap<>(9);
                properties.put(DEVICE_UID, device.getDSUID());
                properties.put(DigitalSTROMBindingConstants.DEVICE_DSID, device.getDSID().getValue());
                properties.put(DigitalSTROMBindingConstants.DEVICE_HW_INFO, device.getHWinfo());
                properties.put(DigitalSTROMBindingConstants.DEVICE_GROUPS, device.getGroups().toString());
                properties.put(DigitalSTROMBindingConstants.DEVICE_OUTPUT_MODE, device.getOutputMode());
                properties.put(DigitalSTROMBindingConstants.DEVICE_ZONE_ID, device.getZoneId());
                properties.put(DigitalSTROMBindingConstants.DEVICE_FUNCTIONAL_COLOR_GROUP,
                        device.getFunctionalColorGroup());
                properties.put(DigitalSTROMBindingConstants.DEVICE_METER_ID, device.getMeterDSID().getValue());
                if (device.getName() != null) {
                    properties.put(DEVICE_NAME, device.getName());
                } else {
                    properties.put(DEVICE_NAME, device.getDSID().getValue());
                }
                DiscoveryResult discoveryResult = DiscoveryResultBuilder.create(thingUID).withProperties(properties)
                        .withBridge(bridgeUID).withLabel(device.getName()).build();

                thingDiscovered(discoveryResult);
            } else {
                logger.debug("discovered unsupported device hardware type '{}' with uid {}", device.getHWinfo(),
                        device.getDSUID());
            }
        } else {
            // ggf. mögliche outputvalues hinzufügen
            logger.debug(
                    "discovered device without output value, don't add to inbox. "
                            + "Device information: hardware info: {}, dSUID: {}, device-name: {}, output value: {}",
                    device.getHWinfo(), device.getDSUID(), device.getName(), device.getOutputMode());
        }
    }

    private ThingUID getThingUID(Device device) {
        ThingUID bridgeUID = digitalSTROMBridgeHandler.getThing().getUID();
        // TODO: ggf. outputmode beachten
        ThingTypeUID thingTypeUID = new ThingTypeUID(BINDING_ID, device.getHWinfo());

        if (getSupportedThingTypes().contains(thingTypeUID)) {
            String thingDeviceId = device.getDSUID();
            ThingUID thingUID = new ThingUID(thingTypeUID, bridgeUID, thingDeviceId);
            return thingUID;
        } else {
            return null;
        }
    }

    @Override
    public void onDeviceStateChanged(DeviceStateUpdate deviceStateUpdate) {
        // nothing to do
    }

    @Override
    public void onDeviceRemoved(Device device) {
        ThingUID thingUID = getThingUID(device);

        if (thingUID != null) {
            thingRemoved(thingUID);
        }
    }

    @Override
    public void onDeviceAdded(Device device) {
        onDeviceAddedInternal(device);
    }

    @Override
    public void onSceneConfigAdded(short sceneId) {
        // nothing to do

    }

    @Override
    public void onDeviceConfigChanged(ChangeableDeviceConfigEnum whichConfig) {
        // nothing to do

    }

    @Override
    public String getID() {
        return DeviceStatusListener.DEVICE_DESCOVERY;
    }

}
