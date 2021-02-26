/*
  @author  Bc. Lukas Tatarin
 * @supervisor Ing. Jaromir Konecny, Ph.D.
 * @email   lukas@tatarin.cz
 * @version 1.00
 * @ide     Android Studio 4.1.2
 * @license GNU GPL v3
 * @brief   SelectTestbedDeviceAdapter.java
 * @lastmodify 2021/02/26 13:41:35
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
import android.widget.CheckBox;

import java.util.ArrayList;
import java.util.List;

import cz.vsb.cbe.testbed.sql.TestbedDevice;

public class SelectTestbedDeviceAdapter extends BaseAdapter {

    private final LayoutInflater mLayoutInflater;
    private final Context mContext;
    private final List<TestbedDevice> mTestbedDevices;

    public SelectTestbedDeviceAdapter(LayoutInflater inflater, Context context) {
        mLayoutInflater = inflater;
        mContext = context;
        mTestbedDevices = new ArrayList<>();
    }

    public void addTestbedDevices(List<TestbedDevice> testbedDevices) {
        mTestbedDevices.addAll(testbedDevices);
    }

    public List<TestbedDevice> getSelectedTestbedDevices() {
        List<TestbedDevice> selectedTestbedDevices = new ArrayList<>();
        for (TestbedDevice testbedDevice : mTestbedDevices) {
            if (testbedDevice.isDeviceSelected()) {
                selectedTestbedDevices.add(testbedDevice);
            }
        }
        return selectedTestbedDevices;
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
    public View getView(final int position, View view, ViewGroup parent) {
        CheckBox chbCheckBox;
        if (view == null) {
            view = mLayoutInflater.inflate(R.layout.list_item_checkbox, null);
            chbCheckBox = view.findViewById(R.id.list_item_checkbox_chb);
            view.setTag(chbCheckBox);
        } else {
            chbCheckBox = (CheckBox) view.getTag();
        }

        final TestbedDevice testbedDevice = mTestbedDevices.get(position);
        chbCheckBox.setText(testbedDevice.getName() + " (#" +
                String.format("%04X", testbedDevice.getDeviceId()) + ")" +
                (testbedDevice.isLastConnected() ? " - " +
                        mContext.getString(
                                R.string.select_testbed_device_adapter_this_device_label) :
                        ""));

        if (testbedDevice.isLastConnected()) {
            chbCheckBox.setChecked(true);
            testbedDevice.selectDevice(true);
            mTestbedDevices.set(position, testbedDevice);
        }

        chbCheckBox.setOnClickListener(arg0 -> {
            testbedDevice.selectDevice(((CheckBox) arg0).isChecked());
            mTestbedDevices.set(position, testbedDevice);
        });
        return view;
    }
}