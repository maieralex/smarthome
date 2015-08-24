/**
 * Copyright (c) 2014-2015 openHAB UG (haftungsbeschraenkt) and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.smarthome.binding.digitalstrom.internal.digitalSTROMLibary.digitalSTROMListener;

import org.eclipse.smarthome.binding.digitalstrom.internal.digitalSTROMLibary.digitalSTROMStructure.digitalSTROMScene.InternalScene;

/**
 * The {@link SceneStatusListener} is notified when a {@link InternalScene} status has changed or a
 * {@link InternalScene} has been removed or added.
 *
 * @author Michael Ochel - Initial contribution
 * @author Mathias Siegele - Initial contribution
 *
 */
public interface SceneStatusListener {

    public final static String SCENE_DESCOVERY = "SceneDiscovey";

    /**
     * This method is called whenever the state of the given scene has changed.
     *
     * @param device
     *
     */
    public void onSceneStateChanged(boolean flag);

    /**
     * This method is called whenever a scene is removed.
     *
     * @param device
     *
     */
    public void onSceneRemoved(InternalScene scene);

    /**
     * This method is called whenever a scene is added.
     *
     * @param device
     *
     */
    public void onSceneAdded(InternalScene scene);

    /**
     * Return the id of this {@link SceneStatusListener}.
     * 
     * @return listener id
     */
    public String getID();

}
