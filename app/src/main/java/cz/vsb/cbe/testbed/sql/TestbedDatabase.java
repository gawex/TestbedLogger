/*
  @author  Bc. Lukas Tatarin
 * @supervisor Ing. Jaromir Konecny, Ph.D.
 * @email   lukas@tatarin.cz
 * @version 1.00
 * @ide     Android Studio 4.1.2
 * @license GNU GPL v3
 * @brief   TestbedDatabase
 * @lastmodify 2021/02/26 14:10:40
 * @verbatim
----------------------------------------------------------------------
Copyright (C) Bc. Lukas Tatarin, 2021

This program is free software: you can redistribute it and/or modify
it under the terms of the GNU General Public License as published by
the Free Software Foundation, either version 3 of the License, or
any later version.

This program is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
See the GNU General Public License for more details.

<http://www.gnu.org/licenses/>
 @endverbatim
 */

package cz.vsb.cbe.testbed.sql;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

import cz.vsb.cbe.testbed.chart.Result;

@SuppressWarnings({"unchecked", "rawtypes"})
public class TestbedDatabase {

    private final Executor mExecutor;

    public TestbedDatabase(Context context) {
        mContext = context;
        mExecutor = Executors.newSingleThreadExecutor();

    }

    private final Context mContext;

    public void insertRecord(TestbedDevice testbedDevice, String dataType, float value,
                             OnInsertListener onInsertListener) {
        mExecutor.execute(() -> {
            String tableName = TestbedDatabaseHelper.Data.TABLE_NAME;
            ContentValues newRecord = new ContentValues();
            newRecord.put(TestbedDatabaseHelper.Data.COLUMN_NAME_DEVICE_ID,
                    testbedDevice.getDeviceId());
            newRecord.put(TestbedDatabaseHelper.Data.COLUMN_NAME_DATA_KEY,
                    dataType);
            newRecord.put(TestbedDatabaseHelper.Data.COLUMN_NAME_DATA_VALUE, value);
            newRecord.put(TestbedDatabaseHelper.Data.COLUMN_NAME_TIMESTAMP,
                    System.currentTimeMillis());
            getWritableDatabase().insert(tableName, null, newRecord);
            onInsertListener.onInsertDone(new Result.Success<>(value));
        });
    }

    public void selectRecordsBetweenTimeStamp(TestbedDevice testbedDevice, String dataType,
                                              Date[] intervals, SORT_BY sort_by,
                                              SORT_ORDER sort_order,
                                              OnSelectListener onSelectListener) {
        mExecutor.execute(() -> {
            try {
                String tableName = TestbedDatabaseHelper.Data.TABLE_NAME;
                String[] columns = new String[]{
                        TestbedDatabaseHelper.Data.COLUMN_NAME_DATA_ID,
                        TestbedDatabaseHelper.Data.COLUMN_NAME_DEVICE_ID,
                        TestbedDatabaseHelper.Data.COLUMN_NAME_DATA_KEY,
                        TestbedDatabaseHelper.Data.COLUMN_NAME_DATA_VALUE,
                        TestbedDatabaseHelper.Data.COLUMN_NAME_TIMESTAMP};
                String selection = TestbedDatabaseHelper.Data.COLUMN_NAME_DEVICE_ID +
                        " = ? AND " +
                        TestbedDatabaseHelper.Data.COLUMN_NAME_DATA_KEY + " = ? AND " +
                        TestbedDatabaseHelper.Data.COLUMN_NAME_TIMESTAMP + " BETWEEN ? AND ?";
                String[] selectionArgs = new String[]{Integer.toString(
                        testbedDevice.getDeviceId()),
                        dataType, Long.toString(intervals[0].getTime()),
                        Long.toString(intervals[1].getTime())};
                String sortBy;
                switch (sort_by) {
                    case VALUE:
                        sortBy = TestbedDatabaseHelper.Data.COLUMN_NAME_DATA_VALUE;
                        break;

                    default:
                    case TIME:
                        sortBy = TestbedDatabaseHelper.Data.COLUMN_NAME_TIMESTAMP;
                        break;
                }
                String sortOrder;
                switch (sort_order) {
                    default:
                    case ASC:
                        sortOrder = " ASC";
                        break;

                    case DESC:
                        sortOrder = " DESC";
                        break;
                }

                onSelectListener.onSelectDone(new Result.Success<>(
                        getRecordsFromCursor(getReadableDatabase().query(
                                tableName, columns, selection, selectionArgs, null,
                                null, sortBy + sortOrder))));
            } catch (Exception e) {
                //noinspection unchecked
                onSelectListener.onSelectDone(new Result.Error<>(e));
            }
        });
    }

