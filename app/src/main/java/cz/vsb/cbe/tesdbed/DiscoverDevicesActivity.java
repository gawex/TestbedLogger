package cz.vsb.cbe.tesdbed;

import android.bluetooth.BluetoothAdapter;
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
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.Handler;
import android.os.IBinder;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class DiscoverDevicesActivity extends AppCompatActivity {

    private final static String BLE_DEVICES_NAME = "Testbed";
    private static final long SCAN_PERIOD_IN_SECOND = 2;

    private static final int NOT_SCANNED_YET_STATE = 0;
    private static final int SCANNING_STATE = 1;
    private static final int DISCOVERING_STATE = 2;
    private static final int DISCOVERED_STATE = 3;

    private BluetoothAdapter bluetoothAdapter;
    private BluetoothLeScanner bluetoothLeScanner;

    private List<ScanFilter> scanFilters;
    private ScanFilter scanNameFilter;
    private ScanSettings scanSettings;

    private ListView devicesListView;
    private DevicesListAdapter devicesListAdapter;

    private int activityState = NOT_SCANNED_YET_STATE;
    private MenuItem scanningStateMenuItem;
    private Handler stopScanningAfterScanPeriodHandler;

    private BluetoothLeService bluetoothLeService;

    private int discoveringDeviceIndex = 0;

    private TestbedDbHelper testbedDbHelper;
    private SQLiteDatabase writabllestbedDb, readableTestbedDb;

    private TextView txv;

    private ProgressBar progressBar;


    private Runnable stopScanningAfterScanPeriodRunnable = new Runnable() {
        @Override
        public void run() {
            scanLeDevices(false);
        }
    };

    private CountDownTimer scanningCountDownTimer = new CountDownTimer(SCAN_PERIOD_IN_SECOND*1000, 1000) {

        public void onTick(long millisUntilFinished) {
            scanningStateMenuItem.setTitle(getResources().getString(R.string.activity_discover_devices_scanning_started) + " (" + ((millisUntilFinished / 1000) + 1) + ")");
        }

        public void onFinish() {
        }
    };

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main_menu, menu);
        scanningStateMenuItem = menu.findItem(R.id.scaning_state);
        scanLeDevices(true);

        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.scaning_state:
                switch (activityState){
                    case NOT_SCANNED_YET_STATE:
                        break;

                    case SCANNING_STATE:
                        scanLeDevices(false);
                        break;

                    case DISCOVERING_STATE:
                        unregisterReceiver(mGattUpdateReceiver);
                        activityState = DISCOVERED_STATE;
                        break;
                    case DISCOVERED_STATE:
                        scanLeDevices(true);
                        break;
                }
                return true;

            default:
                return super.onOptionsItemSelected(item);
        }
    }

    private final ServiceConnection mServiceConnection = new ServiceConnection() {

        @Override
        public void onServiceConnected(ComponentName componentName, IBinder service) {
            bluetoothLeService = ((BluetoothLeService.LocalBinder) service).getService();
            if (!bluetoothLeService.initialize()) {
                finish();
            }
        }

        @Override
        public void onServiceDisconnected(ComponentName componentName) {
            bluetoothLeService = null;
        }
    };

    private static IntentFilter makeGattUpdateIntentFilter() {
        final IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(BluetoothLeService.ACTION_GATT_CONNECTED);
        intentFilter.addAction(BluetoothLeService.ACTION_GATT_SERVICES_DISCOVERED);
        intentFilter.addAction(BluetoothLeService.ACTION_DATA_AVAILABLE);
        intentFilter.addAction(BluetoothLeService.ACTION_GATT_DISCONNECTED);
        return intentFilter;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_discover_devices);
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_NOSENSOR);

        Toolbar actionBar = (Toolbar) findViewById(R.id.activity_discover_devices_action_bar);
        actionBar.setTitle(getResources().getString(R.string.activity_discover_devices_action_bar_title));
        setSupportActionBar(actionBar);

        final BluetoothManager bluetoothManager = (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
        bluetoothAdapter = bluetoothManager.getAdapter();
        bluetoothLeScanner = bluetoothAdapter.getBluetoothLeScanner();

        scanFilters = new ArrayList<>();
        scanNameFilter = new ScanFilter.Builder()
                .setDeviceName(BLE_DEVICES_NAME)
                .build();
        scanFilters.add(scanNameFilter);

        scanSettings = new ScanSettings.Builder().build();

        stopScanningAfterScanPeriodHandler = new Handler();

        devicesListView = (ListView) findViewById(R.id.activity_discover_lsv_devices);
        devicesListAdapter = new DevicesListAdapter(this.getLayoutInflater(), getApplicationContext());
        devicesListView.setAdapter(devicesListAdapter);

        devicesListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                devicesListAdapter.getDevice(position).storeIntoDatabase();
                Intent intent = new Intent(DiscoverDevicesActivity.this, DatabaseActivity.class);
                intent.putExtra("TestbedDevice", devicesListAdapter.getDevice(position));
                //unbindService(mServiceConnection);
                //bluetoothLeService = null;
                //unbindService(mServiceConnection);
                //bluetoothLeService = null;
                startActivity(intent);
                //finish();
            }
        });



        progressBar = (ProgressBar) findViewById(R.id.activity_discover_devices_pgb_progress);
        progressBar.setVisibility(ProgressBar.VISIBLE);

        txv = (TextView) findViewById(R.id.activity_database_txv_info);
        txv.setText(TestbedDbHelper.DATABASE_NAME);
    }

    private void scanLeDevices(final boolean state) {
        if (state) {
            scanningStateMenuItem.setTitle(getResources().getString(R.string.activity_discover_devices_scanning_started) + " (" + SCAN_PERIOD_IN_SECOND + 1 + ")");
            devicesListAdapter.clear();
            devicesListAdapter.notifyDataSetChanged();
            bluetoothLeScanner.startScan(scanFilters,scanSettings, scanLeDevicesCallback);
            activityState = SCANNING_STATE;
            stopScanningAfterScanPeriodHandler.postDelayed(stopScanningAfterScanPeriodRunnable, SCAN_PERIOD_IN_SECOND*1000);
            scanningCountDownTimer.start();

        } else {
            //scanningStateMenuItem.setTitle(getResources().getString(R.string.activity_discover_devices_discovering_stopped));
            bluetoothLeScanner.stopScan(scanLeDevicesCallback);
            activityState = DISCOVERING_STATE;
            stopScanningAfterScanPeriodHandler.removeCallbacks(stopScanningAfterScanPeriodRunnable);
            scanningCountDownTimer.cancel();
            discoveringDeviceIndex = 0;
            discoverDevices();
        }
    }

    private ScanCallback scanLeDevicesCallback = new ScanCallback() {

        @Override
        public void onScanResult(int callbackType, ScanResult result) {
            TestbedDevice testbedDevice = new TestbedDevice(getApplicationContext());
            testbedDevice.addBluetoothDevice(result.getDevice());
            testbedDevice.addRssi(result.getRssi());
            devicesListAdapter.setDevice(testbedDevice);
            devicesListAdapter.notifyDataSetChanged();
        }

        @Override
        public void onScanFailed(int errorCode) {
            Toast.makeText(getApplicationContext(), getResources().getString(R.string.activity_discover_devices_scanning_failed) + errorCode, Toast.LENGTH_LONG).show();
            finish();
        }
    };

    private void setDiscoveringStateMenuItem(int currentProgress, int maxProgress){
        if (maxProgress >= currentProgress && scanningStateMenuItem != null)
            scanningStateMenuItem.setTitle(getResources().getString(R.string.activity_discover_devices_discovering_started) + " (" + currentProgress + " " + getResources().getString(R.string.activity_discover_devices_discovering_conjunction) + " " + maxProgress + ")");
    }

    private void discoverDevices(){

        if(discoveringDeviceIndex < devicesListAdapter.getCount()) {
            registerReceiver(mGattUpdateReceiver, makeGattUpdateIntentFilter());
            setDiscoveringStateMenuItem(discoveringDeviceIndex + 1,devicesListAdapter.getCount());
            if (bluetoothLeService != null) {
                bluetoothLeService.connect(devicesListAdapter.getDevice(discoveringDeviceIndex).getBluetoothDevice().getAddress());
            }
        } else {
            discoveringDeviceIndex = 0;
            activityState = DISCOVERED_STATE;
            scanningStateMenuItem.setTitle(getResources().getString(R.string.activity_discover_devices_discovering_stopped));
            progressBar.setVisibility(ProgressBar.INVISIBLE);
        }

    }

    private final BroadcastReceiver mGattUpdateReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            final String action = intent.getAction();
            if (BluetoothLeService.ACTION_GATT_CONNECTED.equals(action)) {

            } else if (BluetoothLeService.ACTION_GATT_DISCONNECTED.equals(action)) {
                unregisterReceiver(mGattUpdateReceiver);
                discoverDevices();
            } else if (BluetoothLeService.ACTION_GATT_SERVICES_DISCOVERED.equals(action)) {
                bluetoothLeService.readCharacteristic(bluetoothLeService.getSupportedGattServices().get(2).getCharacteristic(UUID.fromString(SampleGattAttributes.DEVICE_IDENTITY_CHARACTERISTIC)));
                //BluetoothGattCharacteristic myBTG = new BluetoothGattCharacteristic(UUID.fromString(SampleGattAttributes.DEVICE_IDENTITY_CHARACTERISTIC),2,0);
                //BluetoothGattService btg = bluetoothLeService.getSupportedGattServices().get(2);
                //btg.getCharacteristics().get(2);//BluetoothGattCharacteristic btg = new BluetoothGattCharacteristic(UUID.fromString(SampleGattAttributes.TESTBED_SERVICE),BluetoothGattCharacteristic.PROPERTY_READ, BluetoothGattCharacteristic.PERMISSION_READ);
                //bluetoothLeService.readCharacteristic(myBTG);
            } else if (BluetoothLeService.ACTION_DATA_AVAILABLE.equals(action)) {
                int  sensors = intent.getIntExtra(BluetoothLeService.AVAILABLE_SENSORS, 8);
                int id = intent.getIntExtra(BluetoothLeService.TESTBED_ID, 0x1FFFF);
                TestbedDevice testbedDevice = new TestbedDevice(getApplicationContext());
                testbedDevice = devicesListAdapter.getDevice(discoveringDeviceIndex);
                testbedDevice.addAvailableSensors(sensors);
                testbedDevice.addDeviceId(id);

                devicesListAdapter.setDevice(testbedDevice);
                devicesListAdapter.notifyDataSetChanged();
                bluetoothLeService.disconnect();
                discoveringDeviceIndex++;
            }
        }
    };

    @Override

    protected void onResume(){
        super.onResume();
        Intent bluetoothLeServiceIntent = new Intent(this, BluetoothLeService.class);
        //startService(bluetoothLeServiceIntent);
        bindService(bluetoothLeServiceIntent, mServiceConnection, BIND_AUTO_CREATE);
    }

    @Override
    protected void onPause()
    {
        super.onPause();
        //if(m)
        unbindService(mServiceConnection);
        bluetoothLeService = null;

    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        //unbindService(mServiceConnection);
        //bluetoothLeService = null;
        //testbedDbHelper.close();
    }
}


