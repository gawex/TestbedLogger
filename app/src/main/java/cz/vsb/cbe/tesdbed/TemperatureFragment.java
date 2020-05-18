package cz.vsb.cbe.tesdbed;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.github.mikephil.charting.charts.BarChart;
import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.data.BarData;
import com.github.mikephil.charting.data.BarDataSet;
import com.github.mikephil.charting.data.BarEntry;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


public class TemperatureFragment extends Fragment {

    TextView textView;
    BarChart chart;

    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {

        View root = inflater.inflate(R.layout.fragment_temperature, container, false);
        chart = (BarChart) root.findViewById(R.id.chart);

        return root;
    }





    public void postData (List<Map<Date, Float>> data){


        List<BarEntry> entries = new ArrayList<>();
        SimpleDateFormat sdf = new SimpleDateFormat("mm");

        for (Map<Date, Float> entry: data) {
            for (Map.Entry<Date, Float> pair : entry.entrySet()) {
                entries.add(new BarEntry(Float.parseFloat(sdf.format(pair.getKey())), pair.getValue()));
            }
        }

        BarDataSet barDataSet= new BarDataSet(entries, "Label"); // add entries to dataset
        BarData barData = new BarData(barDataSet);
        chart.setData(barData);
        chart.invalidate(); // refresh

        BarDataSet set2 = new BarDataSet(entries, "test");
        BarData barData1 = new BarData(set2);

        }



}