    public Record selectFirstRecord(TestbedDevice testbedDevice, String dataType)
            throws EmptyCursorException {
        String tableName = TestbedDatabaseHelper.Data.TABLE_NAME;
        String[] columns = new String[]{
                TestbedDatabaseHelper.Data.COLUMN_NAME_DATA_ID,
                TestbedDatabaseHelper.Data.COLUMN_NAME_DEVICE_ID,
                TestbedDatabaseHelper.Data.COLUMN_NAME_DATA_KEY,
                TestbedDatabaseHelper.Data.COLUMN_NAME_DATA_VALUE,
                TestbedDatabaseHelper.Data.COLUMN_NAME_TIMESTAMP};
        String selection = TestbedDatabaseHelper.Data.COLUMN_NAME_DEVICE_ID + " = ? AND " +
                TestbedDatabaseHelper.Data.COLUMN_NAME_DATA_KEY + " = ?";
        String[] selectionArgs = new String[]{Integer.toString(testbedDevice.getDeviceId()),
                dataType};
        return getRecordFromCursor(getReadableDatabase().query(
                tableName, columns, selection, selectionArgs, null, null,
                TestbedDatabaseHelper.Data.COLUMN_NAME_TIMESTAMP + " ASC LIMIT 1"));
    }

    public Record selectLastRecord(TestbedDevice testbedDevice, String dataType)
            throws EmptyCursorException {
        String tableName = TestbedDatabaseHelper.Data.TABLE_NAME;
        String[] columns = new String[]{
                TestbedDatabaseHelper.Data.COLUMN_NAME_DATA_ID,
                TestbedDatabaseHelper.Data.COLUMN_NAME_DEVICE_ID,
                TestbedDatabaseHelper.Data.COLUMN_NAME_DATA_KEY,
                TestbedDatabaseHelper.Data.COLUMN_NAME_DATA_VALUE,
                TestbedDatabaseHelper.Data.COLUMN_NAME_TIMESTAMP};
        String selection = TestbedDatabaseHelper.Data.COLUMN_NAME_DEVICE_ID + " = ? AND " +
                TestbedDatabaseHelper.Data.COLUMN_NAME_DATA_KEY + " = ?";
        String[] selectionArgs = new String[]{Integer.toString(testbedDevice.getDeviceId()),
                dataType};
        return getRecordFromCursor(getReadableDatabase().query(
                tableName, columns, selection, selectionArgs, null, null,
                TestbedDatabaseHelper.Data.COLUMN_NAME_TIMESTAMP + " DESC LIMIT 1"));
    }

    private SQLiteDatabase getReadableDatabase() {
        return TestbedDatabaseHelper.getInstance(mContext).getReadableDatabase();
    }

    private SQLiteDatabase getWritableDatabase() {
        return TestbedDatabaseHelper.getInstance(mContext).getWritableDatabase();
    }

