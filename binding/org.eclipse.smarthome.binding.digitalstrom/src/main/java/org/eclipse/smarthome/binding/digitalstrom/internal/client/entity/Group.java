/**
 * Copyright (c) 2010-2014, openHAB.org and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.smarthome.binding.digitalstrom.internal.client.entity;

/**
 * The {@link Group} represent DigitalSTROM-Group.
 * 
 * @author 	Alexander Betker
 * @since 1.3.0
 */
public interface Group {
	
	/**
	 * Returns the group id of this {@link Group}.
	 * 
	 * @return group id
	 */
	public short getGroupID();
	
	/**
	 * Returns the group name of this {@link Group}.
	 * 
	 * @return group name
	 */
	public String getGroupName();

}
