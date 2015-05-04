package org.eclipse.smarthome.binding.digitalstrom.internal.client.entity;

/**
 * Represents a device state update for lights, shades and sensordata. 
 * 
 * @author Michael Ochel - Initial contribution
 * @author Matthias Siegele - Initial contribution
 *
 */
public interface DeviceStateUpdate {

	//Update types
	
	//light
	public final static String UPDATE_BRIGHTNESS = "brightness";
	public final static String UPDATE_ON_OFF = "OnOff";
	public final static String UPDATE_BRIGHTNESS_INCREASE = "brightnessIncrese";
	public final static String UPDATE_BRIGHTNESS_DECREASE = "brightnessDecrese";
	
	
	//shades
	public final static String UPDATE_SLATPOSITION = "slatposition";
	public final static String UPDATE_SLAT_INCREASE = "slatIncrese";
	public final static String UPDATE_SLAT_DECREASE = "slatDecrese";
	public final static String UPDATE_SLAT_MOVE = "slatMove";
	public final static String UPDATE_SLAT_STOP = "slatStop";
	
	//sensordata
	public final static String UPDATE_POWER_CONSUMPTION = "powerConsumption";
	public final static String UPDATE_ENERGY_METER_VALUE = "energyMeterValue";
	public final static String UPDATE_ELECTRIC_METER_VALUE = "electricMeterValue";

	/**
	 * Returns the state update value. 
	 * 
	 * NOTE: For the OnOff-type is the value for off < 0 and for on > 0. 
	 * 		 And for all Increase- and decrease-types is the vale the new output value.
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
