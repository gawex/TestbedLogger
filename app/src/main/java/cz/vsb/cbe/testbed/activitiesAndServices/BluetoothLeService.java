/*
  @author  Bc. Lukas Tatarin
 * @supervisor Ing. Jaromir Konecny, Ph.D.
 * @email   lukas@tatarin.cz
 * @version 1.10
 * @ide     Android Studio 4.1.2
 * @license GNU GPL v3
 * @brief   BluetoothLeService.java
 * @lastmodify 2021/03/05 11:36:56
 * @verbatim
----------------------------------------------------------------------
Copyright (C) Bc. Lukas Tatarin, 2021

This program is free software: you can redistribute it and/or modify
it under the terms of the GNU General Public License as published by
the Free Software Foundation, either version 3 of the License, or
any later version.

This program is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
See the GNU General Public License for more details.

<http://www.gnu.org/licenses/>
 @endverbatim
 */

package cz.vsb.cbe.testbed.activitiesAndServices;

import android.app.PendingIntent;
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
import android.content.Context;
import android.content.Intent;
import android.graphics.Typeface;
import android.os.Binder;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.TextUtils;
import android.text.style.ForegroundColorSpan;
import android.text.style.StyleSpan;

import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Objects;

import cz.vsb.cbe.testbed.R;
import cz.vsb.cbe.testbed.sql.DatabaseResult;
import cz.vsb.cbe.testbed.sql.TestbedDatabase;
import cz.vsb.cbe.testbed.sql.TestbedDevice;
import cz.vsb.cbe.testbed.utils.RecordValueFormatter;
import cz.vsb.cbe.testbed.utils.SampleGattAttributes;


public class BluetoothLeService extends Service {

    public static final String NOTIFICATION_CHANNEL_ID = "cz.vsb.cbe.testbed.LOGGING_CHANNEL";
    public static final String ACTION_GATT_CONNECTED =
            "cz.vsb.cbe.testbed.ACTION_GATT_CONNECTED";
    public static final String ACTION_GATT_SERVICES_DISCOVERED =
            "cz.vsb.cbe.testbed.ACTION_GATT_SERVICES_DISCOVERED";
    public static final String ACTION_GATT_DESCRIPTOR_WRITTEN =
            "cz.vsb.cbe.testbed.ACTION_GATT_DESCRIPTOR_WRITTEN";
    public static final String ACTION_GATT_ALL_DESCRIPTORS_WRITTEN =
            "cz.vsb.cbe.testbed.ACTION_GATT_ALL_DESCRIPTORS_WRITTEN";
    public static final String ACTION_TESTBED_ID_DATA_AVAILABLE =
            "cz.vsb.cbe.testbed.ACTION_TESTBED_ID_DATA_AVAILABLE";
    public static final String ACTION_STEP_DATA_AVAILABLE =
            "cz.vsb.cbe.testbed.ACTION_STEP_DATA_AVAILABLE";
    public static final String ACTION_HEART_RATE_DATA_AVAILABLE =
            "cz.vsb.cbe.testbed.ACTION_HEART_RATE_DATA_AVAILABLE";
    public static final String ACTION_TEMPERATURE_DATA_AVAILABLE =
            "cz.vsb.cbe.testbed.ACTION_TEMPERATURE_DATA_AVAILABLE";
    public static final String ACTION_UNKNOWN_DATA_AVAILABLE =
            "cz.vsb.cbe.testbed.ACTION_UNKNOWN_DATA_AVAILABLE";
    public static final String ACTION_GATT_DISCONNECTED =
            "cz.vsb.cbe.testbed.ACTION_GATT_DISCONNECTED";
    public static final String AVAILABLE_SENSORS_DATA =
            "cz.vsb.cbe.testbed.AVAILABLE_SENSORS_DATA";
    public static final String TESTBED_ID_DATA = "cz.vsb.cbe.testbed.TESTBED_ID_DATA";
    public static final String STEPS_DATA = "cz.vsb.cbe.testbed.STEPS_DATA";
    public static final String HEART_RATE_DATA = "cz.vsb.cbe.testbed.HEART_RATE_DATA";
    public static final String TEMPERATURE_DATA = "cz.vsb.cbe.testbed.TEMPERATURE_DATA";
    public static final String UNKNOWN_DATA = "cz.vsb.cbe.testbed.UNKNOWN_DATA";
    private static final int NOTIFICATION_ID = 0;
    private static final String ERROR_MESSAGE = "ERROR";
    private static final int DELAY_BEFORE_WRITE_DESCRIPTORS_MS = 2000;
    private final IBinder mLocalBinder = new LocalBinder();
    private TestbedDevice mTestbedDevice;
    private TestbedDatabase mTestbedDatabase;
    private BluetoothManager mBluetoothManager;
    private BluetoothAdapter mBluetoothAdapter;
    private String mBluetoothDeviceAddress;
    private BluetoothGatt mBluetoothGatt;
    private List<BluetoothGattCharacteristic> mCharacteristicsForDescriptorWrite;
    private int mDescriptorWriteIndex = 0;

