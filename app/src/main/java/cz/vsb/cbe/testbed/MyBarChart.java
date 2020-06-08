package cz.vsb.cbe.testbed;

import android.content.Context;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;

import com.github.mikephil.charting.charts.BarChart;
import com.github.mikephil.charting.components.YAxis.AxisDependency;
import com.github.mikephil.charting.listener.ChartTouchListener;
import com.github.mikephil.charting.listener.OnChartGestureListener;
import com.github.mikephil.charting.listener.OnChartValueSelectedListener;
import com.github.mikephil.charting.utils.MPPointD;

public class MyBarChart extends BarChart implements OnChartGestureListener  {


    public void setOnChartValueSelectedListenerMuj(OnChartValueSelectedListener l) {
        super.setOnChartValueSelectedListener(l);
    }

    @Override
    public void setOnChartValueSelectedListener(OnChartValueSelectedListener l) {
        super.setOnChartValueSelectedListener(l);
    }

    public MyBarChart(Context context) {
        super(context);
    }

    public MyBarChart(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public MyBarChart(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    public interface mujInterface{
        void onMyLongClick(double x, double y);
    };

    mujInterface MujInterface;
    private BarChart BarChart;


    public void setOnMujInteface( mujInterface l) {
        this.MujInterface = l;
        this.setOnChartGestureListener(this);
    }



    @Override
    public void onChartGestureStart(MotionEvent me, ChartTouchListener.ChartGesture lastPerformedGesture) {

    }

    @Override
    public void onChartGestureEnd(MotionEvent me, ChartTouchListener.ChartGesture lastPerformedGesture) {

    }

    @Override
    public void onChartLongPressed(MotionEvent me) {
        float tappedX = me.getX();
        float tappedY = me.getY();
        MPPointD point = this.getTransformer(AxisDependency.LEFT).getValuesByTouchPoint(tappedX, tappedY);
        MujInterface.onMyLongClick(point.x, point.y);
        Log.i("ZIJU", "JO");
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
