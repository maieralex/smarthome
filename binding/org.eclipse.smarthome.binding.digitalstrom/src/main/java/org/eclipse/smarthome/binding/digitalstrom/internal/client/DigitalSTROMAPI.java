/**
 * Copyright (c) 2010-2014, openHAB.org and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.smarthome.binding.digitalstrom.internal.client;

import java.util.List;

import org.eclipse.smarthome.binding.digitalstrom.internal.client.constants.DeviceParameterClassEnum;
import org.eclipse.smarthome.binding.digitalstrom.internal.client.constants.MeteringTypeEnum;
import org.eclipse.smarthome.binding.digitalstrom.internal.client.constants.MeteringUnitsEnum;
import org.eclipse.smarthome.binding.digitalstrom.internal.client.constants.SensorIndexEnum;
import org.eclipse.smarthome.binding.digitalstrom.internal.client.constants.ZoneSceneEnum;
import org.eclipse.smarthome.binding.digitalstrom.internal.client.entity.Apartment;
import org.eclipse.smarthome.binding.digitalstrom.internal.client.entity.CachedMeteringValue;
import org.eclipse.smarthome.binding.digitalstrom.internal.client.entity.DSID;
import org.eclipse.smarthome.binding.digitalstrom.internal.client.entity.Device;
import org.eclipse.smarthome.binding.digitalstrom.internal.client.entity.DeviceConfig;
import org.eclipse.smarthome.binding.digitalstrom.internal.client.entity.DeviceSceneSpec;
import org.eclipse.smarthome.binding.digitalstrom.internal.client.entity.Scene;



/**
 * digitalSTROM-API
 * 
 * @author	Alexander Betker
 * @author	Michael Ochel - added and updated some methods and java-doc
 * @author	Matthias Siegele - added and updated some methods and java-doc
 * @see		http://developer.digitalstrom.org/download/dss/dss-1.14.5-doc/dss-1.14.5-json_api.html
 * @see		http://developer.digitalstrom.org/Architecture/dss-json.pdf
 */
public interface DigitalSTROMAPI {
	
	/**
	 * Calls the scene sceneNumber on all devices of the apartment. If groupID 
	 * or groupName are specified, only devices contained in this group will be 
	 * addressed.
	 * 
	 * @param groupID		this parameter is optional (not required)
	 * @param groupName		this parameter is optional (not required)
	 * @param sceneNumber	required
	 * @param force			this parameter is optional (not required)
	 * @return				true on success
	 */ 
	public boolean callApartmentScene(String token, int groupID, String groupName, Scene sceneNumber, boolean force);
	
	/**
	 * Undo the scene sceneNumber on all devices of the apartment. If groupID 
	 * or groupName are specified, only devices contained in this group will be 
	 * addressed.
	 * 
	 * @param groupID		this parameter is optional (not required)
	 * @param groupName		this parameter is optional (not required)
	 * @param sceneNumber	required
	 * @return				true on success
	 */
	public boolean undoApartmentScene(String token, int groupID, String groupName, Scene sceneNumber);
	
	/**
	 * Returns the {@link Apartment} structure of the DigitalSTROM-System.
	 * 
	 * @return	DigitalSTROMApartment which has a list of all zones
	 */
	public Apartment getApartmentStructure(String token);
	
	/**
	 * Returns the list of devices in the apartment. If unassigned is true, 
	 * only devices that are not assigned to a zone get returned.
	 * 
	 * @param unassigned	this parameter is optional (not required)
	 * @return				List of DigitalSTROMDevices
	 */
	public List<Device> getApartmentDevices(String token, boolean unassigned);
	
	
	/**
	 * Returns a list of dSID of all meters(dSMs).
	 * 
	 * @return String-List with dSID
	 */
	public List<String> getMeterList(String token);
	
	/**
	 * Calls sceneNumber on all devices in the zone. If groupID or groupName
	 * are specified, only devices contained in this group will be addressed.
	 * 
	 * @param zoneID		needs either zoneID or zoneName
	 * @param zoneName		needs either zoneID or zoneName
	 * @param groupID		this parameter is optional (not required)
	 * @param groupName		this parameter is optional (not required)
	 * @param sceneNumber	required	(only a zone/user scene is possible -> sceneNumber 0..63 )
	 * @param force			this parameter is optional (not required)
	 * @return				true on success
	 */
	public boolean callZoneScene(String token, int zoneID, String zoneName, int groupID, String groupName, ZoneSceneEnum sceneNumber, boolean force);
	
