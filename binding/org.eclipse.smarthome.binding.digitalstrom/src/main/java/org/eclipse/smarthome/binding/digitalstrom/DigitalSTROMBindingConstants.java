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
    
    // Bridge config properties
    public static final String HOST = "ipAddress";
	public static final String USER_NAME = "userName";
	public static final String PASSWORD = "password";
	public static final String APPLICATION_TOKEN = "applicationToken";
	public static final String DS_ID = "dSID";
	public static final String DS_NAME = "dsName";
	
    // Device config properties
	public static final String DEVICE_ID = "deviceId";
	public static final String DEVICE_NAME = "deviceName";
	
	//Client configuration
	//connection Configuration
	public final static int DEFAULT_CONNECTION_TIMEOUT = 4000;
	public final static int DEFAULT_READ_TIMEOUT = 10000;

	//DeviceListener refresh interval
	public final static int DEFAULT_DEVICE_LISTENER_REFRESH_INTERVAL = 10000;
}
