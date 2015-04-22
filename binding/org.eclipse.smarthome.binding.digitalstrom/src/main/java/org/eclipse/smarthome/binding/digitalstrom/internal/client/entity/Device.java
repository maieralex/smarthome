/**
 * Copyright (c) 2010-2014, openHAB.org and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.smarthome.binding.digitalstrom.internal.client.entity;

import java.util.List;

import org.eclipse.smarthome.binding.digitalstrom.internal.client.constants.OutputModeEnum;
import org.eclipse.smarthome.binding.digitalstrom.internal.client.events.DeviceListener;



/**
 * The {@link Device} represents a DigitalSTROM device in ESH.
 * 
 * @author 	Alexander Betker - Initial contribution
 * @since 1.3.0
 * @author Michael Ochel - add methods for ESH and JavaDoc
 * @author Mathias Siegele - add methods for ESH and JavaDoc
 */
public interface Device {
	
	/**
	 * Returns the dSID of this device.
	 * @return {@link DSID} dSID 
	 */
	public DSID getDSID();
	
	/**
	 * Returns the dSUID of this device.
	 * @return dSID
	 */
	public String getDSUID();
	
	/**
	 * Returns the hardware info of this device. 
	 * You can see all available hardware info here {@link http://www.digitalstrom.com/Partner/Support/Techn-Dokumentation/}
	 *   
	 * @return hardware info
	 */
	public String getHWinfo();
	
	/**
	 * Returns the user defined name of this device.
	 * 
	 * @return name of this device
	 */
	public String getName();
	
	/**
	 * Sets the name of this device;
	 * 
	 * @param new name for this device
	 */
	public void setName(String name);
	
	/**
	 * Returns the zone id in which the device is.
	 *  
	 * @return zone id
	 */
	public int getZoneId();
	
	/**
	 * This device is available in his zone or not.
	 * Every 24h the dSM (meter) checks, if the devices are
	 * plugged in
	 * 
	 * @return	true, if device is available otherwise false
	 */
	public boolean isPresent();
	
	/**
	 * Set this device is available in his zone or not.
	 * 
	 * @param isPresent (true = available | false = not available)
	 */
	public void setIsPresent(boolean isPresent);
	
	/**
	 * Returns true if this device is on otherwise false.
	 * 
	 * @return is on (true = on | false = off)
	 */
	public boolean isOn();
	
	/**
	 * Set this device on if the flag is true or off if it is false.
	 * 
	 * @param flag (true = on | false = off)
	 */
	public void setIsOn(boolean flag);
	
	/**
	 * Return true if this device is dimmable, otherwise false. 
	 * 
	 * @return is dimmable (true = yes | false = no)
	 */
	public boolean isDimmable();
	
	/**
	 * Returns true if this device is a shade device (grey), otherwise false.
	 *  
	 * @return is shade (true = yes | false = no)
	 */
	public boolean isRollershutter();
	
	/**
	 * There are different modes for devices (for example: 
	 * a device can be in dim mode or not). Please have
	 * a look at the name of this enum (a little bit self-explaining)
	 * 
	 * @return	the current mode of this device
	 */
	public OutputModeEnum getOutputMode();
	
	
	public void increase();
	
	public void decrease();
	
	
	public int getSlatPosition();
	
	/**
	 * 
	 * @param position
	 */
	public void setSlatPosition(int position);
	
	public int getMaxSlatPosition();
	
	public int getMinSlatPosition();
	
		
	public int getOutputValue();
	
	public void setOutputValue(int value);
	
	public int getMaxOutPutValue();
	
	
	public int getPowerConsumption();
	
	/**
	 * current power consumption in watt
	 * @param powerConsumption in w
	 */
	public void setPowerConsumption(int powerConsumption);
	
	/**
	 * to get the energy meter value of this device
	 * @return	energy meter value in wh
	 */
	public int getEnergyMeterValue();
	
	/**
	 * set the energy meter value of this device
	 * 
	 * @param energy meter value in wh
	 */
	public void setEnergyMeterValue(int value);
	
	/**
	 * amperage of this device
	 * 
	 * @return	electric meter value in mA 
	 */
	public int getElectricMeterValue();
	
	/**
	 * set the amperage of this device
	 * 
	 * @param electric meter value in mA
	 */
	public void setElectricMeterValue(int electricMeterValue);
	
	/**
	 * Return a list with group id's in which the device is
	 * 
	 * @return List of group id's
	 */
	public List<Short> getGroups();
	
	
	public int getSceneOutputValue(short sceneId);
	
	
	public void setSceneOutputValue(short sceneId, int sceneValue);
	
	/**
	 * This configuration is very important. The devices can
	 * be configured to not react to some commands (scene calls).
	 * So you can't imply that a device automatically turns on (by default yes,
	 * but if someone configured his own scenes, then maybe not) after a
	 * scene call. This method returns true or false, if the configuration 
	 * for this sceneID already has been read
	 * 
	 * @param sceneId	the sceneID
	 * @return			true if this device has the config for this specific scene
	 */
	public boolean containsSceneConfig(short sceneId);
	
	/**
	 * Add the config for this scene. The config has the configuration
	 * for the specific sceneID.
	 * 
	 * @param sceneId	scene call id
	 * @param sceneSpec	config for this sceneID
	 */
	public void addSceneConfig(short sceneId, DeviceSceneSpec sceneSpec);
	
	/**
	 * Should the device react on this scene call or not 
	 * @param sceneId	scene call id
	 * @return			true, if this device should react on this sceneID
	 */
	public boolean doIgnoreScene(short sceneId);
	
	/**
	 * To get notifications if something happens
	 * (for example a new metering value)
	 * 
	 * @param listener
	 */
	public void addDeviceListener(DeviceListener listener);
	
	/**
	 * Don't get notifications anymore
	 * 
	 * @param listener
	 */
	public void removeDeviceListener(DeviceListener listener);
	
	/**
	 * To send notifications
	 * 
	 * @param dsid	the device unique id
	 * @param event	what happend
	 */
	public void notifyDeviceListener(String dsid);
	
	//for ESH
	
	public boolean isPowerConsumptionUpToDate();
	
	public boolean isElectricMeterUpToDate();
	
	public boolean isEnergyMeterUpToDate();
	
	public boolean isSensorDataUpToDate();
	
	public void setSensorDataRefreshPriority(String powerConsumptionRefreshPriority, 
			String electricMeterRefreshPriority, 
			String energyMeterRefreshPriority);
	
	public String getPowerConsumptionRefreshPriority();
	
	public String getElectricMeterRefreshPriority();
	
	public String getEnergyMeterRefreshPriority();
	
	public boolean isAddToESH();
	
	public void setIsAddToESH(boolean isAdd);
	
	/**
	 * Returns the next ESH-Thing-State-Update.
	 * 
	 * @return DeviceStateUpdate
	 */
	public DeviceStateUpdate getNextESHThingUpdateStates();
	
	/**
	 * Returns true if the ESH-Thing is up to date.
	 *  
	 * @return
	 */
	public boolean isESHThingUpToDate();
	
	/**
	 * Returns true if the device is up to date.
	 * 
	 * @return
	 */
	public boolean isDeviceUpToDate();
	
	/**
	 * Returns the next DigitalSTROM-Device-Update to send it to the DigitalSTROM-Server.
	 * 
	 * @return
	 */
	public DeviceStateUpdate getNextDeviceUpdateState();
	
	/**
	 * Update the in ESH internal stored device object.
	 * 
	 * @param deviceStateUpdate
	 */
	public void updateInternalDeviceState(DeviceStateUpdate deviceStateUpdate);
	
}
