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

/**
 * The {@link Zone} represent DigitalSTROM-Zone.
 * 
 * @author 	Alexander Betker
 * @since 1.3.0
 */
public interface Zone {
	
	/**
	 * Return the zone id of this {@link Zone}.
	 * 
	 * @return zone id
	 */
	public int getZoneId();
	
	/**
	 * Sets the zone id of this {@link Zone}.
	 * 
	 * @parm zone id
	 */
	public void setZoneId(int id);
	
	/**
	 * Return the zone name of this {@link Zone}.
	 * 
	 * @return zone name
	 */
	public String getName();
	
	/**
	 * Sets the zone name of this {@link Zone}.
	 * 
	 * @param zone name
	 */
	public void setName(String name);
	
	/**
	 * Return a {@link List} the {@link DetailedGroupInfo}s of this {@link Zone}.
	 * 
	 * @return {@link List} with {@link DetailedGroupInfo}s
	 */
	public List<DetailedGroupInfo> getGroups();
	
	/**
	 * Adds a {@link DetailedGroupInfo} to this {@link Zone}.
	 * 
	 * @param group
	 */
	public void addGroup(DetailedGroupInfo group);
	
	/**
	 * Return a {@link List} the {@link Device}s of this {@link Zone}.
	 * 
	 * @return {@link List} with {@link Device}s
	 */
	public List<Device> getDevices();
	
	/**
	 * Adds a {@link Device} to this {@link Zone}.
	 * 
	 * @param device
	 */
	public void addDevice(Device device);
	
}