	/**
	 * Undo sceneNumber on all devices in the zone. If groupID or groupName
	 * are specified, only devices contained in this group will be addressed.
	 * 
	 * @param zoneID		needs either zoneID or zoneName
	 * @param zoneName		needs either zoneID or zoneName
	 * @param groupID		this parameter is optional (not required)
	 * @param groupName		this parameter is optional (not required)
	 * @param sceneNumber	required	(only a zone/user scene is possible -> sceneNumber 0..63 )
	 * @return				true on success
	 */
	public boolean undoZoneScene(String token, int zoneID, String zoneName, int groupID, String groupName, ZoneSceneEnum sceneNumber);
	
	/**
	 * Turns the device on. This will call SceneMax on the device.
	 * 
	 * @param dSID	needs either dSID id or name
	 * @param name	needs either dSID id or name
	 * @return		true on success
	 */
	public boolean turnDeviceOn(String token, DSID dSID, String name);
	
	/**
	 * Turns the device off. This will call SceneMin on the device.
	 * 
	 * @param dSID	needs either dSID id or name
	 * @param name	needs either dSID id or name
	 * @return		true on success
	 */
	public boolean turnDeviceOff(String token, DSID dSID, String name);
	
	/**
	 * Set the output value of the given device.
	 * 
	 * @param dSID	needs either dSID id or name
	 * @param name	needs either dSID id or name
	 * @param value	required (0 - 255)
	 * @return		true on success
	 */
	public boolean setDeviceValue(String token, DSID dSID, String name, int value);
	
	/**
	 * Gets the value of config class at offset index.
	 * 
	 * @param dSID		needs either dSID id or name
	 * @param name		needs either dSID id or name
	 * @param class_	required
	 * @param index		required
	 * @return			config with values
	 */
	public DeviceConfig getDeviceConfig(String token, DSID dSID, String name, DeviceParameterClassEnum class_, int index);
	
	/**
	 * Gets the device output value from parameter at the given offset.
	 * The available parameters and offsets depend on the features of the
	 * hardware components.
	 * 
	 * @param dSID		needs either dSID id or name
	 * @param name		needs either dSID id or name
	 * @param offset	required (known offset f.e. 0)
	 * @return device output value
	 */
	public int getDeviceOutputValue(String token, DSID dSID, String name, int offset); 
	 
	/**
	 * Sets the device output value at the given offset. The available
	 * parameters and offsets depend on the features of the hardware components.
	 * 
	 * @param dSID		needs either dSID id or name
	 * @param name		needs either dSID id or name
	 * @param offset	required
	 * @param value		required	(0 - 65535)
	 * @return			true on success
	 */
	public boolean setDeviceOutputValue(String token, DSID dSID, String name, int offset, int value);
	
	/**
	 * Gets the device configuration for a specific scene command.
	 * 
	 * @param dSID		needs either dSID id or name
	 * @param name		needs either dSID id or name
	 * @param sceneID	required	(0 .. 255)
	 * @return device scene specification
	 */
	public DeviceSceneSpec getDeviceSceneMode(String token, DSID dSID, String name, short sceneID);
	
	/**
	 * Gets the scene value from a specific scene from a device.
	 * 
	 * @param token
	 * @param dSID
	 * @param sceneID
	 * @return Scene value
	 */
	public int getSceneValue(String token, DSID dSID, short sceneID);
	
	/**
	 * Request the sensor value of a given index.
	 * 
	 * @param dSID			needs either dSID id or name
	 * @param name			needs either dSID id or name
	 * @param sensorIndex	required
	 * @return sensorValue			
	 */
	public short getDeviceSensorValue(String token, DSID dSID, String name, SensorIndexEnum sensorIndex);
	
	/**
	 * Calls scene sceneNumber on the device.
	 * 
	 * @param dSID			needs either dSID id or name
	 * @param name			needs either dSID id or name
	 * @param sceneNumber	required
	 * @param force			this parameter is optional (not required)
	 * @return				true on success
	 */
	public boolean callDeviceScene(String token, DSID dSID, String name, Scene sceneNumber, boolean force);
	
