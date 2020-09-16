package cz.vsb.cbe.testbed.chart;

import android.annotation.SuppressLint;
import android.content.Context;
import android.widget.TextView;

import com.github.mikephil.charting.components.MarkerView;
import com.github.mikephil.charting.data.CandleEntry;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.highlight.Highlight;
import com.github.mikephil.charting.utils.MPPointF;

import java.text.DecimalFormat;
import java.util.Calendar;

import cz.vsb.cbe.testbed.R;
import cz.vsb.cbe.testbed.chart.axisValueFormater.GeneralIntegerValueFormatter;
import cz.vsb.cbe.testbed.chart.axisValueFormater.MonthValueFormater;
import cz.vsb.cbe.testbed.sql.TestbedDatabase;

/**
 * Custom implementation of the MarkerView.
 *
 * @author Philipp Jahoda
 */
@SuppressLint("ViewConstructor")
public class MyMarkerViewNew extends MarkerView {

    private final TextView tvContent;
    private final GeneralIntegerValueFormatter GeneralIntegerValueFormatter;
    private final MonthValueFormater MonthValueFormater;
    private final int RangeLevel;

    private String XLabelAndValue;

    private final DecimalFormat format;
    private final DecimalFormat floatFormat;

    private final String Unit;


    public MyMarkerViewNew(Context context, int rangeLevel, String pattern, String patternFloat, String unit) {
        super(context, R.layout.marker_view_5_lines);
        tvContent = findViewById(R.id.tvContent5);
        GeneralIntegerValueFormatter = new GeneralIntegerValueFormatter();
        GeneralIntegerValueFormatter.setSuffix("");
        MonthValueFormater = new MonthValueFormater(context);
        RangeLevel = rangeLevel;
        format = new DecimalFormat(pattern);
        floatFormat = new DecimalFormat(patternFloat);
        Unit = unit;
    }

    // runs every time the MarkerView is redrawn, can be used to update the
    // content (user-interface)
    @Override
    public void refreshContent(Entry e, Highlight highlight) {
        switch (RangeLevel){
            case Calendar.YEAR:
                GeneralIntegerValueFormatter.setSuffix("");
                XLabelAndValue = "rok: " + GeneralIntegerValueFormatter.getFormattedValue(e.getX());
                break;
            case Calendar.MONTH:
                XLabelAndValue = "měsíc: " + MonthValueFormater.getFullFormattedValue(e.getX());
                break;
            case Calendar.DAY_OF_MONTH:
                GeneralIntegerValueFormatter.setSuffix(".");
                XLabelAndValue = "den: " + GeneralIntegerValueFormatter.getFormattedValue(e.getX());
                break;
            case Calendar.HOUR_OF_DAY:
                GeneralIntegerValueFormatter.setSuffix(".");
                XLabelAndValue = "hodina: " + GeneralIntegerValueFormatter.getFormattedValue(e.getX());
                break;
            case Calendar.MINUTE:
                GeneralIntegerValueFormatter.setSuffix(".");
                XLabelAndValue = "minuta: " + GeneralIntegerValueFormatter.getFormattedValue(e.getX());
                break;
            case Calendar.SECOND:
                GeneralIntegerValueFormatter.setSuffix(".");
                XLabelAndValue = "sekunda: " + GeneralIntegerValueFormatter.getFormattedValue(e.getX());
                break;
            default:
                XLabelAndValue = "x: " + e.getX();
        }
        if(e instanceof CandleEntry){
            CandleEntry candleEntry = (CandleEntry) e;
            tvContent.setText(XLabelAndValue + "\n" +
                    String.format("min: %s %s",format.format(candleEntry.getLow()), Unit) + "\n" +
                    String.format("max: %s %s",format.format(candleEntry.getHigh()), Unit) + "\n" +
                    String.format("DK: %s %s",format.format(candleEntry.getClose()), Unit) + "\n" +
                    String.format("HQ: %s %s",format.format(candleEntry.getOpen()), Unit));

        }
        else {
            tvContent.setText(XLabelAndValue + "\n" +
                    String.format("prům. hod.: %s %s", floatFormat.format(e.getY()), Unit));

        }
        super.refreshContent(e, highlight);

    }

    @Override
    public MPPointF getOffset() {
        return new MPPointF(-(getWidth() / 2), -getHeight());
    }
}