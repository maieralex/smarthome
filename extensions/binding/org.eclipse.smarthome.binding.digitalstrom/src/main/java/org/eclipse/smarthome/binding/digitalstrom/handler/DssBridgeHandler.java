/**
 * Copyright (c) 2014 openHAB UG (haftungsbeschraenkt) and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.smarthome.binding.digitalstrom.handler;

import static org.eclipse.smarthome.binding.digitalstrom.DigitalSTROMBindingConstants.*;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import org.eclipse.smarthome.binding.digitalstrom.DigitalSTROMBindingConstants;
import org.eclipse.smarthome.binding.digitalstrom.internal.DigitalSTROMThingTypeProvider;
import org.eclipse.smarthome.binding.digitalstrom.internal.digitalSTROMLibary.digitalSTROMConfiguration.DigitalSTROMConfig;
import org.eclipse.smarthome.binding.digitalstrom.internal.digitalSTROMLibary.digitalSTROMListener.DeviceStatusListener;
import org.eclipse.smarthome.binding.digitalstrom.internal.digitalSTROMLibary.digitalSTROMListener.DigitalSTROMConnectionListener;
import org.eclipse.smarthome.binding.digitalstrom.internal.digitalSTROMLibary.digitalSTROMListener.SceneStatusListener;
import org.eclipse.smarthome.binding.digitalstrom.internal.digitalSTROMLibary.digitalSTROMListener.TotalPowerConsumptionListener;
import org.eclipse.smarthome.binding.digitalstrom.internal.digitalSTROMLibary.digitalSTROMManager.DigitalSTROMConnectionManager;
import org.eclipse.smarthome.binding.digitalstrom.internal.digitalSTROMLibary.digitalSTROMManager.DigitalSTROMDeviceStatusManager;
import org.eclipse.smarthome.binding.digitalstrom.internal.digitalSTROMLibary.digitalSTROMManager.DigitalSTROMSceneManager;
import org.eclipse.smarthome.binding.digitalstrom.internal.digitalSTROMLibary.digitalSTROMManager.DigitalSTROMStructureManager;
import org.eclipse.smarthome.binding.digitalstrom.internal.digitalSTROMLibary.digitalSTROMManager.impl.DigitalSTROMConnectionManagerImpl;
import org.eclipse.smarthome.binding.digitalstrom.internal.digitalSTROMLibary.digitalSTROMManager.impl.DigitalSTROMDeviceStatusManagerImpl;
import org.eclipse.smarthome.binding.digitalstrom.internal.digitalSTROMLibary.digitalSTROMManager.impl.DigitalSTROMSceneManagerImpl;
import org.eclipse.smarthome.binding.digitalstrom.internal.digitalSTROMLibary.digitalSTROMManager.impl.DigitalSTROMStructureManagerImpl;
import org.eclipse.smarthome.binding.digitalstrom.internal.digitalSTROMLibary.digitalSTROMStructure.digitalSTROMDevices.Device;
import org.eclipse.smarthome.binding.digitalstrom.internal.digitalSTROMLibary.digitalSTROMStructure.digitalSTROMScene.InternalScene;
import org.eclipse.smarthome.config.core.Configuration;
import org.eclipse.smarthome.core.library.types.DecimalType;
import org.eclipse.smarthome.core.thing.Bridge;
import org.eclipse.smarthome.core.thing.ChannelUID;
import org.eclipse.smarthome.core.thing.ThingStatus;
import org.eclipse.smarthome.core.thing.ThingStatusDetail;
import org.eclipse.smarthome.core.thing.ThingTypeUID;
import org.eclipse.smarthome.core.thing.binding.BaseBridgeHandler;
import org.eclipse.smarthome.core.types.Command;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The {@link DigitalSTROMHandler} is the handler for a DigitalSTROM-Server and connects it to
 * the framework. All {@link DsGrayHandler}s, {@link DsDeviceHandler}s and {@link DsSceneHandler} use the
 * {@link DigitalSTROMHandler} to execute the actual commands.
 * The digitalSTROM handler also informs all other digitalSTROM handler about status changes from the outside.
 *
 * @author Alex Maier - Initial contribution
 * @author Michael Ochel - Initial contribution
 * @author Mathias Siegele - Initial contribution
 */
