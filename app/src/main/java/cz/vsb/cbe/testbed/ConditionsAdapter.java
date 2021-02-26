/*
  @author  Bc. Lukas Tatarin
 * @supervisor Ing. Jaromir Konecny, Ph.D.
 * @email   lukas@tatarin.cz
 * @version 1.00
 * @ide     Android Studio 4.1.2
 * @license GNU GPL v3
 * @brief   ConditionsAdapter.java
 * @lastmodify 2021/02/26 13:28:44
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

package cz.vsb.cbe.testbed;

import android.annotation.SuppressLint;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.appcompat.content.res.AppCompatResources;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

public class ConditionsAdapter extends BaseAdapter {

    public static final int UNKNOWN = 0;
    public static final int IN_PROGRESS = 1;
    public static final int FAIL = 2;
    public static final int PASS = 3;
    private static final int STATUS = 0;
    private static final int LABEL = 1;
    private static final Map<Integer, Object> DEFAULT_MAP = new HashMap<>();

    static {
        DEFAULT_MAP.put(STATUS, UNKNOWN);
        DEFAULT_MAP.put(LABEL, "");
    }

    private final LayoutInflater mLayoutInflater;
    private final ArrayList<Map<Integer, Object>> mConditions;
    private final Context mContext;

    public ConditionsAdapter(LayoutInflater inflater, Context context, int size) {
        mLayoutInflater = inflater;
        mContext = context;
        mConditions = new ArrayList<>();
        for (int i = 0; i < size; i++)
            mConditions.add(0, DEFAULT_MAP);

    }

    public void setCondition(int conditionIndex, int status, String name) {
        final Map<Integer, Object> condition = new HashMap<>(mConditions.get(conditionIndex));
        condition.remove(STATUS);
        condition.remove(LABEL);
        condition.put(STATUS, status);
        condition.put(LABEL, name);
        mConditions.set(conditionIndex, condition);
    }

    @Override
    public int getCount() {
        return mConditions.size();
    }

    @Override
    public Object getItem(int i) {
        return mConditions.get(i);
    }

    @Override
    public long getItemId(int i) {
        return i;
    }

    @SuppressLint("InflateParams")
    @Override
    public View getView(int position, View view, ViewGroup parent) {
        ViewHolder viewHolder;
        if (view == null) {
            view = mLayoutInflater.inflate(R.layout.list_item_condition, null);
            viewHolder = new ViewHolder();
            viewHolder.mTxvLabel = view.findViewById(R.id.list_item_condition_txv_condition);
            viewHolder.mImvStatus = view.findViewById(R.id.list_item_condition_imv_status);
            viewHolder.mPgbProgress = view.findViewById(R.id.list_item_condition_pgb_progress);
            view.setTag(viewHolder);
        } else {
            viewHolder = (ViewHolder) view.getTag();
        }
        Map<Integer, Object> condition = mConditions.get(position);

        if (Objects.requireNonNull(condition.get(STATUS)).equals(UNKNOWN)) {
            viewHolder.mImvStatus.setVisibility(View.INVISIBLE);
            viewHolder.mPgbProgress.setVisibility(View.INVISIBLE);
            viewHolder.mTxvLabel.setVisibility(View.INVISIBLE);
        } else if (Objects.requireNonNull(condition.get(STATUS)).equals(IN_PROGRESS)) {
            viewHolder.mImvStatus.setVisibility(View.INVISIBLE);
            viewHolder.mPgbProgress.setVisibility(View.VISIBLE);
            viewHolder.mTxvLabel.setVisibility(View.VISIBLE);
            viewHolder.mTxvLabel.setText(Objects.requireNonNull(condition.get(LABEL)).toString());
        } else if (Objects.requireNonNull(condition.get(STATUS)).equals(PASS)) {
            viewHolder.mImvStatus.setImageDrawable(
                    AppCompatResources.getDrawable(mContext, R.drawable.ic_pass_20dp_color_vsb));
            viewHolder.mPgbProgress.setVisibility(View.INVISIBLE);
            viewHolder.mImvStatus.setVisibility(View.VISIBLE);
            viewHolder.mTxvLabel.setVisibility(View.VISIBLE);
            viewHolder.mTxvLabel.setText(Objects.requireNonNull(condition.get(LABEL)).toString());
        } else {
            viewHolder.mImvStatus.setImageDrawable(
                    AppCompatResources.getDrawable(mContext, R.drawable.ic_fail_20dp_color_red));
            viewHolder.mPgbProgress.setVisibility(View.INVISIBLE);
            viewHolder.mImvStatus.setVisibility(View.VISIBLE);
            viewHolder.mTxvLabel.setVisibility(View.VISIBLE);
            viewHolder.mTxvLabel.setText(Objects.requireNonNull(condition.get(LABEL)).toString());
        }
        return view;
    }

    static class ViewHolder {
        ImageView mImvStatus;
        TextView mTxvLabel;
        ProgressBar mPgbProgress;
    }
}