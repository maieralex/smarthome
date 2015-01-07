package org.eclipse.smarthome.binding.digitalstrom.internal.client;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.eclipse.smarthome.binding.digitalstrom.DigitalSTROMBindingConstants;
import org.eclipse.smarthome.binding.digitalstrom.handler.DssBridgeHandler;
import org.eclipse.smarthome.binding.digitalstrom.internal.client.connection.JSONResponseHandler;
import org.eclipse.smarthome.binding.digitalstrom.internal.client.connection.transport.HttpTransport;
import org.eclipse.smarthome.binding.digitalstrom.internal.client.constants.DeviceConstants;
import org.eclipse.smarthome.binding.digitalstrom.internal.client.constants.JSONApiResponseKeysEnum;
import org.eclipse.smarthome.binding.digitalstrom.internal.client.constants.JSONRequestConstants;
import org.eclipse.smarthome.binding.digitalstrom.internal.client.constants.OutputModeEnum;
import org.eclipse.smarthome.binding.digitalstrom.internal.client.constants.SceneToStateMapper;
import org.eclipse.smarthome.binding.digitalstrom.internal.client.entity.Device;
import org.eclipse.smarthome.binding.digitalstrom.internal.client.entity.Event;
import org.eclipse.smarthome.binding.digitalstrom.internal.client.entity.EventItem;
import org.eclipse.smarthome.binding.digitalstrom.internal.client.entity.impl.JSONEventImpl;
import org.eclipse.smarthome.binding.digitalstrom.internal.client.events.EventPropertyEnum;
import org.eclipse.smarthome.binding.digitalstrom.internal.client.impl.DigitalSTROMJSONImpl;
import org.eclipse.smarthome.binding.digitalstrom.internal.client.job.DeviceOutputValueSensorJob;
import org.eclipse.smarthome.binding.digitalstrom.internal.client.job.SceneOutputValueSensorJob;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * If someone turns a device or a zone etc. on, we will get a notification
 * to update the state of the item
 * 
 * @author Alexander Betker
 * @since 1.3.0
 * 
 */
public class DigitalSTROMEventListener extends Thread {

	private Logger logger = LoggerFactory.getLogger(DigitalSTROMEventListener.class);

	private List<String> echoBox = Collections
			.synchronizedList(new LinkedList<String>());
	
	private boolean shutdown = false;
	private final String EVENT_NAME = "openhabEvent"; //namen ändern
	private final int ID = 11;
	private final DssBridgeHandler dssBridgeHandler;
	private SensorJobExecutor sensorJobExecutor;
	
	private int timeout = 1000;

	private final String INVALID_SESSION = "Invalid session!";// Invalid
																// session!

	/** Mapping digitalSTROM-Scene to digitalSTROM-State */
	private SceneToStateMapper stateMapper = new SceneToStateMapper();
	
	private HttpTransport transport = null;
	private JSONResponseHandler handler = null;
	private DigitalSTROMJSONImpl digitalSTROM;
	private String applicationToken;

	public synchronized void shutdown() {
		this.shutdown = true;
		this.sensorJobExecutor.shutdown();
		unsubscribe();
	}

	public DigitalSTROMEventListener(String uri, DigitalSTROMJSONImpl digitalSTROM, DssBridgeHandler dssBridgeHandler) {
		this.handler = new JSONResponseHandler();
		this.transport = new HttpTransport(uri, 
				DigitalSTROMBindingConstants.DEFAULT_CONNECTION_TIMEOUT, 
				DigitalSTROMBindingConstants.DEFAULT_READ_TIMEOUT);
		this.digitalSTROM = digitalSTROM;
		this.dssBridgeHandler = dssBridgeHandler;
		this.applicationToken = dssBridgeHandler.getApplicationToken();
	
		this.subscribe();
	}

