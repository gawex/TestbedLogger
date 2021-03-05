/*
  @author  Bc. Lukas Tatarin
 * @supervisor Ing. Jaromir Konecny, Ph.D.
 * @email   lukas@tatarin.cz
 * @version 1.10
 * @ide     Android Studio 4.1.2
 * @license GNU GPL v3
 * @brief   ChartFragment.java
 * @lastmodify 2021/03/05 11:55:37
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
import android.graphics.Color;
import android.graphics.Paint;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.Toast;

import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.BarData;
import com.github.mikephil.charting.data.BarDataSet;
import com.github.mikephil.charting.data.BarEntry;
import com.github.mikephil.charting.data.CandleData;
import com.github.mikephil.charting.data.CandleDataSet;
import com.github.mikephil.charting.data.CandleEntry;
import com.github.mikephil.charting.data.CombinedData;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.highlight.Highlight;
import com.github.mikephil.charting.listener.OnChartValueSelectedListener;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

import cz.vsb.cbe.testbed.R;
import cz.vsb.cbe.testbed.activitiesAndServices.BluetoothLeService;
import cz.vsb.cbe.testbed.activitiesAndServices.DatabaseActivity;
import cz.vsb.cbe.testbed.chart.MyCombinedChart;
import cz.vsb.cbe.testbed.chart.MyMarkerView;
import cz.vsb.cbe.testbed.sql.DatabaseResult;
import cz.vsb.cbe.testbed.sql.Record;
import cz.vsb.cbe.testbed.sql.TestbedDatabase;
import cz.vsb.cbe.testbed.utils.IntervalValueFormatter;
import cz.vsb.cbe.testbed.utils.RecordValueFormatter;
import cz.vsb.cbe.testbed.utils.StatisticalData;

@SuppressWarnings("CatchMayIgnoreException")
public class ChartFragment extends BaseVisualisationFragment {

    private static final int CHART_ANIMATION_TIME_MS = 500;

    private static final float PEDOMETER_MIN_VALUE = 0;
    private static final float HEART_RATE_SENSOR_MIN_VALUE = 0;
    private static final float HEART_RATE_SENSOR_MAX_VALUE = 255;
    private static final float TEMPERATURE_SENSOR_MIN_VALUE = -55;
    private static final float TEMPERATURE_SENSOR_MAX_VALUE = 125;

    private static final float BAR_AND_CANDLE_WIDTH = 0.9f;
    private static final float BAR_AND_CANDLE_SPACE = 1 - BAR_AND_CANDLE_WIDTH;

    private MyCombinedChart mChrChart;
    private MyMarkerView mMyMarkerView;

    private CheckBox mChbSumAndRangeValues;
    private CheckBox mChbMeanValues;

    private boolean mSumAndRangeVisible = true;
    private boolean mMeanVisible = true;

    private BarData mBarData;
    private CandleData mCandleData;
    private LineData mLineData;

    private final OnChangeActualIntervaListener mOnChangeActualIntervaListener = () ->
            updateChartData(true);

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
        View view = inflater.inflate(R.layout.fragment_chart_view, container, false);

        mImbDecrementActualInterval = view
                .findViewById(R.id.fragment_chart_view_imb_decrement_sorting);
        mImbDecrementActualInterval.setOnClickListener(v -> {
            decrementSorting();
            updateChartData(true);
        });
        mTxvActualInterval = view.findViewById(R.id.fragment_chart_view_txv_actual_interval);
        mImbChangeInterval = view.findViewById(R.id.fragment_chart_view_imb_change_actual_interval);
        mImbChangeInterval.setOnClickListener(v -> {
            mDatabaseActivity.getActualSortingInterval().setTime(new Date());
            mDatabaseActivity.setActualSortingLevel(Calendar.HOUR_OF_DAY);
            updateChartData(true);
        });
        mImbChangeInterval.setOnLongClickListener(v -> {
            showChangeIntervalDialog(mOnChangeActualIntervaListener);
            return true;
        });

        mChrChart = view.findViewById(R.id.fragment_chart_view_chr);
        setUpChart(mChrChart);
        setUpYAxisRight(mChrChart);
        mChrChart.getLegend().setEnabled(false);
        mChrChart.setOnChartValueSelectedListener(new OnChartValueSelectedListener() {
            @Override
            public void onValueSelected(Entry e, Highlight h) {
                mChrChart.setMarker(mMyMarkerView);
            }

            @Override
            public void onNothingSelected() {
                mChrChart.setMarker(null);
            }
        });
        mChrChart.setOnChartValueLongClickListener(e -> {
            mChrChart.setMarker(null);
            switch (mDatabaseActivity.getActualSortingLevel()) {
                case Calendar.YEAR:
                    mDatabaseActivity.getActualSortingInterval()
                            .set(Calendar.YEAR, (int) e.getX());
                    mDatabaseActivity.setActualSortingLevel(Calendar.MONTH);
                    updateChartData(true);
                    break;
                case Calendar.MONTH:
                    mDatabaseActivity.getActualSortingInterval()
                            .set(Calendar.MONTH, (int) e.getX() - 1);
                    mDatabaseActivity.setActualSortingLevel(Calendar.DAY_OF_MONTH);
                    updateChartData(true);
                    break;
                case Calendar.DAY_OF_MONTH:
                    mDatabaseActivity.getActualSortingInterval()
                            .set(Calendar.DAY_OF_MONTH, (int) e.getX());
                    mDatabaseActivity.setActualSortingLevel(Calendar.HOUR_OF_DAY);
                    updateChartData(true);
                    break;
                case Calendar.HOUR_OF_DAY:
                    mDatabaseActivity.getActualSortingInterval()
                            .set(Calendar.HOUR_OF_DAY, (int) e.getX());
                    mDatabaseActivity.setActualSortingLevel(Calendar.MINUTE);
                    updateChartData(true);
                    break;
                case Calendar.MINUTE:
                    mDatabaseActivity.getActualSortingInterval()
                            .set(Calendar.MINUTE, (int) e.getX());
                    mDatabaseActivity.setActualSortingLevel(Calendar.SECOND);
                    updateChartData(true);
                    break;

                case Calendar.SECOND:
                    Toast.makeText(getContext(), getString(
                            R.string.fragment_chart_max_interval_scale),
                            Toast.LENGTH_SHORT).show();
                    break;
                default:
                    throw new IllegalStateException(
                            "Unexpected value of mDatabaseActivity.getActualSortingLevel(): " +
                                    mDatabaseActivity.getActualSortingLevel());
            }
        });

        mPgbWorking = view.findViewById(R.id.fragment_chart_view_pgb_working);

        mTxvNoDataAvailableLabel = view
                .findViewById(R.id.fragment_chart_view_txv_no_data_available_label);
        mTxvNoDataAvailableInterval = view
                .findViewById(R.id.fragment_chart_view_txv_no_data_available_interval);
        mBtnChangeInterval = view
                .findViewById(R.id.fragment_chart_view_btn_change_interval);
        mBtnChangeInterval.setOnClickListener(
                v -> showChangeIntervalDialog(mOnChangeActualIntervaListener));

        mChbSumAndRangeValues = view
                .findViewById(R.id.fragment_chart_view_chb_sum_and_candle_values);
        mChbSumAndRangeValues.setChecked(true);
        mChbSumAndRangeValues.setOnCheckedChangeListener(
                (compoundButton, b) -> {
                    mSumAndRangeVisible = b;
                    renderChart(false);
                });

        mChbMeanValues = view.findViewById(R.id.fragment_chart_view_chb_mean_values);
        mChbMeanValues.setChecked(true);
        mChbMeanValues.setOnCheckedChangeListener((compoundButton, b) -> {
            mMeanVisible = b;
            renderChart(false);
        });

        mImbShowStatisticsInfo = view
                .findViewById(R.id.fragment_chart_view_imb_show_statistical_data);
        mImbShowStatisticsInfo.setOnClickListener(
                v -> showStatisticalDataDialog(getContext(), new StatisticalData(mRecords)));

        return view;
    }

    @Override
    public void onResume() {
        super.onResume();
        updateChartData(true);
    }

    @SuppressWarnings("unchecked")
    @SuppressLint("NonConstantResourceId")
    public void updateChartData(boolean animation) {
        Calendar actualSortingInterval = mDatabaseActivity.getActualSortingInterval();
        int actualSortingLevel = mDatabaseActivity.getActualSortingLevel();
        Calendar calendar = Calendar.getInstance();
        int[] years = new int[2];
        switch (mDatabaseActivity.getActualSelectedSensor()) {
            case R.id.pedometer:
                dataLoading(true);
                Date[] intervals = getIntervals(actualSortingInterval, actualSortingLevel);
                mTestbedDatabase.selectRecordsBetweenTimeStamp(
                        mTestbedDevice,
                        BluetoothLeService.STEPS_DATA,
                        intervals,
                        TestbedDatabase.SORT_BY.DEFAULT, TestbedDatabase.SORT_ORDER.DEFAULT,
                        databaseResult -> {
                            if (databaseResult instanceof DatabaseResult.Success) {
                                List<Record> selectedRecords =
                                        ((DatabaseResult.Success<List<Record>>) databaseResult).data;
                                mTestbedDatabase.selectFirstRecordLessThanTimeStamp(mTestbedDevice,
                                        BluetoothLeService.STEPS_DATA, intervals[0],
                                        databaseResult1 -> {
                                            Record lastRecordBeforeInterval = null;
                                            if (databaseResult1 instanceof DatabaseResult.Success) {
                                                lastRecordBeforeInterval =
                                                        ((DatabaseResult.Success<Record>) databaseResult1).data;
                                            } else if (databaseResult1 instanceof DatabaseResult.Error) {
                                                lastRecordBeforeInterval = new Record();
                                            }
                                            mRecords = sortSteps(lastRecordBeforeInterval, selectedRecords);
                                            calendar.setTime(mTestbedDatabase.getFirstRecord(selectedRecords)
                                                    .getTimeStamp());
                                            years[0] = calendar.get(Calendar.YEAR);
                                            calendar.setTime(mTestbedDatabase.getLastRecord(selectedRecords)
                                                    .getTimeStamp());
                                            years[1] = calendar.get(Calendar.YEAR);

                                            assert lastRecordBeforeInterval != null;
                                            setPedometerData(
                                                    getStatisticalDataByIntervals(
                                                            sortStepsByIntervals(
                                                                    lastRecordBeforeInterval,
                                                                    sortRecordsByIntervals(
                                                                            selectedRecords,
                                                                            actualSortingInterval,
                                                                            actualSortingLevel))),
                                                    years, animation);

                                            dataAvailable(true);
                                        });
                            } else if (databaseResult instanceof DatabaseResult.Error) {
                                dataAvailable(false);
                            }
                        });
                break;

            case R.id.heart_rate:
                dataLoading(true);
                mTestbedDatabase.selectRecordsBetweenTimeStamp(
                        mTestbedDevice,
                        BluetoothLeService.HEART_RATE_DATA,
                        getIntervals(actualSortingInterval, actualSortingLevel),
                        TestbedDatabase.SORT_BY.DEFAULT, TestbedDatabase.SORT_ORDER.DEFAULT,
                        databaseResult -> {
                            if (databaseResult instanceof DatabaseResult.Success) {
                                mRecords = ((DatabaseResult.Success<List<Record>>) databaseResult).data;
                                calendar.setTime(mTestbedDatabase.getFirstRecord(mRecords).getTimeStamp());
                                years[0] = calendar.get(Calendar.YEAR);
                                calendar.setTime(mTestbedDatabase.getLastRecord(mRecords).getTimeStamp());
                                years[1] = calendar.get(Calendar.YEAR);

                                setHeartRateData(getStatisticalDataByIntervals(
                                        sortRecordsByIntervals(mRecords, actualSortingInterval,
                                                actualSortingLevel)), years, animation);
                                dataAvailable(true);
                            } else if (databaseResult instanceof DatabaseResult.Error) {
                                dataAvailable(false);
                            }
                        });
                break;

            case R.id.temperature:
                dataLoading(true);
                mTestbedDatabase.selectRecordsBetweenTimeStamp(
                        mTestbedDevice,
                        BluetoothLeService.TEMPERATURE_DATA,
                        getIntervals(actualSortingInterval, actualSortingLevel),
                        TestbedDatabase.SORT_BY.DEFAULT, TestbedDatabase.SORT_ORDER.DEFAULT,
                        databaseResult -> {
                            if (databaseResult instanceof DatabaseResult.Success) {
                                mRecords = ((DatabaseResult.Success<List<Record>>) databaseResult).data;
                                calendar.setTime(mTestbedDatabase.getFirstRecord(mRecords).getTimeStamp());
                                years[0] = calendar.get(Calendar.YEAR);
                                calendar.setTime(mTestbedDatabase.getLastRecord(mRecords).getTimeStamp());
                                years[1] = calendar.get(Calendar.YEAR);

                                setTemperatureData(getStatisticalDataByIntervals(
                                        sortRecordsByIntervals(mRecords, actualSortingInterval,
                                                actualSortingLevel)), years, animation);
                                dataAvailable(true);
                            } else if (databaseResult instanceof DatabaseResult.Error) {
                                dataAvailable(false);
                            }
                        });
                break;
            default:
                throw new IllegalStateException(
                        "Unexpected value of  mDatabaseActivity.getActualSelectedSensor(): " +
                                mDatabaseActivity.getActualSelectedSensor());
        }
    }

    @SuppressLint("NonConstantResourceId")
    @Override
    public void dataAvailable(boolean available) {
        super.dataAvailable(available);
        mDatabaseActivity.runOnUiThread(() -> {
            if (available) {
                mChrChart.setVisibility(View.VISIBLE);
                mChbSumAndRangeValues.setVisibility(View.VISIBLE);
                switch (mDatabaseActivity.getActualSelectedSensor()) {
                    case R.id.pedometer:
                        mChbSumAndRangeValues.setText(
                                getString(R.string.fragment_chart_steps_ranges_label));
                        mChbMeanValues.setVisibility(View.INVISIBLE);
                        break;

                    case R.id.heart_rate:
                        mChbSumAndRangeValues.setText(
                                getString(R.string.fragment_chart_heart_rates_ranges_label));
                        mChbMeanValues.setVisibility(View.VISIBLE);
                        mChbMeanValues.setText(
                                getString(R.string.fragment_chart_heart_rates_means_label));
                        break;

                    case R.id.temperature:
                        mChbSumAndRangeValues.setText(
                                getString(R.string.fragment_chart_temperatures_ranges_label));
                        mChbMeanValues.setVisibility(View.VISIBLE);
                        mChbMeanValues.setText(
                                getString(R.string.fragment_chart_temperatures_means_label));
                        break;

                    default:
                        throw new IllegalStateException(
                                "Unexpected value of  mDatabaseActivity.getActualSelectedSensor(): " +
                                        mDatabaseActivity.getActualSelectedSensor());
                }
            } else {
                mChrChart.setVisibility(View.INVISIBLE);
                mChbSumAndRangeValues.setVisibility(View.INVISIBLE);
                mChbMeanValues.setVisibility(View.INVISIBLE);
            }
        });
    }

    public void setPedometerData(List<StatisticalData> statisticalDataByInterval, int[] years,
                                 boolean animation) {
        mDatabaseActivity.runOnUiThread(() -> {
            ArrayList<BarEntry> maxFloatValues = new ArrayList<>();
            int firstEntry = setUpXAxis(mChrChart, years);
            float maximumChartValue = PEDOMETER_MIN_VALUE;
            mTxvActualInterval.setText(formatIntervalLabel());
            for (int i = 0; i < statisticalDataByInterval.size(); i++) {
                if (statisticalDataByInterval.get(i) != null) {
                    try {
                        maxFloatValues.add(new BarEntry(
                                firstEntry + i,
                                statisticalDataByInterval.get(i).getDataSetSum()));
                        if (statisticalDataByInterval.get(i).getDataSetSum() > maximumChartValue) {
                            maximumChartValue = statisticalDataByInterval.get(i).getDataSetSum();
                        }
                    } catch (NullPointerException e) {

                    }
                }
            }
            setUpYAxisLeft(mChrChart, new float[]{0, maximumChartValue * 1.1f});

            BarDataSet barDataSet = new BarDataSet(maxFloatValues, null);
            setUpBarDataSet(barDataSet);

            mBarData = new BarData(barDataSet);
            mBarData.setBarWidth(BAR_AND_CANDLE_WIDTH);

            mLineData = null;

            mMyMarkerView = new MyMarkerView(getContext(), mDatabaseActivity.getActualSortingLevel(),
                    mDatabaseActivity.getActualSelectedSensor());
            mMyMarkerView.setChartView(mChrChart);
            mChrChart.setMarker(mMyMarkerView);
            renderChart(animation);
        });
    }

    public void setHeartRateData(List<StatisticalData> statisticalDataByInterval, int[] years,
                                 boolean animation) {
        mDatabaseActivity.runOnUiThread(() -> {
            ArrayList<CandleEntry> minMaxAndQuartileFloatValues = new ArrayList<>();
            ArrayList<Entry> meanFloatValues = new ArrayList<>();
            int firstEntry = setUpXAxis(mChrChart, years);
            float minimumChartValue = HEART_RATE_SENSOR_MAX_VALUE;
            float maximumChartValue = HEART_RATE_SENSOR_MIN_VALUE;
            mTxvActualInterval.setText(formatIntervalLabel());
            for (int i = 0; i < statisticalDataByInterval.size(); i++) {
                if (statisticalDataByInterval.get(i) != null) {
                    try {
                        minMaxAndQuartileFloatValues.add(new CandleEntry(
                                firstEntry + i,
                                statisticalDataByInterval.get(i).getMaxValue().getValue(),
                                statisticalDataByInterval.get(i).getMinValue().getValue(),
                                statisticalDataByInterval.get(i).getThirdQuartile().getValue(),
                                statisticalDataByInterval.get(i).getFirstQuartile().getValue()));
                        meanFloatValues.add(new Entry(
                                firstEntry + i,
                                statisticalDataByInterval.get(i).getMeanValue()));
                        if (statisticalDataByInterval.get(i).getMinValue().getValue() <
                                minimumChartValue) {
                            minimumChartValue =
                                    statisticalDataByInterval.get(i).getMinValue().getValue();
                        }
                        if (statisticalDataByInterval.get(i).getMaxValue().getValue() >
                                maximumChartValue) {
                            maximumChartValue =
                                    statisticalDataByInterval.get(i).getMaxValue().getValue();
                        }
                    } catch (NullPointerException e) {

                    }
                }
            }
            setUpYAxisLeft(mChrChart, new float[]{
                    minimumChartValue - 0.1f * (maximumChartValue - minimumChartValue),
                    maximumChartValue + 0.1f * (maximumChartValue - minimumChartValue)});

            CandleDataSet candleDataSet = new CandleDataSet(minMaxAndQuartileFloatValues, null);
            setUpCandleDataSet(candleDataSet);

            mCandleData = new CandleData(candleDataSet);

            LineDataSet lineDataSet = new LineDataSet(meanFloatValues, null);
            setUpLineDataSet(lineDataSet);

            mLineData = new LineData(lineDataSet);

            mMyMarkerView = new MyMarkerView(getContext(), mDatabaseActivity.getActualSortingLevel(),
                    mDatabaseActivity.getActualSelectedSensor());
            mMyMarkerView.setChartView(mChrChart);
            mChrChart.setMarker(mMyMarkerView);
            renderChart(animation);
        });
    }

    public void setTemperatureData(List<StatisticalData> statisticalDataByInterval, int[] years,
                                   boolean animation) {
        mDatabaseActivity.runOnUiThread(() -> {
            ArrayList<CandleEntry> minMaxAndQuartileFloatValues = new ArrayList<>();
            ArrayList<Entry> meanFloatValues = new ArrayList<>();
            int firstEntry = setUpXAxis(mChrChart, years);
            float minimumChartValue = TEMPERATURE_SENSOR_MAX_VALUE;
            float maximumChartValue = TEMPERATURE_SENSOR_MIN_VALUE;
            mTxvActualInterval.setText(formatIntervalLabel());
            for (int i = 0; i < statisticalDataByInterval.size(); i++) {
                if (statisticalDataByInterval.get(i) != null) {
                    try {
                        minMaxAndQuartileFloatValues.add(new CandleEntry(
                                firstEntry + i,
                                statisticalDataByInterval.get(i).getMaxValue().getValue(),
                                statisticalDataByInterval.get(i).getMinValue().getValue(),
                                statisticalDataByInterval.get(i).getThirdQuartile().getValue(),
                                statisticalDataByInterval.get(i).getFirstQuartile().getValue()));
                        meanFloatValues.add(new Entry(
                                firstEntry + i,
                                statisticalDataByInterval.get(i).getMeanValue()));
                        if (statisticalDataByInterval.get(i).getMinValue().getValue() <
                                minimumChartValue) {
                            minimumChartValue =
                                    statisticalDataByInterval.get(i).getMinValue().getValue();
                        }
                        if (statisticalDataByInterval.get(i).getMaxValue().getValue() >
                                maximumChartValue) {
                            maximumChartValue =
                                    statisticalDataByInterval.get(i).getMaxValue().getValue();
                        }
                    } catch (NullPointerException e) {

                    }
                }
            }
            setUpYAxisLeft(mChrChart, new float[]{
                    minimumChartValue - 0.1f * (maximumChartValue - minimumChartValue),
                    maximumChartValue + 0.1f * (maximumChartValue - minimumChartValue)});

            CandleDataSet candleDataSet = new CandleDataSet(minMaxAndQuartileFloatValues, null);
            setUpCandleDataSet(candleDataSet);

            mCandleData = new CandleData(candleDataSet);

            LineDataSet lineDataSet = new LineDataSet(meanFloatValues, null);
            setUpLineDataSet(lineDataSet);

            mLineData = new LineData(lineDataSet);

            mMyMarkerView = new MyMarkerView(getContext(), mDatabaseActivity.getActualSortingLevel(),
                    mDatabaseActivity.getActualSelectedSensor());
            mMyMarkerView.setChartView(mChrChart);
            mChrChart.setMarker(mMyMarkerView);
            renderChart(animation);
        });
    }

    private void renderChart(boolean animation) {
        CombinedData combinedData = new CombinedData();
        if (mSumAndRangeVisible) {
            if (mDatabaseActivity.getActualSelectedSensor() == R.id.pedometer) {
                combinedData.setData(mBarData);
            } else {
                combinedData.setData(mCandleData);
            }
        } else {
            combinedData.setData(new BarData());
        }
        if (mMeanVisible) {
            combinedData.setData(mLineData);
        } else {
            combinedData.setData(new LineData());
        }
        combinedData.setValueTextSize(10f);
        combinedData.setHighlightEnabled(true);

        mChrChart.setData(null);
        mChrChart.invalidate();
        mChrChart.setData(combinedData);
        mChrChart.invalidate();
        mChrChart.setVisibility(View.VISIBLE);
        if (animation) {
            mChrChart.animateY(CHART_ANIMATION_TIME_MS);
        }
    }

    private int setUpXAxis(MyCombinedChart myCombinedChart, int[] years) {
        XAxis xAxis = myCombinedChart.getXAxis();
        xAxis.setValueFormatter(new IntervalValueFormatter(requireContext(),
                mDatabaseActivity.getActualSortingLevel()));
        xAxis.setLabelCount(7, false);
        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
        xAxis.setDrawGridLines(false);
        xAxis.setGranularity(1);
        switch (mDatabaseActivity.getActualSortingLevel()) {
            case Calendar.YEAR:
                xAxis.setAxisMinimum(years[0] - (BAR_AND_CANDLE_WIDTH + BAR_AND_CANDLE_SPACE) / 2);
                xAxis.setAxisMaximum(years[1] + (BAR_AND_CANDLE_WIDTH + BAR_AND_CANDLE_SPACE) / 2);
                return years[0];

            case Calendar.MONTH:
                xAxis.setAxisMinimum(mDatabaseActivity.getActualSortingInterval()
                        .getActualMinimum(Calendar.MONTH) + 1 -
                        (BAR_AND_CANDLE_WIDTH + BAR_AND_CANDLE_SPACE) / 2);
                xAxis.setAxisMaximum(mDatabaseActivity.getActualSortingInterval()
                        .getActualMaximum(Calendar.MONTH) + 1 +
                        (BAR_AND_CANDLE_WIDTH + BAR_AND_CANDLE_SPACE) / 2);
                return mDatabaseActivity.getActualSortingInterval()
                        .getActualMinimum(Calendar.MONTH);

            case Calendar.DAY_OF_MONTH:
                xAxis.setAxisMinimum(mDatabaseActivity.getActualSortingInterval()
                        .getActualMinimum(Calendar.DAY_OF_MONTH) -
                        (BAR_AND_CANDLE_WIDTH + BAR_AND_CANDLE_SPACE) / 2);
                xAxis.setAxisMaximum(mDatabaseActivity.getActualSortingInterval()
                        .getActualMaximum(Calendar.DAY_OF_MONTH) +
                        (BAR_AND_CANDLE_WIDTH + BAR_AND_CANDLE_SPACE) / 2);
                return mDatabaseActivity.getActualSortingInterval()
                        .getActualMinimum(Calendar.DAY_OF_MONTH) - 1;

            case Calendar.HOUR_OF_DAY:
                xAxis.setAxisMinimum(mDatabaseActivity.getActualSortingInterval()
                        .getActualMinimum(Calendar.HOUR_OF_DAY) -
                        (BAR_AND_CANDLE_WIDTH + BAR_AND_CANDLE_SPACE) / 2);
                xAxis.setAxisMaximum(mDatabaseActivity.getActualSortingInterval()
                        .getActualMaximum(Calendar.HOUR_OF_DAY) +
                        (BAR_AND_CANDLE_WIDTH + BAR_AND_CANDLE_SPACE) / 2);
                return mDatabaseActivity.getActualSortingInterval()
                        .getActualMinimum(Calendar.HOUR_OF_DAY);

            case Calendar.MINUTE:
                xAxis.setAxisMinimum(mDatabaseActivity.getActualSortingInterval()
                        .getActualMinimum(Calendar.MINUTE) -
                        (BAR_AND_CANDLE_WIDTH + BAR_AND_CANDLE_SPACE) / 2);
                xAxis.setAxisMaximum(mDatabaseActivity.getActualSortingInterval()
                        .getActualMaximum(Calendar.MINUTE) +
                        (BAR_AND_CANDLE_WIDTH + BAR_AND_CANDLE_SPACE) / 2);
                return mDatabaseActivity.getActualSortingInterval()
                        .getActualMinimum(Calendar.MINUTE);

            case Calendar.SECOND:
                xAxis.setAxisMinimum(mDatabaseActivity.getActualSortingInterval()
                        .getActualMinimum(Calendar.SECOND) -
                        (BAR_AND_CANDLE_WIDTH + BAR_AND_CANDLE_SPACE) / 2);
                xAxis.setAxisMaximum(mDatabaseActivity.getActualSortingInterval()
                        .getActualMaximum(Calendar.SECOND) +
                        (BAR_AND_CANDLE_WIDTH + BAR_AND_CANDLE_SPACE) / 2);
                return mDatabaseActivity.getActualSortingInterval()
                        .getActualMinimum(Calendar.SECOND);

            default:
                throw new IllegalStateException(
                        "Unexpected value of  mDatabaseActivity.getActualSortingLevel(): " +
                                mDatabaseActivity.getActualSortingLevel());
        }
    }

    private void setUpChart(MyCombinedChart myCombinedChart) {
        myCombinedChart.setMaxVisibleValueCount(0);
        myCombinedChart.getDescription().setEnabled(false);
        myCombinedChart.setPinchZoom(true);
        myCombinedChart.setScaleYEnabled(false);
    }

    public void setUpYAxisLeft(MyCombinedChart myCombinedChart, float[] ranges) {
        YAxis yAxisLeft = myCombinedChart.getAxisLeft();
        yAxisLeft.setAxisMinimum(ranges[0]);
        yAxisLeft.setAxisMaximum(ranges[1]);
        yAxisLeft.setValueFormatter(
                new RecordValueFormatter(mDatabaseActivity.getActualSelectedSensor()));
        yAxisLeft.setLabelCount(8, false);
        yAxisLeft.setPosition(YAxis.YAxisLabelPosition.OUTSIDE_CHART);
        yAxisLeft.setSpaceTop(15f);
    }

    private void setUpYAxisRight(MyCombinedChart myCombinedChart) {
        YAxis yAxisRight = myCombinedChart.getAxisRight();
        yAxisRight.setEnabled(false);
    }

    private void setUpBarDataSet(BarDataSet barDataSet) {
        barDataSet.setColor(requireContext().getColor(R.color.ColorFei));
        barDataSet.setAxisDependency(YAxis.AxisDependency.LEFT);
    }

    private void setUpCandleDataSet(CandleDataSet candleDataSet) {
        candleDataSet.setDrawIcons(false);
        candleDataSet.setAxisDependency(YAxis.AxisDependency.LEFT);
        candleDataSet.setShadowColor(Color.BLACK);
        candleDataSet.setShadowWidth(1f);
        candleDataSet.setDecreasingColor(requireContext().getColor(R.color.ColorFei));
        candleDataSet.setDecreasingPaintStyle(Paint.Style.FILL);
        candleDataSet.setNeutralColor(requireContext().getColor(R.color.ColorFei));
        candleDataSet.setHighlightLineWidth(1f);
    }

    private void setUpLineDataSet(LineDataSet lineDataSet) {
        lineDataSet.setColor(requireContext().getColor(R.color.ColorVsb));
        lineDataSet.setLineWidth(2f);
        lineDataSet.setCircleColor(requireContext().getColor(R.color.ColorVsb));
        lineDataSet.setCircleRadius(2f);
        lineDataSet.setFillColor(requireContext().getColor(R.color.ColorVsb));
        lineDataSet.setMode(LineDataSet.Mode.HORIZONTAL_BEZIER);
        lineDataSet.setDrawValues(true);
        lineDataSet.setValueTextSize(10f);
        lineDataSet.setAxisDependency(YAxis.AxisDependency.LEFT);
    }
}