	/**
	 * Subscribes to an event given by the name. The subscriptionID is a unique id 
	 * that is defined by the subscriber. It is possible to subscribe to several events, 
	 * using the same subscription id, this allows to retrieve a grouped output of the 
	 * events (i.e. get output of all subscribed by the given id)
	 *  
	 * @param name				required
	 * @param subscriptionID	required
	 * @return					true on success
	 */
	public boolean subscribeEvent(String token, String name, int subscriptionID, int connectionTimeout, int readTimeout);
	
	/**
	 * Unsubscribes from an event given by the name. The subscriptionID is a unique 
	 * id that was used in the subscribe call
	 *  
	 * @param name				required
	 * @param subscriptionID	required
	 * @return					true on success
	 */
	public boolean unsubscribeEvent(String token, String name, int subscriptionID, int connectionTimeout, int readTimeout);
	
	/**
	 * Get event information and output. The subscriptionID is a unique id 
	 * that was used in the subscribe call. All events, subscribed with the 
	 * given id will be handled by this call. A timeout, in case no events 
	 * are taken place, can be specified (in MS). By default the timeout 
	 * is disabled: 0 (zero), if no events occur the call will block.
	 * 
	 * @param subscriptionID	required
	 * @param timeout			optional
	 * @return					Event-String 
	 */
	public String getEvent(String token, int subscriptionID, int timeout);
	
	/**
	 * Returns the dSS time in UTC seconds since epoch.
	 * 
	 * @return time
	 */
	public int getTime(String token);
	
	/**
	 * Creates a new session using the registered application token.
	 * 
	 * @param loginToken required
	 */
	public String loginApplication(String loginToken);
	
	/**
	 * Creates a new session.
	 * 
	 * @param user 		required
	 * @param password 	required
	 */
	public String login(String user, String password);
	
	/**
	 * Destroys the session and signs out the user.
	 * 
	 * @return	true on success
	 */
	public boolean logout();

	/**
	 * Returns the dSID of the digitalSTROM Server.
	 * 
	 * @param token required
	 * @return dsID
	 */
	public String getDSID(String token);
	
	/**
	 * Returns a token for passwordless login. The token will need to be approved
	 * by a user first, the caller must not be logged in.
	 * 
	 * @param applicationName required
	 * @return applicationToken
	 */
	public String requestAppplicationToken(String applicationName);
	
	/**
	 * Revokes an application token, caller must be logged in.
	 * 
	 * @param applicationToken
	 */
	public boolean revokeToken(String applicationToken);
	
	/**
	 * Enables an application token, caller must be logged in.
	 * 
	 * @param applicationToken required
	 * @return true on success
	 */
	public boolean enableApplicationToken(String applicationToken, String sessionToken);
	
	/**
	 * Returns all resolutions stored on this dSS.
	 * 
	 * @param 	sessionToken
	 * @return	List of resolutions
	 */
	public List<Integer> getResolutions(String token);
	
	/**
	 * Returns cached energy meter value or cached power consumption 
	 * value in watt (W). The type parameter defines what should 
	 * be returned, valid types, 'energyDelta' are 'energy' and 
	 * 'consumption'. 'energy' and 'energyDelta' are available in two units: 
	 * 'Wh' (default) and 'Ws'. The from parameter follows the set-syntax, 
	 * currently it supports: .meters(dSID1,dSID2,...) and .meters(all)
	 *  
	 * @param type		required
	 * @param from		required
	 * @param unit		optional
	 * @return cached metering values
	 */
	public List<CachedMeteringValue> getLatest(String token, MeteringTypeEnum type, String from, MeteringUnitsEnum unit);
	
	/**
	 * Sends a tests JSON-request to the DigitalSTROM-Server and returns the HTTP-Code. 
	 * 
	 * @param sessionToken
	 * @return HTTP-Code
	 */
	public int checkConnection(String sessionToken);
	
	/**
	 * Increases the value of the device with the given dSID.
	 * 
	 * @param sessionToken
	 * @param dSID
	 * @return true on success
	 */
	public boolean increaseValue(String sessionToken, DSID dSID);
	
	/**
	 * Decreases the value of the device with the given dSID.
	 * 
	 * @param sessionToken
	 * @param dSID
	 * @return true on success
	 */
	public boolean decreaseValue(String sessionToken, DSID dSID);


}
