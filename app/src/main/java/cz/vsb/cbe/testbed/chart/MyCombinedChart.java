/*
 * @author  Bc. Lukas Tatarin
 * @supervisor Ing. Jaromir Konecny, Ph.D.
 * @email   lukas@tatarin.cz
 * @version 1.00
 * @ide     Android Studio 4.1.2
 * @license GNU GPL v3
 * @brief   MyCombinedChart
 * @lastmodify 2021/02/15 12:37:47
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

package cz.vsb.cbe.testbed.chart;

import android.content.Context;
import android.view.MotionEvent;

import com.github.mikephil.charting.charts.CombinedChart;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.listener.ChartTouchListener;
import com.github.mikephil.charting.listener.OnChartGestureListener;

public class MyCombinedChart extends CombinedChart implements OnChartGestureListener {

    private OnChartValueLongClickListener mOnChartValueLongClickListener;

    public MyCombinedChart(Context context) {
        super(context);
    }

    public void setOnChartValueLongClickListener(OnChartValueLongClickListener onChartValueClickListener) {
        this.mOnChartValueLongClickListener = onChartValueClickListener;
        setOnChartGestureListener(this);
    }

    @Override
    public void onChartLongPressed(MotionEvent me) {
        Entry entry = getEntryByTouchPoint(me.getX(), me.getY());
        mOnChartValueLongClickListener.onChartValueLongClickListener(entry);
    }

    @Override
    public void onChartGestureStart(MotionEvent me, ChartTouchListener.ChartGesture lastPerformedGesture) {

    }

    @Override
    public void onChartGestureEnd(MotionEvent me, ChartTouchListener.ChartGesture lastPerformedGesture) {

    }

    public interface OnChartValueLongClickListener {
        void onChartValueLongClickListener(Entry e);
    }

    @Override
    public void onChartDoubleTapped(MotionEvent me) {

    }

    @Override
    public void onChartSingleTapped(MotionEvent me) {

    }

    @Override
    public void onChartFling(MotionEvent me1, MotionEvent me2, float velocityX, float velocityY) {

    }

    @Override
    public void onChartScale(MotionEvent me, float scaleX, float scaleY) {

    }

    @Override
    public void onChartTranslate(MotionEvent me, float dX, float dY) {

    }

}
