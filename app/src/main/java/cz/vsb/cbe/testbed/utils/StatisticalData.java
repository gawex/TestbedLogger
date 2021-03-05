/*
  @author  Bc. Lukas Tatarin
 * @supervisor Ing. Jaromir Konecny, Ph.D.
 * @email   lukas@tatarin.cz
 * @version 1.10
 * @ide     Android Studio 4.1.2
 * @license GNU GPL v3
 * @brief   StatisticalData.java
 * @lastmodify 2021/03/05 12:12:13
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

package cz.vsb.cbe.testbed.utils;

import java.util.Collections;
import java.util.List;

import cz.vsb.cbe.testbed.sql.Record;

public class StatisticalData {

    private final int mDataSetSize;
    private final float mMeanValue;
    private final Record mMinValue;
    private final Record mFirstQuartile;
    private final Record mMedian;
    private final Record mThirdQuartile;
    private final Record mMaxValue;
    private final float mStandardDeviation;
    private float mDataSetSum;

    public StatisticalData(List<Record> records) {
        if (records != null) {
            Collections.sort(records);

            this.mDataSetSum = 0;
            this.mDataSetSize = records.size();
            this.mMinValue = records.get(0);
            if ((int) Math.ceil((float) records.size() * 25 / 100) >= records.size()) {
                this.mFirstQuartile = records.get(records.size() - 1);
            } else {
                this.mFirstQuartile = records.get((int) Math.ceil((float) records.size() * 25 / 100));
            }
            if ((int) Math.ceil((float) records.size() * 50 / 100) >= records.size()) {
                this.mMedian = records.get(records.size() - 1);
            } else {
                this.mMedian = records.get((int) Math.ceil((float) records.size() * 50 / 100));
            }
            if ((int) Math.ceil((float) records.size() * 75 / 100) >= records.size()) {
                this.mThirdQuartile = records.get(records.size() - 1);
            } else {
                this.mThirdQuartile = records.get((int) Math.ceil((float) records.size() * 75 / 100));
            }
            this.mMaxValue = records.get(records.size() - 1);
            float variance;

            for (Record record : records) {
                this.mDataSetSum += record.getValue();
            }

            this.mMeanValue = this.mDataSetSum / records.size();

            float squareSum = 0;
            for (Record record : records) {
                squareSum += Math.pow((record.getValue() - this.mMeanValue), 2);
            }

            variance = squareSum / (this.mDataSetSum - 1);
            this.mStandardDeviation = (float) Math.sqrt(variance);
        } else {
            mDataSetSize = 0;
            mMeanValue = 0;
            mMinValue = new Record();
            mFirstQuartile = new Record();
            mMedian = new Record();
            mThirdQuartile = new Record();
            mMaxValue = new Record();
            mStandardDeviation = 0;
            mDataSetSum = 0;
        }
    }

    public int getDataSetSize() {
        return mDataSetSize;
    }

    public float getDataSetSum() {
        return mDataSetSum;
    }

    public float getMeanValue() {
        return mMeanValue;
    }

    public Record getMinValue() {
        return mMinValue;
    }

    public Record getFirstQuartile() {
        return mFirstQuartile;
    }

    public Record getMedian() {
        return mMedian;
    }

    public Record getThirdQuartile() {
        return mThirdQuartile;
    }

    public Record getMaxValue() {
        return mMaxValue;
    }

    public float getStandardDeviation() {
        return mStandardDeviation;
    }
}