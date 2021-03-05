/*
  @author  Bc. Lukas Tatarin
 * @supervisor Ing. Jaromir Konecny, Ph.D.
 * @email   lukas@tatarin.cz
 * @version 1.10
 * @ide     Android Studio 4.1.2
 * @license GNU GPL v3
 * @brief   IntervalValueFormatter.java
 * @lastmodify 2021/03/05 12:10:11
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

package cz.vsb.cbe.testbed.utils;

import android.content.Context;

import com.github.mikephil.charting.formatter.ValueFormatter;

import java.text.DecimalFormat;
import java.util.Calendar;

import cz.vsb.cbe.testbed.R;

public class IntervalValueFormatter extends ValueFormatter {

    public static final String PATERN_4_0 = "####";
    public static final String PATERN_1_0 = "##";

    private final Context mContext;
    private final int mActualSortingLevel;
    private final String[] mMonths;

    public IntervalValueFormatter(Context context, int actualSortingLevel) {
        mContext = context;
        mActualSortingLevel = actualSortingLevel;
        mMonths = context.getResources().getStringArray(R.array.months);
    }

    public static String formatIntervalLevel(Context context, int intervalLevel, float value) {
        DecimalFormat decimalFormat;
        switch (intervalLevel) {
            case Calendar.YEAR:
                decimalFormat = new DecimalFormat(PATERN_4_0);
                return "rok: " + decimalFormat.format(value);

            case Calendar.MONTH:
                String[] months = context.getResources().getStringArray(R.array.full_months);
                try {
                    return "měsíc: " + months[(int) value - 1];
                } catch (ArrayIndexOutOfBoundsException e) {
                    return "měsíc: ???";
                }

            case Calendar.DAY_OF_MONTH:
                decimalFormat = new DecimalFormat(PATERN_1_0);
                return "den: " + decimalFormat.format(value) + ".";

            case Calendar.HOUR_OF_DAY:
                decimalFormat = new DecimalFormat(PATERN_1_0);
                return "hodina: " + decimalFormat.format(value) + ".";

            case Calendar.MINUTE:
                decimalFormat = new DecimalFormat(PATERN_1_0);
                return "minuta: " + decimalFormat.format(value) + ".";

            case Calendar.SECOND:
                decimalFormat = new DecimalFormat(PATERN_1_0);
                return "sekunda: " + decimalFormat.format(value) + ".";

            default:
                throw new IllegalStateException("Unexpected value of intervalLevel: "
                        + intervalLevel);
        }
    }

    @Override
    public String getFormattedValue(float value) {
        switch (mActualSortingLevel) {
            case Calendar.YEAR:
                return new DecimalFormat(IntervalValueFormatter.PATERN_4_0).format(value);

            case Calendar.MONTH:
                try {
                    return mMonths[(int) value - 1];
                } catch (ArrayIndexOutOfBoundsException e) {
                    return "???";
                }

            case Calendar.DAY_OF_MONTH:
                return new DecimalFormat(IntervalValueFormatter.PATERN_1_0).format(value) + ".";

            case Calendar.HOUR_OF_DAY:
                return new DecimalFormat(IntervalValueFormatter.PATERN_1_0).format(value) + " " +
                        mContext.getString(R.string.interval_value_formatter_hour_label_short);

            case Calendar.MINUTE:
                return new DecimalFormat(IntervalValueFormatter.PATERN_1_0).format(value) + " " +
                        mContext.getString(R.string.interval_value_formatter_minute_label_short);

            case Calendar.SECOND:
                return new DecimalFormat(IntervalValueFormatter.PATERN_1_0).format(value) + " " +
                        mContext.getString(R.string.interval_value_formatter_second_label_short);

            default:
                throw new IllegalStateException("Unexpected value of mActualSortingLevel: " +
                        mActualSortingLevel);
        }

    }

}