    private Record getRecordFromCursor(Cursor cursor) throws EmptyCursorException {
        if (cursor.getCount() == 1) {
            cursor.moveToFirst();
            return new Record(
                    cursor.getInt(cursor.getColumnIndexOrThrow(
                            TestbedDatabaseHelper.Data.COLUMN_NAME_DATA_ID)),
                    cursor.getInt(cursor.getColumnIndexOrThrow(
                            TestbedDatabaseHelper.Data.COLUMN_NAME_DEVICE_ID)),
                    cursor.getString(cursor.getColumnIndexOrThrow(
                            TestbedDatabaseHelper.Data.COLUMN_NAME_DATA_KEY)),
                    cursor.getFloat(cursor.getColumnIndexOrThrow(
                            TestbedDatabaseHelper.Data.COLUMN_NAME_DATA_VALUE)),
                    cursor.getLong(cursor.getColumnIndexOrThrow(
                            TestbedDatabaseHelper.Data.COLUMN_NAME_TIMESTAMP)));
        } else {
            throw new EmptyCursorException("No roows in cursor!");
        }
    }

    private List<Record> getRecordsFromCursor(Cursor cursor) throws EmptyCursorException {
        if (cursor.getCount() > 0) {
            final List<Record> records = new ArrayList<>();
            while (cursor.moveToNext()) {
                records.add(new Record(
                        cursor.getInt(cursor.getColumnIndexOrThrow(
                                TestbedDatabaseHelper.Data.COLUMN_NAME_DATA_ID)),
                        cursor.getInt(cursor.getColumnIndexOrThrow(
                                TestbedDatabaseHelper.Data.COLUMN_NAME_DEVICE_ID)),
                        cursor.getString(cursor.getColumnIndexOrThrow(
                                TestbedDatabaseHelper.Data.COLUMN_NAME_DATA_KEY)),
                        cursor.getFloat(cursor.getColumnIndexOrThrow(
                                TestbedDatabaseHelper.Data.COLUMN_NAME_DATA_VALUE)),
                        cursor.getLong(cursor.getColumnIndexOrThrow(
                                TestbedDatabaseHelper.Data.COLUMN_NAME_TIMESTAMP))));
            }
            cursor.close();
            return records;
        } else {
            throw new EmptyCursorException("No roows in cursor!");
        }
    }

    private TestbedDevice getTestbedDeviceFromCursor(Cursor cursor) throws EmptyCursorException {
        if (cursor.getCount() == 1) {
            cursor.moveToFirst();
            return new TestbedDevice(
                    cursor.getInt(cursor.getColumnIndexOrThrow(
                            TestbedDatabaseHelper.Device.COLUMN_NAME_DEVICE_ID)),
                    cursor.getString(cursor.getColumnIndexOrThrow(
                            TestbedDatabaseHelper.Device.COLUMN_NAME_DEVICE_NAME)),
                    cursor.getInt(cursor.getColumnIndexOrThrow(
                            TestbedDatabaseHelper.Device.COLUMN_NAME_AVAILABLE_SENSORS)),
                    cursor.getString(cursor.getColumnIndexOrThrow(
                            TestbedDatabaseHelper.Device.COLUMN_NAME_BLUETOOTH_MAC_ADDRESS)),
                    cursor.getInt(cursor.getColumnIndexOrThrow(
                            TestbedDatabaseHelper.Device.COLUMN_NAME_IS_LAST_CONNECTED)),
                    cursor.getLong(cursor.getColumnIndexOrThrow(
                            TestbedDatabaseHelper.Device.COLUMN_NAME_TIMESTAMP)));
        } else {
            throw new EmptyCursorException("No roows in cursor!");
        }
    }

