package cz.vsb.cbe.testbed.sql;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import cz.vsb.cbe.testbed.BluetoothLeService;
import cz.vsb.cbe.testbed.TestbedDevice;

public class TestbedDatabaseNew {

    public static final float PEDOMETER_MIN_VALUE = 0;
    public static final float PEDOMETER_MAX_VALUE = 999999;

     private final Context Context;

    public TestbedDatabaseNew(Context context) {
        Context = context;
    }

    private final SQLiteDatabase getReadableDatabase(){
        return TestbedDatabaseHelper.getInstance(Context).getReadableDatabase();
    }



    private List<Record> getRecordsFromCursor(Cursor cursor){
        final List<Record> records = new ArrayList<>();
        while (cursor.moveToNext()){
            records.add(new Record(cursor.getInt(cursor.getColumnIndexOrThrow(TestbedDatabaseHelper.Data.COLUMN_NAME_DEVICE_ID)),
                    cursor.getFloat(cursor.getColumnIndexOrThrow(TestbedDatabaseHelper.Data.COLUMN_NAME_DATA_VALUE)),
                    cursor.getLong(cursor.getColumnIndexOrThrow(TestbedDatabaseHelper.Data.COLUMN_NAME_TIMESTAMP))));
        }
        return records;
    }

    public List<Record> selectAllData(TestbedDevice testbedDevice, String dataType){
        String tableName = TestbedDatabaseHelper.Data.TABLE_NAME;
        String[] columns = new String[]{TestbedDatabaseHelper.Data.COLUMN_NAME_DEVICE_ID,
                TestbedDatabaseHelper.Data.COLUMN_NAME_DATA_VALUE,
                TestbedDatabaseHelper.Data.COLUMN_NAME_TIMESTAMP};
        String selection = TestbedDatabaseHelper.Data.COLUMN_NAME_DEVICE_ID + " = ? AND " +
                TestbedDatabaseHelper.Data.COLUMN_NAME_DATA_KEY + " = ?";
        String[] selectionArgs = new String[]{Integer.toString(testbedDevice.getDeviceId()),
                dataType};

        return getRecordsFromCursor(getReadableDatabase().query(tableName,columns,selection,selectionArgs,null,null,TestbedDatabaseHelper.Data.COLUMN_NAME_TIMESTAMP + " ASC"));
    }

    public Record getFirstRecord(List<Record> records){
        if(records.size() > 0) {
            return records.get(0);
        } else {
            return null;
        }
    }

    public Record getLastRecord(List<Record> records){
        if(records.size() > 0) {
            return records.get(records.size() - 1);
        } else {
            return null;
        }
    }

    public List<Record> selectDataBetweenTimeStamp(TestbedDevice testbedDevice, String dataType, Date startDate, Date endDate){
        String tableName = TestbedDatabaseHelper.Data.TABLE_NAME;
        String[] columns = new String[]{TestbedDatabaseHelper.Data.COLUMN_NAME_DEVICE_ID,
                TestbedDatabaseHelper.Data.COLUMN_NAME_DATA_VALUE,
                TestbedDatabaseHelper.Data.COLUMN_NAME_TIMESTAMP};
        String selection = TestbedDatabaseHelper.Data.COLUMN_NAME_DEVICE_ID + " = ? AND " +
                TestbedDatabaseHelper.Data.COLUMN_NAME_DATA_KEY + " = ? AND " +
                TestbedDatabaseHelper.Data.COLUMN_NAME_TIMESTAMP + " BETWEEN ? AND ?";
        String[] selectionArgs = new String[]{Integer.toString(testbedDevice.getDeviceId()),
                dataType,
                Long.toString(startDate.getTime()),
                Long.toString(endDate.getTime())};

        return getRecordsFromCursor(getReadableDatabase().query(tableName,columns,selection,selectionArgs,null,null,TestbedDatabaseHelper.Data.COLUMN_NAME_TIMESTAMP + " ASC"));
    }

    public List<Record> selectDataLessThanTimeStamp(TestbedDevice testbedDevice, String dataType, Date limitDate){
        String tableName = TestbedDatabaseHelper.Data.TABLE_NAME;
        String[] columns = new String[]{TestbedDatabaseHelper.Data.COLUMN_NAME_DEVICE_ID,
                TestbedDatabaseHelper.Data.COLUMN_NAME_DATA_VALUE,
                TestbedDatabaseHelper.Data.COLUMN_NAME_TIMESTAMP};
        String selection = TestbedDatabaseHelper.Data.COLUMN_NAME_DEVICE_ID + " = ? AND " +
                TestbedDatabaseHelper.Data.COLUMN_NAME_DATA_KEY + " = ? AND " +
                TestbedDatabaseHelper.Data.COLUMN_NAME_TIMESTAMP + " < ?";
        String[] selectionArgs = new String[]{Integer.toString(testbedDevice.getDeviceId()),
                dataType,
                Long.toString(limitDate.getTime())};
        return getRecordsFromCursor(getReadableDatabase().query(tableName,columns,selection,selectionArgs,null,null,TestbedDatabaseHelper.Data.COLUMN_NAME_TIMESTAMP + " DESC"));

    }

