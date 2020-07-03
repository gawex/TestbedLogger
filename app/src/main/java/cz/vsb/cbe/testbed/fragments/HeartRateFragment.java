package cz.vsb.cbe.testbed.fragments;

import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.NumberPicker;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;

import com.github.mikephil.charting.components.Legend;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.components.YAxis.YAxisLabelPosition;
import com.github.mikephil.charting.data.BarData;
import com.github.mikephil.charting.data.BarDataSet;
import com.github.mikephil.charting.data.BarEntry;
import com.github.mikephil.charting.data.CombinedData;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.formatter.ValueFormatter;
import com.github.mikephil.charting.highlight.Highlight;
import com.github.mikephil.charting.interfaces.datasets.IBarDataSet;
import com.github.mikephil.charting.listener.OnChartValueSelectedListener;

import java.util.ArrayList;
import java.util.Calendar;

import cz.vsb.cbe.testbed.DatabaseActivity;
import cz.vsb.cbe.testbed.TestbedDevice;
import cz.vsb.cbe.testbed.chart.DayAxisValueFormatter;
import cz.vsb.cbe.testbed.R;
import cz.vsb.cbe.testbed.chart.MyBarChart;
import cz.vsb.cbe.testbed.chart.MyValueFormatter;
import cz.vsb.cbe.testbed.chart.XYMarkerView;


public class HeartRateFragment extends BaseFragment {


    private static final String TAG = HeartRateFragment.class.getSimpleName();

    private MyBarChart chart;

    protected Typeface tfLight;

    AlertDialog alertDialog;
    AlertDialog alertDialogt;

    private String[] pickerVals;


