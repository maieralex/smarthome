/**
 * Copyright (c) 2010-2014, openHAB.org and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.smarthome.binding.digitalstrom.internal.client.entity;


/**
 * The {@link DeviceSceneSpec} represents a DigitalSTROM-Device-Scene-Specification of an device scene.
 * 
 * @author	Alexander Betker
 * @since 1.3.0
 * @see http://developer.digitalstrom.org/Architecture/ds-basics.pdf
 */
public interface DeviceSceneSpec {
	
	/**
	 * Returns the Scene.
	 * 
	 * @return scene
	 */
	public Scene getScene();
	
	/**
	 * Returns if the the isDontCare flag is set otherwise false.
	 * 
	 * @return is don't care (true | false)
	 */
	public boolean isDontCare();
	
	/**
	 * Sets the isDontCare flag to the given flag.
	 * 
	 * @param is don't care (true | false)
	 */
	public void setDontcare(boolean dontcare);
	
	/**
	 * Returns if the the isLocalPrio flag is set otherwise false.
	 * 
	 * @return is local prio (true | false)
	 */
	public boolean isLocalPrio();
	
	/**
	 * Sets the isLocalPrio flag to the given flag.
	 * 
	 * @param is local prio (true | false)
	 */
	public void setLocalPrio(boolean localPrio);
	
	/**
	 * Returns if the the isSpecialMode flag is set otherwise false.
	 * 
	 * @return is special mode (true | false)
	 */
	public boolean isSpecialMode();
	
	/**
	 * Sets the isSpecialMode flag to the given flag.
	 * 
	 * @param is special mode (true | false)
	 */
	public void setSpecialMode(boolean specialMode);
	
	/**
	 * Returns if the the isFlashMode flag is set otherwise false.
	 * 
	 * @return is flash mode (true | false)
	 */
	public boolean isFlashMode();
	
	/**
	 * Sets the isFlashMode flag to the given flag.
	 * 
	 * @param is flash mode (true | false)
	 */
	public void setFlashMode(boolean flashMode);
	
}
