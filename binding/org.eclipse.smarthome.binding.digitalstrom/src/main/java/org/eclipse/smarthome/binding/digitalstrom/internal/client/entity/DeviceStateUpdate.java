package org.eclipse.smarthome.binding.digitalstrom.internal.client.entity;

/**
 * Represents a device state update for lights, shades and sensordata. 
 * 
 * @author Michael Ochel
 * @author Matthias Siegele
 *
 */
public interface DeviceStateUpdate {

	//Update types
	
	//light
	public final static String UPDATE_BRIGHTNESS = "brightness";
	public final static String UPDATE_ON_OFF = "OnOff";
	
	//shades
	public final static String UPDATE_SLATPOSITION = "slatposition";
	
	//sensordata
	public final static String UPDATE_POWER_CONSUMPTION = "powerConsumption";
	public final static String UPDATE_ENERGY_METER_VALUE = "energyMeterValue";
	public final static String UPDATE_ELECTRIC_METER_VALUE = "electricMeterValue";

	/**
	 * Returns the state update value. 
	 * 
	 * NOTE: For the OnOff-type is the value for off < 0 and for on > 0. 
	 * 
	 * @return new Statevalue
	 */
	public int getValue();
	
	/**
	 * Returns the state update type.
	 * 
	 * @return
	 */
	public String getType();
}
