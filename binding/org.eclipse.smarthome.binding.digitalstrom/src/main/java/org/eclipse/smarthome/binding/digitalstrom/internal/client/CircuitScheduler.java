package org.eclipse.smarthome.binding.digitalstrom.internal.client;

import java.util.Comparator;
import java.util.Iterator;
import java.util.PriorityQueue;

import org.eclipse.smarthome.binding.digitalstrom.DigitalSTROMBindingConstants;
import org.eclipse.smarthome.binding.digitalstrom.internal.client.entity.DSID;
import org.eclipse.smarthome.binding.digitalstrom.internal.client.job.SensorJob;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 
 * @author Michael Ochel
 * @author Matthias Siegele
 *
 */
public class CircuitScheduler {
	
	private class SensorJobComparator implements Comparator<SensorJob>{

		@Override
		public int compare(SensorJob job1, SensorJob job2) {
			return ((Long) job1.getInitalisationTime()).compareTo(job2.getInitalisationTime());
		}
		
	}
	
	private final DSID meterDSID;
	private long nextExecutionTime = System.currentTimeMillis();
	private PriorityQueue<SensorJob> sensorJobQueue = new PriorityQueue<SensorJob>(10, new SensorJobComparator());
	
	private Logger logger = LoggerFactory.getLogger(CircuitScheduler.class);
	
	/**
	 * Creates a new CircuitScheduler.
	 * 
	 * @param meterDSID
	 */
	public CircuitScheduler(DSID meterDSID){
		this.meterDSID = meterDSID;
	}
	
	/**
	 * Creates a new CircuitScheduler and add the first SensorJob to the CircuitScheduler.
	 * @param sensorJob
	 */
	public CircuitScheduler(SensorJob sensorJob){
		this.meterDSID = sensorJob.getMeterDSID();
		this.sensorJobQueue.add(sensorJob);
		logger.debug("create circuitScheduler: "+ this.getMeterDSID() +" and add sensorJob: " + sensorJob.getDsid().toString());
	}
	
	/**
	 * Returns the ds-Meter-ID of the ds-Meter in which the jobs are to be executed.  
	 * 
	 * @return ds-Meter-ID
	 */
	public DSID getMeterDSID(){
		return this.meterDSID;
	}
	
	/**
	 * Adds a new SensorJob to this CircuitScheduler.
	 * 
	 * @param sensorJob
	 */
	public void addSensorJob(SensorJob sensorJob){
		synchronized(sensorJobQueue){
			if(!this.sensorJobQueue.contains(sensorJob)){
				sensorJobQueue.add(sensorJob);
				logger.debug("add sensorJob: " + sensorJob.getDsid().toString() + "to circuitScheduler: " + this.getMeterDSID());
			} else logger.debug("sensorJob: " + sensorJob.getDsid().toString() + " allready exist");
		}
	}
	
	/**
	 * Returns the next SensorJob which can be executed or null if there is no more SensorJob to execute 
	 * or the wait time between SensorJob executions has not yet expired. 
	 * 
	 * @return next SensorJob
	 */
	public SensorJob getNextSensorJob(){
		synchronized(sensorJobQueue){
			if(sensorJobQueue.peek() != null && this.nextExecutionTime <= System.currentTimeMillis()){
				nextExecutionTime = System.currentTimeMillis() + DigitalSTROMBindingConstants.DEFAULT_SENSOR_READING_WAIT_TIME;
				return sensorJobQueue.poll();
			} else return null;
		}
	}
	
	/**
	 * Returns the time when the next SensorJob can be executed.
	 * 
	 * @return next SesnorJob execution time 
	 */
	public Long getNextExecutionTime(){
		return this.nextExecutionTime;
	}
	
	/**
	 * Remove all SensorJobs of a specific ds-device.
	 * 
	 * @param dsid of the ds-device
	 */
	public void removeSensorJob(DSID dsid){
		synchronized(sensorJobQueue){
			for (Iterator<SensorJob> iter = sensorJobQueue.iterator(); iter
					.hasNext();) {
				SensorJob job = iter.next();
				if (job.getDsid().equals(dsid))
					iter.remove();
			}
		
			logger.debug("Remove SensorJobs from device with DSID {}."+ dsid);
		}
	}
	
	/**
	 * 
	 * @return
	 */
	public boolean noMoreJobs(){
		synchronized(sensorJobQueue){
			return this.sensorJobQueue.isEmpty();
		}
	}
	
	
}
