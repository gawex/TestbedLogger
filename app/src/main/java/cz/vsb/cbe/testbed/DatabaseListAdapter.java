package cz.vsb.cbe.testbed;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class DatabaseListAdapter extends BaseAdapter {

    private static final int ID = 0;
    private static final int VALUE = 1;
    private static final int TIME_STAMP = 2;

    private LayoutInflater LayoutInflater;
    private List<Map<Integer, Object>> Records;
    private String FloatFormat;
    private Context Context;

    public DatabaseListAdapter(LayoutInflater inflater, String floatFormat, Context context) {
        FloatFormat = floatFormat;
        LayoutInflater = inflater ;
        Records = new ArrayList<>();
        Context = context;
    }

    public void addItem (int id, float value, String timeStamp) {
        final Map<Integer, Object> record = new HashMap<>();
        record.put(ID, id);
        record.put(VALUE, value);
        record.put(TIME_STAMP, timeStamp);
        Records.add(record);
    }


    public void clear(){
        Records.clear();
    }

    @Override
    public int getCount() {
        return Records.size();
    }

    @Override
    public Object getItem(int i) {
        return Records.get(i);
    }

    @Override
    public long getItemId(int i) {
        return i;
    }

    @Override
    public View getView (int position, View view, ViewGroup parent){
        ViewHolder viewHolder ;
        if (view == null) {
            view = LayoutInflater.inflate(R.layout.list_item_database, null);
            viewHolder = new ViewHolder();
            viewHolder.id = view.findViewById(R.id.id);
            viewHolder.value = view.findViewById(R.id.value);
            viewHolder.timeStamp = view.findViewById(R.id.time_stamp);
            view.setTag(viewHolder);
        } else {
            viewHolder = (ViewHolder) view.getTag();
        }
        Map<Integer, Object> record = Records.get(position);
        viewHolder.id.setText(record.get(ID).toString());
        viewHolder.value.setText(String.format(FloatFormat, record.get(VALUE)));
        viewHolder.timeStamp.setText(record.get(TIME_STAMP).toString());

        return view;
    }

    static class ViewHolder {
        TextView id;
        TextView value;
        TextView timeStamp;
    }
}