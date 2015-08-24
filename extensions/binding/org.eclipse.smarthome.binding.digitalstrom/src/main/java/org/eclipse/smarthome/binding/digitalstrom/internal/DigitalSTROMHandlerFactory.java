/**
 * Copyright (c) 2014 openHAB UG (haftungsbeschraenkt) and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.smarthome.binding.digitalstrom.internal;

import static org.eclipse.smarthome.binding.digitalstrom.DigitalSTROMBindingConstants.*;

import java.util.HashMap;
import java.util.Hashtable;
import java.util.Map;
import java.util.Set;

import org.eclipse.smarthome.binding.digitalstrom.DigitalSTROMBindingConstants;
import org.eclipse.smarthome.binding.digitalstrom.handler.DsDeviceHandler;
import org.eclipse.smarthome.binding.digitalstrom.handler.DsSceneHandler;
import org.eclipse.smarthome.binding.digitalstrom.handler.DssBridgeHandler;
import org.eclipse.smarthome.binding.digitalstrom.internal.digitalSTROMLibary.digitalSTROMManager.DigitalSTROMConnectionManager;
import org.eclipse.smarthome.binding.digitalstrom.internal.digitalSTROMLibary.digitalSTROMManager.impl.DigitalSTROMConnectionManagerImpl;
import org.eclipse.smarthome.binding.digitalstrom.internal.discovery.DsDeviceDiscoveryService;
import org.eclipse.smarthome.binding.digitalstrom.internal.discovery.DsSceneDiscoveryService;
import org.eclipse.smarthome.config.core.Configuration;
import org.eclipse.smarthome.config.discovery.DiscoveryService;
import org.eclipse.smarthome.core.thing.Bridge;
import org.eclipse.smarthome.core.thing.Thing;
import org.eclipse.smarthome.core.thing.ThingTypeUID;
import org.eclipse.smarthome.core.thing.ThingUID;
import org.eclipse.smarthome.core.thing.binding.BaseThingHandlerFactory;
import org.eclipse.smarthome.core.thing.binding.ThingHandler;
import org.eclipse.smarthome.core.thing.binding.ThingTypeProvider;
import org.osgi.framework.ServiceRegistration;

import com.google.common.collect.Sets;

/**
 * The {@link DigitalSTROMHandlerFactory} is responsible for creating things and thing
 * handlers.
 *
 * @author Alex Maier - Initial contribution
 * @author Michael Ochel - Initial contribution
 * @author Mathias Siegele - Initial contribution
 *
 */
public class DigitalSTROMHandlerFactory extends BaseThingHandlerFactory {

    private Map<ThingUID, ServiceRegistration<?>> discoveryServiceRegs = new HashMap<>();

    public final static Set<ThingTypeUID> SUPPORTED_THING_TYPES = Sets.union(DsSceneHandler.SUPPORTED_THING_TYPES,
            Sets.union(DssBridgeHandler.SUPPORTED_THING_TYPES, DsDeviceHandler.SUPPORTED_THING_TYPES));

    private DigitalSTROMConnectionManager connMan = null;

    @Override
    public boolean supportsThingType(ThingTypeUID thingTypeUID) {
        return SUPPORTED_THING_TYPES.contains(thingTypeUID);
    }

    @Override
    public Thing createThing(ThingTypeUID thingTypeUID, Configuration configuration, ThingUID thingUID,
            ThingUID bridgeUID) {
        if (DssBridgeHandler.SUPPORTED_THING_TYPES.contains(thingTypeUID)) {
            ThingUID digitalStromUID = getBridgeThingUID(thingTypeUID, thingUID, configuration);
            System.out.println("digitalStromUID: " + digitalStromUID);
            return super.createThing(thingTypeUID, configuration, digitalStromUID, null);
        }

        if (DsDeviceHandler.SUPPORTED_THING_TYPES.contains(thingTypeUID)) {
            ThingUID dsDeviceUID = getDeviceUID(thingTypeUID, thingUID, configuration, bridgeUID);
            return super.createThing(thingTypeUID, configuration, dsDeviceUID, bridgeUID);
        }

        if (DsSceneHandler.SUPPORTED_THING_TYPES.contains(thingTypeUID)) {
            ThingUID dssSceneUID = getSceneUID(thingTypeUID, thingUID, configuration, bridgeUID);
            return super.createThing(thingTypeUID, configuration, dssSceneUID, bridgeUID);
        }

        throw new IllegalArgumentException(
                "The thing type " + thingTypeUID + " is not supported by the DigitalSTROM binding.");

    }

