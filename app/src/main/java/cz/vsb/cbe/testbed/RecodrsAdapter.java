/*
  @author  Bc. Lukas Tatarin
 * @supervisor Ing. Jaromir Konecny, Ph.D.
 * @email   lukas@tatarin.cz
 * @version 1.00
 * @ide     Android Studio 4.1.2
 * @license GNU GPL v3
 * @brief   RecordsAdapter.java
 * @lastmodify 2021/02/26 13:14:25
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

package cz.vsb.cbe.testbed;

import android.annotation.SuppressLint;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;

import cz.vsb.cbe.testbed.chart.axisValueFormater.RecordValueFormater;
import cz.vsb.cbe.testbed.sql.Record;

public class RecodrsAdapter extends BaseAdapter {

    private final LayoutInflater mLayoutInflater;
    private final List<Record> mRecords;
    private int mSensorType;

    public RecodrsAdapter(LayoutInflater inflater) {
        mLayoutInflater = inflater;
        mRecords = new ArrayList<>();
    }

    public void clearAndAddRecords(List<Record> records, int sensorType) {
        mRecords.clear();
        mRecords.addAll(records);
        mSensorType = sensorType;
    }

    public Record getRecord(int position) {
        return mRecords.get(position);
    }

    @Override
    public int getCount() {
        return mRecords.size();
    }

    @Override
    public Object getItem(int i) {
        return mRecords.get(i);
    }

    @Override
    public long getItemId(int i) {
        return i;
    }

    @SuppressLint({"InflateParams", "NonConstantResourceId"})
    @Override
    public View getView(int position, View view, ViewGroup parent) {
        ViewHolder viewHolder;
        if (view == null) {
            view = mLayoutInflater.inflate(R.layout.list_item_record, null);
            viewHolder = new ViewHolder();
            viewHolder.mTxvValue = view.findViewById(R.id.list_item_record_txv_value);
            viewHolder.mTxvTimeStamp = view.findViewById(R.id.list_item_record_txv_time_stamp);
            view.setTag(viewHolder);
        } else {
            viewHolder = (ViewHolder) view.getTag();
        }
        Record record = mRecords.get(position);
        switch (mSensorType) {
            case R.id.pedometer:
                viewHolder.mTxvValue.setText(
                        RecordValueFormater.formatSteps(
                                RecordValueFormater.PATERN_6_0, record.getValue()));
                break;

            case R.id.heart_rate:
                viewHolder.mTxvValue.setText(
                        RecordValueFormater.formatHearRate(
                                RecordValueFormater.PATERN_3_0, record.getValue()));
                break;

            case R.id.temperature:
                viewHolder.mTxvValue.setText(
                        RecordValueFormater.formatTemperature(
                                RecordValueFormater.PATERN_3_2, record.getValue()));
                break;
        }
        viewHolder.mTxvTimeStamp.setText(
                RecordValueFormater.formatTimeStampFull(record.getTimeStamp()));
        return view;
    }

    static class ViewHolder {
        TextView mTxvValue;
        TextView mTxvTimeStamp;
    }
}