	private void subscribe() {
		if (applicationToken != null) {

			boolean transmitted = digitalSTROM.subscribeEvent(
					applicationToken, 
					EVENT_NAME, 
					this.ID, 
					DigitalSTROMBindingConstants.DEFAULT_CONNECTION_TIMEOUT,
					DigitalSTROMBindingConstants.DEFAULT_READ_TIMEOUT);

			if (!transmitted) {
				this.shutdown = true;
				logger.error("Couldn't subscribe eventListener ... maybe timeout because system is to busy ...");
			}
		} else {
			logger.error("Couldn't subscribe eventListener because there is no token (no connection)");
		}
	}

	@Override
	public void run() {
		this.sensorJobExecutor = new SensorJobExecutor(digitalSTROM, applicationToken);
		sensorJobExecutor.run();
		
		while (!this.shutdown) {

			String request = this.getEventAsRequest(this.ID, 500);

			if (request != null) {

				String response = this.transport.execute(request,
						2 * this.timeout, this.timeout);

				JSONObject responseObj = this.handler
						.toJSONObject(response);

				if (this.handler.checkResponse(responseObj)) {
					JSONObject obj = this.handler
							.getResultJSONObject(responseObj);

					if (obj != null
							&& obj.get(JSONApiResponseKeysEnum.EVENT_GET_EVENT
									.getKey()) instanceof JSONArray) {
						JSONArray array = (JSONArray) obj
								.get(JSONApiResponseKeysEnum.EVENT_GET_EVENT
										.getKey());
						try {
							handleEvent(array);
						} catch (Exception e) {
							logger.warn("EXCEPTION in eventListener thread : "
									+ e.getLocalizedMessage());
						}
					}
				} else {
					String errorStr = null;
					if (responseObj != null
							&& responseObj
									.get(JSONApiResponseKeysEnum.EVENT_GET_EVENT_ERROR
											.getKey()) != null) {
						errorStr = responseObj
								.get(JSONApiResponseKeysEnum.EVENT_GET_EVENT_ERROR
										.getKey()).toString();
					}

					if (errorStr != null
							&& errorStr.equals(this.INVALID_SESSION)) {
						this.subscribe();
					} else if (errorStr != null) {
						logger.error("Unknown error message in event response: "
								+ errorStr);
					}
				}
			}
		}
	}

	private String getEventAsRequest(int subscriptionID, int timeout) {
		if (applicationToken != null) {
			return JSONRequestConstants.JSON_EVENT_GET
					+ JSONRequestConstants.PARAMETER_TOKEN
					+ applicationToken
					+ JSONRequestConstants.INFIX_PARAMETER_SUBSCRIPTION_ID
					+ subscriptionID
					+ JSONRequestConstants.INFIX_PARAMETER_TIMEOUT
					+ timeout;
		}
		return null;
	}

	private boolean unsubscribeEvent(String name, int subscriptionID) {
		if (applicationToken != null) {
			return digitalSTROM.unsubscribeEvent(applicationToken,
					EVENT_NAME, 
					this.ID, 
					DigitalSTROMBindingConstants.DEFAULT_CONNECTION_TIMEOUT, 
					DigitalSTROMBindingConstants.DEFAULT_READ_TIMEOUT);
		}
		return false;
	}

	private boolean unsubscribe() {
		return this.unsubscribeEvent(this.EVENT_NAME, this.ID);
	}

	private void handleEvent(JSONArray array) {
		if (array.size() > 0) {
			Event event = new JSONEventImpl(array);

			for (EventItem item : event.getEventItems()) {
				if (item.getName() != null
						&& item.getName().equals(this.EVENT_NAME)) {
					handleOpenhabEvent(item);
				}
			}
		}
	}
	
