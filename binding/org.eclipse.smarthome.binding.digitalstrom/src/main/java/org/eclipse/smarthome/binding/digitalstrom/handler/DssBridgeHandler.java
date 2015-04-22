/**
 * Copyright (c) 2014 openHAB UG (haftungsbeschraenkt) and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.smarthome.binding.digitalstrom.handler;

import static org.eclipse.smarthome.binding.digitalstrom.DigitalSTROMBindingConstants.APPLICATION_TOKEN;
import static org.eclipse.smarthome.binding.digitalstrom.DigitalSTROMBindingConstants.CHANNEL_POWER_CONSUMPTION;
import static org.eclipse.smarthome.binding.digitalstrom.DigitalSTROMBindingConstants.DEFAULT_CONNECTION_TIMEOUT;
import static org.eclipse.smarthome.binding.digitalstrom.DigitalSTROMBindingConstants.DEFAULT_READ_TIMEOUT;
import static org.eclipse.smarthome.binding.digitalstrom.DigitalSTROMBindingConstants.HOST;
import static org.eclipse.smarthome.binding.digitalstrom.DigitalSTROMBindingConstants.PASSWORD;
import static org.eclipse.smarthome.binding.digitalstrom.DigitalSTROMBindingConstants.THING_TYPE_DSS_BRIDGE;
import static org.eclipse.smarthome.binding.digitalstrom.DigitalSTROMBindingConstants.USER_NAME;

import java.net.HttpURLConnection;
import java.util.Calendar;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import org.eclipse.smarthome.binding.digitalstrom.DigitalSTROMBindingConstants;
import org.eclipse.smarthome.binding.digitalstrom.internal.client.DigitalSTROMAPI;
import org.eclipse.smarthome.binding.digitalstrom.internal.client.DigitalSTROMEventListener;
import org.eclipse.smarthome.binding.digitalstrom.internal.client.SensorJobExecutor;
import org.eclipse.smarthome.binding.digitalstrom.internal.client.constants.MeteringTypeEnum;
import org.eclipse.smarthome.binding.digitalstrom.internal.client.constants.MeteringUnitsEnum;
import org.eclipse.smarthome.binding.digitalstrom.internal.client.constants.SensorIndexEnum;
import org.eclipse.smarthome.binding.digitalstrom.internal.client.constants.ZoneSceneEnum;
import org.eclipse.smarthome.binding.digitalstrom.internal.client.entity.Apartment;
import org.eclipse.smarthome.binding.digitalstrom.internal.client.entity.CachedMeteringValue;
import org.eclipse.smarthome.binding.digitalstrom.internal.client.entity.DSID;
import org.eclipse.smarthome.binding.digitalstrom.internal.client.entity.DetailedGroupInfo;
import org.eclipse.smarthome.binding.digitalstrom.internal.client.entity.Device;
import org.eclipse.smarthome.binding.digitalstrom.internal.client.entity.DeviceSceneSpec;
import org.eclipse.smarthome.binding.digitalstrom.internal.client.entity.DeviceStateUpdate;
import org.eclipse.smarthome.binding.digitalstrom.internal.client.entity.Zone;
import org.eclipse.smarthome.binding.digitalstrom.internal.client.impl.DigitalSTROMJSONImpl;
import org.eclipse.smarthome.binding.digitalstrom.internal.client.job.DeviceConsumptionSensorJob;
import org.eclipse.smarthome.binding.digitalstrom.internal.client.job.SensorJob;
import org.eclipse.smarthome.config.core.Configuration;
import org.eclipse.smarthome.core.library.types.DecimalType;
import org.eclipse.smarthome.core.thing.Bridge;
import org.eclipse.smarthome.core.thing.ChannelUID;
import org.eclipse.smarthome.core.thing.Thing;
import org.eclipse.smarthome.core.thing.ThingStatus;
import org.eclipse.smarthome.core.thing.ThingTypeUID;
import org.eclipse.smarthome.core.thing.binding.BaseBridgeHandler;
import org.eclipse.smarthome.core.thing.binding.ThingHandler;
import org.eclipse.smarthome.core.types.Command;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The {@link DigitalSTROMHandler} is responsible for handling commands, which are
 * sent to one of the channels.
 * 
 * @author Alex Maier - Initial contribution
 * 
 */
public class DssBridgeHandler extends BaseBridgeHandler {


	private Logger logger = LoggerFactory.getLogger(DssBridgeHandler.class);
    
    public final static Set<ThingTypeUID> SUPPORTED_THING_TYPES = Collections.singleton(THING_TYPE_DSS_BRIDGE);

