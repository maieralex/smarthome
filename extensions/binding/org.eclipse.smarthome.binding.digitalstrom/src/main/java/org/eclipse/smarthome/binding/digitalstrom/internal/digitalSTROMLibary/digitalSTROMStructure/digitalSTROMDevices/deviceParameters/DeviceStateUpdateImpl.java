/**
 * Copyright (c) 2014-2015 openHAB UG (haftungsbeschraenkt) and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.smarthome.binding.digitalstrom.internal.digitalSTROMLibary.digitalSTROMStructure.digitalSTROMDevices.deviceParameters;

public class DeviceStateUpdateImpl implements DeviceStateUpdate {

    private final String UPDATE_TYPE;
    private final int VALUE;

    public DeviceStateUpdateImpl(String updateType, int value) {
        this.UPDATE_TYPE = updateType;
        this.VALUE = value;
    }

    @Override
    public int getValue() {
        return VALUE;
    }

    @Override
    public String getType() {
        return UPDATE_TYPE;
    }

}
