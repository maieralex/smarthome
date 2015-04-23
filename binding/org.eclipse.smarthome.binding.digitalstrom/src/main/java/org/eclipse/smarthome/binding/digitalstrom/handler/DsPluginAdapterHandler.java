package org.eclipse.smarthome.binding.digitalstrom.handler;

import org.eclipse.smarthome.binding.digitalstrom.internal.client.entity.Device;
import org.eclipse.smarthome.binding.digitalstrom.internal.client.entity.DeviceSceneSpec;
import org.eclipse.smarthome.core.thing.ChannelUID;
import org.eclipse.smarthome.core.thing.Thing;
import org.eclipse.smarthome.core.thing.binding.BaseThingHandler;
import org.eclipse.smarthome.core.types.Command;

public class DsPluginAdapterHandler extends BaseThingHandler implements
		DeviceStatusListener {

	public DsPluginAdapterHandler(Thing thing) {
		super(thing);
		// TODO Auto-generated constructor stub
	}

	@Override
	public void handleCommand(ChannelUID channelUID, Command command) {
		// TODO Auto-generated method stub

	}

	@Override
	public void onDeviceStateChanged(Device device) {
		// TODO Auto-generated method stub

	}

	@Override
	public void onDeviceRemoved(Device device) {
		// TODO Auto-generated method stub

	}

	@Override
	public void onDeviceAdded(Device device) {
		// TODO Auto-generated method stub

	}

	@Override
	public void onSceneConfigAdded(short sceneId, Device device) {
		// TODO Auto-generated method stub

	}

}
