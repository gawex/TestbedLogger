package cz.vsb.cbe.tesdbed;

import android.bluetooth.BluetoothDevice;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Parcel;
import android.os.Parcelable;

import java.util.HashMap;
import java.util.Map;

public class TestbedDevice implements Parcelable {

    public static Integer BLUETOOTH_DEVICE = 0;
    public static Integer RSSI = 1;
    public static Integer AVAILABLE_SENSORS = 2;
    public static Integer DEVICE_ID = 3;
    public static Integer IS_STORED = 4;
    public static Integer IS_DISCOVERED = 5;

    public static Integer NOT_STORED = 0;
    public static Integer STORED_BUT_MODIFIED = 1;
    public static Integer STORED_CONSISTENTLY = 2;

    private HashMap<Integer, Object> HashMap;
    private Context Context;

    public static final Parcelable.Creator<TestbedDevice> CREATOR = new Parcelable.Creator<TestbedDevice>() {
        @Override
        public TestbedDevice createFromParcel(Parcel source) {
            return new TestbedDevice(source);
        }
        @Override
        public TestbedDevice[] newArray(int size) {
            return new TestbedDevice[size];
        }
    };

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel out, int flags) {
        out.writeInt(HashMap.size());
        for(Map.Entry<Integer, Object> entry : HashMap.entrySet()) {
            out.writeInt(entry.getKey());
            out.writeValue(entry.getValue());
        }
    }

    private TestbedDevice(Parcel parcel) {
        this.HashMap = new HashMap<>();
        int size = parcel.readInt();

        for(int i = 0; i < size; i++) {
            int key = parcel.readInt();
            Object value = parcel.readValue(Object.class.getClassLoader());
            HashMap.put(key,value);
        }
    }

    public TestbedDevice(Context context){
        HashMap = new HashMap<>();
        Context = context;
    }

    public void addBluetoothDevice(BluetoothDevice bluetoothDevice){
        HashMap.put(BLUETOOTH_DEVICE, bluetoothDevice);
    }

    public BluetoothDevice getBluetoothDevice(){
        return (BluetoothDevice) HashMap.get(BLUETOOTH_DEVICE);
    }

    public void addRssi(int rssi){
        HashMap.put(RSSI, rssi);
    }

    public int getRssi(){
        return (Integer) HashMap.get(RSSI);
    }

    public void addAvailableSensors(int availableSensors) {
        HashMap.put(AVAILABLE_SENSORS, availableSensors);

    }

    public int getAvailableSensors(){
        if(HashMap.containsKey(AVAILABLE_SENSORS))
            return (Integer) HashMap.get(AVAILABLE_SENSORS);
        else
            return 8;
    }

    public void addDeviceId(int id){
        HashMap.put(DEVICE_ID, id);
    }

    public int getDeviceId(){
        if(HashMap.containsKey(DEVICE_ID))
            return (Integer) HashMap.get(DEVICE_ID);
        else
            return 0x1FFFF;
    }

    public void storeIntoDatabase(){
        TestbedDbHelper testbedDbHelper = TestbedDbHelper.getInstance(Context);
        SQLiteDatabase writableTestbedDb = testbedDbHelper.getWritableDatabase();

        ContentValues values = new ContentValues();
        values.put(TestbedDbHelper.Device.COLUMN_NAME_DEVICE_ID, getDeviceId());
        values.put(TestbedDbHelper.Device.COLUMN_NAME_AVAILABLE_SENSORS, getAvailableSensors());
        values.put(TestbedDbHelper.Device.COLUMN_NAME_BLUETOOTH_MAC_ADDRESS, getBluetoothDevice().getAddress());
        values.put(TestbedDbHelper.Device.COLUMN_NAME_TIMESTAMP, System.currentTimeMillis());

        writableTestbedDb.insert(TestbedDbHelper.Device.TABLE_NAME, null, values);

        testbedDbHelper.close();
    }

    public int isStored(){
        TestbedDbHelper testbedDbHelper = TestbedDbHelper.getInstance(Context);
        SQLiteDatabase readableTestbedDb = testbedDbHelper.getReadableDatabase();
        int status = NOT_STORED;
        Cursor storedTestbedDevices = readableTestbedDb.query(
                // The table to query
                TestbedDbHelper.Device.TABLE_NAME,
                // The array of columns to return (pass null to get all)
                new String[]{
                        TestbedDbHelper.Device.COLUMN_NAME_DEVICE_ID,
                        TestbedDbHelper.Device.COLUMN_NAME_AVAILABLE_SENSORS,
                        TestbedDbHelper.Device.COLUMN_NAME_BLUETOOTH_MAC_ADDRESS,
                        TestbedDbHelper.Device.COLUMN_NAME_TIMESTAMP},
                // The columns for the WHERE clause
                TestbedDbHelper.Device.COLUMN_NAME_DEVICE_ID + " = ?",
                // The values for the WHERE clause
                new String[]{Integer.toString(getDeviceId())},
                // don't group the rows
                null,
                // don't filter by row groups
                null,
                // The sort order
                null
        );

        while(storedTestbedDevices.moveToNext()) {
            if (storedTestbedDevices.getLong(storedTestbedDevices.getColumnIndexOrThrow(TestbedDbHelper.Device.COLUMN_NAME_DEVICE_ID)) == getDeviceId()) {
                if (storedTestbedDevices.getLong(storedTestbedDevices.getColumnIndexOrThrow(TestbedDbHelper.Device.COLUMN_NAME_AVAILABLE_SENSORS)) == getAvailableSensors() &&
                    storedTestbedDevices.getString(storedTestbedDevices.getColumnIndexOrThrow(TestbedDbHelper.Device.COLUMN_NAME_BLUETOOTH_MAC_ADDRESS)).equals(getBluetoothDevice().getAddress()) &&
                    storedTestbedDevices.getLong(storedTestbedDevices.getColumnIndexOrThrow(TestbedDbHelper.Device.COLUMN_NAME_TIMESTAMP)) <= System.currentTimeMillis()) {
                    status = STORED_CONSISTENTLY;
                } else {
                    status = STORED_BUT_MODIFIED;
                }
                break;
            }
        }
        storedTestbedDevices.close();
        testbedDbHelper.close();
        return status;

    }

    public void deviceIsDiscovered(){
        HashMap.put(IS_DISCOVERED, true);
    }

    public boolean isDeviceDiscovered(){
        if(HashMap.containsKey(IS_DISCOVERED)){
            if ((boolean) HashMap.get(IS_DISCOVERED)) {
                return true;
            } else {
                return false;
            }
        } else {
            return false;
        }
    }
}