    /****configuration****/
    private DigitalSTROMAPI digitalSTROMClient = null;
    private String applicationToken = null;
    private String sessionToken = "123";
    
    private static final int POLLING_FREQUENCY = 10; // in seconds
	private final int BIN_CHECK_TIME = 360000; //in milliseconds
    
    /****States****/
    private boolean lastConnectionState = false;
    private long lastBinCheck = 0;
    private int consumption = 0;
    
    /****Maps****/
    private HashMap<String, DeviceStatusListener> deviceStatusListeners = new HashMap<String, DeviceStatusListener>();
   
    private List<TrashDevice> trashDevices = new LinkedList<TrashDevice>(); 
    
    private HashMap<String, Device> deviceMap = new HashMap<String, Device>();
    private HashMap<String, String> dSUIDtoDSID = new HashMap<String, String>();
    
	// zoneID - Map < groupID, List<dsid-String>>
	private Map<Integer, Map<Short, List<String>>> digitalSTROMZoneGroupMap = Collections
			.synchronizedMap(new HashMap<Integer, Map<Short, List<String>>>());

	/****Threads****/
	private DigitalSTROMEventListener digitalSTROMEventListener = null;
	private SensorJobExecutor sensorJobExecuter = null;
    
	private ScheduledFuture<?> pollingJob;
	
