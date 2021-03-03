/*
  @author  Bc. Lukas Tatarin
 * @supervisor Ing. Jaromir Konecny, Ph.D.
 * @email   lukas@tatarin.cz
 * @version 1.00
 * @ide     Android Studio 4.1.2
 * @license GNU GPL v3
 * @brief   StartUpActivity.java
 * @lastmodify 2021/03/03 12:36:28
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

import android.Manifest;
import android.app.Activity;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.location.LocationManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.provider.Settings;
import android.view.View;
import android.view.WindowManager.LayoutParams;
import android.widget.ListView;

import androidx.activity.result.ActivityResult;
import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.content.res.AppCompatResources;
import androidx.core.location.LocationManagerCompat;

import org.jetbrains.annotations.NotNull;

import java.util.Objects;

import static cz.vsb.cbe.testbed.BluetoothLeService.NOTIFICATION_CHANNEL_ID;

@SuppressWarnings("AccessStaticViaInstance")
public class StartUpActivity extends AppCompatActivity {

    public static final String FIRST_RUN_APPLICATION = "cz.vsb.cbe.testbed.FIRST_RUN_APPLICATION";

    private static final int SPLASH_TIME_MS = 1000;
    private static final int CANCEL_TIME_MS = 15000;

    private static final int MY_PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION = 0;

    private static final int START_UP_ACTIVITY_STATE = 0;
    private static final int PERMISSION_CHECK_ACTIVITY_STATE = 1;
    private static final int LOCATION_CHECK_ACTIVITY_STATE = 2;
    private final String[] mPermissions = {Manifest.permission.ACCESS_FINE_LOCATION};
    private final Runnable mFinishAppRunnable = this::finish;
    private int mActivityState = START_UP_ACTIVITY_STATE;
    private BluetoothAdapter mBluetoothAdapter;
    private Handler mFinishAppHandler;
    private ConditionsAdapter mStartUpConditionsAdapter;
    final ActivityResultLauncher<Intent> mOnBluetoothAdapterEnabledActivityResult =
            registerForActivityResult(
                    new ActivityResultContracts.StartActivityForResult(),
                    new ActivityResultCallback<ActivityResult>() {
                        @Override
                        public void onActivityResult(ActivityResult result) {
                            if (result.getResultCode() == Activity.RESULT_OK) {
                                mStartUpConditionsAdapter.setCondition(
                                        new StartUpConditionsOrder().BLUETOOTH_ADAPTER_STATUS,
                                        ConditionsAdapter.PASS,
                                        getString(R.string.activity_start_up_condition_bluetooth_adapter));
                                mStartUpConditionsAdapter.notifyDataSetChanged();
                                continueApplication();
                            } else {
                                AlertDialog bluetoothAdapterDeniedDialog =
                                        new AlertDialog.Builder(StartUpActivity.this)
                                                .setIcon(AppCompatResources.getDrawable(StartUpActivity.this,
                                                        R.drawable.ic_bluetooth_20dp_color_red))
                                                .setTitle(R.string.dialog_bluetooth_adapter_denied_title)
                                                .setMessage(R.string.dialog_bluetooth_adapter_denied_message)
                                                .setNegativeButton(
                                                        R.string.dialog_bluetooth_adapter_denied_negative_button,
                                                        (dialog, which) -> finish())
                                                .setPositiveButton(
                                                        R.string.dialog_bluetooth_adapter_denied_positive_button,
                                                        (dialog, which) -> {
                                                            enableBluetoothAdapter();
                                                            mStartUpConditionsAdapter.setCondition(
                                                                    new StartUpConditionsOrder().BLUETOOTH_ADAPTER_STATUS,
                                                                    ConditionsAdapter.IN_PROGRESS,
                                                                    getString(R.string.activity_start_up_condition_bluetooth_adapter));
                                                            mStartUpConditionsAdapter.notifyDataSetChanged();
                                                            mFinishAppHandler.removeCallbacks(mFinishAppRunnable);
                                                        })
                                                .create();
                                bluetoothAdapterDeniedDialog.setCancelable(false);
                                bluetoothAdapterDeniedDialog.setCanceledOnTouchOutside(false);
                                bluetoothAdapterDeniedDialog.show();
                                mFinishAppHandler.postDelayed(mFinishAppRunnable, CANCEL_TIME_MS);

                                mStartUpConditionsAdapter.setCondition(
                                        new StartUpConditionsOrder().BLUETOOTH_ADAPTER_STATUS,
                                        ConditionsAdapter.FAIL,
                                        getString(R.string.activity_start_up_condition_bluetooth_adapter));
                                mStartUpConditionsAdapter.notifyDataSetChanged();
                            }
                        }
                    });

    @Override
    public void onRequestPermissionsResult(int requestCode, @NotNull String[] permissions,
                                           @NotNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == MY_PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                mStartUpConditionsAdapter.setCondition(
                        new StartUpConditionsOrder().PERMISSION_STATUS,
                        ConditionsAdapter.PASS,
                        getString(R.string.activity_start_up_condition_permissions));
                mStartUpConditionsAdapter.notifyDataSetChanged();
                enableLocation();
            } else if (shouldShowRequestPermissionRationale(
                    Manifest.permission.ACCESS_FINE_LOCATION)) {
                permissionAndExplanationProcedure();
            } else {
                AlertDialog locationPermissionDeniedForeverDialog =
                        new AlertDialog.Builder(StartUpActivity.this)
                                .setIcon(AppCompatResources.getDrawable(this,
                                        R.drawable.ic_location_20dp_color_red))
                                .setTitle(R.string.dialog_location_permission_denied_forever_title)
                                .setMessage(R.string.dialog_location_permission_denied_forever_message)
                                .setNegativeButton(
                                        R.string.dialog_location_permission_denied_forever_negative_button,
                                        (dialog, which) -> finish())
                                .setPositiveButton(
                                        R.string.dialog_location_permission_denied_forever_positive_button,
                                        (dialog, which) -> {
                                            Intent intent = new Intent();
                                            intent.setAction(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
                                            intent.setData(
                                                    Uri.parse("package:" + StartUpActivity.this.getPackageName()));
                                            mActivityState = PERMISSION_CHECK_ACTIVITY_STATE;
                                            startActivity(intent);
                                        })
                                .create();
                locationPermissionDeniedForeverDialog.setCancelable(false);
                locationPermissionDeniedForeverDialog.setCanceledOnTouchOutside(false);
                locationPermissionDeniedForeverDialog.show();

                mStartUpConditionsAdapter.setCondition(
                        new StartUpConditionsOrder().PERMISSION_STATUS,
                        ConditionsAdapter.FAIL,
                        getString(R.string.activity_start_up_condition_permissions));
                mStartUpConditionsAdapter.notifyDataSetChanged();
            }
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
        if (mActivityState == PERMISSION_CHECK_ACTIVITY_STATE) {
            permissionAndExplanationProcedure();
        } else if (mActivityState == LOCATION_CHECK_ACTIVITY_STATE) {
            enableLocation();
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_FULLSCREEN |
                View.SYSTEM_UI_FLAG_HIDE_NAVIGATION |
                View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY);

        getWindow().addFlags(LayoutParams.FLAG_TRANSLUCENT_STATUS);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            LayoutParams layoutParams = getWindow().getAttributes();
            layoutParams.layoutInDisplayCutoutMode =
                    LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES;
        }

        setContentView(R.layout.activity_start_up);
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_NOSENSOR);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            String name = getString(R.string.notification_chanel_logging_name);
            String description = getString(R.string.notification_chanel_logging_description);
            int importance = NotificationManager.IMPORTANCE_DEFAULT;
            NotificationChannel channel = new NotificationChannel(NOTIFICATION_CHANNEL_ID,
                    name, importance);
            channel.setDescription(description);
            NotificationManager notificationManager = getSystemService(NotificationManager.class);
            Objects.requireNonNull(notificationManager).createNotificationChannel(channel);
        }

        mFinishAppHandler = new Handler();
        ListView lsvStartUpConditions = findViewById(R.id.activity_start_up_lsv_conditions);
        mStartUpConditionsAdapter = new ConditionsAdapter(this.getLayoutInflater(), this,
                new StartUpConditionsOrder().getNumberOdConditions());
        lsvStartUpConditions.setAdapter(mStartUpConditionsAdapter);

        mStartUpConditionsAdapter.setCondition(
                new StartUpConditionsOrder().BLUETOOTH_LOW_ENERGY_STATUS,
                ConditionsAdapter.IN_PROGRESS,
                getString(R.string.activity_start_up_condition_bluetooth_low_energy));
        mStartUpConditionsAdapter.notifyDataSetChanged();

        if (!getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)) {
            mStartUpConditionsAdapter.setCondition(
                    new StartUpConditionsOrder().BLUETOOTH_LOW_ENERGY_STATUS,
                    ConditionsAdapter.FAIL,
                    getString(R.string.activity_start_up_condition_bluetooth_low_energy));
            mStartUpConditionsAdapter.notifyDataSetChanged();

            AlertDialog bleNotSupportedDialog = new AlertDialog.Builder(this)
                    .setIcon(AppCompatResources.getDrawable(this,
                            R.drawable.ic_bluetooth_20dp_color_red))
                    .setTitle(R.string.dialog_ble_not_supported_title)
                    .setMessage(R.string.dialog_ble_not_supported_message)
                    .setNeutralButton(R.string.dialog_ble_not_supported_negative_button,
                            (dialog, which) -> finish())
                    .create();
            bleNotSupportedDialog.show();
            mFinishAppHandler.postDelayed(mFinishAppRunnable, CANCEL_TIME_MS);

        } else {
            final BluetoothManager bluetoothManager =
                    (BluetoothManager) getSystemService(BLUETOOTH_SERVICE);
            mBluetoothAdapter = Objects.requireNonNull(bluetoothManager).getAdapter();
            mStartUpConditionsAdapter.setCondition(
                    new StartUpConditionsOrder().BLUETOOTH_LOW_ENERGY_STATUS,
                    ConditionsAdapter.PASS,
                    getString(R.string.activity_start_up_condition_bluetooth_low_energy));
            mStartUpConditionsAdapter.setCondition(
                    new StartUpConditionsOrder().PERMISSION_STATUS,
                    ConditionsAdapter.IN_PROGRESS,
                    getString(R.string.activity_start_up_condition_permissions));
            mStartUpConditionsAdapter.notifyDataSetChanged();
            permissionAndExplanationProcedure();
        }
    }

    private void permissionAndExplanationProcedure() {
        if (checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) !=
                PackageManager.PERMISSION_GRANTED) {
            if (shouldShowRequestPermissionRationale(Manifest.permission.ACCESS_FINE_LOCATION)) {
                AlertDialog locationPermissionRequestDialog = new AlertDialog.Builder(this)
                        .setIcon(AppCompatResources.getDrawable(this,
                                R.drawable.ic_lock_20dp_color_vsb))
                        .setTitle(R.string.dialog_location_permission_request_title)
                        .setMessage(R.string.dialog_location_permission_request_message)
                        .setPositiveButton(R.string.dialog_location_permission_request_positive_button,
                                (dialog, which) -> requestPermissions(
                                        mPermissions, MY_PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION))
                        .setNegativeButton(R.string.dialog_location_permission_request_negative_button,
                                (dialog, which) -> {
                                    AlertDialog locationPermissionDeniedDialog =
                                            new AlertDialog.Builder(StartUpActivity.this)
                                                    .setIcon(AppCompatResources.getDrawable(this,
                                                            R.drawable.ic_location_20dp_color_red))
                                                    .setTitle(R.string.dialog_location_permission_denied_title)
                                                    .setMessage(R.string.dialog_location_permission_denied_message)
                                                    .setNegativeButton(
                                                            R.string.dialog_location_permission_denied_negative_button,
                                                            (dialog1, which1) -> finish())
                                                    .setPositiveButton(
                                                            R.string.dialog_location_permission_denied_positive_button,
                                                            (dialog12, which12) -> {
                                                                requestPermissions(mPermissions,
                                                                        MY_PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION);
                                                                mFinishAppHandler.removeCallbacks(mFinishAppRunnable);
                                                            })
                                                    .create();
                                    locationPermissionDeniedDialog.setCancelable(false);
                                    locationPermissionDeniedDialog.setCanceledOnTouchOutside(false);
                                    locationPermissionDeniedDialog.show();
                                    mFinishAppHandler.postDelayed(mFinishAppRunnable, CANCEL_TIME_MS);
                                })
                        .create();
                locationPermissionRequestDialog.setCancelable(false);
                locationPermissionRequestDialog.setCanceledOnTouchOutside(false);
                locationPermissionRequestDialog.show();
            } else {
                requestPermissions(mPermissions, MY_PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION);
            }
        } else {
            mStartUpConditionsAdapter.setCondition(
                    new StartUpConditionsOrder().PERMISSION_STATUS,
                    ConditionsAdapter.PASS,
                    getString(R.string.activity_start_up_condition_permissions));
            mStartUpConditionsAdapter.notifyDataSetChanged();
            enableLocation();
        }
    }

    private void enableLocation(){
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            mStartUpConditionsAdapter.setCondition(new StartUpConditionsOrder().LOCATION_STATUS,
                    ConditionsAdapter.IN_PROGRESS,
                    getString(R.string.activity_start_up_condition_location));
            mStartUpConditionsAdapter.notifyDataSetChanged();
            if (LocationManagerCompat.isLocationEnabled(
                    (LocationManager) Objects.requireNonNull(
                            getSystemService(Context.LOCATION_SERVICE)))) {
                mStartUpConditionsAdapter.setCondition(new StartUpConditionsOrder().LOCATION_STATUS,
                        ConditionsAdapter.PASS,
                        getString(R.string.activity_start_up_condition_location));
                mStartUpConditionsAdapter.notifyDataSetChanged();
                enableBluetoothAdapter();
            } else {
                mStartUpConditionsAdapter.setCondition(
                        new StartUpConditionsOrder().LOCATION_STATUS,
                        ConditionsAdapter.FAIL,
                        getString(R.string.activity_start_up_condition_location));
                mStartUpConditionsAdapter.notifyDataSetChanged();
                AlertDialog locationRequestDialog = new AlertDialog.Builder(this)
                        .setIcon(AppCompatResources.getDrawable(this,
                                R.drawable.ic_location_20dp_color_vsb))
                        .setTitle(R.string.dialog_location_request_title)
                        .setMessage(R.string.dialog_location_request_message)
                        .setPositiveButton(R.string.dialog_location_request_positive_button,
                                (dialog, which) -> {
                                    mFinishAppHandler.removeCallbacks(mFinishAppRunnable);
                                    mActivityState = LOCATION_CHECK_ACTIVITY_STATE;
                                    startActivity(new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS));
                                })
                        .setNegativeButton(R.string.dialog_location_request_negative_button,
                                (dialog, which) -> {
                                    mFinishAppHandler.removeCallbacks(mFinishAppRunnable);
                                    AlertDialog locationDeniedDialog =
                                            new AlertDialog.Builder(StartUpActivity.this)
                                                    .setIcon(AppCompatResources.getDrawable(this,
                                                            R.drawable.ic_location_20dp_color_red))
                                                    .setTitle(R.string.dialog_location_denied_title)
                                                    .setMessage(R.string.dialog_location_denied_message)
                                                    .setPositiveButton(
                                                            R.string.dialog_location_denied_positive_button,
                                                            (dialog1, which1) -> {
                                                                mFinishAppHandler.removeCallbacks(mFinishAppRunnable);
                                                                mActivityState = LOCATION_CHECK_ACTIVITY_STATE;
                                                                startActivity(new Intent(
                                                                        Settings.ACTION_LOCATION_SOURCE_SETTINGS));
                                                            })
                                                    .setNegativeButton(R.string.dialog_location_denied_negative_button,
                                                            (dialog12, which12) -> {
                                                                mFinishAppHandler.removeCallbacks(mFinishAppRunnable);
                                                                finish();
                                                            })
                                                    .create();
                                    locationDeniedDialog.setCancelable(false);
                                    locationDeniedDialog.setCanceledOnTouchOutside(false);
                                    locationDeniedDialog.show();
                                    mFinishAppHandler.postDelayed(mFinishAppRunnable, CANCEL_TIME_MS);

                                })
                        .create();
                locationRequestDialog.setCancelable(false);
                locationRequestDialog.setCanceledOnTouchOutside(false);
                locationRequestDialog.show();
                mFinishAppHandler.postDelayed(mFinishAppRunnable, CANCEL_TIME_MS);
            }
        } else{
            enableBluetoothAdapter();
        }
    }

    private void enableBluetoothAdapter() {
        if (mBluetoothAdapter == null || !mBluetoothAdapter.isEnabled()) {
            mOnBluetoothAdapterEnabledActivityResult.launch(
                    new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE));
            mStartUpConditionsAdapter.setCondition(
                    new StartUpConditionsOrder().BLUETOOTH_ADAPTER_STATUS,
                    ConditionsAdapter.IN_PROGRESS,
                    getString(R.string.activity_start_up_condition_bluetooth_adapter));
        } else {
            mStartUpConditionsAdapter.setCondition(
                    new StartUpConditionsOrder().BLUETOOTH_ADAPTER_STATUS,
                    ConditionsAdapter.PASS,
                    getString(R.string.activity_start_up_condition_bluetooth_adapter));
            mStartUpConditionsAdapter.notifyDataSetChanged();
            continueApplication();
        }
    }

    private void continueApplication() {
        mStartUpConditionsAdapter.setCondition(new StartUpConditionsOrder().START_UP_STATUS,
                ConditionsAdapter.IN_PROGRESS,
                getString(R.string.activity_start_up_condition_start_application));
        startService(new Intent(StartUpActivity.this, BluetoothLeService.class));
        new Handler().postDelayed(() -> {
            mStartUpConditionsAdapter.setCondition(new StartUpConditionsOrder().START_UP_STATUS,
                    ConditionsAdapter.PASS,
                    getString(R.string.activity_start_up_condition_start_application));
            mStartUpConditionsAdapter.notifyDataSetChanged();
        }, SPLASH_TIME_MS - 300);

        new Handler().postDelayed(() -> {
            Intent intent = new Intent(StartUpActivity.this,
                    DiscoverDevicesActivity.class);
            intent.putExtra(FIRST_RUN_APPLICATION, true);
            startActivity(intent);
            finish();
        }, SPLASH_TIME_MS);
    }

    public static final class StartUpConditionsOrder {

        public static final int BLUETOOTH_LOW_ENERGY_STATUS = 0;
        public static final int PERMISSION_STATUS = 1;
        public static int LOCATION_STATUS;
        public static int BLUETOOTH_ADAPTER_STATUS;
        public static int START_UP_STATUS;

        public StartUpConditionsOrder() {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                LOCATION_STATUS = 2;
                BLUETOOTH_ADAPTER_STATUS = 3;
                START_UP_STATUS = 4;
            } else {
                BLUETOOTH_ADAPTER_STATUS = 2;
                START_UP_STATUS = 3;
            }
        }

        public int getNumberOdConditions(){
            return START_UP_STATUS + 1;
        }
    }
}