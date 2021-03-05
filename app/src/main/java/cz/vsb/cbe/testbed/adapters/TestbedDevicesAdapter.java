/*
  @author  Bc. Lukas Tatarin
 * @supervisor Ing. Jaromir Konecny, Ph.D.
 * @email   lukas@tatarin.cz
 * @version 1.10
 * @ide     Android Studio 4.1.2
 * @license GNU GPL v3
 * @brief   TestbedDeviceAdapter.java
 * @lastmodify 2021/03/05 11:47:15
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

package cz.vsb.cbe.testbed.adapters;

import android.annotation.SuppressLint;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.appcompat.content.res.AppCompatResources;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;

import cz.vsb.cbe.testbed.R;
import cz.vsb.cbe.testbed.sql.TestbedDevice;

public class TestbedDevicesAdapter extends BaseAdapter {

    private static final int DEFAULT_POSITION_VALUE = -1;

    private static final int EXCELLENT_SIGNAL_LIMIT = -70;
    private static final int GOOD_SIGNAL_LIMIT = -85;
    private static final int POOR_SIGNAL_LIMIT = -100;

    private final LayoutInflater mLayoutInflater;
    private final List<TestbedDevice> mTestbedDevices;
    private final Context mContext;

    public TestbedDevicesAdapter(LayoutInflater layoutInflater, Context context) {
        mLayoutInflater = layoutInflater;
        mTestbedDevices = new ArrayList<>();
        mContext = context;
    }

    public void setTestbedDevice(TestbedDevice testbedDevice) {
        boolean existAtLeastOneTestBedDevice = false;
        int position = DEFAULT_POSITION_VALUE;
        for (int index = 0; index < mTestbedDevices.size(); index++) {
            if (mTestbedDevices.get(index).getMacAddress().equals(testbedDevice.getMacAddress())) {
                existAtLeastOneTestBedDevice = true;
                position = index;
                break;
            }
        }

        if (existAtLeastOneTestBedDevice) {
            mTestbedDevices.set(position, testbedDevice);
        } else {
            mTestbedDevices.add(testbedDevice);
        }
    }

    public void removeTestbedDevices(List<TestbedDevice> testbedDevices) {
        mTestbedDevices.removeAll(testbedDevices);
    }

    public TestbedDevice getTestbedDevice(int position) {
        return mTestbedDevices.get(position);
    }

    public List<TestbedDevice> getTestbedDevices() {
        return mTestbedDevices;
    }

    public void clear() {
        mTestbedDevices.clear();
    }

    @Override
    public int getCount() {
        return mTestbedDevices.size();
    }

    @Override
    public Object getItem(int i) {
        return mTestbedDevices.get(i);
    }

    @Override
    public long getItemId(int i) {
        return i;
    }

    @SuppressLint({"InflateParams", "SetTextI18n"})
    @Override
    public View getView (int position, View view, ViewGroup parent){
        ViewHolder viewHolder ;
        if (view == null) {
            view = mLayoutInflater.inflate(R.layout.list_item_device, null);
            viewHolder = new ViewHolder();
            viewHolder.mTxvTestbedDeviceName =
                    view.findViewById(R.id.list_item_device_txv_device_name);
            viewHolder.mImvDatabaseStatus =
                    view.findViewById(R.id.list_item_device_imv_stored_status);
            viewHolder.mTxvTestbedDeviceId = view.findViewById(R.id.list_item_device_txt_device_id);
            viewHolder.mImvSignalStrenght =
                    view.findViewById(R.id.list_item_device_imv_signal_strength);
            viewHolder.mTxvRssi = view.findViewById(R.id.list_item_device_txt_rssi);
            viewHolder.mImvPedometerStatus = view.findViewById(R.id.list_item_device_imv_pedometer);
            viewHolder.mImvHeartRateMeterStatus =
                    view.findViewById(R.id.list_item_device_imv_heart_rate_meter);
            viewHolder.mImvThermometerStatus =
                    view.findViewById(R.id.list_item_device_imv_thermometer);
            viewHolder.mImvMacAddress = view.findViewById(R.id.list_item_device_txv_mac_address);
            view.setTag(viewHolder);
        } else {
            viewHolder = (ViewHolder) view.getTag();
        }
        TestbedDevice testbedDevice = mTestbedDevices.get(position);
        viewHolder.mTxvTestbedDeviceName.setText(testbedDevice.getName());
        viewHolder.mImvMacAddress.setText(testbedDevice.getMacAddress());

        if (testbedDevice.getRssi() > EXCELLENT_SIGNAL_LIMIT) {
            viewHolder.mImvSignalStrenght.setImageDrawable(
                    AppCompatResources.getDrawable(
                            mContext, R.drawable.ic_signal_excelent_20dp_color_vsb));
            viewHolder.mTxvRssi.setText(testbedDevice.getRssi() + " dBm");
        } else if (testbedDevice.getRssi() <= EXCELLENT_SIGNAL_LIMIT &&
                testbedDevice.getRssi() >= GOOD_SIGNAL_LIMIT) {
            viewHolder.mImvSignalStrenght.setImageDrawable(
                    AppCompatResources.getDrawable(
                            mContext, R.drawable.ic_signal_good_20dp_color_vsb));
            viewHolder.mTxvRssi.setText(testbedDevice.getRssi() + " dBm");
        } else if (testbedDevice.getRssi() < GOOD_SIGNAL_LIMIT &&
                testbedDevice.getRssi() >= POOR_SIGNAL_LIMIT) {
            viewHolder.mImvSignalStrenght.setImageDrawable(
                    AppCompatResources.getDrawable(
                            mContext, R.drawable.ic_signal_poor_20dp_color_vsb));
            viewHolder.mTxvRssi.setText(testbedDevice.getRssi() + " dBm");
        } else {
            viewHolder.mImvSignalStrenght.setImageDrawable(
                    AppCompatResources.getDrawable(
                            mContext, R.drawable.ic_signal_bad_20dp_color_vsb));
            viewHolder.mTxvRssi.setText(testbedDevice.getRssi() + " dBm");
        }

        if (!testbedDevice.isTestbedDeviceDiscovered()) {
            viewHolder.mImvDatabaseStatus.setImageDrawable(
                    AppCompatResources.getDrawable(mContext, android.R.color.transparent));
            viewHolder.mTxvTestbedDeviceId.setText(null);
            viewHolder.mImvPedometerStatus.setImageDrawable(
                    AppCompatResources.getDrawable(mContext, android.R.color.transparent));
            viewHolder.mImvHeartRateMeterStatus.setImageDrawable(
                    AppCompatResources.getDrawable(mContext, android.R.color.transparent));
            viewHolder.mImvThermometerStatus.setImageDrawable(
                    AppCompatResources.getDrawable(mContext, android.R.color.transparent));
        } else {
            if (testbedDevice.getStoredState() == TestbedDevice.STORED_CONSISTENTLY) {
                viewHolder.mImvDatabaseStatus.setImageDrawable(
                        AppCompatResources.getDrawable(mContext,
                                R.drawable.ic_database_20dp_color_fei));
            } else if (testbedDevice.getStoredState() == TestbedDevice.NOT_STORED) {
                viewHolder.mImvDatabaseStatus.setImageDrawable(
                        AppCompatResources.getDrawable(mContext,
                                R.drawable.ic_database_20dp_color_gray));
            } else {
                viewHolder.mImvDatabaseStatus.setImageDrawable(
                        AppCompatResources.getDrawable(mContext,
                                R.drawable.ic_database_20dp_color_red));
            }

            String id = String.format("%04X", testbedDevice.getDeviceId());
            id = id.replace("B", "b");
            id = id.replace("D", "d");
            viewHolder.mTxvTestbedDeviceId.setText(id);

            if (BigInteger.valueOf(testbedDevice.getAvailableSensors()).testBit(2)) {
                viewHolder.mImvPedometerStatus.setImageDrawable(
                        AppCompatResources.getDrawable(mContext,
                                R.drawable.ic_steps_20dp_color_fei));
            } else {
                viewHolder.mImvPedometerStatus.setImageDrawable(
                        AppCompatResources.getDrawable(mContext,
                                R.drawable.ic_steps_20dp_color_gray));
            }

            if (BigInteger.valueOf(testbedDevice.getAvailableSensors()).testBit(1)) {
                viewHolder.mImvHeartRateMeterStatus.setImageDrawable(
                        AppCompatResources.getDrawable(mContext,
                                R.drawable.ic_heart_20dp_color_fei));
            } else {
                viewHolder.mImvHeartRateMeterStatus.setImageDrawable(
                        AppCompatResources.getDrawable(mContext,
                                R.drawable.ic_heart_20dp_color_gray));
            }

            if (BigInteger.valueOf(testbedDevice.getAvailableSensors()).testBit(0)) {
                viewHolder.mImvThermometerStatus.setImageDrawable(
                        AppCompatResources.getDrawable(mContext,
                                R.drawable.ic_thermometer_20dp_color_fei));
            } else {
                viewHolder.mImvThermometerStatus.setImageDrawable(
                        AppCompatResources.getDrawable(mContext,
                                R.drawable.ic_thermometer_20dp_color_gray));
            }
        }
        return view;
    }

    static class ViewHolder {
        TextView mTxvTestbedDeviceName;
        ImageView mImvDatabaseStatus;
        TextView mTxvTestbedDeviceId;
        ImageView mImvSignalStrenght;
        TextView mTxvRssi;
        ImageView mImvPedometerStatus;
        ImageView mImvHeartRateMeterStatus;
        ImageView mImvThermometerStatus;
        TextView mImvMacAddress;
    }
}