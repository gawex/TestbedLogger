package cz.vsb.cbe.tesdbed;

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

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

public class DiscoverDevicesActivity extends AppCompatActivity {

    private static final long SCAN_PERIOD_IN_SECOND = 5;

    private static final int NO_FOUNDED_DEVICES_STATE = 0;
    private static final int SCANNING_STATE = 1;
    private static final int DISCOVERING_STATE = 2;
    private static final int DISCOVERING_CANCELED = 3;
    private static final int DISCOVERED_STATE = 4;

    private static final int START_SCAN = 0;
    private static final int STOP_SCAN = 1;
    private static final int CANCEL_SCAN = 2;

    private int activityState = NO_FOUNDED_DEVICES_STATE;

    private DevicesListAdapter devicesListAdapter;

    private BluetoothLeScanner bluetoothLeScanner;
    private List<ScanFilter> scanFilters;
    private ScanSettings scanSettings;
    private Handler stopScanningAfterScanPeriodHandler;
    private long MillisUntilFinishedScanning;

    private BluetoothLeService bluetoothLeService;
    private int discoveringDeviceIndex = 0;

    private ProgressBar scanningAndDiscoveringProgressBar;

    private TextView noDevicesFound;

    private final Runnable stopScanningAfterScanPeriodRunnable = new Runnable() {
        @Override
        public void run() {
            scanLeDevices(STOP_SCAN);
        }
    };

