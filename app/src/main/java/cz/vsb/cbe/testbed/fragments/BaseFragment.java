package cz.vsb.cbe.testbed.fragments;

import android.annotation.SuppressLint;
import android.graphics.Color;
import android.graphics.Paint;
import android.os.Bundle;
import android.os.Handler;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Switch;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.github.mikephil.charting.components.Legend;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.CandleData;
import com.github.mikephil.charting.data.CandleDataSet;
import com.github.mikephil.charting.data.CandleEntry;
import com.github.mikephil.charting.data.CombinedData;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;

import cz.vsb.cbe.testbed.DatabaseActivity;
import cz.vsb.cbe.testbed.R;
import cz.vsb.cbe.testbed.TestbedDevice;
import cz.vsb.cbe.testbed.chart.MyMarkerView;
import cz.vsb.cbe.testbed.chart.MyBarChart;
import cz.vsb.cbe.testbed.chart.MyMarkerViewNew;
import cz.vsb.cbe.testbed.chart.axisValueFormater.GeneralFloatValueFormatter;
import cz.vsb.cbe.testbed.chart.axisValueFormater.GeneralIntegerValueFormatter;
import cz.vsb.cbe.testbed.chart.axisValueFormater.MonthValueFormater;
import cz.vsb.cbe.testbed.sql.TestbedDatabase;

abstract public class BaseFragment extends Fragment {

    public static final int BLINK_TIME = 250;

    public static final float CANDLE_WIDTH = 0.9f;
    public static final float CANDLE_SPACE = 1 - CANDLE_WIDTH;



    protected TextView txvNoDataAvailable;

    public MyBarChart combinedChart;

    public XAxis xAxis;
    public YAxis yAxisLeft;
    public YAxis yAxisRight;
    private Legend legend;

    public MyMarkerView myMarkerView;
    public MyMarkerViewNew myMarkerViewNew;

    public int actualSortingLevel;

    protected float minimumFloatValue;
    protected float maximumFloatValue;

    protected int minimumIntegerValue;
    protected int maximumIntegerValue;


    protected enum UNITS{
        STEPS,
        BEATS_PER_MINUTE,
        DEGREES_OF_CELSIUS
    }

    protected SimpleDateFormat yearFormatter;
    protected SimpleDateFormat monthFormatter;
    protected SimpleDateFormat dayOfMonthFormatter;
    protected SimpleDateFormat hourOfDayFormatter;
    protected SimpleDateFormat minuteFormatter;
    protected SimpleDateFormat secondFormatter;

    protected ArrayList<CandleEntry> minMaxAndQuartileFloatValues;
    protected ArrayList<Entry> meanFloatValues;

    protected CandleDataSet candleDataSet;
    protected CandleData candleData;

    protected LineDataSet lineDataSet;
    protected LineData lineData;

    protected CombinedData combinedData;




    public TestbedDatabase testbedDatabase;
    public TestbedDevice testbedDevice;
    public int colorVsb;
    public int colorFei;

    public TextView txvLastValue;
    public  TextView txvLastValueTimestamp;
    public ImageView imvBack;
    public TextView txvInterval;

    public Switch swcValuesRange;
    public Switch swcValuesMean;

    public Calendar actualInterval;

    public boolean lastDataReady;

    protected boolean rangeVisible;
    protected boolean meanVisible;
    public Date[] intervals;

    protected Button btnMoreStatsInfo;

    protected MonthValueFormater monthValueFormater;
    protected GeneralIntegerValueFormatter XAxisIntegerValueFormatter;
    protected GeneralIntegerValueFormatter YAxisIntegerValueFormatter;
    protected GeneralFloatValueFormatter YAxisFloatValueFormatter;



    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        monthValueFormater = new MonthValueFormater(getContext());
        XAxisIntegerValueFormatter = new GeneralIntegerValueFormatter();
        YAxisIntegerValueFormatter = new GeneralIntegerValueFormatter();
        YAxisFloatValueFormatter = new GeneralFloatValueFormatter();
        colorVsb = getContext().getColor(R.color.VSB);
        colorFei = getContext().getColor(R.color.FEI);

        yearFormatter = new SimpleDateFormat("yyyy");
        monthFormatter = new SimpleDateFormat("MMMM");
        dayOfMonthFormatter = new SimpleDateFormat("d. ");
        hourOfDayFormatter = new SimpleDateFormat("HH");
        minuteFormatter = new SimpleDateFormat("mm");
        secondFormatter = new SimpleDateFormat("ss");

        testbedDatabase = TestbedDatabase.getInstance(getContext());
        testbedDevice = getArguments().getParcelable(DatabaseActivity.TESTBED_DEVICE);

