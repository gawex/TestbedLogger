/*
  @author  Bc. Lukas Tatarin
 * @supervisor Ing. Jaromir Konecny, Ph.D.
 * @email   lukas@tatarin.cz
 * @version 1.10
 * @ide     Android Studio 4.1.2
 * @license GNU GPL v3
 * @brief   BaseVisualisationFragment.java
 * @lastmodify 2021/03/05 11:54:47
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
import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.NumberPicker;
import android.widget.ProgressBar;
import android.widget.SeekBar;
import android.widget.TextView;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.content.res.AppCompatResources;
import androidx.fragment.app.Fragment;

import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import cz.vsb.cbe.testbed.R;
import cz.vsb.cbe.testbed.activitiesAndServices.BluetoothLeService;
import cz.vsb.cbe.testbed.activitiesAndServices.DatabaseActivity;
import cz.vsb.cbe.testbed.sql.Record;
import cz.vsb.cbe.testbed.sql.TestbedDatabase;
import cz.vsb.cbe.testbed.sql.TestbedDevice;
import cz.vsb.cbe.testbed.utils.RecordValueFormatter;
import cz.vsb.cbe.testbed.utils.StatisticalData;

@SuppressWarnings("ALL")
public class BaseVisualisationFragment extends Fragment {

    public static final int BLINK_TIME_MS = 250;

    protected DatabaseActivity mDatabaseActivity;
    protected TestbedDatabase mTestbedDatabase;
    protected TestbedDevice mTestbedDevice;
    protected List<Record> mRecords;

    protected ImageButton mImbDecrementActualInterval;
    protected TextView mTxvActualInterval;
    protected ImageButton mImbChangeInterval;
    protected ProgressBar mPgbWorking;

    protected TextView mTxvNoDataAvailableLabel;
    protected TextView mTxvNoDataAvailableInterval;
    protected Button mBtnChangeInterval;
    protected ImageButton mImbShowStatisticsInfo;

    private AlertDialog mStatisticalDataDialog;
    private boolean mStatisticalDataDialogVisible = false;

    private TextView mTxvDataSetSize;
    private TextView mTxvDataSetSum;
    private TextView mTxvMeanValue;
    private TextView mTxvStandardDeviation;

    private TextView mTxvMinValue;
    private TextView mTxvMinValueTimestamp;
    private TextView mTxvMinValueRecordId;

    private TextView mTxvMedian;
    private TextView mTxvMedianTimestamp;
    private TextView mTxvMedianRecordId;

    private TextView mTxvMaxValue;
    private TextView mTxvMaxValueTimestamp;
    private TextView mTxvMaxValueRecordId;

    public List<Record> getRecords() {
        return mRecords;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mDatabaseActivity = (DatabaseActivity) getActivity();
        mTestbedDatabase = mDatabaseActivity.getTestbedDatabase();
        mTestbedDevice = mDatabaseActivity.getTestbedDevice();
    }

    protected Date[] getIntervals(Calendar intervalBase, int intervalScale) {
        Calendar startPeriod = Calendar.getInstance();
        startPeriod.set(Calendar.YEAR, startPeriod.getActualMinimum(Calendar.YEAR));
        startPeriod.set(Calendar.MONTH, startPeriod.getActualMinimum(Calendar.MONTH));
        startPeriod.set(Calendar.DAY_OF_MONTH, startPeriod.getActualMinimum(Calendar.DAY_OF_MONTH));
        startPeriod.set(Calendar.HOUR_OF_DAY, startPeriod.getActualMinimum(Calendar.HOUR_OF_DAY));
        startPeriod.set(Calendar.MINUTE, startPeriod.getActualMinimum(Calendar.MINUTE));
        startPeriod.set(Calendar.SECOND, startPeriod.getActualMinimum(Calendar.SECOND));
        startPeriod.set(Calendar.MILLISECOND, startPeriod.getActualMinimum(Calendar.MILLISECOND));

        Calendar endPeriod = Calendar.getInstance();
        endPeriod.set(Calendar.YEAR, endPeriod.getActualMaximum(Calendar.YEAR));
        endPeriod.set(Calendar.MONTH, endPeriod.getActualMaximum(Calendar.MONTH));
        endPeriod.set(Calendar.DAY_OF_MONTH, endPeriod.getActualMaximum(Calendar.DAY_OF_MONTH));
        endPeriod.set(Calendar.HOUR_OF_DAY, endPeriod.getActualMaximum(Calendar.HOUR_OF_DAY));
        endPeriod.set(Calendar.MINUTE, endPeriod.getActualMaximum(Calendar.MINUTE));
        endPeriod.set(Calendar.SECOND, endPeriod.getActualMaximum(Calendar.SECOND));
        endPeriod.set(Calendar.MILLISECOND, endPeriod.getActualMaximum(Calendar.MILLISECOND));

        switch (intervalScale) {
            default:
            case Calendar.YEAR:
                startPeriod.set(Calendar.YEAR, startPeriod.getActualMinimum(Calendar.YEAR));
                endPeriod.set(Calendar.YEAR, endPeriod.getActualMaximum(Calendar.YEAR));
                break;

            case Calendar.MONTH:
                startPeriod.set(Calendar.YEAR, intervalBase.get(Calendar.YEAR));
                endPeriod.set(Calendar.YEAR, intervalBase.get(Calendar.YEAR));
                break;

            case Calendar.DAY_OF_MONTH:
                startPeriod.set(Calendar.YEAR, intervalBase.get(Calendar.YEAR));
                startPeriod.set(Calendar.MONTH, intervalBase.get(Calendar.MONTH));
                endPeriod.set(Calendar.YEAR, intervalBase.get(Calendar.YEAR));
                endPeriod.set(Calendar.MONTH, intervalBase.get(Calendar.MONTH));
                endPeriod.set(Calendar.DAY_OF_MONTH, intervalBase
                        .getActualMaximum(Calendar.DAY_OF_MONTH));
                break;

            case Calendar.DAY_OF_WEEK:
                startPeriod.set(Calendar.YEAR, intervalBase.get(Calendar.YEAR));
                startPeriod.set(Calendar.MONTH, intervalBase.get(Calendar.MONTH));
                startPeriod.set(Calendar.DAY_OF_WEEK, Calendar.MONDAY);
                endPeriod.setTime(startPeriod.getTime());
                endPeriod.add(Calendar.WEEK_OF_YEAR, 1);
                endPeriod.add(Calendar.MILLISECOND, -1);
                break;

            case Calendar.HOUR_OF_DAY:
                startPeriod.set(Calendar.YEAR, intervalBase.get(Calendar.YEAR));
                startPeriod.set(Calendar.MONTH, intervalBase.get(Calendar.MONTH));
                startPeriod.set(Calendar.DAY_OF_MONTH, intervalBase.get(Calendar.DAY_OF_MONTH));
                endPeriod.set(Calendar.YEAR, intervalBase.get(Calendar.YEAR));
                endPeriod.set(Calendar.MONTH, intervalBase.get(Calendar.MONTH));
                endPeriod.set(Calendar.DAY_OF_MONTH, intervalBase.get(Calendar.DAY_OF_MONTH));
                break;

            case Calendar.MINUTE:
                startPeriod.set(Calendar.YEAR, intervalBase.get(Calendar.YEAR));
                startPeriod.set(Calendar.MONTH, intervalBase.get(Calendar.MONTH));
                startPeriod.set(Calendar.DAY_OF_MONTH, intervalBase.get(Calendar.DAY_OF_MONTH));
                startPeriod.set(Calendar.HOUR_OF_DAY, intervalBase.get(Calendar.HOUR_OF_DAY));
                endPeriod.set(Calendar.YEAR, intervalBase.get(Calendar.YEAR));
                endPeriod.set(Calendar.MONTH, intervalBase.get(Calendar.MONTH));
                endPeriod.set(Calendar.DAY_OF_MONTH, intervalBase.get(Calendar.DAY_OF_MONTH));
                endPeriod.set(Calendar.HOUR_OF_DAY, intervalBase.get(Calendar.HOUR_OF_DAY));
                break;

            case Calendar.SECOND:
                startPeriod.set(Calendar.YEAR, intervalBase.get(Calendar.YEAR));
                startPeriod.set(Calendar.MONTH, intervalBase.get(Calendar.MONTH));
                startPeriod.set(Calendar.DAY_OF_MONTH, intervalBase.get(Calendar.DAY_OF_MONTH));
                startPeriod.set(Calendar.HOUR_OF_DAY, intervalBase.get(Calendar.HOUR_OF_DAY));
                startPeriod.set(Calendar.MINUTE, intervalBase.get(Calendar.MINUTE));
                endPeriod.set(Calendar.YEAR, intervalBase.get(Calendar.YEAR));
                endPeriod.set(Calendar.MONTH, intervalBase.get(Calendar.MONTH));
                endPeriod.set(Calendar.DAY_OF_MONTH, intervalBase.get(Calendar.DAY_OF_MONTH));
                endPeriod.set(Calendar.HOUR_OF_DAY, intervalBase.get(Calendar.HOUR_OF_DAY));
                endPeriod.set(Calendar.MINUTE, intervalBase.get(Calendar.MINUTE));
                break;

        }
        return new Date[]{startPeriod.getTime(), endPeriod.getTime()};
    }

    protected String formatIntervalLabel() {
        String intervalText;
        @SuppressLint("SimpleDateFormat") SimpleDateFormat yearFormatter =
                new SimpleDateFormat("yyyy");
        @SuppressLint("SimpleDateFormat") SimpleDateFormat monthFormatter =
                new SimpleDateFormat("MMMM");
        @SuppressLint("SimpleDateFormat") SimpleDateFormat dayOfMonthFormatter =
                new SimpleDateFormat("d. ");
        @SuppressLint("SimpleDateFormat") SimpleDateFormat hourOfDayFormatter =
                new SimpleDateFormat("HH");
        @SuppressLint("SimpleDateFormat") SimpleDateFormat minuteFormatter =
                new SimpleDateFormat("mm");
        switch (mDatabaseActivity.getActualSortingLevel()) {
            case Calendar.YEAR:
                intervalText = getString(R.string.fragment_base_visualisation_interval_year);
                break;

            case Calendar.MONTH:
                intervalText = getResources().getStringArray(R.array.full_months)[0] + " - " +
                        getResources().getStringArray(R.array.full_months)[11] + " " +
                        yearFormatter.format(mDatabaseActivity.getActualSortingInterval().getTime());
                break;

            case Calendar.DAY_OF_MONTH:
                intervalText = getResources()
                        .getStringArray(R.array.full_months)[mDatabaseActivity
                        .getActualSortingInterval().get(Calendar.MONTH)] + " " +
                        yearFormatter.format(mDatabaseActivity.getActualSortingInterval().getTime());
                break;

            case Calendar.HOUR_OF_DAY:
                intervalText = dayOfMonthFormatter.format(mDatabaseActivity
                        .getActualSortingInterval().getTime()) +
                        monthFormatter.format(mDatabaseActivity.getActualSortingInterval().getTime()) +
                        " " + yearFormatter.format(mDatabaseActivity
                        .getActualSortingInterval().getTime());
                break;

            case Calendar.MINUTE:
                intervalText = dayOfMonthFormatter.format(mDatabaseActivity
                        .getActualSortingInterval().getTime()) +
                        monthFormatter.format(mDatabaseActivity
                                .getActualSortingInterval().getTime()) +
                        " " + yearFormatter.format(mDatabaseActivity
                        .getActualSortingInterval().getTime()) +
                        " " + hourOfDayFormatter.format(mDatabaseActivity
                        .getActualSortingInterval().getTime()) +
                        ":00 - " + hourOfDayFormatter.format(mDatabaseActivity
                        .getActualSortingInterval().getTime()) + ":59";
                break;

            case Calendar.SECOND:
                intervalText = dayOfMonthFormatter.format(mDatabaseActivity
                        .getActualSortingInterval().getTime()) +
                        monthFormatter.format(mDatabaseActivity
                                .getActualSortingInterval().getTime()) +
                        " " + yearFormatter.format(mDatabaseActivity
                        .getActualSortingInterval().getTime()) +
                        " " + hourOfDayFormatter.format(mDatabaseActivity
                        .getActualSortingInterval().getTime()) +
                        ":" + minuteFormatter.format(mDatabaseActivity
                        .getActualSortingInterval().getTime()) + ":00 - 59";
                break;

            default:
                throw new IllegalStateException(
                        "Unexpected value of mDatabaseActivity.getActualSortingLevel(): " +
                                mDatabaseActivity.getActualSortingLevel());
        }
        return intervalText;
    }

    protected void decrementSorting() {
        switch (mDatabaseActivity.getActualSortingLevel()) {
            case Calendar.YEAR:
            case Calendar.MONTH:
                mDatabaseActivity.setActualSortingLevel(Calendar.YEAR);
                break;

            case Calendar.DAY_OF_MONTH:
                mDatabaseActivity.setActualSortingLevel(Calendar.MONTH);
                break;

            case Calendar.HOUR_OF_DAY:
                mDatabaseActivity.setActualSortingLevel(Calendar.DAY_OF_MONTH);
                break;

            case Calendar.MINUTE:
                mDatabaseActivity.setActualSortingLevel(Calendar.HOUR_OF_DAY);
                break;

            case Calendar.SECOND:
                mDatabaseActivity.setActualSortingLevel(Calendar.MINUTE);
                break;

            default:
                throw new IllegalStateException(
                        "Unexpected value of mDatabaseActivity.getActualSortingLevel(): " +
                                mDatabaseActivity.getActualSortingLevel());
        }
    }

    @SuppressLint("NonConstantResourceId")
    private void updateValueTextView(final TextView txvValue, Float value, String patern) {
        switch (mDatabaseActivity.getActualSelectedSensor()) {
            case R.id.pedometer:
                txvValue.setText(RecordValueFormatter.formatSteps(patern, value));
                break;

            case R.id.heart_rate:
                txvValue.setText(RecordValueFormatter.formatHearRate(patern, value));
                break;

            case R.id.temperature:
                txvValue.setText(RecordValueFormatter.formatTemperature(patern, value));
                break;

            default:
                throw new IllegalStateException(
                        "Unexpected value of mDatabaseActivity.getActualSelectedSensor(): " +
                                mDatabaseActivity.getActualSelectedSensor());
        }
    }

    protected void updateValueAndTimestampTextviewPair(final TextView txvValue,
                                                       final TextView txvTimestamp, Float value,
                                                       Date date, String patern, boolean blink) {
        mDatabaseActivity.runOnUiThread(() -> {
            updateValueTextView(txvValue, value, patern);
            txvTimestamp.setText(RecordValueFormatter.formatTimeStampFull(date));
            if (blink) {
                txvValue.setTextColor(getContext().getColor(android.R.color.holo_red_dark));
                txvTimestamp.setTextColor(getContext().getColor(android.R.color.holo_red_dark));
                new Handler().postDelayed(() -> {
                    txvValue.setTextColor(getContext().getColor(R.color.ColorVsb));
                    txvTimestamp.setTextColor(getContext().getColor(R.color.ColorFei));
                }, BLINK_TIME_MS);
            }
        });
    }

    @SuppressLint("SetTextI18n")
    protected void dataAvailable(boolean available) {
        mDatabaseActivity.runOnUiThread(() -> {
            if (available) {
                mImbShowStatisticsInfo.setVisibility(View.VISIBLE);
                mTxvNoDataAvailableLabel.setVisibility(View.INVISIBLE);
                mTxvNoDataAvailableInterval.setVisibility(View.INVISIBLE);
                mBtnChangeInterval.setVisibility(View.INVISIBLE);
            } else {
                mImbShowStatisticsInfo.setVisibility(View.INVISIBLE);
                mTxvNoDataAvailableLabel.setVisibility(View.VISIBLE);
                mTxvNoDataAvailableInterval.setVisibility(View.VISIBLE);
                mBtnChangeInterval.setVisibility(View.VISIBLE);
                mTxvActualInterval.setText(formatIntervalLabel());
                mTxvNoDataAvailableInterval.setText(
                        getString(R.string.fragment_base_visualisation_no_data_for_interval_1_2) +
                                " \"" + formatIntervalLabel() + "\" " +
                                getString(R.string.fragment_base_visualisation_no_data_for_interval_2_2));
            }
            dataLoading(false);
        });
    }

    protected void dataLoading(boolean loading) {
        mDatabaseActivity.runOnUiThread(() -> {
            if (loading) {
                mPgbWorking.setVisibility(View.VISIBLE);
            } else {
                mPgbWorking.setVisibility(View.INVISIBLE);
            }
        });
    }

    protected List<List<Record>> sortRecordsByIntervals(final List<Record> records,
                                                        Calendar intervalsBase, int intervalsScale) {
        List<List<Record>> sortedRecordsByIntervals;
        int currentInterval;
        int intervalOffset;
        int yearOffset;
        switch (intervalsScale) {
            case Calendar.YEAR:
                Calendar firstYear = Calendar.getInstance();
                firstYear.setTimeInMillis(mTestbedDatabase.getFirstRecord(records)
                        .getTimeStamp().getTime());
                Calendar lastYear = Calendar.getInstance();
                lastYear.setTimeInMillis(mTestbedDatabase.getLastRecord(records)
                        .getTimeStamp().getTime());
                sortedRecordsByIntervals = new ArrayList(Collections.nCopies(
                        lastYear.get(Calendar.YEAR) - firstYear.get(Calendar.YEAR) + 1,
                        null));
                intervalOffset = 0;
                yearOffset = firstYear.get(Calendar.YEAR);
                break;

            case Calendar.MONTH:
                sortedRecordsByIntervals = new ArrayList(Collections.nCopies(
                        intervalsBase.getActualMaximum(intervalsScale) + 2,
                        null));
                intervalOffset = 1;
                yearOffset = 0;
                break;

            case Calendar.WEEK_OF_YEAR:
            case Calendar.DAY_OF_MONTH:
            case Calendar.HOUR_OF_DAY:
            case Calendar.MINUTE:
            case Calendar.SECOND:
                sortedRecordsByIntervals = new ArrayList(Collections.nCopies(
                        intervalsBase.getActualMaximum(intervalsScale) + 1,
                        null));
                intervalOffset = 0;
                yearOffset = 0;
                break;

            default:
                throw new IllegalStateException(
                        "Unexpected value of intervalsScale: " + intervalsScale);
        }
        Calendar calendar = Calendar.getInstance();
        for (Record record : records) {
            calendar.setTimeInMillis(record.getTimeStamp().getTime());
            currentInterval = calendar.get(intervalsScale) + intervalOffset - yearOffset;
            if (sortedRecordsByIntervals.get(currentInterval) == null) {
                sortedRecordsByIntervals.set(currentInterval, new ArrayList<>());
            }
            sortedRecordsByIntervals.get(currentInterval).add(record);
        }
        return sortedRecordsByIntervals;
    }

    protected List<Record> sortSteps(Record lastValueBeforeInterval, List<Record> records) {
        List<Record> sortedSteps = new ArrayList<>();
        float steps;
        Record lastRecord = lastValueBeforeInterval;
        Record newRecord;
        for (Record record : records) {
            if (lastRecord.getValue() < record.getValue()) {
                steps = record.getValue() - lastRecord.getValue();
            } else {
                steps = record.getValue();
            }
            lastRecord = record;
            newRecord = record.clone();
            newRecord.setValue(steps);
            sortedSteps.add(newRecord);
        }
        return sortedSteps;
    }

    protected List<List<Record>> sortStepsByIntervals(final Record lastValueBeforeInterval,
                                                      final List<List<Record>> sortedRecordsByInterval) {
        @SuppressWarnings("unchecked") List<List<Record>> sortedStepsByIntervals = new ArrayList(Collections.nCopies(
                sortedRecordsByInterval.size(),
                null));
        float lastStepValue = lastValueBeforeInterval.getValue();
        float steps;
        for (int i = 0; i < sortedRecordsByInterval.size(); i++) {
            if (sortedRecordsByInterval.get(i) != null) {
                List<Record> sortedStepsByInterval = new ArrayList<>();
                for (Record record : sortedRecordsByInterval.get(i)) {
                    if (lastStepValue < record.getValue()) {
                        steps = record.getValue() - lastStepValue;
                    } else {
                        steps = record.getValue();
                    }
                    lastStepValue = record.getValue();
                    Record newRecord = record.clone();
                    newRecord.setValue(steps);
                    sortedStepsByInterval.add(newRecord);
                }
                sortedStepsByIntervals.set(i, sortedStepsByInterval);
            }
        }
        return sortedStepsByIntervals;
    }

    protected List<StatisticalData> getStatisticalDataByIntervals(
            final List<List<Record>> recordsByIntervals) {
        List<StatisticalData> statisticalDataByIntervals = new ArrayList(Collections.nCopies(
                recordsByIntervals.size(),
                null));
        for (int i = 0; i < recordsByIntervals.size(); i++) {
            if (recordsByIntervals.get(i) != null) {
                statisticalDataByIntervals.set(i, new StatisticalData(recordsByIntervals.get(i)));
            }
        }
        return statisticalDataByIntervals;
    }

    private int[] getFirstAndLastYear(String recordKey) {
        try {
            int[] firstAndLastYear = new int[2];
            Calendar calendar = Calendar.getInstance();

            Record record = mTestbedDatabase.selectFirstRecord(mTestbedDevice, recordKey);
            calendar.setTime(record.getTimeStamp());
            firstAndLastYear[0] = calendar.get(Calendar.YEAR);

            record = mTestbedDatabase.selectLastRecord(mTestbedDevice, recordKey);
            calendar.setTime(record.getTimeStamp());
            firstAndLastYear[1] = calendar.get(Calendar.YEAR);
            return firstAndLastYear;
        } catch (TestbedDatabase.EmptyCursorException e) {
            Calendar calendar = Calendar.getInstance();
            calendar.get(Calendar.YEAR);
            return new int[]{calendar.get(Calendar.YEAR), calendar.get(Calendar.YEAR)};
        }
    }

    @SuppressLint("NonConstantResourceId")
    protected void showChangeIntervalDialog(
            final OnChangeActualIntervaListener onChangeIntervaDialogClickListener) {
        LayoutInflater layoutInflater = getLayoutInflater();
        View changeIntervalDialogView = layoutInflater
                .inflate(R.layout.dialog_change_interval, null);

        final CheckBox mChbAllRecords = changeIntervalDialogView
                .findViewById(R.id.dialog_change_interval_chb_all_records);
        final LinearLayout linearLayout = changeIntervalDialogView
                .findViewById(R.id.dialog_change_interval_lnl_labels_and_pickers);
        final SeekBar mSkbInterval = changeIntervalDialogView
                .findViewById(R.id.dialog_change_interval_skb_interval);
        final NumberPicker mNupYear = changeIntervalDialogView
                .findViewById(R.id.dialog_change_interval_nup_year);
        final NumberPicker mNupMonth = changeIntervalDialogView
                .findViewById(R.id.dialog_change_interval_nup_month);
        final NumberPicker mNupDayOfMonth = changeIntervalDialogView
                .findViewById(R.id.dialog_change_interval_nup_day_of_month);
        final NumberPicker mNupHourOfDay = changeIntervalDialogView
                .findViewById(R.id.dialog_change_interval_nup_hour_of_day);
        final NumberPicker mNupMinute = changeIntervalDialogView
                .findViewById(R.id.dialog_change_interval_nup_minute);
        NumberPicker[] numberPickers =
                new NumberPicker[]{mNupYear, mNupMonth, mNupDayOfMonth, mNupHourOfDay, mNupMinute};

        final ViewGroup.LayoutParams layoutParams = linearLayout.getLayoutParams();

        int[] firstAndLastYear;

        mChbAllRecords.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked) {
                linearLayout.setVisibility(View.INVISIBLE);
                layoutParams.height = 0;
            } else {
                linearLayout.setVisibility(View.VISIBLE);
                layoutParams.height = -1;

            }
            linearLayout.setLayoutParams(layoutParams);
        });

        switch (mDatabaseActivity.getActualSortingLevel()) {
            case Calendar.YEAR:
                mChbAllRecords.setChecked(true);
                break;

            case Calendar.MONTH:
                mSkbInterval.setProgress(0);
                break;

            case Calendar.DAY_OF_MONTH:
                mSkbInterval.setProgress(1);
                break;

            case Calendar.HOUR_OF_DAY:
                mSkbInterval.setProgress(2);
                break;

            case Calendar.MINUTE:
                mSkbInterval.setProgress(3);
                break;

            case Calendar.SECOND:
                mSkbInterval.setProgress(4);
                break;

            default:
                throw new IllegalStateException(
                        "Unexpected value of mDatabaseActivity.getActualSortingLevel(): " +
                                mDatabaseActivity.getActualSortingLevel());
        }

        for (int numberPickerIndex = 0; numberPickerIndex < numberPickers.length;
             numberPickerIndex++) {
            if (mSkbInterval.getProgress() >= numberPickerIndex) {
                numberPickers[numberPickerIndex].setVisibility(View.VISIBLE);
            } else {
                numberPickers[numberPickerIndex].setVisibility(View.INVISIBLE);
            }
        }

        mSkbInterval.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                for (int numberPickerIndex = 0; numberPickerIndex < numberPickers.length;
                     numberPickerIndex++) {
                    if (mSkbInterval.getProgress() >= numberPickerIndex) {
                        numberPickers[numberPickerIndex].setVisibility(View.VISIBLE);
                    } else {
                        numberPickers[numberPickerIndex].setVisibility(View.INVISIBLE);
                    }
                }
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

            }
        });

        switch (mDatabaseActivity.getActualSelectedSensor()) {
            case R.id.pedometer:
                firstAndLastYear = getFirstAndLastYear(BluetoothLeService.STEPS_DATA);
                break;

            case R.id.heart_rate:
                firstAndLastYear = getFirstAndLastYear(BluetoothLeService.HEART_RATE_DATA);
                break;

            case R.id.temperature:
                firstAndLastYear = getFirstAndLastYear(BluetoothLeService.TEMPERATURE_DATA);
                break;

            default:
                throw new IllegalStateException(
                        "Unexpected value of mDatabaseActivity.getActualSelectedSensor(): " +
                                mDatabaseActivity.getActualSelectedSensor());
        }

        mNupYear.setMinValue(firstAndLastYear[0]);
        mNupYear.setMaxValue(firstAndLastYear[1]);
        mNupYear.setValue(mDatabaseActivity.getActualSortingInterval().get(Calendar.YEAR));
        mNupYear.setOnValueChangedListener((picker, oldVal, newVal) -> {
            Calendar calendar = Calendar.getInstance(Locale.US);
            calendar.set(Calendar.YEAR, newVal);
            calendar.set(Calendar.MONTH, mNupMonth.getValue());
            mNupDayOfMonth.setMinValue(1);
            mNupDayOfMonth.setMaxValue(calendar.getActualMaximum(Calendar.DAY_OF_MONTH));
        });

        mNupMonth.setMinValue(0);
        mNupMonth.setMaxValue(11);
        mNupMonth.setDisplayedValues(getResources().getStringArray(R.array.full_months));
        mNupMonth.setValue(mDatabaseActivity.getActualSortingInterval().get(Calendar.MONTH));
        mNupMonth.setOnValueChangedListener((picker, oldVal, newVal) -> {
            Calendar calendar = Calendar.getInstance(Locale.US);
            calendar.set(Calendar.YEAR, mNupYear.getValue());
            calendar.set(Calendar.MONTH, newVal);
            mNupDayOfMonth.setMinValue(1);
            mNupDayOfMonth.setMaxValue(calendar.getActualMaximum(Calendar.DAY_OF_MONTH));
        });

        mNupDayOfMonth.setMinValue(1);
        mNupDayOfMonth.setMaxValue(mDatabaseActivity.getActualSortingInterval()
                .getActualMaximum(Calendar.DAY_OF_MONTH));
        mNupDayOfMonth.setValue(mDatabaseActivity.getActualSortingInterval()
                .get(Calendar.DAY_OF_MONTH));
        mNupHourOfDay.setMinValue(0);
        mNupHourOfDay.setMaxValue(23);
        mNupHourOfDay.setValue(mDatabaseActivity.getActualSortingInterval()
                .get(Calendar.HOUR_OF_DAY));
        mNupMinute.setMinValue(0);
        mNupMinute.setMaxValue(59);
        mNupMinute.setValue(mDatabaseActivity.getActualSortingInterval()
                .get(Calendar.MINUTE));

        AlertDialog changeActualIntervalDialog = new AlertDialog.Builder(getContext())
                .setIcon(AppCompatResources.getDrawable(getContext(),
                        R.drawable.ic_change_interval_20dp_color_vsb))
                .setTitle(getString(R.string.dialog_change_actual_interval_title))
                .setView(changeIntervalDialogView)
                .setPositiveButton(getString(R.string.dialog_change_actual_interval_positive_button),
                        (dialog, which) -> {
                            if (mChbAllRecords.isChecked()) {
                                mDatabaseActivity.setActualSortingLevel(Calendar.YEAR);
                            } else {
                                mDatabaseActivity.getActualSortingInterval()
                                        .set(Calendar.YEAR, mNupYear.getValue());
                                mDatabaseActivity.getActualSortingInterval()
                                        .set(Calendar.MONTH, mNupMonth.getValue());
                                mDatabaseActivity.getActualSortingInterval()
                                        .set(Calendar.DAY_OF_MONTH, mNupDayOfMonth.getValue());
                                mDatabaseActivity.getActualSortingInterval()
                                        .set(Calendar.HOUR_OF_DAY, mNupHourOfDay.getValue());
                                mDatabaseActivity.getActualSortingInterval()
                                        .set(Calendar.MINUTE, mNupMinute.getValue());
                                switch (mSkbInterval.getProgress()) {
                                    case 0:
                                        mDatabaseActivity.setActualSortingLevel(Calendar.MONTH);
                                        break;
                                    case 1:
                                        mDatabaseActivity
                                                .setActualSortingLevel(Calendar.DAY_OF_MONTH);
                                        break;
                                    case 2:
                                        mDatabaseActivity
                                                .setActualSortingLevel(Calendar.HOUR_OF_DAY);
                                        break;
                                    case 3:
                                        mDatabaseActivity
                                                .setActualSortingLevel(Calendar.MINUTE);
                                        break;
                                    case 4:
                                        mDatabaseActivity
                                                .setActualSortingLevel(Calendar.SECOND);
                                        break;
                                }
                            }
                            onChangeIntervaDialogClickListener.onChangeClick();

                        })
                .setNegativeButton(getString(R.string.dialog_change_actual_interval_negative_button),
                        null)
                .create();
        changeActualIntervalDialog.show();
    }

    @SuppressLint({"NonConstantResourceId", "InflateParams"})
    protected void showStatisticalDataDialog(Context context, StatisticalData statisticalData) {
        View staticticalDataDialogView;
        switch (mDatabaseActivity.getActualSelectedSensor()) {
            case R.id.pedometer:
                staticticalDataDialogView = getLayoutInflater()
                        .inflate(R.layout.dialog_dependent_statistical_data, null);
                mTxvDataSetSize = staticticalDataDialogView
                        .findViewById(R.id.dialog_dependent_statistical_data_txv_data_set_size_value);
                mTxvDataSetSum = staticticalDataDialogView
                        .findViewById(R.id.dialog_dependent_statistical_data_set_sum_value);
                break;

            case R.id.heart_rate:
            case R.id.temperature:
                staticticalDataDialogView = getLayoutInflater()
                        .inflate(R.layout.dialog_independent_statistical_data, null);
                mTxvDataSetSize = staticticalDataDialogView
                        .findViewById(R.id.dialog_independent_statistical_data_txv_data_set_size_value);
                mTxvMeanValue = staticticalDataDialogView
                        .findViewById(R.id.dialog_independent_statistical_data_txv_mean_value_value);
                mTxvStandardDeviation = staticticalDataDialogView
                        .findViewById(R.id.dialog_independent_statistical_data_txv_standard_deviation_value);

                mTxvMinValue = staticticalDataDialogView
                        .findViewById(R.id.dialog_independent_statistical_data_txv_min_value_value);
                mTxvMinValueTimestamp = staticticalDataDialogView
                        .findViewById(R.id.dialog_independent_statistical_data_txv_min_value_timestamp);
                mTxvMinValueRecordId = staticticalDataDialogView
                        .findViewById(R.id.dialog_independent_statistical_data_txv_min_value_record_id);

                mTxvMedian = staticticalDataDialogView
                        .findViewById(R.id.dialog_independent_statistical_data_txv_median_value);
                mTxvMedianTimestamp = staticticalDataDialogView
                        .findViewById(R.id.dialog_independent_statistical_data_txv_median_timestamp);
                mTxvMedianRecordId = staticticalDataDialogView
                        .findViewById(R.id.dialog_independent_statistical_data_txv_median_record_id);

                mTxvMaxValue = staticticalDataDialogView
                        .findViewById(R.id.dialog_independent_statistical_data_txv_max_value_value);
                mTxvMaxValueTimestamp = staticticalDataDialogView
                        .findViewById(R.id.dialog_independent_statistical_data_txv_max_value_timestamp);
                mTxvMaxValueRecordId = staticticalDataDialogView
                        .findViewById(R.id.dialog_independent_statistical_data_txv_max_value_record_id);
                break;

            default:
                throw new IllegalStateException(
                        "Unexpected value of mDatabaseActivity.getActualSelectedSensor(): " +
                                mDatabaseActivity.getActualSelectedSensor());
        }

        mStatisticalDataDialog = new AlertDialog.Builder(getContext())
                .setIcon(AppCompatResources.getDrawable(getContext(),
                        R.drawable.ic_statistical_data_20dp_color_vsb))
                .setTitle(getString(R.string.dialog_statistical_data_title))
                .setView(staticticalDataDialogView)
                .setNeutralButton(getString(R.string.dialog_statistical_data_neutral_button),
                        null)
                .setOnDismissListener(dialog -> mStatisticalDataDialogVisible = false)
                .create();

        mStatisticalDataDialogVisible = true;
        updateStatisticalDataDialog(statisticalData);
    }

    @SuppressLint({"NonConstantResourceId", "SetTextI18n"})
    public void updateStatisticalDataDialog(StatisticalData statisticalData) {
        if (mStatisticalDataDialogVisible) {
            DecimalFormat dataSetSizeFormat = new DecimalFormat("###,###,###");
            DecimalFormat standardDeviationFormat = new DecimalFormat("##0.00");

            mTxvDataSetSize.setText(dataSetSizeFormat.format(statisticalData.getDataSetSize()));

            switch (mDatabaseActivity.getActualSelectedSensor()) {
                case R.id.pedometer:
                    updateValueTextView(mTxvDataSetSum, statisticalData.getDataSetSum(),
                            RecordValueFormatter.PATERN_6_0);
                    break;

                case R.id.heart_rate:
                    updateValueTextView(mTxvMeanValue, statisticalData.getMeanValue(),
                            RecordValueFormatter.PATERN_3_2);
                    mTxvStandardDeviation.setText(standardDeviationFormat
                            .format(statisticalData.getStandardDeviation()));

                    updateValueAndTimestampTextviewPair(mTxvMinValue, mTxvMinValueTimestamp,
                            statisticalData.getMinValue().getValue(),
                            statisticalData.getMinValue().getTimeStamp(),
                            RecordValueFormatter.PATERN_3_0, false);
                    mTxvMinValueRecordId.setText(
                            getString(R.string.dialog_statistical_data_record_id_label) +
                                    " " + statisticalData.getMinValue().getDataId());

                    updateValueAndTimestampTextviewPair(mTxvMedian, mTxvMedianTimestamp,
                            statisticalData.getMedian().getValue(),
                            statisticalData.getMedian().getTimeStamp(),
                            RecordValueFormatter.PATERN_3_0, false);
                    mTxvMedianRecordId.setText(
                            getString(R.string.dialog_statistical_data_record_id_label) +
                                    " " + statisticalData.getMinValue().getDataId());

                    updateValueAndTimestampTextviewPair(mTxvMaxValue, mTxvMaxValueTimestamp,
                            statisticalData.getMaxValue().getValue(),
                            statisticalData.getMaxValue().getTimeStamp(),
                            RecordValueFormatter.PATERN_3_0, false);
                    mTxvMaxValueRecordId.setText(
                            getString(R.string.dialog_statistical_data_record_id_label) +
                                    " " + statisticalData.getMinValue().getDataId());
                    break;

                case R.id.temperature:
                    updateValueTextView(mTxvMeanValue, statisticalData.getMeanValue(),
                            RecordValueFormatter.PATERN_3_2);
                    mTxvStandardDeviation.setText(standardDeviationFormat
                            .format(statisticalData.getStandardDeviation()));

                    updateValueAndTimestampTextviewPair(mTxvMinValue, mTxvMinValueTimestamp,
                            statisticalData.getMinValue().getValue(),
                            statisticalData.getMinValue().getTimeStamp(),
                            RecordValueFormatter.PATERN_3_2, false);
                    mTxvMinValueRecordId.setText(
                            getString(R.string.dialog_statistical_data_record_id_label) +
                                    " " + statisticalData.getMinValue().getDataId());

                    updateValueAndTimestampTextviewPair(mTxvMedian, mTxvMedianTimestamp,
                            statisticalData.getMedian().getValue(),
                            statisticalData.getMedian().getTimeStamp(),
                            RecordValueFormatter.PATERN_3_2, false);
                    mTxvMedianRecordId.setText(
                            getString(R.string.dialog_statistical_data_record_id_label) +
                                    " " + statisticalData.getMinValue().getDataId());

                    updateValueAndTimestampTextviewPair(mTxvMaxValue, mTxvMaxValueTimestamp,
                            statisticalData.getMaxValue().getValue(),
                            statisticalData.getMaxValue().getTimeStamp(),
                            RecordValueFormatter.PATERN_3_2, false);
                    mTxvMaxValueRecordId.setText(
                            getString(R.string.dialog_statistical_data_record_id_label) +
                                    " " + statisticalData.getMinValue().getDataId());
                    break;

                default:
                    throw new IllegalStateException(
                            "Unexpected value of mDatabaseActivity.getActualSelectedSensor(): " +
                                    mDatabaseActivity.getActualSelectedSensor());
            }
            mStatisticalDataDialog.show();
        }
    }

    protected interface OnChangeActualIntervaListener {
        void onChangeClick();
    }
}