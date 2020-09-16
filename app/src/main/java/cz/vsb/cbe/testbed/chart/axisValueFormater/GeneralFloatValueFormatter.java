package cz.vsb.cbe.testbed.chart.axisValueFormater;

import com.github.mikephil.charting.formatter.ValueFormatter;

import java.text.DecimalFormat;

public class GeneralFloatValueFormatter extends ValueFormatter
{

    private final DecimalFormat DecimalFormat;
    private String Suffix;

    public GeneralFloatValueFormatter() {
        DecimalFormat = new DecimalFormat("###0.00");
    }

    public void setSuffix(String suffix) {
        Suffix = suffix;
    }

    @Override
    public String getFormattedValue(float value) {
        return DecimalFormat.format(value) + Suffix;
    }
}
