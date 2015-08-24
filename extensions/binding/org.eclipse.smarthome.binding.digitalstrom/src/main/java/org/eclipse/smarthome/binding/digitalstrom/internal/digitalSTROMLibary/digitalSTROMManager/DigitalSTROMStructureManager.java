/**
 * Copyright (c) 2014-2015 openHAB UG (haftungsbeschraenkt) and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.smarthome.binding.digitalstrom.internal.digitalSTROMLibary.digitalSTROMManager;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.eclipse.smarthome.binding.digitalstrom.internal.digitalSTROMLibary.digitalSTROMStructure.digitalSTROMDevices.Device;
import org.eclipse.smarthome.binding.digitalstrom.internal.digitalSTROMLibary.digitalSTROMStructure.digitalSTROMDevices.deviceParameters.DSID;

/**
 *
 * @author Michael Ochel - Initial contribution
 * @author Mathias Siegele - Initial contribution
 *
 */
public interface DigitalSTROMStructureManager {

    /**
     *
     * @param connectionManager
     * @return
     */
    public boolean generateZoneGroupNames(DigitalSTROMConnectionManager connectionManager);

    /**
     *
     * @param zoneID
     * @return
     */
    public String getZoneName(int zoneID);

    public int getZoneId(String zoneName);

    /**
     *
     * @param zoneID
     * @param groupID
     * @return
     */
    public String getZoneGroupName(int zoneID, short groupID);

    public short getZoneGroupId(String zoneName, String groupName);

    /**
     *
     * @return
     */
    public Map<DSID, Device> getDeviceMap();

    /**
     *
     * @return
     */
    public Map<DSID, Device> getDeviceHashMapReference();

    /**
     *
     * @return
     */
    public Map<Integer, HashMap<Short, List<Device>>> getStructureReference();

    /**
     *
     * @param zoneID
     * @return
     */
    public HashMap<Short, List<Device>> getGroupsFromZoneX(int zoneID);

    /**
     *
     * @param zoneID
     * @param groupID
     * @return
     */
    public List<Device> getReferenceDeviceListFromZoneXGroupX(int zoneID, short groupID);

    /**
     *
     * @param dSID
     * @return
     */
    public Device getDeviceByDSID(String dSID);

    /**
     *
     * @param dSID
     * @return
     */
    public Device getDeviceByDSID(DSID dSID);

    /**
     *
     * @param dSUID
     * @return
     */
    public Device getDeviceByDSUID(String dSUID);

    /**
     *
     * @param oldZone
     * @param oldGroups
     * @param device
     */
    public void updateDevice(int oldZone, List<Short> oldGroups, Device device);

    /**
     *
     * @param device
     */
    public void updateDevice(Device device);

    /**
     *
     * @param device
     */
    public void deleteDevice(Device device);

    /**
     *
     * @param device
     */
    public void addDeviceToStructure(Device device);

    /**
     *
     * @return
     */
    public Set<Integer> getZoneIDs();

    public boolean checkZoneID(int zoneID);

    public boolean checkZoneGroupID(int zoneID, short groupID);

}
