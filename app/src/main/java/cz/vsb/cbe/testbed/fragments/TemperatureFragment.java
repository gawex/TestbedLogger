package cz.vsb.cbe.testbed.fragments;

import android.annotation.SuppressLint;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CompoundButton;
import android.widget.Toast;

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
import java.util.Map;

import cz.vsb.cbe.testbed.DatabaseActivity;

import cz.vsb.cbe.testbed.R;
import cz.vsb.cbe.testbed.chart.MyBarChart;
import cz.vsb.cbe.testbed.chart.MyMarkerView;
import cz.vsb.cbe.testbed.sql.TestbedDatabase;


public class TemperatureFragment extends BaseFragment {

    @Override
    public View onCreateView(final LayoutInflater inflater, final ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_temperature, container, false);

        txvLastValue = view.findViewById(R.id.fragment_temperature_txv_last_value);
        txvLastValueTimestamp = view.findViewById(R.id.fragment_temperature_txv_last_value_timestamp);

        txvInterval = view.findViewById(R.id.fragment_temperature_txv_interval);

        combinedChart = view.findViewById(R.id.fragment_temperature_chr);
        txvNoDataAvailable = view.findViewById(R.id.fragment_temperature_txv_no_data_available);

        swcValuesRange = view.findViewById(R.id.fragment_temperature_swc_min_max_and_quartile_values);
        swcValuesMean = view.findViewById(R.id.fragment_temperature_swc_mean_values);
        btnMoreStatsInfo = view.findViewById(R.id.fragment_temperature_btn_more_stats_info);

        txvInterval.setClickable(true);
        txvInterval.setOnLongClickListener(new View.OnLongClickListener() {
            @SuppressLint({"WrongConstant", "ShowToast"})
            @Override
            public boolean onLongClick(View view) {
                Toast.makeText(getContext(), "Not implement yet :-(", Toast.LENGTH_SHORT).show();

                return false;
            }
        });

        setUpChart();
        combinedChart.setOnChartValueShortClickListener(new OnChartValueSelectedListener() {
            @Override
            public void onValueSelected(Entry e, Highlight h) {
                Log.w("onMyShortClick", "(" + e.getX() + " | " + e.getY() + ")");
                if(e.getY() > 0){
                    combinedChart.setMarker(myMarkerView); // Set the marker to the chart
                }else {
                    combinedChart.setMarker(null);
                }
            }

            @Override
            public void onNothingSelected() {

            }
        });
        combinedChart.setOnChartValueLongClickListener(new MyBarChart.OnChartValueClickListener() {
            @Override
            public void onChartValueLongClickListener(Entry e) {
                if (e != null) {
                    Log.w("onMyLongClick", "(" + e.getX() + " | " + e.getY() + ")");
                    combinedChart.setMarker(null); // Set the marker to the chart

                    switch (actualSortingLevel) {
                        case TestbedDatabase.YEAR:
                            actualInterval.set(Calendar.YEAR, (int) e.getX());
                            actualSortingLevel = TestbedDatabase.MONTH;
                            break;
                        case TestbedDatabase.MONTH:
                            actualInterval.set(Calendar.MONTH, (int) e.getX() - 1);
                            actualSortingLevel = TestbedDatabase.DAY_OF_MONTH;
                            break;
                        case TestbedDatabase.DAY_OF_MONTH:
                            actualInterval.set(Calendar.DAY_OF_MONTH, (int) e.getX());
                            actualSortingLevel = TestbedDatabase.HOUR_OF_DAY;
                            break;
                        case TestbedDatabase.HOUR_OF_DAY:
                            actualInterval.set(Calendar.HOUR_OF_DAY, (int) e.getX());
                            actualSortingLevel = TestbedDatabase.MINUTE;
                            break;
                        case TestbedDatabase.MINUTE:
                            actualInterval.set(Calendar.MINUTE, (int) e.getX());
                            actualSortingLevel = TestbedDatabase.SECONDS;
                            break;
                        default:
                            break;
                    }
                    setChartData(actualInterval, actualSortingLevel, true);
                }
            }
        });