	private Runnable pollingRunnable = new Runnable() {

        @Override
        public void run() {
        	if(checkConnection()){
        		HashMap<String, Device> tempDeviceMap = new HashMap<String, Device>(deviceMap);
        		List<Device> currentDeviceList = new LinkedList<Device>(digitalSTROMClient.getApartmentDevices(sessionToken, false));
        		
        		//update the current total power consumption
        		int tempConsumtion = 0;
        		for(CachedMeteringValue value:digitalSTROMClient.getLatest(sessionToken, 
        						MeteringTypeEnum.consumption, 
        						".meters"+ 
        						digitalSTROMClient.getMeterList(sessionToken).toString().replace(" ", "").replace("[", "(").replace("]", ")"), 
        						MeteringUnitsEnum.W)){
        			tempConsumtion += value.getValue();
        		}
        		if(tempConsumtion != consumption){
        			consumption = tempConsumtion;
        			updateState(new ChannelUID(getThing().getUID(), 
        				CHANNEL_POWER_CONSUMPTION), 
        				new DecimalType(consumption));
        		}
        		
        		while (!currentDeviceList.isEmpty()){
        			handleStructure(digitalSTROMClient
        					.getApartmentStructure(sessionToken));
        			
        			Device currentDevice = currentDeviceList.remove(0);
        			String currentDeviceDSUID = currentDevice.getDSUID();
        			Device eshDevice = tempDeviceMap.remove(currentDeviceDSUID);
        			
        			if(eshDevice != null){
        				//check device availability has changed and inform the deviceStatusListener about the change
        				if(currentDevice.isPresent() != eshDevice.isPresent()){
        					eshDevice.setIsPresent(currentDevice.isPresent());
        					if(deviceStatusListeners.get(currentDeviceDSUID) != null){
        						if(eshDevice.isPresent()){
        							deviceStatusListeners.get(currentDeviceDSUID).onDeviceAdded(eshDevice);
        						} else{
        							deviceStatusListeners.get(currentDeviceDSUID).onDeviceRemoved(eshDevice);
        						}
        					}
        				}
        				
        				if(deviceStatusListeners.get(currentDeviceDSUID) != null && eshDevice.isPresent()){
        					logger.debug("Check device updates");
        					if(!eshDevice.isAddToESH()){
        						logger.debug("Set device is add to esh");
        						eshDevice.setIsAddToESH(true);
        						logger.debug("inform listener about the added Device");
        			    		if(eshDevice.isPresent()){
        			    			deviceStatusListeners.get(currentDeviceDSUID).onDeviceAdded(eshDevice);
        			    		}else{
        			    			deviceStatusListeners.get(currentDeviceDSUID).onDeviceRemoved(eshDevice);
        			    		}
        						
        					}
        					while(!eshDevice.isDeviceUpToDate()){
        						DeviceStateUpdate deviceStateUpdate = eshDevice.getNextDeviceUpdateState();
        						if(deviceStateUpdate.getType() != DeviceStateUpdate.UPDATE_BRIGHTNESS){
        							sendComandsToDSS(eshDevice, deviceStateUpdate);
        						} else{
        							DeviceStateUpdate nextDeviceStateUpdate = eshDevice.getNextDeviceUpdateState();
        							while(nextDeviceStateUpdate != null && nextDeviceStateUpdate.getType() == DeviceStateUpdate.UPDATE_BRIGHTNESS){
        								deviceStateUpdate = nextDeviceStateUpdate;
        								nextDeviceStateUpdate = eshDevice.getNextDeviceUpdateState();
        							}
        							sendComandsToDSS(eshDevice, deviceStateUpdate);
        							if(nextDeviceStateUpdate != null){
        								sendComandsToDSS(eshDevice, nextDeviceStateUpdate);
        							}
        						}
        					 
        					}
        					
        					if(!eshDevice.isESHThingUpToDate()){
        						deviceStatusListeners.get(currentDeviceDSUID).onDeviceStateChanged(eshDevice);
        						logger.debug("inform deviceStatusListener from  Device \""
        								+ currentDeviceDSUID
        								+ "\" about update ESH-Update");
        					}
        				        				
        					if(!eshDevice.isSensorDataUpToDate()){
        						logger.info("Device need SensorData update");
        		        		
        						if(!eshDevice.isPowerConsumptionUpToDate()){
        						
        							updateSensorData(new DeviceConsumptionSensorJob(eshDevice, SensorIndexEnum.ACTIVE_POWER), eshDevice.getPowerConsumptionRefreshPriority());
        						}
        					
        						if(!eshDevice.isEnergyMeterUpToDate()){
        							updateSensorData(new DeviceConsumptionSensorJob(eshDevice, SensorIndexEnum.OUTPUT_CURRENT), eshDevice.getEnergyMeterRefreshPriority());
        						}	
        					
        						if(!eshDevice.isElectricMeterUpToDate()){
        							updateSensorData(new DeviceConsumptionSensorJob(eshDevice, SensorIndexEnum.ELECTRIC_METER), eshDevice.getEnergyMeterRefreshPriority());
        						}
        					}
        				}
        				
        			} else{
        				logger.debug("Found new Device!");
        				
        				if(trashDevices.isEmpty()){
        					deviceMap.put(currentDeviceDSUID, currentDevice);
        					dSUIDtoDSID.put(currentDevice.getDSID().getValue(), currentDeviceDSUID);
        					logger.debug("trashDevices are empty, add Device to the deviceMap!");
        				} else{
        					logger.debug("Search device in trashDevices.");
        					
        					boolean found = false;
        					for(TrashDevice trashDevice: trashDevices){
        						if(trashDevice.getDevice().equals(currentDevice)){
        							Device device =  trashDevice.getDevice();
        							trashDevices.remove(trashDevice);
        							deviceMap.put(device.getDSUID(), device);
        							found = true;
        							logger.debug("Found device in trashDevices, add TrashDevice to the deviceMap!");
        						}
        					 } 
        					
        					if(!found){
        						 deviceMap.put(currentDeviceDSUID, currentDevice);
        						 logger.debug("Can't find device in trashDevices, add Device to the deviceMap!");
        					 }
        				}
        				
        				deviceStatusListeners.get(DeviceStatusListener.DEVICE_DESCOVERY).onDeviceAdded(currentDevice);
        				logger.debug("inform DeviceStatusListener: \"" 
        						+ DeviceStatusListener.DEVICE_DESCOVERY 
        						+ "\" about Device with DSID: \"" 
        						+ currentDevice.getDSUID() 
        						+ "\" added.");
        			}
        		}
        		        		
        		for(Device device: tempDeviceMap.values()){
        			logger.debug("Found removed Devices.");
        			String dSUID = device.getDSUID();
        			
        			trashDevices.add(new TrashDevice(deviceMap.remove(dSUID)));
        			logger.debug("Add Device: "+ device.getDSID().getValue() + " to trashDevices");
        			
        			deviceStatusListeners.get(dSUID).onDeviceRemoved(device);
        			logger.debug("inform DeviceStatusListener: " 
    						+ dSUID
    						+ " about Device: " 
    						+ dSUID 
    						+ " removed.");
        			deviceStatusListeners.get(DeviceStatusListener.DEVICE_DESCOVERY).onDeviceRemoved(device);
        			logger.debug("inform DeviceStatusListener: " 
    						+ DeviceStatusListener.DEVICE_DESCOVERY
    						+ " about Device: " 
    						+ dSUID 
    						+ " removed.");
        		}
        		
        		if(!trashDevices.isEmpty() && (lastBinCheck + BIN_CHECK_TIME < System.currentTimeMillis())){
        			for(TrashDevice trashDevice: trashDevices){
        				if(trashDevice.isTimeToDelete(Calendar.getInstance().get(Calendar.DAY_OF_YEAR))){
        					logger.debug("Found trashDevice that have to deleate!");
        					trashDevices.remove(trashDevice);
        					logger.debug("Delete trashDevice: "+ trashDevice.getDevice().getDSID().getValue());
        				}
        			}
        			lastBinCheck = System.currentTimeMillis();
        		}
        	} 
        	
        }
	};
		
