/*
  @author  Bc. Lukas Tatarin
 * @supervisor Ing. Jaromir Konecny, Ph.D.
 * @email   lukas@tatarin.cz
 * @version 1.00
 * @ide     Android Studio 4.1.2
 * @license GNU GPL v3
 * @brief   Record
 * @lastmodify 2021/02/15 12:20:04
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

import org.jetbrains.annotations.NotNull;

import java.util.Date;

public class Record implements Comparable<Record> {

    private int mDaId;
    private int mDeId;
    private String mDaKey;
    private Float mDaValue;
    private Date mDaTimeStamp;

    public Record() {
        mDaValue = (float) 0;
    }

    public Record(int daId, int deId, String daKey, float daValue, long daTimeStamp) {
        mDaId = daId;
        mDeId = deId;
        mDaKey = daKey;
        mDaValue = daValue;
        mDaTimeStamp = new Date(daTimeStamp);
    }

    @NotNull
    @Override
    public Record clone() {
        try {
            return (Record) super.clone();
        } catch (CloneNotSupportedException e) {
            return new Record(mDaId, mDeId, mDaKey, mDaValue, mDaTimeStamp.getTime());
        }
    }

    public int getDataId() {
        return mDaId;
    }

    public int getDeviceId() {
        return mDeId;
    }

    public String getDataKey() {
        return mDaKey;
    }

    public float getValue() {
        return mDaValue;
    }

    public void setValue(float value) {
        mDaValue = value;
    }

    public Date getTimeStamp() {
        return mDaTimeStamp;
    }

    @Override
    public int compareTo(Record o) {
        return mDaValue.compareTo(o.mDaValue);
    }
}
