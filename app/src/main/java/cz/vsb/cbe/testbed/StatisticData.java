/*
  @author  Bc. Lukas Tatarin
 * @supervisor Ing. Jaromir Konecny, Ph.D.
 * @email   lukas@tatarin.cz
 * @version 1.00
 * @ide     Android Studio 4.1.2
 * @license GNU GPL v3
 * @brief   StatisticData
 * @lastmodify 2021/02/15 12:17:53
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

import java.util.Collections;
import java.util.List;

import cz.vsb.cbe.testbed.sql.Record;

public class StatisticData {

    private final int mDataSetSize;
    private final float mMeanValue;
    private final Record mMinValue;
    private final Record mFirstQuartile;
    private final Record mMedian;
    private final Record mThirdQuartile;
    private final Record mMaxValue;
    private final float mStandardDeviation;
    private float mDataSetSum;

    public StatisticData(List<Record> records) {
        Collections.sort(records);

        this.mDataSetSum = 0;
        this.mDataSetSize = records.size();
        this.mMinValue = records.get(0);
        this.mFirstQuartile = records.get((int) Math.ceil((float) records.size() * 25 / 100));
        this.mMedian = records.get((int) Math.ceil((float) records.size() * 50 / 100));
        this.mThirdQuartile = records.get((int) Math.ceil((float) records.size() * 75 / 100));
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