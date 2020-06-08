package cz.vsb.cbe.testbed;

import android.app.PendingIntent;
import android.bluetooth.BluetoothGattCharacteristic;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.pm.ActivityInfo;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ListView;

import com.google.android.material.bottomnavigation.BottomNavigationView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;

import java.math.BigInteger;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;

public class DatabaseActivity extends AppCompatActivity {

    private static final int CONNECTION_STATUS = 0;
    private static final int SERVICE_DISCOVERING_STATUS = 1;
    private static final int DESCRIPTOR_WRITTEN_STATUS = 2;

    private static final int SPLASH_TIME = 500; //This is 0.5 seconds

    Fragment currentFragment = null;
    FragmentTransaction ft;

    private PedometerFragment pedometerFragment;
    private HeartRateFragment heartRateFragment;
    private TemperatureFragment temperatureFragment;


    public static final String TESTBED_DEVICE = "cz.vsb.cbe.testbed.TESTBED_DEVICE";

    private final static String TAG = DatabaseActivity.class.getSimpleName();

    private int totalDescriptorForWrite = 0;
    private int descriptorWriteIndex = 0;

    private TestbedDevice TestbedDevice;

    private BluetoothLeService bluetoothLeService;

    private ConditionsListAdapter conditionsListAdapter;
    private AlertDialog connectingDialog;

    private SimpleDateFormat systemTimeFormat = new SimpleDateFormat("HH:mm:ss");

    private Handler systemTimeHandler;