    private boolean mAutoReconnect = false;

    private NotificationManagerCompat mNotificationManager;

    private Float mLastStepValue;
    private Float mLastHeartRateValue;
    private Float mLastTemperatureValue;

    private Date mLastStepTimeStamp;
    private Date mLastHeartRateTimeStamp;
    private Date mLastTemperatureTimeStamp;
    private boolean mShowNotification;
    private final BluetoothGattCallback mGattCallback = new BluetoothGattCallback() {
        @Override
        public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
            if (newState == BluetoothProfile.STATE_CONNECTED) {
                if (mShowNotification) {
                    mNotificationManager = NotificationManagerCompat.from(getApplicationContext());
                    mNotificationManager.notify(NOTIFICATION_ID, Objects.requireNonNull(
                            buildNotification(getColor(R.color.colorPrimary)))
                            .setSmallIcon((R.drawable.ic_device_id_20dp_color_fei)).build());
                }
                if (mAutoReconnect) {
                    mBluetoothGatt.discoverServices();
                }
                broadcastUpdate(ACTION_GATT_CONNECTED);
            } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                if (mNotificationManager != null) {
                    mNotificationManager.cancel(NOTIFICATION_ID);
                }
                if (mAutoReconnect) {
                    connect(mTestbedDevice, true);
                }
                broadcastUpdate(ACTION_GATT_DISCONNECTED);
            }
        }

        @Override
        public void onServicesDiscovered(BluetoothGatt gatt, int status) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                mCharacteristicsForDescriptorWrite = new ArrayList<>();
                mDescriptorWriteIndex = 0;
                for (BluetoothGattService bluetoothGattService : getSupportedGattServices()) {
                    if (bluetoothGattService.getUuid()
                            .equals(SampleGattAttributes.TESTBED_SERVICE_UUID)) {
                        for (BluetoothGattCharacteristic bluetoothGattCharacteristic :
                                bluetoothGattService.getCharacteristics()) {
                            if (bluetoothGattCharacteristic.getUuid()
                                    .equals(SampleGattAttributes.STEPS_CHARACTERISTIC_UUID) &&
                                    BigInteger.valueOf(mTestbedDevice.getAvailableSensors())
                                            .testBit(2)) {
                                mCharacteristicsForDescriptorWrite.add(bluetoothGattCharacteristic);
                            } else if (bluetoothGattCharacteristic.getUuid()
                                    .equals(SampleGattAttributes.HEART_RATE_CHARACTERISTIC_UUID) &&
                                    BigInteger.valueOf(mTestbedDevice.getAvailableSensors())
                                            .testBit(1)) {
                                mCharacteristicsForDescriptorWrite.add(bluetoothGattCharacteristic);
                            } else if (bluetoothGattCharacteristic.getUuid()
                                    .equals(SampleGattAttributes.TEMPERATURE_CHARACTERISTIC_UUID) &&
                                    BigInteger.valueOf(mTestbedDevice.getAvailableSensors())
                                            .testBit(0)) {
                                mCharacteristicsForDescriptorWrite.add(bluetoothGattCharacteristic);
                            }
                        }
                    }
                }
                if (mAutoReconnect) {
                    startWritingCharacteristicNotification();
                }
                broadcastUpdate(ACTION_GATT_SERVICES_DISCOVERED);
            }
        }

        @Override
        public void onDescriptorWrite(BluetoothGatt gatt, BluetoothGattDescriptor descriptor,
                                      int status) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                mDescriptorWriteIndex++;
                if (mDescriptorWriteIndex < mCharacteristicsForDescriptorWrite.size()) {
                    setCharacteristicNotification(mCharacteristicsForDescriptorWrite
                            .get(mDescriptorWriteIndex));
                    broadcastUpdate(ACTION_GATT_DESCRIPTOR_WRITTEN);
                } else {
                    broadcastUpdate(ACTION_GATT_ALL_DESCRIPTORS_WRITTEN);
                }
                //broadcastUpdate(ACTION_GATT_DESCRIPTOR_WRITTEN);
            }
        }

        @Override
        public void onCharacteristicRead(BluetoothGatt gatt,
                                         BluetoothGattCharacteristic characteristic, int status) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                if (characteristic.getUuid().equals(SampleGattAttributes.STEPS_CHARACTERISTIC_UUID)) {
                    if (!characteristic.getStringValue(0).equals(ERROR_MESSAGE)) {
                        broadcastUpdate(ACTION_STEP_DATA_AVAILABLE, characteristic);
                    }
                } else if (characteristic.getUuid()
                        .equals(SampleGattAttributes.HEART_RATE_CHARACTERISTIC_UUID)) {
                    if (!characteristic.getStringValue(0).equals(ERROR_MESSAGE)) {
                        broadcastUpdate(ACTION_HEART_RATE_DATA_AVAILABLE, characteristic);
                    }
                } else if (characteristic.getUuid()
                        .equals(SampleGattAttributes.TEMPERATURE_CHARACTERISTIC_UUID)) {
                    if (!characteristic.getStringValue(0).equals(ERROR_MESSAGE))
                        broadcastUpdate(ACTION_TEMPERATURE_DATA_AVAILABLE, characteristic);
                } else if (characteristic.getUuid()
                        .equals(SampleGattAttributes.TESTBED_ID_CHARACTERISTIC_UUID)) {
                    if (!characteristic.getStringValue(0).equals(ERROR_MESSAGE)) {
                        broadcastUpdate(ACTION_TESTBED_ID_DATA_AVAILABLE, characteristic);
                    }
                } else {
                    broadcastUpdate(ACTION_UNKNOWN_DATA_AVAILABLE, characteristic);
                }
            }
        }

        @Override
        public void onCharacteristicChanged(BluetoothGatt gatt,
                                            BluetoothGattCharacteristic characteristic) {
            if (characteristic.getUuid().equals(SampleGattAttributes.STEPS_CHARACTERISTIC_UUID)) {
                if (!characteristic.getStringValue(0).equals(ERROR_MESSAGE)) {
                    broadcastUpdate(ACTION_STEP_DATA_AVAILABLE, characteristic);
                }
            } else if (characteristic.getUuid()
                    .equals(SampleGattAttributes.HEART_RATE_CHARACTERISTIC_UUID)) {
                if (!characteristic.getStringValue(0).equals(ERROR_MESSAGE)) {
                    broadcastUpdate(ACTION_HEART_RATE_DATA_AVAILABLE, characteristic);
                }
            } else if (characteristic.getUuid()
                    .equals(SampleGattAttributes.TEMPERATURE_CHARACTERISTIC_UUID)) {
                if (!characteristic.getStringValue(0).equals(ERROR_MESSAGE)) {
                    broadcastUpdate(ACTION_TEMPERATURE_DATA_AVAILABLE, characteristic);
                }
            } else {
                broadcastUpdate(ACTION_UNKNOWN_DATA_AVAILABLE, characteristic);
            }
        }
    };

    public int getCharacteristicForDescriptorWriteSize() {
        return mCharacteristicsForDescriptorWrite.size();
    }

    public int getDescriptorWriteIndex() {
        return mDescriptorWriteIndex;
    }

    public Float getLastStepValue() {
        return mLastStepValue;
    }

    public Float getLastHeartRateValue() {
        return mLastHeartRateValue;
    }

    public Float getmLastTemperatureValue() {
        return mLastTemperatureValue;
    }

    public Date getLastStepTimeStamp() {
        return mLastStepTimeStamp;
    }

    public Date getLastHeartRateTimeStamp() {
        return mLastHeartRateTimeStamp;
    }

    public Date getmLastTemperatureTimeStamp() {
        return mLastTemperatureTimeStamp;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        mTestbedDatabase = new TestbedDatabase(this);
        mCharacteristicsForDescriptorWrite = new ArrayList<>();
    }

    @Override
    public IBinder onBind(Intent intent) {
        return mLocalBinder;
    }

    public void removeTestbedDevice() {
        this.mTestbedDevice = null;
    }

    public void setAutoReconnect(boolean enable) {
        this.mAutoReconnect = enable;
    }

    public void discoverServices() {
        mBluetoothGatt.discoverServices();
    }

    public void startWritingCharacteristicNotification() {
        new Handler(Looper.getMainLooper()).postDelayed(() ->
                setCharacteristicNotification(mCharacteristicsForDescriptorWrite
                        .get(mDescriptorWriteIndex)), DELAY_BEFORE_WRITE_DESCRIPTORS_MS);
    }

    private void broadcastUpdate(final String action) {
        sendBroadcast(new Intent(action));
    }

    @SuppressWarnings("unchecked")
    private void broadcastUpdate(final String action,
                                 final BluetoothGattCharacteristic characteristic) {
        final Intent intent = new Intent(action);
        if (SampleGattAttributes.TESTBED_ID_CHARACTERISTIC_UUID
                .equals(characteristic.getUuid())) {
            intent.putExtra(AVAILABLE_SENSORS_DATA,
                    Integer.parseInt(characteristic.getStringValue(0).substring(0, 1)));
            intent.putExtra(TESTBED_ID_DATA,
                    Integer.parseInt(characteristic.getStringValue(1), 16));
            sendBroadcast(intent);
        } else if (SampleGattAttributes.STEPS_CHARACTERISTIC_UUID.equals(characteristic.getUuid())) {
            mTestbedDatabase.insertRecord(mTestbedDevice, BluetoothLeService.STEPS_DATA,
                    Integer.parseInt(characteristic.getStringValue(0)), databaseResult -> {
                        float steps = ((DatabaseResult.Success<Float>) databaseResult).data;
                        intent.putExtra(STEPS_DATA, steps);
                        mLastStepValue = steps;
                        mLastStepTimeStamp = new Date();
                        mNotificationManager.notify(NOTIFICATION_ID, Objects.requireNonNull(
                                buildNotification(getColor(R.color.colorPrimary)))
                                .setSmallIcon((R.drawable.ic_steps_20dp_color_fei)).build());
                        sendBroadcast(intent);
                    });
        } else if (SampleGattAttributes.HEART_RATE_CHARACTERISTIC_UUID.equals(characteristic.getUuid())) {
            if (Integer.parseInt(characteristic.getStringValue(0)) != 0) {
                mTestbedDatabase.insertRecord(mTestbedDevice, BluetoothLeService.HEART_RATE_DATA,
                        Integer.parseInt(characteristic.getStringValue(0)), databaseResult -> {
                            float heartRate = ((DatabaseResult.Success<Float>) databaseResult).data;
                            intent.putExtra(HEART_RATE_DATA, heartRate);
                            mLastHeartRateValue = heartRate;
                            mLastHeartRateTimeStamp = new Date();
                            mNotificationManager.notify(NOTIFICATION_ID, Objects.requireNonNull(
                                    buildNotification(getColor(R.color.colorPrimary)))
                                    .setSmallIcon((R.drawable.ic_heart_20dp_color_fei)).build());
                            sendBroadcast(intent);
                        });
            }
        } else if (SampleGattAttributes.TEMPERATURE_CHARACTERISTIC_UUID.equals(characteristic.getUuid())) {
            mTestbedDatabase.insertRecord(mTestbedDevice, BluetoothLeService.TEMPERATURE_DATA,
                    Float.parseFloat(characteristic.getStringValue(0)), databaseResult -> {
                        float temperature = ((DatabaseResult.Success<Float>) databaseResult).data;
                        intent.putExtra(TEMPERATURE_DATA, temperature);
                        mLastTemperatureValue = temperature;
                        mLastTemperatureTimeStamp = new Date(System.currentTimeMillis());
                        mNotificationManager.notify(NOTIFICATION_ID, Objects.requireNonNull(
                                buildNotification(getColor(R.color.colorPrimary)))
                                .setSmallIcon((R.drawable.ic_thermometer_20dp_color_fei)).build());
                        sendBroadcast(intent);
                    });
        } else {
            intent.putExtra(UNKNOWN_DATA, characteristic.getStringValue(0));
            sendBroadcast(intent);
        }
    }

    private NotificationCompat.Builder buildNotification(int colorHeadlight) {
        if (mTestbedDevice != null) {
            NotificationCompat.Builder notificationBuilder =
                    new NotificationCompat.Builder(getApplicationContext(), NOTIFICATION_CHANNEL_ID);
            String title = getString(R.string.notification_logging_title) + ": " +
                    mTestbedDevice.getName() + " (" +
                    TestbedDevice.formatTestbedDeviceId(mTestbedDevice) + ")";

            NotificationCompat.InboxStyle inboxStyle = new NotificationCompat.InboxStyle();
            inboxStyle.setBigContentTitle(title);
            inboxStyle.setSummaryText(getString(R.string.notification_logging_text));

            if (mLastStepValue != null) {
                Spannable stepHeader = new SpannableString(
                        getString(R.string.activity_database_fragment_title_pedometer));
                stepHeader.setSpan(new StyleSpan(Typeface.BOLD), 0, stepHeader.length(),
                        Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);

                Spannable stepValue = new SpannableString(RecordValueFormatter
                        .formatSteps(RecordValueFormatter.PATERN_6_0, mLastStepValue));
                stepValue.setSpan(new StyleSpan(Typeface.BOLD), 0, stepValue.length(),
                        Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
                stepValue.setSpan(new ForegroundColorSpan(colorHeadlight), 0,
                        stepValue.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);

                Spannable time = new SpannableString(RecordValueFormatter
                        .formatTimeStampTime(mLastStepTimeStamp));

                inboxStyle.addLine(TextUtils.concat(stepHeader, ": ", stepValue,
                        " (", time, ")"));
            }

            if (mLastHeartRateValue != null) {
                Spannable heartRateHeader = new SpannableString(getString(R.string.activity_database_fragment_title_heart_rate_meter));
                heartRateHeader.setSpan(new StyleSpan(Typeface.BOLD), 0,
                        heartRateHeader.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);

                Spannable heartRateValue = new SpannableString(RecordValueFormatter
                        .formatHearRate(RecordValueFormatter.PATERN_3_0, mLastHeartRateValue));
                heartRateValue.setSpan(new StyleSpan(Typeface.BOLD), 0,
                        heartRateValue.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
                heartRateValue.setSpan(new ForegroundColorSpan(colorHeadlight), 0,
                        heartRateValue.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);

                Spannable time = new SpannableString(RecordValueFormatter
                        .formatTimeStampTime(mLastHeartRateTimeStamp));

                inboxStyle.addLine(TextUtils.concat(heartRateHeader, ": ", heartRateValue,
                        " (", time, ")"));
            }

            if (mLastTemperatureValue != null) {
                Spannable temperatureHeader = new SpannableString(getString(R.string.activity_database_fragment_title_thermometer));
                temperatureHeader.setSpan(new StyleSpan(Typeface.BOLD), 0,
                        temperatureHeader.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);

                Spannable temperatureValue = new SpannableString(RecordValueFormatter
                        .formatTemperature(RecordValueFormatter.PATERN_3_2, mLastTemperatureValue));
                temperatureValue.setSpan(new StyleSpan(Typeface.BOLD), 0,
                        temperatureValue.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
                temperatureValue.setSpan(new ForegroundColorSpan(colorHeadlight), 0,
                        temperatureValue.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);

                Spannable time = new SpannableString(RecordValueFormatter.
                        formatTimeStampTime(mLastTemperatureTimeStamp));

                inboxStyle.addLine(TextUtils.concat(temperatureHeader, ": ", temperatureValue,
                        " (", time, ")"));
            }

            Intent notifyIntent = new Intent(this, DatabaseActivity.class);
            notifyIntent.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP | Intent.FLAG_ACTIVITY_CLEAR_TOP);
            PendingIntent notifyPendingIntent = PendingIntent.getActivity(this,
                    0, notifyIntent, PendingIntent.FLAG_UPDATE_CURRENT);

            return notificationBuilder.setContentTitle(title)
                    .setContentText(getString(R.string.notification_logging_text))
                    .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                    .setStyle(inboxStyle)
                    .setNumber(1)
                    .setContentIntent(notifyPendingIntent)
                    .setOngoing(true)
                    .setColor(getColor(R.color.ColorVsb));
        } else {
            return null;
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (mNotificationManager != null) {
            mNotificationManager.cancel(NOTIFICATION_ID);
        }
        setAutoReconnect(false);
        mTestbedDatabase.close();
        close();
    }

    public boolean initialize() {
        if (mBluetoothManager == null) {
            mBluetoothManager = (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
            if (mBluetoothManager == null) {
                return false;
            }
        }
        mBluetoothAdapter = mBluetoothManager.getAdapter();
        return mBluetoothAdapter != null;
    }

    public void connect(TestbedDevice testbedDevice, boolean showNotification) {
        mTestbedDevice = testbedDevice;
        mShowNotification = showNotification;
        if (mBluetoothAdapter != null && testbedDevice.getMacAddress() != null) {
            if (mBluetoothGatt != null && testbedDevice.getMacAddress()
                    .equals(mBluetoothDeviceAddress)) {
                mBluetoothGatt.connect();
            } else {
                final BluetoothDevice device = mBluetoothAdapter
                        .getRemoteDevice(testbedDevice.getMacAddress());
                if (device != null) {
                    mBluetoothGatt = device.connectGatt(this, false,
                            mGattCallback);
                    mBluetoothDeviceAddress = testbedDevice.getMacAddress();
                }
            }
        }
    }

    public void disconnect() {
        if (mBluetoothAdapter != null && mBluetoothGatt != null) {
            mBluetoothGatt.disconnect();
            setAutoReconnect(false);
            mLastStepValue = null;
            mLastStepTimeStamp = null;
            mLastHeartRateValue = null;
            mLastHeartRateTimeStamp = null;
            mLastTemperatureValue = null;
            mLastTemperatureTimeStamp = null;
        }
    }

    public void close() {
        if (mBluetoothGatt != null) {
            mBluetoothGatt.close();
            mBluetoothGatt = null;
        }
    }

    public void readCharacteristic(BluetoothGattCharacteristic characteristic) {
        if (mBluetoothAdapter != null && mBluetoothGatt != null) {
            mBluetoothGatt.readCharacteristic(characteristic);
        }
    }

    public void setCharacteristicNotification(BluetoothGattCharacteristic characteristic) {
        if (mBluetoothAdapter != null && mBluetoothGatt != null)
            if (characteristic.getUuid().equals(SampleGattAttributes.STEPS_CHARACTERISTIC_UUID) ||
                    characteristic.getUuid().equals(SampleGattAttributes.HEART_RATE_CHARACTERISTIC_UUID) ||
                    characteristic.getUuid().equals(SampleGattAttributes.TEMPERATURE_CHARACTERISTIC_UUID)) {
                mBluetoothGatt.setCharacteristicNotification(characteristic, true);
                BluetoothGattDescriptor bluetoothGattDescriptor = characteristic
                        .getDescriptor(SampleGattAttributes.CLIENT_CHARACTERISTIC_CONFIGURATION_UUID);
                bluetoothGattDescriptor.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
                mBluetoothGatt.writeDescriptor(bluetoothGattDescriptor);
            }
    }

    public List<BluetoothGattService> getSupportedGattServices() {
        if (mBluetoothGatt == null) {
            return null;
        } else {
            return mBluetoothGatt.getServices();
        }
    }

    public class LocalBinder extends Binder {
        BluetoothLeService getService() {
            return BluetoothLeService.this;
        }
    }
}