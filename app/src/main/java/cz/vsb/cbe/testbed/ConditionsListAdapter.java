package cz.vsb.cbe.testbed;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class ConditionsListAdapter extends BaseAdapter {

    private static final int STATUS = 0;
    private static final int NAME = 1;

    public static final int UNKNOWN = 0;
    public static final int PROGRESS = 1;
    public static final int FAIL = 2;
    public static final int PASS = 3;


    private static final Map<Integer, Object> DEFAULT_MAP = new HashMap<Integer, Object>();
    static {
        DEFAULT_MAP.put(STATUS, UNKNOWN);
        DEFAULT_MAP.put(NAME, "");
    }

    private LayoutInflater LayoutInflater;
    private ArrayList<Map<Integer, Object>> Conditions;
    private Context Context;

    public ConditionsListAdapter(LayoutInflater inflater, Context context, int size) {
        LayoutInflater = inflater ;
        Conditions = new ArrayList<>();
        for(int i = 0; i < size; i++)
            Conditions.add(0, DEFAULT_MAP);
        Context = context;
    }

    public void setCondition (int conditionIndex, int status, String name) {
        final Map<Integer, Object> condition = new HashMap<>(Conditions.get(conditionIndex));
        condition.remove(STATUS);
        condition.remove(NAME);
        condition.put(STATUS, status);
        condition.put(NAME, name);
        Conditions.set(conditionIndex, condition);
    }

    /*public void setConditionName (int conditionIndex, ) {
        final Map<Integer, Object> condition = new HashMap<>(Conditions.get(conditionIndex));
        condition.remove(NAME);
        condition.put(NAME, name);
        Conditions.set(conditionIndex, condition);
    }*/

    @Override
    public int getCount() {
        return Conditions.size();
    }

    @Override
    public Object getItem(int i) {
        return Conditions.get(i);
    }

    @Override
    public long getItemId(int i) {
        return i;
    }

    @Override
    public View getView (int position, View view, ViewGroup parent){
        ViewHolder viewHolder ;
        if (view == null) {
            view = LayoutInflater.inflate(R.layout.list_item_condition, null);
            viewHolder = new ViewHolder();
            viewHolder.item = (TextView) view.findViewById(R.id.conditions_list_txv_item);
            viewHolder.status = (ImageView) view.findViewById(R.id.conditions_list_imv_status);
            viewHolder.progress = (ProgressBar) view.findViewById(R.id.conditions_list_pgb_progress);
            view.setTag(viewHolder);
        } else {
            viewHolder = (ViewHolder) view.getTag();
        }
        Map<Integer, Object> condition = Conditions.get(position);

        if (condition.get(STATUS).equals(UNKNOWN)) {
            viewHolder.status.setVisibility(View.INVISIBLE);
            viewHolder.progress.setVisibility(View.INVISIBLE);
            viewHolder.item.setVisibility(View.INVISIBLE);
        } else if (condition.get(STATUS).equals(PROGRESS)){
            viewHolder.status.setVisibility(View.INVISIBLE);
            viewHolder.progress.setVisibility(View.VISIBLE);
            viewHolder.item.setVisibility(View.VISIBLE);
            viewHolder.item.setText(condition.get(NAME).toString());
        } else if (condition.get(STATUS).equals(PASS)) {
            viewHolder.status.setImageDrawable(Context.getDrawable(R.drawable.ic_pass));
            viewHolder.progress.setVisibility(View.INVISIBLE);
            viewHolder.status.setVisibility(View.VISIBLE);
            viewHolder.item.setVisibility(View.VISIBLE);
            viewHolder.item.setText(condition.get(NAME).toString());
        } else{
            viewHolder.status.setImageDrawable(Context.getDrawable(R.drawable.ic_fail));
            viewHolder.progress.setVisibility(View.INVISIBLE);
            viewHolder.status.setVisibility(View.VISIBLE);
            viewHolder.item.setVisibility(View.VISIBLE);
            viewHolder.item.setText(condition.get(NAME).toString());
        }
        return view;
    }

    static class ViewHolder {
        ImageView status;
        TextView item;
        ProgressBar progress;
    }
}