    Runnable currentTime = new Runnable() {
        @Override
        public void run() {
            invalidateOptionsMenu();
            systemTimeHandler.postDelayed(currentTime, 1000);
        }
    };

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main_menu, menu);
        systemTimeHandler = new Handler();
        systemTimeHandler.postDelayed(currentTime, 1000);
        return true;
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        MenuItem menuItem = menu.findItem(R.id.scaning_state);
        menuItem.setTitle(getString(R.string.activity_database_options_menu_title_system_time) + ": " + systemTimeFormat.format(System.currentTimeMillis()));
        return super.onPrepareOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.settings:
                startActivity(new Intent(this, SettingsActivity.class));
                return true;

            case R.id.about_application:
                final AlertDialog alertDialog = new AlertDialog.Builder(this)
                        .setIcon(getDrawable(R.drawable.ic_testbed_id))
                        .setTitle(getString(R.string.dialog_title_about_application))
                        .setMessage(getString(R.string.dialog_message_about_application))
                        .setNeutralButton(getString(R.string.dialog_button_neutral_close), new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {

                            }
                        })
                        .create();
                alertDialog.setCancelable(false);
                alertDialog.setCanceledOnTouchOutside(false);
                alertDialog.show();
                return true;

            default:
                return super.onOptionsItemSelected(item);
        }
    }


    private static IntentFilter makeGattUpdateIntentFilterForConnect() {
        final IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(BluetoothLeService.ACTION_GATT_CONNECTED);
        intentFilter.addAction(BluetoothLeService.ACTION_GATT_SERVICES_DISCOVERED);
        intentFilter.addAction(BluetoothLeService.ACTION_GATT_DESCRIPTOR_WRITTEN);
        intentFilter.addAction(BluetoothLeService.ACTION_STEP_DATA_AVAILABLE);
        intentFilter.addAction(BluetoothLeService.ACTION_HEART_RATE_DATA_AVAILABLE);
        intentFilter.addAction(BluetoothLeService.ACTION_TEMPERATURE_DATA_AVAILABLE);
        intentFilter.addAction(BluetoothLeService.ACTION_GATT_DISCONNECTED);
        return intentFilter;
    }

    private final ServiceConnection mServiceConnection = new ServiceConnection() {

        @Override
        public void onServiceConnected(ComponentName componentName, IBinder service) {
            bluetoothLeService = ((BluetoothLeService.LocalBinder) service).getService();


            bluetoothLeService.setTestbedDevice(TestbedDevice);
            bluetoothLeService.setAutoReconnectAndNotificationEnabled(true);

            if (!bluetoothLeService.initialize()) {
                Log.d(TAG, "Unable to initialize Bluetooth");

                finish();
            } else {


                Log.i(TAG, "bindnul jsem se");
                if (bluetoothLeService != null) {
                    final boolean result = bluetoothLeService.connect(TestbedDevice.getBluetoothDevice().getAddress());
                    Log.d(TAG, "Connect request result=" + result);
                }
            }

        }

        @Override
        public void onServiceDisconnected(ComponentName componentName) {
            bluetoothLeService = null;
            Log.i(TAG, "unbindnul jsem se");
        }
    };

    private BottomNavigationView.OnNavigationItemSelectedListener mOnNavigationItemSelectedListener
            = new BottomNavigationView.OnNavigationItemSelectedListener() {

        @Override
        public boolean onNavigationItemSelected(@NonNull MenuItem item) {
            switch (item.getItemId()) {
                case R.id.navigation_pedometer:
                    currentFragment = pedometerFragment;
                    ft = getSupportFragmentManager().beginTransaction();
                    ft.replace(R.id.activity_test_host_fragment, currentFragment);
                    ft.commit();
                    return true;
                case R.id.navigation_heart_rate:
                    currentFragment = heartRateFragment;
                    ft = getSupportFragmentManager().beginTransaction();
                    ft.replace(R.id.activity_test_host_fragment, currentFragment);
                    ft.commit();
                    return true;
                case R.id.navigation_temperature:
                    currentFragment = temperatureFragment;
                    ft = getSupportFragmentManager().beginTransaction();
                    ft.replace(R.id.activity_test_host_fragment, currentFragment);
                    ft.commit();
                    return true;
                default:
                    return false;
            }
        }
    };


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_database);
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_NOSENSOR);

        TestbedDevice = getIntent().getExtras().getParcelable(TESTBED_DEVICE);

        setTitle(getString(R.string.ble_devices_name) + " (#" + Integer.toHexString(TestbedDevice.getDeviceId()) + ")");

        BottomNavigationView navigation = (BottomNavigationView) findViewById(R.id.nav_view);
        navigation.setOnNavigationItemSelectedListener(mOnNavigationItemSelectedListener);

        Bundle bundle = new Bundle();
        bundle.putParcelable(TESTBED_DEVICE, TestbedDevice);

        if(BigInteger.valueOf(TestbedDevice.getAvailableSensors()).testBit(0)){
            temperatureFragment = new TemperatureFragment();
            temperatureFragment.setArguments(bundle);
            currentFragment = temperatureFragment;
            navigation.setSelectedItemId(R.id.navigation_temperature);
        } else {
            navigation.getMenu().removeItem(R.id.navigation_heart_rate);
        }

        if(BigInteger.valueOf(TestbedDevice.getAvailableSensors()).testBit(1)){
            heartRateFragment = new HeartRateFragment();
            heartRateFragment.setArguments(bundle);
            currentFragment = heartRateFragment;
            navigation.setSelectedItemId(R.id.navigation_heart_rate);
        } else {
            navigation.getMenu().removeItem(R.id.navigation_heart_rate);
        }

        if(BigInteger.valueOf(TestbedDevice.getAvailableSensors()).testBit(2)){
            pedometerFragment = new PedometerFragment();
            pedometerFragment.setArguments(bundle);
            currentFragment = pedometerFragment;
            navigation.setSelectedItemId(R.id.navigation_pedometer);
        } else {
            navigation.getMenu().removeItem(R.id.navigation_pedometer);
        }


        ft = getSupportFragmentManager().beginTransaction();
        ft.replace(R.id.activity_test_host_fragment, currentFragment);
        ft.commit();

        View dialogView = getLayoutInflater().inflate(R.layout.dialog_list_view, null);
        final ListView conditionsListView = dialogView.findViewById(R.id.dialog_list_view_lsv);
        conditionsListAdapter = new ConditionsListAdapter(getLayoutInflater(), getApplicationContext(), 3);
        conditionsListView.setAdapter(conditionsListAdapter);

        connectingDialog = new AlertDialog.Builder(this)
                .setView(dialogView)
                .setTitle(getString(R.string.dialog_title_reconnecting_progress))
                .setIcon(getDrawable(R.drawable.ic_testbed_id))
                .setNegativeButton(getString(R.string.dialog_button_negative_cancel), new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        bluetoothLeService.setAutoReconnectAndNotificationEnabled(false);
                        bluetoothLeService.removeTestbedDevice();
                        bluetoothLeService.disconnect();
                        finish();
                        /*
                        unregisterReceiver(connectingBroadcastReceiver);
                        conditionsListAdapter = new ConditionsListAdapter(getLayoutInflater(), getApplicationContext(), 3);
                        conditionsListView.setAdapter(conditionsListAdapter);
                        testbedDevicesListAdapter.clear();
                        testbedDevicesListAdapter.notifyDataSetChanged();
                        noDevicesFound.setVisibility(View.VISIBLE);*/
                    }
                })
                .create();
        connectingDialog.setCancelable(false);
        connectingDialog.setCanceledOnTouchOutside(false);

        totalDescriptorForWrite = Integer.bitCount(TestbedDevice.getAvailableSensors());





    }

    List<BluetoothGattCharacteristic> charWithNotif = new ArrayList<>();

    private final BroadcastReceiver mGattUpdateReceiverForConnect = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            final String action = intent.getAction();
            if (BluetoothLeService.ACTION_GATT_CONNECTED.equals(action)) {
                conditionsListAdapter.setCondition(CONNECTION_STATUS, ConditionsListAdapter.PASS, getString(R.string.dialog_condition_re_connecting_progress_connected) + ": " +
                        getString(R.string.ble_devices_name) + " (#" +
                        Integer.toHexString(TestbedDevice.getDeviceId()) + ")");
                conditionsListAdapter.setCondition(SERVICE_DISCOVERING_STATUS, ConditionsListAdapter.PROGRESS, getString(R.string.dialog_condition_re_connecting_progress_services_discovering));
                conditionsListAdapter.notifyDataSetChanged();
            } else if (BluetoothLeService.ACTION_GATT_DISCONNECTED.equals(action)) {
                connectingDialog.show();
                conditionsListAdapter.setCondition(CONNECTION_STATUS, ConditionsListAdapter.PROGRESS, getString(R.string.dialog_condition_reconnecting_progress_connecting) + ": " +
                        getString(R.string.ble_devices_name) + " (#" +
                        Integer.toHexString(TestbedDevice.getDeviceId()) + ")");
                conditionsListAdapter.notifyDataSetChanged();
            } else if (BluetoothLeService.ACTION_GATT_SERVICES_DISCOVERED.equals(action)) {
                conditionsListAdapter.setCondition(SERVICE_DISCOVERING_STATUS, ConditionsListAdapter.PASS, getString(R.string.dialog_condition_re_connecting_progress_services_discovered));
                conditionsListAdapter.setCondition(DESCRIPTOR_WRITTEN_STATUS, ConditionsListAdapter.PROGRESS,  getString(R.string.dialog_condition_re_connecting_progress_descriptor_write_prepare) + " (" + totalDescriptorForWrite + ")");
                conditionsListAdapter.notifyDataSetChanged();
            } else if (BluetoothLeService.ACTION_GATT_DESCRIPTOR_WRITTEN.equals(action)) {
                descriptorWriteIndex++;
                conditionsListAdapter.setCondition(DESCRIPTOR_WRITTEN_STATUS, ConditionsListAdapter.PROGRESS,  getString(R.string.dialog_condition_re_connecting_progress_descriptor_writing) + " (" + descriptorWriteIndex + " " + getString(R.string.dialog_condition_re_connecting_progress_descriptor_writing_conjunction) + " " + totalDescriptorForWrite + ")");
                conditionsListAdapter.notifyDataSetChanged();
                if (descriptorWriteIndex == totalDescriptorForWrite) {
                    conditionsListAdapter.setCondition(2, ConditionsListAdapter.PASS, getString(R.string.dialog_condition_re_connecting_progress_descriptor_written));
                    conditionsListAdapter.notifyDataSetChanged();
                    new Handler().postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            connectingDialog.hide();
                            connectingDialog.cancel();
                        }
                    }, SPLASH_TIME);
                }

            } else if (BluetoothLeService.ACTION_STEP_DATA_AVAILABLE.equals(action)) {


            } else if (BluetoothLeService.ACTION_HEART_RATE_DATA_AVAILABLE.equals(action)) {

            } else if (BluetoothLeService.ACTION_TEMPERATURE_DATA_AVAILABLE.equals(action)) {
                if(currentFragment.equals(temperatureFragment)){
                    temperatureFragment.funkce();
                }

            } else if (BluetoothLeService.ACTION_TESTBED_ID_DATA_AVAILABLE.equals(action)) {

            }
        }
    };


    @Override
    protected void onResume() {
        super.onResume();
        bindService(new Intent(this, BluetoothLeService.class), mServiceConnection, BIND_ADJUST_WITH_ACTIVITY);
        registerReceiver(mGattUpdateReceiverForConnect, makeGattUpdateIntentFilterForConnect());
    }

    @Override
    protected void onPause() {
        super.onPause();
        unbindService(mServiceConnection);
        unregisterReceiver(mGattUpdateReceiverForConnect);
        Log.w(TAG, "Pausnuto");
    }

    @Override
    public void onStop() {
        super.onStop();
        Log.w(TAG, "Stopnuto");

    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        stopService(new Intent(this, BluetoothLeService.class));
        bluetoothLeService=null;
        Log.w(TAG, "Zniceno");
    }

}



