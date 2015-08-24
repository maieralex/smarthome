/**
 * Copyright (c) 2014-2015 openHAB UG (haftungsbeschraenkt) and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.smarthome.binding.digitalstrom.internal.digitalSTROMLibary.digitalSTROMManager;

import java.util.List;

import org.eclipse.smarthome.binding.digitalstrom.internal.digitalSTROMLibary.digitalSTROMListener.SceneStatusListener;
import org.eclipse.smarthome.binding.digitalstrom.internal.digitalSTROMLibary.digitalSTROMStructure.digitalSTROMDevices.Device;
import org.eclipse.smarthome.binding.digitalstrom.internal.digitalSTROMLibary.digitalSTROMStructure.digitalSTROMDevices.deviceParameters.DSID;
import org.eclipse.smarthome.binding.digitalstrom.internal.digitalSTROMLibary.digitalSTROMStructure.digitalSTROMScene.InternalScene;
import org.eclipse.smarthome.binding.digitalstrom.internal.digitalSTROMLibary.digitalSTROMStructure.digitalSTROMScene.sceneEvent.EventItem;

/**
 *
 * @author Michael Ochel - Initial contribution
 * @author Mathias Siegele - Initial contribution
 *
 */
public interface DigitalSTROMSceneManager {

    /**
     * only works on openhabEvent! please copy "openhab/openhab.js" to your dSS
     * server (/usr/share/dss/add-ons/) and "openhab.xml" to
     * /usr/share/dss/data/subscriptions.d/ than you need to restart your dSS
     *
     * If you don't, you will not get detailed infos about, what exactly
     * happened (for example: which device was turned on by a browser or handy
     * app )
     *
     * @param eventItem
     */
    public void handleEvent(EventItem eventItem);

    /**
     *
     * @param scene
     */
    public void callInternalScene(InternalScene scene);

    /**
     *
     * @param sceneID
     */
    public void callInternalScene(String sceneID);

    /**
     *
     * @param dSID
     * @param sceneID
     */
    public void callDeviceScene(DSID dSID, Short sceneID);

    /**
     *
     * @param device
     * @param sceneID
     */
    public void callDeviceScene(Device device, Short sceneID);

    /**
     *
     * @param scene
     */
    public void undoInternalScene(InternalScene scene);

    /**
     *
     * @param sceneID
     */
    public void undoInternalScene(String sceneID);

    /**
     *
     * @param dSID
     */
    public void undoDeviceScene(DSID dSID);

    /**
     *
     * @param device
     */
    public void undoDeviceScene(Device device);

    /**
     *
     * @param sceneListener
     */
    public void registerSceneListener(SceneStatusListener sceneListener);

    /**
     *
     * @param sceneListener
     */
    public void unregisterSceneListener(SceneStatusListener sceneListener);

    /**
     *
     * @param intScene
     */
    public void addInternalScene(InternalScene intScene);

    /**
     *
     * @param dsid
     * @param sceneId
     */
    public void addEcho(String dsid, short sceneId);

    /**
     *
     * @param internalSceneID
     */
    public void addEcho(String internalSceneID);

    public List<InternalScene> getScenes();

    boolean scenesGenerated();

    void generateScenes();

    boolean isDiscoveryRegistrated();
}