	/**
	 * only works on openhabEvent! please copy "openhab/openhab.js" to your dSS
	 * server (/usr/share/dss/add-ons/) and "openhab.xml" to
	 * /usr/share/dss/data/subscriptions.d/ than you need to restart your dSS
	 * 
	 * If you don't, you will not get detailed infos about, what exactly
	 * happened (for example: which device was turned on by a browser or handy
	 * app )
	 * 
	 * @param eventItem
	 */
	private void handleOpenhabEvent(EventItem eventItem) {
		if (eventItem != null) {
			int zoneId = -1;
			short groupId = -1;
			short sceneId = -1;

			boolean isDeviceCall = false;
			String dsidStr = null;

			String zoneIDStr = eventItem.getProperties().get(
					EventPropertyEnum.ZONEID);
			if (zoneIDStr != null) {
				try {
					zoneId = Integer.parseInt(zoneIDStr);
				} catch (java.lang.NumberFormatException e) {
					logger.error("NumberFormatException by handling event at parsing zoneId");
				}
			}

			String sceneStr = eventItem.getProperties().get(
					EventPropertyEnum.SCENEID);
			if (sceneStr != null) {
				try {
					sceneId = Short.parseShort(sceneStr);
				} catch (java.lang.NumberFormatException e) {
					logger.error("NumberFormatException by handling event at parsing sceneId: "
							+ sceneStr);
				}
			}

			String groupStr = eventItem.getProperties().get(
					EventPropertyEnum.GROUPID);
			if (groupStr != null) {
				try {
					groupId = Short.parseShort(groupStr);
				} catch (java.lang.NumberFormatException e) {
					logger.error("NumberFormatException by handling event at parsing groupId");
				}
			}

			dsidStr = eventItem.getProperties().get(EventPropertyEnum.DSID);

			String deviceCallStr = eventItem.getProperties().get(
					EventPropertyEnum.IS_DEVICE_CALL);
			if (deviceCallStr != null) {
				isDeviceCall = deviceCallStr.equals("true");
			}

			if (sceneId != -1) {

				if (!isEcho(dsidStr, sceneId)) {

					if (isDeviceCall) {

						if (dsidStr != null) {
							Device device = dssBridgeHandler.getDsidToDeviceMap().get(dsidStr);

							if (device != null) {
								if (!device.containsSceneConfig(sceneId)) {
									dssBridgeHandler.getSceneSpec(device, sceneId);
								}

								if (isDimmScene(sceneId)) {
									if (!device.doIgnoreScene(sceneId)) {
										handleDimmScene(device, sceneId,
												(short) -1, true);
									}
								} else if (stateMapper.isMappable(sceneId)) {
									boolean shouldBeOn = stateMapper
											.getMapping(sceneId);

									if (!device.doIgnoreScene(sceneId)) {
										if (shouldBeOn) {
											device.setOutputValue(device
													.getMaxOutPutValue());
										} else {
											device.setOutputValue(0);
										}
									}
								} else {
									if (!device.doIgnoreScene(sceneId)) {
										short value = device
												.getSceneOutputValue(sceneId);
										if (value != -1) {
											device.setOutputValue(value);
										} else {
											initDeviceOutputValue(
													device,
													DeviceConstants.DEVICE_SENSOR_OUTPUT);
											initSceneOutputValue(device,
													sceneId);
										}
									}
								}
							}
						}
					} else {

						if (isApartmentScene(sceneId)) {
							handleApartmentScene(sceneId, groupId);
						} else {

							if (zoneId == 0) {
								if (isDimmScene(sceneId)) {

									Map<String, Device> deviceMap = dssBridgeHandler.getDsidToDeviceMap();

									if (groupId == 0) {

										Set<String> dsidSet = deviceMap
												.keySet();

										if (dsidSet != null) {
											for (String dsid : dsidSet) {
												Device device = deviceMap
														.get(dsid);

												if (device != null) {

													if (!device
															.containsSceneConfig(sceneId)) {
														dssBridgeHandler.getSceneSpec(device,
																sceneId);
													}

													if (!device
															.doIgnoreScene(sceneId)) {
														handleDimmScene(
																deviceMap
																		.get(dsid),
																sceneId,
																groupId, false);
													}

												}
											}
										}
									} else if (groupId != -1) {

										Map<Short, List<String>> map = dssBridgeHandler.getDigitalSTROMZoneGroupMap()
												.get(zoneId);

										if (map != null) {
											List<String> dsidList = map
													.get(groupId);
											if (dsidList != null) {
												for (String dsid : dsidList) {
													Device device = deviceMap
															.get(dsid);

													if (device != null) {

														if (!device
																.containsSceneConfig(sceneId)) {
															dssBridgeHandler.getSceneSpec(
																	device,
																	sceneId);
														}

														if (!device
																.doIgnoreScene(sceneId)) {
															handleDimmScene(
																	deviceMap
																			.get(dsid),
																	sceneId,
																	groupId,
																	false);
														}
													}
												}
											}
										}
									}
								} else if (stateMapper.isMappable(sceneId)) {

									boolean shouldBeOn = stateMapper
											.getMapping(sceneId);

									if (groupId == 0) {

										Map<String, Device> deviceMap = dssBridgeHandler.getDsidToDeviceMap();
										Set<String> dsidSet = deviceMap
												.keySet();
										if (dsidSet != null) {
											for (String dsid : dsidSet) {
												Device device = deviceMap
														.get(dsid);
												if (device != null) {

													if (!device
															.containsSceneConfig(sceneId)) {
														dssBridgeHandler.getSceneSpec(device,
																sceneId);
													}

													if (!device
															.doIgnoreScene(sceneId)) {

														if (shouldBeOn) {
															device.setOutputValue(device
																	.getMaxOutPutValue());
														} else {
															device.setOutputValue(0);
														}

													}
												}
											}
										}
									} else if (groupId != -1) {

										Map<Short, List<String>> map = dssBridgeHandler.getDigitalSTROMZoneGroupMap()
												.get(zoneId);
										Map<String, Device> deviceMap = dssBridgeHandler.getDsidToDeviceMap();
										if (map != null) {
											List<String> dsidList = map
													.get(groupId);
											if (dsidList != null) {
												for (String dsid : dsidList) {
													Device device = deviceMap
															.get(dsid);
													if (device != null) {

														if (!device
																.containsSceneConfig(sceneId)) {
															dssBridgeHandler.getSceneSpec(
																	device,
																	sceneId);
														}

														if (!device
																.doIgnoreScene(sceneId)) {

															if (shouldBeOn) {
																device.setOutputValue(device
																		.getMaxOutPutValue());
															} else {
																device.setOutputValue(0);
															}

														}
													}
												}
											}
										}
									}
								} else {

									Map<String, Device> deviceMap = dssBridgeHandler.getDsidToDeviceMap();

									if (groupId != -1) {
										Map<Short, List<String>> map = dssBridgeHandler.getDigitalSTROMZoneGroupMap()
												.get(zoneId);
										if (map != null) {
											List<String> dsidList = map
													.get(groupId);
											if (dsidList != null) {
												for (String dsid : dsidList) {
													Device device = deviceMap
															.get(dsid);

													if (device != null) {

														if (!device
																.containsSceneConfig(sceneId)) {
															dssBridgeHandler.getSceneSpec(
																	device,
																	sceneId);
														}

														if (!device
																.doIgnoreScene(sceneId)) {
															short sceneValue = device
																	.getSceneOutputValue(sceneId);
															if (sceneValue == -1) {
																initDeviceOutputValue(
																		device,
																		DeviceConstants.DEVICE_SENSOR_OUTPUT);
																initSceneOutputValue(
																		device,
																		sceneId);
															} else {
																device.setOutputValue(sceneValue);
															}
														}
													}
												}
											}
										}
									}
								}
							}

							else {

								if (isDimmScene(sceneId)) {

									if (groupId != -1) {
										Map<Short, List<String>> map = dssBridgeHandler.getDigitalSTROMZoneGroupMap()
												.get(zoneId);
										if (map != null) {
											List<String> devicesInGroup = map
													.get(groupId);
											if (devicesInGroup != null) {
												Map<String, Device> deviceMap = dssBridgeHandler.getDsidToDeviceMap();

												for (String dsid : devicesInGroup) {
													Device device = deviceMap
															.get(dsid);

													if (device != null) {

														if (!device
																.containsSceneConfig(sceneId)) {
															dssBridgeHandler.getSceneSpec(
																	device,
																	sceneId);
														}

														if (!device
																.doIgnoreScene(sceneId)) {
															handleDimmScene(
																	deviceMap
																			.get(dsid),
																	sceneId,
																	groupId,
																	false);
														}

													}
												}
											}
										}
									}
								} else if (stateMapper.isMappable(sceneId)) {

									boolean shouldBeOn = stateMapper
											.getMapping(sceneId);
									Map<Short, List<String>> map = dssBridgeHandler.getDigitalSTROMZoneGroupMap()
											.get(zoneId);
									Map<String, Device> deviceMap = dssBridgeHandler.getDsidToDeviceMap();

									if (map != null) {

										if (groupId != -1) {
											List<String> devicesInGroup = map
													.get(groupId);
											if (devicesInGroup != null) {
												for (String dsid : devicesInGroup) {
													Device device = deviceMap
															.get(dsid);

													if (device != null) {
														if (!device
																.containsSceneConfig(sceneId)) {
															dssBridgeHandler.getSceneSpec(
																	device,
																	sceneId);
														}

														if (!device
																.doIgnoreScene(sceneId)) {
															if (shouldBeOn) {
																device.setOutputValue(device
																		.getMaxOutPutValue());
															} else {
																device.setOutputValue(0);
															}
														}

													}
												}
											}
										}
									}
								} else {

									Map<Short, List<String>> map = dssBridgeHandler.getDigitalSTROMZoneGroupMap()
											.get(zoneId);
									Map<String, Device> deviceMap = dssBridgeHandler.getDsidToDeviceMap();
									if (map != null) {

										if (groupId != -1) {
											List<String> devicesInGroup = map
													.get(groupId);
											if (devicesInGroup != null) {
												for (String dsid : devicesInGroup) {
													Device device = deviceMap
															.get(dsid);
													if (device != null) {

														if (!device
																.containsSceneConfig(sceneId)) {
															dssBridgeHandler.getSceneSpec(
																	device,
																	sceneId);
														}

														if (!device
																.doIgnoreScene(sceneId)) {
															short outputValue = device
																	.getSceneOutputValue(sceneId);
															if (outputValue == -1) {
																initDeviceOutputValue(
																		device,
																		DeviceConstants.DEVICE_SENSOR_OUTPUT);
																initSceneOutputValue(
																		device,
																		sceneId);
															} else {
																device.setOutputValue(outputValue);
															}
														}
													}
												}
											}
										}
									}
								}
							}
						}
					}
				}
			} else {
				logger.error("an event without a sceneID; groupID:" + groupId
						+ ", zoneID:" + zoneId + ", isDeviceCall:"
						+ deviceCallStr + ", dsid:" + dsidStr);
			}
		}
	}

