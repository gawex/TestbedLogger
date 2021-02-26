/*
  @author  Bc. Lukas Tatarin
 * @supervisor Ing. Jaromir Konecny, Ph.D.
 * @email   lukas@tatarin.cz
 * @version 1.00
 * @ide     Android Studio 4.1.2
 * @license GNU GPL v3
 * @brief   RecordDetailsAdapter.java
 * @lastmodify 2021/02/26 13:10:35
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
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.appcompat.content.res.AppCompatResources;

import java.util.ArrayList;
import java.util.List;

import cz.vsb.cbe.testbed.chart.axisValueFormater.RecordValueFormater;
import cz.vsb.cbe.testbed.sql.Record;

public class RecordDetailsAdapter extends BaseAdapter {

    private final Context mContext;
    private final LayoutInflater mLayoutInflater;
    private final List<DetailsItem> mDetailsItems;

    public RecordDetailsAdapter(LayoutInflater layoutInflater, Context context) {
        mLayoutInflater = layoutInflater;
        mContext = context;
        mDetailsItems = new ArrayList<>();
    }

    public void setRecord(Record record) {
        mDetailsItems.clear();
        mDetailsItems.add(new DetailsItem(LABELS.RECORD_ID, record));
        mDetailsItems.add(new DetailsItem(LABELS.DEVICE_ID, record));
        mDetailsItems.add(new DetailsItem(LABELS.RECORD_KEY, record));
        mDetailsItems.add(new DetailsItem(LABELS.RECORD_VALUE, record));
        mDetailsItems.add(new DetailsItem(LABELS.RECORD_TIMESTAMP, record));
    }

    @Override
    public int getCount() {
        return mDetailsItems.size();
    }

    @Override
    public Object getItem(int position) {
        return null;
    }

    @Override
    public long getItemId(int position) {
        return 0;
    }

    @SuppressLint("InflateParams")
    @Override
    public View getView(int position, View view, ViewGroup parent) {
        ViewHolder viewHolder;
        if (view == null) {
            view = mLayoutInflater.inflate(R.layout.list_item_record_details, null);
            viewHolder = new ViewHolder();
            viewHolder.mImvIcon = view.findViewById(R.id.list_item_record_details_imv_icon);
            viewHolder.mTxvKey = view.findViewById(R.id.list_item_record_details_txv_label);
            viewHolder.mTxvValue = view.findViewById(R.id.list_item_record_details_lnl_txv_value);
            view.setTag(viewHolder);
        } else {
            viewHolder = (ViewHolder) view.getTag();
        }

        viewHolder.mImvIcon.setImageDrawable(
                AppCompatResources.getDrawable(
                        mContext, mDetailsItems.get(position).mIconDrawableId));
        viewHolder.mTxvKey.setText(mContext.getString(mDetailsItems.get(position).mLabelId));
        viewHolder.mTxvValue.setText(mDetailsItems.get(position).mValue);

        return view;
    }

    public enum LABELS {
        RECORD_ID,
        DEVICE_ID,
        RECORD_KEY,
        RECORD_VALUE,
        RECORD_TIMESTAMP
    }

    private static class ViewHolder {
        ImageView mImvIcon;
        TextView mTxvKey;
        TextView mTxvValue;
    }

    @SuppressWarnings("InnerClassMayBeStatic")
    private class DetailsItem {

        private final int mIconDrawableId;
        private final int mLabelId;
        private final String mValue;

        public DetailsItem(LABELS label, Record record) {
            switch (label) {
                case RECORD_ID:
                    mIconDrawableId = R.drawable.ic_record_id_20dp_color_fei;
                    mLabelId = R.string.record_details_adapter_record_id_label;
                    mValue = String.valueOf(record.getDataId());
                    break;

                case DEVICE_ID:
                    mIconDrawableId = R.drawable.ic_device_id_20dp_color_fei;
                    mLabelId = R.string.record_details_adapter_device_id_label;
                    mValue = "#" + String.format("%04X", record.getDeviceId());
                    break;

                case RECORD_KEY:
                    mIconDrawableId = R.drawable.ic_key_20dp_color_fei;
                    mLabelId = R.string.record_details_adapter_record_key_label;
                    mValue = record.getDataKey();
                    break;

                case RECORD_VALUE:
                    switch (record.getDataKey()) {
                        case BluetoothLeService.STEPS_DATA:
                            mIconDrawableId = R.drawable.ic_steps_20dp_color_fei;
                            mValue = RecordValueFormater.formatSteps(
                                    "###,###", record.getValue());
                            break;

                        case BluetoothLeService.HEART_RATE_DATA:
                            mIconDrawableId = R.drawable.ic_heart_20dp_color_fei;
                            mValue = RecordValueFormater.formatHearRate(
                                    "###", record.getValue());
                            break;

                        case BluetoothLeService.TEMPERATURE_DATA:
                            mIconDrawableId = R.drawable.ic_thermometer_20dp_color_fei;
                            mValue = RecordValueFormater.formatTemperature(
                                    "##0.00", record.getValue());
                            break;

                        default:
                            throw new IllegalStateException(
                                    "Unexpected value of record.getDataKey(): " +
                                            record.getDataKey());
                    }
                    mLabelId = R.string.record_details_adapter_record_value_label;
                    break;

                case RECORD_TIMESTAMP:
                    mIconDrawableId = R.drawable.ic_timestamp_20dp_color_fei;
                    mLabelId = R.string.record_details_adapter_record_timestamp_label;
                    mValue = RecordValueFormater.formatTimeStamp(record.getTimeStamp());
                    break;

                default:
                    throw new IllegalStateException("Unexpected value of label: " + label);
            }
        }
    }
}