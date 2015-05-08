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
 * The {@link DetailedGroupInfo} stores all dSID of the DigitalSTROM-Devices which are included in this group.
 * 
 * @author 	Alexander Betker
 * @since 1.3.0
 */
public interface DetailedGroupInfo extends Group {
	
	/**
	 * Returns a {@link List} of all dSID of the DigitalSTROM-Devices which are included in this group.
	 * 
	 * @return list with dSIDs
	 */
	public List<String> getDeviceList();

}
