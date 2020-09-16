package cz.vsb.cbe.testbed.sql;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.AsyncTask;
import android.util.Log;

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

public class TestbedDatabase {

    private static String TAG;

    public static float TEMPERATURE_SENSOR_MIN_VALUE = -55;
    public static float TEMPERATURE_SENSOR_MAX_VALUE = 125;

    public static int HEART_RATE_SENSOR_MIN_VALUE = 0;
    public static int HEART_RATE_SENSOR_MAX_VALUE = 255;

    public static int DATA_SET_SIZE = 0;
    public static int MIN_VALUE = 1;
    public static int MAX_VALUE = 2;
    public static int LOW_QUARTILE = 3;
    public static int HIGH_QUARTILE = 4;
    public static int MEAN_VALUE = 5;
    public static int MEDIAN_VALUE = 6;
    public static int STANDARD_DEVIATION_VALUE = 7;

    private static TestbedDatabase TestbedDatabase;
    private static Context Context;
    private static SQLiteDatabase WritableDatabase;
    private static SQLiteDatabase ReadableDatabase;

    OnInsertIntoDatabase OnInsertIntoDatabase;
    OnUpdateIntoDatabase OnUpdateIntoDatabase;
    OnIsTestbedDeviceStored OnIsTestbedDeviceStored;
    OnGetLastConnectedTestbedDevice OnGetLastConnectedTestbedDevice;
    OnSelectTemperatureDataOld onSelectTemperatureDataOld;
    OnSelectFloatData OnSelectStepData;
    OnSelectFloatData OnSelectHeartRateData;
    OnSelectFloatData OnSelectTemperatureData;

    public interface OnInsertIntoDatabase {
        void onInsertCompleted();
    }

    public interface OnUpdateIntoDatabase {
        void onUpdatedCompleted();
    }

    public interface OnIsTestbedDeviceStored {
        void onStoredResult(int result);
    }

    public interface OnGetLastConnectedTestbedDevice {
        void onLastConnectedSuccess(int id, String macAddress);

        void onLastConnectResultFail();
    }

    public interface OnSelectTemperatureDataOld {
        void onSelectSuccess(List<Record> records);
        void onSelectFailed();
    }

    public interface OnSelectIntegerData {
        void onSelectSuccess(ArrayList<Map<Integer,Integer>> statisticDataByIntervals,  int firstX, int lastX);
        void onSelectFailed();
    }

    public interface OnSelectFloatData {
        void onSelectSuccess(ArrayList<Map<Integer,Float>> statisticDataByIntervals,  int firstX, int lastX);
        void onSelectFailed();
    }

    public static TestbedDatabase getInstance(Context context) {
        if (TestbedDatabase == null) {
            TestbedDatabase = new TestbedDatabase(context);
        }
        TAG = context.getPackageName();
        return TestbedDatabase;
    }

    private TestbedDatabase(Context context) {
        this.Context = context;
        WritableDatabase = TestbedDatabaseHelper.getInstance(context).getWritableDatabase();
        ReadableDatabase = TestbedDatabaseHelper.getInstance(context).getReadableDatabase();
    }

    private class InsertQuery {
        String TableName;
        ContentValues NewRecord;

        private InsertQuery(String tableName, ContentValues newRecord) {
            this.TableName = tableName;
            this.NewRecord = newRecord;
        }
    }

    private class UpdateQuery {
        String TableName;
        ContentValues Values;
        String WhereClause;
        String[] WhereArgs;

        private UpdateQuery(String tableName, ContentValues values, String whereClause, String[] whereArgs) {
            this.TableName = tableName;
            this.Values = values;
            this.WhereClause = whereClause;
            this.WhereArgs = whereArgs;
        }
    }

    private class SelectQuery {
        String TableName;
        String[] Columns;
        String Selection;
        String[] SelectionArgs;
        String GroupBy;
        String Having;
        String OrderBy;

        private SelectQuery(String tableName, String[] columns, String selection, String[] selectionArgs,
                            String groupBy, String having, String orderBy) {
            this.TableName = tableName;
            this.Columns = columns;
            this.Selection = selection;
            this.SelectionArgs = selectionArgs;
            this.GroupBy = groupBy;
            this.Having = having;
            this.OrderBy = orderBy;
        }

        ;
    }

    public class Record{
        private int Id;
        private float Value;
        private Date TimeStamp;

        private Record(int id, float value, long timeStamp){
            Id = id;
            Value = value;
            TimeStamp = new Date(timeStamp);
        }

        public int getId(){
            return Id;
        }

        public float getValue() {
            return Value;
        }

        public Date getTimeStamp() {
            return TimeStamp;
        }

        public String getStringTimeStamp(){
            SimpleDateFormat simpleFormatter = new SimpleDateFormat("HH:mm:ss.SSS dd. MM. YYYY");
            return simpleFormatter.format(TimeStamp);
        }
    }

    public void insertTestbedDevice(TestbedDevice testbedDevice) {
        ContentValues newRecord = new ContentValues();
        newRecord.put(TestbedDatabaseHelper.Device.COLUMN_NAME_DEVICE_ID, testbedDevice.getDeviceId());
        newRecord.put(TestbedDatabaseHelper.Device.COLUMN_NAME_AVAILABLE_SENSORS, testbedDevice.getAvailableSensors());
        newRecord.put(TestbedDatabaseHelper.Device.COLUMN_NAME_BLUETOOTH_MAC_ADDRESS, testbedDevice.getBluetoothDevice().getAddress());
        newRecord.put(TestbedDatabaseHelper.Device.COLUMN_NAME_IS_LAST_CONNECTED, false);
        newRecord.put(TestbedDatabaseHelper.Device.COLUMN_NAME_TIMESTAMP, System.currentTimeMillis());

        InsertQuery insertQuery = new InsertQuery(TestbedDatabaseHelper.Device.TABLE_NAME, newRecord);

        InsertIntoDatabase insertIntoDatabase = new InsertIntoDatabase();
        insertIntoDatabase.execute(insertQuery);
    }

    public void insertSteps(int id, int steps) {
        ContentValues newRecord = new ContentValues();
        newRecord.put(TestbedDatabaseHelper.Data.COLUMN_NAME_DEVICE_ID, id);
        newRecord.put(TestbedDatabaseHelper.Data.COLUMN_NAME_DATA_KEY, BluetoothLeService.STEPS_DATA);
        newRecord.put(TestbedDatabaseHelper.Data.COLUMN_NAME_DATA_VALUE, (float) steps);
        newRecord.put(TestbedDatabaseHelper.Data.COLUMN_NAME_TIMESTAMP, System.currentTimeMillis());

        InsertQuery insertQuery = new InsertQuery(TestbedDatabaseHelper.Data.TABLE_NAME, newRecord);
        InsertIntoDatabase insertIntoDatabase = new InsertIntoDatabase();
        insertIntoDatabase.execute(insertQuery);
    }

