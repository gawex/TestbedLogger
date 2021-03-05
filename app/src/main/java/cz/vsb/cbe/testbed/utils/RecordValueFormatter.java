/*
  @author  Bc. Lukas Tatarin
 * @supervisor Ing. Jaromir Konecny, Ph.D.
 * @email   lukas@tatarin.cz
 * @version 1.10
 * @ide     Android Studio 4.1.2
 * @license GNU GPL v3
 * @brief   RecordValueFormatter.java
 * @lastmodify 2021/03/05 12:11:17
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

import android.annotation.SuppressLint;

import com.github.mikephil.charting.formatter.ValueFormatter;

import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.Date;

import cz.vsb.cbe.testbed.R;

public class RecordValueFormatter extends ValueFormatter {

    public static final String PATERN_6_0 = "###,###";
    public static final String PATERN_3_0 = "###";
    public static final String PATERN_3_2 = "##0.00";

    public static final String STEPS_SUFFIX = "STEPS";
    public static final String HEART_RATE_SUFFIX = "BPM";
    public static final String TEMPERATURE_SUFFIX = "°C";
    private final int mActualSensor;

    public RecordValueFormatter(int actualSensor) {
        mActualSensor = actualSensor;
    }

    public static String formatSteps(String pattern, Float value) {
        String suffix = " " + STEPS_SUFFIX;
        if (value != null) {
            DecimalFormat decimalFormat = new DecimalFormat(pattern);
            return decimalFormat.format(value) + suffix;
        } else {
            return "--- ---" + suffix;
        }
    }

    public static String formatHearRate(String pattern, Float value) {
        String suffix = " " + HEART_RATE_SUFFIX;
        if (value != null) {
            DecimalFormat decimalFormat = new DecimalFormat(pattern);
            return decimalFormat.format(value) + suffix;
        } else {
            return "---" + suffix;
        }
    }

    public static String formatTemperature(String pattern, Float value) {
        String suffix = " " + TEMPERATURE_SUFFIX;
        if (value != null) {
            DecimalFormat decimalFormat = new DecimalFormat(pattern);
            if (value >= 0) {
                return "+ " + decimalFormat.format(value) + suffix;
            } else {
                return decimalFormat.format(value) + suffix;
            }
        } else {
            return "- ---,--" + suffix;
        }
    }

    public static String formatTimeStampFull(Date date) {
        if (date != null) {
            @SuppressLint("SimpleDateFormat") SimpleDateFormat simpleDateFormat =
                    new SimpleDateFormat("EEEE, d. MMMM yyyy HH:mm:ss");
            return simpleDateFormat.format(date);
        } else {
            return "---";
        }
    }

    public static String formatTimeStampTime(Date date) {
        @SuppressLint("SimpleDateFormat") SimpleDateFormat simpleDateFormat =
                new SimpleDateFormat("HH:mm:ss");
        return simpleDateFormat.format(date);
    }

    @SuppressLint("NonConstantResourceId")
    @Override
    public String getFormattedValue(float value) {
        switch (mActualSensor) {
            case R.id.pedometer:
                return new DecimalFormat(RecordValueFormatter.PATERN_6_0).format(value) +
                        " " + RecordValueFormatter.STEPS_SUFFIX;

            case R.id.heart_rate:
                return new DecimalFormat(RecordValueFormatter.PATERN_3_0).format(value) +
                        " " + RecordValueFormatter.HEART_RATE_SUFFIX;

            case R.id.temperature:
                if (value >= 0) {
                    return "+ " + new DecimalFormat(RecordValueFormatter.PATERN_3_2).format(value) +
                            " " + RecordValueFormatter.TEMPERATURE_SUFFIX;
                } else {
                    return new DecimalFormat(RecordValueFormatter.PATERN_3_2).format(value) +
                            " " + RecordValueFormatter.TEMPERATURE_SUFFIX;
                }

            default:
                throw new IllegalStateException("Unexpected value of mActualSensor: "
                        + mActualSensor);

        }
    }

}
