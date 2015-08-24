/**
 * Copyright (c) 2014-2015 openHAB UG (haftungsbeschraenkt) and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.smarthome.binding.digitalstrom.internal.digitalSTROMLibary.digitalSTROMSensorJobExecuter;

import org.eclipse.smarthome.binding.digitalstrom.internal.digitalSTROMLibary.digitalSTROMConfiguration.DigitalSTROMConfig;
import org.eclipse.smarthome.binding.digitalstrom.internal.digitalSTROMLibary.digitalSTROMManager.DigitalSTROMConnectionManager;
import org.eclipse.smarthome.binding.digitalstrom.internal.digitalSTROMLibary.digitalSTROMSensorJobExecuter.sensorJob.SensorJob;

/**
 * This class performs the sensor Jobs by DigitalSTROM Rule 9 "Application processes that do automatic
 * cyclic reads of measured values are subject to a request limit: at maximum one request per minute
 * and circuit is allowed.".
 * <p>
 * In addition priorities can be assigned to jobs .
 *
 * @author Michael Ochel
 * @author Matthias Siegele
 *
 */
public class SensorJobExecutor extends AbstractSensorJobExecutor {

    private final long mediumFactor = DigitalSTROMConfig.SENSOR_READING_WAIT_TIME
            * DigitalSTROMConfig.MEDIUM_PRIORITY_FACTOR;
    private final long lowFactor = DigitalSTROMConfig.SENSOR_READING_WAIT_TIME * DigitalSTROMConfig.LOW_PRIORITY_FACTOR;

    public SensorJobExecutor(DigitalSTROMConnectionManager connectionManager) {
        super(connectionManager);
    }

    /**
     * Adds a high priority SensorJob to the SensorJobExecuter.
     * 
     * @param sensorJob
     */
    public void addHighPriorityJob(SensorJob sensorJob) {
        if (sensorJob == null)
            return;
        addSensorJobToCircuitScheduler(sensorJob);
    }

    /**
     * Adds a medium priority SensorJob to the SensorJobExecuter.
     * 
     * @param sensorJob
     */
    public void addMediumPriorityJob(SensorJob sensorJob) {
        if (sensorJob == null)
            return;
        sensorJob.setInitalisationTime(sensorJob.getInitalisationTime() + this.mediumFactor);
        addSensorJobToCircuitScheduler(sensorJob);
    }

    /**
     * Adds a low priority SensorJob to the SensorJobExecuter.
     * 
     * @param sensorJob
     */
    public void addLowPriorityJob(SensorJob sensorJob) {
        if (sensorJob == null)
            return;
        sensorJob.setInitalisationTime(sensorJob.getInitalisationTime() + this.lowFactor);
        addSensorJobToCircuitScheduler(sensorJob);
    }

}