    public void insertHeartRate(int id, int heartRate) {
        ContentValues newRecord = new ContentValues();
        newRecord.put(TestbedDatabaseHelper.Data.COLUMN_NAME_DEVICE_ID, id);
        newRecord.put(TestbedDatabaseHelper.Data.COLUMN_NAME_DATA_KEY, BluetoothLeService.HEART_RATE_DATA);
        newRecord.put(TestbedDatabaseHelper.Data.COLUMN_NAME_DATA_VALUE, (float) heartRate);
        newRecord.put(TestbedDatabaseHelper.Data.COLUMN_NAME_TIMESTAMP, System.currentTimeMillis());

        InsertQuery insertQuery = new InsertQuery(TestbedDatabaseHelper.Data.TABLE_NAME, newRecord);
        InsertIntoDatabase insertIntoDatabase = new InsertIntoDatabase();
        insertIntoDatabase.execute(insertQuery);
    }

    public void insertTemperature(int id, float temperature) {
        ContentValues newRecord = new ContentValues();
        newRecord.put(TestbedDatabaseHelper.Data.COLUMN_NAME_DEVICE_ID, id);
        newRecord.put(TestbedDatabaseHelper.Data.COLUMN_NAME_DATA_KEY, BluetoothLeService.TEMPERATURE_DATA);
        newRecord.put(TestbedDatabaseHelper.Data.COLUMN_NAME_DATA_VALUE, temperature);
        newRecord.put(TestbedDatabaseHelper.Data.COLUMN_NAME_TIMESTAMP, System.currentTimeMillis());

        InsertQuery insertQuery = new InsertQuery(TestbedDatabaseHelper.Data.TABLE_NAME, newRecord);
        InsertIntoDatabase insertIntoDatabase = new InsertIntoDatabase();
        insertIntoDatabase.execute(insertQuery);
    }

    public void updateLastConnectedTestbedDevice(final TestbedDevice testbedDevice) {
        final ContentValues oldRecord = new ContentValues();
        oldRecord.put(TestbedDatabaseHelper.Device.COLUMN_NAME_IS_LAST_CONNECTED, false);

        final ContentValues newRecord = new ContentValues();
        newRecord.put(TestbedDatabaseHelper.Device.COLUMN_NAME_IS_LAST_CONNECTED, true);

        final String whereClause = TestbedDatabaseHelper.Device.COLUMN_NAME_DEVICE_ID + " = ?";

        selectLastConnectedTestbedDevice(new OnGetLastConnectedTestbedDevice() {
            @Override
            public void onLastConnectedSuccess(int id, String macAddress) {
                String[] oldWhereArgs = new String[]{Integer.toString(id)};
                String[] newWhereArgs = new String[]{Integer.toString(testbedDevice.getDeviceId())};

                UpdateQuery oldQuery = new UpdateQuery(TestbedDatabaseHelper.Device.TABLE_NAME, oldRecord, whereClause, oldWhereArgs);
                UpdateQuery newQuery = new UpdateQuery(TestbedDatabaseHelper.Device.TABLE_NAME, newRecord, whereClause, newWhereArgs);

                UpdateIntoDatabase updateOldRecord = new UpdateIntoDatabase();
                UpdateIntoDatabase updateNewRecord = new UpdateIntoDatabase();
                updateOldRecord.execute(oldQuery);
                updateNewRecord.execute(newQuery);
            }

            @Override
            public void onLastConnectResultFail() {
                String[] newWhereArgs = new String[]{Integer.toString(testbedDevice.getDeviceId())};
                UpdateQuery newQuery = new UpdateQuery(TestbedDatabaseHelper.Device.TABLE_NAME, newRecord, whereClause, newWhereArgs);
                UpdateIntoDatabase updateNewRecord = new UpdateIntoDatabase();
                updateNewRecord.execute(newQuery);
            }
        });
    }

    public void selectStoredTestbedDevice(TestbedDevice testbedDevice, OnIsTestbedDeviceStored onIsTestbedDeviceStored) {
        this.OnIsTestbedDeviceStored = onIsTestbedDeviceStored;
        String tableName = TestbedDatabaseHelper.Device.TABLE_NAME;
        String[] columns = new String[]{TestbedDatabaseHelper.Device.COLUMN_NAME_DEVICE_ID,
                TestbedDatabaseHelper.Device.COLUMN_NAME_AVAILABLE_SENSORS,
                TestbedDatabaseHelper.Device.COLUMN_NAME_BLUETOOTH_MAC_ADDRESS,
                TestbedDatabaseHelper.Device.COLUMN_NAME_TIMESTAMP};
        String selection = TestbedDatabaseHelper.Device.COLUMN_NAME_DEVICE_ID + " = ?";
        String[] selectionArgs = new String[]{Integer.toString(testbedDevice.getDeviceId())};
        SelectQuery selectQuery = new SelectQuery(tableName, columns, selection, selectionArgs, null, null, null);
        SelectDeviceById selectDeviceById = new SelectDeviceById(testbedDevice);
        selectDeviceById.execute(selectQuery);
    }

    public void selectLastConnectedTestbedDevice(OnGetLastConnectedTestbedDevice onGetLastConnectedTestbedDevice) {
        this.OnGetLastConnectedTestbedDevice = onGetLastConnectedTestbedDevice;
        String tableName = TestbedDatabaseHelper.Device.TABLE_NAME;
        String[] columns = new String[]{TestbedDatabaseHelper.Device.COLUMN_NAME_DEVICE_ID,
                TestbedDatabaseHelper.Device.COLUMN_NAME_BLUETOOTH_MAC_ADDRESS};
        String selection = TestbedDatabaseHelper.Device.COLUMN_NAME_IS_LAST_CONNECTED + " = ?";
        String[] selectionArgs = new String[]{Integer.toString(1)};
        SelectQuery selectQuery = new SelectQuery(tableName, columns, selection, selectionArgs, null, null, null);
        SelectLastConnectedDevice selectLastConnectedDevice = new SelectLastConnectedDevice();
        selectLastConnectedDevice.execute(selectQuery);

    }

    public void selectTemperatureDataOld(TestbedDevice testbedDevice, Date startDate, Date endDate, OnSelectTemperatureDataOld onSelectTemperatureData) {
        this.onSelectTemperatureDataOld = onSelectTemperatureData;
        String tableName = TestbedDatabaseHelper.Data.TABLE_NAME;
        String[] columns = new String[]{TestbedDatabaseHelper.Data.COLUMN_NAME_DATA_ID,
                TestbedDatabaseHelper.Data.COLUMN_NAME_DATA_VALUE,
                TestbedDatabaseHelper.Data.COLUMN_NAME_TIMESTAMP};
        String selection = TestbedDatabaseHelper.Data.COLUMN_NAME_DEVICE_ID + " = ? AND " +
                TestbedDatabaseHelper.Data.COLUMN_NAME_DATA_KEY + " = ? AND " +
                TestbedDatabaseHelper.Data.COLUMN_NAME_TIMESTAMP + " BETWEEN ? AND ?";
        String[] selectionArgs = new String[]{Integer.toString(testbedDevice.getDeviceId()),
                BluetoothLeService.TEMPERATURE_DATA,
                Long.toString(startDate.getTime()),
                Long.toString(endDate.getTime())};
        SelectQuery selectQuery = new SelectQuery(tableName, columns, selection, selectionArgs, null , null, null);
        SelectTemperatureOld selectTemperature = new SelectTemperatureOld();
        selectTemperature.execute(selectQuery);
    }