    @Override
    protected ThingHandler createHandler(Thing thing) {

        ThingTypeUID thingTypeUID = thing.getThingTypeUID();

        if (thingTypeUID == null)
            return null;

        if (DssBridgeHandler.SUPPORTED_THING_TYPES.contains(thing.getThingTypeUID())) {
            DssBridgeHandler handler = new DssBridgeHandler((Bridge) thing, connMan);

            registerServices(handler);
            return handler;
        }

        if (DsDeviceHandler.SUPPORTED_THING_TYPES.contains(thing.getThingTypeUID())) {

            return new DsDeviceHandler(thing);
        }

        if (DsSceneHandler.SUPPORTED_THING_TYPES.contains(thing.getThingTypeUID())) {
            return new DsSceneHandler(thing);
        }

        return null;
    }

    private void registerServices(DssBridgeHandler handler) {
        if (handler != null) {
            DsDeviceDiscoveryService deviceDiscoveryService = new DsDeviceDiscoveryService(handler);

            DsSceneDiscoveryService sceneDiscoveryService = new DsSceneDiscoveryService(handler);
            DigitalSTROMThingTypeProvider thingTypeProvider = new DigitalSTROMThingTypeProvider();
            handler.registerThingTypeProvider(thingTypeProvider);

            deviceDiscoveryService.activate();
            sceneDiscoveryService.activate();
            this.discoveryServiceRegs.put(handler.getThing().getUID(), bundleContext.registerService(
                    DiscoveryService.class.getName(), deviceDiscoveryService, new Hashtable<String, Object>()));
            this.discoveryServiceRegs.put(handler.getThing().getUID(), bundleContext.registerService(
                    DiscoveryService.class.getName(), sceneDiscoveryService, new Hashtable<String, Object>()));
            this.discoveryServiceRegs.put(handler.getThing().getUID(), bundleContext.registerService(
                    ThingTypeProvider.class.getName(), thingTypeProvider, new Hashtable<String, Object>()));
        }
    }

    private ThingUID getDeviceUID(ThingTypeUID thingTypeUID, ThingUID thingUID, Configuration configuration,
            ThingUID bridgeUID) {
        String deviceId = configuration.get(DEVICE_DSID).toString();
        if (thingUID == null) {
            thingUID = new ThingUID(thingTypeUID, deviceId, bridgeUID.getId());
        }
        return thingUID;
    }

    private ThingUID getSceneUID(ThingTypeUID thingTypeUID, ThingUID thingUID, Configuration configuration,
            ThingUID bridgeUID) {
        String sceneId = configuration.get(DigitalSTROMBindingConstants.SCENE_ZONE_ID).toString() + "-"
                + configuration.get(DigitalSTROMBindingConstants.SCENE_GROUP_ID).toString() + "-"
                + configuration.get(DigitalSTROMBindingConstants.SCENE_ID).toString();
        if (thingUID == null) {
            thingUID = new ThingUID(thingTypeUID, sceneId, bridgeUID.getId());
        }
        return thingUID;
    }

    private ThingUID getBridgeThingUID(ThingTypeUID thingTypeUID, ThingUID thingUID, Configuration configuration) {

        if (thingUID == null) {

            String dSID;
            if (configuration.get(DS_ID) == null) {

                dSID = getDssID(configuration);

                configuration.put(DS_ID, dSID);
            } else
                dSID = configuration.get(DS_ID).toString();

            thingUID = new ThingUID(thingTypeUID, dSID);
        }
        return thingUID;
    }

    private String getDssID(Configuration configuration) {
        String dsID = null;
        if (configuration.get(HOST) != null && !configuration.get(HOST).toString().isEmpty()) {
            String applicationToken = null;

            String host = configuration.get(HOST).toString();
            String user = null;
            String pw = null;

            if (configuration.get(APPLICATION_TOKEN) != null
                    && !(applicationToken = configuration.get(APPLICATION_TOKEN).toString()).trim().isEmpty()) {
                if (checkUserPassword(configuration)) {
                    user = configuration.get(USER_NAME).toString();
                    user = configuration.get(PASSWORD).toString();
                }
            }

            this.connMan = new DigitalSTROMConnectionManagerImpl(host, user, pw, applicationToken, false);

            if (connMan.checkConnection()) {
                dsID = connMan.getDigitalSTROMAPI().getDSID(connMan.getSessionToken());
            }
        }

        return dsID;
    }

    private boolean checkUserPassword(Configuration configuration) {
        if ((configuration.get(USER_NAME) != null && configuration.get(PASSWORD) != null)
                && (!configuration.get(USER_NAME).toString().trim().isEmpty()
                        && !configuration.get(PASSWORD).toString().trim().isEmpty())) { // notwendig?
            return true;
        }
        return false;
    }
}
