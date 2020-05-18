/*
 * Copyright (C) 2013 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package cz.vsb.cbe.tesdbed;

import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;

import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothProfile;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.database.sqlite.SQLiteDatabase;
import android.os.Binder;
import android.os.IBinder;
import android.util.Log;
import android.widget.Toast;

import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

/**
 * Service for managing connection and data communication with a GATT server hosted on a
 * given Bluetooth LE device.
 */
public class BluetoothLeService extends Service {
    private final static String TAG = BluetoothLeService.class.getSimpleName();

    private BluetoothManager mBluetoothManager;
    private BluetoothAdapter mBluetoothAdapter;
    private String mBluetoothDeviceAddress;
    private BluetoothGatt mBluetoothGatt;
    private int mConnectionState = STATE_DISCONNECTED;

    private static final int STATE_DISCONNECTED = 0;
    private static final int STATE_CONNECTING = 1;
    private static final int STATE_CONNECTED = 2;

    public final static String SERVICE_STARTED =
            "cz.vsb.cbe.testbed.SERVICE_STARTED ";

    public final static String ACTION_GATT_CONNECTED =
            "cz.vsb.cbe.testbed.ACTION_GATT_CONNECTED";
    public final static String ACTION_GATT_DISCONNECTED =
            "cz.vsb.cbe.testbed.ACTION_GATT_DISCONNECTED";
    public final static String ACTION_GATT_SERVICES_DISCOVERED =
            "cz.vsb.cbe.testbed.ACTION_GATT_SERVICES_DISCOVERED";
    public final static String ACTION_GATT_DESCRIPTOR_WRITTEN =
            "cz.vsb.cbe.testbed.ACTION_GATT_DESCRIPTOR_WRITTEN";
    public final static String ACTION_DATA_AVAILABLE =
            "cz.vsb.cbe.testbed.ACTION_DATA_AVAILABLE";
    public final static String STEP_DATA_AVAILABLE =
            "cz.vsb.cbe.testbed.STEP_DATA_AVAILABLE";
    public final static String HEART_RATE_DATA_AVAILABLE =
            "cz.vsb.cbe.testbed.HEART_RATE_DATA_AVAILABLE";
    public final static String TEMPERATURE_DATA_AVAILABLE =
            "cz.vsb.cbe.testbed.TEMPERATURE_DATA_AVAILABLE";
    public final static String EXTRA_DATA =
            "cz.vsb.cbe.testbed.EXTRA_DATA";

    public final static String AVAILABLE_SENSORS =
            "cz.vsb.cbe.testbed.AVAILABLE_SENSORS";

    public final static String TESTBED_ID =
            "cz.vsb.cbe.testbed.TESTBED_ID";

    private TestbedDevice testbedDevice;
    private TestbedDbHelper testbedDbHelper;
    private SQLiteDatabase writableTestbedDb;

