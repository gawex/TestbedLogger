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
import android.bluetooth.BluetoothClass;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothProfile;
import android.content.Context;
import android.content.Intent;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Typeface;
import android.os.Binder;
import android.os.IBinder;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.TextUtils;
import android.text.style.ForegroundColorSpan;
import android.text.style.StyleSpan;
import android.util.Log;

import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;

import java.util.Arrays;
import java.util.List;

import cz.vsb.cbe.tesdbed.sql.TestbedDatabase;

/**
 * Service for managing connection and data communication with a GATT server hosted on a
 * given Bluetooth LE device.
 */
public class BluetoothLeService extends Service {

    private final static String TAG = BluetoothLeService.class.getSimpleName();

    private static final int NOTIFICATION_ID = 0;

    private static final int STATE_DISCONNECTED = 0;
    private static final int STATE_CONNECTING = 1;
    private static final int STATE_CONNECTED = 2;

    public final static String ACTION_GATT_CONNECTED                = "cz.vsb.cbe.testbed.ACTION_GATT_CONNECTED";
    public final static String ACTION_GATT_SERVICES_DISCOVERED      = "cz.vsb.cbe.testbed.ACTION_GATT_SERVICES_DISCOVERED";
    public final static String ACTION_GATT_DESCRIPTOR_WRITTEN       = "cz.vsb.cbe.testbed.ACTION_GATT_DESCRIPTOR_WRITTEN";
    public final static String ACTION_TESTBED_ID_DATA_AVAILABLE     = "cz.vsb.cbe.testbed.ACTION_TESTBED_ID_DATA_AVAILABLE";
    public final static String ACTION_STEP_DATA_AVAILABLE           = "cz.vsb.cbe.testbed.ACTION_STEP_DATA_AVAILABLE";
    public final static String ACTION_HEART_RATE_DATA_AVAILABLE     = "cz.vsb.cbe.testbed.ACTION_HEART_RATE_DATA_AVAILABLE";
    public final static String ACTION_TEMPERATURE_DATA_AVAILABLE    = "cz.vsb.cbe.testbed.ACTION_TEMPERATURE_DATA_AVAILABLE";
    public final static String ACTION_UNKNOWN_DATA_AVAILABLE        = "cz.vsb.cbe.testbed.ACTION_UNKNOWN_DATA_AVAILABLE";
    public final static String ACTION_GATT_DISCONNECTED             = "cz.vsb.cbe.testbed.ACTION_GATT_DISCONNECTED";

    public final static String AVAILABLE_SENSORS_DATA   = "cz.vsb.cbe.testbed.AVAILABLE_SENSORS_DATA";
    public final static String TESTBED_ID_DATA          = "cz.vsb.cbe.testbed.TESTBED_ID_DATA";
    public static final String STEPS_DATA               = "cz.vsb.cbe.testbed.STEPS_DATA";
    public static final String HEART_RATE_DATA          = "cz.vsb.cbe.testbed.HEART_RATE_DATA";
    public static final String TEMPERATURE_DATA         = "cz.vsb.cbe.testbed.TEMPERATURE_DATA";
    public final static String UNKNOWN_DATA           = "cz.vsb.cbe.testbed.UNKNOWN_DATA";

    private TestbedDevice TestbedDevice;

    private BluetoothManager BluetoothManager;
    private BluetoothAdapter BluetoothAdapter;
    private String BluetoothDeviceAddress;
    private BluetoothGatt BluetoothGatt;

    private NotificationManagerCompat NotificationManager;

    private int LastStepValue = -100;
    private int LastHeartRateValue = -100;
    private float LastTemperatureValue = -100;


    public class LocalBinder extends Binder {
        BluetoothLeService getService() {
            return BluetoothLeService.this;
        }
    }

    private final IBinder localBinder = new LocalBinder();

    @Override
    public void onCreate() {
        super.onCreate();
        Log.w(TAG, "SERVICE CREATED");
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.w(TAG, "SERVICE STARTED");
        return START_NOT_STICKY;

    }