    private List<TestbedDevice> getTestbedDevicesFromCursor(Cursor cursor)
            throws EmptyCursorException {
        List<TestbedDevice> testbedDevices = new ArrayList<>();
        if (cursor.getCount() != 0) {
            while (cursor.moveToNext()) {
                testbedDevices.add(new TestbedDevice(
                        cursor.getInt(cursor.getColumnIndexOrThrow(
                                TestbedDatabaseHelper.Device.COLUMN_NAME_DEVICE_ID)),
                        cursor.getString(cursor.getColumnIndexOrThrow(
                                TestbedDatabaseHelper.Device.COLUMN_NAME_DEVICE_NAME)),
                        cursor.getInt(cursor.getColumnIndexOrThrow(
                                TestbedDatabaseHelper.Device.COLUMN_NAME_AVAILABLE_SENSORS)),
                        cursor.getString(cursor.getColumnIndexOrThrow(
                                TestbedDatabaseHelper.Device.COLUMN_NAME_BLUETOOTH_MAC_ADDRESS)),
                        cursor.getInt(cursor.getColumnIndexOrThrow(
                                TestbedDatabaseHelper.Device.COLUMN_NAME_IS_LAST_CONNECTED)),
                        cursor.getLong(cursor.getColumnIndexOrThrow(
                                TestbedDatabaseHelper.Device.COLUMN_NAME_TIMESTAMP))));
            }
            cursor.close();
            return testbedDevices;
        } else {
            throw new EmptyCursorException("No roows in cursor!");
        }
    }

    public void selectFirstRecordLessThanTimeStamp(TestbedDevice testbedDevice, String dataType,
                                                   Date limitDate,
                                                   OnSelectListener onSelectListener) {
        mExecutor.execute(() -> {
            try {
                String tableName = TestbedDatabaseHelper.Data.TABLE_NAME;
                String[] columns = new String[]{
                        TestbedDatabaseHelper.Data.COLUMN_NAME_DATA_ID,
                        TestbedDatabaseHelper.Data.COLUMN_NAME_DEVICE_ID,
                        TestbedDatabaseHelper.Data.COLUMN_NAME_DATA_KEY,
                        TestbedDatabaseHelper.Data.COLUMN_NAME_DATA_VALUE,
                        TestbedDatabaseHelper.Data.COLUMN_NAME_TIMESTAMP};
                String selection = TestbedDatabaseHelper.Data.COLUMN_NAME_DEVICE_ID +
                        " = ? AND " +
                        TestbedDatabaseHelper.Data.COLUMN_NAME_DATA_KEY + " = ? AND " +
                        TestbedDatabaseHelper.Data.COLUMN_NAME_TIMESTAMP + " < ?";
                String[] selectionArgs = new String[]{
                        Integer.toString(testbedDevice.getDeviceId()), dataType,
                        Long.toString(limitDate.getTime())};
                onSelectListener.onSelectDone(new Result.Success<>(getRecordFromCursor(
                        getReadableDatabase().query(tableName, columns, selection,
                                selectionArgs, null, null,
                                TestbedDatabaseHelper.Data.COLUMN_NAME_TIMESTAMP +
                                        " DESC LIMIT 1"))));
            } catch (EmptyCursorException e) {
                onSelectListener.onSelectDone(new Result.Error<>(e));
            }
        });
    }

    public List<Record> selectAllRecords(TestbedDevice testbedDevice, String dataType)
            throws EmptyCursorException {
        String tableName = TestbedDatabaseHelper.Data.TABLE_NAME;
        String[] columns = new String[]{
                TestbedDatabaseHelper.Data.COLUMN_NAME_DATA_ID,
                TestbedDatabaseHelper.Data.COLUMN_NAME_DEVICE_ID,
                TestbedDatabaseHelper.Data.COLUMN_NAME_DATA_KEY,
                TestbedDatabaseHelper.Data.COLUMN_NAME_DATA_VALUE,
                TestbedDatabaseHelper.Data.COLUMN_NAME_TIMESTAMP};
        String selection = TestbedDatabaseHelper.Data.COLUMN_NAME_DEVICE_ID + " = ? AND " +
                TestbedDatabaseHelper.Data.COLUMN_NAME_DATA_KEY + " = ?";
        String[] selectionArgs = new String[]{
                Integer.toString(testbedDevice.getDeviceId()),
                dataType};

        return getRecordsFromCursor(getReadableDatabase().query(
                tableName, columns, selection, selectionArgs, null, null,
                TestbedDatabaseHelper.Data.COLUMN_NAME_TIMESTAMP + " ASC"));
    }

