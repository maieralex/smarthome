package org.eclipse.smarthome.binding.digitalstrom.handler;

import org.eclipse.smarthome.binding.digitalstrom.internal.client.entity.Device;

public interface DeviceStatusListener {

	public final static String DEVICE_DESCOVERY = "DeviceDiscovey";
	
	/**
     * This method is called whenever the state of the given device has changed. The new state can be obtained by {@link FullLight#getState()}.
     * 
     */
    public void onDeviceStateChanged(Device device);

    public void onDeviceNeededSensorDataUpdate(Device device);
    
    /**
     * This method us called whenever a device is removed.
     * 
     */
    public void onDeviceRemoved(Device device);

    /**
     * This method us called whenever a device is added.
     * 
     */
    public void onDeviceAdded(Device device);
    
    
    
}
