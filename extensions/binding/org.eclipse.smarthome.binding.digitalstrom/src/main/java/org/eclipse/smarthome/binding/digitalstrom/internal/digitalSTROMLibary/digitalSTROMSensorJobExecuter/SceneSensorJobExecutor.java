/**
 * Copyright (c) 2014-2015 openHAB UG (haftungsbeschraenkt) and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.smarthome.binding.digitalstrom.internal.digitalSTROMLibary.digitalSTROMSensorJobExecuter;

import org.eclipse.smarthome.binding.digitalstrom.internal.digitalSTROMLibary.digitalSTROMManager.DigitalSTROMConnectionManager;
import org.eclipse.smarthome.binding.digitalstrom.internal.digitalSTROMLibary.digitalSTROMSensorJobExecuter.sensorJob.SensorJob;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This class performs the sensor Jobs by DigitalSTROM Rule 9 "Application processes that do automatic
 * cyclic reads of measured values are subject to a request limit: at maximum one request per minute
 * and circuit is allowed.".
 *
 * In addition priorities can be assigned to jobs therefor an {@link SceneSensorJobExecutor} offers the methods
 * {@link #addHighPriorityJob()}, {@link #addLowPriorityJob()} and {@link #addLowPriorityJob()}.
 * <p>
 * Note:
 * In contrast to the {@link SensorJobExecutor} the {@link SceneSensorJobExecutor} will execute {@link SensorJob}s with
 * high priority
 * always before medium priority {@link SensorJob}s and so on.
 *
 *
 * @author Michael Ochel
 * @author Matthias Siegele
 *
 */
public class SceneSensorJobExecutor extends AbstractSensorJobExecutor {

    private Logger logger = LoggerFactory.getLogger(SceneSensorJobExecutor.class);

    /**
     *
     * @param connectionManager
     */
    public SceneSensorJobExecutor(DigitalSTROMConnectionManager connectionManager) {
        super(connectionManager);
    }

    /**
     * Adds a high priority SensorJob to the SensorJobExecuter.
     *
     * @param sensorJob
     */
    @Override
    public void addHighPriorityJob(SensorJob sensorJob) {
        if (sensorJob == null)
            return;
        sensorJob.setInitalisationTime(0);
        addSensorJobToCircuitScheduler(sensorJob);
        logger.debug("Add SceneSensorJob from device with dSID {} and high-priority to SceneJensorJobExecuter",
                sensorJob.getDsid());

    }

    /**
     * Adds a medium priority SensorJob to the SensorJobExecuter.
     *
     * @param sensorJob
     */
    @Override
    public void addMediumPriorityJob(SensorJob sensorJob) {
        if (sensorJob == null)
            return;
        sensorJob.setInitalisationTime(1);
        addSensorJobToCircuitScheduler(sensorJob);
        logger.debug("Add SceneSensorJob from device with dSID {} and medium-priority to SceneJensorJobExecuter",
                sensorJob.getDsid());
    }

    /**
     * Adds a low priority SensorJob to the SensorJobExecuter.
     *
     * @param sensorJob
     */
    @Override
    public void addLowPriorityJob(SensorJob sensorJob) {
        if (sensorJob == null)
            return;
        sensorJob.setInitalisationTime(2);
        addSensorJobToCircuitScheduler(sensorJob);
        logger.debug("Add SceneSensorJob from device with dSID {} and low-priority to SceneJensorJobExecuter",
                sensorJob.getDsid());
    }

}
