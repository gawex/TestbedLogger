/*
  @author  Bc. Lukas Tatarin
 * @supervisor Ing. Jaromir Konecny, Ph.D.
 * @email   lukas@tatarin.cz
 * @version 1.00
 * @ide     Android Studio 4.1.2
 * @license GNU GPL v3
 * @brief   DiscoveryDevicesActivity.java
 * @lastmodify 2021/03/04 07:23:29
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

package cz.vsb.cbe.testbed;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanFilter;
import android.bluetooth.le.ScanResult;
import android.bluetooth.le.ScanSettings;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.pm.ActivityInfo;
import android.location.LocationManager;
import android.os.Build;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.Handler;
import android.os.IBinder;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResult;
import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.content.res.AppCompatResources;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

import cz.vsb.cbe.testbed.BluetoothLeService.LocalBinder;
import cz.vsb.cbe.testbed.sql.TestbedDatabase;
import cz.vsb.cbe.testbed.sql.TestbedDevice;

import static cz.vsb.cbe.testbed.StartUpActivity.FIRST_RUN_APPLICATION;

public class DiscoverDevicesActivity extends AppCompatActivity {

    private static final long SCAN_PERIOD_MS = 10000;
    private static final int SPLASH_TIME_START_ACTIVITY_MS = 500;
    private static final int SPLASH_TIME_HIDE_DIALOG_MS = 1000;

    private static final int LAST_CONNECTED_TESTBED_DEVICE_STATUS_COUNT = 5;
    private static final int SCANNING_FOR_LAST_CONNECTED_TESTBED_DEVICE_STATUS = 0;
    private static final int CONNECTING_TO_LAST_CONNECTED_TESTBED_DEVICE_STATUS = 1;
    private static final int LAST_CONNECTED_TESTBED_DEVICE_SERVICE_DISCOVERING_STATUS = 2;
    private static final int LAST_CONNECTED_TESTBED_DEVICE_STORED_STATUS_CHECKING_STATUS = 3;
    private static final int LAST_CONNECTED_TESTBED_DEVICE_NOTIFICATION_ENABLED_STATUS = 4;

    private static final int SELECTED_TESTBED_DEVICE_STATUS_COUNT = 3;
    private static final int CONNECTING_TO_SELCTED_TESTBED_DEVICE_STATUS = 0;
    private static final int SELECTED_TESTBED_DEVICE_SERVICE_DISCOVERING_STATUS = 1;
    private static final int SELECTED_TESTBED_DEVICE_NOTIFICATION_ENABLED_STATUS = 2;

    private static final String DEVICE_NAME_FILTER = "Testbed";
    private TestbedDatabase mTestbedDatabase;

    private static IntentFilter SYSTEM_SERVICES_INTENT_FILTER() {
        final IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(LocationManager.MODE_CHANGED_ACTION);
        intentFilter.addAction(BluetoothAdapter.ACTION_STATE_CHANGED);
        return intentFilter;
    }

    private BluetoothLeService mBluetoothLeService;
    private final ServiceConnection mServiceConnection = new ServiceConnection() {

        @Override
        public void onServiceConnected(ComponentName componentName, IBinder service) {
            mBluetoothLeService = ((LocalBinder) service).getService();
            if (!mBluetoothLeService.initialize()) {
                Toast.makeText(DiscoverDevicesActivity.this,
                        getString(R.string.activity_discover_devices_bluetooth_le_service_failed),
                        Toast.LENGTH_SHORT).show();
                finish();
            }
        }

        @Override
        public void onServiceDisconnected(ComponentName componentName) {
            mBluetoothLeService = null;
        }
    };
    private BluetoothAdapter mBluetoothAdapter;
    private BluetoothLeScanner mBluetoothLeScanner;
    private TestbedDevicesAdapter mDiscoveredTestbedDevicesAdapter;
    private final ScanCallback mScanTestbedDevicesCallback = new ScanCallback() {
        @Override
        public void onScanResult(int callbackType, ScanResult result) {
            super.onScanResult(callbackType, result);
            mDiscoveredTestbedDevicesAdapter.setTestbedDevice(
                    new TestbedDevice(result.getDevice(), result.getRssi()));
            mDiscoveredTestbedDevicesAdapter.notifyDataSetChanged();
        }
    };
    private ConditionsAdapter mConnectingToSelectedTestbedDeviceConditionsAdapter;
    private ConditionsAdapter mConnectingToLastConnectedTestbedDeviceConditionsAdapter;
    private long mMillisUntilFinishedScanning;
    private ACTIVITY_STATE mActivityState = ACTIVITY_STATE.NO_FOUNDED_DEVICES_STATE;
    private int mDiscoveringDeviceIndex = 0;
    private ProgressBar mPgbWorking;
    private TextView mTxvNoDevicesFound;
    private final BroadcastReceiver mDicovoveringTestbedDevicesBroadcastReceiver
            = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            switch (intent.getAction()) {
                case BluetoothLeService.ACTION_GATT_CONNECTED:
                    mBluetoothLeService.discoverServices();
                    break;

                case BluetoothLeService.ACTION_GATT_SERVICES_DISCOVERED:
                    for (BluetoothGattService bluetoothGattService :
                            mBluetoothLeService.getSupportedGattServices()) {
                        for (BluetoothGattCharacteristic bluetoothGattCharacteristic :
                                bluetoothGattService.getCharacteristics()) {
                            if (bluetoothGattCharacteristic.getUuid().equals(
                                    SampleGattAttributes.TESTBED_ID_CHARACTERISTIC_UUID)) {
                                mBluetoothLeService.readCharacteristic(bluetoothGattCharacteristic);
                            }
                        }
                    }
                    break;

                case BluetoothLeService.ACTION_TESTBED_ID_DATA_AVAILABLE:
                    final TestbedDevice discoveringTestbedDevice = mDiscoveredTestbedDevicesAdapter
                            .getTestbedDevice(mDiscoveringDeviceIndex);
                    discoveringTestbedDevice.setDeviceId(intent.getIntExtra(
                            BluetoothLeService.TESTBED_ID_DATA, 0x1FFFF));
                    discoveringTestbedDevice.setAvailableSensors(intent.getIntExtra(
                            BluetoothLeService.AVAILABLE_SENSORS_DATA, 8));
                    discoveringTestbedDevice.deviceWasDiscovered();
                    discoveringTestbedDevice.setStoredState(
                            mTestbedDatabase.getStoredStatusOfTestbedDevice(
                                    discoveringTestbedDevice));
                    mDiscoveredTestbedDevicesAdapter.setTestbedDevice(discoveringTestbedDevice);
                    mDiscoveredTestbedDevicesAdapter.notifyDataSetChanged();
                    mBluetoothLeService.disconnect();
                    break;

                case BluetoothLeService.ACTION_GATT_DISCONNECTED:
                    if (mDiscoveredTestbedDevicesAdapter.getTestbedDevice(mDiscoveringDeviceIndex)
                            .isTestbedDeviceDiscovered()) {
                        mDiscoveringDeviceIndex++;
                    }
                    if (mDiscoveringDeviceIndex < mDiscoveredTestbedDevicesAdapter.getCount() &&
                            mBluetoothLeService != null) {
                        mBluetoothLeService.connect(mDiscoveredTestbedDevicesAdapter
                                .getTestbedDevice(mDiscoveringDeviceIndex), false);
                        invalidateOptionsMenu();
                    } else {
                        discoveringDone();
                    }
                    break;
                default:
                    throw new IllegalStateException("Unexpected value of intent.getAction(): " +
                            intent.getAction());
            }
        }
    };
    private AlertDialog mConnectingToSelectedTestbedDeviceDialog;
    private AlertDialog mConnectingToLastConnectedTestbedDeviceDialog;
    private AlertDialog mBluetoothAdapterDisabledDialog;
    private AlertDialog mLocationDisabledDialog;
    private final BroadcastReceiver mSystemServicesStateChangedBroadcastReceiver =
            new BroadcastReceiver() {
                @SuppressWarnings("SwitchStatementWithTooFewBranches")
                @Override
                public void onReceive(Context context, Intent intent) {
                    switch (intent.getAction()) {
                        case BluetoothAdapter.ACTION_STATE_CHANGED:
                            if (intent.getIntExtra(
                                    BluetoothAdapter.EXTRA_STATE, BluetoothAdapter.ERROR) ==
                                    BluetoothAdapter.STATE_OFF) {
                                switch (mActivityState) {
                                    case SCANNING_STATE:
                                        scanTestbedDevices(SCAN_COMMAND.CANCEL_SCANING);
                                        break;
                                    case DISCOVERING_STATE:
                                        cancelDiscovering();
                                        break;
                                    case DISCOVERING_CANCELED:
                                        break;
                                    default:
                                        throw new IllegalStateException(
                                                "Unexpected value of mActivityState: " + mActivityState);
                                }
                                mBluetoothAdapterDisabledDialog.show();
                            }
                            break;

                        case LocationManager.MODE_CHANGED_ACTION:
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                                LocationManager locationManager =
                                        (LocationManager) Objects.requireNonNull(
                                                getSystemService(Context.LOCATION_SERVICE));
                                if (!locationManager.isLocationEnabled()) {
                                    switch (mActivityState) {
                                        case SCANNING_STATE:
                                            scanTestbedDevices(SCAN_COMMAND.CANCEL_SCANING);
                                            mLocationDisabledDialog.show();
                                            break;
                                        default:
                                            throw new IllegalStateException(
                                                    "Unexpected value mActivityState: " + mActivityState);
                                    }
                                }
                            }
                            break;
                        default:
                            throw new IllegalStateException(
                                    "Unexpected value of intent.getAction(): " + intent.getAction());
                    }
                }
            };
    private TestbedDevice mSelectedTestbedDevice;
    private TestbedDevice mLastConnectedTestbedDevice;

    private boolean mDestroyBackgroundService = true;
    private final BroadcastReceiver mConnectingToLastConnectedTestbedDeviceBroadcastReceiver =
            new BroadcastReceiver() {
                @Override
                public void onReceive(Context context, Intent intent) {
                    switch (intent.getAction()) {
                        case BluetoothLeService.ACTION_GATT_CONNECTED:
                            mConnectingToLastConnectedTestbedDeviceConditionsAdapter.setCondition(
                                    CONNECTING_TO_LAST_CONNECTED_TESTBED_DEVICE_STATUS,
                                    ConditionsAdapter.PASS,
                                    getString(R.string.dialog_connecting_to_last_connected_testbed_device_connected) +
                                            " " + mLastConnectedTestbedDevice.getName() + " (" +
                                            TestbedDevice.formatTestbedDeviceId(mLastConnectedTestbedDevice) + ")");
                            mConnectingToLastConnectedTestbedDeviceConditionsAdapter.setCondition(
                                    LAST_CONNECTED_TESTBED_DEVICE_SERVICE_DISCOVERING_STATUS,
                                    ConditionsAdapter.IN_PROGRESS,
                                    getString(R.string.dialog_connecting_to_last_connected_testbed_device_discovering) +
                                            " " + mLastConnectedTestbedDevice.getName() + " (" +
                                            TestbedDevice.formatTestbedDeviceId(mLastConnectedTestbedDevice) + ") ...");
                            mConnectingToLastConnectedTestbedDeviceConditionsAdapter.notifyDataSetChanged();
                            mBluetoothLeService.discoverServices();
                            break;

                        case BluetoothLeService.ACTION_GATT_SERVICES_DISCOVERED:
                            for (BluetoothGattService bluetoothGattService :
                                    mBluetoothLeService.getSupportedGattServices()) {
                                if (bluetoothGattService.getUuid().equals(
                                        SampleGattAttributes.TESTBED_SERVICE_UUID)) {
                                    for (BluetoothGattCharacteristic bluetoothGattCharacteristic :
                                            bluetoothGattService.getCharacteristics()) {
                                        if (bluetoothGattCharacteristic.getUuid().equals(
                                                SampleGattAttributes.TESTBED_ID_CHARACTERISTIC_UUID)) {
                                            mBluetoothLeService.readCharacteristic(
                                                    bluetoothGattCharacteristic);
                                        }
                                    }
                                }
                            }
                            mConnectingToLastConnectedTestbedDeviceConditionsAdapter.setCondition(
                                    LAST_CONNECTED_TESTBED_DEVICE_SERVICE_DISCOVERING_STATUS,
                                    ConditionsAdapter.PASS,
                                    getString(R.string.dialog_connecting_to_last_connected_testbed_device_discovered_1_2) +
                                            " " + mLastConnectedTestbedDevice.getName() + " (" +
                                            TestbedDevice.formatTestbedDeviceId(mLastConnectedTestbedDevice) + ") " +
                                            getString(R.string.dialog_connecting_to_last_connected_testbed_device_discovered_2_2));
                            mConnectingToLastConnectedTestbedDeviceConditionsAdapter.setCondition(
                                    LAST_CONNECTED_TESTBED_DEVICE_STORED_STATUS_CHECKING_STATUS,
                                    ConditionsAdapter.IN_PROGRESS,
                                    getString(R.string.dialog_connecting_to_last_connected_testbed_device_verification_in_progress) +
                                            " " + mLastConnectedTestbedDevice.getName() + " (" +
                                            TestbedDevice.formatTestbedDeviceId(mLastConnectedTestbedDevice) + ") ...");
                            mConnectingToLastConnectedTestbedDeviceConditionsAdapter.notifyDataSetChanged();
                            break;

                        case BluetoothLeService.ACTION_TESTBED_ID_DATA_AVAILABLE:
                            if (intent.getIntExtra(BluetoothLeService.TESTBED_ID_DATA, 0x1FFFF) ==
                                    mLastConnectedTestbedDevice.getDeviceId() &&
                                    intent.getIntExtra(BluetoothLeService.AVAILABLE_SENSORS_DATA, 8) ==
                                            mLastConnectedTestbedDevice.getAvailableSensors()) {
                                mConnectingToLastConnectedTestbedDeviceConditionsAdapter.setCondition(
                                        LAST_CONNECTED_TESTBED_DEVICE_STORED_STATUS_CHECKING_STATUS,
                                        ConditionsAdapter.PASS,
                                        getString(R.string.dialog_connecting_to_last_connected_testbed_device_verification_success_1_2) +
                                                " " + mLastConnectedTestbedDevice.getName() + " (" +
                                                TestbedDevice.formatTestbedDeviceId(mLastConnectedTestbedDevice) + ") " +
                                                getString(R.string.dialog_connecting_to_last_connected_testbed_device_verification_success_2_2));
                                mConnectingToLastConnectedTestbedDeviceConditionsAdapter.setCondition(
                                        LAST_CONNECTED_TESTBED_DEVICE_NOTIFICATION_ENABLED_STATUS,
                                        ConditionsAdapter.IN_PROGRESS,
                                        getString(R.string.dialog_connecting_to_last_connected_testbed_device_notification_prepare) +
                                                " (" + mBluetoothLeService.mCharacteristicsForDescriptorWrite.size() + ") ...");
                                mBluetoothLeService.startWritingCharacteristicNotification();
                                mBluetoothLeService.setAutoReconnect(true);
                            } else {
                                mConnectingToLastConnectedTestbedDeviceConditionsAdapter.setCondition(
                                        LAST_CONNECTED_TESTBED_DEVICE_STORED_STATUS_CHECKING_STATUS,
                                        ConditionsAdapter.FAIL,
                                        getString(R.string.dialog_connecting_to_last_connected_testbed_device_verification_fail_1_2) +
                                                " " + mLastConnectedTestbedDevice.getName() + " (" + TestbedDevice.formatTestbedDeviceId(mLastConnectedTestbedDevice) + ") " +
                                                getString(R.string.dialog_connecting_to_last_connected_testbed_device_verification_fail_2_2));
                                unregisterReceiver(this);
                                mBluetoothLeService.disconnect();
                                new Handler().postDelayed(() ->
                                        mConnectingToLastConnectedTestbedDeviceDialog.hide(), SPLASH_TIME_HIDE_DIALOG_MS);
                            }
                            mConnectingToLastConnectedTestbedDeviceConditionsAdapter.notifyDataSetChanged();
                            break;

                        case BluetoothLeService.ACTION_GATT_DESCRIPTOR_WRITTEN:
                            mConnectingToLastConnectedTestbedDeviceConditionsAdapter.setCondition(
                                    LAST_CONNECTED_TESTBED_DEVICE_NOTIFICATION_ENABLED_STATUS,
                                    ConditionsAdapter.IN_PROGRESS,
                                    getString(R.string.dialog_connecting_to_last_connected_testbed_device_notificationr_writing) +
                                            " (" + mBluetoothLeService.mDescriptorWriteIndex + " " +
                                            getString(R.string.dialog_connecting_to_last_connected_testbed_device_notification_writing_conjunction) +
                                            " " + mBluetoothLeService.mCharacteristicsForDescriptorWrite.size() + ")");
                            mConnectingToLastConnectedTestbedDeviceConditionsAdapter.notifyDataSetChanged();
                            break;

                        case BluetoothLeService.ACTION_GATT_ALL_DESCRIPTORS_WRITTEN:
                            mConnectingToLastConnectedTestbedDeviceConditionsAdapter.setCondition(
                                    LAST_CONNECTED_TESTBED_DEVICE_NOTIFICATION_ENABLED_STATUS,
                                    ConditionsAdapter.PASS,
                                    getString(R.string.dialog_connecting_to_last_connected_testbed_device_notification_written));
                            mConnectingToLastConnectedTestbedDeviceConditionsAdapter.notifyDataSetChanged();
                            unregisterReceiver(this);
                            mDestroyBackgroundService = false;
                            new Handler().postDelayed(() -> {
                                mConnectingToLastConnectedTestbedDeviceDialog.hide();
                                mConnectingToLastConnectedTestbedDeviceDialog.cancel();
                                Intent intentActivity = new Intent(DiscoverDevicesActivity.this, DatabaseActivity.class);
                                intentActivity.putExtra(TestbedDevice.TESTBED_DEVICE, mLastConnectedTestbedDevice);
                                startActivity(intentActivity);
                                finish();
                            }, SPLASH_TIME_START_ACTIVITY_MS);
                            break;

                        case BluetoothLeService.ACTION_GATT_DISCONNECTED:
                            mConnectingToLastConnectedTestbedDeviceConditionsAdapter.setCondition(
                                    CONNECTING_TO_LAST_CONNECTED_TESTBED_DEVICE_STATUS,
                                    ConditionsAdapter.IN_PROGRESS,
                                    getString(R.string.dialog_connecting_to_last_connected_testbed_device_connecting) +
                                            " " + mLastConnectedTestbedDevice.getName() + " (" +
                                            TestbedDevice.formatTestbedDeviceId(mLastConnectedTestbedDevice) + ") ...");
                            mConnectingToLastConnectedTestbedDeviceConditionsAdapter.setCondition(
                                    LAST_CONNECTED_TESTBED_DEVICE_SERVICE_DISCOVERING_STATUS,
                                    ConditionsAdapter.FAIL,
                                    getString(R.string.dialog_connecting_to_last_connected_testbed_device_not_discovered_1_2) +
                                            " " + mLastConnectedTestbedDevice.getName() + " (" +
                                            TestbedDevice.formatTestbedDeviceId(mLastConnectedTestbedDevice) + ") " +
                                            getString(R.string.dialog_connecting_to_last_connected_testbed_device_not_discovered_2_2));
                            mConnectingToLastConnectedTestbedDeviceConditionsAdapter.setCondition(
                                    LAST_CONNECTED_TESTBED_DEVICE_STORED_STATUS_CHECKING_STATUS,
                                    ConditionsAdapter.FAIL, getString(R.string.dialog_connecting_to_last_connected_testbed_device_verification_unfinished_1_2) +
                                            " " + mLastConnectedTestbedDevice.getName() + " (" + TestbedDevice.formatTestbedDeviceId(mLastConnectedTestbedDevice) + ") " +
                                            getString(R.string.dialog_connecting_to_last_connected_testbed_device_verification_unfinished_2_2));
                            mConnectingToLastConnectedTestbedDeviceConditionsAdapter.setCondition(
                                    LAST_CONNECTED_TESTBED_DEVICE_NOTIFICATION_ENABLED_STATUS,
                                    ConditionsAdapter.FAIL,
                                    getString(R.string.dialog_connecting_to_last_connected_testbed_device_notification_fail));
                            mConnectingToLastConnectedTestbedDeviceConditionsAdapter.notifyDataSetChanged();
                            mBluetoothLeService.connect(mLastConnectedTestbedDevice, true);
                            break;

                        default:
                            throw new IllegalStateException("Unexpected value of intent.getAction(): " + intent.getAction());
                    }
                }
            };
    private final CountDownTimer mScanningTestbedDevicesCountDownTimer =
            new CountDownTimer(SCAN_PERIOD_MS, 1000) {

                @Override
                public void onTick(long millis) {
                    mMillisUntilFinishedScanning = millis;
                    invalidateOptionsMenu();
                }

                @Override
                public void onFinish() {
                    scanTestbedDevices(SCAN_COMMAND.STOP_SCANING);
                }
            };
    private final BroadcastReceiver mConnectingToSelectedTestbedDeviceBroadcastReceiver =
            new BroadcastReceiver() {
                @Override
                public void onReceive(Context context, Intent intent) {
                    switch (intent.getAction()) {
                        case BluetoothLeService.ACTION_GATT_CONNECTED:
                            mTestbedDatabase.updateLastConnectedTestbedDevice(mSelectedTestbedDevice);
                            mConnectingToSelectedTestbedDeviceConditionsAdapter.setCondition(
                                    CONNECTING_TO_SELCTED_TESTBED_DEVICE_STATUS,
                                    ConditionsAdapter.PASS,
                                    getString(R.string.dialog_connecting_to_slected_testbed_device_connected) +
                                            " " + mSelectedTestbedDevice.getName() + " (" +
                                            TestbedDevice.formatTestbedDeviceId(mSelectedTestbedDevice) + ")");
                            mConnectingToSelectedTestbedDeviceConditionsAdapter.setCondition(
                                    SELECTED_TESTBED_DEVICE_SERVICE_DISCOVERING_STATUS,
                                    ConditionsAdapter.IN_PROGRESS,
                                    getString(R.string.dialog_connecting_to_selected_testbed_device_discovering) +
                                            " " + mSelectedTestbedDevice.getName() + " (" +
                                            TestbedDevice.formatTestbedDeviceId(mSelectedTestbedDevice) + ")");
                            mConnectingToSelectedTestbedDeviceConditionsAdapter.notifyDataSetChanged();
                            break;

                        case BluetoothLeService.ACTION_GATT_SERVICES_DISCOVERED:
                            mConnectingToSelectedTestbedDeviceConditionsAdapter.setCondition(
                                    SELECTED_TESTBED_DEVICE_SERVICE_DISCOVERING_STATUS,
                                    ConditionsAdapter.PASS,
                                    getString(R.string.dialog_connecting_to_selected_testbed_device_discovered_1_2) +
                                            " " + mSelectedTestbedDevice.getName() + " (" +
                                            TestbedDevice.formatTestbedDeviceId(mSelectedTestbedDevice) + ") " +
                                            getString(R.string.dialog_connecting_to_selected_testbed_device_discovered_2_2));
                            mConnectingToSelectedTestbedDeviceConditionsAdapter.setCondition(
                                    SELECTED_TESTBED_DEVICE_NOTIFICATION_ENABLED_STATUS,
                                    ConditionsAdapter.IN_PROGRESS,
                                    getString(R.string.dialog_connecting_to_selected_testbed_device_notification_prepare) +
                                            " (" + mBluetoothLeService.mCharacteristicsForDescriptorWrite.size() + ") ...");
                            mConnectingToSelectedTestbedDeviceConditionsAdapter.notifyDataSetChanged();
                            break;

                        case BluetoothLeService.ACTION_GATT_DESCRIPTOR_WRITTEN:
                            mConnectingToSelectedTestbedDeviceConditionsAdapter.setCondition(
                                    SELECTED_TESTBED_DEVICE_NOTIFICATION_ENABLED_STATUS,
                                    ConditionsAdapter.IN_PROGRESS,
                                    getString(R.string.dialog_connecting_to_selected_testbed_device_notificationr_writing) +
                                            " (" + mBluetoothLeService.mDescriptorWriteIndex + " " +
                                            getString(R.string.dialog_connecting_to_selected_testbed_device_notification_writing_conjunction) +
                                            " " + mBluetoothLeService.mCharacteristicsForDescriptorWrite.size() + ")");
                            mConnectingToSelectedTestbedDeviceConditionsAdapter.notifyDataSetChanged();
                            break;

                        case BluetoothLeService.ACTION_GATT_ALL_DESCRIPTORS_WRITTEN:
                            mConnectingToSelectedTestbedDeviceConditionsAdapter.setCondition(
                                    SELECTED_TESTBED_DEVICE_NOTIFICATION_ENABLED_STATUS,
                                    ConditionsAdapter.PASS,
                                    getString(R.string.dialog_connecting_to_selected_testbed_device_notification_written));
                            mConnectingToSelectedTestbedDeviceConditionsAdapter.notifyDataSetChanged();
                            unregisterReceiver(this);
                            mDestroyBackgroundService = false;
                            new Handler().postDelayed(() -> {
                                mConnectingToSelectedTestbedDeviceDialog.hide();
                                mConnectingToSelectedTestbedDeviceDialog.cancel();
                                Intent intentActivity = new Intent(DiscoverDevicesActivity.this, DatabaseActivity.class);
                                intentActivity.putExtra(TestbedDevice.TESTBED_DEVICE, mSelectedTestbedDevice);
                                startActivity(intentActivity);
                                finish();
                            }, SPLASH_TIME_START_ACTIVITY_MS);
                            break;

                        case BluetoothLeService.ACTION_GATT_DISCONNECTED:
                            mConnectingToSelectedTestbedDeviceConditionsAdapter.setCondition(
                                    CONNECTING_TO_SELCTED_TESTBED_DEVICE_STATUS,
                                    ConditionsAdapter.IN_PROGRESS,
                                    getString(R.string.dialog_connecting_to_selected_testbed_device_connecting) +
                                            " " + mSelectedTestbedDevice.getName() + " (" +
                                            TestbedDevice.formatTestbedDeviceId(mSelectedTestbedDevice) + ") ...");
                            mConnectingToSelectedTestbedDeviceConditionsAdapter.setCondition(
                                    SELECTED_TESTBED_DEVICE_SERVICE_DISCOVERING_STATUS,
                                    ConditionsAdapter.FAIL,
                                    getString(R.string.dialog_connecting_to_selected_testbed_device_not_discovered_1_2) +
                                            " " + mSelectedTestbedDevice.getName() + " (" +
                                            TestbedDevice.formatTestbedDeviceId(mSelectedTestbedDevice) + ") " +
                                            getString(R.string.dialog_connecting_to_selected_testbed_device_not_discovered_2_2));
                            mConnectingToSelectedTestbedDeviceConditionsAdapter.setCondition(
                                    SELECTED_TESTBED_DEVICE_NOTIFICATION_ENABLED_STATUS,
                                    ConditionsAdapter.FAIL,
                                    getString(R.string.dialog_connecting_to_selected_testbed_device_notification_fail));
                            mConnectingToSelectedTestbedDeviceConditionsAdapter.notifyDataSetChanged();
                            mBluetoothLeService.connect(mSelectedTestbedDevice, true);
                            break;
                    }
                }
            };
    private final ActivityResultLauncher<Intent> mOnBluetoothAdapterEnabledActivityResult
            = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(),
            new ActivityResultCallback<ActivityResult>() {
                @Override
                public void onActivityResult(ActivityResult result) {
                    if (result.getResultCode() == Activity.RESULT_OK) {
                        mBluetoothAdapterDisabledDialog.hide();
                        mBluetoothAdapterDisabledDialog.cancel();
                        if (mActivityState == ACTIVITY_STATE.DISCOVERED_STATE) {
                            mBluetoothLeService.initialize();
                            connectToSelectedTestbedDevice(mSelectedTestbedDevice);
                        } else {
                            mBluetoothAdapter = Objects.requireNonNull((BluetoothManager)
                                    getSystemService(Context.BLUETOOTH_SERVICE)).getAdapter();
                            mBluetoothLeScanner = mBluetoothAdapter.getBluetoothLeScanner();
                            scanTestbedDevices(SCAN_COMMAND.START_SCANING);
                        }
                    } else {
                        mBluetoothAdapterDisabledDialog.show();
                    }
                }
            });
    private boolean mFirstActivityRun;
    private boolean mScaningForLastConnectedTestbedDevice = false;
    private final ScanCallback mScanLastConnectedTestbedDeviceCallback = new ScanCallback() {
        @Override
        public void onScanResult(int callbackType, ScanResult result) {
            super.onScanResult(callbackType, result);
            if (mBluetoothLeService != null) {
                mBluetoothLeScanner.stopScan(this);
                mScanningLastConnectedTestbedDeviceCountDownTimer.cancel();
                registerReceiver(mConnectingToLastConnectedTestbedDeviceBroadcastReceiver,
                        GATT_ACTION_UPDATE_INTENT_FILTER(INTENT_FILTER_TYPES.FOR_FIRST_CONNECTING));
                mBluetoothLeService.connect(mLastConnectedTestbedDevice, true);
                mConnectingToLastConnectedTestbedDeviceConditionsAdapter.setCondition(
                        SCANNING_FOR_LAST_CONNECTED_TESTBED_DEVICE_STATUS,
                        ConditionsAdapter.PASS,
                        getString(R.string.dialog_connecting_to_last_connected_testbed_device_found_1_2) +
                                " " + mLastConnectedTestbedDevice.getName() + " (" +
                                TestbedDevice.formatTestbedDeviceId(mLastConnectedTestbedDevice) +
                                " " + getString(R.string.dialog_connecting_to_last_connected_testbed_device_found_2_2) +
                                " RSSI: " + result.getRssi() + " dBm");
                mConnectingToLastConnectedTestbedDeviceConditionsAdapter.setCondition(
                        CONNECTING_TO_LAST_CONNECTED_TESTBED_DEVICE_STATUS,
                        ConditionsAdapter.IN_PROGRESS,
                        getString(R.string.dialog_connecting_to_last_connected_testbed_device_connecting) +
                                " " + mLastConnectedTestbedDevice.getName() + " (" +
                                TestbedDevice.formatTestbedDeviceId(mLastConnectedTestbedDevice) + ") ...");
                mConnectingToLastConnectedTestbedDeviceConditionsAdapter.notifyDataSetChanged();
                mScaningForLastConnectedTestbedDevice = false;
            }
        }
    };
    private final CountDownTimer mScanningLastConnectedTestbedDeviceCountDownTimer =
            new CountDownTimer(SCAN_PERIOD_MS, 1000) {
                @Override
                public void onTick(long millisUntilFinished) {
                    mConnectingToLastConnectedTestbedDeviceConditionsAdapter.setCondition(
                            SCANNING_FOR_LAST_CONNECTED_TESTBED_DEVICE_STATUS,
                            ConditionsAdapter.IN_PROGRESS,
                            getString(R.string.dialog_connecting_to_last_connected_testbed_device_scanning) +
                                    " " + mLastConnectedTestbedDevice.getName() + " (" +
                                    TestbedDevice.formatTestbedDeviceId(mLastConnectedTestbedDevice) + ") (" +
                                    ((millisUntilFinished / 1000) + 1) + ") ...");
                    mConnectingToLastConnectedTestbedDeviceConditionsAdapter.notifyDataSetChanged();
                }

                @Override
                public void onFinish() {
                    mBluetoothLeScanner.stopScan(mScanLastConnectedTestbedDeviceCallback);
                    mConnectingToLastConnectedTestbedDeviceConditionsAdapter.setCondition(
                            SCANNING_FOR_LAST_CONNECTED_TESTBED_DEVICE_STATUS,
                            ConditionsAdapter.FAIL,
                            getString(R.string.dialog_connecting_to_last_connected_testbed_device_not_found_1_2) +
                                    " " + mLastConnectedTestbedDevice.getName() + " (" +
                                    TestbedDevice.formatTestbedDeviceId(mLastConnectedTestbedDevice) + ") " +
                                    getString(R.string.dialog_connecting_to_last_connected_testbed_device_not_found_2_2));
                    mConnectingToLastConnectedTestbedDeviceConditionsAdapter.notifyDataSetChanged();
                }
            };

    private static IntentFilter GATT_ACTION_UPDATE_INTENT_FILTER(
            INTENT_FILTER_TYPES intent_filter_types) {
        final IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(BluetoothLeService.ACTION_GATT_CONNECTED);
        intentFilter.addAction(BluetoothLeService.ACTION_GATT_SERVICES_DISCOVERED);
        switch (intent_filter_types) {
            case FOR_FIRST_CONNECTING:
                intentFilter.addAction(BluetoothLeService.ACTION_TESTBED_ID_DATA_AVAILABLE);
                intentFilter.addAction(BluetoothLeService.ACTION_GATT_DESCRIPTOR_WRITTEN);
                intentFilter.addAction(BluetoothLeService.ACTION_GATT_ALL_DESCRIPTORS_WRITTEN);
                break;

            case FOR_DISCOVERING:
                intentFilter.addAction(BluetoothLeService.ACTION_TESTBED_ID_DATA_AVAILABLE);
                break;

            case FOR_CONNECTING:
                intentFilter.addAction(BluetoothLeService.ACTION_GATT_DESCRIPTOR_WRITTEN);
                intentFilter.addAction(BluetoothLeService.ACTION_GATT_ALL_DESCRIPTORS_WRITTEN);
                break;

            default:
                throw new IllegalStateException("Unexpected value of intent_filter_types: "
                        + intent_filter_types);
        }
        intentFilter.addAction(BluetoothLeService.ACTION_GATT_DISCONNECTED);
        return intentFilter;
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_discovery_activity, menu);
        return true;
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        MenuItem menuItem = menu.findItem(R.id.scaning_state);
        switch (mActivityState) {
            case NO_FOUNDED_DEVICES_STATE:
            case DISCOVERED_STATE:
                menuItem.setTitle(getString(R.string.menu_scan_devices_again));
                break;

            case SCANNING_STATE:
                menuItem.setTitle(getString(R.string.menu_scaning_devices) + " (" +
                        ((mMillisUntilFinishedScanning / 1000) + 1) + ")");
                break;

            case DISCOVERING_STATE:
                menuItem.setTitle(getString(R.string.menu_discovering_devices) +
                        " (" + (mDiscoveringDeviceIndex + 1) + " " +
                        getString(R.string.menu_discovering_devices_conjuction) + " " +
                        mDiscoveredTestbedDevicesAdapter.getCount() + ")");
                break;

            case DISCOVERING_CANCELED:
                menuItem.setTitle(getString(R.string.menu_canceling_discovering_devices));
                break;
        }
        return super.onPrepareOptionsMenu(menu);
    }

    @SuppressLint("NonConstantResourceId")
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.scaning_state:
                switch (mActivityState) {
                    case NO_FOUNDED_DEVICES_STATE:
                    case DISCOVERED_STATE:
                        scanTestbedDevices(SCAN_COMMAND.START_SCANING);
                        break;

                    case SCANNING_STATE:
                        scanTestbedDevices(SCAN_COMMAND.STOP_SCANING);
                        break;

                    case DISCOVERING_STATE:
                        cancelDiscovering();
                        break;
                }
                return true;

            case R.id.about_application:
                final AlertDialog aboutApplicationDialog = new AlertDialog.Builder(this)
                        .setIcon(AppCompatResources.getDrawable(this,
                                R.drawable.ic_information_20dp_color_vsb))
                        .setTitle(getString(R.string.dialog_about_application_title))
                        .setMessage(getString(R.string.dialog_about_application_message))
                        .setNeutralButton(getString(R.string.dialog_about_application_neutral_button),
                                null)
                        .create();
                aboutApplicationDialog.setCancelable(false);
                aboutApplicationDialog.setCanceledOnTouchOutside(false);
                aboutApplicationDialog.show();
                return true;

            case R.id.close_application:
                finish();
                return true;

            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_discover_devices);
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_NOSENSOR);

        mFirstActivityRun = getIntent().getExtras().getBoolean(FIRST_RUN_APPLICATION,
                false);
        mTestbedDatabase = new TestbedDatabase(this);
        mBluetoothAdapter = Objects.requireNonNull(
                (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE)).getAdapter();
        mBluetoothLeScanner = mBluetoothAdapter.getBluetoothLeScanner();

        ListView lsvDiscoveredTestbedDevices = findViewById(R.id.activity_discover_lsv_devices);
        mDiscoveredTestbedDevicesAdapter =
                new TestbedDevicesAdapter(this.getLayoutInflater(), getApplicationContext());
        lsvDiscoveredTestbedDevices.setAdapter(mDiscoveredTestbedDevicesAdapter);

        lsvDiscoveredTestbedDevices.setOnItemClickListener((parent, view, position, id) -> {
            if (mActivityState == ACTIVITY_STATE.DISCOVERED_STATE) {
                mSelectedTestbedDevice = mDiscoveredTestbedDevicesAdapter.getTestbedDevice(position);
                if (mSelectedTestbedDevice.getStoredState() == TestbedDevice.STORED_BUT_MODIFIED) {
                    AlertDialog deviceIsNotInitilaziedProperlyDialog = new AlertDialog.Builder(this)
                            .setIcon(AppCompatResources.getDrawable(this,
                                    R.drawable.ic_database_20dp_color_red))
                            .setTitle(getString(R.string.dialog_device_is_not_initilazied_properly_title))
                            .setMessage(getString(R.string.dialog_device_is_not_initilazied_properly_message))
                            .setNeutralButton(
                                    getString(R.string.dialog_device_is_not_initilazied_properly_neutral_button),
                                    (dialog, which) -> {
                                        mDiscoveredTestbedDevicesAdapter.clear();
                                        mDiscoveredTestbedDevicesAdapter.notifyDataSetChanged();
                                        mTxvNoDevicesFound.setVisibility(View.VISIBLE);
                                    })
                            .create();
                    deviceIsNotInitilaziedProperlyDialog.show();
                } else {
                    connectToSelectedTestbedDevice(mSelectedTestbedDevice);
                }
            }
        });

        mPgbWorking = findViewById(R.id.activity_discover_devices_pgb_working);
        mPgbWorking.setVisibility(ProgressBar.INVISIBLE);

        mTxvNoDevicesFound = findViewById(R.id.activity_discover_devices_txv_no_devices_found);
        mTxvNoDevicesFound.setVisibility(View.INVISIBLE);


        View connectingToLastConnectedTestbedDeviceDialoView = getLayoutInflater().
                inflate(R.layout.dialog_general_list_view, null);
        final ListView lswConnectingToLastConnectedTestbedDeviceConditions =
                connectingToLastConnectedTestbedDeviceDialoView.
                        findViewById(R.id.dialog_list_view_lsv);
        mConnectingToLastConnectedTestbedDeviceConditionsAdapter =
                new ConditionsAdapter(getLayoutInflater(), getApplicationContext(),
                        LAST_CONNECTED_TESTBED_DEVICE_STATUS_COUNT);
        lswConnectingToLastConnectedTestbedDeviceConditions.
                setAdapter(mConnectingToLastConnectedTestbedDeviceConditionsAdapter);
        mConnectingToLastConnectedTestbedDeviceDialog = new AlertDialog.Builder(this)
                .setIcon(AppCompatResources.getDrawable(this,
                        R.drawable.ic_connecting_20dp_color_vsb))
                .setTitle(getString(R.string.dialog_connecting_to_last_connected_testbed_device_title))
                .setView(connectingToLastConnectedTestbedDeviceDialoView)
                .setNegativeButton(
                        getString(R.string.dialog_connecting_to_last_connected_testbed_device_negative_button),
                        (dialog, which) -> {
                            if (mScaningForLastConnectedTestbedDevice) {
                                mBluetoothLeScanner.stopScan(mScanLastConnectedTestbedDeviceCallback);
                            } else {
                                mBluetoothLeService.setAutoReconnect(false);
                                mBluetoothLeService.removeTestbedDevice();
                                unregisterReceiver(mConnectingToLastConnectedTestbedDeviceBroadcastReceiver);
                                mBluetoothLeService.disconnect();
                                mTxvNoDevicesFound.setVisibility(View.VISIBLE);
                            }
                            scanTestbedDevices(SCAN_COMMAND.START_SCANING);
                        })
                .create();
        mConnectingToLastConnectedTestbedDeviceDialog.setCancelable(false);
        mConnectingToLastConnectedTestbedDeviceDialog.setCanceledOnTouchOutside(false);

        View connectingToSelectedTestbedDeviceDialoView =
                getLayoutInflater().inflate(R.layout.dialog_general_list_view, null);
        final ListView lswConnectingToSelectedTestbedDeviceConditions =
                connectingToSelectedTestbedDeviceDialoView.findViewById(R.id.dialog_list_view_lsv);
        mConnectingToSelectedTestbedDeviceConditionsAdapter =
                new ConditionsAdapter(getLayoutInflater(), getApplicationContext(),
                        SELECTED_TESTBED_DEVICE_STATUS_COUNT);
        lswConnectingToSelectedTestbedDeviceConditions.
                setAdapter(mConnectingToSelectedTestbedDeviceConditionsAdapter);
        mConnectingToSelectedTestbedDeviceDialog = new AlertDialog.Builder(this)
                .setView(connectingToSelectedTestbedDeviceDialoView)
                .setIcon(AppCompatResources.getDrawable(this,
                        R.drawable.ic_connecting_20dp_color_vsb))
                .setTitle(getString(R.string.dialog_connecting_to_selected_testbed_device_title))
                .setNegativeButton(
                        getString(R.string.dialog_connecting_to_selected_testbed_device_negative_button),
                        (dialog, which) -> {
                            mBluetoothLeService.setAutoReconnect(false);
                            mBluetoothLeService.removeTestbedDevice();
                            mBluetoothLeService.disconnect();
                            unregisterReceiver(mConnectingToSelectedTestbedDeviceBroadcastReceiver);
                            mConnectingToSelectedTestbedDeviceConditionsAdapter =
                                    new ConditionsAdapter(getLayoutInflater(), getApplicationContext(),
                                            SELECTED_TESTBED_DEVICE_STATUS_COUNT);
                            lswConnectingToSelectedTestbedDeviceConditions
                                    .setAdapter(mConnectingToSelectedTestbedDeviceConditionsAdapter);
                            mDiscoveredTestbedDevicesAdapter.clear();
                            mDiscoveredTestbedDevicesAdapter.notifyDataSetChanged();
                            mTxvNoDevicesFound.setVisibility(View.VISIBLE);
                        })
                .create();
        mConnectingToSelectedTestbedDeviceDialog.setCancelable(false);
        mConnectingToSelectedTestbedDeviceDialog.setCanceledOnTouchOutside(false);

        mBluetoothAdapterDisabledDialog = new AlertDialog.Builder(this)
                .setIcon(R.drawable.ic_bluetooth_20dp_color_red)
                .setTitle(R.string.dialog_bluetooth_adapter_disabled_title)
                .setMessage(R.string.dialog_bluetooth_adapter_disabled_message)
                .setPositiveButton(R.string.dialog_bluetooth_adapter_disabled_positive_button,
                        (dialog, which) -> mOnBluetoothAdapterEnabledActivityResult.launch(
                                new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)))
                .setNegativeButton(R.string.dialog_bluetooth_adapter_disabled_negative_button,
                        (dialog, which) -> finish())
                .create();
        mBluetoothAdapterDisabledDialog.setCancelable(false);
        mBluetoothAdapterDisabledDialog.setCanceledOnTouchOutside(false);

        mLocationDisabledDialog = new AlertDialog.Builder(this)
                .setIcon(R.drawable.ic_location_20dp_color_red)
                .setTitle(R.string.dialog_location_disabled_title)
                .setMessage(R.string.dialog_location_disabled_message)
                .setPositiveButton(R.string.dialog_location_disabled_positive_button,
                        (dialog, which) ->
                                startActivity(new Intent(
                                        android.provider.Settings.ACTION_LOCATION_SOURCE_SETTINGS)))
                .setNegativeButton(R.string.dialog_location_disabled_negative_button,
                        (dialog, which) -> finish())
                .create();
        mLocationDisabledDialog.setCancelable(false);
        mLocationDisabledDialog.setCanceledOnTouchOutside(false);
    }

    @Override
    protected void onResume() {
        super.onResume();
        bindService(new Intent(this, BluetoothLeService.class),
                mServiceConnection, BIND_AUTO_CREATE);
        registerReceiver(mSystemServicesStateChangedBroadcastReceiver,
                SYSTEM_SERVICES_INTENT_FILTER());
        if (mFirstActivityRun) {
            scanLastConnectedTestbedDevice();
            mFirstActivityRun = false;
        } else {
            scanTestbedDevices(SCAN_COMMAND.START_SCANING);
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        scanTestbedDevices(SCAN_COMMAND.CANCEL_SCANING);
        unregisterReceiver(mSystemServicesStateChangedBroadcastReceiver);
        unbindService(mServiceConnection);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mBluetoothLeService = null;
        if (mDestroyBackgroundService) {
            stopService(new Intent(this, BluetoothLeService.class));
        }
    }

    private void scanLastConnectedTestbedDevice() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            LocationManager locationManager =
                    (LocationManager) Objects.requireNonNull(getSystemService(Context.LOCATION_SERVICE));
            if (!locationManager.isLocationEnabled() && mBluetoothAdapter.isEnabled()) {
                mLocationDisabledDialog.show();
                return;
            }
        }
        if (mBluetoothAdapter.isEnabled()) {
            try {
                mLastConnectedTestbedDevice = mTestbedDatabase.selectLastConnectedTestbedDevice();
                ScanFilter scanFilter = new ScanFilter.Builder()
                        .setDeviceName(mLastConnectedTestbedDevice.getName())
                        .setDeviceAddress(mLastConnectedTestbedDevice.getMacAddress())
                        .build();
                mBluetoothLeScanner.startScan(Collections.singletonList(scanFilter),
                        new ScanSettings.Builder().build(), mScanLastConnectedTestbedDeviceCallback);
                mScanningLastConnectedTestbedDeviceCountDownTimer.start();
                mScaningForLastConnectedTestbedDevice = true;
                mConnectingToLastConnectedTestbedDeviceDialog.show();
            } catch (TestbedDatabase.EmptyCursorException e) {
                scanTestbedDevices(SCAN_COMMAND.START_SCANING);
            }
        } else {
            mBluetoothAdapterDisabledDialog.show();
        }
    }

    private void scanTestbedDevices(SCAN_COMMAND scanCommand) {
        if (scanCommand == SCAN_COMMAND.START_SCANING) {
            mActivityState = ACTIVITY_STATE.SCANNING_STATE;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                LocationManager locationManager =
                        (LocationManager) Objects.requireNonNull(getSystemService(Context.LOCATION_SERVICE));
                if (!locationManager.isLocationEnabled() && mBluetoothAdapter.isEnabled()) {
                    mLocationDisabledDialog.show();
                    return;
                }
            }
            if (mBluetoothAdapter.isEnabled()) {
                invalidateOptionsMenu();
                mPgbWorking.setVisibility(ProgressBar.VISIBLE);
                mTxvNoDevicesFound.setVisibility(View.INVISIBLE);
                mDiscoveredTestbedDevicesAdapter.clear();
                mDiscoveredTestbedDevicesAdapter.notifyDataSetChanged();
                List<ScanFilter> scanFilters = new ArrayList<>();
                ScanFilter scanFilter = new ScanFilter.Builder()
                        .setDeviceName(DEVICE_NAME_FILTER)
                        .build();
                scanFilters.add(scanFilter);
                ScanSettings scanSettings = new ScanSettings.Builder().build();
                mBluetoothLeScanner.startScan(scanFilters, scanSettings, mScanTestbedDevicesCallback);
                mScanningTestbedDevicesCountDownTimer.start();
            } else {
                mBluetoothAdapterDisabledDialog.show();
            }
        } else if (scanCommand == SCAN_COMMAND.STOP_SCANING) {
            mBluetoothLeScanner.stopScan(mScanTestbedDevicesCallback);
            mScanningTestbedDevicesCountDownTimer.cancel();
            if (mDiscoveredTestbedDevicesAdapter.getCount() > 0 && mBluetoothLeService != null) {
                //noinspection CatchMayIgnoreException
                try {
                    unregisterReceiver(mConnectingToLastConnectedTestbedDeviceBroadcastReceiver);
                } catch (IllegalArgumentException e) {

                } finally {
                    registerReceiver(mDicovoveringTestbedDevicesBroadcastReceiver,
                            GATT_ACTION_UPDATE_INTENT_FILTER(INTENT_FILTER_TYPES.FOR_DISCOVERING));
                    mDiscoveringDeviceIndex = 0;
                    mBluetoothLeService.connect(mDiscoveredTestbedDevicesAdapter
                            .getTestbedDevice(mDiscoveringDeviceIndex), false);
                    mActivityState = ACTIVITY_STATE.DISCOVERING_STATE;
                }
            } else {
                mActivityState = ACTIVITY_STATE.NO_FOUNDED_DEVICES_STATE;
                mPgbWorking.setVisibility(ProgressBar.INVISIBLE);
                mTxvNoDevicesFound.setVisibility(View.VISIBLE);
            }
            invalidateOptionsMenu();

        } else {
            if (mBluetoothAdapter.isEnabled()) {
                mBluetoothLeScanner.stopScan(mScanTestbedDevicesCallback);
            }
            mDiscoveredTestbedDevicesAdapter.clear();
            mDiscoveredTestbedDevicesAdapter.notifyDataSetChanged();
            mActivityState = ACTIVITY_STATE.NO_FOUNDED_DEVICES_STATE;
            mScanningTestbedDevicesCountDownTimer.cancel();
            mPgbWorking.setVisibility(View.INVISIBLE);
            mTxvNoDevicesFound.setVisibility(View.INVISIBLE);
            invalidateOptionsMenu();
        }
    }

    private void cancelDiscovering() {
        mActivityState = ACTIVITY_STATE.DISCOVERING_CANCELED;
        invalidateOptionsMenu();
        mBluetoothLeService.disconnect();
        discoveringDone();
    }

    private void discoveringDone() {
        unregisterReceiver(mDicovoveringTestbedDevicesBroadcastReceiver);
        List<TestbedDevice> devicesForRemove = new ArrayList<>();
        for (TestbedDevice testbedDevice : mDiscoveredTestbedDevicesAdapter.getTestbedDevices()) {
            if (!testbedDevice.isTestbedDeviceDiscovered()) {
                devicesForRemove.add(testbedDevice);
            }
        }
        mDiscoveredTestbedDevicesAdapter.removeTestbedDevices(devicesForRemove);
        mDiscoveredTestbedDevicesAdapter.notifyDataSetChanged();
        mActivityState = ACTIVITY_STATE.DISCOVERED_STATE;
        invalidateOptionsMenu();
        if (mDiscoveredTestbedDevicesAdapter.getCount() == 0) {
            mTxvNoDevicesFound.setVisibility(View.VISIBLE);
            mActivityState = ACTIVITY_STATE.NO_FOUNDED_DEVICES_STATE;
            invalidateOptionsMenu();
        }
        mPgbWorking.setVisibility(ProgressBar.INVISIBLE);
    }

    public void connectToSelectedTestbedDevice(final TestbedDevice testbedDevice) {
        mBluetoothLeService.setAutoReconnect(true);
        mBluetoothLeService.connect(testbedDevice, true);
        mBluetoothLeService.setAutoReconnect(true);
        registerReceiver(mConnectingToSelectedTestbedDeviceBroadcastReceiver,
                GATT_ACTION_UPDATE_INTENT_FILTER(INTENT_FILTER_TYPES.FOR_CONNECTING));
        mConnectingToSelectedTestbedDeviceDialog.show();
        mConnectingToSelectedTestbedDeviceConditionsAdapter.setCondition(
                CONNECTING_TO_SELCTED_TESTBED_DEVICE_STATUS,
                ConditionsAdapter.IN_PROGRESS,
                getString(R.string.dialog_connecting_to_selected_testbed_device_connecting) +
                        " " + mSelectedTestbedDevice.getName() + " (" +
                        TestbedDevice.formatTestbedDeviceId(mSelectedTestbedDevice) + ") ...");
        mConnectingToSelectedTestbedDeviceConditionsAdapter.notifyDataSetChanged();
    }

    private enum ACTIVITY_STATE {
        NO_FOUNDED_DEVICES_STATE,
        SCANNING_STATE,
        DISCOVERING_STATE,
        DISCOVERING_CANCELED,
        DISCOVERED_STATE
    }

    private enum SCAN_COMMAND {
        START_SCANING,
        STOP_SCANING,
        CANCEL_SCANING
    }

    private enum INTENT_FILTER_TYPES {
        FOR_DISCOVERING,
        FOR_CONNECTING,
        FOR_FIRST_CONNECTING
    }
}