	private class TrashDevice {
		private Device device;
		private int timeStamp;
		
		public TrashDevice(Device device){
			this.device = device;
			this.timeStamp = Calendar.getInstance().get(Calendar.DAY_OF_YEAR);
		}
		
		public Device getDevice(){
			return device;
		}
		
		public boolean isTimeToDelete(int dayOfYear){
			return this.timeStamp + DigitalSTROMBindingConstants.DEFAULT_TRASH_DEVICE_DELEATE_TIME <= dayOfYear; 
		}
		
		@Override
		public boolean equals(Object object){
			return object instanceof TrashDevice ? 
					this.device.getDSID().equals(((TrashDevice) object).getDevice().getDSID()): false;
		}
	}
	
	public DssBridgeHandler(Bridge bridge) {
		super(bridge);
	}
	
	@Override
    public void initialize() {
        logger.debug("Initializing DigitalSTROM Bridge handler.");
        Configuration configuration = this.getConfig();
        if(configuration.get(HOST).toString() != null){
        	this.digitalSTROMClient = new DigitalSTROMJSONImpl(
        			configuration.get(HOST).toString(), 
        			DEFAULT_CONNECTION_TIMEOUT, 
        			DEFAULT_READ_TIMEOUT);

        	//get Configurations
    		if(configuration.get(DigitalSTROMBindingConstants.SENSOR_DATA_UPDATE_INTERVALL) != null &&
    				!configuration.get(DigitalSTROMBindingConstants.SENSOR_DATA_UPDATE_INTERVALL).toString().trim().isEmpty()){
    			
    			DigitalSTROMBindingConstants.DEFAULT_SENSORDATA_REFRESH_INTERVAL = Integer.
    					parseInt(configuration.get(DigitalSTROMBindingConstants.SENSOR_DATA_UPDATE_INTERVALL).
    							toString() + "000");
    		}
    		if(configuration.get(DigitalSTROMBindingConstants.DEFAULT_TRASH_DEVICE_DELEATE_TIME_KEY) != null &&
    				!configuration.get(DigitalSTROMBindingConstants.DEFAULT_TRASH_DEVICE_DELEATE_TIME_KEY).toString().trim().isEmpty()){
    			
    			DigitalSTROMBindingConstants.DEFAULT_TRASH_DEVICE_DELEATE_TIME = Integer.
    					parseInt(configuration.get(DigitalSTROMBindingConstants.DEFAULT_TRASH_DEVICE_DELEATE_TIME_KEY).
    							toString());
    		}
    		if(configuration.get(DigitalSTROMBindingConstants.TRUST_CERT_PATH_KEY) != null &&
    				!configuration.get(DigitalSTROMBindingConstants.TRUST_CERT_PATH_KEY).toString().trim().isEmpty()){
    			
    			DigitalSTROMBindingConstants.TRUST_CERT_PATH = configuration.
    					get(DigitalSTROMBindingConstants.TRUST_CERT_PATH_KEY).toString();
    		}
    		
    		//if right connect data are set and the connection to the server
        	/*if(checkConnection() && configuration.get(APPLICATION_TOKEN) != null && 
        			!(this.applicationToken = configuration.get(APPLICATION_TOKEN).toString()).trim().isEmpty()){
        	
        		handleStructure(digitalSTROMClient
        				.getApartmentStructure(sessionToken));
        		configuration.remove(PASSWORD);
    			configuration.remove(USER_NAME);
        		        		
        		this.digitalSTROMEventListener = new DigitalSTROMEventListener(
        				configuration.get(HOST).toString(), 
        				(DigitalSTROMJSONImpl) digitalSTROMClient, 
        				this);
        			
        		this.digitalSTROMEventListener.start();
        	*/
        		//vieleiecht besser bei updateSensorData?
        		/*this.sensorJobExecuter = new SensorJobExecutor((DigitalSTROMJSONImpl) digitalSTROMClient, this);
        		this.sensorJobExecuter.start();
        	*/
        	/*	onUpdate();
        		
        	} else{*/
        		onUpdate();
        	//}
        } else{
        	logger.warn("Cannot connect to DigitalSTROMSever. Host address is not set.");
        }
        
    }
    
