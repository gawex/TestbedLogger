/*
 * @author  Bc. Lukas Tatarin
 * @supervisor Ing. Jaromir Konecny, Ph.D.
 * @email   lukas@tatarin.cz
 * @version 1.00
 * @ide     Android Studio 4.1.2
 * @license GNU GPL v3
 * @brief   MonthValueFormatter
 * @lastmodify 2021/02/15 12:31:15
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

package cz.vsb.cbe.testbed.chart.axisValueFormater;

import android.content.Context;

import com.github.mikephil.charting.formatter.ValueFormatter;

import cz.vsb.cbe.testbed.R;

public class MonthValueFormater extends ValueFormatter {

    private final String[] Months;
    private final String[] FullMonths;

    public MonthValueFormater(Context context) {
        Months = context.getResources().getStringArray(R.array.months);
        FullMonths = context.getResources().getStringArray(R.array.full_months);
    }

    @Override
    public String getFormattedValue(float value) {
        try{
            return Months[(int) value - 1];
        } catch (ArrayIndexOutOfBoundsException e){
            return "???";
        }
    }

    public String getFullFormattedValue(float value) {
        try{
            return FullMonths[(int) value - 1];
        } catch (ArrayIndexOutOfBoundsException e){
            return "???";
        }
    }
}
