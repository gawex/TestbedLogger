package cz.vsb.cbe.tesdbed;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.AsyncTask;

import org.jetbrains.annotations.Contract;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.logging.SimpleFormatter;

public class SelectFromDatabase extends AsyncTask<SelectFromDatabase.SelectQuery, Void, List<SelectFromDatabase.Record>>  {


    public static class SelectQuery {
        String [] Where;

        public  SelectQuery(int id, String key, Date startDate, Date endDate ) {
            this.Where = new String [] {
                    Integer.toString(id),
                    key,
                    Long.toString(startDate.getTime()),
                    Long.toString(endDate.getTime())
            };
        };
    }

    protected class Record{

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

    public interface OnRecordsSelected {
        void onRecordsSelectedSuccessAsList(List<Record> records);
    }

    private OnRecordsSelected onRecordsSelected;
    private SQLiteDatabase sqLiteDatabase;

    public SelectFromDatabase(Context context, OnRecordsSelected onRecordsSelected){
        this.onRecordsSelected = onRecordsSelected;
        sqLiteDatabase = TestbedDbHelper.getInstance(context).getReadableDatabase();
    }

    @Override
    protected List<Record> doInBackground(SelectQuery... selectQueries) {
        List<Record> records = new ArrayList<>();
        for (int i = 0; i < selectQueries.length; i++) {
            Cursor readCursor = sqLiteDatabase.query(
                    // The table to query
                    TestbedDbHelper.Data.TABLE_NAME,
                    // The array of columns to return (pass null to get all)
                    new String[]{
                            TestbedDbHelper.Data.COLUMN_NAME_DATA_ID,
                            TestbedDbHelper.Data.COLUMN_NAME_DATA_VALUE,
                            TestbedDbHelper.Data.COLUMN_NAME_TIMESTAMP},
                    // The columns for the WHERE clause
                    TestbedDbHelper.Data.COLUMN_NAME_DEVICE_ID + " = ? AND " +
                            TestbedDbHelper.Data.COLUMN_NAME_DATA_KEY + " = ? AND " +
                            TestbedDbHelper.Data.COLUMN_NAME_TIMESTAMP + " BETWEEN ? AND ?",
                    // The values for the WHERE clause
                    selectQueries[i].Where,
                    //new String[]{Integer.toString(26407), BluetoothLeService.TEMPERATURE},
                    // don't group the rows
                    null,
                    // don't filter by row groups
                    null,
                    // The sort order
                    TestbedDbHelper.Data.COLUMN_NAME_TIMESTAMP + " DESC"
            );

            while (readCursor.moveToNext()){
                records.add(new Record(readCursor.getInt(readCursor.getColumnIndexOrThrow(TestbedDbHelper.Data.COLUMN_NAME_DATA_ID)),
                                       readCursor.getFloat(readCursor.getColumnIndexOrThrow(TestbedDbHelper.Data.COLUMN_NAME_DATA_VALUE)),
                                       readCursor.getLong(readCursor.getColumnIndexOrThrow(TestbedDbHelper.Data.COLUMN_NAME_TIMESTAMP))));
            }
            readCursor.close();
        }
        return records;
    }

    @Override
    protected void onPostExecute(List<Record> records){
        if(onRecordsSelected != null)
            onRecordsSelected.onRecordsSelectedSuccessAsList(records);

    }

    public void removeCallbacks (){
        onRecordsSelected = null;
    }
}
