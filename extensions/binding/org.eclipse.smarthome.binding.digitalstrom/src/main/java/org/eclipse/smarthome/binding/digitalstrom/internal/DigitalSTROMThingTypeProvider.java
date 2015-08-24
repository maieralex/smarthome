package org.eclipse.smarthome.binding.digitalstrom.internal;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;

import org.eclipse.smarthome.binding.digitalstrom.DigitalSTROMBindingConstants;
import org.eclipse.smarthome.binding.digitalstrom.handler.DsDeviceHandler;
import org.eclipse.smarthome.binding.digitalstrom.internal.digitalSTROMLibary.digitalSTROMManager.DigitalSTROMConnectionManager;
import org.eclipse.smarthome.binding.digitalstrom.internal.digitalSTROMLibary.digitalSTROMStructure.digitalSTROMDevices.Device;
import org.eclipse.smarthome.core.thing.ThingTypeUID;
import org.eclipse.smarthome.core.thing.binding.ThingTypeProvider;
import org.eclipse.smarthome.core.thing.type.ChannelDefinition;
import org.eclipse.smarthome.core.thing.type.ChannelType;
import org.eclipse.smarthome.core.thing.type.ChannelTypeUID;
import org.eclipse.smarthome.core.thing.type.ThingType;

public class DigitalSTROMThingTypeProvider implements ThingTypeProvider {

    List<String> supportedBridgeTypeUIDs = new LinkedList<String>();

    HashMap<String, ThingType> thingTypeMapEN = new HashMap<String, ThingType>();
    HashMap<String, ThingType> thingTypeMapDE = new HashMap<String, ThingType>();

    DigitalSTROMThingTypeProvider() {
        supportedBridgeTypeUIDs.add(DigitalSTROMBindingConstants.THING_TYPE_DSS_BRIDGE.toString());
    }

    public void registerConnectionManagerHandler(DigitalSTROMConnectionManager connMan) {
        if (connMan != null) {
            if (connMan.checkConnection()) {
                generateReachableThingTypes(
                        connMan.getDigitalSTROMAPI().getApartmentDevices(connMan.getSessionToken(), false));
            }
        }
    }

    @Override
    public Collection<ThingType> getThingTypes(Locale locale) {
        return locale != null && locale.getLanguage().equals(Locale.GERMAN) ? this.thingTypeMapDE.values()
                : this.thingTypeMapEN.values();
    }

    @Override
    public ThingType getThingType(ThingTypeUID thingTypeUID, Locale locale) {
        ThingType thingType = null;
        String hwInfo = thingTypeUID.getId();
        if (locale != null && locale.getDisplayLanguage().equals(Locale.GERMAN)) {
            thingType = this.thingTypeMapDE.get(hwInfo);
        } else {
            thingType = this.thingTypeMapEN.get(hwInfo);
        }

        if (thingType == null) {
            thingType = generateThingType(thingTypeUID, locale);
        }
        return thingType;
    }

    /* Label and description build constats */
    private final String PREFIX_LABEL_EN = "DigitalSTROM device ";
    private final String PREFIX_DESC_EN = "This is a DigitalStrom ";
    private final String POSTFIX_DESC_EN = " device.";

    private final String GE_EN = "(yellow)";
    private final String GE_FUNC_EN = "light";
    private final String GR_EN = "(gray)";
    private final String GR_FUNC_EN = "shade";
    private final String SW_EN = "(black)";
    private final String SW_FUNC_EN = "joker";

    private final String PREFIX_LABEL_DE = "DigitalSTROM Klemme ";
    private final String PREFIX_DESC_DE = "Dies ist eine DigitalSTROM ";
    private final String POSTFIX_DESC_DE = " Klemme.";

    private final String GE_DE = "(gelb)";
    private final String GE_FUNC_DE = "Licht";
    private final String GR_DE = "(grau)";
    private final String GR_FUNC_DE = "Schatten";
    private final String SW_DE = "(schwarz)";
    private final String SW_FUNC_DE = "Joker";