public class DssBridgeHandler extends BaseBridgeHandler
        implements DigitalSTROMConnectionListener, TotalPowerConsumptionListener {

    private Logger logger = LoggerFactory.getLogger(DssBridgeHandler.class);

    public final static Set<ThingTypeUID> SUPPORTED_THING_TYPES = Collections.singleton(THING_TYPE_DSS_BRIDGE);

    /**** configuration ****/
    // private DigitalSTROMAPI digitalSTROMClient = null;
    private DigitalSTROMConnectionManager connMan;
    private DigitalSTROMStructureManager structMan;
    private DigitalSTROMSceneManager sceneMan;
    private DigitalSTROMDeviceStatusManager devStatMan;

    private List<SceneStatusListener> sceneListener;
    private List<DeviceStatusListener> devListener;
    private DigitalSTROMThingTypeProvider thingTypeProvider = null;

    public DssBridgeHandler(Bridge bridge, DigitalSTROMConnectionManager connectionManager) {
        super(bridge);
        this.connMan = connectionManager;
    }

    @Override
    public void initialize() {
        logger.debug("Initializing DigitalSTROM Bridge handler.");
        Configuration configuration = this.getConfig();
        if (configuration.get(HOST) != null && !configuration.get(HOST).toString().isEmpty()) {
            logger.debug("Initializing DigitalSTROM Manager.");
            if (connMan == null) {
                this.connMan = new DigitalSTROMConnectionManagerImpl(configuration.get(HOST).toString(),
                        configuration.get(USER_NAME).toString(), configuration.get(PASSWORD).toString(), null, false,
                        this);// configuration.get(APPLICATION_TOKEN).toString()
            } else {
                connMan.registerConnectionListener(this);
            }

            this.structMan = new DigitalSTROMStructureManagerImpl();
            this.sceneMan = new DigitalSTROMSceneManagerImpl(this.connMan, this.structMan);
            this.devStatMan = new DigitalSTROMDeviceStatusManagerImpl(this.connMan, this.structMan, this.sceneMan);
            structMan.generateZoneGroupNames(connMan);
            // this.devStatMan.registerTotalPowerConsumptionListener(this);
            this.devStatMan.start();

            // get Configurations
            if (configuration.get(DigitalSTROMBindingConstants.SENSOR_DATA_UPDATE_INTERVALL) != null
                    && !configuration.get(DigitalSTROMBindingConstants.SENSOR_DATA_UPDATE_INTERVALL).toString().trim()
                            .replace(" ", "").isEmpty()) {

                DigitalSTROMConfig.SENSORDATA_REFRESH_INTERVAL = Integer.parseInt(
                        configuration.get(DigitalSTROMBindingConstants.SENSOR_DATA_UPDATE_INTERVALL).toString()
                                + "000");
            }
            if (configuration.get(DigitalSTROMBindingConstants.DEFAULT_TRASH_DEVICE_DELEATE_TIME_KEY) != null
                    && !configuration.get(DigitalSTROMBindingConstants.DEFAULT_TRASH_DEVICE_DELEATE_TIME_KEY).toString()
                            .trim().replace(" ", "").isEmpty()) {

                DigitalSTROMConfig.TRASH_DEVICE_DELEATE_TIME = Integer.parseInt(configuration
                        .get(DigitalSTROMBindingConstants.DEFAULT_TRASH_DEVICE_DELEATE_TIME_KEY).toString());
            }
            if (configuration.get(DigitalSTROMBindingConstants.TRUST_CERT_PATH_KEY) != null
                    && !configuration.get(DigitalSTROMBindingConstants.TRUST_CERT_PATH_KEY).toString().trim()
                            .replace(" ", "").isEmpty()) {

                DigitalSTROMConfig.TRUST_CERT_PATH = configuration.get(DigitalSTROMBindingConstants.TRUST_CERT_PATH_KEY)
                        .toString();
            }

            if (this.thingTypeProvider != null) {
                this.thingTypeProvider.registerConnectionManagerHandler(connMan);
            }

            if (this.devListener != null) {
                for (DeviceStatusListener listener : this.devListener) {
                    this.registerDeviceStatusListener(listener);
                }
                this.devListener = null;
            }

            if (this.sceneListener != null) {
                for (SceneStatusListener listener : this.sceneListener) {
                    this.registerSceneStatusListener(listener);
                }
                this.sceneListener = null;
            }

        } else {
            logger.warn("Cannot connect to DigitalSTROMSever. Host address is not set.");
        }

    }

    @Override
    public void dispose() {
        logger.debug("Handler disposed.");

        if (this.getThing().getStatus().equals(ThingStatus.ONLINE)) {
            updateStatus(ThingStatus.OFFLINE);
        }

        this.devStatMan.stop();
        this.connMan = null;
        this.structMan = null;
        this.devStatMan = null;
    }

    @Override
    public void handleCommand(ChannelUID channelUID, Command command) {
        // nothing to do
    }

    @Override
    public void handleRemoval() {
        if (connMan == null) {
            Configuration configuration = this.getConfig();
            this.connMan = new DigitalSTROMConnectionManagerImpl(configuration.get(HOST).toString(),
                    configuration.get(USER_NAME).toString(), configuration.get(PASSWORD).toString(),
                    configuration.get(APPLICATION_TOKEN).toString(), false, this);
        }

        if (connMan.removeApplicationToken()) {
            updateStatus(ThingStatus.REMOVED);
        }
        this.connMan = null;
    }
    /**** methods to store DeviceStatusListener ****/

    /**
     * Registers a new {@link DeviceStatusListener} on the {@link DsBridgeHandler}.
     *
     * @param deviceStatusListener
     */
    public synchronized void registerDeviceStatusListener(DeviceStatusListener deviceStatusListener) {
        if (this.devStatMan != null) {
            if (deviceStatusListener == null) {
                throw new NullPointerException("It's not allowed to pass a null DeviceStatusListener.");
            }

            if (deviceStatusListener.getID() != null) {
                devStatMan.registerDeviceListener(deviceStatusListener);
            } else {
                throw new NullPointerException("It's not allowed to pass a null ID.");
            }
        } else {
            devListener = new LinkedList<DeviceStatusListener>();
            devListener.add(deviceStatusListener);
        }

    }

    public synchronized void registerThingTypeProvider(DigitalSTROMThingTypeProvider thingTypeProvider) {
        this.thingTypeProvider = thingTypeProvider;
    }

    /**
     * Unregisters a new {@link DeviceStatusListener} on the {@link DsBridgeHandler}.
     *
     * @param devicetatusListener
     */
    public void unregisterDeviceStatusListener(DeviceStatusListener deviceStatusListener) {
        if (this.devStatMan != null) {
            if (deviceStatusListener.getID() != null) {
                this.devStatMan.unregisterDeviceListener(deviceStatusListener);
            } else {
                throw new NullPointerException("It's not allowed to pass a null ID.");
            }
        }
    }

    /**
     * Registers a new {@link DeviceStatusListener} on the {@link DsBridgeHandler}.
     *
     * @param deviceStatusListener
     */
    public synchronized void registerSceneStatusListener(SceneStatusListener sceneStatusListener) {
        if (this.sceneMan != null) {
            if (sceneStatusListener == null) {
                throw new NullPointerException("It's not allowed to pass a null DeviceStatusListener.");
            }

            if (sceneStatusListener.getID() != null) {
                this.sceneMan.registerSceneListener(sceneStatusListener);
            } else {
                throw new NullPointerException("It's not allowed to pass a null ID.");
            }
        } else {
            sceneListener = new LinkedList<SceneStatusListener>();
            sceneListener.add(sceneStatusListener);
        }

    }

    /**
     * Unregisters a new {@link DeviceStatusListener} on the {@link DsBridgeHandler}.
     *
     * @param devicetatusListener
     */
    public void unregisterSceneStatusListener(SceneStatusListener sceneStatusListener) {
        if (this.sceneMan != null) {
            if (sceneStatusListener.getID() != null) {
                this.sceneMan.unregisterSceneListener(sceneStatusListener);
            } else {
                throw new NullPointerException("It's not allowed to pass a null ID.");
            }
        }
    }

    public void stopOutputValue(Device device) {
        this.devStatMan.sendStopComandsToDSS(device);
    }

    @Override
    public void onTotalPowerConsumptionChanged(int newPowerConsumption) {
        updateState(new ChannelUID(getThing().getUID(), CHANNEL_POWER_CONSUMPTION),
                new DecimalType(newPowerConsumption));

    }

    @Override
    public void onConnectionStateChange(String newConnectionState) {
        switch (newConnectionState) {
            case CONNECTION_LOST:
                updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.COMMUNICATION_ERROR,
                        "The connection to the digitalSTROM-Server can't established");
                devStatMan.stop();
                break;
            case CONNECTION_RESUMED:
                updateStatus(ThingStatus.ONLINE);
                devStatMan.restart();
                break;
            case APPLICATION_TOKEN_GENERATED:
                this.getConfig().remove(USER_NAME);
                this.getConfig().remove(PASSWORD);
            default:
                // TODO: Fehlermeldung
        }
    }

    @Override
    public void onConnectionStateChange(String newConnectionState, String reason) {
        // logger.debug(newConnectionState + " " + reason);
        if (newConnectionState.equals(NOT_AUTHENTICATED) || newConnectionState.equals(CONNECTION_LOST)) {
            switch (reason) {
                case WRONG_APP_TOKEN:
                    updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.CONFIGURATION_ERROR,
                            "User defined Applicationtoken is wrong.");
                    break;
                case WRONG_USER_OR_PASSWORD:
                    updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.CONFIGURATION_ERROR,
                            "The set username or password is wrong.");
                    break;
                case NO_USER_PASSWORD:
                    updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.CONFIGURATION_ERROR,
                            "no username or password is set to genarate Appicationtoken.");
                    break;
                case CONNECTON_TIMEOUT:
                    updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.COMMUNICATION_ERROR,
                            "Connection lost because connection timeout to Server.");
                    break;
                case HOST_NOT_FOUND:
                    updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.COMMUNICATION_ERROR,
                            "Server not found! Please check this points:\n" + " - DigitalSTROM-Server turned on?\n"
                                    + " - hostadress correct?\n" + " - ethernet cable connection established?");
                    break;
                case INVALIDE_URL:
                    updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.CONFIGURATION_ERROR, "Invalide URL is set.");
                    break;
                default:
                    // TODO: Fehlermeldung
            }
        }

    }

    public List<Device> getDevices() {
        return this.structMan.getDeviceMap() != null ? new LinkedList<Device>(this.structMan.getDeviceMap().values())
                : null;
    }

    public DigitalSTROMStructureManager getStructureManager() {
        return this.structMan;
    }

    public void sendSceneComandToDSS(InternalScene scene, boolean call_undo) {
        if (devStatMan != null) {
            devStatMan.sendSceneComandsToDSS(scene, call_undo);
        }
    }

    public List<InternalScene> getScenes() {
        return this.sceneMan.getScenes();
    }

    public DigitalSTROMConnectionManager getConnectionManager() {
        return this.connMan;
    }

}
