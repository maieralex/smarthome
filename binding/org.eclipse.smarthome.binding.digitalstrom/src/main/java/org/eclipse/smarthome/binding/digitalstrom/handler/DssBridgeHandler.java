/**
 * Copyright (c) 2014 openHAB UG (haftungsbeschraenkt) and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.smarthome.binding.digitalstrom.handler;

import static org.eclipse.smarthome.binding.digitalstrom.DigitalSTROMBindingConstants.APPLICATION_TOKEN;
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
import org.eclipse.smarthome.binding.digitalstrom.internal.client.constants.ZoneSceneEnum;
import org.eclipse.smarthome.binding.digitalstrom.internal.client.entity.Apartment;
import org.eclipse.smarthome.binding.digitalstrom.internal.client.entity.DSID;
import org.eclipse.smarthome.binding.digitalstrom.internal.client.entity.DetailedGroupInfo;
import org.eclipse.smarthome.binding.digitalstrom.internal.client.entity.Device;
import org.eclipse.smarthome.binding.digitalstrom.internal.client.entity.DeviceSceneSpec;
import org.eclipse.smarthome.binding.digitalstrom.internal.client.entity.DeviceStateUpdate;
import org.eclipse.smarthome.binding.digitalstrom.internal.client.entity.Zone;
import org.eclipse.smarthome.binding.digitalstrom.internal.client.impl.DigitalSTROMJSONImpl;
import org.eclipse.smarthome.binding.digitalstrom.internal.client.job.SensorJob;
import org.eclipse.smarthome.config.core.Configuration;
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
 */
public class DssBridgeHandler extends BaseBridgeHandler {


	private Logger logger = LoggerFactory.getLogger(DssBridgeHandler.class);
    
    public final static Set<ThingTypeUID> SUPPORTED_THING_TYPES = Collections.singleton(THING_TYPE_DSS_BRIDGE);

    /****configuration****/
    private DigitalSTROMAPI digitalSTROMClient = null;
    private String applicationToken = null;
    private String sessionToken = null;
    
    private static final int POLLING_FREQUENCY = 10; // in seconds
	private final int BIN_CHECK_TIME = 360000; //in milliseconds
    
    /****States****/
    private boolean lastConnectionState = false;
    private long lastBinCheck = 0;
    
    /****Maps****/
    private HashMap<String, DeviceStatusListener> deviceStatusListeners = new HashMap<String, DeviceStatusListener>();
   
    private List<TrashDevice> trashDevices = new LinkedList<TrashDevice>(); 
    
    private HashMap<String, Device> deviceMap = new HashMap<String, Device>();
    
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
        		
        		while (!currentDeviceList.isEmpty()){
        			Device currentDevice = currentDeviceList.remove(0);
        			String currentDeviceDSID = currentDevice.getDSID().getValue();
        			Device eshDevice = tempDeviceMap.remove(currentDeviceDSID);
        			
        			if(eshDevice != null){
        				
        				if(!eshDevice.isESHThingUpToDate()){
        					deviceStatusListeners.get(currentDeviceDSID).onDeviceStateChanged(eshDevice);
        					logger.debug("inform deviceStatusListener from  Device \""
        							+ currentDeviceDSID
        							+ "\" about update ESH-Update");
        				}
        				
        				if(!eshDevice.isSensorDataUpToDate()){
        					deviceStatusListeners.get(currentDeviceDSID).onDeviceNeededSensorDataUpdate(eshDevice);
        					logger.debug("inform deviceStatusListener from  Device \""
        							+ currentDeviceDSID
        							+ "\" about Sensordata need update");
        				}
        				
        			} else{
        				logger.debug("Found new Device!");
        				
        				if(trashDevices.isEmpty()){
        					deviceMap.put(currentDeviceDSID, currentDevice);
        					logger.debug("trashDevices are empty, add Device to the deviceMap!");
        				} else{
        					logger.debug("Search device in trashDevices.");
        					
        					int index = trashDevices.indexOf(currentDevice);
        					 if(index != -1){
        						 Device device =  trashDevices.get(index).getDevice();
        						 deviceMap.put(device.getDSID().getValue(), device);
        						 logger.debug("Found device in trashDevices, add TrashDevice to the deviceMap!");
        					 } else{
        						 deviceMap.put(currentDeviceDSID, currentDevice);
        						 logger.debug("Can't find device in trashDevices, add Device to the deviceMap!");
        					 }
        				}
        				
        				deviceStatusListeners.get(DeviceStatusListener.DEVICE_DESCOVERY).onDeviceAdded(currentDevice);
        				//Testen ob das nötig ist, evtl muss erst das Thing über den DeviceDiscoveryService erstellt werden
        				logger.debug("inform DeviceStatusListener: " 
        						+ DeviceStatusListener.DEVICE_DESCOVERY 
        						+ " about Device: " 
        						+ currentDevice 
        						+ " added.");
        				try {
							wait(100);
						} catch (InterruptedException e) {
							e.printStackTrace();
						}
        				deviceStatusListeners.get(currentDeviceDSID).onDeviceAdded(currentDevice);
        				logger.debug("inform DeviceStatusListener: " 
        						+ currentDevice 
        						+ " about Device: " 
        						+ currentDevice 
        						+ " added.");
        			}
        		}
        		        		
