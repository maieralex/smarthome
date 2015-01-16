/**
 * Copyright (c) 2014 openHAB UG (haftungsbeschraenkt) and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.smarthome.binding.digitalstrom;

import org.eclipse.smarthome.core.thing.ThingTypeUID;

/**
 * The {@link DigitalSTROMBinding} class defines common constants, which are 
 * used across the whole binding.
 * 
 * @author Alex Maier - Initial contribution
 */
public class DigitalSTROMBindingConstants {

    public static final String BINDING_ID = "digitalstrom";
    
    // List of all Thing Type Ids
    public static final String THING_TYPE_ID_DSS_BRIDGE = "dssBridge";
    public static final String THING_TYPE_ID_GE_KM200 = "GE-KM200";
    public static final String THING_TYPE_ID_GE_KL200 = "GE-KL200";
    
    // List of all Thing Type UIDs
    public final static ThingTypeUID THING_TYPE_DSS_BRIDGE = new ThingTypeUID(BINDING_ID, THING_TYPE_ID_DSS_BRIDGE);
    public final static ThingTypeUID THING_TYPE_GE_KM200 = new ThingTypeUID(BINDING_ID, THING_TYPE_ID_GE_KM200);
    public final static ThingTypeUID THING_TYPE_GE_KL200 = new ThingTypeUID(BINDING_ID, THING_TYPE_ID_GE_KL200);
    
    // List of all Channels
    public static final String CHANNEL_BRIGHTNESS = "brightness";
    public static final String CHANNEL_ELECTRIC_METER = "electricMeter";
    public static final String CHANNEL_ENERGY_METER = "energyMeter";
    public static final String CHANNEL_POWER_CONSUMPTION = "powerConsumption";
    
    //Sensor data channel properties
    public static final String CHANNEL_REFRESH_PRIORITY = "refreshPriority";
    	//options
    	public static final String REFRESH_PRIORITY_NEVER = "never";
    	public static final String REFRESH_PRIORITY_LOW = "low";
    	public static final String REFRESH_PRIORITY_MEDIUM = "medium";
    	public static final String REFRESH_PRIORITY_HIGH = "high";
    
    // Bridge config properties
    public static final String HOST = "ipAddress";
	public static final String USER_NAME = "userName";
	public static final String PASSWORD = "password";
	public static final String APPLICATION_TOKEN = "applicationToken";
	public static final String DS_ID = "dSID";
	public static final String DS_NAME = "dsName";
	
	public static final int DEFAULT_TRASH_DEVICE_DELEATE_TIME = 7;//days after the trash devices get deleted
	
    // Device config properties
	public static final String DEVICE_ID = "deviceId";
	public static final String DEVICE_NAME = "deviceName";
	
	//Client configuration
	//connection Configuration
	public final static int DEFAULT_CONNECTION_TIMEOUT = 4000;
	public final static int DEFAULT_READ_TIMEOUT = 10000;
	public final static String APPLICATION_NAME = "ESH";

	//DeviceListener refresh interval
	public final static int DEFAULT_DEVICE_LISTENER_REFRESH_INTERVAL = 10000;
	
	//SensorData
	public final static int DEFAULT_SENSORDATA_REFRESH_INTERVAL = 10000;
}
