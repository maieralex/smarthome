/**
 * Copyright (c) 2010-2014, openHAB.org and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.smarthome.binding.digitalstrom.internal.client.entity.impl;


import org.eclipse.smarthome.binding.digitalstrom.internal.client.constants.JSONApiResponseKeysEnum;
import org.eclipse.smarthome.binding.digitalstrom.internal.client.entity.CachedMeteringValue;
import org.eclipse.smarthome.binding.digitalstrom.internal.client.entity.DSID;
import org.json.simple.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The {@link JSONCachedMeteringValueImpl} is the implementation of the {@link CachedMeteringValue}.
 * 
 * @author 	Alexander Betker
 * @since 1.3.0
 */
public class JSONCachedMeteringValueImpl implements CachedMeteringValue {
	
	private static final Logger logger = LoggerFactory.getLogger(JSONCachedMeteringValueImpl.class);
	
	private DSID dsid = null;
	private double	value = 0;
	private String date = null;
	
	/**
	 * Creates a new {@link JSONCachedMeteringValueImpl} from the given DigitalSTROM cached metering value {@link JSONObject}.
	 * 
	 * @param cached metering value json object
	 */
	public JSONCachedMeteringValueImpl(JSONObject jObject) {
		if (jObject.get(JSONApiResponseKeysEnum.METERING_GET_LATEST_DSID.getKey()) != null) {
			this.dsid = new DSID(jObject.get(JSONApiResponseKeysEnum.METERING_GET_LATEST_DSID.getKey()).toString());
		}
		
		if (jObject.get(JSONApiResponseKeysEnum.METERING_GET_LATEST_VALUE.getKey()) != null) {
			try {
				this.value = Double.parseDouble(jObject.get(JSONApiResponseKeysEnum.METERING_GET_LATEST_VALUE.getKey()).toString());
			}
			catch (java.lang.NumberFormatException e) {
				logger.error("NumberFormatException by getting value: "+jObject.get(JSONApiResponseKeysEnum.METERING_GET_LATEST_VALUE.getKey()).toString());
			}
		}
		
		if (jObject.get(JSONApiResponseKeysEnum.METERING_GET_LATEST_DATE.getKey()) != null) {
			this.date = jObject.get(JSONApiResponseKeysEnum.METERING_GET_LATEST_DATE.getKey()).toString();
		}
		
	}

	@Override
	public DSID getDsid() {
		return dsid;
	}

	@Override
	public double getValue() {
		return value;
	}

	@Override
	public String getDate() {
		return date;
	}
	
	@Override
	public String toString() {
		return "dsid: "+this.getDsid()+", date: "+this.getDate()+", value: "+this.getValue();
	}

}
