package org.eclipse.smarthome.binding.digitalstrom.internal.client;

import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import org.eclipse.smarthome.binding.digitalstrom.DigitalSTROMBindingConstants;
import org.eclipse.smarthome.binding.digitalstrom.handler.DssBridgeHandler;
import org.eclipse.smarthome.binding.digitalstrom.internal.client.entity.DSID;
import org.eclipse.smarthome.binding.digitalstrom.internal.client.impl.DigitalSTROMJSONImpl;
import org.eclipse.smarthome.binding.digitalstrom.internal.client.job.SensorJob;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * In order to avoid many sensor readings in a time, this thread starts the
 * jobs, after the old one is finished
 * 
 * @author Alexander Betker
 * @since 1.3.0
 * 
 */
public class SensorJobExecutor extends Thread {

	/* 
	 * NOTE:
	 * Optimierung: 
	 * 1. Thread schlafen legen wenn es keine Jobs gibt und wecken, wenn ein neuer Job hinzugefügt wird
	 * 2. evtl. statt LinkedList Prioritätswarteschlange
	 */
	
	private boolean shutdown = false;

	private final int sleepTime = DigitalSTROMBindingConstants.DEFAULT_DEVICE_LISTENER_REFRESH_INTERVAL;
	private final DigitalSTROMJSONImpl digitalSTROM;
	
	private DssBridgeHandler dssBrideHandler = null;
	
	private Logger logger = LoggerFactory.getLogger(SensorJobExecutor.class);

	private List<SensorJob> highPrioritySensorJobs = Collections
			.synchronizedList(new LinkedList<SensorJob>());
	private List<SensorJob> mediumPrioritySensorJobs = Collections
			.synchronizedList(new LinkedList<SensorJob>());
	private List<SensorJob> lowPrioritySensorJobs = Collections
			.synchronizedList(new LinkedList<SensorJob>());
	
	public SensorJobExecutor(DigitalSTROMJSONImpl digitalStrom, DssBridgeHandler dssBrideHandler){
		this.digitalSTROM = digitalStrom;
		this.dssBrideHandler = dssBrideHandler;
	}
	
	@Override
	public void run() {

		while (!this.shutdown) {
			if(this.dssBrideHandler.checkConnection()){
				SensorJob job = getHighPriorityJob();

				if (job == null) {
					job = getMediumPriorityJob();
					if (job == null)
						job = getLowPriorityJob();
				}
				if (job != null) {
					job.execute(digitalSTROM, this.dssBrideHandler.getSessionToken());
				}else{
					//schlafen legen
				}
			}
			try {
				sleep(this.sleepTime);
			} catch (InterruptedException e) {
				this.shutdown();
				logger.error("InterruptedException in SensorJobExecutor Thread ... "
						+ e.getStackTrace());
			}
		}
	}

	public synchronized void shutdown() {
		this.shutdown = true;
	}
	
	public void addHighPriorityJob(SensorJob sensorJob) {
		synchronized (highPrioritySensorJobs) {
			if (!highPrioritySensorJobs.contains(sensorJob)) {
				highPrioritySensorJobs.add(sensorJob);
				//aufwecken
			}
		}
	}

	public void addMediumPriorityJob(SensorJob sensorJob) {
		synchronized (mediumPrioritySensorJobs) {
			if (!mediumPrioritySensorJobs.contains(sensorJob)) {
				mediumPrioritySensorJobs.add(sensorJob);
				//aufwecken
			}
		}
	}

	public void addLowPriorityJob(SensorJob sensorJob) {
		synchronized (lowPrioritySensorJobs) {
			if (!lowPrioritySensorJobs.contains(sensorJob)) {
				lowPrioritySensorJobs.add(sensorJob);
				//aufwecken
			}
		}
	}

	private SensorJob getLowPriorityJob() {
		SensorJob job = null;
		synchronized (lowPrioritySensorJobs) {
			if (lowPrioritySensorJobs.size() > 0) {
				job = lowPrioritySensorJobs.remove(0);
			}
		}
		return job;
	}

	private SensorJob getMediumPriorityJob() {
		SensorJob job = null;
		synchronized (mediumPrioritySensorJobs) {
			if (mediumPrioritySensorJobs.size() > 0) {
				//job = mediumPrioritySensorJobs.get(0);
				job = mediumPrioritySensorJobs.remove(0);
			}
		}
		return job;
	}

	private SensorJob getHighPriorityJob() {
		SensorJob job = null;
		synchronized (highPrioritySensorJobs) {
			if (highPrioritySensorJobs.size() > 0) {
				//job = highPrioritySensorJobs.get(0);
				job = highPrioritySensorJobs.remove(0);
			}
		}
		return job;
	}

	public void removeSensorJobs(DSID dsid) {
		synchronized (lowPrioritySensorJobs) {
			for (Iterator<SensorJob> iter = lowPrioritySensorJobs.iterator(); iter
					.hasNext();) {
				SensorJob job = iter.next();
				if (job.getDsid().equals(dsid))
					iter.remove();
			}
		}
		synchronized (mediumPrioritySensorJobs) {
			for (Iterator<SensorJob> iter = mediumPrioritySensorJobs.iterator(); iter
					.hasNext();) {
				SensorJob job = iter.next();
				if (job.getDsid().equals(dsid))
					iter.remove();
			}
		}
		synchronized (highPrioritySensorJobs) {
			for (Iterator<SensorJob> iter = highPrioritySensorJobs.iterator(); iter
					.hasNext();) {
				SensorJob job = iter.next();
				if (job.getDsid().equals(dsid))
					iter.remove();
			}
		}
	}

}
