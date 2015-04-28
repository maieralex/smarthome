/**
 * Copyright (c) 2010-2014, openHAB.org and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.smarthome.binding.digitalstrom.internal.client.job;


import org.eclipse.smarthome.binding.digitalstrom.handler.DssBridgeHandler;
import org.eclipse.smarthome.binding.digitalstrom.internal.client.DigitalSTROMAPI;
import org.eclipse.smarthome.binding.digitalstrom.internal.client.entity.DSID;
import org.eclipse.smarthome.binding.digitalstrom.internal.client.entity.Device;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Alexander Betker
 * @author Alex Maier
 * @since 1.3.0
 */
public class SceneOutputValueSensorJob implements SensorJob {

	private static final Logger logger = LoggerFactory
			.getLogger(SceneOutputValueSensorJob.class);
	
	private Device device = null;
	private short sceneId = 0;
	private DssBridgeHandler dssBridgeHandler;
	private DSID meterDSID = null;
	private long initalisationTime = 0;

	public SceneOutputValueSensorJob(Device device, short sceneId, DssBridgeHandler dssBridgeHandler) {
		this.device = device;
		this.sceneId = sceneId;
		this.dssBridgeHandler = dssBridgeHandler;
		this.meterDSID = device.getMeterDSID();
		this.initalisationTime = System.currentTimeMillis();
	}
	/* (non-Javadoc)
	 * @see org.openhab.binding.digitalSTROM2.internal.client.job.SensorJob#execute(org.openhab.binding.digitalSTROM2.internal.client.DigitalSTROMAPI, java.lang.String)
	 */
	@Override
	public void execute(DigitalSTROMAPI digitalSTROM, String token) {
		//DeviceConfig config = digitalSTROM.getDeviceConfig(token, this.device.getDSID(), null, DeviceParameterClassEnum.CLASS_128, this.sceneId);
		int sceneValue = digitalSTROM.getSceneValue(token, this.device.getDSID(), this.sceneId);
		
		if (sceneValue != -1) {
			this.device.setSceneOutputValue(this.sceneId, sceneValue);
			this.dssBridgeHandler.informListenerAboutSceneConfigAdded(sceneId, device);
			logger.info("UPDATED sceneOutputValue for dsid: "+this.device.getDSID()+", sceneID: "+sceneId+", value: " + sceneValue);
		}
	}

	@Override
	public boolean equals(Object obj) {
		if (obj instanceof SceneOutputValueSensorJob) {
			SceneOutputValueSensorJob other = (SceneOutputValueSensorJob) obj;
			String str = other.device.getDSID().getValue()+"-"+other.sceneId;
			return (this.device.getDSID().getValue()+"-"+this.sceneId).equals(str);
		}
		return false;
	}
	
	@Override
	public int hashCode(){
		return new String(this.device.getDSID().getValue()+this.sceneId).hashCode();
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
