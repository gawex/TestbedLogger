/*
  @author  Bc. Lukas Tatarin
 * @supervisor Ing. Jaromir Konecny, Ph.D.
 * @email   lukas@tatarin.cz
 * @version 1.10
 * @ide     Android Studio 4.1.2
 * @license GNU GPL v3
 * @brief   MyMarkerView.java
 * @lastmodify 2021/03/05 11:51:11
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

package cz.vsb.cbe.testbed.chart;

import android.annotation.SuppressLint;
import android.content.Context;
import android.widget.TextView;

import com.github.mikephil.charting.components.MarkerView;
import com.github.mikephil.charting.data.BarEntry;
import com.github.mikephil.charting.data.CandleEntry;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.highlight.Highlight;
import com.github.mikephil.charting.utils.MPPointF;

import cz.vsb.cbe.testbed.R;
import cz.vsb.cbe.testbed.utils.IntervalValueFormatter;
import cz.vsb.cbe.testbed.utils.RecordValueFormatter;

@SuppressLint("ViewConstructor")
public class MyMarkerView extends MarkerView {

    private final TextView mTxvContent;
    private final Context mContext;
    private final int mRangeLevel;
    private final int mDataType;

    public MyMarkerView(Context context, int rangeLevel, int dataType) {
        super(context, R.layout.chart_marker_view);
        mTxvContent = findViewById(R.id.marker_view_txv);
        mRangeLevel = rangeLevel;
        mDataType = dataType;
        mContext = context;
    }

    @SuppressLint({"NonConstantResourceId", "SetTextI18n"})
    @Override
    public void refreshContent(Entry e, Highlight highlight) {
        if (e instanceof BarEntry) {
            if (mDataType == (R.id.pedometer)) {
                mTxvContent.setText(IntervalValueFormatter.formatIntervalLevel(mContext, mRangeLevel,
                        e.getX()) +
                        "\n" +
                        mContext.getString(R.string.my_marker_view_steps_sum_label_short) + "  " +
                        RecordValueFormatter.formatSteps(RecordValueFormatter.PATERN_6_0, e.getY()));
            }
        } else if (e instanceof CandleEntry) {
            CandleEntry candleEntry = (CandleEntry) e;
            switch (mDataType) {
                case R.id.heart_rate:
                    mTxvContent.setText(IntervalValueFormatter.formatIntervalLevel(mContext, mRangeLevel,
                            e.getX()) +
                            "\n" +
                            mContext.getString(R.string.my_marker_view_min_value_label_short) + "  " +
                            RecordValueFormatter
                                    .formatHearRate(RecordValueFormatter.PATERN_3_0,
                                            candleEntry.getLow()) +
                            "\n" +
                            mContext.getString(R.string.my_marker_view_first_quartile_label_short) + "  " +
                            RecordValueFormatter
                                    .formatHearRate(RecordValueFormatter.PATERN_3_0,
                                            candleEntry.getClose()) +
                            "\n" +
                            mContext.getString(R.string.my_marker_view_third_quartile_short) + "  " +
                            RecordValueFormatter
                                    .formatHearRate(RecordValueFormatter.PATERN_3_0,
                                            candleEntry.getOpen()) +
                            "\n" +
                            mContext.getString(R.string.my_marker_view_max_value_short) + "  " +
                            RecordValueFormatter
                                    .formatHearRate(RecordValueFormatter.PATERN_3_0,
                                            candleEntry.getHigh()));
                    break;

                case R.id.temperature:
                    mTxvContent.setText(IntervalValueFormatter.formatIntervalLevel(mContext, mRangeLevel,
                            e.getX()) +
                            "\n" +
                            mContext.getString(R.string.my_marker_view_min_value_label_short) + "  " +
                            RecordValueFormatter
                                    .formatTemperature(RecordValueFormatter.PATERN_3_2,
                                            candleEntry.getLow()) +
                            "\n" +
                            mContext.getString(R.string.my_marker_view_first_quartile_label_short) + "  " +
                            RecordValueFormatter
                                    .formatTemperature(RecordValueFormatter.PATERN_3_2,
                                            candleEntry.getClose()) +
                            "\n" +
                            mContext.getString(R.string.my_marker_view_third_quartile_short) + "  " +
                            RecordValueFormatter
                                    .formatTemperature(RecordValueFormatter.PATERN_3_2,
                                            candleEntry.getOpen()) +
                            "\n" +
                            mContext.getString(R.string.my_marker_view_max_value_short) + "  " +
                            RecordValueFormatter
                                    .formatTemperature(RecordValueFormatter.PATERN_3_2,
                                            candleEntry.getHigh()));
                    break;

                default:
                    throw new IllegalStateException("Unexpected value of mDataType: " + mDataType);
            }
        } else if (e != null) {
            switch (mDataType) {
                case R.id.heart_rate:
                    mTxvContent.setText(IntervalValueFormatter.formatIntervalLevel(mContext,
                            mRangeLevel,
                            e.getX()) +
                            "\n" +
                            mContext.getString(R.string.my_marker_view_mean_value_short) + "  " +
                            RecordValueFormatter
                                    .formatHearRate(RecordValueFormatter.PATERN_3_2, e.getY()));
                    break;

                case R.id.temperature:
                    mTxvContent.setText(IntervalValueFormatter.formatIntervalLevel(mContext,
                            mRangeLevel,
                            e.getX()) +
                            "\n" +
                            mContext.getString(R.string.my_marker_view_mean_value_short) + "  " +
                            RecordValueFormatter
                                    .formatTemperature(RecordValueFormatter.PATERN_3_2, e.getY()));
                    break;

                default:
                    throw new IllegalStateException("Unexpected value of mDataType: " + mDataType);
            }
        }
        super.refreshContent(e, highlight);
    }


    @Override
    public MPPointF getOffset() {
        return new MPPointF(-(getWidth() / 2f), -getHeight());
    }
}