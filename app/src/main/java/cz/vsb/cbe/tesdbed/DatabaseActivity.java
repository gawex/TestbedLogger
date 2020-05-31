package cz.vsb.cbe.tesdbed;

import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCharacteristic;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.pm.ActivityInfo;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.ListView;
import android.widget.Toast;

import com.google.android.material.bottomnavigation.BottomNavigationView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class DatabaseActivity extends AppCompatActivity {

    Fragment currentFragment = null;
    FragmentTransaction ft;

    private PedometerFragment pedometerFragment;
    private HeartRateFragment heartRateFragment;
    private TemperatureFragment temperatureFragment;

    public static final String TESTBED_DEVICE = "cz.vsb.cbe.testbed.TestbedDevice";

    private final static String TAG = DatabaseActivity.class.getSimpleName();
    public final static String STEPS =
            "cz.vsb.cbe.testbed.STEPS";

    private TestbedDevice testbedDevice;

    private BluetoothLeService bluetoothLeService;

    private BluetoothGatt bluetoothGatt;
    BluetoothGattCharacteristic characteristic;
    boolean enabled;

    private int iterator = 1;

    private TestbedDbHelper testbedDbHelper;
    private SQLiteDatabase writableTestbedDb;

    private AlertDialog conectingDialog;

    ConditionsListAdapter conditionsListAdapter;

    private static IntentFilter makeGattUpdateIntentFilter() {
        final IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(BluetoothLeService.ACTION_GATT_CONNECTED);
        intentFilter.addAction(BluetoothLeService.ACTION_GATT_SERVICES_DISCOVERED);
        intentFilter.addAction(BluetoothLeService.ACTION_GATT_DESCRIPTOR_WRITTEN);
        intentFilter.addAction(BluetoothLeService.ACTION_DATA_AVAILABLE);
        intentFilter.addAction(BluetoothLeService.STEP_DATA_AVAILABLE);
        intentFilter.addAction(BluetoothLeService.HEART_RATE_DATA_AVAILABLE);
        intentFilter.addAction(BluetoothLeService.TEMPERATURE_DATA_AVAILABLE);
        intentFilter.addAction(BluetoothLeService.ACTION_GATT_DISCONNECTED);
        return intentFilter;
    }

    private final ServiceConnection mServiceConnection = new ServiceConnection() {

        @Override
        public void onServiceConnected(ComponentName componentName, IBinder service) {
            bluetoothLeService = ((BluetoothLeService.LocalBinder) service).getService();
            if (!bluetoothLeService.initialize()) {
                Log.d(TAG, "Unable to initialize Bluetooth");

                finish();
            } else {

                registerReceiver(mGattUpdateReceiver, makeGattUpdateIntentFilter());
                if (bluetoothLeService != null) {
                    final boolean result = bluetoothLeService.connect(testbedDevice.getBluetoothDevice().getAddress());
                    Log.d(TAG, "Connect request result=" + result);
                }
            }

        }

        @Override
        public void onServiceDisconnected(ComponentName componentName) {
            bluetoothLeService = null;
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
                default:
                    return false;
            }
        }
    };


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_database);

        testbedDevice = getIntent().getExtras().getParcelable(TESTBED_DEVICE);

        Bundle bundle = new Bundle();
        bundle.putParcelable(BluetoothLeService.TESTBED_ID, testbedDevice);

        pedometerFragment = new PedometerFragment();
        heartRateFragment = new HeartRateFragment();
        temperatureFragment = new TemperatureFragment();

        pedometerFragment.setArguments(bundle);
        heartRateFragment.setArguments(bundle);
        temperatureFragment.setArguments(bundle);

        ft = getSupportFragmentManager().beginTransaction();
        currentFragment = temperatureFragment;
        ft.replace(R.id.activity_test_host_fragment, currentFragment);
        ft.commit();

        BottomNavigationView navigation = (BottomNavigationView) findViewById(R.id.nav_view);
        navigation.setSelectedItemId(R.id.navigation_notifications);
        navigation.setOnNavigationItemSelectedListener(mOnNavigationItemSelectedListener);



        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_NOSENSOR);

        setTitle("[" + testbedDevice.getBluetoothDevice().getAddress() + "] (" + String.format("%04X", testbedDevice.getDeviceId()) + ")");


        getSupportActionBar().setDisplayShowHomeEnabled(true);



        //write("Připraven k připojení: [" + testbedDevice.getBluetoothDevice().getAddress() + "] (" + String.format("%04X", testbedDevice.getDeviceId()) + ")");


        AlertDialog.Builder conectingDialogBuilder = new AlertDialog.Builder(this);
        //LayoutInflater layoutInflater = (LayoutInflater) this.getSystemService(LAYOUT_INFLATER_SERVICE);
        View conectingDialogBuilderView = getLayoutInflater().inflate(R.layout.dialog_list_view, null);
        ListView listView = (ListView) conectingDialogBuilderView.findViewById(R.id.dialog_list_view_lsv);
        conditionsListAdapter = new ConditionsListAdapter(getLayoutInflater(), getApplicationContext(), 3);
        listView.setAdapter(conditionsListAdapter);
        conectingDialogBuilder.setIcon(getDrawable(R.drawable.ic_testbed_id)); //TODO: Jiná ikona
        conectingDialogBuilder.setTitle("Připojování");
        conectingDialogBuilder.setView(conectingDialogBuilderView);
        conectingDialog = conectingDialogBuilder.create();
        conectingDialog.setCancelable(false);
        conectingDialog.setCanceledOnTouchOutside(false);
        conectingDialog.show();


        conditionsListAdapter.setCondition(0, ConditionsListAdapter.PROGRESS, "Připojování zařízení...");
        conditionsListAdapter.notifyDataSetChanged();

       /* Intent gattServiceIntent = new Intent(this, BluetoothLeService.class);
        gattServiceIntent.putExtra(BluetoothLeService.TESTBED_ID, testbedDevice);
        bindService(gattServiceIntent, mServiceConnection, BIND_AUTO_CREATE);*/
        startService(new Intent(this, BluetoothLeService.class));


    }

    private void writeDescriptor(int characteristic) {
        bluetoothLeService.setCharacteristicNotification(bluetoothLeService.getSupportedGattServices().get(2).getCharacteristics().get(characteristic), true);
    }

    private final BroadcastReceiver mGattUpdateReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            final String action = intent.getAction();
            if (BluetoothLeService.ACTION_GATT_CONNECTED.equals(action)) {
                conditionsListAdapter.setCondition(0, ConditionsListAdapter.PASS, "Připojeno");
                conditionsListAdapter.setCondition(1, ConditionsListAdapter.PROGRESS, "Probíhá objevování services...");
                conditionsListAdapter.notifyDataSetChanged();
            } else if (BluetoothLeService.ACTION_GATT_DISCONNECTED.equals(action)) {
                Toast.makeText(getApplicationContext(), "ODPOJENO!", Toast.LENGTH_SHORT).show();
            } else if (BluetoothLeService.ACTION_GATT_SERVICES_DISCOVERED.equals(action)) {
                conditionsListAdapter.setCondition(1, ConditionsListAdapter.PASS, "Services objeveny.");
                conditionsListAdapter.setCondition(2, ConditionsListAdapter.PROGRESS, "Probíhá příprava na zápis descriptorů (1 z 3)");
                conditionsListAdapter.notifyDataSetChanged();
                //conectingDialog.setMessage("Objeveny dostupné sevices u: [" + testbedDevice.getBluetoothDevice().getAddress() + "] (" + String.format("%04X", testbedDevice.getDeviceId()) + ")");
                //conectingDialog.show();
                new Handler().postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        writeDescriptor(iterator);
                        conditionsListAdapter.setCondition(2, ConditionsListAdapter.PROGRESS, "Probíhá zápis descriptorů (1 z 3)");
                        conditionsListAdapter.notifyDataSetChanged();
                    }
                }, 2000);

            } else if (BluetoothLeService.ACTION_GATT_DESCRIPTOR_WRITTEN.equals(action)) {
                //conectingDialog.setMessage("Nastaveno přijímání notifikací u: [" + testbedDevice.getBluetoothDevice().getAddress() + "] (" + String.format("%04X", testbedDevice.getDeviceId()) + ") na service: " + SampleGattAttributes.lookupFromUUID(SampleGattAttributes.TESTBED_SERVICE, "unknown)") + ", characteristic: " + iterator);
                //conectingDialog.show();
                iterator++;
                if (iterator <= 3) {
                    writeDescriptor(iterator);
                    conditionsListAdapter.setCondition(2, ConditionsListAdapter.PROGRESS, "Probíhá zápis descriptorů (" + iterator + " z 3)");
                    conditionsListAdapter.notifyDataSetChanged();
                } else {
                    conditionsListAdapter.setCondition(2, ConditionsListAdapter.PASS, "Zápis descriptorů dokončen.");
                    conditionsListAdapter.notifyDataSetChanged();
                    conectingDialog.hide();
                    conectingDialog.cancel();

                }
                ;//conectingDialog.hide();

            } else if (BluetoothLeService.STEP_DATA_AVAILABLE.equals(action)) {


            } else if (BluetoothLeService.HEART_RATE_DATA_AVAILABLE.equals(action)) {

            } else if (BluetoothLeService.TEMPERATURE_DATA_AVAILABLE.equals(action)) {
                if(currentFragment.equals(temperatureFragment)){
                    temperatureFragment.funkce();
                }

            } else if (BluetoothLeService.ACTION_DATA_AVAILABLE.equals(action)) {

                byte[] data = intent.getByteArrayExtra(BluetoothLeService.EXTRA_DATA);
                //String data = intent.getStringExtra(BluetoothLeService.EXTRA_DATA);
                SimpleDateFormat sdf = new SimpleDateFormat("HH:mm:ss.SSS dd-MM-yyyy", Locale.getDefault());
                String str = intent.getAction();


                //bluetoothLeService.disconnect();
                /*TestbedDevice testbedDevice = new TestbedDevice();
                testbedDevice = devicesListAdapter.getDevice(discoveringDeviceIndex);
                testbedDevice.addAvailableSensors(data[0]);
                testbedDevice.addDeviceId(((data[1]& 0xFF) <<8) + (data[2]& 0xFF));
                devicesListAdapter.setDevice(testbedDevice);
                devicesListAdapter.notifyDataSetChanged();
                bluetoothLeService.disconnect();
                discoveringDeviceIndex++;*/
            }
        }
    };

    @Override
    protected void onResume() {
        super.onResume();
        Intent gattServiceIntent = new Intent(this, BluetoothLeService.class);
        gattServiceIntent.putExtra(BluetoothLeService.TESTBED_ID, testbedDevice);
        bindService(gattServiceIntent, mServiceConnection, BIND_AUTO_CREATE);

        //testbedDbHelper = new TestbedDbHelper(getApplicationContext());
        //writableTestbedDb = testbedDbHelper.getWritableDatabase();
    }

    @Override
    protected void onPause() {
        super.onPause();
        //unregisterReceiver(mGattUpdateReceiver);
        //unbindService(mServiceConnection);
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
        //unbindService(mServiceConnection);
        //bluetoothLeService=null;
        Log.w(TAG, "Zniceno");


    }


    }