	private boolean isEcho(String dsid, short sceneId) {
		String echo = dsid + "-" + sceneId;
		synchronized (echoBox) {
			if (echoBox.contains(echo)) {
				echoBox.remove(echo);
				return true;
			}
		}
		return false;
	}
	
	// ... we want to ignore own 'command-echos'
	public void addEcho(String dsid, short sceneId) {
		synchronized (echoBox) {
			echoBox.add(dsid + "-" + sceneId);
		}
	}
	
	private boolean isDimmScene(short sceneId) {
		if (sceneId > 9 && sceneId < 13) {
			return true;
		}
		if (sceneId == 15) { // command to dim or for a roller shutter
			return true;
		}
		if (sceneId > 41 && sceneId < 50) {
			return true;
		}
		if (sceneId > 51 && sceneId < 56) { // command to dim or for a roller
											// shutter
			return true;
		}
		return false;
	}
	
	private void handleDimmScene(Device device, short sceneID, short groupID,
			boolean force) {

		if ((groupID == -1 && !force)) {
			return;
		}

		if (device.isDimmable()) {

			switch (sceneID) {

			case 11:
			case 42:
			case 44:
			case 46:
			case 48:
				decrease(device);
				break;

			case 12:
			case 43:
			case 45:
			case 47:
			case 49:
				increase(device);
				break;
			default:
				break;

			}

		} else if (device.isRollershutter()) {
			switch (sceneID) {

			case 15:

				initDeviceOutputValue(device,
						DeviceConstants.DEVICE_SENSOR_OUTPUT);
				if (device.getOutputMode().equals(OutputModeEnum.SLAT)) {
					initDeviceOutputValue(device,
							DeviceConstants.DEVICE_SENSOR_SLAT_OUTPUT);
				}
				break;
			case 52:
			case 53:
			case 54:
			case 55:

				initDeviceOutputValue(device,
						DeviceConstants.DEVICE_SENSOR_OUTPUT);
				if (device.getOutputMode().equals(OutputModeEnum.SLAT)) {
					initDeviceOutputValue(device,
							DeviceConstants.DEVICE_SENSOR_SLAT_OUTPUT);
				}
				break;
			default:

			}
		}

	}
	
