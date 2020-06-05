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
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.pm.ActivityInfo;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.Handler;
import android.os.IBinder;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import cz.vsb.cbe.tesdbed.sql.TestbedDatabase;
import cz.vsb.cbe.tesdbed.BluetoothLeService.LocalBinder;

public class DiscoverDevicesActivity extends AppCompatActivity {

    private static final String TAG = DiscoverDevicesActivity.class.getSimpleName();

    private static final long SCAN_PERIOD_IN_SECOND = 5;

    private static final int NO_FOUNDED_DEVICES_STATE = 0;
    private static final int SCANNING_STATE = 1;
    private static final int DISCOVERING_STATE = 2;
    private static final int DISCOVERING_CANCELED = 3;
    private static final int DISCOVERED_STATE = 4;

    private static final int START_SCAN = 0;
    private static final int STOP_SCAN = 1;
    private static final int CANCEL_SCAN = 2;

    private static final int CONNECTION_STATUS = 0;
    private static final int SERVICE_DISCOVERING_STATUS = 1;
    private static final int DESCRIPTOR_WRITTEN_STATUS = 2;

    private int activityState = NO_FOUNDED_DEVICES_STATE;

    private TestbedDevicesListAdapter testbedDevicesListAdapter;

    private BluetoothLeScanner bluetoothLeScanner;
    private List<ScanFilter> scanFilters;
    private ScanSettings scanSettings;
    private Handler stopScanningAfterScanPeriodHandler;
    private long MillisUntilFinishedScanning;

    private BluetoothLeService bluetoothLeService;
    private int discoveringDeviceIndex = 0;

    private ProgressBar scanningAndDiscoveringProgressBar;

    private TextView noDevicesFound;

    private AlertDialog connectingDialog;

    private TestbedDevice selectedTestbedDevice;

    ConditionsListAdapter conditionsListAdapter;

    private boolean destroyBackgroundService = true;

    List<BluetoothGattCharacteristic> characteristicsForDescriptorWrite = new ArrayList<>();
    private int descriptorWriteIndex = 0;

    private enum INTENT_FILTER_TYPES {
        FOR_DISCOVERING,
        FOR_CONNECTING
    }

    private static IntentFilter makeGattUpdateIntentFilter(INTENT_FILTER_TYPES type) {
        final IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(BluetoothLeService.ACTION_GATT_CONNECTED);
        intentFilter.addAction(BluetoothLeService.ACTION_GATT_SERVICES_DISCOVERED);
        if (type == INTENT_FILTER_TYPES.FOR_DISCOVERING){
            intentFilter.addAction(BluetoothLeService.ACTION_TESTBED_ID_DATA_AVAILABLE);
        } else if (type == INTENT_FILTER_TYPES.FOR_CONNECTING){
            intentFilter.addAction(BluetoothLeService.ACTION_GATT_DESCRIPTOR_WRITTEN);
        }
        intentFilter.addAction(BluetoothLeService.ACTION_GATT_DISCONNECTED);
        return intentFilter;
    }

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
                menuItem.setTitle(getResources().getString(R.string.activity_discover_devices_discovering_started) + " (" + (discoveringDeviceIndex + 1) + " " + getResources().getString(R.string.activity_discover_devices_discovering_conjunction) + " " + testbedDevicesListAdapter.getCount() + ")");
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
            bluetoothLeService = ((LocalBinder) service).getService();
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
        testbedDevicesListAdapter = new TestbedDevicesListAdapter(this.getLayoutInflater(), getApplicationContext());
        devicesListView.setAdapter(testbedDevicesListAdapter);

        devicesListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                if (activityState == DISCOVERED_STATE) {
                    selectedTestbedDevice = testbedDevicesListAdapter.getDevice(position);
                    TestbedDatabase.getInstance(getApplicationContext()).selectStoredTestbedDevice(selectedTestbedDevice, new TestbedDatabase.OnIsTestbedDeviceStored() {
                        @Override
                        public void onStoredResult(int result) {
                            if (result == TestbedDevice.NOT_STORED) {
                                TestbedDatabase.getInstance(getApplicationContext()).insertTestbedDevice(selectedTestbedDevice);
                            }
                            bluetoothLeService.setTestbedDevice(selectedTestbedDevice);
                            bluetoothLeService.connect(selectedTestbedDevice.getBluetoothDevice().getAddress());
                            registerReceiver(connectingBroadcastReceiver, makeGattUpdateIntentFilter(INTENT_FILTER_TYPES.FOR_CONNECTING));
                            connectingDialog.show();
                            conditionsListAdapter.setCondition(CONNECTION_STATUS, ConditionsListAdapter.PROGRESS, getString(R.string.alert_dialog_text_connecting));
                            conditionsListAdapter.notifyDataSetChanged();
                        }
                    });
                }
            }
        });

        scanningAndDiscoveringProgressBar = findViewById(R.id.activity_discover_devices_pgb_progress);
        scanningAndDiscoveringProgressBar.setVisibility(ProgressBar.INVISIBLE);

        noDevicesFound = findViewById(R.id.activity_discover_devices_txv_no_devices_found);
        noDevicesFound.setVisibility(View.INVISIBLE);

        View dialogView = getLayoutInflater().inflate(R.layout.dialog_list_view, null);
        final ListView conditionsListView = dialogView.findViewById(R.id.dialog_list_view_lsv);
        conditionsListAdapter = new ConditionsListAdapter(getLayoutInflater(), getApplicationContext(), 3);
        conditionsListView.setAdapter(conditionsListAdapter);

        connectingDialog = new AlertDialog.Builder(this)
                .setView(dialogView)
                .setTitle(getString(R.string.alert_dialog_title))
                .setIcon(getDrawable(R.drawable.ic_testbed_id))
                .setNeutralButton(getString(R.string.alert_dialog_text_cancel), new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        bluetoothLeService.disconnect();
                        conditionsListAdapter = new ConditionsListAdapter(getLayoutInflater(), getApplicationContext(), 3);
                        conditionsListView.setAdapter(conditionsListAdapter);
                    }
                })
                .create();
        connectingDialog.setCancelable(false);
        connectingDialog.setCanceledOnTouchOutside(false);
    }

    @Override
    protected void onStart(){
        super.onStart();
        bindService(new Intent(this, BluetoothLeService.class), mServiceConnection, BIND_AUTO_CREATE);
        scanLeDevices(START_SCAN);
    }


    private void scanLeDevices(final int state) {
        if (state == START_SCAN) {
            invalidateOptionsMenu();
            scanningAndDiscoveringProgressBar.setVisibility(ProgressBar.VISIBLE);
            noDevicesFound.setVisibility(View.INVISIBLE);
            testbedDevicesListAdapter.clear();
            testbedDevicesListAdapter.notifyDataSetChanged();
            bluetoothLeScanner.startScan(scanFilters,scanSettings, scanLeDevicesCallback);
            activityState = SCANNING_STATE;
            stopScanningAfterScanPeriodHandler.postDelayed(stopScanningAfterScanPeriodRunnable, SCAN_PERIOD_IN_SECOND*1000);
            scanningCountDownTimer.start();

        } else if (state == STOP_SCAN) {
            bluetoothLeScanner.stopScan(scanLeDevicesCallback);
            stopScanningAfterScanPeriodHandler.removeCallbacks(stopScanningAfterScanPeriodRunnable);
            scanningCountDownTimer.cancel();

            if(testbedDevicesListAdapter.getCount() >= 1 && bluetoothLeService != null) {
                registerReceiver(discoveringBroadcastReceiver, makeGattUpdateIntentFilter(INTENT_FILTER_TYPES.FOR_DISCOVERING));
                discoveringDeviceIndex = 0;
                bluetoothLeService.connect(testbedDevicesListAdapter.getDevice(discoveringDeviceIndex).getBluetoothDevice().getAddress());
                activityState = DISCOVERING_STATE;
            } else {
                activityState = NO_FOUNDED_DEVICES_STATE;
                scanningAndDiscoveringProgressBar.setVisibility(ProgressBar.INVISIBLE);
                noDevicesFound.setVisibility(View.VISIBLE);

            }
            invalidateOptionsMenu();

        } else {
            bluetoothLeScanner.stopScan(scanLeDevicesCallback);
            testbedDevicesListAdapter.clear();
            testbedDevicesListAdapter.notifyDataSetChanged();
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
            testbedDevicesListAdapter.setDevice(testbedDevice);
            testbedDevicesListAdapter.notifyDataSetChanged();
        }

        @Override
        public void onScanFailed(int errorCode) {
            Toast.makeText(getApplicationContext(), getResources().getString(R.string.activity_discover_devices_scanning_failed) + errorCode, Toast.LENGTH_LONG).show();
            finish();
        }
    };

    private final BroadcastReceiver discoveringBroadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            final String action = intent.getAction();
            if(activityState == DISCOVERING_CANCELED){
                unregisterReceiver(discoveringBroadcastReceiver);
                bluetoothLeService.disconnect();
                List<TestbedDevice> devicesForRemove = new ArrayList<>();
                for(TestbedDevice testbedDevice : testbedDevicesListAdapter.getDevices()){
                    if(!testbedDevice.isDeviceDiscovered()){
                        devicesForRemove.add(testbedDevice);
                    }
                }
                testbedDevicesListAdapter.removeDevices(devicesForRemove);
                testbedDevicesListAdapter.notifyDataSetChanged();
                activityState = DISCOVERED_STATE;
                invalidateOptionsMenu();
                if(testbedDevicesListAdapter.getCount() == 0){
                    noDevicesFound.setVisibility(View.VISIBLE);
                }
                scanningAndDiscoveringProgressBar.setVisibility(ProgressBar.INVISIBLE);
            } else {
                 if (BluetoothLeService.ACTION_GATT_DISCONNECTED.equals(action)) {
                    discoveringDeviceIndex++;
                    if (discoveringDeviceIndex < testbedDevicesListAdapter.getCount() && bluetoothLeService != null) {
                        bluetoothLeService.connect(testbedDevicesListAdapter.getDevice(discoveringDeviceIndex).getBluetoothDevice().getAddress());
                        invalidateOptionsMenu();
                    } else {
                        unregisterReceiver(discoveringBroadcastReceiver);
                        List<TestbedDevice> devicesForRemove = new ArrayList<>();
                        for(TestbedDevice testbedDevice : testbedDevicesListAdapter.getDevices()){
                            if(!testbedDevice.isDeviceDiscovered()){
                                devicesForRemove.add(testbedDevice);
                            }
                        }
                        testbedDevicesListAdapter.removeDevices(devicesForRemove);
                        testbedDevicesListAdapter.notifyDataSetChanged();
                        activityState = DISCOVERED_STATE;
                        invalidateOptionsMenu();
                        if(testbedDevicesListAdapter.getCount() == 0){
                            noDevicesFound.setVisibility(View.VISIBLE);
                        }
                        scanningAndDiscoveringProgressBar.setVisibility(ProgressBar.INVISIBLE);
                    }
                } else if (BluetoothLeService.ACTION_GATT_SERVICES_DISCOVERED.equals(action)) {
                    for (BluetoothGattService bluetoothGattService : bluetoothLeService.getSupportedGattServices()) {
                        for (BluetoothGattCharacteristic bluetoothGattCharacteristic : bluetoothGattService.getCharacteristics()) {
                            if (bluetoothGattCharacteristic.getUuid().equals(SampleGattAttributes.TESTBED_ID_CHARACTERISTIC_UUID)) {
                                bluetoothLeService.readCharacteristic(bluetoothGattCharacteristic);
                            }
                        }
                    }
                } else if (BluetoothLeService.ACTION_TESTBED_ID_DATA_AVAILABLE.equals(action)) {
                    final TestbedDevice testbedDevice = testbedDevicesListAdapter.getDevice(discoveringDeviceIndex);
                    testbedDevice.addAvailableSensors(intent.getIntExtra(BluetoothLeService.AVAILABLE_SENSORS_DATA, 8));
                    testbedDevice.addDeviceId(intent.getIntExtra(BluetoothLeService.TESTBED_ID_DATA, 0x1FFFF));
                    testbedDevice.deviceIsDiscovered();
                    TestbedDatabase.getInstance(getApplicationContext()).selectStoredTestbedDevice(testbedDevice, new TestbedDatabase.OnIsTestbedDeviceStored() {
                        @Override
                        public void onStoredResult(int result) {
                            testbedDevice.setStoredStatus(result);
                            testbedDevicesListAdapter.setDevice(testbedDevice);
                            testbedDevicesListAdapter.notifyDataSetChanged();
                            bluetoothLeService.disconnect();
                        }
                    });
                }
            }
        }
    };

    private final BroadcastReceiver connectingBroadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            final String action = intent.getAction();
            if (BluetoothLeService.ACTION_GATT_CONNECTED.equals(action)) {
                TestbedDatabase.getInstance(getApplicationContext()).updateLastConnectedTestbedDevice(selectedTestbedDevice);
                conditionsListAdapter.setCondition(CONNECTION_STATUS, ConditionsListAdapter.PASS, getString(R.string.notification_title) + ": " +
                        getString(R.string.ble_devices_name) + " (#" +
                        Integer.toHexString(selectedTestbedDevice.getDeviceId()) + ")");
                conditionsListAdapter.setCondition(SERVICE_DISCOVERING_STATUS, ConditionsListAdapter.PROGRESS, getString(R.string.alert_dialog_text_service_discovering));
                conditionsListAdapter.notifyDataSetChanged();
            } else if (BluetoothLeService.ACTION_GATT_DISCONNECTED.equals(action)) {
                Toast.makeText(getApplicationContext(), "ODPOJENO!", Toast.LENGTH_SHORT).show();
            } else if (BluetoothLeService.ACTION_GATT_SERVICES_DISCOVERED.equals(action)) {
                conditionsListAdapter.setCondition(SERVICE_DISCOVERING_STATUS, ConditionsListAdapter.PASS, getString(R.string.alert_dialog_text_service_discovered));
                conditionsListAdapter.notifyDataSetChanged();


                characteristicsForDescriptorWrite = new ArrayList<>();
                descriptorWriteIndex = 0;

                for (BluetoothGattService bluetoothGattService : bluetoothLeService.getSupportedGattServices()) {
                    if (bluetoothGattService.getUuid().equals(SampleGattAttributes.TESTBED_SERVICE_UUID)) {
                        for (BluetoothGattCharacteristic bluetoothGattCharacteristic : bluetoothGattService.getCharacteristics()) {
                            if (bluetoothGattCharacteristic.getUuid().equals(SampleGattAttributes.STEPS_CHARACTERISTIC_UUID) && BigInteger.valueOf(selectedTestbedDevice.getAvailableSensors()).testBit(2)) {
                                characteristicsForDescriptorWrite.add(bluetoothGattCharacteristic);
                            } else if (bluetoothGattCharacteristic.getUuid().equals(SampleGattAttributes.HEART_RATE_CHARACTERISTIC_UUID) && BigInteger.valueOf(selectedTestbedDevice.getAvailableSensors()).testBit(1)) {
                                characteristicsForDescriptorWrite.add(bluetoothGattCharacteristic);
                            } else if (bluetoothGattCharacteristic.getUuid().equals(SampleGattAttributes.TEMPERATURE_CHARACTERISTIC_UUID) && BigInteger.valueOf(selectedTestbedDevice.getAvailableSensors()).testBit(0)) {
                                characteristicsForDescriptorWrite.add(bluetoothGattCharacteristic);
                            }
                        }
                    }
                }
                new Handler().postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        bluetoothLeService.setCharacteristicNotification(characteristicsForDescriptorWrite.get(descriptorWriteIndex), true);
                        conditionsListAdapter.setCondition(DESCRIPTOR_WRITTEN_STATUS, ConditionsListAdapter.PROGRESS,  getString(R.string.alert_dialog_text_descriptor_writing) + " (" + (descriptorWriteIndex + 1) + " " + getString(R.string.alert_dialog_text_descriptor_writing_conjuction) + " " + characteristicsForDescriptorWrite.size() + ")");
                        conditionsListAdapter.notifyDataSetChanged();
                    }
                }, 2000);
                conditionsListAdapter.setCondition(DESCRIPTOR_WRITTEN_STATUS, ConditionsListAdapter.PROGRESS, getString(R.string.alert_dialog_text_descriptor_write_prepare) + " (" + characteristicsForDescriptorWrite.size() + ")");
                conditionsListAdapter.notifyDataSetChanged();

            } else if (BluetoothLeService.ACTION_GATT_DESCRIPTOR_WRITTEN.equals(action)) {
                descriptorWriteIndex++;
                if (descriptorWriteIndex < characteristicsForDescriptorWrite.size()) {
                    bluetoothLeService.setCharacteristicNotification(characteristicsForDescriptorWrite.get(descriptorWriteIndex), true);
                    conditionsListAdapter.setCondition(DESCRIPTOR_WRITTEN_STATUS, ConditionsListAdapter.PROGRESS,  getString(R.string.alert_dialog_text_descriptor_writing) + " (" + (descriptorWriteIndex + 1) + " " + getString(R.string.alert_dialog_text_descriptor_writing_conjuction) + " " + characteristicsForDescriptorWrite.size() + ")");
                    conditionsListAdapter.notifyDataSetChanged();
                } else {
                    conditionsListAdapter.setCondition(2, ConditionsListAdapter.PASS, getString(R.string.alert_dialog_text_descriptor_write_done));
                    conditionsListAdapter.notifyDataSetChanged();
                    connectingDialog.hide();
                    connectingDialog.cancel();
                    unregisterReceiver(connectingBroadcastReceiver);
                    Intent intentActivity = new Intent(DiscoverDevicesActivity.this, DatabaseActivity.class);
                    intentActivity.putExtra(DatabaseActivity.TESTBED_DEVICE, selectedTestbedDevice);
                    startActivity(intentActivity);
                    destroyBackgroundService = false;
                    finish();
                }
            }
        }
    };



    @Override
    protected void onStop() {
        super.onStop();
        scanLeDevices(CANCEL_SCAN);
        unbindService(mServiceConnection);
        bluetoothLeService = null;

        Log.w(TAG, "Activity stopped");
    }


    @Override
    protected void onDestroy() {
        super.onDestroy();
        Log.w(TAG, "Activity destroyed");
        if(destroyBackgroundService){
            stopService(new Intent(this, BluetoothLeService.class));
        }
    }
}