    @Override
    public void dispose() {
        logger.debug("Handler disposed.");
        if(this.digitalSTROMEventListener != null && this.digitalSTROMEventListener.isAlive()){
        	this.digitalSTROMEventListener.shutdown();
        	this.digitalSTROMEventListener = null;
        }
        if(this.sensorJobExecuter != null && this.sensorJobExecuter.isAlive()){
        	this.sensorJobExecuter.shutdown();
        	this.sensorJobExecuter = null;
        }
        
        if(pollingJob!=null && !pollingJob.isCancelled()) {
        	pollingJob.cancel(true);
        	pollingJob = null;
        }
        
        if(this.getThing().getStatus().equals(ThingStatus.ONLINE)){
        	updateStatus(ThingStatus.OFFLINE);
        }
        
        
        //TODO: füllen mit allem rest
    }

    /****update methods****/
    
    @Override
	protected void updateStatus(ThingStatus status) {
		super.updateStatus(status);
        for(Thing child : getThing().getThings()) {
        	child.setStatus(status);
        }
	}
    
    /**
     * This method adds a {@link SensorJobs} with the appropriate priority in the {@link SensorJobExecuter}.
     * 
     * @param sensorJob
     * @param priority
     */
    public void updateSensorData(SensorJob sensorJob, String priority){
    	if(sensorJobExecuter == null){
			sensorJobExecuter = new SensorJobExecutor((DigitalSTROMJSONImpl) digitalSTROMClient, this);
			this.sensorJobExecuter.start();
		}
    	
		if(sensorJob != null && priority != null){
			if(priority.contains(DigitalSTROMBindingConstants.REFRESH_PRIORITY_HIGH)){
				sensorJobExecuter.addHighPriorityJob(sensorJob);
			}else if(priority.contains(DigitalSTROMBindingConstants.REFRESH_PRIORITY_MEDIUM)){
					sensorJobExecuter.addMediumPriorityJob(sensorJob);
			}else if(priority.contains(DigitalSTROMBindingConstants.REFRESH_PRIORITY_LOW)){
					sensorJobExecuter.addLowPriorityJob(sensorJob);
			}else{
				logger.error("Sensor data update priority do not exist! Please check the input!");
				return;
			}
			logger.debug("Add new sensorJob with priority: {} to sensorJobExecuter", priority);
			
		}
	}
	
	private synchronized void onUpdate() {
		if (digitalSTROMClient != null) {
			if (pollingJob == null || pollingJob.isCancelled()) {
				pollingJob = scheduler.scheduleAtFixedRate(pollingRunnable, 1, POLLING_FREQUENCY, TimeUnit.SECONDS);
				logger.debug("start pollingJob");
			}
	    }
	}
    
	/****handling methods****/
	
	@Override
	public void handleCommand(ChannelUID channelUID, Command command) {
      //nothing to do
	}
	
