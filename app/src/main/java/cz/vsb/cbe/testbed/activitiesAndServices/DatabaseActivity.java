/*
  @author  Bc. Lukas Tatarin
 * @supervisor Ing. Jaromir Konecny, Ph.D.
 * @email   lukas@tatarin.cz
 * @version 1.10
 * @ide     Android Studio 4.1.2
 * @license GNU GPL v3
 * @brief   DatabaseActivity.java
 * @lastmodify 2021/03/05 11:38:57
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

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.CheckBox;
import android.widget.ListView;
import android.widget.Toast;

import androidx.activity.result.ActivityResult;
import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.content.res.AppCompatResources;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.tabs.TabLayout;
import com.opencsv.CSVWriter;

import java.io.IOException;
import java.io.OutputStreamWriter;
import java.math.BigInteger;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import cz.vsb.cbe.testbed.R;
import cz.vsb.cbe.testbed.adapters.ConditionsAdapter;
import cz.vsb.cbe.testbed.adapters.SelectTestbedDeviceAdapter;
import cz.vsb.cbe.testbed.fragments.ChartFragment;
import cz.vsb.cbe.testbed.fragments.ListItemFragment;
import cz.vsb.cbe.testbed.fragments.OverviewFragment;
import cz.vsb.cbe.testbed.sql.Record;
import cz.vsb.cbe.testbed.sql.TestbedDatabase;
import cz.vsb.cbe.testbed.sql.TestbedDevice;
import cz.vsb.cbe.testbed.utils.StatisticalData;

public class DatabaseActivity extends AppCompatActivity {

    private static final int LOSED_TESTBED_DEVICE_STATUS_COUNT = 3;
    private static final int CONNECTING_TO_LOSED_TESTBED_DEVICE_STATUS = 0;
    private static final int LOSED_TESTBED_DEVICE_SERVICE_DISCOVERING_STATUS = 1;
    private static final int LOSED_TESTBED_DEVICE_NOTIFICATION_ENABLED_STATUS = 2;

    private static final int TBI_OVERVIEW_POSITION = 0;
    private static final int TBI_CHARTVIEW_POSITION = 1;
    private static final int TBI_LISTVIEW_POSITION = 2;

    private static final int SPLASH_TIME_HIDE_DIALOG = 500;
    public BottomNavigationView mBnvChangeSensor;
    private BluetoothLeService mBluetoothLeService;
    private final ServiceConnection mBluetoothLeServiceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName componentName, IBinder service) {
            mBluetoothLeService = ((BluetoothLeService.LocalBinder) service).getService();
            mBluetoothLeService.setAutoReconnect(true);
            if (!mBluetoothLeService.initialize()) {
                Toast.makeText(DatabaseActivity.this,
                        getString(R.string.activity_database_bluetooth_le_service_failed),
                        Toast.LENGTH_SHORT).show();
                finish();
            }
        }

        @Override
        public void onServiceDisconnected(ComponentName componentName) {
            mBluetoothLeService = null;
        }
    };
    private TestbedDevice mTestbedDevice;
    private TestbedDatabase mTestbedDatabase;
    private Fragment mCurrentFragment;
    private FragmentTransaction fragmentTransaction;
    private OverviewFragment mOverviewFragment;
    private ChartFragment mChartFragment;
    private ListItemFragment mListItemFragment;
    private ConditionsAdapter mConnectingToLosedTestbedDeviceConditionsAdapter;
    private AlertDialog mConnectingToLosedTestbedDeviceDialog;

    private int mActualSelectedSensor;
    private Calendar mActualSortingInterval;
    private int mActualSortingLevel;
    private final BroadcastReceiver mConnectingToLosedTestbedDeviceBroadcastReceiver =
            new BroadcastReceiver() {
                @Override
                public void onReceive(Context context, Intent intent) {
                    switch (intent.getAction()) {
                        case BluetoothLeService.ACTION_GATT_DISCONNECTED:
                            mConnectingToLosedTestbedDeviceDialog.show();
                            mConnectingToLosedTestbedDeviceConditionsAdapter.setCondition(
                                    CONNECTING_TO_LOSED_TESTBED_DEVICE_STATUS,
                                    ConditionsAdapter.IN_PROGRESS,
                                    getString(R.string.dialog_connecting_to_losed_testbed_device_connecting) +
                                            " " + mTestbedDevice.getName() + " (" +
                                            TestbedDevice.formatTestbedDeviceId(mTestbedDevice) +
                                            ") ...");
                            mConnectingToLosedTestbedDeviceConditionsAdapter.setCondition(
                                    LOSED_TESTBED_DEVICE_SERVICE_DISCOVERING_STATUS,
                                    ConditionsAdapter.UNKNOWN, "");
                            mConnectingToLosedTestbedDeviceConditionsAdapter.setCondition(
                                    LOSED_TESTBED_DEVICE_NOTIFICATION_ENABLED_STATUS,
                                    ConditionsAdapter.UNKNOWN, "");
                            mConnectingToLosedTestbedDeviceConditionsAdapter.notifyDataSetChanged();
                            break;

                        case BluetoothLeService.ACTION_GATT_CONNECTED:
                            mConnectingToLosedTestbedDeviceConditionsAdapter.setCondition(
                                    CONNECTING_TO_LOSED_TESTBED_DEVICE_STATUS,
                                    ConditionsAdapter.PASS,
                                    getString(R.string.dialog_connecting_to_losed_testbed_device_connected) +
                                            " " + mTestbedDevice.getName() + " (" +
                                            TestbedDevice.formatTestbedDeviceId(mTestbedDevice) +
                                            ")");
                            mConnectingToLosedTestbedDeviceConditionsAdapter.setCondition(
                                    LOSED_TESTBED_DEVICE_SERVICE_DISCOVERING_STATUS,
                                    ConditionsAdapter.IN_PROGRESS,
                                    getString(R.string.dialog_connecting_to_losed_testbed_device_discovering) +
                                            " " + mTestbedDevice.getName() + " (" +
                                            TestbedDevice.formatTestbedDeviceId(mTestbedDevice) +
                                            ") ...");
                            mConnectingToLosedTestbedDeviceConditionsAdapter.notifyDataSetChanged();
                            break;

                        case BluetoothLeService.ACTION_GATT_SERVICES_DISCOVERED:
                            mConnectingToLosedTestbedDeviceConditionsAdapter.setCondition(
                                    LOSED_TESTBED_DEVICE_SERVICE_DISCOVERING_STATUS,
                                    ConditionsAdapter.PASS,
                                    getString(R.string.dialog_connecting_to_losed_testbed_device_discovered_1_2) +
                                            " " + mTestbedDevice.getName() + " (" +
                                            TestbedDevice.formatTestbedDeviceId(mTestbedDevice) +
                                            ") " +
                                            getString(R.string.dialog_connecting_to_losed_testbed_device_discovered_2_2));
                            mConnectingToLosedTestbedDeviceConditionsAdapter.setCondition(
                                    LOSED_TESTBED_DEVICE_NOTIFICATION_ENABLED_STATUS,
                                    ConditionsAdapter.IN_PROGRESS,
                                    getString(R.string.dialog_connecting_to_losed_testbed_device_notification_prepare) +
                                            " (" + mBluetoothLeService
                                            .getCharacteristicForDescriptorWriteSize() + ") ...");
                            mConnectingToLosedTestbedDeviceConditionsAdapter.notifyDataSetChanged();
                            break;

                        case BluetoothLeService.ACTION_GATT_DESCRIPTOR_WRITTEN:
                            mConnectingToLosedTestbedDeviceConditionsAdapter.setCondition(
                                    LOSED_TESTBED_DEVICE_NOTIFICATION_ENABLED_STATUS,
                                    ConditionsAdapter.IN_PROGRESS,
                                    getString(R.string.dialog_connecting_to_losed_testbed_device_notificationr_writing) +
                                            " (" + mBluetoothLeService.getDescriptorWriteIndex() +
                                            " " +
                                            getString(R.string.dialog_connecting_to_losed_testbed_device_notification_writing_conjunction) +
                                            " " + mBluetoothLeService.getCharacteristicForDescriptorWriteSize()
                                            + ")");
                            mConnectingToLosedTestbedDeviceConditionsAdapter.notifyDataSetChanged();
                            break;

                        case BluetoothLeService.ACTION_GATT_ALL_DESCRIPTORS_WRITTEN:
                            mConnectingToLosedTestbedDeviceConditionsAdapter.setCondition(
                                    LOSED_TESTBED_DEVICE_NOTIFICATION_ENABLED_STATUS,
                                    ConditionsAdapter.PASS,
                                    getString(R.string.dialog_connecting_to_losed_testbed_device_notification_written));
                            mConnectingToLosedTestbedDeviceConditionsAdapter.notifyDataSetChanged();
                            new Handler().postDelayed(() -> mConnectingToLosedTestbedDeviceDialog.hide(), SPLASH_TIME_HIDE_DIALOG);
                            break;

                        case BluetoothLeService.ACTION_STEP_DATA_AVAILABLE:
                            if (mActualSelectedSensor == R.id.pedometer) {
                                if (mCurrentFragment instanceof OverviewFragment) {
                                    mOverviewFragment.updateValues(true);
                                } else if (mCurrentFragment instanceof ChartFragment) {
                                    mChartFragment.updateChartData(false);
                                    mChartFragment.updateStatisticalDataDialog(
                                            new StatisticalData(mChartFragment.getRecords()));
                                } else if (mCurrentFragment instanceof ListItemFragment) {
                                    mListItemFragment.updateListViewData();
                                    mListItemFragment.updateStatisticalDataDialog(
                                            new StatisticalData(mListItemFragment.getRecords()));
                                }
                            }
                            break;

                        case BluetoothLeService.ACTION_HEART_RATE_DATA_AVAILABLE:
                            if (mActualSelectedSensor == R.id.heart_rate) {
                                if (mCurrentFragment instanceof OverviewFragment) {
                                    mOverviewFragment.updateValues(true);
                                } else if (mCurrentFragment instanceof ChartFragment) {
                                    mChartFragment.updateChartData(false);
                                    mChartFragment.updateStatisticalDataDialog(
                                            new StatisticalData(mChartFragment.getRecords()));
                                } else if (mCurrentFragment instanceof ListItemFragment) {
                                    mListItemFragment.updateListViewData();
                                    mListItemFragment.updateStatisticalDataDialog(
                                            new StatisticalData(mListItemFragment.getRecords()));
                                }
                            }
                            break;

                        case BluetoothLeService.ACTION_TEMPERATURE_DATA_AVAILABLE:
                            if (mActualSelectedSensor == R.id.temperature) {
                                if (mCurrentFragment instanceof OverviewFragment) {
                                    mOverviewFragment.updateValues(true);
                                } else if (mCurrentFragment instanceof ChartFragment) {
                                    mChartFragment.updateChartData(false);
                                    mChartFragment.updateStatisticalDataDialog(
                                            new StatisticalData(mChartFragment.getRecords()));
                                } else if (mCurrentFragment instanceof ListItemFragment) {
                                    mListItemFragment.updateListViewData();
                                    mListItemFragment.updateStatisticalDataDialog(
                                            new StatisticalData(mListItemFragment.getRecords()));
                                }
                            }
                            break;

                        default:
                            throw new IllegalStateException(
                                    "Unexpected value of intent.getAction(): " + intent.getAction());
                    }
                }
            };
    private List<Record> mRecordsForExport;
    private List<TestbedDevice> mTestbedDevicesForExport;
    private final ActivityResultLauncher<Intent> mOnDataExportedActivityLanucher
            = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(),
            new ActivityResultCallback<ActivityResult>() {
                @Override
                public void onActivityResult(ActivityResult result) {
                    if (result.getResultCode() == Activity.RESULT_OK) {
                        CSVWriter csvWriter;
                        List<String[]> entries;
                        try {
                            csvWriter = new CSVWriter(new OutputStreamWriter(
                                    DatabaseActivity.this.getContentResolver()
                                            .openOutputStream(Objects.requireNonNull(
                                                    result.getData()).getData())));
                            csvWriter.writeNext(("DEVICE ID;DEVICE NAME;" +
                                    "AVAILABLE SENSORS (0b) [ P H T ];" +
                                    "DEVICE MAC ADDRESS;" +
                                    "LAST CONNECTED;TIMESTAMP")
                                    .split(";"));
                            entries = new ArrayList<>();
                            for (TestbedDevice testbedDevice : mTestbedDevicesForExport) {
                                String[] entry = new String[6];
                                entry[0] = String.valueOf(testbedDevice.getDeviceId());
                                entry[1] = testbedDevice.getName();
                                entry[2] = String.valueOf(testbedDevice.getAvailableSensors());
                                entry[3] = testbedDevice.getMacAddress();
                                entry[4] = String.valueOf(testbedDevice.isLastConnected());
                                entry[5] = String.valueOf(testbedDevice.getTimeStamp().getTime());
                                entries.add(entry);
                            }
                            csvWriter.writeAll(entries);
                            csvWriter.writeNext(new String[1]);
                            csvWriter.writeNext(("DATA ID;" +
                                    "DEVICE ID;" +
                                    "DATA KEY;" +
                                    "DATA VALUE;" +
                                    "TIMESTAMP")
                                    .split(";"));
                            entries = new ArrayList<>();
                            for (Record record : mRecordsForExport) {
                                String[] entry = new String[5];
                                entry[0] = String.valueOf(record.getDataId());
                                entry[1] = String.valueOf(record.getDeviceId());
                                entry[2] = record.getDataKey();
                                entry[3] = String.valueOf(record.getValue());
                                entry[4] = String.valueOf(record.getTimeStamp().getTime());
                                entries.add(entry);
                            }
                            csvWriter.writeAll(entries);
                            csvWriter.close();
                        } catch (IOException e) {
                            Toast.makeText(DatabaseActivity.this,
                                    getString(R.string.activity_database_export_data_failed),
                                    Toast.LENGTH_SHORT).show();
                        }
                    }
                }
            });
    private boolean mDestroyBackgroundService = true;
    private ExecutorService mUpdateCurentSystemTimeExecutorService;
    private final Runnable mUpdateCurrentSystemTimeRunnable = new Runnable() {
        @Override
        public void run() {
            //noinspection CatchMayIgnoreException
            try {
                Thread.sleep(100);
                invalidateOptionsMenu();
                mUpdateCurentSystemTimeExecutorService.submit(mUpdateCurrentSystemTimeRunnable);
            } catch (InterruptedException e) {

            }
        }
    };

    private static IntentFilter GATT_ACTION_UPDATE_INTENT_FILTER() {
        final IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(BluetoothLeService.ACTION_GATT_CONNECTED);
        intentFilter.addAction(BluetoothLeService.ACTION_GATT_SERVICES_DISCOVERED);
        intentFilter.addAction(BluetoothLeService.ACTION_GATT_DESCRIPTOR_WRITTEN);
        intentFilter.addAction(BluetoothLeService.ACTION_GATT_ALL_DESCRIPTORS_WRITTEN);
        intentFilter.addAction(BluetoothLeService.ACTION_STEP_DATA_AVAILABLE);
        intentFilter.addAction(BluetoothLeService.ACTION_HEART_RATE_DATA_AVAILABLE);
        intentFilter.addAction(BluetoothLeService.ACTION_TEMPERATURE_DATA_AVAILABLE);
        intentFilter.addAction(BluetoothLeService.ACTION_GATT_DISCONNECTED);
        return intentFilter;
    }

    public BluetoothLeService getBluetoothLeService() {
        return mBluetoothLeService;
    }

    public TestbedDevice getTestbedDevice() {
        return mTestbedDevice;
    }

    public TestbedDatabase getTestbedDatabase() {
        return mTestbedDatabase;
    }

    public int getActualSelectedSensor() {
        return mActualSelectedSensor;
    }

    public Calendar getActualSortingInterval() {
        return mActualSortingInterval;
    }

    public int getActualSortingLevel() {
        return mActualSortingLevel;
    }

    public void setActualSortingLevel(int actualSortingLevel) {
        mActualSortingLevel = actualSortingLevel;
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_database_activity, menu);
        return true;
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        @SuppressLint("SimpleDateFormat") SimpleDateFormat simpleDateFormat =
                new SimpleDateFormat("HH:mm:ss");
        menu.findItem(R.id.system_time).setTitle(
                getString(R.string.menu_system_time) + ": " +
                        simpleDateFormat.format(new Date()));
        return super.onPrepareOptionsMenu(menu);
    }

    @SuppressLint("NonConstantResourceId")
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.export_data:
                exportData();
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
                aboutApplicationDialog.show();
                return true;

            case R.id.disconnect_from_device:
                mDestroyBackgroundService = false;
                mBluetoothLeService.disconnect();
                finish();
                startActivity(new Intent(
                        DatabaseActivity.this, DiscoverDevicesActivity.class)
                        .putExtra(StartUpActivity.FIRST_RUN_APPLICATION_KEY, false));
                return true;

            case R.id.close_application:
                AlertDialog closeAppliactionDialog = new AlertDialog.Builder(this)
                        .setIcon(AppCompatResources.getDrawable(
                                this, R.drawable.ic_shutdown_20dp_color_vsb))
                        .setTitle(getString(R.string.dialog_close_application_title))
                        .setPositiveButton(getString(R.string.dialog_close_application_positive_button),
                                (dialog, which) -> finish())
                        .setNeutralButton(getString(R.string.dialog_close_application_neutral_button),
                                (dialog, which) -> {
                                    mDestroyBackgroundService = false;
                                    mBluetoothLeService.disconnect();
                                    finish();
                                    startActivity(new Intent(
                                            DatabaseActivity.this,
                                            DiscoverDevicesActivity.class)
                                            .putExtra(StartUpActivity.FIRST_RUN_APPLICATION_KEY,
                                                    false));
                                })
                        .setNegativeButton(getString(R.string.dialog_close_application_negative_button),
                                null)
                        .create();
                closeAppliactionDialog.show();
                return true;

            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mTestbedDatabase = new TestbedDatabase(DatabaseActivity.this);
        mTestbedDevice = (TestbedDevice) getIntent()
                .getSerializableExtra(TestbedDevice.TESTBED_DEVICE);

        mOverviewFragment = new OverviewFragment();
        mChartFragment = new ChartFragment();
        mListItemFragment = new ListItemFragment();

        fragmentTransaction = getSupportFragmentManager().beginTransaction();
        fragmentTransaction.replace(R.id.activity_database_frg, mOverviewFragment);
        mCurrentFragment = mOverviewFragment;
        fragmentTransaction.commit();

        mActualSortingInterval = Calendar.getInstance();
        mActualSortingLevel = Calendar.MONTH;

        mUpdateCurentSystemTimeExecutorService = Executors.newSingleThreadExecutor();
        mUpdateCurentSystemTimeExecutorService.submit(mUpdateCurrentSystemTimeRunnable);

        setContentView(R.layout.activity_database);
        setTitle(mTestbedDevice.getName() + " (" +
                TestbedDevice.formatTestbedDeviceId(mTestbedDevice) + ")");

        TabLayout tblChangeFragment = findViewById(R.id.activity_database_tbl_select_view);
        tblChangeFragment.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                fragmentTransaction = getSupportFragmentManager().beginTransaction();
                switch (tab.getPosition()) {
                    case TBI_OVERVIEW_POSITION:
                        fragmentTransaction.replace(R.id.activity_database_frg, mOverviewFragment);
                        mCurrentFragment = mOverviewFragment;
                        break;

                    case TBI_CHARTVIEW_POSITION:
                        fragmentTransaction.replace(R.id.activity_database_frg, mChartFragment);
                        mCurrentFragment = mChartFragment;
                        break;

                    case TBI_LISTVIEW_POSITION:
                        fragmentTransaction.replace(R.id.activity_database_frg, mListItemFragment);
                        mCurrentFragment = mListItemFragment;
                        break;
                }
                fragmentTransaction.commit();
            }

            @Override
            public void onTabUnselected(TabLayout.Tab tab) {

            }

            @Override
            public void onTabReselected(TabLayout.Tab tab) {

            }

        });

        mBnvChangeSensor = findViewById(R.id.activity_database_bnv_main);

        if (!BigInteger.valueOf(mTestbedDevice.getAvailableSensors()).testBit(0)) {
            mBnvChangeSensor.getMenu().removeItem(R.id.temperature);
        }

        if (!BigInteger.valueOf(mTestbedDevice.getAvailableSensors()).testBit(1)) {
            mBnvChangeSensor.getMenu().removeItem(R.id.heart_rate);
        }

        if (!BigInteger.valueOf(mTestbedDevice.getAvailableSensors()).testBit(2)) {
            mBnvChangeSensor.getMenu().removeItem(R.id.pedometer);
        }

        mActualSelectedSensor = mBnvChangeSensor.getSelectedItemId();

        mBnvChangeSensor.setOnNavigationItemSelectedListener(item -> {
            mActualSelectedSensor = item.getItemId();
            if (mCurrentFragment instanceof OverviewFragment) {
                mOverviewFragment.sensorChanged();
            } else if (mCurrentFragment instanceof ChartFragment) {
                mChartFragment.updateChartData(true);
            } else if (mCurrentFragment instanceof ListItemFragment) {
                mListItemFragment.updateListViewData();
            }
            return true;
        });

        View connectingToLosedTestbedDeviceDialogView =
                getLayoutInflater().inflate(R.layout.dialog_general_list_view, null);
        final ListView lswConnectingToLosedTestbedDeviceConditions =
                connectingToLosedTestbedDeviceDialogView.findViewById(R.id.dialog_list_view_lsv);
        mConnectingToLosedTestbedDeviceConditionsAdapter =
                new ConditionsAdapter(getLayoutInflater(), getApplicationContext(),
                        LOSED_TESTBED_DEVICE_STATUS_COUNT);
        lswConnectingToLosedTestbedDeviceConditions
                .setAdapter(mConnectingToLosedTestbedDeviceConditionsAdapter);

        mConnectingToLosedTestbedDeviceDialog = new AlertDialog.Builder(this)
                .setIcon(AppCompatResources.getDrawable(this,
                        R.drawable.ic_connecting_20dp_color_vsb))
                .setTitle(getString(R.string.dialog_connecting_to_losed_testbed_device_title))
                .setView(connectingToLosedTestbedDeviceDialogView)
                .setNegativeButton(
                        getString(R.string.dialog_connecting_to_losed_testbed_device_negative_button),
                        (dialog, which) -> {
                            mBluetoothLeService.setAutoReconnect(false);
                            mBluetoothLeService.removeTestbedDevice();
                            mBluetoothLeService.disconnect();
                            finish();
                        })
                .create();
        mConnectingToLosedTestbedDeviceDialog.setCancelable(false);
        mConnectingToLosedTestbedDeviceDialog.setCanceledOnTouchOutside(false);
    }

    @Override
    protected void onResume() {
        super.onResume();
        bindService(new Intent(this, BluetoothLeService.class),
                mBluetoothLeServiceConnection, BIND_ADJUST_WITH_ACTIVITY);
        registerReceiver(mConnectingToLosedTestbedDeviceBroadcastReceiver,
                GATT_ACTION_UPDATE_INTENT_FILTER());
        if (mCurrentFragment instanceof OverviewFragment) {
            mOverviewFragment.sensorChanged();
        } else if (mCurrentFragment instanceof ChartFragment) {
            mChartFragment.updateChartData(true);
        } else if (mCurrentFragment instanceof ListItemFragment) {
            mListItemFragment.updateListViewData();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        unbindService(mBluetoothLeServiceConnection);
        unregisterReceiver(mConnectingToLosedTestbedDeviceBroadcastReceiver);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mBluetoothLeService = null;
        if (mDestroyBackgroundService) {
            stopService(new Intent(this, BluetoothLeService.class));
        }
    }

    @Override
    public void onBackPressed() {
        AlertDialog closeAppliactionDialog = new AlertDialog.Builder(this)
                .setIcon(AppCompatResources.getDrawable(
                        this, R.drawable.ic_shutdown_20dp_color_vsb))
                .setTitle(getString(R.string.dialog_close_application_title))
                .setPositiveButton(getString(R.string.dialog_close_application_positive_button),
                        (dialog, which) -> finish())
                .setNeutralButton(getString(R.string.dialog_close_application_neutral_button),
                        (dialog, which) -> {
                            mDestroyBackgroundService = false;
                            mBluetoothLeService.disconnect();
                            finish();
                            startActivity(new Intent(
                                    DatabaseActivity.this, DiscoverDevicesActivity.class)
                                    .putExtra(StartUpActivity.FIRST_RUN_APPLICATION_KEY, false));
                        })
                .setNegativeButton(getString(R.string.dialog_close_application_negative_button),
                        null)
                .create();
        closeAppliactionDialog.show();
    }

    @SuppressWarnings("CatchMayIgnoreException")
    public void exportData() {
        View exportDataDialogView = getLayoutInflater()
                .inflate(R.layout.dialog_export_data, null);
        ListView lsvExportDataDialog =
                exportDataDialogView.findViewById(R.id.dialog_export_data_lsv_devices_for_export);
        final SelectTestbedDeviceAdapter selectTestbedDeviceAdapter =
                new SelectTestbedDeviceAdapter(getLayoutInflater(), this);
        lsvExportDataDialog.setAdapter(selectTestbedDeviceAdapter);
        final CheckBox chbStepsData =
                exportDataDialogView.findViewById(R.id.dialog_export_data_chb_steps_data);
        final CheckBox chbHeartRateData =
                exportDataDialogView.findViewById(R.id.dialog_export_data_chb_heart_rate_data);
        final CheckBox chbTemperatureData =
                exportDataDialogView.findViewById(R.id.dialog_export_data_chb_temperature_data);

        try {
            selectTestbedDeviceAdapter.addTestbedDevices(mTestbedDatabase.selectAllTestbedDevices());
            selectTestbedDeviceAdapter.notifyDataSetChanged();
        } catch (TestbedDatabase.EmptyCursorException e) {
            finish();
        }

        AlertDialog exportDataDialog = new AlertDialog.Builder(this)
                .setIcon(AppCompatResources.getDrawable(this,
                        R.drawable.ic_save_20dp_color_vsb))
                .setTitle(getString(R.string.dialog_export_data_title))
                .setView(exportDataDialogView)
                .setPositiveButton(getString(R.string.dialog_export_data_positive_button),
                        (dialog, which) -> {
                            SimpleDateFormat simpleDateFormat =
                                    new SimpleDateFormat("yyyyMMdd_EE_HHmmss", Locale.US);
                            StringBuilder fileName =
                                    new StringBuilder(simpleDateFormat.format(new Date()));
                            mRecordsForExport = new ArrayList<>();
                            mTestbedDevicesForExport =
                                    selectTestbedDeviceAdapter.getSelectedTestbedDevices();
                            for (TestbedDevice testbedDevice : mTestbedDevicesForExport) {
                                fileName.append("_TBD_").append(String.format("%04X",
                                        testbedDevice.getDeviceId()));
                                if (chbStepsData.isChecked()) {
                                    try {
                                        mRecordsForExport.addAll(mTestbedDatabase.selectAllRecords(
                                                testbedDevice, BluetoothLeService.STEPS_DATA));
                                    } catch (TestbedDatabase.EmptyCursorException e) {

                                    }
                                }
                                if (chbHeartRateData.isChecked()) {
                                    try {
                                        mRecordsForExport.addAll(mTestbedDatabase.selectAllRecords(
                                                testbedDevice, BluetoothLeService.HEART_RATE_DATA));
                                    } catch (TestbedDatabase.EmptyCursorException e) {
                                    }
                                }
                                if (chbTemperatureData.isChecked()) {
                                    try {
                                        mRecordsForExport.addAll(mTestbedDatabase.selectAllRecords(
                                                testbedDevice, BluetoothLeService.TEMPERATURE_DATA));
                                    } catch (TestbedDatabase.EmptyCursorException e) {
                                    }
                                }

                            }
                            if (chbStepsData.isChecked()) {
                                fileName.append("_PED");
                            }
                            if (chbHeartRateData.isChecked()) {
                                fileName.append("_HRT");
                            }
                            if (chbTemperatureData.isChecked()) {
                                fileName.append("_TEM");
                            }

                            Intent intent = new Intent(Intent.ACTION_CREATE_DOCUMENT);
                            intent.addCategory(Intent.CATEGORY_OPENABLE);
                            intent.setType("*/*");
                            intent.putExtra(Intent.EXTRA_TITLE, fileName.toString().toUpperCase() +
                                    ".csv");
                            mOnDataExportedActivityLanucher.launch(intent);
                        })
                .setNegativeButton(getString(R.string.dialog_export_data_negative_button), null)
                .create();

        exportDataDialog.show();

    }
}