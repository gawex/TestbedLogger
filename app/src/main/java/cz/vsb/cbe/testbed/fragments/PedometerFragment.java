package cz.vsb.cbe.testbed.fragments;

import android.annotation.SuppressLint;
import android.database.Cursor;
import android.os.AsyncTask;
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
import java.util.List;
import java.util.Map;

import cz.vsb.cbe.testbed.BluetoothLeService;
import cz.vsb.cbe.testbed.R;
import cz.vsb.cbe.testbed.TestbedDevice;
import cz.vsb.cbe.testbed.chart.MyBarChart;
import cz.vsb.cbe.testbed.chart.MyMarkerView;
import cz.vsb.cbe.testbed.chart.MyMarkerViewNew;
import cz.vsb.cbe.testbed.sql.Record;
import cz.vsb.cbe.testbed.sql.TestbedDatabase;
import cz.vsb.cbe.testbed.sql.TestbedDatabaseHelper;
import cz.vsb.cbe.testbed.sql.TestbedDatabaseNew;


public class PedometerFragment extends BaseFragment {

    @Override
    public View onCreateView(final LayoutInflater inflater, final ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_pedometer, container, false);

        txvLastValue = view.findViewById(R.id.fragment_pedometer_txv_last_value);
        txvLastValueTimestamp = view.findViewById(R.id.fragment_pedometer_txv_last_value_timestamp);

        txvInterval = view.findViewById(R.id.fragment_pedometer_txv_interval);

        combinedChart = view.findViewById(R.id.fragment_pedometer_chr);
        txvNoDataAvailable = view.findViewById(R.id.fragment_pedometer_txv_no_data_available);

        swcValuesRange = view.findViewById(R.id.fragment_pedometer_swc_min_max_and_quartile_values);
        swcValuesMean = view.findViewById(R.id.fragment_pedometer_swc_mean_values);
        btnMoreStatsInfo = view.findViewById(R.id.fragment_pedometer_btn_more_stats_info);

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
                    combinedChart.setMarker(myMarkerViewNew); // Set the marker to the chart
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
                        case Calendar.YEAR:
                            actualInterval.set(Calendar.YEAR, (int) e.getX());
                            actualSortingLevel = Calendar.MONTH;
                            break;
                        case Calendar.MONTH:
                            actualInterval.set(Calendar.MONTH, (int) e.getX() - 1);
                            actualSortingLevel = Calendar.DAY_OF_MONTH;
                            break;
                        case Calendar.DAY_OF_MONTH:
                            actualInterval.set(Calendar.DAY_OF_MONTH, (int) e.getX());
                            actualSortingLevel = Calendar.HOUR_OF_DAY;
                            break;
                        case Calendar.HOUR_OF_DAY:
                            actualInterval.set(Calendar.HOUR_OF_DAY, (int) e.getX());
                            actualSortingLevel = Calendar.MINUTE;
                            break;
                        case Calendar.MINUTE:
                            actualInterval.set(Calendar.MINUTE, (int) e.getX());
                            actualSortingLevel = Calendar.SECOND;
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
        setUpYAxisLeft(UNITS.STEPS);
        setUpYAxisRight();
        setUpLegend();

        setChartData(actualInterval, actualSortingLevel, true);

