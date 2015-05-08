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
 * The {@link CachedMeteringValue} store cached metering values.
 * 
 * @author 	Alexander Betker
 * @since 1.3.0
 */
public interface CachedMeteringValue {
	
	/**
	 * Returns the dSID.
	 * 
	 * @return dSID
	 */
	public DSID getDsid();
	
	/**
	 * Returns the metering value.
	 * 
	 * @return metering value
	 */
	public double getValue();
	
	/**
	 * Returns the date on which the metering value has been detected. 
	 * @return
	 */
	public String getDate();

}