    public void selectDataBetweenIntervals(TestbedDevice testbedDevice, Date startDate, Date endDate, int sorting, OnSelectFloatData onSelectStepData){
        OnSelectStepData = onSelectStepData;
        String tableName = TestbedDatabaseHelper.Data.TABLE_NAME;
        String[] columns = new String[]{TestbedDatabaseHelper.Data.COLUMN_NAME_DATA_VALUE,
                TestbedDatabaseHelper.Data.COLUMN_NAME_TIMESTAMP};
        String selection = TestbedDatabaseHelper.Data.COLUMN_NAME_DEVICE_ID + " = ? AND " +
                TestbedDatabaseHelper.Data.COLUMN_NAME_DATA_KEY + " = ?";
        String[] selectionArgs = new String[]{Integer.toString(testbedDevice.getDeviceId()),
                BluetoothLeService.STEPS_DATA};
        SelectQuery selectQuery = new SelectQuery(tableName, columns, selection, selectionArgs, null , null,  TestbedDatabaseHelper.Data.COLUMN_NAME_TIMESTAMP + " ASC");
        SelectStepData selectStepData = new SelectStepData(sorting, startDate, endDate);
        selectStepData.execute(selectQuery);
    }


    public void selectStepData(TestbedDevice testbedDevice, Date startDate, Date endDate, int sorting, OnSelectFloatData onSelectStepData){
        OnSelectStepData = onSelectStepData;
        String tableName = TestbedDatabaseHelper.Data.TABLE_NAME;
        String[] columns = new String[]{TestbedDatabaseHelper.Data.COLUMN_NAME_DATA_VALUE,
                TestbedDatabaseHelper.Data.COLUMN_NAME_TIMESTAMP};
        String selection = TestbedDatabaseHelper.Data.COLUMN_NAME_DEVICE_ID + " = ? AND " +
                TestbedDatabaseHelper.Data.COLUMN_NAME_DATA_KEY + " = ?";
        String[] selectionArgs = new String[]{Integer.toString(testbedDevice.getDeviceId()),
                BluetoothLeService.STEPS_DATA};
        SelectQuery selectQuery = new SelectQuery(tableName, columns, selection, selectionArgs, null , null,  TestbedDatabaseHelper.Data.COLUMN_NAME_TIMESTAMP + " ASC");
        SelectStepData selectStepData = new SelectStepData(sorting, startDate, endDate);
        selectStepData.execute(selectQuery);
    }

    public void selectHeartRateData(TestbedDevice testbedDevice, Date startDate, Date endDate, int sorting, OnSelectFloatData onSelectHeartRateData){
        this.OnSelectHeartRateData = onSelectHeartRateData;
        String tableName = TestbedDatabaseHelper.Data.TABLE_NAME;
        String[] columns = new String[]{TestbedDatabaseHelper.Data.COLUMN_NAME_DATA_VALUE,
                TestbedDatabaseHelper.Data.COLUMN_NAME_TIMESTAMP};
        String selection = TestbedDatabaseHelper.Data.COLUMN_NAME_DEVICE_ID + " = ? AND " +
                TestbedDatabaseHelper.Data.COLUMN_NAME_DATA_KEY + " = ? AND " +
                TestbedDatabaseHelper.Data.COLUMN_NAME_TIMESTAMP + " BETWEEN ? AND ?";
        String[] selectionArgs = new String[]{Integer.toString(testbedDevice.getDeviceId()),
                BluetoothLeService.HEART_RATE_DATA,
                Long.toString(startDate.getTime()),
                Long.toString(endDate.getTime())};
        SelectQuery selectQuery = new SelectQuery(tableName, columns, selection, selectionArgs, null , null,  TestbedDatabaseHelper.Data.COLUMN_NAME_TIMESTAMP + " ASC");
        SelectHeartRateData selectHeartRateData = new SelectHeartRateData(sorting);
        selectHeartRateData.execute(selectQuery);
    }

    public void selectTemperatureData(TestbedDevice testbedDevice, Date startDate, Date endDate, int sorting, OnSelectFloatData onSelectTemperatureData){
        this.OnSelectTemperatureData = onSelectTemperatureData;
        String tableName = TestbedDatabaseHelper.Data.TABLE_NAME;
        String[] columns = new String[]{TestbedDatabaseHelper.Data.COLUMN_NAME_DATA_VALUE,
                TestbedDatabaseHelper.Data.COLUMN_NAME_TIMESTAMP};
        String selection = TestbedDatabaseHelper.Data.COLUMN_NAME_DEVICE_ID + " = ? AND " +
                TestbedDatabaseHelper.Data.COLUMN_NAME_DATA_KEY + " = ? AND " +
                TestbedDatabaseHelper.Data.COLUMN_NAME_TIMESTAMP + " BETWEEN ? AND ?";
        String[] selectionArgs = new String[]{Integer.toString(testbedDevice.getDeviceId()),
                BluetoothLeService.TEMPERATURE_DATA,
                Long.toString(startDate.getTime()),
                Long.toString(endDate.getTime())};
        SelectQuery selectQuery = new SelectQuery(tableName, columns, selection, selectionArgs, null , null,  TestbedDatabaseHelper.Data.COLUMN_NAME_TIMESTAMP + " ASC");
        SelectTemperatureData selectTemperatureData = new SelectTemperatureData(sorting);
        selectTemperatureData.execute(selectQuery);

    }

    private class InsertIntoDatabase extends AsyncTask<InsertQuery, Void, Void>{
        @Override
        protected Void doInBackground(InsertQuery... insertQueries) {
            for (int i = 0; i < insertQueries.length; i++) {
                WritableDatabase.insert(insertQueries[i].TableName, null, insertQueries[i].NewRecord);
            }
            return null;
        }

        @Override
        protected void onPostExecute(Void aVoid) {
            super.onPostExecute(aVoid);
            //OnInsertIntoDatabase.onInsertCompleted();
        }
    }

    private class UpdateIntoDatabase extends AsyncTask<UpdateQuery, Void, Void>{

        @Override
        protected Void doInBackground(UpdateQuery... updateQueries) {
            for (int i = 0; i < updateQueries.length; i++) {
                WritableDatabase.update(
                    updateQueries[i].TableName,
                    updateQueries[i].Values,
                    updateQueries[i].WhereClause,
                    updateQueries[i].WhereArgs
                );
            }
            return null;
        }

        @Override
        protected void onPostExecute(Void aVoid) {
            super.onPostExecute(aVoid);
            //OnUpdateIntoDatabase.onUpdatedCompleted();
        }
    }

    abstract private class SelectFromDatabase extends AsyncTask<SelectQuery, Void, List<Cursor>> {

        @Override
        protected List<Cursor> doInBackground(SelectQuery... selectQueries) {
            List<Cursor> records = new ArrayList<>();
            for (int i = 0; i < selectQueries.length; i++) {
                Cursor readCursor = ReadableDatabase.query(
                        // The table to query
                        selectQueries[i].TableName,
                        // The array of columns to return (pass null to get all)
                        selectQueries[i].Columns,
                        // The columns for the WHERE clause
                        selectQueries[i].Selection,
                        // The values for the WHERE clause
                        selectQueries[i].SelectionArgs,
                        //new String[]{Integer.toString(26407), BluetoothLeService.TEMPERATURE},
                        // don't group the rows
                        selectQueries[i].GroupBy,
                        // don't filter by row groups
                        selectQueries[i].Having,
                        // The sort order
                        selectQueries[i].OrderBy
                );
                records.add(readCursor);

            }
            return records;
        }
    }