        actualInterval = Calendar.getInstance();
        actualSortingLevel = TestbedDatabase.DAY_OF_MONTH;

        rangeVisible = true;
        meanVisible = true;

    }

    protected void dataAvailable(boolean available){
        if(available){
            combinedChart.setVisibility(View.VISIBLE);
            swcValuesRange.setVisibility(View.VISIBLE);
            swcValuesMean.setVisibility(View.VISIBLE);
            btnMoreStatsInfo.setVisibility(View.VISIBLE);
            txvNoDataAvailable.setVisibility(View.INVISIBLE);
        }else {
            combinedChart.setVisibility(View.INVISIBLE);
            swcValuesRange.setVisibility(View.INVISIBLE);
            swcValuesMean.setVisibility(View.INVISIBLE);
            btnMoreStatsInfo.setVisibility(View.INVISIBLE);
            txvNoDataAvailable.setVisibility(View.VISIBLE);
        }
    }

    protected void setUpChart(){
        combinedChart.setMaxVisibleValueCount(0);
        combinedChart.getDescription().setEnabled(false);
        combinedChart.setPinchZoom(true);
        combinedChart.setScaleYEnabled(false);
    }

    protected void setUpXAxis(){
        xAxis = combinedChart.getXAxis();
        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
        xAxis.setDrawGridLines(false);
        xAxis.setGranularity(1);
        xAxis.setLabelCount(7);
    }

    protected void setUpYAxisLeft(UNITS unit){
        yAxisLeft = combinedChart.getAxisLeft();
        switch (unit){
            case STEPS:
                YAxisIntegerValueFormatter.setSuffix(" steps");
                yAxisLeft.setValueFormatter(YAxisIntegerValueFormatter);
                break;
            case BEATS_PER_MINUTE:
                YAxisIntegerValueFormatter.setSuffix(" bpm");
                yAxisLeft.setValueFormatter(YAxisIntegerValueFormatter);
                break;
            case DEGREES_OF_CELSIUS:
                YAxisFloatValueFormatter.setSuffix(" °C");
                yAxisLeft.setValueFormatter(YAxisFloatValueFormatter);
                break;
            default:
                YAxisFloatValueFormatter.setSuffix(" ???");
                yAxisLeft.setValueFormatter(YAxisFloatValueFormatter);
                break;
        }
        yAxisLeft.setLabelCount(8, false);
        yAxisLeft.setPosition(YAxis.YAxisLabelPosition.OUTSIDE_CHART);
        yAxisLeft.setSpaceTop(15f);
    }

    protected void setUpYAxisRight(){
        yAxisRight = combinedChart.getAxisRight();
        yAxisRight.setEnabled(false);
    }

    protected void setUpLegend(){
        legend = combinedChart.getLegend();
        legend.setVerticalAlignment(Legend.LegendVerticalAlignment.BOTTOM);
        legend.setHorizontalAlignment(Legend.LegendHorizontalAlignment.LEFT);
        legend.setOrientation(Legend.LegendOrientation.HORIZONTAL);
        legend.setDrawInside(false);
        legend.setForm(Legend.LegendForm.SQUARE);
        legend.setFormSize(11);
        legend.setTextSize(11);
        legend.setXEntrySpace(4);
    }

    protected void setUpCandleDataSet(){
        candleDataSet.setDrawIcons(false);
        candleDataSet.setAxisDependency(YAxis.AxisDependency.LEFT);
        candleDataSet.setShadowColor(Color.BLACK);
        candleDataSet.setShadowWidth(1f);
        candleDataSet.setDecreasingColor(colorFei);
        candleDataSet.setDecreasingPaintStyle(Paint.Style.FILL);
        candleDataSet.setNeutralColor(colorFei);
        candleDataSet.setHighlightLineWidth(1f);
    }

    protected void setUpLineDataSet(){
        lineDataSet.setColor(colorVsb);
        lineDataSet.setLineWidth(2f);
        lineDataSet.setCircleColor(colorVsb);
        lineDataSet.setCircleRadius(2f);
        lineDataSet.setFillColor(colorVsb);
        lineDataSet.setMode(LineDataSet.Mode.HORIZONTAL_BEZIER);
        lineDataSet.setDrawValues(true);
        lineDataSet.setValueTextSize(10f);
        lineDataSet.setAxisDependency(YAxis.AxisDependency.LEFT);
    }

    protected void setUpCombinedData(){
        combinedData.setValueTextSize(10f);
        combinedData.setHighlightEnabled(true);
    }

    protected Date[] getIntervals(int sortingLevel, Calendar period){
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

        switch (sortingLevel){
            default:
            case TestbedDatabase.YEAR:
                break;
            case TestbedDatabase.MONTH:
                startPeriod.set(Calendar.YEAR, period.get(Calendar.YEAR));
                endPeriod.set(Calendar.YEAR, period.get(Calendar.YEAR));
                break;
            case TestbedDatabase.DAY_OF_MONTH:
                startPeriod.set(Calendar.YEAR, period.get(Calendar.YEAR));
                startPeriod.set(Calendar.MONTH, period.get(Calendar.MONTH));
                endPeriod.set(Calendar.YEAR, period.get(Calendar.YEAR));
                endPeriod.set(Calendar.MONTH, period.get(Calendar.MONTH));
                endPeriod.set(Calendar.DAY_OF_MONTH, period.getActualMaximum(Calendar.DAY_OF_MONTH));
                break;
            case TestbedDatabase.HOUR_OF_DAY:
                startPeriod.set(Calendar.YEAR, period.get(Calendar.YEAR));
                startPeriod.set(Calendar.MONTH, period.get(Calendar.MONTH));
                startPeriod.set(Calendar.DAY_OF_MONTH, period.get(Calendar.DAY_OF_MONTH));
                endPeriod.set(Calendar.YEAR, period.get(Calendar.YEAR));
                endPeriod.set(Calendar.MONTH, period.get(Calendar.MONTH));
                endPeriod.set(Calendar.DAY_OF_MONTH, period.get(Calendar.DAY_OF_MONTH));
                break;
            case TestbedDatabase.MINUTE:
                startPeriod.set(Calendar.YEAR, period.get(Calendar.YEAR));
                startPeriod.set(Calendar.MONTH, period.get(Calendar.MONTH));
                startPeriod.set(Calendar.DAY_OF_MONTH, period.get(Calendar.DAY_OF_MONTH));
                startPeriod.set(Calendar.HOUR_OF_DAY, period.get(Calendar.HOUR_OF_DAY));
                endPeriod.set(Calendar.YEAR, period.get(Calendar.YEAR));
                endPeriod.set(Calendar.MONTH, period.get(Calendar.MONTH));
                endPeriod.set(Calendar.DAY_OF_MONTH, period.get(Calendar.DAY_OF_MONTH));
                endPeriod.set(Calendar.HOUR_OF_DAY, period.get(Calendar.HOUR_OF_DAY));
                break;
            case TestbedDatabase.SECONDS:
                startPeriod.set(Calendar.YEAR, period.get(Calendar.YEAR));
                startPeriod.set(Calendar.MONTH, period.get(Calendar.MONTH));
                startPeriod.set(Calendar.DAY_OF_MONTH, period.get(Calendar.DAY_OF_MONTH));
                startPeriod.set(Calendar.HOUR_OF_DAY, period.get(Calendar.HOUR_OF_DAY));
                startPeriod.set(Calendar.MINUTE, period.get(Calendar.MINUTE));
                endPeriod.set(Calendar.YEAR, period.get(Calendar.YEAR));
                endPeriod.set(Calendar.MONTH, period.get(Calendar.MONTH));
                endPeriod.set(Calendar.DAY_OF_MONTH, period.get(Calendar.DAY_OF_MONTH));
                endPeriod.set(Calendar.HOUR_OF_DAY, period.get(Calendar.HOUR_OF_DAY));
                endPeriod.set(Calendar.MINUTE, period.get(Calendar.MINUTE));
                break;
        }
        return new Date[] {startPeriod.getTime(), endPeriod.getTime()};
    }

    protected Date[] getIntervalsNew(int sortingLevel, Calendar period){
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

        switch (sortingLevel){
            default:
            case Calendar.YEAR:
                break;
            case Calendar.MONTH:
                startPeriod.set(Calendar.YEAR, period.get(Calendar.YEAR));
                endPeriod.set(Calendar.YEAR, period.get(Calendar.YEAR));
                break;
            case Calendar.DAY_OF_MONTH:
                startPeriod.set(Calendar.YEAR, period.get(Calendar.YEAR));
                startPeriod.set(Calendar.MONTH, period.get(Calendar.MONTH));
                endPeriod.set(Calendar.YEAR, period.get(Calendar.YEAR));
                endPeriod.set(Calendar.MONTH, period.get(Calendar.MONTH));
                endPeriod.set(Calendar.DAY_OF_MONTH, period.getActualMaximum(Calendar.DAY_OF_MONTH));
                break;
            case Calendar.HOUR_OF_DAY:
                startPeriod.set(Calendar.YEAR, period.get(Calendar.YEAR));
                startPeriod.set(Calendar.MONTH, period.get(Calendar.MONTH));
                startPeriod.set(Calendar.DAY_OF_MONTH, period.get(Calendar.DAY_OF_MONTH));
                endPeriod.set(Calendar.YEAR, period.get(Calendar.YEAR));
                endPeriod.set(Calendar.MONTH, period.get(Calendar.MONTH));
                endPeriod.set(Calendar.DAY_OF_MONTH, period.get(Calendar.DAY_OF_MONTH));
                break;
            case Calendar.MINUTE:
                startPeriod.set(Calendar.YEAR, period.get(Calendar.YEAR));
                startPeriod.set(Calendar.MONTH, period.get(Calendar.MONTH));
                startPeriod.set(Calendar.DAY_OF_MONTH, period.get(Calendar.DAY_OF_MONTH));
                startPeriod.set(Calendar.HOUR_OF_DAY, period.get(Calendar.HOUR_OF_DAY));
                endPeriod.set(Calendar.YEAR, period.get(Calendar.YEAR));
                endPeriod.set(Calendar.MONTH, period.get(Calendar.MONTH));
                endPeriod.set(Calendar.DAY_OF_MONTH, period.get(Calendar.DAY_OF_MONTH));
                endPeriod.set(Calendar.HOUR_OF_DAY, period.get(Calendar.HOUR_OF_DAY));
                break;
            case Calendar.SECOND:
                startPeriod.set(Calendar.YEAR, period.get(Calendar.YEAR));
                startPeriod.set(Calendar.MONTH, period.get(Calendar.MONTH));
                startPeriod.set(Calendar.DAY_OF_MONTH, period.get(Calendar.DAY_OF_MONTH));
                startPeriod.set(Calendar.HOUR_OF_DAY, period.get(Calendar.HOUR_OF_DAY));
                startPeriod.set(Calendar.MINUTE, period.get(Calendar.MINUTE));
                endPeriod.set(Calendar.YEAR, period.get(Calendar.YEAR));
                endPeriod.set(Calendar.MONTH, period.get(Calendar.MONTH));
                endPeriod.set(Calendar.DAY_OF_MONTH, period.get(Calendar.DAY_OF_MONTH));
                endPeriod.set(Calendar.HOUR_OF_DAY, period.get(Calendar.HOUR_OF_DAY));
                endPeriod.set(Calendar.MINUTE, period.get(Calendar.MINUTE));
                break;
        }
        return new Date[] {startPeriod.getTime(), endPeriod.getTime()};
    }

    public void updateLastHeartRateValue(int value, Date date, boolean blink){
        updateLastDataValue(String.format("%d", value) + " BPM", date, blink);
    }

    public void updateLastTemperatureValue(float value, Date date, boolean blink){
        if (value > 0) {
            updateLastDataValue("+ " + String.format("%3.2f", value) + " °C", date, blink);
        } else if (value < 0){
            updateLastDataValue("- " + String.format("%3.2f", value) + " °C", date, blink);
        } else {
            updateLastDataValue(String.format("%3.2f", value) + " °C", date, blink);
        }
    }

    public void updateLastStepsValue(int value, Date date, boolean blink){
        updateLastDataValue(String.format("%d", value) + " STEPS", date, blink);
    }

    public int decrementSorting(){
        actualSortingLevel--;
        return actualSortingLevel;
    }

    public int decrementSortingNew(){
        switch (actualSortingLevel) {
            default:
            case Calendar.YEAR:
                actualSortingLevel = -1;
                break;
            case Calendar.MONTH:
                actualSortingLevel = Calendar.YEAR;
                break;
            case Calendar.DAY_OF_MONTH:
                actualSortingLevel = Calendar.MONTH;
                break;
            case Calendar.HOUR_OF_DAY:
                actualSortingLevel = Calendar.DAY_OF_MONTH;
                break;
            case Calendar.MINUTE:
                actualSortingLevel = Calendar.HOUR_OF_DAY;
                break;
            case Calendar.SECOND:
                actualSortingLevel = Calendar.MINUTE;
                break;
        }
        return actualSortingLevel;
    }

    private void updateLastDataValue(String data, Date date, boolean blink){
        txvLastValue.setText(data);
        @SuppressLint("SimpleDateFormat") SimpleDateFormat simpleDateFormat = new SimpleDateFormat("EEEE, d. MMMM yyyy HH:mm:ss.SSS XXX");
        txvLastValueTimestamp.setText(simpleDateFormat.format(date) + " GMT");
        if(blink) {
            txvLastValue.setTextColor(getContext().getColor(android.R.color.holo_red_dark));
            txvLastValueTimestamp.setTextColor(getContext().getColor(android.R.color.holo_red_dark));
            new Handler().postDelayed(new Runnable() {
                @Override
                public void run() {
                    txvLastValue.setTextColor(getContext().getColor(R.color.VSB));
                    txvLastValueTimestamp.setTextColor(getContext().getColor(R.color.FEI));
                }
            }, BLINK_TIME);
        }
    }

}
