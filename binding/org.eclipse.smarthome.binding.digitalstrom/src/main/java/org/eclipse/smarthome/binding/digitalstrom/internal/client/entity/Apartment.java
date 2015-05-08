/**
 * Copyright (c) 2010-2014, openHAB.org and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.smarthome.binding.digitalstrom.internal.client.entity;

import java.util.Map;

/**
 * The {@link Apartment} represents the DigitalSTROM apartment structure.
 * 
 * @author 	Alexander Betker
 * @since 1.3.0
 */
public interface Apartment {
	
	/**
	 * Returns a {@link Map} including zones, the key is the zone id and the value is the {@link Zone}.
	 *  
	 * @return Map (key = zone id, value = zone)
	 */
	public Map<Integer, Zone> getZoneMap();

}