    @Override
    public IBinder onBind(Intent intent) {
        Log.w(TAG, "SERVICE BOUND");
        return localBinder;
    }

    public void setTestbedDevice(TestbedDevice testbedDevice){
        this.TestbedDevice = testbedDevice;
    }

    // Implements callback methods for GATT events that the app cares about.  For example,
    // connection change and services discovered.
    private final BluetoothGattCallback mGattCallback = new BluetoothGattCallback() {
        @Override
        public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
            //String intentAction;
            if (newState == BluetoothProfile.STATE_CONNECTED) {
                broadcastUpdate(ACTION_GATT_CONNECTED);
                Log.i(TAG, "Connected to GATT server.");
                // Attempts to discover services after successful connection.
                Log.i(TAG, "Attempting to start service discovery: " +
                        BluetoothGatt.discoverServices());
                if (TestbedDevice != null) {
                    NotificationManager = NotificationManagerCompat.from(getApplicationContext());
                    NotificationManager.notify(NOTIFICATION_ID, buildNotification(getColor(R.color.colorPrimary)).setSmallIcon((R.drawable.ic_mac_address)).build());
                }
            } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                broadcastUpdate(ACTION_GATT_DISCONNECTED);
                Log.i(TAG, "Disconnected from GATT server.");
                if (NotificationManager != null) {
                    NotificationManager.cancel(NOTIFICATION_ID);
                }
            }
        }

        @Override
        public void onServicesDiscovered(BluetoothGatt gatt, int status) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                broadcastUpdate(ACTION_GATT_SERVICES_DISCOVERED);
                Log.i(TAG, "onServicesDiscovered received: " + status);
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
        public void onCharacteristicRead(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                if(characteristic.getUuid().equals(SampleGattAttributes.STEPS_CHARACTERISTIC_UUID)) {
                    if(!characteristic.getStringValue(0).equals("ERROR")) {
                        broadcastUpdate(ACTION_STEP_DATA_AVAILABLE, characteristic);
                    }
                } else if (characteristic.getUuid().equals(SampleGattAttributes.HEART_RATE_CHARACTERISTIC_UUID)) {
                    if(!characteristic.getStringValue(0).equals("ERROR")){
                        broadcastUpdate(ACTION_HEART_RATE_DATA_AVAILABLE, characteristic);
                    }
                } else if (characteristic.getUuid().equals(SampleGattAttributes.TEMPERATURE_CHARACTERISTIC_UUID)) {
                    if(!characteristic.getStringValue(0).equals("ERROR"))
                        broadcastUpdate(ACTION_TEMPERATURE_DATA_AVAILABLE, characteristic);
                }
                else if (characteristic.getUuid().equals(SampleGattAttributes.TESTBED_ID_CHARACTERISTIC_UUID)) {
                    if(!characteristic.getStringValue(0).equals("ERROR")) {
                        broadcastUpdate(ACTION_TESTBED_ID_DATA_AVAILABLE, characteristic);
                    }
                }else {
                    broadcastUpdate(ACTION_UNKNOWN_DATA_AVAILABLE, characteristic);
                }
            }
        }

        @Override
        public void onCharacteristicChanged(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic) {
            if(characteristic.getUuid().equals(SampleGattAttributes.STEPS_CHARACTERISTIC_UUID)) {
                if(!characteristic.getStringValue(0).equals("ERROR")) {
                    broadcastUpdate(ACTION_STEP_DATA_AVAILABLE, characteristic);
                }
            } else if (characteristic.getUuid().equals(SampleGattAttributes.HEART_RATE_CHARACTERISTIC_UUID)) {
                if(!characteristic.getStringValue(0).equals("ERROR")){
                    broadcastUpdate(ACTION_HEART_RATE_DATA_AVAILABLE, characteristic);
                }
            } else if (characteristic.getUuid().equals(SampleGattAttributes.TEMPERATURE_CHARACTERISTIC_UUID)) {
                if(!characteristic.getStringValue(0).equals("ERROR")) {
                    broadcastUpdate(ACTION_TEMPERATURE_DATA_AVAILABLE, characteristic);
                }
            } else {
                broadcastUpdate(ACTION_UNKNOWN_DATA_AVAILABLE, characteristic);
            }
        }
    };

    private void broadcastUpdate(final String action) {
        sendBroadcast(new Intent(action));
    }

    private NotificationCompat.Builder buildNotification(int colorHeadlight) {
        NotificationCompat.Builder notificationBuilder = new NotificationCompat.Builder(getApplicationContext(), getString(R.string.notification_channel_id));
        NotificationCompat.InboxStyle inboxStyle = new NotificationCompat.InboxStyle();
        String title;
        int [] notificationStatus = new int [] {0, 0, 0};
        int notificationNumber = 0;
        if(TestbedDevice != null){
            title = getString(R.string.notification_title) + ": " +
                    getString(R.string.ble_devices_name) + " (#" +
                    Integer.toHexString(TestbedDevice.getDeviceId()) + ")";
        } else {
            title = getString(R.string.notification_title) + ": " +
                    getString(R.string.ble_devices_name) + " (#1FFFF)";
        }

        inboxStyle.setBigContentTitle(title);
        inboxStyle.setSummaryText(getString(R.string.notification_text) + "muj je text");

        if (LastStepValue != -100) {
            Spannable stepHeader = new SpannableString(getString(R.string.notification_last_step_value_header));
            stepHeader.setSpan(new StyleSpan(Typeface.BOLD), 0, stepHeader.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);

            Spannable stepValue = new SpannableString(String.valueOf(LastStepValue));
            stepValue.setSpan(new StyleSpan(Typeface.BOLD), 0, stepValue.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
            stepValue.setSpan(new ForegroundColorSpan(colorHeadlight), 0, stepValue.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);

            Spannable stepUnit =  new SpannableString(getString(R.string.notification_last_step_value_unit));

            inboxStyle.addLine(TextUtils.concat(stepHeader, " ", stepValue, " ", stepUnit));
            notificationStatus[0] = 1;
        }

        if (LastHeartRateValue != -100) {
            Spannable heartRateHeader = new SpannableString(getString(R.string.notification_last_heart_rate_value_header));
            heartRateHeader.setSpan(new StyleSpan(Typeface.BOLD), 0, heartRateHeader.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);

            Spannable heartRateValue = new SpannableString(String.valueOf(LastHeartRateValue));
            heartRateValue.setSpan(new StyleSpan(Typeface.BOLD), 0, heartRateValue.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
            heartRateValue.setSpan(new ForegroundColorSpan(colorHeadlight), 0, heartRateValue.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);

            Spannable heartRateUnit =  new SpannableString(getString(R.string.notification_last_heart_rate_value_unit));

            inboxStyle.addLine(TextUtils.concat(heartRateHeader, " ", heartRateValue, " ", heartRateUnit));
            notificationStatus[1] = 1;
        }

        if (LastTemperatureValue != -100) {
            Spannable temperatureHeader = new SpannableString(getString(R.string.notification_last_temperature_value_header));
            temperatureHeader.setSpan(new StyleSpan(Typeface.BOLD), 0, temperatureHeader.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);

            Spannable temperatureValue = new SpannableString(String.format("%3.2f", LastTemperatureValue));
            temperatureValue.setSpan(new StyleSpan(Typeface.BOLD), 0, temperatureValue.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
            temperatureValue.setSpan(new ForegroundColorSpan(colorHeadlight), 0, temperatureValue.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);

            Spannable temperatureUnit =  new SpannableString(getString(R.string.notification_last_temperature_value_unit));

            inboxStyle.addLine(TextUtils.concat(temperatureHeader, " ", temperatureValue, " ", temperatureUnit));
            notificationStatus[2] = 1;
        }

        for (int i : notificationStatus){
            notificationNumber += i;
        }

         return notificationBuilder.setContentTitle(title)
                .setContentText(getString(R.string.notification_text))
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setStyle(inboxStyle)
                .setNumber(notificationNumber > 0 ? notificationNumber : 0);
    }


    private void broadcastUpdate(final String action, final BluetoothGattCharacteristic characteristic) {
        final Intent intent = new Intent(action);

         if(SampleGattAttributes.TESTBED_ID_CHARACTERISTIC_UUID.equals(characteristic.getUuid())) {
             intent.putExtra(AVAILABLE_SENSORS_DATA, Integer.parseInt(characteristic.getStringValue(0).substring(0,1)));
             intent.putExtra(TESTBED_ID_DATA, Integer.parseInt(characteristic.getStringValue(1),16));
         } else if (SampleGattAttributes.STEPS_CHARACTERISTIC_UUID.equals(characteristic.getUuid())) {
             int steps = Integer.parseInt(characteristic.getStringValue(0));
             TestbedDatabase.getInstance(getApplicationContext()).insertSteps(TestbedDevice.getDeviceId(), steps);
             LastStepValue = steps;
             NotificationManager.notify(NOTIFICATION_ID, buildNotification(getColor(R.color.colorPrimary)).setSmallIcon((R.drawable.ic_pedometer_available)).build());
             Log.w(TAG, STEPS_DATA + " = " + Float.parseFloat(characteristic.getStringValue(0)) + " steps.");
         } else if (SampleGattAttributes.HEART_RATE_CHARACTERISTIC_UUID.equals(characteristic.getUuid())) {
             int heartRate = Integer.parseInt(characteristic.getStringValue(0));
             TestbedDatabase.getInstance(getApplicationContext()).insertHeartRate(TestbedDevice.getDeviceId(), heartRate);
             LastHeartRateValue = heartRate;
             NotificationManager.notify(NOTIFICATION_ID, buildNotification(getColor(R.color.colorPrimary)).setSmallIcon((R.drawable.ic_heart_rate_meter_available)).build());
             Log.w(TAG, HEART_RATE_DATA + " = " + Float.parseFloat(characteristic.getStringValue(0)) + " beats per one minute.");
         } else if (SampleGattAttributes.TEMPERATURE_CHARACTERISTIC_UUID.equals(characteristic.getUuid())) {
             float temperature = Float.parseFloat(characteristic.getStringValue(0));
             TestbedDatabase.getInstance(getApplicationContext()).insertTemperature(TestbedDevice.getDeviceId(), temperature);
             LastTemperatureValue = temperature;
             NotificationManager.notify(NOTIFICATION_ID, buildNotification(getColor(R.color.colorPrimary)).setSmallIcon((R.drawable.ic_thermometer_available)).build());
             Log.w(TAG, TEMPERATURE_DATA + " = " + Float.parseFloat(characteristic.getStringValue(0)) + " °C.");
         }  else {
            intent.putExtra(UNKNOWN_DATA, characteristic.getStringValue(0));
            Log.w(TAG, UNKNOWN_DATA + " = " + characteristic.getStringValue(0));
        }
        sendBroadcast(intent);
    }

    @Override
    public boolean onUnbind(Intent intent) {
        Log.w(TAG, "SERVICE UNBOUNDED");
        return super.onUnbind(intent);
    }

    @Override
    public void onDestroy() {
        Log.w(TAG, "SERVICE DESTROYED");
        close();
        TestbedDatabase.getInstance(this).close();
        super.onDestroy();
    }

    /**
     * Initializes a reference to the local Bluetooth adapter.
     *
     * @return Return true if the initialization is successful.
     */
    public boolean initialize() {
        // For API level 18 and above, get a reference to BluetoothAdapter through
        // BluetoothManager.
        if (BluetoothManager == null) {
            BluetoothManager = (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
            if (BluetoothManager == null) {
                Log.e(TAG, "Unable to initialize BluetoothManager.");
                return false;
            }
        }

        BluetoothAdapter = BluetoothManager.getAdapter();
        if (BluetoothAdapter == null) {
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
    // Connects to given device


    public boolean connect(final String address) {
        if (BluetoothAdapter == null || address == null) {
            Log.w(TAG, "BluetoothAdapter not initialized or unspecified address.");
            return false;
        }
        // Previously connected device.  Try to reconnect.
        if (BluetoothDeviceAddress != null && address.equals(BluetoothDeviceAddress)
                && BluetoothGatt != null) {
            Log.d(TAG, "Trying to use an existing mBluetoothGatt for connection.");
            if (BluetoothGatt.connect()) {
                return true;
            } else {
                return false;
            }
        }

        final BluetoothDevice device = BluetoothAdapter.getRemoteDevice(address);
        if (device == null) {
            Log.w(TAG, "Device not found.  Unable to connect.");
            return false;
        }
        // We want to directly connect to the device, so we are setting the autoConnect
        // parameter to false.
        BluetoothGatt = device.connectGatt(this, false, mGattCallback);
        Log.d(TAG, "Trying to create a new connection.");
        BluetoothDeviceAddress = address;
        return true;
    }

    /**
     * Disconnects an existing connection or cancel a pending connection. The disconnection result
     * is reported asynchronously through the
     * {@code BluetoothGattCallback#onConnectionStateChange(android.bluetooth.BluetoothGatt, int, int)}
     * callback.
     */
    public void disconnect() {
        if (BluetoothAdapter == null || BluetoothGatt == null) {
            Log.w(TAG, "BluetoothAdapter not initialized");
            return;
        }
        BluetoothGatt.disconnect();
    }

    /**
     * After using a given BLE device, the app must call this method to ensure resources are
     * released properly.
     */
    public void close() {
        if (BluetoothGatt == null) {
            return;
        }
        BluetoothGatt.close();
        BluetoothGatt = null;
    }

    /**
     * Request a read on a given {@code BluetoothGattCharacteristic}. The read result is reported
     * asynchronously through the {@code BluetoothGattCallback#onCharacteristicRead(android.bluetooth.BluetoothGatt, android.bluetooth.BluetoothGattCharacteristic, int)}
     * callback.
     *
     * @param characteristic The characteristic to read from.
     */
    public void readCharacteristic(BluetoothGattCharacteristic characteristic) {
        if (BluetoothAdapter == null || BluetoothGatt == null) {
            Log.w(TAG, "BluetoothAdapter not initialized");
            return;
        }
        BluetoothGatt.readCharacteristic(characteristic);
    }

    /**
     * Enables or disables notification on a give characteristic.
     *
     * @param characteristic Characteristic to act on.
     * @param enabled If true, enable notification.  False otherwise.
     */
    public void setCharacteristicNotification(BluetoothGattCharacteristic characteristic, boolean enabled) {
        if (BluetoothAdapter == null || BluetoothGatt == null) {
            Log.w(TAG, "BluetoothAdapter not initialized");
            return;
        }
        BluetoothGatt.setCharacteristicNotification(characteristic, enabled);

        if (characteristic.getUuid().equals(SampleGattAttributes.STEPS_CHARACTERISTIC_UUID) ||
            characteristic.getUuid().equals(SampleGattAttributes.HEART_RATE_CHARACTERISTIC_UUID) ||
            characteristic.getUuid().equals(SampleGattAttributes.TEMPERATURE_CHARACTERISTIC_UUID)) {

            BluetoothGattDescriptor bluetoothGattDescriptor = characteristic.getDescriptor(SampleGattAttributes.CLIENT_CHARACTERISTIC_CONFIGURATION_UUID);
            bluetoothGattDescriptor.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
            BluetoothGatt.writeDescriptor(bluetoothGattDescriptor);
        }
    }

    /**
     * Retrieves a list of supported GATT services on the connected device. This should be
     * invoked only after {@code BluetoothGatt#discoverServices()} completes successfully.
     *
     * @return A {@code List} of supported services.
     */
    public List<BluetoothGattService> getSupportedGattServices() {
        if (BluetoothGatt == null) return null;

        return BluetoothGatt.getServices();
    }
}
