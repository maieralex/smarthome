/**
 * Copyright (c) 2014-2015 openHAB UG (haftungsbeschraenkt) and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.smarthome.binding.digitalstrom.handler;

import static org.eclipse.smarthome.binding.digitalstrom.DigitalSTROMBindingConstants.*;

import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.eclipse.smarthome.binding.digitalstrom.DigitalSTROMBindingConstants;
import org.eclipse.smarthome.binding.digitalstrom.internal.digitalSTROMLibary.digitalSTROMListener.DeviceStatusListener;
import org.eclipse.smarthome.binding.digitalstrom.internal.digitalSTROMLibary.digitalSTROMStructure.digitalSTROMDevices.Device;
import org.eclipse.smarthome.binding.digitalstrom.internal.digitalSTROMLibary.digitalSTROMStructure.digitalSTROMDevices.deviceParameters.ChangeableDeviceConfigEnum;
import org.eclipse.smarthome.binding.digitalstrom.internal.digitalSTROMLibary.digitalSTROMStructure.digitalSTROMDevices.deviceParameters.DeviceSceneSpec;
import org.eclipse.smarthome.binding.digitalstrom.internal.digitalSTROMLibary.digitalSTROMStructure.digitalSTROMDevices.deviceParameters.DeviceStateUpdate;
import org.eclipse.smarthome.binding.digitalstrom.internal.digitalSTROMLibary.digitalSTROMStructure.digitalSTROMDevices.deviceParameters.JSONDeviceSceneSpecImpl;
import org.eclipse.smarthome.config.core.Configuration;
import org.eclipse.smarthome.core.library.types.DecimalType;
import org.eclipse.smarthome.core.library.types.IncreaseDecreaseType;
import org.eclipse.smarthome.core.library.types.OnOffType;
import org.eclipse.smarthome.core.library.types.PercentType;
import org.eclipse.smarthome.core.library.types.StopMoveType;
import org.eclipse.smarthome.core.library.types.UpDownType;
import org.eclipse.smarthome.core.thing.Bridge;
import org.eclipse.smarthome.core.thing.Channel;
import org.eclipse.smarthome.core.thing.ChannelUID;
import org.eclipse.smarthome.core.thing.Thing;
import org.eclipse.smarthome.core.thing.ThingStatus;
import org.eclipse.smarthome.core.thing.ThingStatusDetail;
import org.eclipse.smarthome.core.thing.ThingStatusInfo;
import org.eclipse.smarthome.core.thing.ThingTypeUID;
import org.eclipse.smarthome.core.thing.binding.BaseThingHandler;
import org.eclipse.smarthome.core.thing.binding.ThingHandler;
import org.eclipse.smarthome.core.thing.binding.builder.ChannelBuilder;
import org.eclipse.smarthome.core.thing.binding.builder.ThingBuilder;
import org.eclipse.smarthome.core.types.Command;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.Sets;

/**
 * The {@link DsDeviceHandler} is responsible for handling commands,
 * which are send to one of the channels of an DigitalSTROM device, which can be switched on or off or/and is dimmable.
 * It uses the {@link DsBridgeHandler} to execute the actual command.
 *
 * @author Michael Ochel - Initial contribution
 * @author Mathias Siegele - Initial contribution
 *
 */
public class DsDeviceHandler extends BaseThingHandler implements DeviceStatusListener {

    private Logger logger = LoggerFactory.getLogger(DsDeviceHandler.class);

    public final static Set<ThingTypeUID> SUPPORTED_THING_TYPES = Sets.newHashSet();// THING_TYPE_GE_KM200,THING_TYPE_GE_KL200

    private String dSID = null;

    private Device device;

    private DssBridgeHandler dssBridgeHandler;

    private Command lastComand = null;

    private String currentChannel = null;

    public DsDeviceHandler(Thing thing) {
        super(thing);
    }

