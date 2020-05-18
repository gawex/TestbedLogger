package cz.vsb.cbe.tesdbed;

import android.Manifest;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothManager;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Handler;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;
import android.provider.Settings;
import android.view.Window;
import android.view.WindowManager;
import android.widget.ListView;
import android.widget.Toast;

public class StartUpActivity extends AppCompatActivity {

    public static final int BLUETOOTH_LOW_ENERGY = 0;
    public static final int PERMISSION = 1;
    public static final int BLUETOOTH = 2;
    public static final int START_UP = 3;

    private static final int SPLASH_TIME = 1000; //This is 3 seconds
    private static final int CANCEL_TIME = 10000; //This is 10 seconds
    private static final int REQUEST_ENABLE_BT = 1;
    private static final int MY_PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION = 0;

    private BluetoothAdapter bluetoothAdapter;

    private Handler finishAppHandler;

    private String [] permissions = {Manifest.permission.ACCESS_COARSE_LOCATION, Manifest.permission.BLUETOOTH, Manifest.permission.BLUETOOTH_ADMIN} ;

    private ListView startUpConditionsListView;

    public ConditionsListAdapter startUpConditionsListAdapter;

    private Runnable finishAppRunnable = new Runnable() {
        @Override
        public void run() {
            finish();
        }
    };

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        switch (requestCode) {
            case MY_PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION: {
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    startUpConditionsListAdapter.setCondition(PERMISSION, ConditionsListAdapter.PASS, getResources().getString(R.string.conditions_permissions));
                    startUpConditionsListAdapter.notifyDataSetChanged();
                    enableBluetoothAdapter();
                } else
                    if (shouldShowRequestPermissionRationale(Manifest.permission.ACCESS_COARSE_LOCATION)) {
                        permissionAndExplanationProcedure();
                    } else {
                    AlertDialog.Builder locationPermissionDeniedForeverDialogBuilder = new AlertDialog.Builder(StartUpActivity.this);
                    locationPermissionDeniedForeverDialogBuilder.setIcon(getDrawable(R.drawable.ic_testbed_id)); //TODO: Jiná ikona
                    locationPermissionDeniedForeverDialogBuilder.setTitle(R.string.location_permission_denied_forever_title);
                    locationPermissionDeniedForeverDialogBuilder.setMessage(R.string.location_permission_denied_forevr_message);
                    locationPermissionDeniedForeverDialogBuilder.setNegativeButton(R.string.location_permission_denied_forever_negative_button, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            finish();
                        }
                    });
                    locationPermissionDeniedForeverDialogBuilder.setPositiveButton(R.string.location_permission_denied_forever_positive_button, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            Intent intent = new Intent();
                            intent.setAction(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
                            Uri uri = Uri.fromParts("PACKAGE", StartUpActivity.this.getPackageName(), null);
                            intent.setData(uri);
                            startActivity(intent);
                            finish();
                        }
                    });
                    AlertDialog locationPermissionDeniedForeverDialog = locationPermissionDeniedForeverDialogBuilder.create();
                    locationPermissionDeniedForeverDialog.setCancelable(false);
                    locationPermissionDeniedForeverDialog.setCanceledOnTouchOutside(false);
                    locationPermissionDeniedForeverDialog.show();

                    startUpConditionsListAdapter.setCondition(PERMISSION, ConditionsListAdapter.FAIL, getResources().getString(R.string.conditions_permissions));
                    startUpConditionsListAdapter.notifyDataSetChanged();
                }
                return;
            }
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent intent) {
        super.onActivityResult(requestCode, resultCode, intent);
        if (requestCode == REQUEST_ENABLE_BT) {
            if (resultCode == RESULT_OK) {
                this.startUpConditionsListAdapter.setCondition(BLUETOOTH, ConditionsListAdapter.PASS, getResources().getString(R.string.conditions_bluetooth_adapter));
                this.startUpConditionsListAdapter.notifyDataSetChanged();
                continueApplication();
            } else {
                AlertDialog.Builder bluetoothDeniedDialogBuilder = new AlertDialog.Builder(StartUpActivity.this);
                bluetoothDeniedDialogBuilder.setIcon(getDrawable(R.drawable.ic_testbed_id)); //TODO: Jiná ikona
                bluetoothDeniedDialogBuilder.setTitle(R.string.bluetooth_denied_title);
                bluetoothDeniedDialogBuilder.setMessage(R.string.bluetooth_denied_message);
                bluetoothDeniedDialogBuilder.setNegativeButton(R.string.bluetooth_denied_negative_button, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        finish();
                    }
                });
                bluetoothDeniedDialogBuilder.setPositiveButton(R.string.bluetooth_denied_positive_button, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        enableBluetoothAdapter();
                        startUpConditionsListAdapter.setCondition(BLUETOOTH, ConditionsListAdapter.PROGRESS, getResources().getString(R.string.conditions_bluetooth_adapter));
                        startUpConditionsListAdapter.notifyDataSetChanged();
                        finishAppHandler.removeCallbacks(finishAppRunnable);
                    }
                });
                AlertDialog bluetoothDeniedDialog = bluetoothDeniedDialogBuilder.create();
                bluetoothDeniedDialog.setCancelable(false);
                bluetoothDeniedDialog.setCanceledOnTouchOutside(false);
                bluetoothDeniedDialog.show();
                finishAppHandler.postDelayed(finishAppRunnable, CANCEL_TIME);

                this.startUpConditionsListAdapter.setCondition(BLUETOOTH, ConditionsListAdapter.FAIL, getResources().getString(R.string.conditions_bluetooth_adapter));
                this.startUpConditionsListAdapter.notifyDataSetChanged();
            }
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
        setContentView(R.layout.activity_start_up);
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_NOSENSOR);

        finishAppHandler = new Handler();

        startUpConditionsListView = (ListView) findViewById(R.id.activity_discover_lsv_devices);
        startUpConditionsListAdapter = new ConditionsListAdapter(this.getLayoutInflater(), this, 4);
        startUpConditionsListView.setAdapter(startUpConditionsListAdapter);

        startUpConditionsListAdapter.setCondition(BLUETOOTH_LOW_ENERGY, ConditionsListAdapter.PROGRESS, getResources().getString(R.string.conditions_bluetooth_low_energy));
        startUpConditionsListAdapter.notifyDataSetChanged();

        if (!getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)) {
            startUpConditionsListAdapter.setCondition(BLUETOOTH_LOW_ENERGY, ConditionsListAdapter.FAIL, getResources().getString(R.string.conditions_bluetooth_low_energy));
            startUpConditionsListAdapter.notifyDataSetChanged();
            AlertDialog.Builder bleNotSupportedDialogBuilder = new AlertDialog.Builder(this);
            bleNotSupportedDialogBuilder.setIcon(getDrawable(R.drawable.ic_testbed_id)); //TODO: Jiná ikona
            bleNotSupportedDialogBuilder.setTitle(R.string.ble_not_supported_title);
            bleNotSupportedDialogBuilder.setMessage(R.string.ble_not_supported_message);
            bleNotSupportedDialogBuilder.setNeutralButton(R.string.ble_not_supported_neutral_button, new DialogInterface.OnClickListener() {
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
            bluetoothAdapter = bluetoothManager.getAdapter();
            startUpConditionsListAdapter.setCondition(BLUETOOTH_LOW_ENERGY, ConditionsListAdapter.PASS, getResources().getString(R.string.conditions_bluetooth_low_energy));
            startUpConditionsListAdapter.setCondition(PERMISSION, ConditionsListAdapter.PROGRESS, getResources().getString(R.string.conditions_permissions));
            startUpConditionsListAdapter.notifyDataSetChanged();
            permissionAndExplanationProcedure();
        }
    }

    private void permissionAndExplanationProcedure(){
        if (checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            if (shouldShowRequestPermissionRationale(Manifest.permission.ACCESS_COARSE_LOCATION)) {
                AlertDialog.Builder locationPermissionRequestDialogBuilder = new AlertDialog.Builder(this);
                locationPermissionRequestDialogBuilder.setIcon(getDrawable(R.drawable.ic_testbed_id)); //TODO: Jiná ikona
                locationPermissionRequestDialogBuilder.setTitle(R.string.location_permission_request_title);
                locationPermissionRequestDialogBuilder.setMessage(R.string.location_permission_request_message);
                locationPermissionRequestDialogBuilder.setPositiveButton(R.string.location_permission_request_positive_button, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            requestPermissions(permissions, MY_PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION);
                        }
                    });
                locationPermissionRequestDialogBuilder.setNegativeButton(R.string.location_permission_request_negative_button, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        AlertDialog.Builder locationPermissionDeniedDialogBuilder = new AlertDialog.Builder(StartUpActivity.this);
                        locationPermissionDeniedDialogBuilder.setIcon(getDrawable(R.drawable.ic_testbed_id)); //TODO: Jiná ikona
                        locationPermissionDeniedDialogBuilder.setTitle(R.string.location_permission_denied_title);
                        locationPermissionDeniedDialogBuilder.setMessage(R.string.location_permission_denied_message);
                        locationPermissionDeniedDialogBuilder.setNegativeButton(R.string.location_permission_denied_negative_button, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                finish();
                            }
                        });
                        locationPermissionDeniedDialogBuilder.setPositiveButton(R.string.location_permission_denied_positive_button, new DialogInterface.OnClickListener() {
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
            startUpConditionsListAdapter.setCondition(PERMISSION, ConditionsListAdapter.PASS, getResources().getString(R.string.conditions_permissions));
            startUpConditionsListAdapter.notifyDataSetChanged();
            enableBluetoothAdapter();
        }
    }

    private void enableBluetoothAdapter(){
        if (bluetoothAdapter == null || !bluetoothAdapter.isEnabled()) {
            Intent enableBluetoothIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableBluetoothIntent, REQUEST_ENABLE_BT);
            startUpConditionsListAdapter.setCondition(BLUETOOTH, ConditionsListAdapter.PROGRESS, getResources().getString(R.string.conditions_bluetooth_adapter));
        } else {
            startUpConditionsListAdapter.setCondition(BLUETOOTH, ConditionsListAdapter.PASS, getResources().getString(R.string.conditions_bluetooth_adapter));
            startUpConditionsListAdapter.notifyDataSetChanged();
            continueApplication();
        }
    }

    private void continueApplication(){
        final Intent intent = new Intent(StartUpActivity.this, DiscoverDevicesActivity.class);
        //final Intent intent = new Intent(StartUpActivity.this, TestActivity.class);
        startUpConditionsListAdapter.setCondition(START_UP, ConditionsListAdapter.PROGRESS, getResources().getString(R.string.conditions_start_application));
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                startUpConditionsListAdapter.setCondition(START_UP, ConditionsListAdapter.PASS, getResources().getString(R.string.conditions_start_application));
                startUpConditionsListAdapter.notifyDataSetChanged();
            }
        }, SPLASH_TIME - 300);

        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                startActivity(intent);
                finish();
            }
        }, SPLASH_TIME);
    }


}