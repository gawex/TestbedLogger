package cz.vsb.cbe.testbed.sql;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.AsyncTask;
import android.util.Log;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import cz.vsb.cbe.testbed.BluetoothLeService;
import cz.vsb.cbe.testbed.TestbedDevice;

public class TestbedDatabase {

    private static TestbedDatabase TestbedDatabase;
    private static Context Context;
    private static SQLiteDatabase WritableDatabase;
    private static SQLiteDatabase ReadableDatabase;

    OnInsertIntoDatabase OnInsertIntoDatabase;
    OnUpdateIntoDatabase OnUpdateIntoDatabase;
    OnIsTestbedDeviceStored OnIsTestbedDeviceStored;
    OnGetLastConnectedTestbedDevice OnGetLastConnectedTestbedDevice;
    OnSelectTemperatureData OnSelectTemperatureData;

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

    public interface OnSelectTemperatureData {
        void onSelectSuccess(List<Record> records);
        void onSelectFailed();
    }

    public static TestbedDatabase getInstance(Context context) {
        if (TestbedDatabase == null) {
            TestbedDatabase = new TestbedDatabase(context);
        }
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

    public void selectTemperatureData(TestbedDevice testbedDevice, Date startDate, Date endDate, OnSelectTemperatureData onSelectTemperatureData) {
        this.OnSelectTemperatureData = onSelectTemperatureData;
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
        SelectTemperature selectTemperature = new SelectTemperature();
        selectTemperature.execute(selectQuery);
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

    public class SelectTemperature extends SelectFromDatabase {

        @Override
        protected void onPostExecute(List<Cursor> cursors) {
            super.onPostExecute(cursors);
            for (Cursor cursor : cursors) {
                if (cursor.getCount() == 0) {
                    OnSelectTemperatureData.onSelectFailed();
                } else {
                    List<Record> records = new ArrayList<>();
                    while (cursor.moveToNext()) {
                        records.add(new Record(cursor.getInt(cursor.getColumnIndexOrThrow(TestbedDatabaseHelper.Data.COLUMN_NAME_DATA_ID)),
                                               cursor.getFloat(cursor.getColumnIndexOrThrow(TestbedDatabaseHelper.Data.COLUMN_NAME_DATA_VALUE)),
                                               cursor.getLong(cursor.getColumnIndexOrThrow(TestbedDatabaseHelper.Data.COLUMN_NAME_TIMESTAMP))));
                    }
                    cursor.close();
                    OnSelectTemperatureData.onSelectSuccess(records);
                }
            }
        }
    }

    private void close(){
        WritableDatabase.close();
        ReadableDatabase.close();
        TestbedDatabaseHelper.getInstance(Context).close();
    }
}
