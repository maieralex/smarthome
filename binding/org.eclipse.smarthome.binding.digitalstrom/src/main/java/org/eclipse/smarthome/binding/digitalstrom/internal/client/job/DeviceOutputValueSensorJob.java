/**
 * Copyright (c) 2010-2014, openHAB.org and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.smarthome.binding.digitalstrom.internal.client.job;


import org.eclipse.smarthome.binding.digitalstrom.internal.client.DigitalSTROMAPI;
import org.eclipse.smarthome.binding.digitalstrom.internal.client.entity.DSID;
import org.eclipse.smarthome.binding.digitalstrom.internal.client.entity.Device;
import org.eclipse.smarthome.binding.digitalstrom.internal.client.entity.DeviceStateUpdate;
import org.eclipse.smarthome.binding.digitalstrom.internal.client.entity.impl.DeviceStateUpdateImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Alexander Betker
 * @author Alex Maier
 * @since 1.3.0
 */
public class DeviceOutputValueSensorJob implements SensorJob {

	private static final Logger logger = LoggerFactory
			.getLogger(DeviceOutputValueSensorJob.class);
	private Device device = null;
	private short index = 0;
	private DSID meterDSID = null;
	private long initalisationTime = 0;
	
	public DeviceOutputValueSensorJob(Device device, short index) {
		this.device = device;
		this.index = index;
		this.meterDSID = device.getMeterDSID();
		this.initalisationTime = System.currentTimeMillis();
	}
	
	/* (non-Javadoc)
	 * @see org.openhab.binding.digitalSTROM2.internal.client.job.SensorJob#execute(org.openhab.binding.digitalSTROM2.internal.client.DigitalSTROMAPI)
	 */
	@Override
	public void execute(DigitalSTROMAPI digitalSTROM, String token) {
		int value = digitalSTROM.getDeviceOutputValue(token, this.device.getDSID(), null, this.index);
		logger.info("DeviceOutputValue on Demand : "+value+", DSID: "+this.device.getDSID().getValue());
	
		if (value != 1) {
			switch (this.index) {
			case 0:
				this.device.updateInternalDeviceState(new DeviceStateUpdateImpl(DeviceStateUpdate.UPDATE_BRIGHTNESS, value));
				break;
			case 4:
				this.device.updateInternalDeviceState(new DeviceStateUpdateImpl(DeviceStateUpdate.UPDATE_SLATPOSITION, value));
				break;
		
			default: 
				break;
			}
		}
	}

	@Override
	public boolean equals(Object obj) {
		if (obj instanceof DeviceOutputValueSensorJob) {
			DeviceOutputValueSensorJob other = (DeviceOutputValueSensorJob) obj;
			String key = this.device.getDSID().getValue()+this.index;
			return key.equals((other.device.getDSID().getValue()+other.index));
		}
		return false;
	}

	@Override
	public int hashCode(){
		return new String(this.device.getDSID().getValue()+this.index).hashCode();
	}
	
	@Override
	public DSID getDsid() {
		return device.getDSID();
	}

	@Override
	public DSID getMeterDSID() {
		return this.meterDSID;
	}
	
	@Override
	public long getInitalisationTime() {
		return this.initalisationTime;
	}
	
	@Override
	public void setInitalisationTime(long time) {
		this.initalisationTime = time;
	}	

}
