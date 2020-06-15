package cz.vsb.cbe.testbed.fragments;

import android.content.DialogInterface;
import android.graphics.Color;
import android.graphics.RectF;
import android.graphics.Typeface;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.NumberPicker;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.viewpager.widget.ViewPager;

import com.github.mikephil.charting.components.Legend;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.components.YAxis.YAxisLabelPosition;
import com.github.mikephil.charting.data.BarData;
import com.github.mikephil.charting.data.BarDataSet;
import com.github.mikephil.charting.data.BarEntry;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.formatter.ValueFormatter;
import com.github.mikephil.charting.highlight.Highlight;
import com.github.mikephil.charting.interfaces.datasets.IBarDataSet;
import com.github.mikephil.charting.listener.OnChartValueSelectedListener;
import com.github.mikephil.charting.utils.MPPointF;
import com.google.android.material.bottomnavigation.BottomNavigationView;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

import cz.vsb.cbe.testbed.DatabaseActivity;
import cz.vsb.cbe.testbed.TestbedDevice;
import cz.vsb.cbe.testbed.chart.DayAxisValueFormatter;
import cz.vsb.cbe.testbed.R;
import cz.vsb.cbe.testbed.chart.MyBarChart;
import cz.vsb.cbe.testbed.chart.MyValueFormatter;
import cz.vsb.cbe.testbed.chart.XYMarkerView;
import cz.vsb.cbe.testbed.sql.TestbedDatabase;


public class HeartRateFragment extends BaseFragment{


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


        pickerVals = new String[] {"dog", "cat", "lizard", "turtle", "axolotl"};


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

        final Spinner spinner =  dialogViewt.findViewById(R.id.dialog_text_picker);
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

        setData(60, 10);

        return view;
    }

    private void setTemperatureData(Calendar period){
        Calendar startPeriod = Calendar.getInstance();
        Toast.makeText(getContext(), startPeriod.get(Calendar.YEAR) + "" + startPeriod.get(Calendar.DAY_OF_MONTH), Toast.LENGTH_SHORT).show();
    }


    private void setData(int count, float range) {

        /*float start = 1f;

        ArrayList<BarEntry> values = new ArrayList<>();

        for (int i = (int) start; i < start + count; i++) {
            float val = (float) (Math.random() * (range + 1));

            if (Math.random() * 100 < 25) {
                values.add(new BarEntry(i, val));
            } else {
                values.add(new BarEntry(i, val));
            }
        }*/

        Date[] period = setPeriod(2020);
        Log.w(TAG, "Začátek = " + period[0].getTime() + " konec = " + period[1].getTime());
        TestbedDatabase testbedDatabase = TestbedDatabase.getInstance(getContext());
        testbedDatabase.selectTemperatureData(testbedDevice, period[0], period[1], new TestbedDatabase.OnSelectTemperatureData() {
            @Override
            public void onSelectSuccess(List<BarEntry> entries) {

                Toast.makeText(getContext(), "JSEM ZDE A N2CO ASI MAM", Toast.LENGTH_SHORT);

                BarDataSet set1;

                if (chart.getData() != null &&
                        chart.getData().getDataSetCount() > 0) {
                    set1 = (BarDataSet) chart.getData().getDataSetByIndex(0);
                    set1.setValues(entries);
                    chart.getData().notifyDataChanged();
                    chart.notifyDataSetChanged();

                } else {
                    set1 = new BarDataSet(entries, "The year 2017");

                    set1.setDrawIcons(false);

                    int startColor1 = ContextCompat.getColor(getContext(), android.R.color.holo_orange_light);
                    int startColor2 = ContextCompat.getColor(getContext(), android.R.color.holo_blue_light);
                    int startColor3 = ContextCompat.getColor(getContext(), android.R.color.holo_orange_light);
                    int startColor4 = ContextCompat.getColor(getContext(), android.R.color.holo_green_light);
                    int startColor5 = ContextCompat.getColor(getContext(), android.R.color.holo_red_light);
                    int endColor1 = ContextCompat.getColor(getContext(), android.R.color.holo_blue_dark);
                    int endColor2 = ContextCompat.getColor(getContext(), android.R.color.holo_purple);
                    int endColor3 = ContextCompat.getColor(getContext(), android.R.color.holo_green_dark);
                    int endColor4 = ContextCompat.getColor(getContext(), android.R.color.holo_red_dark);
                    int endColor5 = ContextCompat.getColor(getContext(), android.R.color.holo_orange_dark);


                    ArrayList<IBarDataSet> dataSets = new ArrayList<>();
                    dataSets.add(set1);

                    BarData data = new BarData(dataSets);
                    data.setValueTextSize(10f);
                    data.setValueTypeface(tfLight);
                    data.setBarWidth(0.9f);
                    data.setHighlightEnabled(true);


                    chart.setData(data);


                }
            }

            @Override
            public void onSelectFailed() {

            }
        });






        }
    }
/*
    private final RectF onValueSelectedRectF = new RectF();

    @Override
    public void onValueSelected(Entry e, Highlight h) {
        if (e == null)
            return;

        RectF bounds = onValueSelectedRectF;
        chart.getBarBounds((BarEntry) e, bounds);
        MPPointF position = chart.getPosition(e, YAxis.AxisDependency.LEFT);

        Log.i("bounds", bounds.toString());
        Log.i("position", position.x + "|" + position.y);

        Log.i("x-index",
                "low: " + chart.getLowestVisibleX() + ", high: "
                        + chart.getHighestVisibleX());

        Log.i("x ", String.valueOf(e.getX()));

        MPPointF.recycleInstance(position);

    }

    @Override
    public void onNothingSelected() {

    }*/