    private final CountDownTimer scanningCountDownTimer = new CountDownTimer(SCAN_PERIOD_IN_SECOND*1000, 1000) {

        @Override
        public void onTick(long millisUntilFinishedScanning) {
            MillisUntilFinishedScanning = millisUntilFinishedScanning;
            invalidateOptionsMenu();
        }

        @Override
        public void onFinish() {

        }
    };

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main_menu, menu);
        return true;
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        MenuItem menuItem = menu.findItem(R.id.scaning_state);
        switch (activityState){
            case NO_FOUNDED_DEVICES_STATE:

            case DISCOVERED_STATE:
                menuItem.setTitle(getResources().getString(R.string.activity_discover_devices_discovering_stopped));
                break;

            case SCANNING_STATE:
                menuItem.setTitle(getResources().getString(R.string.activity_discover_devices_scanning_started) + " (" + ((MillisUntilFinishedScanning / 1000) + 1) + ")");
                break;

            case DISCOVERING_STATE:
                menuItem.setTitle(getResources().getString(R.string.activity_discover_devices_discovering_started) + " (" + (discoveringDeviceIndex + 1) + " " + getResources().getString(R.string.activity_discover_devices_discovering_conjunction) + " " + devicesListAdapter.getCount() + ")");
                break;

            case DISCOVERING_CANCELED:
                menuItem.setTitle(getResources().getString(R.string.activity_discover_devices_discovering_canceling));
                break;
        }
        return super.onPrepareOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.scaning_state:
                switch (activityState){
                    case NO_FOUNDED_DEVICES_STATE :

                    case DISCOVERED_STATE:
                        scanLeDevices(START_SCAN);
                        break;

                    case SCANNING_STATE:
                        scanLeDevices(STOP_SCAN);
                        break;

                    case DISCOVERING_STATE:
                        activityState = DISCOVERING_CANCELED;
                        invalidateOptionsMenu();
                        break;
                }
                return true;

            case R.id.settings:
                startActivity(new Intent(this, SettingsActivity.class));
                return true;

            case R.id.about_application:
                Toast.makeText(this, "About has not been implemented yet.", Toast.LENGTH_SHORT).show();
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
                // TODO: Adapter initialization failed.
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

        final BluetoothManager bluetoothManager = (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
        final BluetoothAdapter bluetoothAdapter = Objects.requireNonNull(bluetoothManager).getAdapter();
        bluetoothLeScanner = bluetoothAdapter.getBluetoothLeScanner();

        scanFilters = new ArrayList<>();
        ScanFilter scanNameFilter = new ScanFilter.Builder()
                .setDeviceName(getResources().getString(R.string.ble_devices_name))
                .build();
        scanFilters.add(scanNameFilter);

        scanSettings = new ScanSettings.Builder().build();

        stopScanningAfterScanPeriodHandler = new Handler();

        ListView devicesListView = findViewById(R.id.activity_discover_lsv_devices);
        devicesListAdapter = new DevicesListAdapter(this.getLayoutInflater(), getApplicationContext());
        devicesListView.setAdapter(devicesListAdapter);

        devicesListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                if(activityState == DISCOVERED_STATE){
                    if(devicesListAdapter.getDevice(position).isStored() != TestbedDevice.STORED_CONSISTENTLY) {
                        devicesListAdapter.getDevice(position).storeIntoDatabase();
                    }
                    Intent intent = new Intent(DiscoverDevicesActivity.this, DatabaseActivity.class);
                    intent.putExtra(DatabaseActivity.TESTBED_DEVICE, devicesListAdapter.getDevice(position));
                    startActivity(intent);

                }
            }
        });

        scanningAndDiscoveringProgressBar = findViewById(R.id.activity_discover_devices_pgb_progress);
        scanningAndDiscoveringProgressBar.setVisibility(ProgressBar.INVISIBLE);

        noDevicesFound = findViewById(R.id.activity_discover_devices_txv_no_devices_found);
        noDevicesFound.setVisibility(View.INVISIBLE);
    }

    @Override
    protected void onResume(){
        super.onResume();
        Intent bluetoothLeServiceIntent = new Intent(this, BluetoothLeService.class);
        bindService(bluetoothLeServiceIntent, mServiceConnection, BIND_AUTO_CREATE);
        scanLeDevices(START_SCAN);
    }


    private void scanLeDevices(final int state) {
        if (state == START_SCAN) {
            invalidateOptionsMenu();
            scanningAndDiscoveringProgressBar.setVisibility(ProgressBar.VISIBLE);
            noDevicesFound.setVisibility(View.INVISIBLE);
            devicesListAdapter.clear();
            devicesListAdapter.notifyDataSetChanged();
            bluetoothLeScanner.startScan(scanFilters,scanSettings, scanLeDevicesCallback);
            activityState = SCANNING_STATE;
            stopScanningAfterScanPeriodHandler.postDelayed(stopScanningAfterScanPeriodRunnable, SCAN_PERIOD_IN_SECOND*1000);
            scanningCountDownTimer.start();

        } else if (state == STOP_SCAN) {
            bluetoothLeScanner.stopScan(scanLeDevicesCallback);
            stopScanningAfterScanPeriodHandler.removeCallbacks(stopScanningAfterScanPeriodRunnable);
            scanningCountDownTimer.cancel();

            if(devicesListAdapter.getCount() >= 1 && bluetoothLeService != null) {
                registerReceiver(mGattUpdateReceiver, makeGattUpdateIntentFilter());
                discoveringDeviceIndex = 0;
                bluetoothLeService.connect(devicesListAdapter.getDevice(discoveringDeviceIndex).getBluetoothDevice().getAddress());
                activityState = DISCOVERING_STATE;
            } else {
                activityState = NO_FOUNDED_DEVICES_STATE;
                scanningAndDiscoveringProgressBar.setVisibility(ProgressBar.INVISIBLE);
                noDevicesFound.setVisibility(View.VISIBLE);

            }
            invalidateOptionsMenu();

        } else {
            bluetoothLeScanner.stopScan(scanLeDevicesCallback);
            devicesListAdapter.clear();
            devicesListAdapter.notifyDataSetChanged();
            activityState = NO_FOUNDED_DEVICES_STATE;
            stopScanningAfterScanPeriodHandler.removeCallbacks(stopScanningAfterScanPeriodRunnable);
            scanningCountDownTimer.cancel();
            invalidateOptionsMenu();
        }

    }

    private final ScanCallback scanLeDevicesCallback = new ScanCallback() {

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

    private final BroadcastReceiver mGattUpdateReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            final String action = intent.getAction();
            if(activityState == DISCOVERING_CANCELED){
                unregisterReceiver(mGattUpdateReceiver);
                bluetoothLeService.disconnect();
                List<TestbedDevice> devicesForRemove = new ArrayList<>();
                for(TestbedDevice testbedDevice : devicesListAdapter.getDevices()){
                    if(!testbedDevice.isDeviceDiscovered()){
                        devicesForRemove.add(testbedDevice);
                    }
                }
                devicesListAdapter.removeDevices(devicesForRemove);
                devicesListAdapter.notifyDataSetChanged();
                activityState = DISCOVERED_STATE;
                invalidateOptionsMenu();
                if(devicesListAdapter.getCount() == 0){
                    noDevicesFound.setVisibility(View.VISIBLE);
                }
                scanningAndDiscoveringProgressBar.setVisibility(ProgressBar.INVISIBLE);
            } else {
                 if (BluetoothLeService.ACTION_GATT_DISCONNECTED.equals(action)) {
                    discoveringDeviceIndex++;
                    if (discoveringDeviceIndex < devicesListAdapter.getCount() && bluetoothLeService != null) {
                        bluetoothLeService.connect(devicesListAdapter.getDevice(discoveringDeviceIndex).getBluetoothDevice().getAddress());
                        invalidateOptionsMenu();
                    } else {
                        unregisterReceiver(mGattUpdateReceiver);
                        List<TestbedDevice> devicesForRemove = new ArrayList<>();
                        for(TestbedDevice testbedDevice : devicesListAdapter.getDevices()){
                            if(!testbedDevice.isDeviceDiscovered()){
                                devicesForRemove.add(testbedDevice);
                            }
                        }
                        devicesListAdapter.removeDevices(devicesForRemove);
                        devicesListAdapter.notifyDataSetChanged();
                        activityState = DISCOVERED_STATE;
                        invalidateOptionsMenu();
                        if(devicesListAdapter.getCount() == 0){
                            noDevicesFound.setVisibility(View.VISIBLE);
                        }
                        scanningAndDiscoveringProgressBar.setVisibility(ProgressBar.INVISIBLE);
                    }
                } else if (BluetoothLeService.ACTION_GATT_SERVICES_DISCOVERED.equals(action)) {
                    for (BluetoothGattService bluetoothGattService : bluetoothLeService.getSupportedGattServices()) {
                        for (BluetoothGattCharacteristic bluetoothGattCharacteristic : bluetoothGattService.getCharacteristics()) {
                            if (bluetoothGattCharacteristic.getUuid().equals(UUID.fromString(SampleGattAttributes.DEVICE_IDENTITY_CHARACTERISTIC))) {
                                bluetoothLeService.readCharacteristic(bluetoothGattCharacteristic);
                            }
                        }
                    }
                } else if (BluetoothLeService.ACTION_DATA_AVAILABLE.equals(action)) {
                    TestbedDevice testbedDevice = devicesListAdapter.getDevice(discoveringDeviceIndex);
                    testbedDevice.addAvailableSensors(intent.getIntExtra(BluetoothLeService.AVAILABLE_SENSORS, 8));
                    testbedDevice.addDeviceId(intent.getIntExtra(BluetoothLeService.TESTBED_ID, 0x1FFFF));
                    testbedDevice.deviceIsDiscovered();
                    devicesListAdapter.setDevice(testbedDevice);
                    devicesListAdapter.notifyDataSetChanged();
                    bluetoothLeService.disconnect();
                }
            }
        }
    };



    @Override
    protected void onPause() {
        super.onPause();
        scanLeDevices(CANCEL_SCAN);
        unbindService(mServiceConnection);
        bluetoothLeService = null;
    }
}