	/**
	 * This method sends outstanding commands to digitalSTROM-Server assuming it is reachable.
	 * 
	 * @param device
	 */
	public void sendComandsToDSS(Device device , DeviceStateUpdate deviceStateUpdate){
		/*if(device.isDeviceUpToDate()){
			logger.debug("Get send command but Device is alraedy up to date");
		}*/
		boolean requestSucsessfull;
		if(checkConnection()){
			
			//DeviceStateUpdate deviceStateUpdate = device.getNextDeviceUpdateState();
			requestSucsessfull = false;
			
			if(deviceStateUpdate != null){
				switch(deviceStateUpdate.getType()){
					case DeviceStateUpdate.UPDATE_BRIGHTNESS_DECREASE:
					case DeviceStateUpdate.UPDATE_SLAT_DECREASE:
						requestSucsessfull = digitalSTROMClient.decreaseValue(sessionToken, device.getDSID());
						if(requestSucsessfull){
							//TODO: checken ob man auch dsuid ins event packen kann, sonst zu dsid ändern ... siehe auch TODO im EventListener ... 
							//		evtl. echo ganz weg lassen und hier kein eshStateupdate schicken .. muss aber in DeviceImpl geändert weren
							digitalSTROMEventListener.addEcho(device.getDSID().getValue(),
									(short) ZoneSceneEnum.DECREMENT.getSceneNumber());
						}
						break;
					case DeviceStateUpdate.UPDATE_BRIGHTNESS_INCREASE:
					case DeviceStateUpdate.UPDATE_SLAT_INCREASE:
						requestSucsessfull = digitalSTROMClient.increaseValue(sessionToken, device.getDSID());
						if(requestSucsessfull){
							//TODO: checken ob man auch dsuid ins event packen kann, sonst zu dsid ändern ... siehe auch TODO im EventListener
							digitalSTROMEventListener.addEcho(device.getDSID().getValue(),
									(short) ZoneSceneEnum.INCREMENT.getSceneNumber());
						}
						break;
					case DeviceStateUpdate.UPDATE_BRIGHTNESS: 
						requestSucsessfull = digitalSTROMClient.setDeviceValue(sessionToken, 
								device.getDSID(), 
								null, 
								deviceStateUpdate.getValue());
						/*if(requestSucsessfull && deviceStateUpdate.getValue() <= 0){
							this.sensorJobExecuter.removeSensorJobs(device.getDSID());
						}*/
						break;
					case DeviceStateUpdate.UPDATE_ON_OFF: 
						if(deviceStateUpdate.getValue() > 0){
							requestSucsessfull = digitalSTROMClient.turnDeviceOn(sessionToken, device.getDSID(), null);
							if(requestSucsessfull){
								digitalSTROMEventListener.addEcho(device.getDSID().getValue(),
										(short) ZoneSceneEnum.MAXIMUM.getSceneNumber());
							}
						} else{
							requestSucsessfull = digitalSTROMClient.turnDeviceOff(sessionToken, device.getDSID(), null);
							if(requestSucsessfull){
								digitalSTROMEventListener.addEcho(device.getDSID().getValue(),
										(short) ZoneSceneEnum.MINIMUM.getSceneNumber());
							}
							if(sensorJobExecuter != null){
								this.sensorJobExecuter.removeSensorJobs(device.getDSID());
							}
						}
						break;
					case DeviceStateUpdate.UPDATE_SLATPOSITION: 
						requestSucsessfull = digitalSTROMClient.setDeviceValue(sessionToken, 
								device.getDSID(), 
								null, 
								deviceStateUpdate.getValue());
						break;
					default: return;
				}
				
				if(requestSucsessfull){
					logger.debug("Send {} command to DSS and updateInternalDeviceState", deviceStateUpdate.getType());
					device.updateInternalDeviceState(deviceStateUpdate);
				} else{
					logger.debug("Can't send {} command to DSS!", deviceStateUpdate.getType());
				}
			}
		}
	}

    // Here we build up a new hashmap in order to replace it with the old one.
	// This hashmap is used to find the affected items after an event from
	// digitalSTROM.
	public void handleStructure(Apartment apartment) {
		if (apartment != null) {

			Map<Integer, Map<Short, List<String>>> newZoneGroupMap = Collections
					.synchronizedMap(new HashMap<Integer, Map<Short, List<String>>>());
			Map<String, Device> clonedDsidMap = getDsuidToDeviceMap();

			for (Zone zone : apartment.getZoneMap().values()) {

				Map<Short, List<String>> groupMap = new HashMap<Short, List<String>>();

				for (DetailedGroupInfo g : zone.getGroups()) {

					List<String> devicesInGroup = new LinkedList<String>();
					for (String dsid : g.getDeviceList()) {
						if (clonedDsidMap.containsKey(dsid)) {
							devicesInGroup.add(dsid);
						}
					}
					groupMap.put(g.getGroupID(), devicesInGroup);
				}
				newZoneGroupMap.put(zone.getZoneId(), groupMap);
			}

			synchronized (digitalSTROMZoneGroupMap) {
				digitalSTROMZoneGroupMap = newZoneGroupMap;
			}
		}
	}
	
	/****methods to store DeviceStatusListener****/
	
	public void registerDeviceStatusListener(String id, DeviceStatusListener deviceStatusListener) {
		if (deviceStatusListener == null) {
			throw new NullPointerException("It's not allowed to pass a null DeviceStatusListener.");
		}
		
		if (id != null) {
			logger.debug("Added DeviceStatusListener {}",id);
				deviceStatusListeners.put(id, deviceStatusListener);
				//save in device that it is added to ESH
				//Device device = this.deviceMap.get(id); 
				
				onUpdate();
		    	/*if(!id.contains(DeviceStatusListener.DEVICE_DESCOVERY) && device != null){
		    		logger.debug("inform listener about the added Device");
		    		device.setIsAddToESH(true);
		    		if(device.isPresent()){
		    			deviceStatusListener.onDeviceAdded(device);
		    		}else{
		    			deviceStatusListener.onDeviceRemoved(device);
		    		}
				} */
			}else {
					throw new NullPointerException("It's not allowed to pass a null ID.");
		}
	}
		 