    public enum SORT_BY {
        VALUE,
        TIME,
        DEFAULT
    }

    public enum SORT_ORDER {
        ASC,
        DESC,
        DEFAULT
    }

    public interface OnInsertListener<T> {
        void onInsertDone(Result<T> result);
    }

    public interface OnSelectListener<T> {
        void onSelectDone(Result<T> result);
    }

    public Record getFirstRecord(List<Record> records) {
        if (records != null && records.size() > 0) {
            return records.get(0);
        } else {
            return null;
        }
    }

    public Record getLastRecord(List<Record> records) {
        if (records != null && records.size() > 0) {
            return records.get(records.size() - 1);
        } else {
            return null;
        }
    }

    public List<TestbedDevice> selectAllTestbedDevices() throws EmptyCursorException {
        String tableName = TestbedDatabaseHelper.Device.TABLE_NAME;
        String[] columns = new String[]{TestbedDatabaseHelper.Device.COLUMN_NAME_DEVICE_ID,
                TestbedDatabaseHelper.Device.COLUMN_NAME_DEVICE_NAME,
                TestbedDatabaseHelper.Device.COLUMN_NAME_AVAILABLE_SENSORS,
                TestbedDatabaseHelper.Device.COLUMN_NAME_BLUETOOTH_MAC_ADDRESS,
                TestbedDatabaseHelper.Device.COLUMN_NAME_IS_LAST_CONNECTED,
                TestbedDatabaseHelper.Device.COLUMN_NAME_TIMESTAMP};

        return getTestbedDevicesFromCursor(getReadableDatabase().query(
                tableName, columns, null, null, null, null,
                TestbedDatabaseHelper.Device.COLUMN_NAME_IS_LAST_CONNECTED + " DESC"));
    }

    public TestbedDevice selectLastConnectedTestbedDevice() throws EmptyCursorException {
        String tableName = TestbedDatabaseHelper.Device.TABLE_NAME;
        String[] columns = new String[]{TestbedDatabaseHelper.Device.COLUMN_NAME_DEVICE_ID,
                TestbedDatabaseHelper.Device.COLUMN_NAME_DEVICE_NAME,
                TestbedDatabaseHelper.Device.COLUMN_NAME_AVAILABLE_SENSORS,
                TestbedDatabaseHelper.Device.COLUMN_NAME_BLUETOOTH_MAC_ADDRESS,
                TestbedDatabaseHelper.Device.COLUMN_NAME_IS_LAST_CONNECTED,
                TestbedDatabaseHelper.Device.COLUMN_NAME_TIMESTAMP};
        String selection = TestbedDatabaseHelper.Device.COLUMN_NAME_IS_LAST_CONNECTED + " = ?";
        String[] selectionArgs = new String[]{Integer.toString(1)};
        return getTestbedDeviceFromCursor(getReadableDatabase().query(
                tableName, columns, selection, selectionArgs, null, null,
                null));
    }