    private ChannelDefinition chanDefBrigh = new ChannelDefinition("brightness",
            new ChannelType(new ChannelTypeUID("digitalstrom:brightness"), false, "Dimmer", "Dimm channel",
                    "this is a channel to to dimm an light Device", "light", null, null, null),
            null, "Dimm channel", "this is a channel to to dimm an light Device");

    private ThingType generateThingType(ThingTypeUID thingTypeUID, Locale locale) {
        return generateThingType(thingTypeUID.getId(), locale, thingTypeUID);

    }

    private ThingType generateThingType(String hwInfo, Locale locale, ThingTypeUID thingTypeUID) {
        String hwType = hwInfo.substring(0, 2);

        String labelEN;
        String descEN;
        String labelDE;
        String descDE;

        switch (hwType) {
            case "GE":
                labelEN = PREFIX_LABEL_EN + hwInfo + " " + GE_EN;
                descEN = PREFIX_DESC_EN + " " + GE_FUNC_EN + " " + POSTFIX_DESC_EN;
                labelDE = PREFIX_LABEL_DE + hwInfo + " " + GE_DE;
                descDE = PREFIX_DESC_DE + " " + GE_FUNC_DE + " " + POSTFIX_DESC_DE;
                break;
            case "GR":
                labelEN = PREFIX_LABEL_EN + hwInfo + " " + GR_EN;
                descEN = PREFIX_DESC_EN + " " + GR_FUNC_EN + " " + POSTFIX_DESC_EN;
                labelDE = PREFIX_LABEL_DE + hwInfo + " " + GR_DE;
                descDE = PREFIX_DESC_DE + " " + GR_FUNC_DE + " " + POSTFIX_DESC_DE;
                break;
            case "SW":
                labelEN = PREFIX_LABEL_EN + hwInfo + " " + SW_EN;
                descEN = PREFIX_DESC_EN + " " + SW_FUNC_EN + " " + POSTFIX_DESC_EN;
                labelDE = PREFIX_LABEL_DE + hwInfo + " " + SW_DE;
                descDE = PREFIX_DESC_DE + " " + SW_FUNC_DE + " " + POSTFIX_DESC_DE;
                break;
            default:
                return null;
        }
        try {
            if (thingTypeUID == null) {
                thingTypeUID = new ThingTypeUID(DigitalSTROMBindingConstants.BINDING_ID, hwInfo);
            }
            List<ChannelDefinition> channelList = new LinkedList<ChannelDefinition>();
            channelList.add(chanDefBrigh);
            ThingType thingTypeEN = new ThingType(thingTypeUID, supportedBridgeTypeUIDs, labelEN, descEN, null, null,
                    null, new URI(DigitalSTROMBindingConstants.DEVICE_CONFIG));
            this.thingTypeMapEN.put(hwInfo, thingTypeEN);
            ThingType thingTypeDE = new ThingType(thingTypeUID, supportedBridgeTypeUIDs, labelDE, descDE, null, null,
                    null, new URI(DigitalSTROMBindingConstants.DEVICE_CONFIG));
            this.thingTypeMapDE.put(hwInfo, thingTypeDE);

            DsDeviceHandler.SUPPORTED_THING_TYPES.add(thingTypeUID);

            if (locale != null) {
                return locale.getLanguage().equals(Locale.GERMAN) ? thingTypeDE : thingTypeEN;
            }
        } catch (IllegalArgumentException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (URISyntaxException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

        return null;
    }

    private void generateReachableThingTypes(List<Device> deviceList) {
        if (deviceList != null) {
            for (Device device : deviceList) {
                if (!this.thingTypeMapDE.containsKey(device.getHWinfo())) {
                    if (device.isDeviceWithOutput()) {
                        generateThingType(device.getHWinfo(), null, null);
                    }
                }
            }
        }
    }

}
