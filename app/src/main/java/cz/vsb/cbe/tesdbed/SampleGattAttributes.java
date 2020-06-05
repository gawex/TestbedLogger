package cz.vsb.cbe.tesdbed;

import java.util.HashMap;
import java.util.UUID;

public class SampleGattAttributes {

    private static HashMap<UUID, String> ATTRIBUTES = new HashMap();

    public static UUID GENERIC_ACCESS_SERVICE_UUID              = UUID.fromString("00001800-0000-1000-8000-00805f9b34fb");
    public static UUID DEVICE_NAME_CHARACTERISTIC_UUID          = UUID.fromString("00002a00-0000-1000-8000-00805f9b34fb");
    public static UUID APPEARANCE_CHARACTERISTIC_UUID           = UUID.fromString("00002a01-0000-1000-8000-00805f9b34fb");


    public static UUID DEVICE_INFORMATION_SERVICE_UUID          = UUID.fromString("0000180a-0000-1000-8000-00805f9b34fb");
    public static UUID MANUFACTURER_NAME_CHARACTERISTIC_UUID    = UUID.fromString("00002a29-0000-1000-8000-00805f9b34fb");

    public static UUID TESTBED_SERVICE_UUID                     = UUID.fromString("21110001-e780-4112-a997-5c3e475adbc3");
    public static UUID TESTBED_ID_CHARACTERISTIC_UUID           = UUID.fromString("21110005-e780-4112-a997-5c3e475adbc3");
    public static UUID HEART_RATE_CHARACTERISTIC_UUID           = UUID.fromString("21110003-e780-4112-a997-5c3e475adbc3");
    public static UUID STEPS_CHARACTERISTIC_UUID                = UUID.fromString("21110004-e780-4112-a997-5c3e475adbc3");
    public static UUID TEMPERATURE_CHARACTERISTIC_UUID          = UUID.fromString("21110002-e780-4112-a997-5c3e475adbc3");

    public static UUID CLIENT_CHARACTERISTIC_CONFIGURATION_UUID = UUID.fromString("00002902-0000-1000-8000-00805f9b34fb");
    public static UUID CHARACTERISTIC_USER_DESCRIPTION_UUID     = UUID.fromString("00002902-0000-1000-8000-00805f9b34fb");

    public static String TESTBED_SERVICE_NAME                   = "Testbed";
    public static String TESTBED_ID_CHARACTERISTIC_NAME         = "TestbedID";
    public static String HEART_RATE_CHARACTERISTIC_NAME         = "Heart rate";
    public static String STEPS_CHARACTERISTIC_NAME              = "Steps";
    public static String TEMPERATURE_CHARACTERISTIC_NAME        = "Temperatrure";



    static {
        // Sample Services
        ATTRIBUTES.put(GENERIC_ACCESS_SERVICE_UUID, "Generic Access");
        ATTRIBUTES.put(DEVICE_INFORMATION_SERVICE_UUID, "Device Information");
        ATTRIBUTES.put(TESTBED_SERVICE_UUID, TESTBED_SERVICE_NAME);
        // Sample Characteristics
        ATTRIBUTES.put(DEVICE_NAME_CHARACTERISTIC_UUID, "Device Name");
        ATTRIBUTES.put(APPEARANCE_CHARACTERISTIC_UUID, "Appearance");
        ATTRIBUTES.put(MANUFACTURER_NAME_CHARACTERISTIC_UUID, "Manufactured Name");
        ATTRIBUTES.put(TEMPERATURE_CHARACTERISTIC_UUID, TEMPERATURE_CHARACTERISTIC_NAME);
        ATTRIBUTES.put(HEART_RATE_CHARACTERISTIC_UUID, HEART_RATE_CHARACTERISTIC_NAME);
        ATTRIBUTES.put(STEPS_CHARACTERISTIC_UUID, STEPS_CHARACTERISTIC_NAME);
        ATTRIBUTES.put(TESTBED_ID_CHARACTERISTIC_UUID, TESTBED_ID_CHARACTERISTIC_NAME);
    }

    public static String lookupFromUUID(UUID uuid, String defaultName) {
        String name = ATTRIBUTES.get(uuid);
        if (name == null)
            return defaultName;
        else
            return name;
    }
}