    TestbedDevice testbedDevice;


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

    }

    int count = 6;
    float groupSpace = 0.06f;

    float barWidth = 0.9f; // x2 dataset
    float barSpace = 1- barWidth; // x2 dataset


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_heart_rate, container, false);


        //View mojeView = inflater.inflate(R.layout.graph_navigation, false );

        final Button mesic = view.findViewById(R.id.mesic);

        Button rok = view.findViewById(R.id.rok);
        rok.setClickable(true);
        rok.setText("KOKOT");
        rok.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mesic.setVisibility(View.GONE);
                setTemperatureData(Calendar.getInstance());
            }
        });



        pickerVals = new String[]{"dog", "cat", "lizard", "turtle", "axolotl"};


        final AlertDialog.Builder d = new AlertDialog.Builder(getContext());
        inflater = this.getLayoutInflater();
        View dialogView = inflater.inflate(R.layout.number_picker_dialog, null);
        d.setTitle("Title");
        d.setMessage("Message");
        d.setView(dialogView);
        final NumberPicker numberPicker = (NumberPicker) dialogView.findViewById(R.id.dialog_number_picker);
        numberPicker.setMaxValue(4);
        numberPicker.setMinValue(0);
        numberPicker.setDisplayedValues(pickerVals);
        numberPicker.setWrapSelectorWheel(true);
        numberPicker.setOnValueChangedListener(new NumberPicker.OnValueChangeListener() {
            @Override
            public void onValueChange(NumberPicker numberPicker, int i, int i1) {
                Log.d(TAG, "onValueChange: ");
            }
        });
        d.setPositiveButton("Done", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                Log.d(TAG, "onClick: " + numberPicker.getValue());
            }
        });
        d.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
            }
        });
        alertDialog = d.create();


        final AlertDialog.Builder t = new AlertDialog.Builder(getContext());
        inflater = this.getLayoutInflater();
        View dialogViewt = inflater.inflate(R.layout.text_picker_dialog, null);
        d.setTitle("Titlet");
        d.setMessage("Messaget");
        d.setView(dialogViewt);

        final Spinner spinner = dialogViewt.findViewById(R.id.dialog_text_picker);
        ArrayAdapter<String> spinnerArrayAdapter = new ArrayAdapter<>
                (getContext(), android.R.layout.simple_spinner_dropdown_item, getResources().getStringArray(R.array.months)); //selected item will look like a spinner set from XML
        //spinnerArrayAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinner.setAdapter(spinnerArrayAdapter);

        t.setPositiveButton("Done", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                Log.d(TAG, "onClick: " + spinner.getSelectedItemId());
            }
        });
        t.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
            }
        });
        alertDialogt = d.create();


        testbedDevice = getArguments().getParcelable(DatabaseActivity.TESTBED_DEVICE);


        tfLight = Typeface.DEFAULT;
        chart = view.findViewById(R.id.chart1);


        chart.setDrawBarShadow(false);
        chart.setDrawValueAboveBar(true);

        chart.getDescription().setEnabled(false);

        // if more than 60 entries are displayed in the chart, no values will be
        // drawn
        chart.setMaxVisibleValueCount(60);

        // scaling can now only be done on x- and y-axis separately
        chart.setPinchZoom(false);

        chart.setDrawGridBackground(false);

        ValueFormatter xAxisFormatter = new DayAxisValueFormatter(chart, getResources().getStringArray(R.array.months));

        XAxis xAxis = chart.getXAxis();
        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
        xAxis.setTypeface(tfLight);
        xAxis.setDrawGridLines(false);
        xAxis.setGranularity(1f); // only intervals of 1 day
        xAxis.setLabelCount(7);
        xAxis.setAxisMinimum(1 - (barWidth / 2) - barSpace);
        xAxis.setAxisMaximum((float) count + (barWidth / 2) + barSpace);
        xAxis.setValueFormatter(xAxisFormatter);

        ValueFormatter custom = new MyValueFormatter("$");

        YAxis leftAxis = chart.getAxisLeft();
        leftAxis.setTypeface(tfLight);
        leftAxis.setLabelCount(8, false);
        leftAxis.setValueFormatter(custom);
        leftAxis.setPosition(YAxisLabelPosition.OUTSIDE_CHART);
        leftAxis.setSpaceTop(15f);
        leftAxis.setAxisMinimum(0f); // this replaces setStartAtZero(true)

        YAxis rightAxis = chart.getAxisRight();
        rightAxis.setDrawGridLines(false);
        rightAxis.setTypeface(tfLight);
        rightAxis.setLabelCount(8, false);
        rightAxis.setValueFormatter(custom);
        rightAxis.setSpaceTop(15f);
        rightAxis.setAxisMinimum(0f); // this replaces setStartAtZero(true)

        Legend l = chart.getLegend();
        l.setVerticalAlignment(Legend.LegendVerticalAlignment.BOTTOM);
        l.setHorizontalAlignment(Legend.LegendHorizontalAlignment.LEFT);
        l.setOrientation(Legend.LegendOrientation.HORIZONTAL);
        l.setDrawInside(false);
        l.setForm(Legend.LegendForm.SQUARE);
        l.setFormSize(9f);
        l.setTextSize(11f);
        l.setXEntrySpace(4f);

        final XYMarkerView mv = new XYMarkerView(getContext(), xAxisFormatter);
        mv.setChartView(chart); // For bounds control
        chart.setMarker(mv); // Set the marker to the chart


        chart.setOnChartValueShortClickListener(new OnChartValueSelectedListener() {
            @Override
            public void onValueSelected(Entry e, Highlight h) {
                Log.w("onMyShortClick", "(" + e.getX() + " | " + e.getY() + ")");
                chart.setMarker(mv); // Set the marker to the chart

            }

            @Override
            public void onNothingSelected() {

            }
        });

        chart.setOnChartValueLongClickListener(new MyBarChart.OnChartValueClickListener() {
            @Override
            public void onChartValueLongClickListener(Entry e) {
                Log.w("onMyLongClick", "(" + e.getX() + " | " + e.getY() + ")");
                chart.setMarker(null); // Set the marker to the chart

            }
        });


        chart.animateY(500);

        setData(count, 10);

        return view;
    }

    private void setTemperatureData(Calendar period) {
        Calendar startPeriod = Calendar.getInstance();
        Toast.makeText(getContext(), startPeriod.get(Calendar.YEAR) + "" + startPeriod.get(Calendar.DAY_OF_MONTH), Toast.LENGTH_SHORT).show();
    }


    private void setData(int count, float range) {

        float start = 1f;
        LineData d = new LineData();

        ArrayList<Entry> entries = new ArrayList<>();

        for (int i = (int) start; i < start + count; i++)
            entries.add(new Entry(i, (float) (Math.random() * (range + 1))));

        LineDataSet set = new LineDataSet(entries, "Line DataSet");
        set.setColor(getContext().getColor(R.color.VSB));
        set.setLineWidth(4f);
        set.setCircleColor(getContext().getColor(R.color.VSB));
        set.setCircleRadius(5f);
        set.setFillColor(getContext().getColor(R.color.VSB));
        set.setMode(LineDataSet.Mode.CUBIC_BEZIER);
        set.setDrawValues(true);
        set.setValueTextSize(10f);
        //set.setValueTextColor(Color.rgb(240, 238, 70));



        set.setAxisDependency(YAxis.AxisDependency.LEFT);
        d.addDataSet(set);


        ArrayList<BarEntry> entries1 = new ArrayList<>();

        for (int index = (int) start; index < start + count; index++) {
            entries1.add(new BarEntry(index, (float) (Math.random() * (range + 1))));
        }

        Log.w(TAG, "BARVA Z COLOR = " + Integer.toHexString(getContext().getColor(R.color.FEI)));

        BarDataSet set1 = new BarDataSet(entries1, "Bar 1");
        set1.setColor(getContext().getColor(R.color.FEI));
        //set1.setValueTextColor(Color.rgb(60, 220, 78));
        set1.setValueTextSize(10f);
        set1.setAxisDependency(YAxis.AxisDependency.LEFT);




        // (0.45 + 0.02) * 2 + 0.06 = 1.00 -> interval per "group"

        BarData e = new BarData(set1);
        e.setBarWidth(barWidth);
        //e.getEntryForHighlight(new Highlight(0,0,0));



        // make this BarData object grouped
        //e.groupBars(0, groupSpace, barSpace); // start at x = 0


            CombinedData data = new CombinedData();
            data.setData(d);
            data.setData(e);
            data.setValueTextSize(10f);
            data.setValueTypeface(tfLight);
            //data.setBarWidth(0.9f);
            data.setHighlightEnabled(true);






            chart.setData(data, data.getBarData().getIndexOfDataSet(set1));
            //chart.highlightValue(1, 0);
            //chart.highlightValue(3, 1);


        }
    }




