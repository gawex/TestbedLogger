/*
  @author  Bc. Lukas Tatarin
 * @supervisor Ing. Jaromir Konecny, Ph.D.
 * @email   lukas@tatarin.cz
 * @version v1.00
 * @ide     Android Studio 4.1.2
 * @license GNU GPL v3
 * @brief   TestbedDivice
 * @lastmodify 2021/02/15 11:10:38
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

import android.bluetooth.BluetoothDevice;

import java.io.Serializable;
import java.util.Date;

public class TestbedDevice implements Serializable {

    public static final String TESTBED_DEVICE =
            TestbedDevice.class.getPackage().toString() + ".TESTBED_DEVICE";

    public static final Integer NOT_STORED = 0;
    public static final Integer STORED_BUT_MODIFIED = 1;
    public static final Integer STORED_CONSISTENTLY = 2;

    private final String mDeName;
    private final String mDeMacAddress;
    private int mRssi;
    private boolean mIsDeviceDiscovered = false;
    private int mStoredState = NOT_STORED;
    private boolean mIsDeviceSelected = false;
    private int mDeId;
    private int mDeAvailableSenosrs;
    private boolean mDeIsLastConnected;
    private Date mDeTimeStamp;

    public TestbedDevice(BluetoothDevice bluetoothDevice, int rssi) {
        mDeName = bluetoothDevice.getName();
        mDeMacAddress = bluetoothDevice.getAddress();
        mRssi = rssi;
    }

    public TestbedDevice(int deId, String deName, int deAvailableSenosrs, String deMacAddress,
                         int deIsLastConnected, long deTimeStamp) {
        mDeId = deId;
        mDeName = deName;
        mDeAvailableSenosrs = deAvailableSenosrs;
        mDeMacAddress = deMacAddress;
        mDeIsLastConnected = deIsLastConnected != 0;
        mDeTimeStamp = new Date(deTimeStamp);
    }

    public boolean isTestbedDeviceDiscovered() {
        return mIsDeviceDiscovered;
    }

    public int getDeviceId() {
        return mDeId;
    }

    public void setDeviceId(int deId) {
        mDeId = deId;
    }

    public int getAvailableSensors() {
        return mDeAvailableSenosrs;
    }

    public void setAvailableSensors(int deAvailableSenosrs) {
        mDeAvailableSenosrs = deAvailableSenosrs;
    }

    public String getName() {
        return mDeName;
    }

    public String getMacAddress() {
        return mDeMacAddress;
    }

    public int getRssi() {
        return mRssi;
    }

    public Date getTimeStamp() {
        return mDeTimeStamp;
    }

    public void deviceWasDiscovered() {
        mIsDeviceDiscovered = true;
    }

    public int getStoredState() {
        return mStoredState;
    }

    public void setStoredState(int storedState) {
        mStoredState = storedState;
    }

    public boolean isLastConnected() {
        return mDeIsLastConnected;
    }

    public void selectDevice(boolean select) {
        mIsDeviceSelected = select;
    }

    public boolean isDeviceSelected() {
        return mIsDeviceSelected;
    }
}