        		for(Device device: tempDeviceMap.values()){
        			logger.debug("Found removed Devices.");
        			String dsID = device.getDSID().getValue();
        			
        			trashDevices.add(new TrashDevice(deviceMap.remove(dsID)));
        			logger.debug("Add Device: "+ device.getDSID().getValue() + " to trashDevices");
        			
        			deviceStatusListeners.get(dsID).onDeviceRemoved(device);
        			logger.debug("inform DeviceStatusListener: " 
    						+ dsID
    						+ " about Device: " 
    						+ dsID 
    						+ " removed.");
        			deviceStatusListeners.get(DeviceStatusListener.DEVICE_DESCOVERY).onDeviceRemoved(device);
        			logger.debug("inform DeviceStatusListener: " 
    						+ DeviceStatusListener.DEVICE_DESCOVERY
    						+ " about Device: " 
    						+ dsID 
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
	}
	
	public DssBridgeHandler(Bridge bridge) {
		super(bridge);
	}
	
	@Override
    public void initialize() {
        logger.debug("Initializing DigitalSTROM Bridge handler.");
        
        if(this.getConfig().get(HOST).toString() != null){
        	this.digitalSTROMClient = new DigitalSTROMJSONImpl(
        			this.getConfig().get(HOST).toString(), 
        			DEFAULT_CONNECTION_TIMEOUT, 
        			DEFAULT_READ_TIMEOUT);
        
        	this.applicationToken = this.getConfig().get(APPLICATION_TOKEN).toString();
		
        	deviceMap.putAll(getDigitalSTROMDeviceHashMap());
        	
        	if(checkConnection()){
        		handleStructure(digitalSTROMClient
        			.getApartmentStructure(sessionToken));
        	}
		
        	this.digitalSTROMEventListener = new DigitalSTROMEventListener(
        			this.getConfig().get(HOST).toString(), 
        			(DigitalSTROMJSONImpl) digitalSTROMClient, 
        			this);
        	this.digitalSTROMEventListener.start();
        	
        	//vieleiecht besser bei updateSensorData?
        	this.sensorJobExecuter.run();
        	
        } else{
            logger.warn("Cannot connect to DigitalSTROMSever. Host address is not set.");
        }
        
    }
    
    @Override
    public void dispose() {
        logger.debug("Handler disposed.");
        this.digitalSTROMEventListener.shutdown();
        this.sensorJobExecuter.shutdown();
        
        if(pollingJob!=null && !pollingJob.isCancelled()) {
        	pollingJob.cancel(true);
        	pollingJob = null;
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
		if(sensorJob != null && priority != null){
			switch(priority){
				case DigitalSTROMBindingConstants.REFRESH_PRIORITY_HIGH:
					sensorJobExecuter.addHighPriorityJob(sensorJob);
					break;
				case DigitalSTROMBindingConstants.REFRESH_PRIORITY_MEDIUM:
					sensorJobExecuter.addMediumPriorityJob(sensorJob);
					break;
				case DigitalSTROMBindingConstants.REFRESH_PRIORITY_LOW:
					sensorJobExecuter.addLowPriorityJob(sensorJob);
					break;
			}
		}
	}
	
	private synchronized void onUpdate() {
		if (digitalSTROMClient != null) {
			if (pollingJob == null || pollingJob.isCancelled()) {
				pollingJob = scheduler.scheduleAtFixedRate(pollingRunnable, 1, POLLING_FREQUENCY, TimeUnit.SECONDS);
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
	public void sendComandsToDSS(Device device){
		while(!device.isDeviceUpToDate() && checkConnection()){
			
			DeviceStateUpdate deviceStateUpdate = device.getNextDeviceUpdateState();
			boolean requestSucsessfull = false;
			
			if(deviceStateUpdate != null){
				switch(deviceStateUpdate.getType()){
					case DeviceStateUpdate.UPDATE_BRIGHTNESS: 
						requestSucsessfull = digitalSTROMClient.setDeviceValue(sessionToken, 
								device.getDSID(), 
								null, 
								deviceStateUpdate.getValue());
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
					device.updateInternalDeviceState(deviceStateUpdate);
				} else{
					//TODO: Fehlerausgabe
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
			Map<String, Device> clonedDsidMap = getDsidToDeviceMap();

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
			deviceStatusListeners.put(id, deviceStatusListener);
			onUpdate();
		    // inform the listener initially about the device and their states
		    if(id != DeviceStatusListener.DEVICE_DESCOVERY){
		    	deviceStatusListener.onDeviceAdded(deviceMap.get(id));
		    }
		} else {
			throw new NullPointerException("It's not allowed to pass a null ID.");
		}
	}
		 
	public void unregisterDeviceStatusListener(String id) {
		if(id != null){
			deviceStatusListeners.remove(id);
			onUpdate();     
			sensorJobExecuter.removeSensorJobs(new DSID(id));
		} else{
			throw new NullPointerException("It's not allowed to pass a null ID.");
		}
	}
		 
	/****Get methods****/
		 
	public Collection<Device> getDevices(){
		return deviceMap.values();
	}
	
	public Map<String, Device> getDsidToDeviceMap() {
		return new HashMap<String, Device>(deviceMap);
	}
	
	public Device getDeviceByDSID(String dsID){
		return deviceMap.get(dsID);
	}
	
	public Map<Integer, Map<Short, List<String>>> getDigitalSTROMZoneGroupMap() {
		return new HashMap<Integer, Map<Short, List<String>>>(
				digitalSTROMZoneGroupMap);
	}
	
	private HashMap<String, Device> getDigitalSTROMDeviceHashMap(){
    	HashMap<String, Device> tempDeviceMap = new HashMap<String, Device>();
    	
    	if(checkConnection()){
    		for(Device device: digitalSTROMClient.getApartmentDevices(sessionToken, false)){
    			tempDeviceMap.put(device.getDSID().getValue(), device);
    		}
    	}
    	return tempDeviceMap;
    }
	
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
		//TODO: informieren obs noch weitere Fälle gibt
		switch(this.digitalSTROMClient.checkConnection(sessionToken)) {
			case HttpURLConnection.HTTP_OK:
				break;
			case HttpURLConnection.HTTP_FORBIDDEN:
			case -1:
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
		String sessionToken;
		Configuration configuration = getConfig();
		
		boolean isAutentificated = false;
		
		logger.info("DigitalSTROM server {} is not authenticated - please set a applicationToken or username and password.", configuration.get(HOST));

		if(configuration.get(APPLICATION_TOKEN) != null && 
				( applicationToken = configuration.get(APPLICATION_TOKEN).toString()) != ""){
			sessionToken = digitalSTROMClient.loginApplication(applicationToken);
			if(digitalSTROMClient.checkConnection(sessionToken) == HttpURLConnection.HTTP_OK) {
				logger.info("User defined Applicationtoken can be used.");
				isAutentificated = true;
			} else{
				logger.info("User defined Applicationtoken can't be used.");
			}
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
					
					if(this.digitalSTROMClient.enableApplicationToken(applicationToken, sessionToken)){
						configuration.put(APPLICATION_TOKEN, applicationToken);
						this.applicationToken = applicationToken;
						isAutentificated = true;
						
						logger.info("Applicationtoken generated and added successfull to DigitalSTROM Server.");
					} else {
						logger.info("Incorrect Username or password. Can't enable Applicationtoken.");
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
			(configuration.get(USER_NAME).toString() != "" && configuration.get(PASSWORD).toString() != ""))//notwendig? 
			return true;
		return false;
	}
	
    /**
     * This method is called whenever the connection to the DigitalSTROM-Server is lost.
     */
    public void onConnectionLost() {
        logger.debug("DigitalSTROM-Server connection lost. Updating thing status to OFFLINE.");
        updateStatus(ThingStatus.OFFLINE);
    }

  
    /**
     * This method is called whenever the connection to the DigitalSTROM-Server is resumed.
     */
    public void onConnectionResumed() {
        logger.debug("DigitalSTROM-Server connection resumed. Updating thing status to ONLINE.");
        updateStatus(ThingStatus.ONLINE);
        // now also re-initialize all light handlers
        for(Thing thing : getThing().getThings()) {
        	ThingHandler handler = thing.getHandler();
        	if(handler!=null) {
        		handler.initialize();
        	}
        }
    }
}
