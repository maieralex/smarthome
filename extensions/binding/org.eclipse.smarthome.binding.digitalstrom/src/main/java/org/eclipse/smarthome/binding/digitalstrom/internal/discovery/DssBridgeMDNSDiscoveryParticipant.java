/**
 * Copyright (c) 2014-2015 openHAB UG (haftungsbeschraenkt) and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.smarthome.binding.digitalstrom.internal.discovery;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import javax.jmdns.ServiceInfo;

import org.eclipse.smarthome.binding.digitalstrom.DigitalSTROMBindingConstants;
import org.eclipse.smarthome.binding.digitalstrom.internal.digitalSTROMLibary.digitalSTROMConfiguration.DigitalSTROMConfig;
import org.eclipse.smarthome.binding.digitalstrom.internal.digitalSTROMLibary.digitalSTROMServerConnection.DigitalSTROMAPI;
import org.eclipse.smarthome.binding.digitalstrom.internal.digitalSTROMLibary.digitalSTROMServerConnection.impl.DigitalSTROMJSONImpl;
import org.eclipse.smarthome.config.discovery.DiscoveryResult;
import org.eclipse.smarthome.config.discovery.DiscoveryResultBuilder;
import org.eclipse.smarthome.core.thing.ThingTypeUID;
import org.eclipse.smarthome.core.thing.ThingUID;
import org.eclipse.smarthome.io.transport.mdns.discovery.MDNSDiscoveryParticipant;
import org.eclipse.smarthome.io.transport.mdns.discovery.MDNSDiscoveryService;
import org.slf4j.LoggerFactory;

/**
 * The {@link DssBridgeMDNSDiscoveryParticipant} is responsible for discovering new and
 * removed DigitalSTROM bridges. It uses the central {@link MDNSDiscoveryService}.
 *
 * @author Michael Ochel
 * @author Matthias Siegele
 *
 */
public class DssBridgeMDNSDiscoveryParticipant implements MDNSDiscoveryParticipant {

    private org.slf4j.Logger logger = LoggerFactory.getLogger(DssBridgeMDNSDiscoveryParticipant.class);

    private String hostAdress = null;
    private String dsid = null;

    @Override
    public Set<ThingTypeUID> getSupportedThingTypeUIDs() {
        return Collections.singleton(DigitalSTROMBindingConstants.THING_TYPE_DSS_BRIDGE);
    }

    @Override
    public String getServiceType() {
        // TODO Auto-generated method stub
        return "_tcp.local.";
    }

    @Override
    public DiscoveryResult createResult(ServiceInfo service) {
        ThingUID uid = getThingUID(service);
        if (uid != null) {
            Map<String, Object> properties = new HashMap<>(2);
            properties.put(DigitalSTROMBindingConstants.HOST, hostAdress);
            properties.put(DigitalSTROMBindingConstants.DS_ID, dsid);

            DiscoveryResult result = DiscoveryResultBuilder.create(uid).withProperties(properties)
                    .withLabel("DigitalSTROM-Server").build();
            return result;
        } else {
            return null;
        }
    }

    @Override
    public ThingUID getThingUID(ServiceInfo service) {
        logger.debug("URL: " + service.getName() + "." + service.getDomain() + ".");
        if (service.getApplication().contains("dssweb")) {
            hostAdress = service.getName() + "." + service.getDomain() + ".";
            DigitalSTROMAPI digitalSTROMClient = new DigitalSTROMJSONImpl(hostAdress,
                    DigitalSTROMConfig.DEFAULT_CONNECTION_TIMEOUT, DigitalSTROMConfig.DEFAULT_READ_TIMEOUT);
            dsid = digitalSTROMClient.getDSID("123");
            return new ThingUID(DigitalSTROMBindingConstants.THING_TYPE_DSS_BRIDGE, dsid);
        }
        return null;
    }

}