    private class SelectLastConnectedDevice extends SelectFromDatabase{

        @Override
        protected void onPostExecute(List<Cursor> cursors) {
            super.onPostExecute(cursors);
            for (Cursor cursor : cursors) {
                if (cursor.getCount() == 0) {
                    OnGetLastConnectedTestbedDevice.onLastConnectResultFail();
                    Log.w(this.getClass().getSimpleName(), "no last connected device");
                } else {
                    while (cursor.moveToNext()) {
                        OnGetLastConnectedTestbedDevice.onLastConnectedSuccess(cursor.getInt(cursor.getColumnIndexOrThrow(TestbedDatabaseHelper.Device.COLUMN_NAME_DEVICE_ID)),
                                cursor.getString(cursor.getColumnIndexOrThrow(TestbedDatabaseHelper.Device.COLUMN_NAME_BLUETOOTH_MAC_ADDRESS)));
                    }
                    cursor.close();
                }
            }

        }
    }

    private class SelectDeviceById extends SelectFromDatabase{

        private TestbedDevice TestbedDevice;

        public SelectDeviceById(TestbedDevice testbedDevice) {
            TestbedDevice = testbedDevice;
        }

        @Override
        protected void onPostExecute(List<Cursor> cursors) {
            super.onPostExecute(cursors);
            for (Cursor cursor : cursors) {
                if (cursor.getCount() == 0) {
                    OnIsTestbedDeviceStored.onStoredResult(TestbedDevice.NOT_STORED);
                } else {
                    while (cursor.moveToNext()) {
                        if (cursor.getLong(cursor.getColumnIndexOrThrow(TestbedDatabaseHelper.Device.COLUMN_NAME_DEVICE_ID)) == TestbedDevice.getDeviceId()) {
                            if (cursor.getLong(cursor.getColumnIndexOrThrow(TestbedDatabaseHelper.Device.COLUMN_NAME_AVAILABLE_SENSORS)) == TestbedDevice.getAvailableSensors() &&
                                    cursor.getString(cursor.getColumnIndexOrThrow(TestbedDatabaseHelper.Device.COLUMN_NAME_BLUETOOTH_MAC_ADDRESS)).equals(TestbedDevice.getBluetoothDevice().getAddress()) &&
                                    cursor.getLong(cursor.getColumnIndexOrThrow(TestbedDatabaseHelper.Device.COLUMN_NAME_TIMESTAMP)) <= System.currentTimeMillis()) {
                                OnIsTestbedDeviceStored.onStoredResult(TestbedDevice.STORED_CONSISTENTLY);
                            } else {
                                OnIsTestbedDeviceStored.onStoredResult(TestbedDevice.STORED_BUT_MODIFIED);
                            }
                        }
                        cursor.close();
                    }
                }
            }
        }
    }

    public class SelectTemperatureOld extends SelectFromDatabase {

        @Override
        protected void onPostExecute(List<Cursor> cursors) {
            super.onPostExecute(cursors);
            for (Cursor cursor : cursors) {
                if (cursor.getCount() == 0) {
                    onSelectTemperatureDataOld.onSelectFailed();
                } else {
                    List<Record> records = new ArrayList<>();
                    while (cursor.moveToNext()) {
                        records.add(new Record(cursor.getInt(cursor.getColumnIndexOrThrow(TestbedDatabaseHelper.Data.COLUMN_NAME_DATA_ID)),
                                               cursor.getFloat(cursor.getColumnIndexOrThrow(TestbedDatabaseHelper.Data.COLUMN_NAME_DATA_VALUE)),
                                               cursor.getLong(cursor.getColumnIndexOrThrow(TestbedDatabaseHelper.Data.COLUMN_NAME_TIMESTAMP))));
                    }
                    cursor.close();
                    onSelectTemperatureDataOld.onSelectSuccess(records);
                }
            }
        }
    }

    public class SelectStepData extends SelectFromDatabase {

        private final int Sorting;
        private final Date StartDate;
        private final Date EndDate;
        private float PreviousStepDataValue;
        private long PreviousStepDataTimeStamp;

        private void findStart(Cursor cursor){
            do{
                if(StartDate.getTime() >= cursor.getLong(cursor.getColumnIndexOrThrow(TestbedDatabaseHelper.Data.COLUMN_NAME_TIMESTAMP))){
                    break;
                }
                PreviousStepDataValue = cursor.getFloat(cursor.getColumnIndexOrThrow(TestbedDatabaseHelper.Data.COLUMN_NAME_DATA_VALUE));
                PreviousStepDataTimeStamp = cursor.getLong(cursor.getColumnIndexOrThrow(TestbedDatabaseHelper.Data.COLUMN_NAME_TIMESTAMP));
            } while (cursor.moveToNext());
        }

        public SelectStepData(int sorting, Date startDate, Date endDate) {
            Sorting = sorting;
            StartDate = startDate;
            EndDate = endDate;
        }

