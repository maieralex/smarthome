package org.eclipse.smarthome.binding.digitalstrom.handler;

import org.eclipse.smarthome.binding.digitalstrom.internal.client.entity.Device;

public interface DeviceStatusListener {

	/**
     * This method is called whenever the state of the given device has changed. The new state can be obtained by {@link FullLight#getState()}.
     * 
     */
    public void onDeviceStateChanged(Device device);

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
