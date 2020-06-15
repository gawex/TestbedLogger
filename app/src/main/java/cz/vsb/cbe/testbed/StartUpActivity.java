package cz.vsb.cbe.testbed;

import android.Manifest;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.location.LocationManager;
import android.net.Uri;
import android.os.Build;
import android.os.Handler;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.location.LocationManagerCompat;

import android.os.Bundle;
import android.provider.Settings;
import android.view.View;
import android.view.WindowManager.LayoutParams;
import android.widget.ListView;

import org.jetbrains.annotations.NotNull;

import java.util.Objects;

public class StartUpActivity extends AppCompatActivity {

    private static final int SPLASH_TIME = 1000; //This is 3 seconds
    private static final int CANCEL_TIME = 15000; //This is 10 seconds
    private static final int REQUEST_ENABLE_BT = 1;
    private static final int MY_PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION = 0;

    private static final int PERMISSION_CHECK_STATE = 0;
    private static final int LOCATION_CHECK_STATE = 1;

    private int activityState = -1;

    private BluetoothAdapter bluetoothAdapter;

    private Handler finishAppHandler;

    private final String [] permissions = {Manifest.permission.ACCESS_FINE_LOCATION} ;

    private ConditionsListAdapter startUpConditionsListAdapter;

    private final Runnable finishAppRunnable = new Runnable() {
        @Override
        public void run() {
            finish();
        }
    };