    public void updateLastConnectedTestbedDevice(final TestbedDevice newTestbedDevice) {
        String tableName = TestbedDatabaseHelper.Device.TABLE_NAME;
        final String whereClause = TestbedDatabaseHelper.Device.COLUMN_NAME_DEVICE_ID + " = ?";
        final ContentValues oldRecord;
        ContentValues newRecord;

        try {
            String[] oldWhereArgs = new String[]{
                    Integer.toString(selectLastConnectedTestbedDevice().getDeviceId())};
            oldRecord = new ContentValues();
            oldRecord.put(TestbedDatabaseHelper.Device.COLUMN_NAME_IS_LAST_CONNECTED, false);
            getWritableDatabase().update(tableName, oldRecord, whereClause, oldWhereArgs);
            newRecord = new ContentValues();
            if (getStoredStatusOfTestbedDevice(newTestbedDevice) ==
                    TestbedDevice.STORED_CONSISTENTLY) {
                newRecord.put(TestbedDatabaseHelper.Device.COLUMN_NAME_IS_LAST_CONNECTED, true);
                String[] newWhereArgs = new String[]{
                        Integer.toString(newTestbedDevice.getDeviceId())};
                getWritableDatabase().update(tableName, newRecord, whereClause, newWhereArgs);
            } else {
                throw new EmptyCursorException(null);
            }
        } catch (EmptyCursorException e) {
            newRecord = new ContentValues();
            newRecord.put(TestbedDatabaseHelper.Device.COLUMN_NAME_DEVICE_ID,
                    newTestbedDevice.getDeviceId());
            newRecord.put(TestbedDatabaseHelper.Device.COLUMN_NAME_DEVICE_NAME,
                    newTestbedDevice.getName());
            newRecord.put(TestbedDatabaseHelper.Device.COLUMN_NAME_AVAILABLE_SENSORS,
                    newTestbedDevice.getAvailableSensors());
            newRecord.put(TestbedDatabaseHelper.Device.COLUMN_NAME_BLUETOOTH_MAC_ADDRESS,
                    newTestbedDevice.getMacAddress());
            newRecord.put(TestbedDatabaseHelper.Device.COLUMN_NAME_IS_LAST_CONNECTED, true);
            newRecord.put(TestbedDatabaseHelper.Device.COLUMN_NAME_TIMESTAMP,
                    System.currentTimeMillis());
            getWritableDatabase().insert(tableName, null, newRecord);
        }
    }

    public int getStoredStatusOfTestbedDevice(TestbedDevice testbedDevice) {
        String tableName = TestbedDatabaseHelper.Device.TABLE_NAME;
        String[] columns = new String[]{
                TestbedDatabaseHelper.Device.COLUMN_NAME_DEVICE_ID,
                TestbedDatabaseHelper.Device.COLUMN_NAME_DEVICE_NAME,
                TestbedDatabaseHelper.Device.COLUMN_NAME_AVAILABLE_SENSORS,
                TestbedDatabaseHelper.Device.COLUMN_NAME_BLUETOOTH_MAC_ADDRESS,
                TestbedDatabaseHelper.Device.COLUMN_NAME_IS_LAST_CONNECTED,
                TestbedDatabaseHelper.Device.COLUMN_NAME_TIMESTAMP};
        String selection = TestbedDatabaseHelper.Device.COLUMN_NAME_DEVICE_ID + " = ?";
        String[] selectionArgs = new String[]{Integer.toString(testbedDevice.getDeviceId())};
        try {
            TestbedDevice storedTestbedDevice = getTestbedDeviceFromCursor(
                    getReadableDatabase().query(tableName, columns, selection, selectionArgs,
                            null, null, null));
            if (storedTestbedDevice.getAvailableSensors() != testbedDevice.getAvailableSensors() ||
                    !storedTestbedDevice.getMacAddress().equals(testbedDevice.getMacAddress()) ||
                    storedTestbedDevice.getTimeStamp().after(new Date())
            ) {
                return TestbedDevice.STORED_BUT_MODIFIED;
            } else {
                return TestbedDevice.STORED_CONSISTENTLY;
            }
        } catch (EmptyCursorException e) {
            return TestbedDevice.NOT_STORED;
        }
    }

    public void close() {
        getReadableDatabase().close();
        getWritableDatabase().close();
        TestbedDatabaseHelper.getInstance(mContext).close();
    }

    @SuppressWarnings("InnerClassMayBeStatic")
    public class EmptyCursorException extends Exception {

        public EmptyCursorException(String message) {
            super(message);
        }
    }
}