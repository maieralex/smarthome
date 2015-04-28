package org.eclipse.smarthome.binding.digitalstrom.internal.client;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

import org.eclipse.smarthome.binding.digitalstrom.DigitalSTROMBindingConstants;
import org.eclipse.smarthome.binding.digitalstrom.handler.DssBridgeHandler;
import org.eclipse.smarthome.binding.digitalstrom.internal.client.entity.DSID;
import org.eclipse.smarthome.binding.digitalstrom.internal.client.entity.Device;
import org.eclipse.smarthome.binding.digitalstrom.internal.client.impl.DigitalSTROMJSONImpl;
import org.eclipse.smarthome.binding.digitalstrom.internal.client.job.SensorJob;

/**
 * This class performs the sensor Jobs by DigitalSTROM Rule 9 "Application processes that do automatic 
 * cyclic reads of measured values are subject to a request limit: at maximum one request per minute
 * and circuit is allowed.". 
 * In addition priorities can be assigned to jobs .
 * 
 * @author Michael Ochel
 * @author Matthias Siegele
 * 
 */
public class SensorJobExecutor {
	private boolean shutdown = false;

	private long lowestNextExecutionTime = 0;
	private long sleepTime = 1000;
	private final long mediumFactor = DigitalSTROMBindingConstants.DEFAULT_SENSOR_READING_WAIT_TIME * DigitalSTROMBindingConstants.MEDIUM_PRIORITY_FACTOR;
	private final long lowFactor = DigitalSTROMBindingConstants.DEFAULT_SENSOR_READING_WAIT_TIME * DigitalSTROMBindingConstants.LOW_PRIORITY_FACTOR;
	
	private final DigitalSTROMJSONImpl digitalSTROM;
	
	private DssBridgeHandler dssBrideHandler = null;
	
	//private Logger logger = LoggerFactory.getLogger(SensorJobExecutor.class);

	private List<CircuitScheduler> circuitSchedulerList = Collections
			.synchronizedList(new LinkedList<CircuitScheduler>());
	
	Thread executer = new Thread(){
		public void run() {
			while (!shutdown) {
				lowestNextExecutionTime = System.currentTimeMillis() + DigitalSTROMBindingConstants.DEFAULT_SENSOR_READING_WAIT_TIME;
				boolean noMoreJobs = true;
			
				synchronized(circuitSchedulerList){
					for(CircuitScheduler circuit: circuitSchedulerList){
						SensorJob sensorJob = circuit.getNextSensorJob();
						if(sensorJob != null && dssBrideHandler.checkConnection()){
							sensorJob.execute(digitalSTROM, dssBrideHandler.getSessionToken());
						} else{
							if(lowestNextExecutionTime > circuit.getNextExecutionTime() && 
									circuit.getNextExecutionTime() > System.currentTimeMillis()){
								lowestNextExecutionTime = circuit.getNextExecutionTime();
							}
						}
						if(!circuit.noMoreJobs()){
							noMoreJobs = false;
						}
					}
				}
				try {
					if(noMoreJobs){
						synchronized(this){
							this.wait();
						}
					}else{
						sleepTime = lowestNextExecutionTime - System.currentTimeMillis();
						if(sleepTime > 0){
							sleep(sleepTime);
						}
					}
				} catch (InterruptedException e) {
					
				}
			}	
		}
	};

	/**
	 * Creates a new SensorJobExecuter.
	 * 
	 * @param digitalStrom
	 * @param dssBrideHandler
	 */
	public SensorJobExecutor(DigitalSTROMJSONImpl digitalStrom, DssBridgeHandler dssBrideHandler){
		this.digitalSTROM = digitalStrom;
		this.dssBrideHandler = dssBrideHandler;
	}
	
	/**
	 * Stops the SensorJobExecuter Thread.
	 * 
	 */
	public synchronized void shutdown() {
		this.shutdown = true;
	}
	
	
	/**
	 * Restarts the SensorJobExecuter Thread.
	 * 
	 */
	public synchronized void wakeUp() {
		this.shutdown = false;
		executer.run();
	}
	
	/**
	 * Starts the SensorJobExecuter Thread.
	 */
	public void startExecuter(){
		executer.start();
	}
	
	/**
	 * Add a high priority SensorJob to the SensorJobExecuter.
	 * 
	 * @param sensorJob
	 */
	public void addHighPriorityJob(SensorJob sensorJob) {
		if(sensorJob == null) return;
		addSensorJobToCircuitScheduler(sensorJob);
	}

	/**
	 * Add a high priority SensorJob to the SensorJobExecuter.
	 * 
	 * @param sensorJob
	 */
	public void addMediumPriorityJob(SensorJob sensorJob) {
		if(sensorJob == null) return;
		sensorJob.setInitalisationTime(sensorJob.getInitalisationTime()+this.mediumFactor);
		addSensorJobToCircuitScheduler(sensorJob);
	}

	/**
	 * Add a high priority SensorJob to the SensorJobExecuter.
	 * 
	 * @param sensorJob
	 */
	public void addLowPriorityJob(SensorJob sensorJob) {
		if(sensorJob == null) return;
		sensorJob.setInitalisationTime(sensorJob.getInitalisationTime()+this.lowFactor);
		addSensorJobToCircuitScheduler(sensorJob);
	}

	private void addSensorJobToCircuitScheduler(SensorJob sensorJob){
		synchronized (this.circuitSchedulerList) {
			CircuitScheduler circuit = getCircuitScheduler(sensorJob.getMeterDSID());
			if(circuit != null){
				circuit.addSensorJob(sensorJob);
				synchronized(executer){
					executer.notifyAll();
				}
			} else{
				circuit = new CircuitScheduler(sensorJob);
				this.circuitSchedulerList.add(circuit);
			}
			if(circuit.getNextExecutionTime() <= System.currentTimeMillis()){
				executer.interrupt();
			}
		}
	}
	
	private CircuitScheduler getCircuitScheduler(DSID dsid){
		for(CircuitScheduler circuit : this.circuitSchedulerList){
			if (circuit.getMeterDSID().equals(dsid)){
				return circuit;
			}
		}
		return null;
	}
	
	/**
	 * Remove all SensorJobs of a specific ds-device.
	 * 
	 * @param dsid of the ds-device
	 */
	public void removeSensorJobs(DSID dsid) {
		Device device = this.dssBrideHandler.getDeviceByDSID(dsid.getValue());
		if(device != null){
			CircuitScheduler circuit = getCircuitScheduler(device.getMeterDSID());
			if(circuit != null){
				circuit.removeSensorJob(dsid);
			}	
		}		
	}

}