    @Override
    public void onRequestPermissionsResult(int requestCode, @NotNull String[] permissions, @NotNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == MY_PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                startUpConditionsListAdapter.setCondition(new StartUpConditionsOrder().PERMISSION, ConditionsListAdapter.PASS, getResources().getString(R.string.activity_start_up_condition_permissions));
                startUpConditionsListAdapter.notifyDataSetChanged();
                enableLocation();
            } else if (shouldShowRequestPermissionRationale(Manifest.permission.ACCESS_FINE_LOCATION)) {
                permissionAndExplanationProcedure();
            } else {
                AlertDialog.Builder locationPermissionDeniedForeverDialogBuilder = new AlertDialog.Builder(StartUpActivity.this);
                locationPermissionDeniedForeverDialogBuilder.setIcon(getDrawable(R.drawable.ic_testbed_id)); //TODO: Jiná ikona
                locationPermissionDeniedForeverDialogBuilder.setTitle(R.string.dialog_title_location_permission_denied_forever);
                locationPermissionDeniedForeverDialogBuilder.setMessage(R.string.dialog_message_location_permission_denied_forever);
                locationPermissionDeniedForeverDialogBuilder.setNegativeButton(R.string.dialog_button_negative_do_not_accept, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        finish();
                    }
                });
                locationPermissionDeniedForeverDialogBuilder.setPositiveButton(R.string.dialog_button_positive_go_to_settings, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        Intent intent = new Intent();
                        intent.setAction(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
                        intent.setData(Uri.parse("package:" + StartUpActivity.this.getPackageName()));
                        activityState = PERMISSION_CHECK_STATE;
                        startActivity(intent);
                    }
                });
                AlertDialog locationPermissionDeniedForeverDialog = locationPermissionDeniedForeverDialogBuilder.create();
                locationPermissionDeniedForeverDialog.setCancelable(false);
                locationPermissionDeniedForeverDialog.setCanceledOnTouchOutside(false);
                locationPermissionDeniedForeverDialog.show();

                startUpConditionsListAdapter.setCondition(new StartUpConditionsOrder().PERMISSION, ConditionsListAdapter.FAIL, getResources().getString(R.string.activity_start_up_condition_permissions));
                startUpConditionsListAdapter.notifyDataSetChanged();
            }
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent intent) {
        super.onActivityResult(requestCode, resultCode, intent);
        if (requestCode == REQUEST_ENABLE_BT) {
            if (resultCode == RESULT_OK) {
                this.startUpConditionsListAdapter.setCondition(new StartUpConditionsOrder().BLUETOOTH, ConditionsListAdapter.PASS, getResources().getString(R.string.activity_start_up_condition_bluetooth_adapter));
                this.startUpConditionsListAdapter.notifyDataSetChanged();
                continueApplication();
            } else {
                AlertDialog.Builder bluetoothDeniedDialogBuilder = new AlertDialog.Builder(StartUpActivity.this);
                bluetoothDeniedDialogBuilder.setIcon(getDrawable(R.drawable.ic_testbed_id)); //TODO: Jiná ikona
                bluetoothDeniedDialogBuilder.setTitle(R.string.dialog_title_bluetooth_denied);
                bluetoothDeniedDialogBuilder.setMessage(R.string.dialog_message_bluetooth_denied);
                bluetoothDeniedDialogBuilder.setNegativeButton(R.string.dialog_button_negative_do_not_accept, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        finish();
                    }
                });
                bluetoothDeniedDialogBuilder.setPositiveButton(R.string.dialog_button_positive_turn_on_bluetooth, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        enableBluetoothAdapter();
                        startUpConditionsListAdapter.setCondition(new StartUpConditionsOrder().BLUETOOTH, ConditionsListAdapter.PROGRESS, getResources().getString(R.string.activity_start_up_condition_bluetooth_adapter));
                        startUpConditionsListAdapter.notifyDataSetChanged();
                        finishAppHandler.removeCallbacks(finishAppRunnable);
                    }
                });
                AlertDialog bluetoothDeniedDialog = bluetoothDeniedDialogBuilder.create();
                bluetoothDeniedDialog.setCancelable(false);
                bluetoothDeniedDialog.setCanceledOnTouchOutside(false);
                bluetoothDeniedDialog.show();
                finishAppHandler.postDelayed(finishAppRunnable, CANCEL_TIME);

                this.startUpConditionsListAdapter.setCondition(new StartUpConditionsOrder().BLUETOOTH, ConditionsListAdapter.FAIL, getResources().getString(R.string.activity_start_up_condition_bluetooth_adapter));
                this.startUpConditionsListAdapter.notifyDataSetChanged();
            }
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
            layoutParams.layoutInDisplayCutoutMode = LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES;
        }

        setContentView(R.layout.activity_start_up);
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_NOSENSOR);

        createNotificationChannel(getString(R.string.notification_channel_id));

        finishAppHandler = new Handler();
        ListView startUpConditionsListView = findViewById(R.id.activity_discover_lsv_devices);
        startUpConditionsListAdapter = new ConditionsListAdapter(this.getLayoutInflater(), this, new StartUpConditionsOrder().getNumberOdConditions());
        startUpConditionsListView.setAdapter(startUpConditionsListAdapter);

        startUpConditionsListAdapter.setCondition(new StartUpConditionsOrder().BLUETOOTH_LOW_ENERGY, ConditionsListAdapter.PROGRESS, getResources().getString(R.string.activity_start_up_condition_bluetooth_low_energy));
        startUpConditionsListAdapter.notifyDataSetChanged();

        if (!getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)) {
            startUpConditionsListAdapter.setCondition(new StartUpConditionsOrder().BLUETOOTH_LOW_ENERGY, ConditionsListAdapter.FAIL, getResources().getString(R.string.activity_start_up_condition_bluetooth_low_energy));
            startUpConditionsListAdapter.notifyDataSetChanged();
            AlertDialog.Builder bleNotSupportedDialogBuilder = new AlertDialog.Builder(this);
            bleNotSupportedDialogBuilder.setIcon(getDrawable(R.drawable.ic_testbed_id)); //TODO: Jiná ikona
            bleNotSupportedDialogBuilder.setTitle(R.string.dialog_title_ble_not_supported);
            bleNotSupportedDialogBuilder.setMessage(R.string.dialog_message_ble_not_supported);
            bleNotSupportedDialogBuilder.setNeutralButton(R.string.dialog_button_neutral_do_not_trust, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    finish();
                }
            });
            AlertDialog bleNotSupportedDialog = bleNotSupportedDialogBuilder.create();
            bleNotSupportedDialog.show();
            finishAppHandler.postDelayed(finishAppRunnable, CANCEL_TIME);

        } else {
            final BluetoothManager bluetoothManager = (BluetoothManager) getSystemService(BLUETOOTH_SERVICE);
            bluetoothAdapter = Objects.requireNonNull(bluetoothManager).getAdapter();
            startUpConditionsListAdapter.setCondition(new StartUpConditionsOrder().BLUETOOTH_LOW_ENERGY, ConditionsListAdapter.PASS, getResources().getString(R.string.activity_start_up_condition_bluetooth_low_energy));
            startUpConditionsListAdapter.setCondition(new StartUpConditionsOrder().PERMISSION, ConditionsListAdapter.PROGRESS, getResources().getString(R.string.activity_start_up_condition_permissions));
            startUpConditionsListAdapter.notifyDataSetChanged();
            permissionAndExplanationProcedure();
        }
    }

    private void permissionAndExplanationProcedure(){
        if (checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            if (shouldShowRequestPermissionRationale(Manifest.permission.ACCESS_FINE_LOCATION)) {
                AlertDialog.Builder locationPermissionRequestDialogBuilder = new AlertDialog.Builder(this);
                locationPermissionRequestDialogBuilder.setIcon(getDrawable(R.drawable.ic_testbed_id)); //TODO: Jiná ikona
                locationPermissionRequestDialogBuilder.setTitle(R.string.dialog_title_location_permission_request);
                locationPermissionRequestDialogBuilder.setMessage(R.string.dialog_message_location_permission_request);
                locationPermissionRequestDialogBuilder.setPositiveButton(R.string.dialog_button_positive_yes_understand, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            requestPermissions(permissions, MY_PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION);
                        }
                    });
                locationPermissionRequestDialogBuilder.setNegativeButton(R.string.dialog_button_negative_do_not_accept, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        AlertDialog.Builder locationPermissionDeniedDialogBuilder = new AlertDialog.Builder(StartUpActivity.this);
                        locationPermissionDeniedDialogBuilder.setIcon(getDrawable(R.drawable.ic_testbed_id)); //TODO: Jiná ikona
                        locationPermissionDeniedDialogBuilder.setTitle(R.string.dialog_title_location_permission_denied);
                        locationPermissionDeniedDialogBuilder.setMessage(R.string.dialog_message_location_permission_denied);
                        locationPermissionDeniedDialogBuilder.setNegativeButton(R.string.dialog_button_negative_do_not_accept, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                finish();
                            }
                        });
                        locationPermissionDeniedDialogBuilder.setPositiveButton(R.string.dialog_button_positive_changed_my_mind, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                requestPermissions(permissions, MY_PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION);
                                finishAppHandler.removeCallbacks(finishAppRunnable);
                            }
                        });
                        AlertDialog locationPermissionDeniedDialog = locationPermissionDeniedDialogBuilder.create();
                        locationPermissionDeniedDialog.setCancelable(false);
                        locationPermissionDeniedDialog.setCanceledOnTouchOutside(false);
                        locationPermissionDeniedDialog.show();
                        finishAppHandler.postDelayed(finishAppRunnable, CANCEL_TIME);
                    }
                });
                AlertDialog locationPermissionRequestDialog = locationPermissionRequestDialogBuilder.create();
                locationPermissionRequestDialog.setCancelable(false);
                locationPermissionRequestDialog.setCanceledOnTouchOutside(false);
                locationPermissionRequestDialog.show();
            } else {
                requestPermissions(permissions, MY_PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION);
            }
        } else {
            startUpConditionsListAdapter.setCondition(new StartUpConditionsOrder().PERMISSION, ConditionsListAdapter.PASS, getResources().getString(R.string.activity_start_up_condition_permissions));
            startUpConditionsListAdapter.notifyDataSetChanged();
            enableLocation();
        }
    }

    private void enableLocation(){
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q){
            startUpConditionsListAdapter.setCondition(new StartUpConditionsOrder().LOCATION, ConditionsListAdapter.PROGRESS, getResources().getString(R.string.activity_start_up_condition_location));
            startUpConditionsListAdapter.notifyDataSetChanged();
            if (LocationManagerCompat.isLocationEnabled((LocationManager) Objects.requireNonNull(getSystemService(Context.LOCATION_SERVICE)))) {
                startUpConditionsListAdapter.setCondition(new StartUpConditionsOrder().LOCATION, ConditionsListAdapter.PASS, getResources().getString(R.string.activity_start_up_condition_location));
                startUpConditionsListAdapter.notifyDataSetChanged();
                enableBluetoothAdapter();
            } else {
                startUpConditionsListAdapter.setCondition(new StartUpConditionsOrder().LOCATION, ConditionsListAdapter.FAIL, getResources().getString(R.string.activity_start_up_condition_location));
                startUpConditionsListAdapter.notifyDataSetChanged();
                AlertDialog.Builder locationRequestDialogBuilder = new AlertDialog.Builder(this);
                locationRequestDialogBuilder.setIcon(getDrawable(R.drawable.ic_testbed_id)); //TODO: Jiná ikona
                locationRequestDialogBuilder.setTitle(R.string.dialog_title_location_request);
                locationRequestDialogBuilder.setMessage(R.string.dialog_message_location_request);
                locationRequestDialogBuilder.setPositiveButton(R.string.dialog_button_positive_turn_on_location, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        finishAppHandler.removeCallbacks(finishAppRunnable);
                        activityState = LOCATION_CHECK_STATE;
                        startActivity(new Intent(android.provider.Settings.ACTION_LOCATION_SOURCE_SETTINGS));
                    }
                });
                locationRequestDialogBuilder.setNegativeButton(R.string.dialog_button_negative_do_not_accept, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        finishAppHandler.removeCallbacks(finishAppRunnable);
                        AlertDialog.Builder locationRequestDeniedDialogBuilder = new AlertDialog.Builder(StartUpActivity.this);
                        locationRequestDeniedDialogBuilder.setIcon(getDrawable(R.drawable.ic_testbed_id)); //TODO: Jiná ikona
                        locationRequestDeniedDialogBuilder.setTitle(R.string.dialog_title_location_denied);
                        locationRequestDeniedDialogBuilder.setMessage(R.string.dialog_message_location_denied);
                        locationRequestDeniedDialogBuilder.setPositiveButton(R.string.dialog_button_positive_turn_on_location, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                finishAppHandler.removeCallbacks(finishAppRunnable);
                                activityState = LOCATION_CHECK_STATE;
                                startActivity(new Intent(android.provider.Settings.ACTION_LOCATION_SOURCE_SETTINGS));
                            }
                        });
                        locationRequestDeniedDialogBuilder.setNegativeButton(R.string.dialog_button_neutral_do_not_trust, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                finishAppHandler.removeCallbacks(finishAppRunnable);
                                finish();
                            }
                        });
                        AlertDialog locationRequestDeniedDialog = locationRequestDeniedDialogBuilder.create();
                        locationRequestDeniedDialog.setCancelable(false);
                        locationRequestDeniedDialog.setCanceledOnTouchOutside(false);
                        locationRequestDeniedDialog.show();
                        finishAppHandler.postDelayed(finishAppRunnable, CANCEL_TIME);

                    }
                });
                AlertDialog locationRequestDialog = locationRequestDialogBuilder.create();
                locationRequestDialog.setCancelable(false);
                locationRequestDialog.setCanceledOnTouchOutside(false);
                locationRequestDialog.show();
                finishAppHandler.postDelayed(finishAppRunnable, CANCEL_TIME);
            }
        } else{
            enableBluetoothAdapter();
        }
    }

    private void enableBluetoothAdapter(){
        if (bluetoothAdapter == null || !bluetoothAdapter.isEnabled()) {
            startActivityForResult(new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE), REQUEST_ENABLE_BT);
            startUpConditionsListAdapter.setCondition(new StartUpConditionsOrder().BLUETOOTH, ConditionsListAdapter.PROGRESS, getResources().getString(R.string.activity_start_up_condition_bluetooth_adapter));
        } else {
            startUpConditionsListAdapter.setCondition(new StartUpConditionsOrder().BLUETOOTH, ConditionsListAdapter.PASS, getResources().getString(R.string.activity_start_up_condition_bluetooth_adapter));
            startUpConditionsListAdapter.notifyDataSetChanged();
            continueApplication();
        }
    }

    private void continueApplication(){
        startUpConditionsListAdapter.setCondition(new StartUpConditionsOrder().START_UP, ConditionsListAdapter.PROGRESS, getResources().getString(R.string.activity_start_up_condition_start_application));
        startService(new Intent(StartUpActivity.this, BluetoothLeService.class));
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                startUpConditionsListAdapter.setCondition(new StartUpConditionsOrder().START_UP, ConditionsListAdapter.PASS, getResources().getString(R.string.activity_start_up_condition_start_application));
                startUpConditionsListAdapter.notifyDataSetChanged();
            }
        }, SPLASH_TIME - 300);

        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                startActivity(new Intent(StartUpActivity.this, DiscoverDevicesActivity.class));
                finish();
            }
        }, SPLASH_TIME);
    }

    private void createNotificationChannel(String channelId) {
        // Create the NotificationChannel, but only on API 26+ because
        // the NotificationChannel class is new and not in the support library
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            String  name = getString(R.string.notification_chanel_logging_name);
            String description = getString(R.string.notification_chanel_logging_description);
            int importance = NotificationManager.IMPORTANCE_DEFAULT;
            NotificationChannel channel = new NotificationChannel(channelId, name, importance);
            channel.setDescription(description);
            // Register the channel with the system; you can't change the importance
            // or other notification behaviors after this
            NotificationManager notificationManager = getSystemService(NotificationManager.class);
            Objects.requireNonNull(notificationManager).createNotificationChannel(channel);
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
        if(activityState == PERMISSION_CHECK_STATE){
            permissionAndExplanationProcedure();
        } else if (activityState == LOCATION_CHECK_STATE ){
            enableLocation();
        }
    }

    public static final class StartUpConditionsOrder {

        public final int BLUETOOTH_LOW_ENERGY = 0;
        public final int PERMISSION = 1;
        public int LOCATION;
        public final int BLUETOOTH;
        public final int START_UP;

        public StartUpConditionsOrder(){
            if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q){
                LOCATION = 2;
                BLUETOOTH = 3;
                START_UP = 4;
            } else {
                BLUETOOTH = 2;
                START_UP = 3;
            }
        }

        public int getNumberOdConditions(){
            return START_UP + 1;
        }
    }

}