        swcValuesRange.setChecked(true);
        swcValuesRange.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
                rangeVisible = b;
                setChartData(actualInterval, actualSortingLevel, false);
            }
        });

        swcValuesMean.setChecked(true);
        swcValuesMean.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
                meanVisible = b;
                setChartData(actualInterval, actualSortingLevel, false);
            }
        });

        btnMoreStatsInfo.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Toast.makeText(getContext(), "Not implement yet :-(", Toast.LENGTH_SHORT).show();
            }
        });

        setUpXAxis();
        setUpYAxisLeft(UNITS.DEGREES_OF_CELSIUS);
        setUpYAxisRight();
        setUpLegend();

        setChartData(actualInterval, actualSortingLevel, true);

        return view;
    }

    public void setChartData(final Calendar period, final int scale, final boolean animation) {

        intervals = getIntervals(scale, period);
        myMarkerView = new MyMarkerView(getContext(), scale, "###.00","###.00", "°C");

        if (intervals != null) {
            testbedDatabase.selectTemperatureData(testbedDevice, intervals[0], intervals[1], scale, new TestbedDatabase.OnSelectFloatData() {
                @Override
                public void onSelectSuccess(ArrayList<Map<Integer,Float>> records,  int firstXValue, int lastXValue ) {
                    minMaxAndQuartileFloatValues = new ArrayList<>();
                    meanFloatValues = new ArrayList<>();
                    minimumFloatValue = TestbedDatabase.TEMPERATURE_SENSOR_MAX_VALUE;
                    maximumFloatValue = TestbedDatabase.TEMPERATURE_SENSOR_MIN_VALUE;
                    xAxis.setAxisMinimum(firstXValue - (CANDLE_WIDTH + CANDLE_SPACE) / 2);
                    xAxis.setAxisMaximum(lastXValue + (CANDLE_WIDTH + CANDLE_SPACE) / 2);
                    switch (scale) {
                        case TestbedDatabase.YEAR:
                            txvInterval.setText(firstXValue + " až " + lastXValue);
                            XAxisIntegerValueFormatter.setSuffix("");
                            xAxis.setValueFormatter(XAxisIntegerValueFormatter);
                            break;
                        case TestbedDatabase.MONTH:
                            txvInterval.setText("leden až  prosinec " + yearFormatter.format(period.getTime()));
                            xAxis.setValueFormatter(monthValueFormater);
                            break;
                        case TestbedDatabase.DAY_OF_MONTH:
                            txvInterval.setText(dayOfMonthFormatter.format(intervals[0]) + "- " + dayOfMonthFormatter.format(intervals[1]) + monthFormatter.format(period.getTime()) + " " + yearFormatter.format(period.getTime()));
                            XAxisIntegerValueFormatter.setSuffix(".");
                            xAxis.setValueFormatter(XAxisIntegerValueFormatter);
                            break;
                        case TestbedDatabase.HOUR_OF_DAY:
                            txvInterval.setText(dayOfMonthFormatter.format(period.getTime()) + monthFormatter.format(period.getTime()) +
                                    " " + yearFormatter.format(period.getTime()) + " " + hourOfDayFormatter.format(intervals[0]) + " - " + hourOfDayFormatter.format(intervals[1]));
                            XAxisIntegerValueFormatter.setSuffix(" h");
                            xAxis.setValueFormatter(XAxisIntegerValueFormatter);
                            break;
                        case TestbedDatabase.MINUTE:
                            txvInterval.setText(dayOfMonthFormatter.format(period.getTime()) + monthFormatter.format(period.getTime()) +
                                    " " + yearFormatter.format(period.getTime()) + " " + hourOfDayFormatter.format(period.getTime()) + ":" + minuteFormatter.format(intervals[0]) + " - " + minuteFormatter.format(intervals[1]));
                            XAxisIntegerValueFormatter.setSuffix(" m");
                            xAxis.setValueFormatter(XAxisIntegerValueFormatter);
                            break;
                        case TestbedDatabase.SECONDS:
                            txvInterval.setText(dayOfMonthFormatter.format(period.getTime()) + monthFormatter.format(period.getTime()) +
                                    " " + yearFormatter.format(period.getTime()) + " " + hourOfDayFormatter.format(period.getTime()) + ":" + minuteFormatter.format(period.getTime()) + ":" + secondFormatter.format(intervals[0]) + " - " + secondFormatter.format(intervals[1]));
                            XAxisIntegerValueFormatter.setSuffix(" s");
                            xAxis.setValueFormatter(XAxisIntegerValueFormatter);
                            break;
                        default:
                            xAxis.setValueFormatter(null);
                            break;
                    }
                    for (int i = 0; i < records.size(); i++) {
                        if (records.get(i) != null) {
                            minMaxAndQuartileFloatValues.add(new CandleEntry(firstXValue + i,
                                    records.get(i).get(TestbedDatabase.MAX_VALUE),
                                    records.get(i).get(TestbedDatabase.MIN_VALUE),
                                    records.get(i).get(TestbedDatabase.HIGH_QUARTILE),
                                    records.get(i).get(TestbedDatabase.LOW_QUARTILE)));
                            meanFloatValues.add(new Entry(firstXValue + i, records.get(i).get(TestbedDatabase.MEAN_VALUE)));

                            if (records.get(i).get(TestbedDatabase.MIN_VALUE) < minimumFloatValue) {
                                minimumFloatValue = records.get(i).get(TestbedDatabase.MIN_VALUE);
                            }
                            if (records.get(i).get(TestbedDatabase.MAX_VALUE) > maximumFloatValue) {
                                maximumFloatValue = records.get(i).get(TestbedDatabase.MAX_VALUE);
                            }
                        }
                    }

                    yAxisLeft.setAxisMinimum(minimumFloatValue - 0.5f);
                    yAxisLeft.setAxisMaximum(maximumFloatValue + 0.5f);

                    if (!meanFloatValues.isEmpty() || !minMaxAndQuartileFloatValues.isEmpty()) {

                        candleDataSet = new CandleDataSet(minMaxAndQuartileFloatValues, "Rozpětí teplot");
                        setUpCandleDataSet();

                        candleData = new CandleData(candleDataSet);

                        lineDataSet = new LineDataSet(meanFloatValues, "Průměr teplot");
                        setUpLineDataSet();

                        lineData = new LineData(lineDataSet);

                        combinedData = new CombinedData();
                        if (rangeVisible) {
                            combinedData.setData(candleData);
                        }
                        if (meanVisible) {
                            combinedData.setData(lineData);
                        }
                        setUpCombinedData();

                        combinedChart.setData(combinedData);
                        myMarkerView.setChartView(combinedChart); // For bounds control
                        combinedChart.setMarker(myMarkerView); // Set the marker to the chart
                        combinedChart.invalidate();

                        if (animation) {
                            combinedChart.animateY(500);
                        }
                        dataAvailable(true);
                    } else {
                        dataAvailable(false);
                    }
                }
                @Override
                public void onSelectFailed() {
                    //TODO: Handle selection ERROR
                }
            });

        } else {
            //TODO: Handle intervals null ERROR
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        if(lastDataReady) {
            updateLastTemperatureValue(getArguments().getFloat(DatabaseActivity.LAST_DATA_VALUE), (Date) getArguments().getSerializable(DatabaseActivity.LAST_DATA_TIME_STAMP), false);
        }
    }

}