        @Override
        protected void onPostExecute(List<Cursor> cursors) {
            super.onPostExecute(cursors);
            ArrayList<Map<Integer,Float>> statisticDataByIntervals;
            Map<Integer, Float> statisticData;
            ArrayList<List<Integer>> rawDataByIntervals;
            int currentTimeValue;
            int firstX, lastX;
            int sum;
            int minValue;
            int maxValue;
            float mean;
            float median;
            float standardDeviation;
            int lowQuartile, highQuartile;
            Calendar calendar = Calendar.getInstance();
            Log.w(TAG, "jsem v postexecute");
            for (Cursor cursor : cursors) {
                if (cursor.getCount() == 0) {
                    OnSelectHeartRateData.onSelectFailed();
                    Log.w(TAG, "jsem v selhal jsem");
                } else {
                    findStart(cursor);
                    Calendar firstRecordTimeStamp = Calendar.getInstance();
                    firstRecordTimeStamp.setTime(new Date(cursor.getLong(cursor.getColumnIndexOrThrow(TestbedDatabaseHelper.Data.COLUMN_NAME_TIMESTAMP))));
                    cursor.moveToLast();
                    Calendar lastRecordTimeStamp = Calendar.getInstance();
                    lastRecordTimeStamp.setTime(new Date(cursor.getLong(cursor.getColumnIndexOrThrow(TestbedDatabaseHelper.Data.COLUMN_NAME_TIMESTAMP))));
                    cursor.moveToFirst();
                    switch (Sorting) {
                        case YEAR:
                            firstX = firstRecordTimeStamp.get(Calendar.YEAR);
                            lastX = lastRecordTimeStamp.get(Calendar.YEAR);
                            rawDataByIntervals = new ArrayList (Collections.nCopies (lastX - firstX + 1, null));
                            statisticDataByIntervals = new ArrayList(Collections.nCopies (lastX - firstX + 1, null));
                            do{
                                calendar.setTimeInMillis(cursor.getLong(cursor.getColumnIndexOrThrow(TestbedDatabaseHelper.Data.COLUMN_NAME_TIMESTAMP)));
                                currentTimeValue = calendar.get(Calendar.YEAR) - firstX;
                                if(rawDataByIntervals.get(currentTimeValue) == null) {
                                    rawDataByIntervals.set(currentTimeValue, new ArrayList<Integer>());
                                }
                                rawDataByIntervals.get(currentTimeValue).add(cursor.getInt(cursor.getColumnIndexOrThrow(TestbedDatabaseHelper.Data.COLUMN_NAME_DATA_VALUE)));
                            } while (cursor.moveToNext());
                            break;
                        case MONTH:
                            firstX = firstRecordTimeStamp.getActualMinimum(Calendar.MONTH) + 1;
                            lastX = lastRecordTimeStamp.getActualMaximum(Calendar.MONTH) + 1;
                            rawDataByIntervals = new ArrayList (Collections.nCopies (lastX, null));
                            statisticDataByIntervals = new ArrayList (Collections.nCopies (lastX, null));
                            do {
                                calendar.setTimeInMillis(cursor.getLong(cursor.getColumnIndexOrThrow(TestbedDatabaseHelper.Data.COLUMN_NAME_TIMESTAMP)));
                                currentTimeValue = calendar.get(Calendar.MONTH);
                                if(rawDataByIntervals.get(currentTimeValue) == null) {
                                    rawDataByIntervals.set(currentTimeValue, new ArrayList<Integer>());
                                }
                                rawDataByIntervals.get(currentTimeValue).add(cursor.getInt(cursor.getColumnIndexOrThrow(TestbedDatabaseHelper.Data.COLUMN_NAME_DATA_VALUE)));
                            } while (cursor.moveToNext());
                            break;
                        case DAY_OF_MONTH:
                            firstX = firstRecordTimeStamp.getActualMinimum(Calendar.DAY_OF_MONTH);
                            lastX = lastRecordTimeStamp.getActualMaximum(Calendar.DAY_OF_MONTH);
                            rawDataByIntervals = new ArrayList (Collections.nCopies (lastX, null));
                            statisticDataByIntervals = new ArrayList (Collections.nCopies (lastX, null));
                            do {
                                calendar.setTimeInMillis(cursor.getLong(cursor.getColumnIndexOrThrow(TestbedDatabaseHelper.Data.COLUMN_NAME_TIMESTAMP)));
                                currentTimeValue = calendar.get(Calendar.DAY_OF_MONTH) - 1;
                                if(rawDataByIntervals.get(currentTimeValue) == null) {
                                    rawDataByIntervals.set(currentTimeValue, new ArrayList<Integer>());
                                }
                                rawDataByIntervals.get(currentTimeValue).add(cursor.getInt(cursor.getColumnIndexOrThrow(TestbedDatabaseHelper.Data.COLUMN_NAME_DATA_VALUE)));
                            } while (cursor.moveToNext());
                            break;
                        case HOUR_OF_DAY:
                            firstX = firstRecordTimeStamp.getActualMinimum(Calendar.HOUR_OF_DAY);
                            lastX = lastRecordTimeStamp.getActualMaximum(Calendar.HOUR_OF_DAY);
                            rawDataByIntervals = new ArrayList (Collections.nCopies (lastX + 1, null));
                            statisticDataByIntervals = new ArrayList (Collections.nCopies (lastX + 1, null));
                            do {
                                calendar.setTimeInMillis(cursor.getLong(cursor.getColumnIndexOrThrow(TestbedDatabaseHelper.Data.COLUMN_NAME_TIMESTAMP)));
                                currentTimeValue = calendar.get(Calendar.HOUR_OF_DAY);
                                if(rawDataByIntervals.get(currentTimeValue) == null) {
                                    rawDataByIntervals.set(currentTimeValue, new ArrayList<Integer>());
                                }
                                rawDataByIntervals.get(currentTimeValue).add(cursor.getInt(cursor.getColumnIndexOrThrow(TestbedDatabaseHelper.Data.COLUMN_NAME_DATA_VALUE)));
                            } while (cursor.moveToNext());
                            break;

                        case MINUTE:
                            firstX = firstRecordTimeStamp.getActualMinimum(Calendar.MINUTE);
                            lastX = lastRecordTimeStamp.getActualMaximum(Calendar.MINUTE);
                            rawDataByIntervals = new ArrayList (Collections.nCopies (lastX + 1, null));
                            statisticDataByIntervals = new ArrayList (Collections.nCopies (lastX + 1, null));
                            do {
                                calendar.setTimeInMillis(cursor.getLong(cursor.getColumnIndexOrThrow(TestbedDatabaseHelper.Data.COLUMN_NAME_TIMESTAMP)));
                                currentTimeValue = calendar.get(Calendar.MINUTE);
                                if(rawDataByIntervals.get(currentTimeValue) == null) {
                                    rawDataByIntervals.set(currentTimeValue, new ArrayList<Integer>());
                                }
                                rawDataByIntervals.get(currentTimeValue).add(cursor.getInt(cursor.getColumnIndexOrThrow(TestbedDatabaseHelper.Data.COLUMN_NAME_DATA_VALUE)));
                            } while (cursor.moveToNext());
                            break;

                        case SECONDS:
                            firstX = firstRecordTimeStamp.getActualMinimum(Calendar.SECOND);
                            lastX = lastRecordTimeStamp.getActualMaximum(Calendar.SECOND);
                            rawDataByIntervals = new ArrayList (Collections.nCopies (lastX + 1, null));
                            statisticDataByIntervals = new ArrayList (Collections.nCopies (lastX + 1, null));
                            do {
                                calendar.setTimeInMillis(cursor.getLong(cursor.getColumnIndexOrThrow(TestbedDatabaseHelper.Data.COLUMN_NAME_TIMESTAMP)));
                                currentTimeValue = calendar.get(Calendar.SECOND);
                                if(rawDataByIntervals.get(currentTimeValue) == null) {
                                    rawDataByIntervals.set(currentTimeValue, new ArrayList<Integer>());
                                }
                                rawDataByIntervals.get(currentTimeValue).add(cursor.getInt(cursor.getColumnIndexOrThrow(TestbedDatabaseHelper.Data.COLUMN_NAME_DATA_VALUE)));
                            } while (cursor.moveToNext());
                            break;
                        default:
                            throw new IllegalStateException("Unexpected value: " + Sorting);
                    }
                    cursor.close();

                    for (int i = 0 ; i < rawDataByIntervals.size() ; i++ ){
                        minValue = HEART_RATE_SENSOR_MAX_VALUE;
                        maxValue = HEART_RATE_SENSOR_MIN_VALUE;
                        sum = 0;
                        if (rawDataByIntervals.get(i) != null) {
                            Collections.sort(rawDataByIntervals.get(i));
                            lowQuartile = rawDataByIntervals.get(i).get((int) Math.ceil(rawDataByIntervals.get(i).size() * 25/100));
                            highQuartile = rawDataByIntervals.get(i).get((int) Math.ceil(rawDataByIntervals.get(i).size() * 75/100));
                            for (int teplota : rawDataByIntervals.get(i)) {
                                if (minValue > teplota) {
                                    minValue = teplota;
                                }
                                if (maxValue < teplota) {
                                    maxValue = teplota;
                                }
                                sum = sum + teplota;
                            }
                            mean = sum / rawDataByIntervals.get(i).size();

                            statisticData = new HashMap<>();
                            statisticData.put(DATA_SET_SIZE, (float) rawDataByIntervals.get(i).size());
                            statisticData.put(MIN_VALUE, (float) minValue);
                            statisticData.put(MAX_VALUE, (float) maxValue);
                            statisticData.put(LOW_QUARTILE, (float) lowQuartile);
                            statisticData.put(HIGH_QUARTILE, (float) highQuartile);
                            statisticData.put(MEAN_VALUE, mean);

                            statisticDataByIntervals.set(i, statisticData);
                        }
                    }
                    OnSelectHeartRateData.onSelectSuccess(statisticDataByIntervals, firstX, lastX);
                    Log.w(TAG, "jsem pryč z postexecute");
                }
            }

        }
    }

