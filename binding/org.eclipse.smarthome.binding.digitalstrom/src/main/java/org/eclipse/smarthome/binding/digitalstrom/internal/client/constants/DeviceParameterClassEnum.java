/**
 * Copyright (c) 2010-2014, openHAB.org and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.smarthome.binding.digitalstrom.internal.client.constants;

/**
 * The {@link DeviceParameterClassEnum} contains all DigitalSTROM-Device class parameters.
 *   
 * @author	Alexander Betker
 * @since	1.3.0
 * @version	digitalSTROM-API 1.14.5
 */
public enum DeviceParameterClassEnum {
	
	/**
	 * communication specific parameters
	 */
	CLASS_0		(0),
	
	/**
	 * digitalSTROM device specific parameters
	 */
	CLASS_1		(1),
	
	/**
	 * function specific parameters
	 */
	CLASS_3		(3),
	
	/**
	 * sensor event table
	 */
	CLASS_6		(6),
	
	/**
	 * output status
	 * 
	 * possible OffsetParameters:
	 * - READ_OUTPUT
	 */
	CLASS_64	(64),
	
	/**
	 * read scene table
	 * use index/offset 0-127 
	 */
	CLASS_128	(128);
	
	private final int		classIndex;
	
	/**
	 * Creates a new {@link DeviceParameterClassEnum} with the given index.
	 * 
	 * @param index
	 */
	DeviceParameterClassEnum(int index) {
		this.classIndex = index;
	}
	
	/**
	 * Return the class index from this {@link DeviceParameterClassEnum}.
	 * 
	 * @return class index
	 */
	public int getClassIndex() {
		return this.classIndex;
	}

}
