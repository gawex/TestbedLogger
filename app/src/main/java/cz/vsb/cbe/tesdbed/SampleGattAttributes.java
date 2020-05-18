package cz.vsb.cbe.tesdbed;

import java.util.HashMap;

public class SampleGattAttributes {

    private static HashMap<String, String> ATTRIBUTES = new HashMap();

    public static String GENERIC_ACCESS_SERVICE                 = "00001800-0000-1000-8000-00805f9b34fb";
    public static String DEVICE_NAME_CHARACTERISTIC             = "00002a00-0000-1000-8000-00805f9b34fb";
    public static String APPEARANCE_CHARACTERISTIC              = "00002a01-0000-1000-8000-00805f9b34fb";

    public static String DEVICE_INFORMATION_SERVICE             = "0000180a-0000-1000-8000-00805f9b34fb";
    public static String MANUFACTURED_NAME_CHARACTERISTIC       = "00002a29-0000-1000-8000-00805f9b34fb";

    public static String TESTBED_SERVICE                        = "21110001-e780-4112-a997-5c3e475adbc3";
    public static String TEMPERATURE_CHARACTERISTIC             = "21110002-e780-4112-a997-5c3e475adbc3";
    public static String HEART_RATE_CHARACTERISTIC              = "21110003-e780-4112-a997-5c3e475adbc3";
    public static String STEPS_CHARACTERISTIC                   = "21110004-e780-4112-a997-5c3e475adbc3";
    public static String DEVICE_IDENTITY_CHARACTERISTIC         = "21110005-e780-4112-a997-5c3e475adbc3";

    public static String CLIENT_CHARACTERISTIC_CONFIG           = "00002902-0000-1000-8000-00805f9b34fb";

    static {
        // Sample Services
        ATTRIBUTES.put(GENERIC_ACCESS_SERVICE, "Generic Access");
        ATTRIBUTES.put(DEVICE_INFORMATION_SERVICE, "Device Information");
        ATTRIBUTES.put(TESTBED_SERVICE, "Testbed");
        // Sample Characteristics
        ATTRIBUTES.put(DEVICE_NAME_CHARACTERISTIC, "Device Name");
        ATTRIBUTES.put(APPEARANCE_CHARACTERISTIC, "Appearance");
        ATTRIBUTES.put(MANUFACTURED_NAME_CHARACTERISTIC, "Manufactured Name");
        ATTRIBUTES.put(TEMPERATURE_CHARACTERISTIC, "Temperature from Testbed");
        ATTRIBUTES.put(HEART_RATE_CHARACTERISTIC, "Heart Rate from Testbed");
        ATTRIBUTES.put(STEPS_CHARACTERISTIC, "Steps from Testbed");
        ATTRIBUTES.put(DEVICE_IDENTITY_CHARACTERISTIC, "Device Identity form Testbed");
    }

    public static String lookupFromUUID(String uuid, String defaultName) {
        String name = ATTRIBUTES.get(uuid);
        if (name == null)
            return defaultName;
        else
            return name;
    }
}