	private boolean isApartmentScene(short sceneId) {
		return (sceneId > 63);
	}

	private void handleApartmentScene(short sceneId, short groupId) {

		if (groupId == 0) {
			Map<String, Device> clonedDeviceMap = dssBridgeHandler.getDsidToDeviceMap();
			Set<String> dsidSet = clonedDeviceMap.keySet();

			for (String dsid : dsidSet) {
				Device device = clonedDeviceMap.get(dsid);

				if (device != null) {

					if (!device.containsSceneConfig(sceneId)) {
						dssBridgeHandler.getSceneSpec(device, sceneId);
					}

					if (!device.doIgnoreScene(sceneId)) {
						short output = device.getSceneOutputValue(sceneId);
						if (output != -1) {
							device.setOutputValue(output);
						} else {
							initDeviceOutputValue(device,
									DeviceConstants.DEVICE_SENSOR_OUTPUT);
							initSceneOutputValue(device, sceneId);
						}
					}
				}
			}
		} else if (groupId != -1) {

			Map<String, Device> clonedDeviceMap = dssBridgeHandler.getDsidToDeviceMap();
			Map<Short, List<String>> map = dssBridgeHandler.getDigitalSTROMZoneGroupMap().get(0);
			List<String> dsidList = map.get(groupId);

			if (dsidList != null) {
				for (String dsid : dsidList) {
					Device device = clonedDeviceMap.get(dsid);

					if (device != null) {

						if (!device.containsSceneConfig(sceneId)) {
							dssBridgeHandler.getSceneSpec(device, sceneId);
						}

						if (!device.doIgnoreScene(sceneId)) {
							short output = device.getSceneOutputValue(sceneId);
							if (output != -1) {
								device.setOutputValue(output);
							} else {
								initDeviceOutputValue(device,
										DeviceConstants.DEVICE_SENSOR_OUTPUT);
								initSceneOutputValue(device, sceneId);
							}
						}
					}
				}
			}
		}
	}


	
	private void decrease(Device device) {
		initDeviceOutputValue(device, DeviceConstants.DEVICE_SENSOR_OUTPUT);
	}

	private void increase(Device device) {
		initDeviceOutputValue(device, DeviceConstants.DEVICE_SENSOR_OUTPUT);
	}

	private void initDeviceOutputValue(Device device, short index) {
		sensorJobExecutor.addHighPriorityJob(new DeviceOutputValueSensorJob(device, index));
	}

	private void initSceneOutputValue(Device device, short sceneId) {
		sensorJobExecutor.addMediumPriorityJob(new SceneOutputValueSensorJob(device, sceneId));
	}

}