        return view;
    }

    public void setChartData(final Calendar period, final int scale, final boolean animation) {

            intervals = getIntervalsNew(scale, period);
            myMarkerViewNew = new MyMarkerViewNew(getContext(), scale, "###,###", "###,###.00", "steps");

            if (intervals != null) {

                SelectFromDatabase selectFromDatabase = new SelectFromDatabase(testbedDevice, BluetoothLeService.STEPS_DATA, intervals[0], intervals[1], period, scale, animation);
                selectFromDatabase.execute();

            } else {
                //TODO: Handle intervals null ERROR
            }
    }

    private class SelectFromDatabase extends AsyncTask<Void, Void, List<Map<Integer,Float>>> {

        private final TestbedDevice TestbedDevice;
        private final String DataType;
        private final Date StartDate;
        private final Date EndDate;
        private final Calendar IntervalBase;
        private final int IntervalScale;
        private final boolean Animation;

        private TestbedDatabaseNew TestbedDatabaseNew;

        private Calendar FirstRecordTimeStamp;
        private Calendar LastRecordTimeStamp;

        private int FirstXValue;

        public SelectFromDatabase(TestbedDevice testbedDevice, String dataType, Date startDate, Date endDate, Calendar intervalBase, int intervalScale, boolean animation){
            TestbedDevice = testbedDevice;
            DataType = dataType;
            StartDate = startDate;
            EndDate = endDate;
            IntervalBase = intervalBase;
            IntervalScale = intervalScale;
            Animation = animation;
            TestbedDatabaseNew = new TestbedDatabaseNew(getContext());
            FirstRecordTimeStamp = Calendar.getInstance();
            LastRecordTimeStamp = Calendar.getInstance();
        }

        @Override
        protected List<Map<Integer, Float>> doInBackground(Void... aVoid) {
            List<Record> records = TestbedDatabaseNew.selectDataBetweenTimeStamp(TestbedDevice, DataType, StartDate, EndDate);
            Record previousRecord = TestbedDatabaseNew.getFirstRecordBeforeTimeStamp(testbedDevice, DataType, StartDate);
            List<List<Record>> sortedRecords = TestbedDatabaseNew.sortRecordsByIntervals(records, IntervalBase, IntervalScale);
            if(previousRecord == null){
                previousRecord = new Record(0,0,0);
            }
            List<List<Record>> sortedSteps = TestbedDatabaseNew.sortStepsByIntervals(previousRecord, sortedRecords);
            try {
                FirstRecordTimeStamp.setTimeInMillis(TestbedDatabaseNew.getFirstRecord(TestbedDatabaseNew.selectAllData(TestbedDevice, DataType)).getTimeStamp().getTime());
                LastRecordTimeStamp.setTimeInMillis(TestbedDatabaseNew.getLastRecord(TestbedDatabaseNew.selectAllData(TestbedDevice, DataType)).getTimeStamp().getTime());
            } catch (NullPointerException e){
                FirstRecordTimeStamp.setTimeInMillis(1577833200000l);
                LastRecordTimeStamp.setTimeInMillis(1609455599999l);
            }

            List<Map<Integer, Float>> stats = TestbedDatabaseNew.getStatisticDataFromRecordsByIntervals(sortedSteps);
            return stats;
        }

        @Override
        protected void onPostExecute(List<Map<Integer, Float>> records) {
            super.onPostExecute(records);
            minMaxAndQuartileFloatValues = new ArrayList<>();
            meanFloatValues = new ArrayList<>();
            minimumFloatValue = TestbedDatabaseNew.PEDOMETER_MAX_VALUE;
            maximumFloatValue = TestbedDatabaseNew.PEDOMETER_MIN_VALUE;
            switch (IntervalScale) {
                case Calendar.YEAR:
                    xAxis.setAxisMinimum(FirstRecordTimeStamp.get(Calendar.YEAR) - (CANDLE_WIDTH + CANDLE_SPACE) / 2);
                    xAxis.setAxisMaximum(LastRecordTimeStamp.get(Calendar.YEAR) + (CANDLE_WIDTH + CANDLE_SPACE) / 2);
                    txvInterval.setText(FirstRecordTimeStamp.get(Calendar.YEAR) + " až " + LastRecordTimeStamp.get(Calendar.YEAR));
                    XAxisIntegerValueFormatter.setSuffix("");
                    xAxis.setValueFormatter(XAxisIntegerValueFormatter);
                    FirstXValue = FirstRecordTimeStamp.get(Calendar.YEAR);
                    break;
                case Calendar.MONTH:
                    xAxis.setAxisMinimum(IntervalBase.getActualMinimum(Calendar.MONTH) + 1 - (CANDLE_WIDTH + CANDLE_SPACE) / 2);
                    xAxis.setAxisMaximum(IntervalBase.getActualMaximum(Calendar.MONTH) + 1 + (CANDLE_WIDTH + CANDLE_SPACE) / 2);
                    txvInterval.setText("leden až  prosinec " + yearFormatter.format(IntervalBase.getTime()));
                    xAxis.setValueFormatter(monthValueFormater);
                    FirstXValue = IntervalBase.getActualMinimum(Calendar.MONTH);
                    break;
                case Calendar.DAY_OF_MONTH:
                    xAxis.setAxisMinimum(IntervalBase.getActualMinimum(Calendar.DAY_OF_MONTH) - (CANDLE_WIDTH + CANDLE_SPACE) / 2);
                    xAxis.setAxisMaximum(IntervalBase.getActualMaximum(Calendar.DAY_OF_MONTH) + (CANDLE_WIDTH + CANDLE_SPACE) / 2);
                    txvInterval.setText(dayOfMonthFormatter.format(StartDate) + "- " + dayOfMonthFormatter.format(EndDate) + monthFormatter.format(IntervalBase.getTime()) + " " + yearFormatter.format(IntervalBase.getTime()));
                    XAxisIntegerValueFormatter.setSuffix(".");
                    xAxis.setValueFormatter(XAxisIntegerValueFormatter);
                    FirstXValue = IntervalBase.getActualMinimum(Calendar.DAY_OF_MONTH) - 1;
                    break;
                case Calendar.HOUR_OF_DAY:
                    xAxis.setAxisMinimum(IntervalBase.getActualMinimum(Calendar.HOUR_OF_DAY) - (CANDLE_WIDTH + CANDLE_SPACE) / 2);
                    xAxis.setAxisMaximum(IntervalBase.getActualMaximum(Calendar.HOUR_OF_DAY) + (CANDLE_WIDTH + CANDLE_SPACE) / 2);
                    txvInterval.setText(dayOfMonthFormatter.format(IntervalBase.getTime()) + monthFormatter.format(IntervalBase.getTime()) +
                            " " + yearFormatter.format(IntervalBase.getTime()) + " " + hourOfDayFormatter.format(StartDate) + " - " + hourOfDayFormatter.format(EndDate));
                    XAxisIntegerValueFormatter.setSuffix(" h");
                    xAxis.setValueFormatter(XAxisIntegerValueFormatter);
                    FirstXValue = IntervalBase.getActualMinimum(Calendar.HOUR_OF_DAY);
                    break;
                case Calendar.MINUTE:
                    xAxis.setAxisMinimum(IntervalBase.getActualMinimum(Calendar.MINUTE) - (CANDLE_WIDTH + CANDLE_SPACE) / 2);
                    xAxis.setAxisMaximum(IntervalBase.getActualMaximum(Calendar.MINUTE) + (CANDLE_WIDTH + CANDLE_SPACE) / 2);
                    txvInterval.setText(dayOfMonthFormatter.format(IntervalBase.getTime()) + monthFormatter.format(IntervalBase.getTime()) +
                            " " + yearFormatter.format(IntervalBase.getTime()) + " " + hourOfDayFormatter.format(IntervalBase.getTime()) + ":" + minuteFormatter.format(StartDate) + " - " + minuteFormatter.format(EndDate));
                    XAxisIntegerValueFormatter.setSuffix(" m");
                    xAxis.setValueFormatter(XAxisIntegerValueFormatter);
                    FirstXValue = IntervalBase.getActualMinimum(Calendar.MINUTE);
                    break;
                case Calendar.SECOND:
                    xAxis.setAxisMinimum(IntervalBase.getActualMinimum(Calendar.SECOND) - (CANDLE_WIDTH + CANDLE_SPACE) / 2);
                    xAxis.setAxisMaximum(IntervalBase.getActualMaximum(Calendar.SECOND) + (CANDLE_WIDTH + CANDLE_SPACE) / 2);
                    txvInterval.setText(dayOfMonthFormatter.format(IntervalBase.getTime()) + monthFormatter.format(IntervalBase.getTime()) +
                            " " + yearFormatter.format(IntervalBase.getTime()) + " " + hourOfDayFormatter.format(IntervalBase.getTime()) + ":" + minuteFormatter.format(IntervalBase.getTime()) + ":" + secondFormatter.format(StartDate) + " - " + secondFormatter.format(EndDate));
                    XAxisIntegerValueFormatter.setSuffix(" s");
                    xAxis.setValueFormatter(XAxisIntegerValueFormatter);
                    FirstXValue = IntervalBase.getActualMinimum(Calendar.SECOND);
                    break;
                default:
                    xAxis.setValueFormatter(null);
                    FirstXValue = 0;
                    break;
            }
            for (int i = 0; i < records.size(); i++) {
                if (records.get(i) != null) {
                    minMaxAndQuartileFloatValues.add(new CandleEntry(FirstXValue + i,
                            records.get(i).get(TestbedDatabase.MAX_VALUE),
                            records.get(i).get(TestbedDatabase.MIN_VALUE),
                            records.get(i).get(TestbedDatabase.HIGH_QUARTILE),
                            records.get(i).get(TestbedDatabase.LOW_QUARTILE)));
                    meanFloatValues.add(new Entry(FirstXValue + i, records.get(i).get(TestbedDatabase.MEAN_VALUE)));

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

                candleDataSet = new CandleDataSet(minMaxAndQuartileFloatValues, "Rozpětí kroků");
                setUpCandleDataSet();

                candleData = new CandleData(candleDataSet);

                lineDataSet = new LineDataSet(meanFloatValues, "Průměr kroků");
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
                myMarkerViewNew.setChartView(combinedChart); // For bounds control
                combinedChart.setMarker(myMarkerViewNew); // Set the marker to the chart
                combinedChart.invalidate();

                if (Animation) {
                    combinedChart.animateY(500);
                }
                dataAvailable(true);
            } else {
                dataAvailable(false);
            }

        }
    }


    @Override
    public void onResume() {
        super.onResume();
        /*if(lastDataReady) {
            updateLastTemperatureValue(getArguments().getFloat(DatabaseActivity.LAST_DATA_VALUE), (Date) getArguments().getSerializable(DatabaseActivity.LAST_DATA_TIME_STAMP), false);
        }*/
    }

}




