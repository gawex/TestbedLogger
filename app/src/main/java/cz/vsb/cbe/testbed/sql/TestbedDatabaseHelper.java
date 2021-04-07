/*
  @author  Bc. Lukas Tatarin
 * @supervisor Ing. Jaromir Konecny, Ph.D.
 * @email   lukas@tatarin.cz
 * @version 1.10
 * @ide     Android Studio 4.1.2
 * @license GNU GPL v3
 * @brief   TestbedDatabaseHelper.java
 * @lastmodify 2021/03/05 12:03:43
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

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.provider.BaseColumns;

public class TestbedDatabaseHelper extends SQLiteOpenHelper {

    protected static TestbedDatabaseHelper mTestbedDatabaseHelper;

    // If you change the database schema, you must increment the database version.
    public static final int DATABASE_VERSION = 1;
    public static final String DATABASE_NAME = "Testbed.db";

    private static final String SQL_CREATE_DEVICES_TABLE =
            "CREATE TABLE " + Device.TABLE_NAME + " (" +
                    Device.COLUMN_NAME_DEVICE_ID + " INTEGER PRIMARY KEY," +
                    Device.COLUMN_NAME_DEVICE_NAME + " TEXT," +
                    Device.COLUMN_NAME_AVAILABLE_SENSORS + " INTEGER," +
                    Device.COLUMN_NAME_BLUETOOTH_MAC_ADDRESS + " TEXT," +
                    Device.COLUMN_NAME_IS_LAST_CONNECTED + " INTEGER," +
                    Device.COLUMN_NAME_TIMESTAMP + " INTEGER)";

    private static final String SQL_CREATE_DATA_TABLE =
            "CREATE TABLE " + Data.TABLE_NAME + " (" +
                    Data.COLUMN_NAME_DATA_ID + " INTEGER PRIMARY KEY," +
                    Data.COLUMN_NAME_DEVICE_ID + " INTEGER, " +
                    Data.COLUMN_NAME_DATA_KEY + " TEXT," +
                    Data.COLUMN_NAME_DATA_VALUE + " REAL," +
                    Data.COLUMN_NAME_TIMESTAMP + " INTEGER)";

    private static final String SQL_DELETE_DEVICES_TABLE =
            "DROP TABLE IF EXISTS " + Device.TABLE_NAME;

    private static final String SQL_DELETE_DATA_TABLE =
            "DROP TABLE IF EXISTS " + Data.TABLE_NAME;

    public static TestbedDatabaseHelper getInstance(Context context) {
        if (mTestbedDatabaseHelper == null) {
            mTestbedDatabaseHelper = new TestbedDatabaseHelper(context);
        }
        return mTestbedDatabaseHelper;
    }

    private TestbedDatabaseHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    public void onCreate(SQLiteDatabase db) {
        db.execSQL(SQL_CREATE_DATA_TABLE);
        db.execSQL(SQL_CREATE_DEVICES_TABLE);
    }

    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        // This database is only a cache for online data, so its upgrade policy is
        // to simply to discard the data and start over
        db.execSQL(SQL_DELETE_DEVICES_TABLE);
        db.execSQL(SQL_DELETE_DATA_TABLE);
        onCreate(db);
    }

    public void onDowngrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        onUpgrade(db, oldVersion, newVersion);
    }

    public static class Device implements BaseColumns {
        public static final String TABLE_NAME = "devices";
        public static final String COLUMN_NAME_DEVICE_ID = "de_id";
        public static final String COLUMN_NAME_DEVICE_NAME = "de_name";
        public static final String COLUMN_NAME_AVAILABLE_SENSORS = "de_available_sensors";
        public static final String COLUMN_NAME_BLUETOOTH_MAC_ADDRESS = "de_bluetooth_mac_address";
        public static final String COLUMN_NAME_IS_LAST_CONNECTED = "de_is_last_connected";
        public static final String COLUMN_NAME_TIMESTAMP = "de_timestamp";
    }

    public static class Data implements BaseColumns{
        public static final String TABLE_NAME = "data";
        public static final String COLUMN_NAME_DATA_ID = "da_id";
        public static final String COLUMN_NAME_DEVICE_ID = "de_id";
        public static final String COLUMN_NAME_DATA_KEY = "da_key";
        public static final String COLUMN_NAME_DATA_VALUE = "da_value";
        public static final String COLUMN_NAME_TIMESTAMP = "da_timestamp";
    }

    @Override
    public synchronized void close() {
        mTestbedDatabaseHelper = null;
        super.close();
    }
}