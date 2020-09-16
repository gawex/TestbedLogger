package cz.vsb.cbe.testbed.chart.axisValueFormater;

import com.github.mikephil.charting.components.AxisBase;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.formatter.ValueFormatter;

import java.text.DecimalFormat;

public class GeneralIntegerValueFormatter extends ValueFormatter
{

    private final DecimalFormat DecimalFormat;
    private String Suffix;

    public GeneralIntegerValueFormatter() {
        DecimalFormat = new DecimalFormat("####");
    }

    public void setSuffix(String suffix) {
        Suffix = suffix;
    }

    @Override
    public String getFormattedValue(float value) {
        return DecimalFormat.format(value) + Suffix;
    }
}