    public Record getFirstRecordBeforeTimeStamp(TestbedDevice testbedDevice, String dataType, Date limitDate){
        try {
            return selectDataLessThanTimeStamp(testbedDevice, dataType, limitDate).get(0);
        } catch (IndexOutOfBoundsException e){
            return null;
        }
    }

    public List<List<Record>> sortRecordsByIntervals(final List<Record> records, Calendar intervalsBase, int intervalsScale){
        List<List<Record>> rawDataByIntervals;
        int currentInterval;
        int intervalOffset;
        int yearOffset;
        switch (intervalsScale){
            case Calendar.YEAR:
                Calendar firstYear = Calendar.getInstance();
                firstYear.setTimeInMillis(records.get(0).getTimeStamp().getTime());
                Calendar lastYear = Calendar.getInstance();
                lastYear.setTimeInMillis(records.get(records.size() - 1).getTimeStamp().getTime());
                rawDataByIntervals = new ArrayList (Collections.nCopies (lastYear.get(Calendar.YEAR) - firstYear.get(Calendar.YEAR) + 1, null));
                intervalOffset = 0;
                yearOffset = firstYear.get(Calendar.YEAR);
                break;
            case Calendar.MONTH:
                rawDataByIntervals = new ArrayList (Collections.nCopies (intervalsBase.getActualMaximum(intervalsScale) + 2, null));
                intervalOffset = 1;
                yearOffset = 0;
                break;
            case Calendar.DAY_OF_MONTH:
            case Calendar.HOUR_OF_DAY:
            case Calendar.MINUTE:
            case Calendar.SECOND:
                rawDataByIntervals = new ArrayList(Collections.nCopies (intervalsBase.getActualMaximum(intervalsScale) + 1, null));
                intervalOffset = 0;
                yearOffset = 0;
                break;
            default:
                rawDataByIntervals = new ArrayList<>();
                intervalOffset = 0;
                yearOffset = 0;
                break;
        }
        Calendar calendar = Calendar.getInstance();
        for (Record record : records){
            calendar.setTimeInMillis(record.getTimeStamp().getTime());
            currentInterval = calendar.get(intervalsScale) + intervalOffset - yearOffset;
            if(rawDataByIntervals.get(currentInterval) == null){
                rawDataByIntervals.set(currentInterval, new ArrayList<Record>());
            }
            rawDataByIntervals.get(currentInterval).add(record);
        }
        return rawDataByIntervals;
    }

    public List<List<Record>> sortStepsByIntervals(Record valueBeforeInterval, final List<List<Record>> sortedRecordsByInterval){
        List<List<Record>> recordsByIntervals = new ArrayList(Collections.nCopies (sortedRecordsByInterval.size(), null));
        float lastStepValue = valueBeforeInterval.getValue();
        float steps;
        for(int i = 0; i < sortedRecordsByInterval.size(); i++ ){
            if(sortedRecordsByInterval.get(i) != null){
                List<Record> recordsByInterval = new ArrayList<>();
                for (Record record : sortedRecordsByInterval.get(i)){
                    if(lastStepValue < record.getValue()){
                        steps = record.getValue() - lastStepValue;
                    } else {
                        steps = record.getValue();
                    }
                    lastStepValue = record.getValue();
                    Record newRecord = new Record(record);
                    newRecord.setValue(steps);
                    recordsByInterval.add(newRecord);
                }
                recordsByIntervals.set(i, recordsByInterval);
            }
        }
        return recordsByIntervals;
    }

    public List<Map<Integer,Float>> getStatisticDataFromRecordsByIntervals(List<List<Record>> recordsByIntervals){
        List<Map<Integer,Float>> statisticDataByIntervals = new ArrayList(Collections.nCopies (recordsByIntervals.size(), null));
        for(int i = 0; i < recordsByIntervals.size(); i++ ){
            if(recordsByIntervals.get(i) != null){
                float minValue = PEDOMETER_MAX_VALUE;
                float maxValue = PEDOMETER_MIN_VALUE;
                float sum = 0;
                List<Float> quartileList = new ArrayList<>();
                for (Record record : recordsByIntervals.get(i)){
                    float value = record.getValue();
                    if(minValue > value){
                        minValue = value;
                    }
                    if(maxValue < value){
                        maxValue = value;
                    }
                    sum += value;
                    quartileList.add(value);
                }
                Collections.sort(quartileList);
                Map<Integer,Float> statisticData = new HashMap<>();
                statisticData.put(TestbedDatabase.DATA_SET_SIZE, (float) recordsByIntervals.get(i).size());
                statisticData.put(TestbedDatabase.DATA_SET_SUM, (float) sum);
                statisticData.put(TestbedDatabase.MIN_VALUE, minValue);
                statisticData.put(TestbedDatabase.MAX_VALUE, maxValue);
                statisticData.put(TestbedDatabase.LOW_QUARTILE, quartileList.get( (int) Math.ceil(quartileList.size() * 25 / 100)));
                statisticData.put(TestbedDatabase.HIGH_QUARTILE, quartileList.get( (int) Math.ceil(quartileList.size() * 75 / 100)));
                statisticData.put(TestbedDatabase.MEAN_VALUE, sum / recordsByIntervals.get(i).size());
                statisticDataByIntervals.set(i, statisticData);
            }
        }
        return statisticDataByIntervals;
    }
}