    // Implements callback methods for GATT events that the app cares about.  For example,
    // connection change and services discovered.
    private final BluetoothGattCallback mGattCallback = new BluetoothGattCallback() {
        @Override
        public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
            String intentAction;
            if (newState == BluetoothProfile.STATE_CONNECTED) {
                intentAction = ACTION_GATT_CONNECTED;
                mConnectionState = STATE_CONNECTED;
                broadcastUpdate(intentAction);
                Log.i(TAG, "Connected to GATT server.");
                // Attempts to discover services after successful connection.
                Log.i(TAG, "Attempting to start service discovery:" +
                        mBluetoothGatt.discoverServices());

            } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                intentAction = ACTION_GATT_DISCONNECTED;
                mConnectionState = STATE_DISCONNECTED;
                Log.i(TAG, "Disconnected from GATT server.");
                broadcastUpdate(intentAction);
            }
        }

        @Override
        public void onServicesDiscovered(BluetoothGatt gatt, int status) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                broadcastUpdate(ACTION_GATT_SERVICES_DISCOVERED);
            } else {
                Log.w(TAG, "onServicesDiscovered received: " + status);
            }
        }

        @Override
        public void onDescriptorWrite (BluetoothGatt gatt, BluetoothGattDescriptor descriptor, int status){
            if (status == BluetoothGatt.GATT_SUCCESS) {
                broadcastUpdate(ACTION_GATT_DESCRIPTOR_WRITTEN);
                Log.w(TAG, "onDescriptorWrite received: " + status);
            } else {
                Log.w(TAG, "onDescriptorWrite received: " + status);
            }
        }

        @Override
        public void onCharacteristicRead(BluetoothGatt gatt,
                                         BluetoothGattCharacteristic characteristic,
                                         int status) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                broadcastUpdate(ACTION_DATA_AVAILABLE, characteristic);
            }
        }

        @Override
        public void onCharacteristicChanged(BluetoothGatt gatt,
                                            BluetoothGattCharacteristic characteristic) {
            if(characteristic.getUuid().equals(UUID.fromString(SampleGattAttributes.STEPS_CHARACTERISTIC))) {
                if(!characteristic.getStringValue(0).equals("ERROR"))
                broadcastUpdate(STEP_DATA_AVAILABLE, characteristic);
            } else if (characteristic.getUuid().equals(UUID.fromString(SampleGattAttributes.HEART_RATE_CHARACTERISTIC))) {
                broadcastUpdate(HEART_RATE_DATA_AVAILABLE, characteristic);
            } else if (characteristic.getUuid().equals(UUID.fromString(SampleGattAttributes.TEMPERATURE_CHARACTERISTIC))) {
                if(!characteristic.getStringValue(0).equals("ERROR"))
                    broadcastUpdate(TEMPERATURE_DATA_AVAILABLE, characteristic);
            } else {
                broadcastUpdate(ACTION_DATA_AVAILABLE, characteristic);
            }
        }
    };

    private void broadcastUpdate(final String action) {
        final Intent intent = new Intent(action);
        sendBroadcast(intent);
    }

    private void broadcastUpdate(final String action,
                                 final BluetoothGattCharacteristic characteristic) {
        final Intent intent = new Intent(action);

        // This is special handling for the Heart Rate Measurement profile.  Data parsing is
        // carried out as per profile specifications:
        // http://developer.bluetooth.org/gatt/characteristics/Pages/CharacteristicViewer.aspx?u=org.bluetooth.characteristic.heart_rate_measurement.xml
        /*if (UUID_HEART_RATE_MEASUREMENT.equals(characteristic.getUuid())) {
            int flag = characteristic.getProperties();
            int format = -1;
            if ((flag & 0x01) != 0) {
                format = BluetoothGattCharacteristic.FORMAT_UINT16;
                Log.d(TAG, "Heart rate format UINT16.");
            } else {
                format = BluetoothGattCharacteristic.FORMAT_UINT8;
                Log.d(TAG, "Heart rate format UINT8.");
            }
            final int heartRate = characteristic.getIntValue(format, 1);
            Log.d(TAG, String.format("Received heart rate: %d", heartRate));
            intent.putExtra(EXTRA_DATA, String.valueOf(heartRate));

        }

        else
            */if(UUID.fromString(SampleGattAttributes.DEVICE_NAME_CHARACTERISTIC).equals(characteristic.getUuid()))
        {
            intent.putExtra(EXTRA_DATA, String.valueOf(characteristic.getStringValue(0)));
        }

        else if(UUID.fromString(SampleGattAttributes.DEVICE_IDENTITY_CHARACTERISTIC).equals(characteristic.getUuid())) {

                String availableSensorsAndTestbedId = characteristic.getStringValue(0);
                String availableSensors = availableSensorsAndTestbedId.substring(0,1);
                String testbedId = availableSensorsAndTestbedId.substring(1, availableSensorsAndTestbedId.length()-1);

                intent.putExtra(AVAILABLE_SENSORS, Integer.parseInt(characteristic.getStringValue(0).substring(0,1)/*availableSensors)*/));
                intent.putExtra(TESTBED_ID, Integer.parseInt(/*testbedId*/characteristic.getStringValue(1),16));
                //int id = Integer.parseInt(testbedId.substring(0,1));
               // int length = testbedId.length();
                //intent.putExtra(AVAILABLE_SENSORS, Integer.parseInt(characteristic.getStringValue(0).substring(0,1)));

                //int idInt  = Integer.parseInt(id, 16);
                //intent.putExtra(TESTBED_ID, Integer.parseInt());

                Log.w(TAG,"SEN_ID: " + availableSensorsAndTestbedId + "(" + availableSensorsAndTestbedId.length() + "), "  +
                                "SEN: " + availableSensors + "(" + availableSensors.length() + "), "+
                                "ID " + testbedId + "(" + testbedId.length() + ")");
                //intent.putExtra(EXTRA_DATA, characteristic.getStringValue(0));


        }

        else if(UUID.fromString(SampleGattAttributes.TEMPERATURE_CHARACTERISTIC).equals(characteristic.getUuid()))
        {
            /*//intent.putExtra(EXTRA_DATA, characteristic.getStringValue(0));
            NumberFormat nf = NumberFormat.getNumberInstance(Locale.US);
            DecimalFormat df = (DecimalFormat) nf;
            df.setMaximumIntegerDigits(3);
            df.setMaximumIntegerDigits(3);
            df.setMinimumFractionDigits(2);
            df.setMaximumFractionDigits(2);
            df.setPositivePrefix("+");*/
            intent.putExtra(EXTRA_DATA, Float.parseFloat(characteristic.getStringValue(0)));
            Log.w(TAG, "TEMP = " + Float.parseFloat(characteristic.getStringValue(0)));

            testbedDbHelper = TestbedDbHelper.getInstance(getApplicationContext());
            writableTestbedDb = testbedDbHelper.getWritableDatabase();

            ContentValues values = new ContentValues();
            values.put(TestbedDbHelper.Data.COLUMN_NAME_DEVICE_ID, testbedDevice.getDeviceId());
            values.put(TestbedDbHelper.Data.COLUMN_NAME_DATA_KEY, "TEMP");
            values.put(TestbedDbHelper.Data.COLUMN_NAME_DATA_VALUE, Float.parseFloat(characteristic.getStringValue(0)));
            values.put(TestbedDbHelper.Data.COLUMN_NAME_TIMESTAMP, System.currentTimeMillis());

            writableTestbedDb.insert(TestbedDbHelper.Data.TABLE_NAME, null, values);

            testbedDbHelper.close();


        }

        else if(UUID.fromString(SampleGattAttributes.STEPS_CHARACTERISTIC).equals(characteristic.getUuid()))
        {
            intent.putExtra(EXTRA_DATA, characteristic.getStringValue(0));

            char[] str = characteristic.getStringValue(0).toCharArray();
            String out = new String();

            for (char c: str){
                if(Character.isDigit(c)){
                    out += c;
                }
            }

            Log.w(TAG, "STEPS = " + out);

            testbedDbHelper = TestbedDbHelper.getInstance(getApplicationContext());
            writableTestbedDb = testbedDbHelper.getWritableDatabase();

            ContentValues values = new ContentValues();
            values.put(TestbedDbHelper.Data.COLUMN_NAME_DEVICE_ID, testbedDevice.getDeviceId());
            values.put(TestbedDbHelper.Data.COLUMN_NAME_DATA_KEY, "STEPS");
            values.put(TestbedDbHelper.Data.COLUMN_NAME_DATA_VALUE, Float.parseFloat(out));
            values.put(TestbedDbHelper.Data.COLUMN_NAME_TIMESTAMP, System.currentTimeMillis());

            writableTestbedDb.insert(TestbedDbHelper.Data.TABLE_NAME, null, values);

            testbedDbHelper.close();
        }

        else if(UUID.fromString(SampleGattAttributes.HEART_RATE_CHARACTERISTIC).equals(characteristic.getUuid()))
        {
            intent.putExtra(EXTRA_DATA, characteristic.getStringValue(0));
            Log.w(TAG, "HEART_RATE = " + characteristic.getStringValue(0));
        }

        else
        {
            intent.putExtra(EXTRA_DATA, characteristic.getStringValue(0));
            Log.w(TAG, "OTHER = " + characteristic.getStringValue(0));




        }

        sendBroadcast(intent);
    }


    public class LocalBinder extends Binder {
        BluetoothLeService getService() {
            return BluetoothLeService.this;
        }
    }

    @Override
    public IBinder onBind(Intent intent) {
        Log.w(TAG, "SERVICE BINDED");
        testbedDevice = intent.getParcelableExtra(TESTBED_ID);

        return mBinder;
    }

    @Override
    public boolean onUnbind(Intent intent) {
        Log.w(TAG, "SERVICE UNBINDED");

        close();
        return super.onUnbind(intent);
    }

    private final IBinder mBinder = new LocalBinder();

    /**
     * Initializes a reference to the local Bluetooth adapter.
     *
     * @return Return true if the initialization is successful.
     */
    public boolean initialize() {
        // For API level 18 and above, get a reference to BluetoothAdapter through
        // BluetoothManager.
        if (mBluetoothManager == null) {
            mBluetoothManager = (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
            if (mBluetoothManager == null) {
                Log.e(TAG, "Unable to initialize BluetoothManager.");
                return false;
            }
        }

        mBluetoothAdapter = mBluetoothManager.getAdapter();
        if (mBluetoothAdapter == null) {
            Log.e(TAG, "Unable to obtain a BluetoothAdapter.");
            return false;
        }

        return true;
    }

    /**
     * Connects to the GATT server hosted on the Bluetooth LE device.
     *
     * @param address The device address of the destination device.
     *
     * @return Return true if the connection is initiated successfully. The connection result
     *         is reported asynchronously through the
     *         {@code BluetoothGattCallback#onConnectionStateChange(android.bluetooth.BluetoothGatt, int, int)}
     *         callback.
     */
    public boolean connect(final String address) {
        if (mBluetoothAdapter == null || address == null) {
            Log.w(TAG, "BluetoothAdapter not initialized or unspecified address.");
            return false;
        }

        // Previously connected device.  Try to reconnect.
        if (mBluetoothDeviceAddress != null && address.equals(mBluetoothDeviceAddress)
                && mBluetoothGatt != null) {
            Log.d(TAG, "Trying to use an existing mBluetoothGatt for connection.");
            if (mBluetoothGatt.connect()) {
                mConnectionState = STATE_CONNECTING;
                return true;
            } else {
                return false;
            }
        }

        final BluetoothDevice device = mBluetoothAdapter.getRemoteDevice(address);
        if (device == null) {
            Log.w(TAG, "Device not found.  Unable to connect.");
            return false;
        }
        // We want to directly connect to the device, so we are setting the autoConnect
        // parameter to false.
        mBluetoothGatt = device.connectGatt(this, false, mGattCallback);
        Log.d(TAG, "Trying to create a new connection.");
        mBluetoothDeviceAddress = address;
        mConnectionState = STATE_CONNECTING;
        return true;
    }

    /**
     * Disconnects an existing connection or cancel a pending connection. The disconnection result
     * is reported asynchronously through the
     * {@code BluetoothGattCallback#onConnectionStateChange(android.bluetooth.BluetoothGatt, int, int)}
     * callback.
     */
    public void disconnect() {
        if (mBluetoothAdapter == null || mBluetoothGatt == null) {
            Log.w(TAG, "BluetoothAdapter not initialized");
            return;
        }
        mBluetoothGatt.disconnect();
    }

    /**
     * After using a given BLE device, the app must call this method to ensure resources are
     * released properly.
     */
    public void close() {
        if (mBluetoothGatt == null) {
            return;
        }
        mBluetoothGatt.close();
        mBluetoothGatt = null;
    }

    /**
     * Request a read on a given {@code BluetoothGattCharacteristic}. The read result is reported
     * asynchronously through the {@code BluetoothGattCallback#onCharacteristicRead(android.bluetooth.BluetoothGatt, android.bluetooth.BluetoothGattCharacteristic, int)}
     * callback.
     *
     * @param characteristic The characteristic to read from.
     */
    public void readCharacteristic(BluetoothGattCharacteristic characteristic) {
        if (mBluetoothAdapter == null || mBluetoothGatt == null) {
            Log.w(TAG, "BluetoothAdapter not initialized");
            return;
        }
        mBluetoothGatt.readCharacteristic(characteristic);
    }

    /**
     * Enables or disables notification on a give characteristic.
     *
     * @param characteristic Characteristic to act on.
     * @param enabled If true, enable notification.  False otherwise.
     */
    public void setCharacteristicNotification(BluetoothGattCharacteristic characteristic,
                                              boolean enabled) {
        if (mBluetoothAdapter == null || mBluetoothGatt == null) {
            Log.w(TAG, "BluetoothAdapter not initialized");
            return;
        }
        mBluetoothGatt.setCharacteristicNotification(characteristic, enabled);


        // This is specific to Heart Rate Measurement.


        //if (UUID.fromString(SampleGattAttributes.TEMPERATURE_CHARACTERISTIC).equals(characteristic.getUuid())) {
            BluetoothGattDescriptor descriptor = characteristic.getDescriptor(
                    UUID.fromString(SampleGattAttributes.CLIENT_CHARACTERISTIC_CONFIG));
            descriptor.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
            mBluetoothGatt.writeDescriptor(descriptor);
        //}

/*
        if (UUID.fromString(SampleGattAttributes.TEMPERATURE_CHARACTERISTIC).equals(characteristic.getUuid())) {
            List<BluetoothGattDescriptor> descriptors = characteristic.getDescriptors();

            for (BluetoothGattDescriptor descriptor : descriptors){
                if (descriptor.equals(UUID.fromString(SampleGattAttributes.CLIENT_CHARACTERISTIC_CONFIG))){
                    Log.w(TAG, "Descriptor" + descriptor.getUuid().toString());
                    if (descriptor != null) {
                        descriptor.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
                        mBluetoothGatt.writeDescriptor(descriptor);
                    }
                }
            }
        }*/
    }

    /**
     * Retrieves a list of supported GATT services on the connected device. This should be
     * invoked only after {@code BluetoothGatt#discoverServices()} completes successfully.
     *
     * @return A {@code List} of supported services.
     */
    public List<BluetoothGattService> getSupportedGattServices() {
        if (mBluetoothGatt == null) return null;

        return mBluetoothGatt.getServices();
    }
}