    public class SelectHeartRateData extends SelectFromDatabase {

        int Sorting;

        public SelectHeartRateData(int sorting) {
            Sorting = sorting;
        }

        @Override
        protected void onPostExecute(List<Cursor> cursors) {
            super.onPostExecute(cursors);
            ArrayList<Map<Integer,Float>> statisticDataByIntervals;
            Map<Integer, Float> statisticData;
            ArrayList<List<Integer>> rawDataByIntervals;
            int currentTimeValue;
            int firstX, lastX;
            int sum;
            int minValue;
            int maxValue;
            float mean;
            float median;
            float standardDeviation;
            int lowQuartile, highQuartile;
            Calendar calendar = Calendar.getInstance();
            Log.w(TAG, "jsem v postexecute");
            for (Cursor cursor : cursors) {
                if (cursor.getCount() == 0) {
                    OnSelectHeartRateData.onSelectFailed();
                    Log.w(TAG, "jsem v selhal jsem");
                } else {
                    cursor.moveToFirst();
                    Calendar firstRecordTimeStamp = Calendar.getInstance();
                    firstRecordTimeStamp.setTime(new Date(cursor.getLong(cursor.getColumnIndexOrThrow(TestbedDatabaseHelper.Data.COLUMN_NAME_TIMESTAMP))));
                    cursor.moveToLast();
                    Calendar lastRecordTimeStamp = Calendar.getInstance();
                    lastRecordTimeStamp.setTime(new Date(cursor.getLong(cursor.getColumnIndexOrThrow(TestbedDatabaseHelper.Data.COLUMN_NAME_TIMESTAMP))));
                    cursor.moveToFirst();
                    switch (Sorting) {
                        case YEAR:
                            firstX = firstRecordTimeStamp.get(Calendar.YEAR);
                            lastX = lastRecordTimeStamp.get(Calendar.YEAR);
                            rawDataByIntervals = new ArrayList (Collections.nCopies (lastX - firstX + 1, null));
                            statisticDataByIntervals = new ArrayList(Collections.nCopies (lastX - firstX + 1, null));
                            do{
                                calendar.setTimeInMillis(cursor.getLong(cursor.getColumnIndexOrThrow(TestbedDatabaseHelper.Data.COLUMN_NAME_TIMESTAMP)));
                                currentTimeValue = calendar.get(Calendar.YEAR) - firstX;
                                if(rawDataByIntervals.get(currentTimeValue) == null) {
                                    rawDataByIntervals.set(currentTimeValue, new ArrayList<Integer>());
                                }
                                rawDataByIntervals.get(currentTimeValue).add(cursor.getInt(cursor.getColumnIndexOrThrow(TestbedDatabaseHelper.Data.COLUMN_NAME_DATA_VALUE)));
                            } while (cursor.moveToNext());
                            break;
                        case MONTH:
                            firstX = firstRecordTimeStamp.getActualMinimum(Calendar.MONTH) + 1;
                            lastX = lastRecordTimeStamp.getActualMaximum(Calendar.MONTH) + 1;
                            rawDataByIntervals = new ArrayList (Collections.nCopies (lastX, null));
                            statisticDataByIntervals = new ArrayList (Collections.nCopies (lastX, null));
                            do {
                                calendar.setTimeInMillis(cursor.getLong(cursor.getColumnIndexOrThrow(TestbedDatabaseHelper.Data.COLUMN_NAME_TIMESTAMP)));
                                currentTimeValue = calendar.get(Calendar.MONTH);
                                if(rawDataByIntervals.get(currentTimeValue) == null) {
                                    rawDataByIntervals.set(currentTimeValue, new ArrayList<Integer>());
                                }
                                rawDataByIntervals.get(currentTimeValue).add(cursor.getInt(cursor.getColumnIndexOrThrow(TestbedDatabaseHelper.Data.COLUMN_NAME_DATA_VALUE)));
                            } while (cursor.moveToNext());
                            break;
                        case DAY_OF_MONTH:
                            firstX = firstRecordTimeStamp.getActualMinimum(Calendar.DAY_OF_MONTH);
                            lastX = lastRecordTimeStamp.getActualMaximum(Calendar.DAY_OF_MONTH);
                            rawDataByIntervals = new ArrayList (Collections.nCopies (lastX, null));
                            statisticDataByIntervals = new ArrayList (Collections.nCopies (lastX, null));
                            do {
                                calendar.setTimeInMillis(cursor.getLong(cursor.getColumnIndexOrThrow(TestbedDatabaseHelper.Data.COLUMN_NAME_TIMESTAMP)));
                                currentTimeValue = calendar.get(Calendar.DAY_OF_MONTH) - 1;
                                if(rawDataByIntervals.get(currentTimeValue) == null) {
                                    rawDataByIntervals.set(currentTimeValue, new ArrayList<Integer>());
                                }
                                rawDataByIntervals.get(currentTimeValue).add(cursor.getInt(cursor.getColumnIndexOrThrow(TestbedDatabaseHelper.Data.COLUMN_NAME_DATA_VALUE)));
                            } while (cursor.moveToNext());
                            break;
                        case HOUR_OF_DAY:
                            firstX = firstRecordTimeStamp.getActualMinimum(Calendar.HOUR_OF_DAY);
                            lastX = lastRecordTimeStamp.getActualMaximum(Calendar.HOUR_OF_DAY);
                            rawDataByIntervals = new ArrayList (Collections.nCopies (lastX + 1, null));
                            statisticDataByIntervals = new ArrayList (Collections.nCopies (lastX + 1, null));
                            do {
                                calendar.setTimeInMillis(cursor.getLong(cursor.getColumnIndexOrThrow(TestbedDatabaseHelper.Data.COLUMN_NAME_TIMESTAMP)));
                                currentTimeValue = calendar.get(Calendar.HOUR_OF_DAY);
                                if(rawDataByIntervals.get(currentTimeValue) == null) {
                                    rawDataByIntervals.set(currentTimeValue, new ArrayList<Integer>());
                                }
                                rawDataByIntervals.get(currentTimeValue).add(cursor.getInt(cursor.getColumnIndexOrThrow(TestbedDatabaseHelper.Data.COLUMN_NAME_DATA_VALUE)));
                            } while (cursor.moveToNext());
                            break;

                        case MINUTE:
                            firstX = firstRecordTimeStamp.getActualMinimum(Calendar.MINUTE);
                            lastX = lastRecordTimeStamp.getActualMaximum(Calendar.MINUTE);
                            rawDataByIntervals = new ArrayList (Collections.nCopies (lastX + 1, null));
                            statisticDataByIntervals = new ArrayList (Collections.nCopies (lastX + 1, null));
                            do {
                                calendar.setTimeInMillis(cursor.getLong(cursor.getColumnIndexOrThrow(TestbedDatabaseHelper.Data.COLUMN_NAME_TIMESTAMP)));
                                currentTimeValue = calendar.get(Calendar.MINUTE);
                                if(rawDataByIntervals.get(currentTimeValue) == null) {
                                    rawDataByIntervals.set(currentTimeValue, new ArrayList<Integer>());
                                }
                                rawDataByIntervals.get(currentTimeValue).add(cursor.getInt(cursor.getColumnIndexOrThrow(TestbedDatabaseHelper.Data.COLUMN_NAME_DATA_VALUE)));
                            } while (cursor.moveToNext());
                            break;

                        case SECONDS:
                            firstX = firstRecordTimeStamp.getActualMinimum(Calendar.SECOND);
                            lastX = lastRecordTimeStamp.getActualMaximum(Calendar.SECOND);
                            rawDataByIntervals = new ArrayList (Collections.nCopies (lastX + 1, null));
                            statisticDataByIntervals = new ArrayList (Collections.nCopies (lastX + 1, null));
                            do {
                                calendar.setTimeInMillis(cursor.getLong(cursor.getColumnIndexOrThrow(TestbedDatabaseHelper.Data.COLUMN_NAME_TIMESTAMP)));
                                currentTimeValue = calendar.get(Calendar.SECOND);
                                if(rawDataByIntervals.get(currentTimeValue) == null) {
                                    rawDataByIntervals.set(currentTimeValue, new ArrayList<Integer>());
                                }
                                rawDataByIntervals.get(currentTimeValue).add(cursor.getInt(cursor.getColumnIndexOrThrow(TestbedDatabaseHelper.Data.COLUMN_NAME_DATA_VALUE)));
                            } while (cursor.moveToNext());
                            break;
                        default:
                            throw new IllegalStateException("Unexpected value: " + Sorting);
                    }
                    cursor.close();

                    for (int i = 0 ; i < rawDataByIntervals.size() ; i++ ){
                        minValue = HEART_RATE_SENSOR_MAX_VALUE;
                        maxValue = HEART_RATE_SENSOR_MIN_VALUE;
                        sum = 0;
                        if (rawDataByIntervals.get(i) != null) {
                            Collections.sort(rawDataByIntervals.get(i));
                            lowQuartile = rawDataByIntervals.get(i).get((int) Math.ceil(rawDataByIntervals.get(i).size() * 25/100));
                            highQuartile = rawDataByIntervals.get(i).get((int) Math.ceil(rawDataByIntervals.get(i).size() * 75/100));
                            for (int teplota : rawDataByIntervals.get(i)) {
                                if (minValue > teplota) {
                                    minValue = teplota;
                                }
                                if (maxValue < teplota) {
                                    maxValue = teplota;
                                }
                                sum = sum + teplota;
                            }
                            mean = sum / rawDataByIntervals.get(i).size();

                            statisticData = new HashMap<>();
                            statisticData.put(DATA_SET_SIZE, (float) rawDataByIntervals.get(i).size());
                            statisticData.put(MIN_VALUE, (float) minValue);
                            statisticData.put(MAX_VALUE, (float) maxValue);
                            statisticData.put(LOW_QUARTILE, (float) lowQuartile);
                            statisticData.put(HIGH_QUARTILE, (float) highQuartile);
                            statisticData.put(MEAN_VALUE, mean);

                            statisticDataByIntervals.set(i, statisticData);
                        }
                    }
                    OnSelectHeartRateData.onSelectSuccess(statisticDataByIntervals, firstX, lastX);
                    Log.w(TAG, "jsem pryč z postexecute");
                }
            }

        }
    }

