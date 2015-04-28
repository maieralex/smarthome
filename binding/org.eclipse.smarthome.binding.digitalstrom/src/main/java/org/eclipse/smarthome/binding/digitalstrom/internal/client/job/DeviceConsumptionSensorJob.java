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
import org.eclipse.smarthome.binding.digitalstrom.internal.client.constants.SensorIndexEnum;
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
 * 
 */
public class DeviceConsumptionSensorJob implements SensorJob {

	private static final Logger logger = LoggerFactory
			.getLogger(DeviceConsumptionSensorJob.class);
	private Device device = null;
	private SensorIndexEnum sensorIndex = null;
	private DSID meterDSID = null;
	private long initalisationTime = 0;
	
	public DeviceConsumptionSensorJob(Device device, SensorIndexEnum index) {
		this.device = device;
		this.sensorIndex = index;
		this.meterDSID = device.getMeterDSID();
		this.initalisationTime = System.currentTimeMillis();
	}
	
	
	@Override
	public void execute(DigitalSTROMAPI digitalSTROM, String token) {
		int consumption = digitalSTROM.getDeviceSensorValue(token, this.device.getDSID(), null, this.sensorIndex);
		logger.info("SensorIndex: "+this.sensorIndex+", DeviceConsumption : "+consumption+", DSID: "+this.device.getDSID().getValue());

		switch (this.sensorIndex) {
	
			case ACTIVE_POWER:
							//logger.info("DeviceConsumption : "+consumption+", DSID: "+this.device.getDSID().getValue());
							this.device.updateInternalDeviceState(new DeviceStateUpdateImpl(DeviceStateUpdate.UPDATE_POWER_CONSUMPTION, consumption));
							break;
			case OUTPUT_CURRENT:
							this.device.updateInternalDeviceState(new DeviceStateUpdateImpl(DeviceStateUpdate.UPDATE_ELECTRIC_METER_VALUE, consumption));
							break;
			case ELECTRIC_METER:
							this.device.updateInternalDeviceState(new DeviceStateUpdateImpl(DeviceStateUpdate.UPDATE_ELECTRIC_METER_VALUE, consumption));
							break;
			default:
				break;
		}
	}

	@Override
	public boolean equals(Object obj) {
		if (obj instanceof DeviceConsumptionSensorJob) {
			DeviceConsumptionSensorJob other = (DeviceConsumptionSensorJob) obj;
			String device = this.device.getDSID().getValue()+this.sensorIndex.getIndex();
			return device.equals(other.device.getDSID().getValue()+other.sensorIndex.getIndex());
		}
		return false;
	}

	@Override
	public int hashCode(){
		return new String(this.device.getDSID().getValue()+this.sensorIndex.getIndex()).hashCode();
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
