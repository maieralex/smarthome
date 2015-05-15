package org.eclipse.smarthome.binding.digitalstrom.internal.client.entity.impl;

import org.eclipse.smarthome.binding.digitalstrom.internal.client.entity.DeviceStateUpdate;

/**
 * The {@link DeviceStateUpdateImpl} is the implementation of the {@link DeviceStateUpdate}.
 *  
 * @author Michael Ochel
 * @author Matthias Siegele
 */
public class DeviceStateUpdateImpl implements DeviceStateUpdate {

	private final String UPDATE_TYPE;
	private final int VALUE;
	
	public DeviceStateUpdateImpl(String updateType, int value){
		this.UPDATE_TYPE = updateType;
		this.VALUE = value;
	}
	
	@Override
	public int getValue() {
		return VALUE;
	}

	@Override
	public String getType() {
		return UPDATE_TYPE;
	}

}