    @Override
    public void initialize() {
        logger.debug("Initializing DigitalSTROM Device handler.");
        String configdSID = getConfig().get(DigitalSTROMBindingConstants.DEVICE_DSID).toString();

        if (!configdSID.isEmpty()) {
            dSID = configdSID;
            if (this.dssBridgeHandler == null) {
                updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.HANDLER_MISSING_ERROR, "Bridge is missig");
            } else {
                bridgeHandlerInitialized(dssBridgeHandler, this.getBridge());
            }

        } else {
            updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.CONFIGURATION_ERROR, "dSID is missig");
        }
    }

    @Override
    protected void bridgeHandlerInitialized(ThingHandler thingHandler, Bridge bridge) {
        if (dSID != null) {
            if (thingHandler instanceof DssBridgeHandler) {
                this.dssBridgeHandler = (DssBridgeHandler) thingHandler;

                updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.CONFIGURATION_PENDING,
                        "waiting for listener registartion");
                logger.debug("Set status on {}", getThing().getStatus());
                // note: this call implicitly registers our handler as a device-listener on the bridge

                this.dssBridgeHandler = (DssBridgeHandler) thingHandler;
                this.dssBridgeHandler.registerDeviceStatusListener(this);

            }
        }
    }

    @Override
    public void dispose() {
        logger.debug("Handler disposes. Unregistering listener.");
        if (dSID != null) {
            DssBridgeHandler dssBridgeHandler = getDssBridgeHandler();
            if (dssBridgeHandler != null) {
                getDssBridgeHandler().unregisterDeviceStatusListener(this);
            }
            dSID = null;
        }
    }

    @Override
    public void handleCommand(ChannelUID channelUID, Command command) {
        DssBridgeHandler dssBridgeHandler = getDssBridgeHandler();
        if (dssBridgeHandler == null) {
            logger.warn("DigitalSTROM bridge handler not found. Cannot handle command without bridge.");
            return;
        }

        if (device == null) {
            logger.debug("DigitalSTROM device not known on bridge. Cannot handle command.");
            return;
        }

        if (device.isDimmable()) {
            if (channelUID.getId().equals(DigitalSTROMBindingConstants.CHANNEL_BRIGHTNESS)
                    || channelUID.getId().equals(DigitalSTROMBindingConstants.CHANNEL_LIGHT_SWITCH)) {
                if (command instanceof PercentType) {
                    device.setOutputValue(
                            fromPercentToValue(((PercentType) command).intValue(), device.getMaxOutputValue()));
                } else if (command instanceof OnOffType) {
                    if (OnOffType.ON.equals(command)) {
                        device.setIsOn(true);
                    } else {
                        device.setIsOn(false);
                    }
                } else if (command instanceof IncreaseDecreaseType) {
                    if (IncreaseDecreaseType.INCREASE.equals(command)) {
                        device.increase();
                    } else {
                        device.decrease();
                    }
                }
            } else {
                logger.warn("Command send to an unknown channel id: " + channelUID);
            }
        } else if (device.isRollershutter()) {
            if (channelUID.getId().equals(DigitalSTROMBindingConstants.CHANNEL_SHADE)) {
                if (command instanceof PercentType) {
                    device.setOutputValue(
                            fromPercentToValue(((PercentType) command).intValue(), device.getMaxOutputValue()));
                    this.lastComand = command;
                } else if (command instanceof StopMoveType) {
                    if (StopMoveType.MOVE.equals(command)) {
                        handleCommand(channelUID, this.lastComand);
                    } else {
                        dssBridgeHandler.stopOutputValue(device);
                    }
                } else if (command instanceof UpDownType) {
                    if (UpDownType.UP.equals(command)) {
                        device.setIsOpen(true);
                        this.lastComand = command;
                    } else {
                        device.setIsOpen(false);
                        this.lastComand = command;
                    }
                }
            } else {
                logger.warn("Command send to an unknown channel id: " + channelUID);
            }
        }
    }

    private int fromPercentToValue(int percent, int max) {
        if (percent < 0 || percent == 0) {
            return 0;
        }
        if (max < 0 || max == 0) {
            return 0;
        }
        return (int) (max * ((float) percent / 100));
    }

    private synchronized DssBridgeHandler getDssBridgeHandler() {
        if (this.dssBridgeHandler == null) {
            Bridge bridge = getBridge();
            if (bridge == null) {
                logger.debug("cant find Bridge");
                return null;
            }
            ThingHandler handler = bridge.getHandler();

            if (handler instanceof DssBridgeHandler) {
                this.dssBridgeHandler = (DssBridgeHandler) handler;
                this.dssBridgeHandler.registerDeviceStatusListener(this);
            } else {
                return null;
            }
        }
        return this.dssBridgeHandler;
    }

    @Override
    public synchronized void onDeviceStateChanged(DeviceStateUpdate deviceStateUpdate) {
        if (device != null) {
            logger.debug("Update ESH State");
            if (device.isDimmable()) {
                if (deviceStateUpdate != null) {
                    switch (deviceStateUpdate.getType()) {
                        case DeviceStateUpdate.UPDATE_BRIGHTNESS:
                            updateState(new ChannelUID(getThing().getUID(), CHANNEL_BRIGHTNESS), new PercentType(
                                    fromValueToPercent(deviceStateUpdate.getValue(), device.getMaxOutputValue())));
                            break;
                        case DeviceStateUpdate.UPDATE_ON_OFF:
                            if (deviceStateUpdate.getValue() > 0) {
                                updateState(new ChannelUID(getThing().getUID(), CHANNEL_BRIGHTNESS), OnOffType.ON);
                                updateState(new ChannelUID(getThing().getUID(), CHANNEL_BRIGHTNESS),
                                        new PercentType(100));
                            } else {
                                updateState(new ChannelUID(getThing().getUID(), CHANNEL_BRIGHTNESS), OnOffType.OFF);
                            }
                            break;
                        case DeviceStateUpdate.UPDATE_ELECTRIC_METER_VALUE:
                            updateState(new ChannelUID(getThing().getUID(), CHANNEL_ELECTRIC_METER),
                                    new DecimalType(deviceStateUpdate.getValue()));
                            break;
                        case DeviceStateUpdate.UPDATE_ENERGY_METER_VALUE:
                            updateState(new ChannelUID(getThing().getUID(), CHANNEL_ENERGY_METER),
                                    new DecimalType(deviceStateUpdate.getValue()));
                            break;
                        case DeviceStateUpdate.UPDATE_POWER_CONSUMPTION:
                            updateState(new ChannelUID(getThing().getUID(), CHANNEL_POWER_CONSUMPTION),
                                    new DecimalType(deviceStateUpdate.getValue()));
                            break;
                        default:
                            return;
                    }
                }
            } else if (device.isRollershutter()) {
                logger.debug("Update ESH State");
                if (deviceStateUpdate != null) {
                    switch (deviceStateUpdate.getType()) {
                        case DeviceStateUpdate.UPDATE_SLATPOSITION:
                            updateState(new ChannelUID(getThing().getUID(), CHANNEL_SHADE), new PercentType(
                                    fromValueToPercent(deviceStateUpdate.getValue(), device.getMaxOutputValue())));
                            break;
                        case DeviceStateUpdate.UPDATE_OPEN_CLOSE:
                            if (deviceStateUpdate.getValue() > 0) {
                                updateState(new ChannelUID(getThing().getUID(), CHANNEL_SHADE), UpDownType.UP);
                                updateState(new ChannelUID(getThing().getUID(), CHANNEL_SHADE), new PercentType(100));
                            } else {
                                updateState(new ChannelUID(getThing().getUID(), CHANNEL_SHADE), UpDownType.DOWN);
                                updateState(new ChannelUID(getThing().getUID(), CHANNEL_SHADE), new PercentType(0));
                            }
                            break;
                        default:
                            return;
                    }
                }
            }

        }
    }

    private int fromValueToPercent(int value, int max) {
        if (value < 0 || value == 0) {
            return 0;
        }
        if (max < 0 || max == 0) {
            return 0;
        }
        return (int) (value * ((float) 100 / max));
    }

    @Override
    public synchronized void onDeviceRemoved(Device device) {
        this.device = null;
        updateStatus(ThingStatus.OFFLINE);
    }

    @Override
    public synchronized void onDeviceAdded(Device device) {
        if (device != null && device.isPresent()) {
            this.device = device;
            ThingStatusInfo statusInfo = this.dssBridgeHandler.getThing().getStatusInfo();
            updateStatus(statusInfo.getStatus(), statusInfo.getStatusDetail(), statusInfo.getDescription());
            logger.debug("Set status on {}", getThing().getStatus());

            // load sensor priorities into the device
            /*
             * boolean configChanged = false;
             *
             * logger.debug("Add sensor priorities to device");
             * Configuration config = getThing().getConfiguration();
             * String powerConsumptionPrio = DigitalSTROMBindingConstants.REFRESH_PRIORITY_NEVER;
             * if (config.get(DigitalSTROMBindingConstants.POWER_CONSUMTION_REFRESH_PRIORITY) != null) {
             * powerConsumptionPrio = config.get(DigitalSTROMBindingConstants.POWER_CONSUMTION_REFRESH_PRIORITY)
             * .toString();
             * } else {
             * config.put(DigitalSTROMBindingConstants.POWER_CONSUMTION_REFRESH_PRIORITY,
             * DigitalSTROMBindingConstants.REFRESH_PRIORITY_NEVER);
             * configChanged = true;
             * }
             *
             * String energyMeterPrio = DigitalSTROMBindingConstants.REFRESH_PRIORITY_NEVER;
             * if (config.get(DigitalSTROMBindingConstants.ENERGY_METER_REFRESH_PRIORITY) != null) {
             * powerConsumptionPrio = config.get(DigitalSTROMBindingConstants.ENERGY_METER_REFRESH_PRIORITY)
             * .toString();
             * } else {
             * config.put(DigitalSTROMBindingConstants.ENERGY_METER_REFRESH_PRIORITY,
             * DigitalSTROMBindingConstants.REFRESH_PRIORITY_NEVER);
             * configChanged = true;
             * }
             *
             * String electricMeterPrio = DigitalSTROMBindingConstants.REFRESH_PRIORITY_NEVER;
             * if (config.get(DigitalSTROMBindingConstants.ELECTRIC_METER_REFRESH_PRIORITY) != null) {
             * powerConsumptionPrio = config.get(DigitalSTROMBindingConstants.ELECTRIC_METER_REFRESH_PRIORITY)
             * .toString();
             * } else {
             * config.put(DigitalSTROMBindingConstants.ELECTRIC_METER_REFRESH_PRIORITY,
             * DigitalSTROMBindingConstants.REFRESH_PRIORITY_NEVER);
             * configChanged = true;
             * }
             * if (configChanged) {
             * super.updateConfiguration(config);
             * configChanged = false;
             * }
             * logger.debug(powerConsumptionPrio + ", " + energyMeterPrio + ", " + electricMeterPrio);
             *
             * device.setSensorDataRefreshPriority(powerConsumptionPrio, energyMeterPrio, electricMeterPrio);
             */
            // check and load sensor channels of the thing
            // checkSensorChannel(powerConsumptionPrio, energyMeterPrio, electricMeterPrio);

            // check and load output channel of the thing
            checkOutputChannel();

            // load first channel values
            // onDeviceStateInitial(device);

            // load scene configurations persistently into the thing
            for (Short i : device.getSavedScenes()) {
                onSceneConfigAdded(i);
            }

            saveConfigSceneSpecificationIntoDevice(device);
            logger.debug("Load saved scene specification into device");
        } else {
            onDeviceRemoved(device);
        }

    }

    private void checkSensorChannel(String powerConsumptionPrio, String energyMeterPrio, String electricMeterPrio) {
        List<Channel> channelList = new LinkedList<Channel>(this.getThing().getChannels());

        // if sensor channels are loaded delete these channels
        if (channelList.size() > 1) {
            Iterator<Channel> channelInter = channelList.iterator();
            while (channelInter.hasNext()) {
                if (channelInter.next().getUID().getId().equals(CHANNEL_POWER_CONSUMPTION)
                        || channelInter.next().getUID().getId().equals(CHANNEL_ENERGY_METER)
                        || channelInter.next().getUID().getId().equals(CHANNEL_ENERGY_METER)) {
                    channelInter.remove();
                }
            }
        }

        if (!powerConsumptionPrio.equals(REFRESH_PRIORITY_NEVER)) {
            Channel channel = ChannelBuilder
                    .create(new ChannelUID(this.getThing().getUID(), CHANNEL_POWER_CONSUMPTION), "String").build();
            channelList.add(channel);
        }
        if (!energyMeterPrio.equals(REFRESH_PRIORITY_NEVER)) {
            Channel channel = ChannelBuilder
                    .create(new ChannelUID(this.getThing().getUID(), CHANNEL_ENERGY_METER), "String").build();
            channelList.add(channel);
        }
        if (!electricMeterPrio.equals(REFRESH_PRIORITY_NEVER)) {
            Channel channel = ChannelBuilder
                    .create(new ChannelUID(this.getThing().getUID(), CHANNEL_ENERGY_METER), "String").build();
            channelList.add(channel);
        }

        ThingBuilder thingBuilder = editThing();
        thingBuilder.withChannels(channelList);
        updateThing(thingBuilder.build());
    }

    private void checkOutputChannel() {
        if (device == null) {
            logger.debug("Can not load a channel without an device!");
            return;
        }
        logger.debug(currentChannel);
        logger.debug("Channel brightness?: "
                + (device.isDimmable() && (currentChannel == null || currentChannel != CHANNEL_BRIGHTNESS)));
        if (device.isDimmable() && (currentChannel == null || currentChannel != CHANNEL_BRIGHTNESS)) {
            loadOutputChannel(CHANNEL_BRIGHTNESS, "Dimmer");
        } else if (device.isRollershutter() && (currentChannel != null || currentChannel != CHANNEL_SHADE)) {
            loadOutputChannel(CHANNEL_SHADE, "Rollershutter");
        } else if (!device.isDimmable() && (currentChannel != null || currentChannel != CHANNEL_LIGHT_SWITCH)) {
            // TODO: plugin-Adapter channel? or only a switch channel for all devices with switched output-mode?
            // We don't know black(joker) devices controls e.g. they can be a plug-in-adapter;
            // Another option can be loading a light_switch channel for the functional_color_group yellow
            // and a general switch channel for the black color group
            loadOutputChannel(CHANNEL_LIGHT_SWITCH, "Switch");
        }
    }

    private void loadOutputChannel(String channelId, String item) {
        Channel channel = ChannelBuilder.create(new ChannelUID(this.getThing().getUID(), channelId), item).build();
        currentChannel = channelId;
        logger.debug("channel = " + channel.getUID());

        // if no output channel is loaded, add only the output channel
        // if (currentChannel == null) {

        // thingBuilder.withChannel(channel); // Immutable List exception or similar
        // currentChannel = channelId;
        /*
         * Thing thing = thingBuilder.build();
         * logger.debug("Thing = " + thing);
         * updateThing(thing);
         */
        // return;
        // }

        List<Channel> channelList = new LinkedList<Channel>(this.getThing().getChannels());
        if (!channelList.isEmpty()) {

            for (int i = 0; i < channelList.size(); i++) {
                // delete current load output channel
                // if (channelList.get(i).getUID().getId().contains(currentChannel)) {
                // why sometimes are more than one channel from the same type are loaded?
                if (channelList.get(i).getUID().getId().contains(CHANNEL_BRIGHTNESS)
                        || channelList.get(i).getUID().getId().contains(CHANNEL_SHADE)
                        || channelList.get(i).getUID().getId().contains(CHANNEL_LIGHT_SWITCH)) {
                    logger.debug(channelList.get(i).getUID().getId());
                    channelList.remove(i);
                }
            }
        }

        channelList.add(channel);
        // logger.debug(channelList.toString());
        ThingBuilder thingBuilder = editThing();
        thingBuilder.withChannels(channelList);
        updateThing(thingBuilder.build());
        logger.debug("load channel: {} with item: {}", channelId, item);
    }

    @Override
    public void thingUpdated(Thing thing) {
        // TODO: perhaps our own sequence, because we not really need to dispose() and initialize() the thing after an
        // update
        dispose();
        this.thing = thing;
        initialize();
    }

    private void onDeviceStateInitial(Device device) {
        if (device != null) {
            // TODO: add rollershutter and check loaded channels e.g. brightness/switch sensor channels (may we need an
            // array or something else to find out if sensor channels are loaded)
            logger.debug("initial channel update");
            updateState(new ChannelUID(getThing().getUID(), CHANNEL_BRIGHTNESS),
                    new PercentType(fromValueToPercent(device.getOutputValue(), device.getMaxOutputValue())));

            if (device.isOn()) {
                updateState(new ChannelUID(getThing().getUID(), CHANNEL_BRIGHTNESS), OnOffType.ON);
            } else {
                updateState(new ChannelUID(getThing().getUID(), CHANNEL_BRIGHTNESS), OnOffType.OFF);
            }

            updateState(new ChannelUID(getThing().getUID(), CHANNEL_ELECTRIC_METER),
                    new DecimalType(device.getElectricMeterValue()));

            updateState(new ChannelUID(getThing().getUID(), CHANNEL_ENERGY_METER),
                    new DecimalType(device.getEnergyMeterValue()));

            updateState(new ChannelUID(getThing().getUID(), CHANNEL_POWER_CONSUMPTION),
                    new DecimalType(device.getPowerConsumption()));
        }

    }

    @Override
    public synchronized void onSceneConfigAdded(short sceneId) {
        String saveScene = "";
        DeviceSceneSpec sceneSpec = device.getSceneConfig(sceneId);
        if (sceneSpec != null) {
            saveScene = sceneSpec.toString();
        }

        int sceneValue = device.getSceneOutputValue(sceneId);
        if (sceneValue != -1) {
            saveScene = saveScene + ", sceneValue: " + sceneValue;
        }
        String key = DigitalSTROMBindingConstants.DEVICE_SCENE + sceneId;
        if (!saveScene.isEmpty()) {
            logger.debug("Save scene configuration: [{}] to thing with UID {}", saveScene, this.getThing().getUID());
            if (this.getThing().getProperties().get(key) != null) {
                this.getThing().setProperty(key, saveScene);
            } else {
                Map<String, String> properties = editProperties();
                properties.put(key, saveScene);
                updateProperties(properties);
            }
        }
    }

    @SuppressWarnings("null")
    private void saveConfigSceneSpecificationIntoDevice(Device device) {
        if (device != null) {
            /*
             * get persistently saved DeviceSceneSpec from Thing and save it in the Device, have to call after the
             * device is
             * added (onDeviceAdded()) to ThingHandler
             */
            Map<String, String> propertries = this.getThing().getProperties();
            String sceneSave;
            for (short i = 0; i < 128; i++) {
                sceneSave = propertries.get(DigitalSTROMBindingConstants.DEVICE_SCENE + i);
                if (sceneSave != null && !sceneSave.isEmpty()) {
                    logger.debug("Find saved scene configuration for scene id " + i);
                    String[] sceneParm = sceneSave.replace(" ", "").split(",");
                    JSONDeviceSceneSpecImpl sceneSpecNew = null;
                    for (int j = 0; j < sceneParm.length; j++) {
                        System.out.println(sceneParm[j]);
                        String[] sceneParmSplit = sceneParm[j].split(":");
                        switch (sceneParmSplit[0]) {
                            case "Scene":
                                sceneSpecNew = new JSONDeviceSceneSpecImpl(sceneParmSplit[1]);
                                break;
                            case "dontcare":
                                sceneSpecNew.setDontcare(Boolean.parseBoolean(sceneParmSplit[1]));
                                break;
                            case "localPrio":
                                sceneSpecNew.setLocalPrio(Boolean.parseBoolean(sceneParmSplit[1]));
                                break;
                            case "specialMode":
                                sceneSpecNew.setSpecialMode(Boolean.parseBoolean(sceneParmSplit[1]));
                                break;
                            case "sceneValue":
                                logger.debug("Saved sceneValue {} for scene id {} into device with dsid {}",
                                        sceneParmSplit[1], i, device.getDSID().getValue());
                                ;
                                device.setSceneOutputValue(i, Integer.parseInt(sceneParmSplit[1]));
                                break;
                        }
                    }
                    if (sceneSpecNew != null) {
                        logger.debug("Saved sceneConfig: [{}] for scene id {} into device with dsid {}",
                                sceneSpecNew.toString(), i, device.getDSID().getValue());
                        ;
                        device.addSceneConfig(i, sceneSpecNew);
                    }
                }

            }
        }

    }

    @Override
    public void onDeviceConfigChanged(ChangeableDeviceConfigEnum whichConfig) {
        Configuration config = editConfiguration();
        switch (whichConfig) {
            case DEVICE_NAME:
                config.put(DEVICE_NAME, device.getName());
                break;
            case METER_DSID:
                config.put(DEVICE_METER_ID, device.getMeterDSID().getValue());
                break;
            case ZONE_ID:
                config.put(DEVICE_ZONE_ID, device.getZoneId());
                break;
            case GROUPS:
                config.put(DEVICE_GROUPS, device.getGroups().toString());
                break;
            case FUNCTIONAL_GROUP:
                config.put(DEVICE_FUNCTIONAL_COLOR_GROUP, device.getFunctionalColorGroup().toString());
                break;
            case OUTPUT_MODE:
                config.put(DEVICE_OUTPUT_MODE, device.getOutputMode().toString());
                checkOutputChannel();
                break;
        }
        super.updateConfiguration(config);
    }

    @Override
    public String getID() {
        return this.dSID;
    }

}
