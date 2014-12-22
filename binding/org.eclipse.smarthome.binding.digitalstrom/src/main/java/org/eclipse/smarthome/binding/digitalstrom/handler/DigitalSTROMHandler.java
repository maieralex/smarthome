/**
 * Copyright (c) 2014 openHAB UG (haftungsbeschraenkt) and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.smarthome.binding.digitalstrom.handler;

import static org.eclipse.smarthome.binding.digitalstrom.DigitalSTROMBindingConstants.*;

import org.eclipse.smarthome.core.thing.ChannelUID;
import org.eclipse.smarthome.core.thing.Thing;
import org.eclipse.smarthome.core.thing.binding.BaseThingHandler;
import org.eclipse.smarthome.core.types.Command;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The {@link DigitalSTROMHandler} is responsible for handling commands, which are
 * sent to one of the channels.
 * 
 * @author Alex Maier - Initial contribution
 */
public class DigitalSTROMHandler extends BaseThingHandler {

    private Logger logger = LoggerFactory.getLogger(DigitalSTROMHandler.class);

	public DigitalSTROMHandler(Thing thing) {
		super(thing);
	}

	@Override
	public void handleCommand(ChannelUID channelUID, Command command) {
       /* if(channelUID.getId().equals(CHANNEL_1)) {
            // TODO: handle command
        }*/
	}
}
