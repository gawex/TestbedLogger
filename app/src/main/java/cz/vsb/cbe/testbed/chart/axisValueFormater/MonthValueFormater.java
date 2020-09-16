package cz.vsb.cbe.testbed.chart.axisValueFormater;

import android.content.Context;

import com.github.mikephil.charting.components.AxisBase;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.formatter.ValueFormatter;

import java.text.DecimalFormat;

import cz.vsb.cbe.testbed.R;

public class MonthValueFormater extends ValueFormatter
{
    private final DecimalFormat DecimalFormat;
    private final String[] Months;
    private final String[] FullMonths;

    public MonthValueFormater(Context context) {
        DecimalFormat = new DecimalFormat("####");
        Months = context.getResources().getStringArray(R.array.months);
        FullMonths = context.getResources().getStringArray(R.array.full_months);
    }

    @Override
    public String getFormattedValue(float value) {
        try{
            return Months[(int) value - 1];
        } catch (ArrayIndexOutOfBoundsException e){
            return DecimalFormat.format(value);
        }
    }

    public String getFullFormattedValue(float value) {
        try{
            return FullMonths[(int) value - 1];
        } catch (ArrayIndexOutOfBoundsException e){
            return DecimalFormat.format(value);
        }
    }
}
