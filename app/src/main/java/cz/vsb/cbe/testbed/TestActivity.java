package cz.vsb.cbe.testbed;

import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;
import android.util.Log;

import java.util.Calendar;
import java.util.List;
import java.util.Map;

import cz.vsb.cbe.testbed.sql.Record;
import cz.vsb.cbe.testbed.sql.TestbedDatabaseNew;

public class TestActivity extends AppCompatActivity {

    private static final String TAG = TestActivity.class.getSimpleName();

    private TestbedDatabaseNew testbedDatabaseNew;

    private TestbedDevice testbedDevice;
    private Calendar startDate;
    private Calendar endDate;
    private Calendar limitDate;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_test);

        testbedDevice = new TestbedDevice(this);
        testbedDevice.addDeviceId(8228);

        startDate = Calendar.getInstance();
        startDate.set(2020,8,16,10,00,00);
        startDate.set(Calendar.MILLISECOND, 0);

        endDate = Calendar.getInstance();
        endDate.set(2020,8,16,13,59, 59);
        endDate.set(Calendar.MILLISECOND, 999);

        limitDate = Calendar.getInstance();
        limitDate.set(2020,8,16,10,00,00);
        limitDate.set(Calendar.MILLISECOND, 0);

        testbedDatabaseNew = new TestbedDatabaseNew(this);

        List<Record> recordsA = testbedDatabaseNew.selectDataBetweenTimeStamp(testbedDevice, BluetoothLeService.STEPS_DATA, startDate.getTime(), endDate.getTime());
        List<Record> recordsB = testbedDatabaseNew.selectDataLessThanTimeStamp(testbedDevice, BluetoothLeService.STEPS_DATA, limitDate.getTime());

        Record record = testbedDatabaseNew.getFirstRecordBeforeTimeStamp(testbedDevice, BluetoothLeService.STEPS_DATA, limitDate.getTime());

        List<List<Record>> sortedRecords = testbedDatabaseNew.sortRecordsByIntervals(recordsA, startDate, Calendar.MINUTE);



        if(record == null){
            record = new Record(0,0,0);
        }

        List<List<Record>> sortedSteps = testbedDatabaseNew.sortStepsByIntervals(record, sortedRecords);

        List<Map<Integer,Float>> stats = testbedDatabaseNew.getStatisticDataFromRecordsByIntervals(sortedSteps);

        Log.w(TAG, "DONE");

    }
}