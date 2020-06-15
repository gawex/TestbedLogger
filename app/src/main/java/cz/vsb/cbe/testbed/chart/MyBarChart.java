package cz.vsb.cbe.testbed.chart;

import android.content.Context;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;

import androidx.annotation.InspectableProperty;

import com.github.mikephil.charting.charts.BarChart;
import com.github.mikephil.charting.components.YAxis.AxisDependency;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.highlight.BarHighlighter;
import com.github.mikephil.charting.highlight.Highlight;
import com.github.mikephil.charting.listener.ChartTouchListener;
import com.github.mikephil.charting.listener.OnChartGestureListener;
import com.github.mikephil.charting.listener.OnChartValueSelectedListener;
import com.github.mikephil.charting.utils.MPPointD;

public class MyBarChart extends BarChart implements OnChartGestureListener  {

    public MyBarChart(Context context) {
        super(context);
    }

    public MyBarChart(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public MyBarChart(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    @Override
    public void setOnChartValueSelectedListener(OnChartValueSelectedListener onChartValueSelectedListener) {
        throw new UnsupportedOperationException();
    }

    public void setOnChartValueShortClickListener(OnChartValueSelectedListener onChartValueSelectedListener) {
        super.setOnChartValueSelectedListener(onChartValueSelectedListener);
    }

    public void setOnChartValueLongClickListener(OnChartValueClickListener onChartValueClickListener) {
        this.OnChartValueClickListener = onChartValueClickListener;
        setOnChartGestureListener(this);
    }

    public interface OnChartValueClickListener {
        void onChartValueLongClickListener(Entry e);
    };

    OnChartValueClickListener OnChartValueClickListener;

    @Override
    public void onChartGestureStart(MotionEvent me, ChartTouchListener.ChartGesture lastPerformedGesture) {

    }

    @Override
    public void onChartGestureEnd(MotionEvent me, ChartTouchListener.ChartGesture lastPerformedGesture) {

    }

    @Override
    public void onChartLongPressed(MotionEvent me) {
        Entry entry = getEntryByTouchPoint(me.getX(), me.getY());
        Highlight highlight = new Highlight(entry.getX(), 0 ,0);
        highlightValue(highlight, false);
        OnChartValueClickListener.onChartValueLongClickListener(entry);
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
