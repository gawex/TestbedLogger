package cz.vsb.cbe.tesdbed;

import android.app.PendingIntent;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattService;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.pm.ActivityInfo;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Messenger;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.ListView;
import android.widget.Toast;

import com.google.android.material.bottomnavigation.BottomNavigationView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;

import cz.vsb.cbe.tesdbed.sql.TestbedDatabase;

public class DatabaseActivity extends AppCompatActivity {

    Fragment currentFragment = null;
    FragmentTransaction ft;

    private PedometerFragment pedometerFragment;
    private HeartRateFragment heartRateFragment;
    private TemperatureFragment temperatureFragment;
    private TestFragment testFragment;

    public static final String TESTBED_DEVICE = "cz.vsb.cbe.testbed.TESTBED_DEVICE";

    private final static String TAG = DatabaseActivity.class.getSimpleName();


    private TestbedDevice testbedDevice;

    private BluetoothLeService bluetoothLeService;

    Intent gattServiceIntent;

    private int iterator = 1;



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

    Messenger messenger = null;

    private final ServiceConnection mServiceConnection = new ServiceConnection() {

        @Override
        public void onServiceConnected(ComponentName componentName, IBinder service) {
            bluetoothLeService = ((BluetoothLeService.LocalBinder) service).getService();


            bluetoothLeService.setTestbedDevice(testbedDevice);

            if (!bluetoothLeService.initialize()) {
                Log.d(TAG, "Unable to initialize Bluetooth");

                finish();
            } else {

                registerReceiver(mGattUpdateReceiverForConnect, makeGattUpdateIntentFilterForConnect());
                Log.i(TAG, "bindnul jsem se");
                if (bluetoothLeService != null) {
                    final boolean result = bluetoothLeService.connect(testbedDevice.getBluetoothDevice().getAddress());
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
                case R.id.navigation_home:
                    currentFragment = pedometerFragment;
                    ft = getSupportFragmentManager().beginTransaction();
                    ft.replace(R.id.activity_test_host_fragment, currentFragment);
                    ft.commit();
                    return true;
                case R.id.navigation_dashboard:
                    currentFragment = heartRateFragment;
                    ft = getSupportFragmentManager().beginTransaction();
                    ft.replace(R.id.activity_test_host_fragment, currentFragment);
                    ft.commit();
                    return true;
                case R.id.navigation_notifications:
                    currentFragment = temperatureFragment;
                    ft = getSupportFragmentManager().beginTransaction();
                    ft.replace(R.id.activity_test_host_fragment, currentFragment);
                    ft.commit();
                    return true;
                case R.id.navigation_temperature_graph:
                    currentFragment = testFragment;
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


        gattServiceIntent = new Intent(this, BluetoothLeService.class);

        testbedDevice = getIntent().getExtras().getParcelable(TESTBED_DEVICE);

        Bundle bundle = new Bundle();
        bundle.putParcelable(TESTBED_DEVICE, testbedDevice);

        pedometerFragment = new PedometerFragment();
        heartRateFragment = new HeartRateFragment();
        temperatureFragment = new TemperatureFragment();
        testFragment = new TestFragment();

        pedometerFragment.setArguments(bundle);
        heartRateFragment.setArguments(bundle);
        temperatureFragment.setArguments(bundle);
        testFragment.setArguments(bundle);

        ft = getSupportFragmentManager().beginTransaction();
        currentFragment = testFragment;
        ft.replace(R.id.activity_test_host_fragment, currentFragment);
        ft.commit();

        BottomNavigationView navigation = (BottomNavigationView) findViewById(R.id.nav_view);
        navigation.setSelectedItemId(R.id.navigation_temperature_graph);
        navigation.setOnNavigationItemSelectedListener(mOnNavigationItemSelectedListener);



        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_NOSENSOR);

        setTitle("[" + testbedDevice.getBluetoothDevice().getAddress() + "] (" + String.format("%04X", testbedDevice.getDeviceId()) + ")");


        getSupportActionBar().setDisplayShowHomeEnabled(true);

    }

    List<BluetoothGattCharacteristic> charWithNotif = new ArrayList<>();

    private final BroadcastReceiver mGattUpdateReceiverForConnect = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            final String action = intent.getAction();
            if (BluetoothLeService.ACTION_GATT_CONNECTED.equals(action)) {
                Toast.makeText(getApplicationContext(), "Znovu připojeno", Toast.LENGTH_SHORT).show();

            } else if (BluetoothLeService.ACTION_GATT_DISCONNECTED.equals(action)) {
                Toast.makeText(getApplicationContext(), "ODPOJENO!", Toast.LENGTH_SHORT).show();
                bluetoothLeService.connect(testbedDevice.getBluetoothDevice().getAddress());
            } else if (BluetoothLeService.ACTION_GATT_SERVICES_DISCOVERED.equals(action)) {


            } else if (BluetoothLeService.ACTION_GATT_DESCRIPTOR_WRITTEN.equals(action)) {


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

        Log.i(TAG, testbedDevice.getBluetoothDevice().getAddress());
        gattServiceIntent.putExtra(DatabaseActivity.TESTBED_DEVICE, testbedDevice);
        boolean b = bindService(gattServiceIntent, mServiceConnection, BIND_ADJUST_WITH_ACTIVITY);

        // Create an explicit intent for an Activity in your app
        Intent intent = new Intent(this, DatabaseActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, intent, 0);

    }
    @Override
    protected void onPause() {
        super.onPause();

        //bluetoothLeService.disconnect();
        this.unbindService(mServiceConnection);
        //stopService(gattServiceIntent);
        Log.w(TAG, "Pausnuto");

    }

    @Override
    public void onStop() {
        super.onStop();
        //unbindService(mServiceConnection);
        //bluetoothLeService.stopSelf();
        Log.w(TAG, "Stopnuto");

    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
//        unbindService(mServiceConnection);
        stopService(new Intent(this, BluetoothLeService.class));
        bluetoothLeService=null;

        Log.w(TAG, "Zniceno");
    }

}



