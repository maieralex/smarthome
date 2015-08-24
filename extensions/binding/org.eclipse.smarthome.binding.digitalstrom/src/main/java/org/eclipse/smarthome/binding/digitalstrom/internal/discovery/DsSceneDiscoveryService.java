package org.eclipse.smarthome.binding.digitalstrom.internal.discovery;

import static org.eclipse.smarthome.binding.digitalstrom.DigitalSTROMBindingConstants.*;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.eclipse.smarthome.binding.digitalstrom.handler.DsSceneHandler;
import org.eclipse.smarthome.binding.digitalstrom.handler.DssBridgeHandler;
import org.eclipse.smarthome.binding.digitalstrom.internal.digitalSTROMLibary.digitalSTROMListener.SceneStatusListener;
import org.eclipse.smarthome.binding.digitalstrom.internal.digitalSTROMLibary.digitalSTROMStructure.digitalSTROMScene.InternalScene;
import org.eclipse.smarthome.config.discovery.AbstractDiscoveryService;
import org.eclipse.smarthome.config.discovery.DiscoveryResult;
import org.eclipse.smarthome.config.discovery.DiscoveryResultBuilder;
import org.eclipse.smarthome.core.thing.ThingTypeUID;
import org.eclipse.smarthome.core.thing.ThingUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The {@link DsSceneDiscoveryService} discovered all DigitalSTROM-Devices
 * which are be able to add them to the ESH-Inbox.
 *
 * @author Michael Ochel
 * @author Matthias Siegele
 */
public class DsSceneDiscoveryService extends AbstractDiscoveryService implements SceneStatusListener {

    private final static Logger logger = LoggerFactory.getLogger(DsSceneDiscoveryService.class);

    private DssBridgeHandler digitalSTROMBridgeHandler;

    /**
     * Creates a new {@link DsSceneDiscoveryService}.
     *
     * @param digitalSTROMBridgeHandler
     * @throws IllegalArgumentException
     */
    public DsSceneDiscoveryService(DssBridgeHandler digitalSTROMBridgeHandler) throws IllegalArgumentException {
        super(5);
        this.digitalSTROMBridgeHandler = digitalSTROMBridgeHandler;
    }

    /**
     * Activate the {@link DsSceneDiscoveryService}.
     */
    public void activate() {
        digitalSTROMBridgeHandler.registerSceneStatusListener(this);
        // this.startScan();
    }

    /**
     * Deactivate the {@link DsSceneDiscoveryService}.
     */
    @Override
    public void deactivate() {
        digitalSTROMBridgeHandler.unregisterSceneStatusListener(this);
    }

    @Override
    protected void startScan() {
        if (digitalSTROMBridgeHandler.getScenes() != null) {
            for (InternalScene scene : digitalSTROMBridgeHandler.getScenes()) {
                onSceneAddedInternal(scene);
            }
        }
    }

    @Override
    public Set<ThingTypeUID> getSupportedThingTypes() {
        return DsSceneHandler.SUPPORTED_THING_TYPES;// union auf alle!
    }

    private void onSceneAddedInternal(InternalScene scene) {
        if (scene != null) {
            ThingUID thingUID = getThingUID(scene);
            if (thingUID != null) {
                ThingUID bridgeUID = digitalSTROMBridgeHandler.getThing().getUID();
                Map<String, Object> properties = new HashMap<>(4);
                properties.put(SCENE_NAME, scene.getSceneName());
                properties.put(SCENE_ZONE_ID, scene.getZoneID());
                properties.put(SCENE_GROUP_ID, scene.getGroupID());
                properties.put(SCENE_ID, scene.getSceneID());
                DiscoveryResult discoveryResult = DiscoveryResultBuilder.create(thingUID).withProperties(properties)
                        .withBridge(bridgeUID).withLabel(scene.getSceneName()).build();

                thingDiscovered(discoveryResult);

            } else {
                logger.debug("discovered unsupported scene: name '{}' with id {}", scene.getSceneName(), scene.getID());
            }
        }

    }

    private ThingUID getThingUID(InternalScene scene) {
        ThingUID bridgeUID = digitalSTROMBridgeHandler.getThing().getUID();
        ThingTypeUID thingTypeUID = new ThingTypeUID(BINDING_ID, THING_TYPE_ID_SCENE);

        if (getSupportedThingTypes().contains(thingTypeUID)) {
            String thingSceneId = scene.getID();
            ThingUID thingUID = new ThingUID(thingTypeUID, bridgeUID, thingSceneId);
            return thingUID;
        } else {
            return null;
        }
    }

    @Override
    public String getID() {
        return SceneStatusListener.SCENE_DESCOVERY;
    }

    @Override
    public void onSceneStateChanged(boolean flag) {
        // nothing to do
    }

    @Override
    public void onSceneRemoved(InternalScene scene) {
        ThingUID thingUID = getThingUID(scene);

        if (thingUID != null) {
            thingRemoved(thingUID);
        }

    }

    @Override
    public void onSceneAdded(InternalScene scene) {
        onSceneAddedInternal(scene);
    }

}