    public class SelectTemperatureData extends SelectFromDatabase {

        int Sorting;

        public SelectTemperatureData(int sorting) {
            Sorting = sorting;
        }

        @Override
        protected void onPostExecute(List<Cursor> cursors) {
            super.onPostExecute(cursors);
            ArrayList<Map<Integer,Float>> statisticDataByIntervals;
            Map<Integer, Float> statisticData;
            ArrayList<List<Float>> rawDataByIntervals;
            int currentTimeValue;
            int firstX, lastX;
            float sum;
            float minValue;
            float maxValue;
            float mean;
            float median;
            float standardDeviation;
            float lowQuartile, highQuartile;
            Calendar calendar = Calendar.getInstance();
            Log.w(TAG, "jsem v postexecute");
                  for (Cursor cursor : cursors) {
                if (cursor.getCount() == 0) {
                    OnSelectTemperatureData.onSelectFailed();
                    Log.w(TAG, "jsem v selhal jsem");
                } else {
                    cursor.moveToFirst();
                    Calendar firstRecordTimeStamp = Calendar.getInstance();
                    firstRecordTimeStamp.setTime(new Date(cursor.getLong(cursor.getColumnIndexOrThrow(TestbedDatabaseHelper.Data.COLUMN_NAME_TIMESTAMP))));
                    cursor.moveToLast();
                    Calendar lastRecordTimeStamp = Calendar.getInstance();
                    lastRecordTimeStamp.setTime(new Date(cursor.getLong(cursor.getColumnIndexOrThrow(TestbedDatabaseHelper.Data.COLUMN_NAME_TIMESTAMP))));
                    cursor.moveToFirst();
                    switch (Sorting) {
                        case YEAR:
                            firstX = firstRecordTimeStamp.get(Calendar.YEAR);
                            lastX = lastRecordTimeStamp.get(Calendar.YEAR);
                            rawDataByIntervals = new ArrayList (Collections.nCopies (lastX - firstX + 1, null));
                            statisticDataByIntervals = new ArrayList(Collections.nCopies (lastX - firstX + 1, null));
                            do {
                                calendar.setTimeInMillis(cursor.getLong(cursor.getColumnIndexOrThrow(TestbedDatabaseHelper.Data.COLUMN_NAME_TIMESTAMP)));
                                currentTimeValue = calendar.get(Calendar.YEAR) - firstX;
                                if(rawDataByIntervals.get(currentTimeValue) == null) {
                                    rawDataByIntervals.set(currentTimeValue, new ArrayList<Float>());
                                }
                                rawDataByIntervals.get(currentTimeValue).add(cursor.getFloat(cursor.getColumnIndexOrThrow(TestbedDatabaseHelper.Data.COLUMN_NAME_DATA_VALUE)));
                            } while (cursor.moveToNext());
                            break;
                        case MONTH:
                            firstX = firstRecordTimeStamp.getActualMinimum(Calendar.MONTH) + 1;
                            lastX = lastRecordTimeStamp.getActualMaximum(Calendar.MONTH) + 1;
                            rawDataByIntervals = new ArrayList (Collections.nCopies (lastX, null));
                            statisticDataByIntervals = new ArrayList (Collections.nCopies (lastX, null));
                            do {
                                calendar.setTimeInMillis(cursor.getLong(cursor.getColumnIndexOrThrow(TestbedDatabaseHelper.Data.COLUMN_NAME_TIMESTAMP)));
                                currentTimeValue = calendar.get(Calendar.MONTH);
                                if(rawDataByIntervals.get(currentTimeValue) == null) {
                                    rawDataByIntervals.set(currentTimeValue, new ArrayList<Float>());
                                }
                                rawDataByIntervals.get(currentTimeValue).add(cursor.getFloat(cursor.getColumnIndexOrThrow(TestbedDatabaseHelper.Data.COLUMN_NAME_DATA_VALUE)));
                            } while (cursor.moveToNext());
                            break;
                        case DAY_OF_MONTH:
                            firstX = firstRecordTimeStamp.getActualMinimum(Calendar.DAY_OF_MONTH);
                            lastX = lastRecordTimeStamp.getActualMaximum(Calendar.DAY_OF_MONTH);
                            rawDataByIntervals = new ArrayList (Collections.nCopies (lastX, null));
                            statisticDataByIntervals = new ArrayList (Collections.nCopies (lastX, null));
                           do {
                                calendar.setTimeInMillis(cursor.getLong(cursor.getColumnIndexOrThrow(TestbedDatabaseHelper.Data.COLUMN_NAME_TIMESTAMP)));
                                currentTimeValue = calendar.get(Calendar.DAY_OF_MONTH) - 1;
                                if(rawDataByIntervals.get(currentTimeValue) == null) {
                                    rawDataByIntervals.set(currentTimeValue, new ArrayList<Float>());
                                }
                                rawDataByIntervals.get(currentTimeValue).add(cursor.getFloat(cursor.getColumnIndexOrThrow(TestbedDatabaseHelper.Data.COLUMN_NAME_DATA_VALUE)));
                            } while (cursor.moveToNext());
                            break;
                        case HOUR_OF_DAY:
                            firstX = firstRecordTimeStamp.getActualMinimum(Calendar.HOUR_OF_DAY);
                            lastX = lastRecordTimeStamp.getActualMaximum(Calendar.HOUR_OF_DAY);
                            rawDataByIntervals = new ArrayList (Collections.nCopies (lastX + 1, null));
                            statisticDataByIntervals = new ArrayList (Collections.nCopies (lastX + 1, null));
                            do {
                                calendar.setTimeInMillis(cursor.getLong(cursor.getColumnIndexOrThrow(TestbedDatabaseHelper.Data.COLUMN_NAME_TIMESTAMP)));
                                currentTimeValue = calendar.get(Calendar.HOUR_OF_DAY);
                                if(rawDataByIntervals.get(currentTimeValue) == null) {
                                    rawDataByIntervals.set(currentTimeValue, new ArrayList<Float>());
                                }
                                rawDataByIntervals.get(currentTimeValue).add(cursor.getFloat(cursor.getColumnIndexOrThrow(TestbedDatabaseHelper.Data.COLUMN_NAME_DATA_VALUE)));
                            } while (cursor.moveToNext());
                            break;

                        case MINUTE:
                            firstX = firstRecordTimeStamp.getActualMinimum(Calendar.MINUTE);
                            lastX = lastRecordTimeStamp.getActualMaximum(Calendar.MINUTE);
                            rawDataByIntervals = new ArrayList (Collections.nCopies (lastX + 1, null));
                            statisticDataByIntervals = new ArrayList (Collections.nCopies (lastX + 1, null));
                            do {
                                calendar.setTimeInMillis(cursor.getLong(cursor.getColumnIndexOrThrow(TestbedDatabaseHelper.Data.COLUMN_NAME_TIMESTAMP)));
                                currentTimeValue = calendar.get(Calendar.MINUTE);
                                if(rawDataByIntervals.get(currentTimeValue) == null) {
                                    rawDataByIntervals.set(currentTimeValue, new ArrayList<Float>());
                                }
                                rawDataByIntervals.get(currentTimeValue).add(cursor.getFloat(cursor.getColumnIndexOrThrow(TestbedDatabaseHelper.Data.COLUMN_NAME_DATA_VALUE)));
                            }  while (cursor.moveToNext());
                            break;

                        case SECONDS:
                            firstX = firstRecordTimeStamp.getActualMinimum(Calendar.SECOND);
                            lastX = lastRecordTimeStamp.getActualMaximum(Calendar.SECOND);
                            rawDataByIntervals = new ArrayList (Collections.nCopies (lastX + 1, null));
                            statisticDataByIntervals = new ArrayList (Collections.nCopies (lastX + 1, null));
                            do {
                                calendar.setTimeInMillis(cursor.getLong(cursor.getColumnIndexOrThrow(TestbedDatabaseHelper.Data.COLUMN_NAME_TIMESTAMP)));
                                currentTimeValue = calendar.get(Calendar.SECOND);
                                if(rawDataByIntervals.get(currentTimeValue) == null) {
                                    rawDataByIntervals.set(currentTimeValue, new ArrayList<Float>());
                                }
                                rawDataByIntervals.get(currentTimeValue).add(cursor.getFloat(cursor.getColumnIndexOrThrow(TestbedDatabaseHelper.Data.COLUMN_NAME_DATA_VALUE)));
                            } while (cursor.moveToNext());
                            break;
                        default:
                            throw new IllegalStateException("Unexpected value: " + Sorting);
                    }
                    cursor.close();

                    for (int i = 0 ; i < rawDataByIntervals.size() ; i++ ){
                        minValue = TEMPERATURE_SENSOR_MAX_VALUE;
                        maxValue = TEMPERATURE_SENSOR_MIN_VALUE;
                        sum = 0;
                        if (rawDataByIntervals.get(i) != null) {
                            Collections.sort(rawDataByIntervals.get(i));
                            lowQuartile = rawDataByIntervals.get(i).get((int) Math.ceil(rawDataByIntervals.get(i).size() * 25/100));
                            highQuartile = rawDataByIntervals.get(i).get((int) Math.ceil(rawDataByIntervals.get(i).size() * 75/100));
                            for (Float teplota : rawDataByIntervals.get(i)) {
                                if (minValue > teplota) {
                                    minValue = teplota;
                                }
                                if (maxValue < teplota) {
                                    maxValue = teplota;
                                }
                                sum = sum + teplota;
                            }
                            mean = sum / rawDataByIntervals.get(i).size();

                            statisticData = new HashMap<>();
                            statisticData.put(DATA_SET_SIZE, (float) rawDataByIntervals.get(i).size());
                            statisticData.put(MIN_VALUE, minValue);
                            statisticData.put(MAX_VALUE, maxValue);
                            statisticData.put(LOW_QUARTILE, lowQuartile);
                            statisticData.put(HIGH_QUARTILE, highQuartile);
                            statisticData.put(MEAN_VALUE, mean);

                            statisticDataByIntervals.set(i, statisticData);
                        }
                    }
                    OnSelectTemperatureData.onSelectSuccess(statisticDataByIntervals, firstX, lastX);
                    Log.w(TAG, "jsem pryč z postexecute");
                }
            }

        }
    }

        public final static int YEAR = 0;
        public final static int MONTH = 1;
        public final static int DAY_OF_MONTH = 2;
        public final static int HOUR_OF_DAY = 3;
        public final static int MINUTE = 4;
        public final static int SECONDS = 5;

    public void close(){
        WritableDatabase.close();
        ReadableDatabase.close();
        TestbedDatabaseHelper.getInstance(Context).close();
    }
}