	public void unregisterDeviceStatusListener(String id) {
		if(id != null){
			//save in device that it is not added to ESH
			this.deviceMap.get(id).setIsAddToESH(false);
			
			if(deviceStatusListeners.remove(id) != null){
				logger.debug("Delete deviceStatuslistener from device with DSUID {}.", id);
			}
			Device device = deviceMap.remove(id);
			if(trashDevices.add(new TrashDevice(device))){
				logger.debug("Add Device with DSUID {} to trashMap.", id);
			}
			onUpdate(); 
			if(sensorJobExecuter != null){
				sensorJobExecuter.removeSensorJobs(device.getDSID());
			}
			//logger.debug("Remove SensorJobs from device with DSID {}.", id);
		} else{
			throw new NullPointerException("It's not allowed to pass a null ID.");
		}
	}
		 
	/****Get methods****/
		 
	public Collection<Device> getDevices(){
		return deviceMap.values();
	}
	
	public Map<String, Device> getDsuidToDeviceMap() {
		return new HashMap<String, Device>(deviceMap);
	}
	
	public Device getDeviceByDSID(String dsID){
		return deviceMap.get(dSUIDtoDSID.get(dsID));
	}
	
	public Device getDeviceByDSUID(String dSUID){
		return deviceMap.get(dSUID);
	}
	
	public Map<Integer, Map<Short, List<String>>> getDigitalSTROMZoneGroupMap() {
		return new HashMap<Integer, Map<Short, List<String>>>(
				digitalSTROMZoneGroupMap);
	}
	
	/*private HashMap<String, Device> getDigitalSTROMDeviceHashMap(){
    	HashMap<String, Device> tempDeviceMap = new HashMap<String, Device>();
    	
    	if(checkConnection()){
    		for(Device device: digitalSTROMClient.getApartmentDevices(sessionToken, false)){
    			tempDeviceMap.put(device.getDSID().getValue(), device);
    		}
    	}
    	return tempDeviceMap;
    }*/
	
	public String getSessionToken(){
		return sessionToken;
	}

	// Here we read the configured and stored (in the chip) output value for a
	// specific scene
	// and we store this value in order to know next time what to do.
	// The first time a scene command is called, it takes some time for the
	// sensor reading,
	// but the next time we react very fast because we learned what to do on
	// this command.
	public void getSceneSpec(Device device, short sceneId) {
		if(checkConnection()){
			// setSensorReading(true); // no metering in this time
			DeviceSceneSpec spec = digitalSTROMClient.getDeviceSceneMode(
					sessionToken, device.getDSID(), null, sceneId);
			// setSensorReading(false);

			if (spec != null) {
				device.addSceneConfig(sceneId, spec);
				logger.info("UPDATED ignoreList for dsid: " + device.getDSID()
						+ " sceneID: " + sceneId);
				//inform DeviceStatusListener about added scene configuration
				
			}
		}
	}

	/****Connection methods****/
	
	/**
	 * This method must be called to the digitalSTROM-Server before each command.
	 * It examines the connection to the server and sets a new session token if it is expired.
	 * 
	 * @return true if the connection is established and false if not 
	 */
	public boolean checkConnection(){
		switch(this.digitalSTROMClient.checkConnection(sessionToken)) {
			case HttpURLConnection.HTTP_OK:
				if(!lastConnectionState){ 
					logger.debug("Connection to DigitalSTROM-Server established.");
					lastConnectionState = true;
					onConnectionResumed();
				}
				break;
			case HttpURLConnection.HTTP_UNAUTHORIZED:
				logger.info("DigitalSTROM server  {} send HTTPStatus {}", this.getConfig().get(HOST), HttpURLConnection.HTTP_UNAUTHORIZED);
				lastConnectionState = false;
				break;
			case HttpURLConnection.HTTP_FORBIDDEN:
				sessionToken = this.digitalSTROMClient.loginApplication(applicationToken);
				if(this.digitalSTROMClient.checkConnection(sessionToken) == HttpURLConnection.HTTP_OK){
					if(!lastConnectionState){
						logger.debug("Connection to DigitalSTROM-Server established.");
						onConnectionResumed();
						lastConnectionState = true;
					}
				} else{
					onNotAuthentificated();
					
					lastConnectionState = false;
				}
				break;
			case -1:
			case HttpURLConnection.HTTP_NOT_FOUND:
				logger.error("Server not found! Please check this points:\n"
						+ " - DigitalSTROM-Server turned on?\n"
						+ " - hostadress correct?\n"
						+ " - ethernet cable connection established?");
				onConnectionLost();
				
				lastConnectionState = false;
				break;
			case -2:
				logger.error("Invalide URL!");
				lastConnectionState = false;
				break;
		}
		return lastConnectionState;
	}
	
