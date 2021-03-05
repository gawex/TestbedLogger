/*
  @author  Bc. Lukas Tatarin
 * @supervisor Ing. Jaromir Konecny, Ph.D.
 * @email   lukas@tatarin.cz
 * @version 1.10
 * @ide     Android Studio 4.1.2
 * @license GNU GPL v3
 * @brief   OverviewFragment.java
 * @lastmodify 2021/03/05 11:57:09
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

package cz.vsb.cbe.testbed.fragments;

import android.annotation.SuppressLint;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.google.android.material.tabs.TabLayout;

import java.util.Calendar;
import java.util.Date;
import java.util.List;

import cz.vsb.cbe.testbed.R;
import cz.vsb.cbe.testbed.activitiesAndServices.BluetoothLeService;
import cz.vsb.cbe.testbed.activitiesAndServices.DatabaseActivity;
import cz.vsb.cbe.testbed.sql.DatabaseResult;
import cz.vsb.cbe.testbed.sql.Record;
import cz.vsb.cbe.testbed.sql.TestbedDatabase;
import cz.vsb.cbe.testbed.utils.RecordValueFormatter;
import cz.vsb.cbe.testbed.utils.StatisticalData;

public class OverviewFragment extends BaseVisualisationFragment {

    public static final int INVISIBLE_POSITION = -1;
    public static final int DATA_SET_SUM_POSITION = 0;
    public static final int MIN_VALUE_POSITION = 1;
    public static final int MEDIAN_VALUE_POSITION = 2;
    public static final int MAX_VALUE_POSITION = 3;
    public static final int MEAN_VALUE_POSITION = 4;

    private TextView mTxvLastValue;
    private TextView mTxvLastValueTimeStamp;

    private int mCurrentSummaryValueType = DATA_SET_SUM_POSITION;
    private int mDataSetSumPosition;
    private int mMinValuePosition;
    private int mMedianPosition;
    private int mMaxValuePosition;
    private int mMeanValuePosition;

    private TabLayout mTblSummary;

    private TextView mTxvThisDayValue;
    private TextView mTxvThisDayValueTimeStamp;
    private TextView mTxvThisWeekValue;
    private TextView mTxvThisWeekValueTimeStamp;
    private TextView mTxvThisMonthValue;
    private TextView mTxvThisMonthValueTimeStamp;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mDatabaseActivity = (DatabaseActivity) getActivity();
        assert mDatabaseActivity != null;
        mTestbedDatabase = mDatabaseActivity.getTestbedDatabase();
        mTestbedDevice = mDatabaseActivity.getTestbedDevice();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.fragment_overview, container, false);

        mTblSummary = view.findViewById(R.id.fragment_overview_tbl_change_sumary);
        mTblSummary.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                if (tab.getPosition() == mDataSetSumPosition) {
                    mCurrentSummaryValueType = DATA_SET_SUM_POSITION;
                } else if (tab.getPosition() == mMinValuePosition) {
                    mCurrentSummaryValueType = MIN_VALUE_POSITION;
                } else if (tab.getPosition() == mMeanValuePosition) {
                    mCurrentSummaryValueType = MEAN_VALUE_POSITION;
                } else if (tab.getPosition() == mMaxValuePosition) {
                    mCurrentSummaryValueType = MAX_VALUE_POSITION;
                } else if (tab.getPosition() == mMedianPosition) {
                    mCurrentSummaryValueType = MEDIAN_VALUE_POSITION;
                }
                updateIntervalValue(Calendar.DAY_OF_MONTH, false);
                updateIntervalValue(Calendar.WEEK_OF_YEAR, false);
                updateIntervalValue(Calendar.MONTH, false);
            }

            @Override
            public void onTabUnselected(TabLayout.Tab tab) {

            }

            @Override
            public void onTabReselected(TabLayout.Tab tab) {

            }
        });

        mTxvLastValue = view.findViewById(R.id.fragment_overview_txv_last_value);
        mTxvLastValueTimeStamp = view.findViewById(R.id.fragment_overview_last_value_timestamp);

        mTxvThisDayValue = view.findViewById(R.id.fragment_overview_txv_this_day_value);
        mTxvThisDayValueTimeStamp = view
                .findViewById(R.id.fragment_overview_txv_this_day_value_timestamp);

        mTxvThisWeekValue = view.findViewById(R.id.fragment_overview_txv_this_week_value);
        mTxvThisWeekValueTimeStamp = view
                .findViewById(R.id.fragment_overview_txv_this_week_value_timestamp);

        mTxvThisMonthValue = view.findViewById(R.id.fragment_overview_txv_this_month_value);
        mTxvThisMonthValueTimeStamp = view
                .findViewById(R.id.fragment_overview_txv_this_month_value_timestamp);

        sensorChanged();
        return view;
    }

    @SuppressLint("NonConstantResourceId")
    public void sensorChanged() {
        mTblSummary.removeAllTabs();

        mDataSetSumPosition = INVISIBLE_POSITION;
        mMinValuePosition = INVISIBLE_POSITION;
        mMedianPosition = INVISIBLE_POSITION;
        mMaxValuePosition = INVISIBLE_POSITION;
        mMeanValuePosition = INVISIBLE_POSITION;

        switch (mDatabaseActivity.getActualSelectedSensor()) {
            case R.id.pedometer:
                TabLayout.Tab tabDataSetSum = mTblSummary.newTab();
                tabDataSetSum.setText(getString(R.string.fragment_overview_data_set_sum_label));
                mTblSummary.addTab(tabDataSetSum, 0);
                mDataSetSumPosition = tabDataSetSum.getPosition();

                mCurrentSummaryValueType = DATA_SET_SUM_POSITION;
                tabDataSetSum.select();
                break;

            case R.id.heart_rate:
            case R.id.temperature:
                TabLayout.Tab tabMinValue = mTblSummary.newTab();
                tabMinValue.setText(getString(R.string.fragment_overview_min_value_label));
                mTblSummary.addTab(tabMinValue, 0);
                mMinValuePosition = tabMinValue.getPosition();
                TabLayout.Tab tabMedian = mTblSummary.newTab();
                tabMedian.setText(getString(R.string.fragment_overview_median_label));
                mTblSummary.addTab(tabMedian, 1);
                mMedianPosition = tabMedian.getPosition();
                TabLayout.Tab tabMaxValue = mTblSummary.newTab();
                tabMaxValue.setText(getString(R.string.fragment_overview_max_value_label));
                mTblSummary.addTab(tabMaxValue, 2);
                mMaxValuePosition = tabMaxValue.getPosition();
                TabLayout.Tab tabMeanValue = mTblSummary.newTab();
                tabMeanValue.setText(getString(R.string.fragment_overview_mean_value_label));
                mTblSummary.addTab(tabMeanValue, 3);
                mMeanValuePosition = tabMeanValue.getPosition();

                mCurrentSummaryValueType = MEAN_VALUE_POSITION;
                tabMeanValue.select();
                break;

            default:
                throw new IllegalStateException(
                        "Unexpected value od mDatabaseActivity.getActualSelectedSensor(): " +
                                mDatabaseActivity.getActualSelectedSensor());
        }
        updateValues(false);
    }

    public void updateValues(boolean blink) {
        updateLastValue(blink);
        updateIntervalValue(Calendar.DAY_OF_MONTH, blink);
        updateIntervalValue(Calendar.WEEK_OF_YEAR, blink);
        updateIntervalValue(Calendar.MONTH, blink);
    }

    @SuppressWarnings("unchecked")
    @SuppressLint("NonConstantResourceId")
    private void updateLastValue(boolean blink) {
        switch (mDatabaseActivity.getActualSelectedSensor()) {
            case R.id.pedometer:
                if (mDatabaseActivity.getBluetoothLeService() != null &&
                        mDatabaseActivity.getBluetoothLeService().getLastStepValue() != null) {
                    updateValueAndTimestampTextviewPair(mTxvLastValue, mTxvLastValueTimeStamp,
                            mDatabaseActivity.getBluetoothLeService().getLastStepValue(),
                            mDatabaseActivity.getBluetoothLeService().getLastStepTimeStamp(),
                            RecordValueFormatter.PATERN_6_0, blink);
                } else {
                    mTestbedDatabase.selectFirstRecordLessThanTimeStamp(mTestbedDevice,
                            BluetoothLeService.STEPS_DATA, new Date(), databaseResult -> {
                                if (databaseResult instanceof DatabaseResult.Success) {
                                    Record lastRecord = ((DatabaseResult.Success<Record>) databaseResult).data;
                                    updateValueAndTimestampTextviewPair(mTxvLastValue,
                                            mTxvLastValueTimeStamp,
                                            lastRecord.getValue(),
                                            lastRecord.getTimeStamp(),
                                            RecordValueFormatter.PATERN_6_0, blink);
                                } else if (databaseResult instanceof DatabaseResult.Error) {
                                    updateValueAndTimestampTextviewPair(mTxvLastValue,
                                            mTxvLastValueTimeStamp, null, null,
                                            null, blink);
                                }
                            });
                }
                break;

            case R.id.heart_rate:
                if (mDatabaseActivity.getBluetoothLeService() != null &&
                        mDatabaseActivity.getBluetoothLeService().getLastHeartRateValue() != null) {
                    updateValueAndTimestampTextviewPair(mTxvLastValue, mTxvLastValueTimeStamp,
                            mDatabaseActivity.getBluetoothLeService().getLastHeartRateValue(),
                            mDatabaseActivity.getBluetoothLeService().getLastHeartRateTimeStamp(),
                            RecordValueFormatter.PATERN_3_0, blink);
                } else {
                    mTestbedDatabase.selectFirstRecordLessThanTimeStamp(mTestbedDevice,
                            BluetoothLeService.HEART_RATE_DATA, new Date(), databaseResult -> {
                                if (databaseResult instanceof DatabaseResult.Success) {
                                    Record lastRecord = ((DatabaseResult.Success<Record>) databaseResult).data;
                                    updateValueAndTimestampTextviewPair(mTxvLastValue,
                                            mTxvLastValueTimeStamp,
                                            lastRecord.getValue(),
                                            lastRecord.getTimeStamp(),
                                            RecordValueFormatter.PATERN_3_0, blink);
                                } else if (databaseResult instanceof DatabaseResult.Error) {
                                    updateValueAndTimestampTextviewPair(mTxvLastValue,
                                            mTxvLastValueTimeStamp, null, null,
                                            null, blink);
                                }
                            });
                }
                break;

            case R.id.temperature:
                if (mDatabaseActivity.getBluetoothLeService() != null &&
                        mDatabaseActivity.getBluetoothLeService().getmLastTemperatureValue() != null) {
                    updateValueAndTimestampTextviewPair(mTxvLastValue, mTxvLastValueTimeStamp,
                            mDatabaseActivity.getBluetoothLeService().getmLastTemperatureValue(),
                            mDatabaseActivity.getBluetoothLeService().getmLastTemperatureTimeStamp(),
                            RecordValueFormatter.PATERN_3_2, blink);
                } else {
                    mTestbedDatabase.selectFirstRecordLessThanTimeStamp(mTestbedDevice,
                            BluetoothLeService.TEMPERATURE_DATA, new Date(), databaseResult -> {
                                if (databaseResult instanceof DatabaseResult.Success) {
                                    Record lastRecord = ((DatabaseResult.Success<Record>) databaseResult).data;
                                    updateValueAndTimestampTextviewPair(mTxvLastValue,
                                            mTxvLastValueTimeStamp,
                                            lastRecord.getValue(),
                                            lastRecord.getTimeStamp(),
                                            RecordValueFormatter.PATERN_3_2, blink);
                                } else if (databaseResult instanceof DatabaseResult.Error) {
                                    updateValueAndTimestampTextviewPair(mTxvLastValue,
                                            mTxvLastValueTimeStamp, null, null,
                                            null, blink);
                                }
                            });
                }
                break;

            default:
                throw new IllegalStateException(
                        "Unexpected value of mDatabaseActivity.getActualSelectedSensor(): " +
                                mDatabaseActivity.getActualSelectedSensor());
        }
    }

    private void updateIntervalValue(int interval, boolean blink) {
        TextView txvValue;
        TextView txvValueTimeStamp;

        switch (interval) {
            case Calendar.DAY_OF_MONTH:
                txvValue = mTxvThisDayValue;
                txvValueTimeStamp = mTxvThisDayValueTimeStamp;
                break;

            case Calendar.WEEK_OF_YEAR:
                txvValue = mTxvThisWeekValue;
                txvValueTimeStamp = mTxvThisWeekValueTimeStamp;
                break;

            case Calendar.MONTH:
                txvValue = mTxvThisMonthValue;
                txvValueTimeStamp = mTxvThisMonthValueTimeStamp;
                break;

            default:
                throw new IllegalStateException("Unexpected value of scale: " + interval);
        }

        getStatisticalData(interval, new OnStatisticDataSelected() {
            @SuppressLint("NonConstantResourceId")
            @Override
            public void onDataAvailable(StatisticalData statisticalData) {
                float value;
                Date valueTimeStamp;
                String pattern;

                switch (mCurrentSummaryValueType) {
                    case MIN_VALUE_POSITION:
                        value = statisticalData.getMinValue().getValue();
                        valueTimeStamp = statisticalData.getMinValue().getTimeStamp();
                        switch (mDatabaseActivity.getActualSelectedSensor()) {
                            case R.id.pedometer:
                                pattern = RecordValueFormatter.PATERN_6_0;
                                break;

                            case R.id.heart_rate:
                                pattern = RecordValueFormatter.PATERN_3_0;
                                break;

                            case R.id.temperature:
                                pattern = RecordValueFormatter.PATERN_3_2;
                                break;

                            default:
                                throw new IllegalStateException(
                                        "Unexpected value of mDatabaseActivity.getActualSelectedSensor(): " +
                                                mDatabaseActivity.getActualSelectedSensor());
                        }
                        break;

                    case MEDIAN_VALUE_POSITION:
                        value = statisticalData.getMedian().getValue();
                        valueTimeStamp = statisticalData.getMedian().getTimeStamp();
                        switch (mDatabaseActivity.getActualSelectedSensor()) {
                            case R.id.pedometer:
                                pattern = RecordValueFormatter.PATERN_6_0;
                                break;

                            case R.id.heart_rate:
                                pattern = RecordValueFormatter.PATERN_3_0;
                                break;

                            case R.id.temperature:
                                pattern = RecordValueFormatter.PATERN_3_2;
                                break;

                            default:
                                throw new IllegalStateException(
                                        "Unexpected value of mDatabaseActivity.getActualSelectedSensor(): " +
                                                mDatabaseActivity.getActualSelectedSensor());
                        }
                        break;

                    case MAX_VALUE_POSITION:
                        value = statisticalData.getMaxValue().getValue();
                        valueTimeStamp = statisticalData.getMaxValue().getTimeStamp();
                        switch (mDatabaseActivity.getActualSelectedSensor()) {
                            case R.id.pedometer:
                                pattern = RecordValueFormatter.PATERN_6_0;
                                break;

                            case R.id.heart_rate:
                                pattern = RecordValueFormatter.PATERN_3_0;
                                break;

                            case R.id.temperature:
                                pattern = RecordValueFormatter.PATERN_3_2;
                                break;

                            default:
                                throw new IllegalStateException(
                                        "Unexpected value of mDatabaseActivity.getActualSelectedSensor(): " +
                                                mDatabaseActivity.getActualSelectedSensor());
                        }
                        break;

                    case MEAN_VALUE_POSITION:
                        value = statisticalData.getMeanValue();
                        valueTimeStamp = null;
                        pattern = RecordValueFormatter.PATERN_3_2;
                        break;

                    case DATA_SET_SUM_POSITION:
                        value = statisticalData.getDataSetSum();
                        valueTimeStamp = null;
                        pattern = RecordValueFormatter.PATERN_6_0;
                        break;

                    default:
                        throw new IllegalStateException(
                                "Unexpected value of: mCurrentSummaryValueType" +
                                        mCurrentSummaryValueType);
                }
                updateValueAndTimestampTextviewPair(txvValue, txvValueTimeStamp, value,
                        valueTimeStamp, pattern, blink);
            }

            @Override
            public void onDataMissing() {
                updateValueAndTimestampTextviewPair(txvValue, txvValueTimeStamp, null,
                        null, null, blink);

            }
        });

    }

    @SuppressWarnings("unchecked")
    @SuppressLint("NonConstantResourceId")
    private void getStatisticalData(int interval, OnStatisticDataSelected onStatisticDataSelected) {
        Date[] intervals;
        Calendar intervalBase = Calendar.getInstance();

        switch (interval) {
            case Calendar.DAY_OF_MONTH:
                intervals = getIntervals(intervalBase, Calendar.HOUR_OF_DAY);
                break;

            case Calendar.WEEK_OF_YEAR:
                intervals = getIntervals(intervalBase, Calendar.DAY_OF_WEEK);
                break;

            case Calendar.MONTH:
                intervals = getIntervals(intervalBase, Calendar.DAY_OF_MONTH);
                break;

            default:
                throw new IllegalStateException("Unexpected value of interval: " + interval);
        }

        switch (mDatabaseActivity.getActualSelectedSensor()) {
            case R.id.pedometer:
                mTestbedDatabase.selectFirstRecordLessThanTimeStamp(
                        mTestbedDevice,
                        BluetoothLeService.STEPS_DATA,
                        intervals[0],
                        databaseResult -> {
                            Record lastRecordBeforeInterval = null;
                            if (databaseResult instanceof DatabaseResult.Success) {
                                lastRecordBeforeInterval = ((DatabaseResult.Success<Record>) databaseResult).data;
                            } else if (databaseResult instanceof DatabaseResult.Error) {
                                lastRecordBeforeInterval = new Record();
                            }
                            Record finalLastRecordBeforeInterval = lastRecordBeforeInterval;
                            mTestbedDatabase.selectRecordsBetweenTimeStamp(
                                    mTestbedDevice,
                                    BluetoothLeService.STEPS_DATA,
                                    intervals,
                                    TestbedDatabase.SORT_BY.DEFAULT,
                                    TestbedDatabase.SORT_ORDER.DEFAULT, databaseResult1 -> {
                                        if (databaseResult1 instanceof DatabaseResult.Success) {
                                            onStatisticDataSelected.onDataAvailable(
                                                    getStatisticalDataByIntervals(
                                                            sortStepsByIntervals(
                                                                    finalLastRecordBeforeInterval,
                                                                    sortRecordsByIntervals(
                                                                            ((DatabaseResult.Success<List<Record>>) databaseResult1).data,
                                                                            intervalBase, interval)))
                                                            .get(interval == Calendar.MONTH ? intervalBase.get(interval) + 1 : intervalBase.get(interval)));
                                        } else if (databaseResult1 instanceof DatabaseResult.Error) {
                                            onStatisticDataSelected.onDataMissing();
                                        }
                                    });
                        });
                break;

            case R.id.heart_rate:
                mTestbedDatabase.selectRecordsBetweenTimeStamp(
                        mTestbedDevice,
                        BluetoothLeService.HEART_RATE_DATA,
                        intervals,
                        TestbedDatabase.SORT_BY.DEFAULT,
                        TestbedDatabase.SORT_ORDER.DEFAULT,
                        databaseResult -> {
                            if (databaseResult instanceof DatabaseResult.Success) {
                                onStatisticDataSelected.onDataAvailable(getStatisticalDataByIntervals(
                                        sortRecordsByIntervals(
                                                ((DatabaseResult.Success<List<Record>>) databaseResult).data,
                                                intervalBase, interval))
                                        .get(interval == Calendar.MONTH ? intervalBase.get(interval) + 1 : intervalBase.get(interval)));
                            } else if (databaseResult instanceof DatabaseResult.Error) {
                                onStatisticDataSelected.onDataMissing();
                            }
                        });
                break;

            case R.id.temperature:
                mTestbedDatabase.selectRecordsBetweenTimeStamp(
                        mTestbedDevice,
                        BluetoothLeService.TEMPERATURE_DATA,
                        intervals,
                        TestbedDatabase.SORT_BY.DEFAULT,
                        TestbedDatabase.SORT_ORDER.DEFAULT,
                        databaseResult -> {
                            if (databaseResult instanceof DatabaseResult.Success) {
                                onStatisticDataSelected.onDataAvailable(getStatisticalDataByIntervals(
                                        sortRecordsByIntervals(
                                                ((DatabaseResult.Success<List<Record>>) databaseResult).data,
                                                intervalBase, interval))
                                        .get(interval == Calendar.MONTH ? intervalBase.get(interval) + 1 : intervalBase.get(interval)));
                            } else if (databaseResult instanceof DatabaseResult.Error) {
                                onStatisticDataSelected.onDataMissing();
                            }
                        });
                break;

            default:
                throw new IllegalStateException(
                        "Unexpected value mDatabaseActivity.getActualSelectedSensor(): " +
                                mDatabaseActivity.getActualSelectedSensor());
        }
    }

    private interface OnStatisticDataSelected {
        void onDataAvailable(StatisticalData statisticalData);

        void onDataMissing();
    }
}