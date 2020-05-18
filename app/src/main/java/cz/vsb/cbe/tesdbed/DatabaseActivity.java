package cz.vsb.cbe.tesdbed;

import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCharacteristic;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.ContentValues;
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
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;
import androidx.loader.content.Loader;

import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

public class DatabaseActivity extends AppCompatActivity {

    Fragment currentFragment = null;
    FragmentTransaction ft;

    private PedometerFragment pedometerFragment;
    private HeartRateFragment heartRateFragment;
    private TemperatureFragment temperatureFragment;

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
                writePedo("Unable to initialize Bluetooth");
                finish();
            }
            // Automatically connects to the device upon successful start-up initialization.
            bluetoothLeService.connect(testbedDevice.getBluetoothDevice().getAddress());
            //conectingDialog.setMessage("Probíhá připojování k: [" + testbedDevice.getBluetoothDevice().getAddress() + "] (" + String.format("%04X", testbedDevice.getDeviceId()) + ")...");
            //conectingDialog.show();
            //write("Probíhá připojování k: [" + testbedDevice.getBluetoothDevice().getAddress() + "] (" + String.format("%04X", testbedDevice.getDeviceId()) + ")...");
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
            }

            return false;
        }

    };


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_test);
        Toolbar actionBar = (Toolbar) findViewById(R.id.activity_test_action_bar);

        pedometerFragment = new PedometerFragment();
        heartRateFragment = new HeartRateFragment();
        temperatureFragment = new TemperatureFragment();

        ft = getSupportFragmentManager().beginTransaction();
        currentFragment = temperatureFragment;
        ft.replace(R.id.activity_test_host_fragment, currentFragment);
        ft.commit();

        BottomNavigationView navigation = (BottomNavigationView) findViewById(R.id.nav_view);
        navigation.setOnNavigationItemSelectedListener(mOnNavigationItemSelectedListener);

        testbedDevice = getIntent().getExtras().getParcelable("TestbedDevice");

        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_NOSENSOR);

        actionBar.setTitle("[" + testbedDevice.getBluetoothDevice().getAddress() + "] (" + String.format("%04X", testbedDevice.getDeviceId()) + ")");
        setSupportActionBar(actionBar);

        //write("Připraven k připojení: [" + testbedDevice.getBluetoothDevice().getAddress() + "] (" + String.format("%04X", testbedDevice.getDeviceId()) + ")");


        AlertDialog.Builder conectingDialogBuilder = new AlertDialog.Builder(this);
        //LayoutInflater layoutInflater = (LayoutInflater) this.getSystemService(LAYOUT_INFLATER_SERVICE);
        View conectingDialogBuilderView = getLayoutInflater().inflate(R.layout.dialog_list_view, null);
        ListView listView = (ListView) conectingDialogBuilderView.findViewById(R.id.dialog_list_view_lsv);
        conditionsListAdapter = new ConditionsListAdapter(getLayoutInflater(), getApplicationContext(), 3);
        listView.setAdapter(conditionsListAdapter);
        conectingDialogBuilder.setIcon(getDrawable(R.drawable.ic_testbed_id)); //TODO: Jiná ikona
        conectingDialogBuilder.setTitle("Dialog no a co!");
        conectingDialogBuilder.setView(conectingDialogBuilderView);
        conectingDialog = conectingDialogBuilder.create();
        conectingDialog.setCancelable(false);
        conectingDialog.setCanceledOnTouchOutside(false);
        conectingDialog.show();


        conditionsListAdapter.setCondition(0, ConditionsListAdapter.PROGRESS, "Připojování zařízení...");
        conditionsListAdapter.notifyDataSetChanged();

        Intent gattServiceIntent = new Intent(this, BluetoothLeService.class);
        gattServiceIntent.putExtra(BluetoothLeService.TESTBED_ID, testbedDevice);
        //startService(gattServiceIntent);
        bindService(gattServiceIntent, mServiceConnection, BIND_AUTO_CREATE);
    }

    private void writeDescriptor(int characteristic){
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
                //conectingDialog.setMessage("Připojeno k: [" + testbedDevice.getBluetoothDevice().getAddress() + "] (" + String.format("%04X", testbedDevice.getDeviceId()) + ")");
                //conectingDialog.show();
                //conectingDialog.setMessage("Objevování dostupných services u: [" + testbedDevice.getBluetoothDevice().getAddress() + "] (" + String.format("%04X", testbedDevice.getDeviceId()) + ")");
                //conectingDialog.show();
            } else if (BluetoothLeService.ACTION_GATT_DISCONNECTED.equals(action)) {
                Toast.makeText(getApplicationContext(), "ODPOJENO!", Toast.LENGTH_SHORT).show();
                //conectingDialog.setMessage("Odpojeno od: [" + testbedDevice.getBluetoothDevice().getAddress() + "] (" + String.format("%04X", testbedDevice.getDeviceId()) + ")");
                //conectingDialog.show();
                //unregisterReceiver(mGattUpdateReceiver);
            } else if (BluetoothLeService.ACTION_GATT_SERVICES_DISCOVERED.equals(action)) {
                conditionsListAdapter.setCondition(1, ConditionsListAdapter.PASS, "Services objeveny.");
                conditionsListAdapter.setCondition(2,ConditionsListAdapter.PROGRESS,"Probíhá příprava na zápis descriptorů (1 z 3)");
                conditionsListAdapter.notifyDataSetChanged();
                //conectingDialog.setMessage("Objeveny dostupné sevices u: [" + testbedDevice.getBluetoothDevice().getAddress() + "] (" + String.format("%04X", testbedDevice.getDeviceId()) + ")");
                //conectingDialog.show();
                new Handler().postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        writeDescriptor(iterator);
                        conditionsListAdapter.setCondition(2,ConditionsListAdapter.PROGRESS,"Probíhá zápis descriptorů (1 z 3)");
                        conditionsListAdapter.notifyDataSetChanged();
                    }
                }, 2000);

            } else if (BluetoothLeService.ACTION_GATT_DESCRIPTOR_WRITTEN.equals(action)) {
                //conectingDialog.setMessage("Nastaveno přijímání notifikací u: [" + testbedDevice.getBluetoothDevice().getAddress() + "] (" + String.format("%04X", testbedDevice.getDeviceId()) + ") na service: " + SampleGattAttributes.lookupFromUUID(SampleGattAttributes.TESTBED_SERVICE, "unknown)") + ", characteristic: " + iterator);
                //conectingDialog.show();
                iterator++;
                if (iterator <= 3) {
                    writeDescriptor(iterator);
                    conditionsListAdapter.setCondition(2,ConditionsListAdapter.PROGRESS,"Probíhá zápis descriptorů (" + iterator + " z 3)");
                    conditionsListAdapter.notifyDataSetChanged();
                } else{
                    conditionsListAdapter.setCondition(2,ConditionsListAdapter.PASS,"Zápis descriptorů dokončen.");
                    conditionsListAdapter.notifyDataSetChanged();
                    conectingDialog.hide();

                }
                    ;//conectingDialog.hide();

            } else if (BluetoothLeService.STEP_DATA_AVAILABLE.equals(action)){
                String data = intent.getStringExtra(BluetoothLeService.EXTRA_DATA);
                writePedo(data);

                //ContentValues values = new ContentValues();
                //values.put(TestbedDbHelper.Data.COLUMN_NAME_DEVICE_ID, testbedDevice.getDeviceId());
                //values.put(TestbedDbHelper.Data.COLUMN_NAME_DATA_KEY, STEPS);
                //values.put(TestbedDbHelper.Data.COLUMN_NAME_DATA_VALUE, Integer.valueOf(data));
                //values.put(TestbedDbHelper.Data.COLUMN_NAME_TIMESTAMP, System.currentTimeMillis());

                //writableTestbedDb.insert(TestbedDbHelper.Data.TABLE_NAME, null, values);

            } else if (BluetoothLeService.HEART_RATE_DATA_AVAILABLE.equals(action)){
                String data = intent.getStringExtra(BluetoothLeService.EXTRA_DATA);
                writeHeartRate(data);
            } else if (BluetoothLeService.TEMPERATURE_DATA_AVAILABLE.equals(action)){

                TestbedDbHelper testbedDbHelper = TestbedDbHelper.getInstance(getApplicationContext());
                SQLiteDatabase db = testbedDbHelper.getReadableDatabase();



/*
                //Map<Float, Long> mapa = new HashMap<>();
                SimpleDateFormat sdf = new SimpleDateFormat("dd. MM HH:mm:ss");

                while(readCursor.moveToNext()) {
                    //mapa.put(readCursor.getFloat(readCursor.getColumnIndexOrThrow(TestbedDbHelper.Data.COLUMN_NAME_DATA_VALUE)),
                    //         readCursor.getLong(readCursor.getColumnIndexOrThrow(TestbedDbHelper.Data.COLUMN_NAME_TIMESTAMP)));
                    Date resultdate = new Date(readCursor.getLong(readCursor.getColumnIndexOrThrow(TestbedDbHelper.Data.COLUMN_NAME_TIMESTAMP)));
                    writeTemp("[" + readCursor.getFloat(readCursor.getColumnIndexOrThrow(TestbedDbHelper.Data.COLUMN_NAME_DATA_VALUE)) + ", " +
                                    sdf.format(resultdate) + "]");
                }

                readCursor.close();*/

                new SelectFromDatabase().execute(db);



                //testbedDbHelper.close();

                //String data = intent.getStringExtra(BluetoothLeService.EXTRA_DATA);
                //float data = intent.getFloatExtra(BluetoothLeService.EXTRA_DATA, 127.99f);
                //writeTemp(String.valueOf(data));
            } else if (BluetoothLeService.ACTION_DATA_AVAILABLE.equals(action)) {

                byte[] data = intent.getByteArrayExtra(BluetoothLeService.EXTRA_DATA);
                //String data = intent.getStringExtra(BluetoothLeService.EXTRA_DATA);
                SimpleDateFormat sdf = new SimpleDateFormat("HH:mm:ss.SSS dd-MM-yyyy", Locale.getDefault());
                 String str = intent.getAction();
                writePedo(str);

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
        registerReceiver(mGattUpdateReceiver, makeGattUpdateIntentFilter());
        if (bluetoothLeService != null) {
            final boolean result = bluetoothLeService.connect(testbedDevice.getBluetoothDevice().getAddress());
            Log.d(TAG, "Connect request result=" + result);
        }
        //testbedDbHelper = new TestbedDbHelper(getApplicationContext());
        //writableTestbedDb = testbedDbHelper.getWritableDatabase();
    }

    @Override
    protected void onPause() {
        super.onPause();
        unregisterReceiver(mGattUpdateReceiver);
        //testbedDbHelper.close();

    }

    @Override
    public void onStop() {
        super.onStop();
        //unbindService(mServiceConnection);
        //bluetoothLeService.stopSelf();
        //bluetoothLeService = null;

    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        unbindService(mServiceConnection);
        bluetoothLeService=null;



    }


    private void writePedo(String text){
        pedometerFragment.updatePedo(text);
    }

    private void writeHeartRate(String text){
        pedometerFragment.updateHeartRate(text);
    }

    private void writeTemp(String text){
        pedometerFragment.updateTemp(text);
    }



    private class SelectFromDatabase extends AsyncTask<SQLiteDatabase, Void, List<Map<Date, Float>>> {


        @Override
        protected void onPreExecute(){
            Log.w(TAG, "Začínáme...");
            //pedometerFragment.updateTemp("");
        }

        @Override
        protected List<Map<Date, Float>> doInBackground(SQLiteDatabase... db) {

            List<Map<Date, Float>> temp = new ArrayList<>();

            int count = db.length;

            Log.w(TAG, "LENGHT = " + count);

            for (int i = 0; i < count; i++) {

            Cursor cursor = db[i].query(
                    // The table to query
                    TestbedDbHelper.Data.TABLE_NAME,
                    // The array of columns to return (pass null to get all)
                    new String[]{
                            TestbedDbHelper.Data.COLUMN_NAME_DATA_VALUE,
                            TestbedDbHelper.Data.COLUMN_NAME_TIMESTAMP},
                    // The columns for the WHERE clause
                    TestbedDbHelper.Data.COLUMN_NAME_DEVICE_ID + " = ? AND " +
                            TestbedDbHelper.Data.COLUMN_NAME_DATA_KEY + " = ?",
                    // The values for the WHERE clause
                    new String[]{Integer.toString(testbedDevice.getDeviceId()), "TEMP"},
                    // don't group the rows
                    null,
                    // don't filter by row groups
                    null,
                    // The sort order
                    null
            );

                Log.w(TAG, "Cursor" + cursor.getCount());


            SimpleDateFormat sdf = new SimpleDateFormat("dd. MM HH:mm:ss");

            while(cursor.moveToNext()) {

                float tep = cursor.getFloat(cursor.getColumnIndexOrThrow(TestbedDbHelper.Data.COLUMN_NAME_DATA_VALUE));
                Date resultdate = new Date(cursor.getLong(cursor.getColumnIndexOrThrow(TestbedDbHelper.Data.COLUMN_NAME_TIMESTAMP)));
                Map<Date, Float > mapa = new HashMap<>();
                mapa.put(resultdate, tep);
                temp.add(mapa);

            }

            cursor.close();
            db[i].close();
            }
            return temp;
        }

        @Override
        protected void onPostExecute(List<Map<Date, Float>> result) {
            super.onPostExecute(result);

            temperatureFragment.postData(result);
        }
    }

}