	 /**
     * This method is called whenever the connection to the digitalSTROM-Server is available,
     * but requests are not allowed due to a missing or invalid authentication.
     */
	private void onNotAuthentificated(){
				
		String applicationToken;
		//String sessionToken;
		Configuration configuration = getConfig();
		
		boolean isAutentificated = false;
		
		logger.info("DigitalSTROM server {} is not authentificated - please set a applicationToken or username and password.", configuration.get(HOST));

		if(configuration.get(APPLICATION_TOKEN) != null && 
				!(applicationToken = configuration.get(APPLICATION_TOKEN).toString()).isEmpty()){
			sessionToken = digitalSTROMClient.loginApplication(applicationToken);
			if(digitalSTROMClient.checkConnection(sessionToken) == HttpURLConnection.HTTP_OK) {
				logger.info("User defined Applicationtoken can be used.");
				isAutentificated = true;
			} else{
				logger.info("User defined Applicationtoken can't be used.");
			}
		} else{
			logger.info("Can't find Appicationtoken.");
		}
		//final ändern in Konsoleneingabe oder Konsoleneingabe hinzufügen
		if(checkUserPassword(configuration)){
			if(!isAutentificated){
				logger.info("Generating Applicationtoken with user and password.");
				
				//generate applicationToken and test host is reachable
				applicationToken = this.digitalSTROMClient.requestAppplicationToken(DigitalSTROMBindingConstants.APPLICATION_NAME);
							
				if(applicationToken != null && applicationToken != ""){
					//enable applicationToken
					sessionToken = this.digitalSTROMClient.login(
							configuration.get(USER_NAME).toString(), 
							configuration.get(PASSWORD).toString());
					//logger.debug("SessionToken: {}, applicationToken: {}", sessionToken, applicationToken);
					if(this.digitalSTROMClient.enableApplicationToken(applicationToken, sessionToken)){
						configuration.put(APPLICATION_TOKEN, applicationToken);
						this.applicationToken = applicationToken;
						isAutentificated = true;
						
						logger.debug("Applicationtoken generated and added successfull to DigitalSTROM Server.");
					} else {
						logger.debug("Incorrect Username or password. Can't enable Applicationtoken.");
					}
				}
			}
			
			//remove password and username, to don't store them persistently   
			if(isAutentificated){
				configuration.remove(PASSWORD);
				configuration.remove(USER_NAME);
			}
		} else 
			if(!isAutentificated){
				logger.info("Can't find Username or password to genarate Appicationtoken.");
			}
	}
	
	
	private boolean checkUserPassword(Configuration configuration){
		if((configuration.get(USER_NAME) != null && configuration.get(PASSWORD) != null) &&
			(!configuration.get(USER_NAME).toString().isEmpty() && !configuration.get(PASSWORD).toString().isEmpty()))//notwendig? 
			return true;
		return false;
	}
	
    /**
     * This method is called whenever the connection to the DigitalSTROM-Server is lost.
     */
	public void onConnectionLost() {
        logger.debug("DigitalSTROM-Server connection lost. Updating thing status to OFFLINE.");
        //stop listener and 
        if(this.digitalSTROMEventListener != null && this.digitalSTROMEventListener.isAlive()){
        	this.digitalSTROMEventListener.shutdown();
        	this.sensorJobExecuter.shutdown();
        }
        if(this.sensorJobExecuter != null && this.sensorJobExecuter.isAlive()){
        	this.sensorJobExecuter.shutdown();
        }
        updateStatus(ThingStatus.OFFLINE);
    }

  
    /**
     * This method is called whenever the connection to the DigitalSTROM-Server is resumed.
     */
	public void onConnectionResumed() {
        logger.debug("DigitalSTROM-Server connection resumed. Updating thing status to ONLINE.");
        updateStatus(ThingStatus.ONLINE);
        if(this.digitalSTROMEventListener != null){
        	this.digitalSTROMEventListener.wakeUp();;
        }else{
        	this.digitalSTROMEventListener = new DigitalSTROMEventListener(
    				this.getThing().getConfiguration().get(HOST).toString(), 
    				(DigitalSTROMJSONImpl) digitalSTROMClient, 
    				this);
    			
    		this.digitalSTROMEventListener.start();
        }
        if(this.sensorJobExecuter != null){
        	this.sensorJobExecuter.wackeUp();
        }
        // now also re-initialize all light handlers
        for(Thing thing : getThing().getThings()) {
        	ThingHandler handler = thing.getHandler();
        	if(handler!=null) {
        		handler.initialize();
        	}
        }
    }
}
