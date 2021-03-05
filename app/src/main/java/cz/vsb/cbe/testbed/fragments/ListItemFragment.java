/*
  @author  Bc. Lukas Tatarin
 * @supervisor Ing. Jaromir Konecny, Ph.D.
 * @email   lukas@tatarin.cz
 * @version 1.10
 * @ide     Android Studio 4.1.2
 * @license GNU GPL v3
 * @brief   ListItemFragment.java
 * @lastmodify 2021/03/05 11:56:13
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

package cz.vsb.cbe.testbed.fragments;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.content.res.AppCompatResources;

import java.util.Calendar;
import java.util.Date;
import java.util.List;

import cz.vsb.cbe.testbed.R;
import cz.vsb.cbe.testbed.activitiesAndServices.BluetoothLeService;
import cz.vsb.cbe.testbed.activitiesAndServices.DatabaseActivity;
import cz.vsb.cbe.testbed.adapters.RecodrsAdapter;
import cz.vsb.cbe.testbed.adapters.RecordDetailsAdapter;
import cz.vsb.cbe.testbed.sql.DatabaseResult;
import cz.vsb.cbe.testbed.sql.Record;
import cz.vsb.cbe.testbed.sql.TestbedDatabase;
import cz.vsb.cbe.testbed.utils.StatisticalData;

public class ListItemFragment extends BaseVisualisationFragment {

    private ListView mLsvRecords;
    private RecodrsAdapter mRecodrsAdapter;

    private TextView mTxvSortBy;
    private TextView mTxvSortOrder;

    private TestbedDatabase.SORT_BY mSortBy;
    private TestbedDatabase.SORT_ORDER mSortOrder;


    private final OnChangeActualIntervaListener mOnChangeActualIntervaListener =
            this::updateListViewData;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mDatabaseActivity = (DatabaseActivity) getActivity();
        assert mDatabaseActivity != null;
        mTestbedDatabase = mDatabaseActivity.getTestbedDatabase();
        mTestbedDevice = mDatabaseActivity.getTestbedDevice();
    }

    @Override
    public View onCreateView(final LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_list_view, container, false);

        mImbDecrementActualInterval = view.findViewById(R.id.fragment_list_view_imb_decrement_sorting);
        mImbDecrementActualInterval.setOnClickListener(v -> {
            decrementSorting();
            updateListViewData();
        });

        mTxvActualInterval = view.findViewById(R.id.fragment_list_view_txv_actual_interval);

        mImbChangeInterval = view.findViewById(R.id.fragment_list_view_imb_change_actual_interval);
        mImbChangeInterval.setOnClickListener(v -> {
            mDatabaseActivity.getActualSortingInterval().setTime(new Date());
            mDatabaseActivity.setActualSortingLevel(Calendar.HOUR_OF_DAY);
            updateListViewData();
        });
        mImbChangeInterval.setOnLongClickListener(v -> {
            showChangeIntervalDialog(mOnChangeActualIntervaListener);
            return true;
        });

        mLsvRecords = view.findViewById(R.id.fragment_list_view_lsv);
        mRecodrsAdapter = new RecodrsAdapter(inflater);
        mLsvRecords.setAdapter(mRecodrsAdapter);
        mLsvRecords.setOnItemClickListener((parent, view1, position, id) -> {
            View recordDetailsDialogView = getLayoutInflater()
                    .inflate(R.layout.dialog_general_list_view, null);
            ListView lsvRecordDetaisl = recordDetailsDialogView.findViewById(R.id.dialog_list_view_lsv);
            RecordDetailsAdapter recordDetailsAdapter = new RecordDetailsAdapter(inflater, getContext());
            lsvRecordDetaisl.setAdapter(recordDetailsAdapter);
            recordDetailsAdapter.setRecord(mRecodrsAdapter.getRecord(position));
            recordDetailsAdapter.notifyDataSetChanged();

            AlertDialog recordDetailsDialog = new AlertDialog.Builder(getContext())
                    .setIcon(R.drawable.ic_magnifier_20dp_color_vsb)
                    .setTitle(getString(R.string.record_details_dialog_title))
                    .setView(recordDetailsDialogView)
                    .setNeutralButton(getString(R.string.record_details_dialog_neutral_button), null)
                    .create();
            recordDetailsDialog.show();
        });

        mLsvRecords.setOnItemLongClickListener((parent, view12, position, id) -> {
            Calendar calendar = Calendar.getInstance();
            calendar.setTime(mRecodrsAdapter.getRecord(position).getTimeStamp());
            switch (mDatabaseActivity.getActualSortingLevel()) {
                case Calendar.YEAR:
                    mDatabaseActivity.getActualSortingInterval().set(Calendar.YEAR, calendar.get(Calendar.YEAR));
                    mDatabaseActivity.setActualSortingLevel(Calendar.MONTH);
                    updateListViewData();
                    break;

                case Calendar.MONTH:
                    mDatabaseActivity.getActualSortingInterval().set(Calendar.MONTH, calendar.get(Calendar.MONTH));
                    mDatabaseActivity.setActualSortingLevel(Calendar.DAY_OF_MONTH);
                    updateListViewData();
                    break;

                case Calendar.DAY_OF_MONTH:
                    mDatabaseActivity.getActualSortingInterval().set(Calendar.DAY_OF_MONTH, calendar.get(Calendar.DAY_OF_MONTH));
                    mDatabaseActivity.setActualSortingLevel(Calendar.HOUR_OF_DAY);
                    updateListViewData();
                    break;

                case Calendar.HOUR_OF_DAY:
                    mDatabaseActivity.getActualSortingInterval().set(Calendar.HOUR_OF_DAY, calendar.get(Calendar.HOUR_OF_DAY));
                    mDatabaseActivity.setActualSortingLevel(Calendar.MINUTE);
                    updateListViewData();
                    break;

                case Calendar.MINUTE:
                    mDatabaseActivity.getActualSortingInterval().set(Calendar.MINUTE, calendar.get(Calendar.MINUTE));
                    mDatabaseActivity.setActualSortingLevel(Calendar.SECOND);
                    updateListViewData();
                    break;

                case Calendar.SECOND:
                    Toast.makeText(getContext(), getString(
                            R.string.fragment_list_item_max_interval_scale),
                            Toast.LENGTH_SHORT).show();
                    break;

                default:
                    throw new IllegalStateException(
                            "Unexpected value of mDatabaseActivity.getActualSortingLevel(): " +
                                    mDatabaseActivity.getActualSortingLevel());
            }
            return true;
        });

        mTxvNoDataAvailableLabel = view
                .findViewById(R.id.fragment_list_view_txv_no_data_available_label);
        mTxvNoDataAvailableInterval = view
                .findViewById(R.id.fragment_chart_view_txv_no_data_available_interval);
        mBtnChangeInterval = view.findViewById(R.id.fragment_list_view_btn_change_actual_interval);
        mBtnChangeInterval.setOnClickListener(
                v -> showChangeIntervalDialog(mOnChangeActualIntervaListener));

        mPgbWorking = view.findViewById(R.id.fragment_list_view_pgb_working);

        mSortBy = TestbedDatabase.SORT_BY.TIME;
        mTxvSortBy = view.findViewById(R.id.fragment_list_view_txv_sort_by);
        mTxvSortBy.setText(getString(R.string.fragment_list_item_sort_by_time));
        mTxvSortBy.setOnClickListener(v -> sortingByChanged());

        mSortOrder = TestbedDatabase.SORT_ORDER.ASC;
        mTxvSortOrder = view.findViewById(R.id.fragment_list_view_txv_sort_order);
        mTxvSortOrder.setText(getString(R.string.fragment_list_item_sort_order_asc));
        mTxvSortOrder.setOnClickListener(v -> sortingOrderChanged());

        mImbShowStatisticsInfo = view.findViewById(R.id.fragment_list_view_imb_statistic_info);
        mImbShowStatisticsInfo.setOnClickListener(
                v -> showStatisticalDataDialog(getContext(), new StatisticalData(mRecords)));

        return view;
    }

    @Override
    public void onResume() {
        super.onResume();
        updateListViewData();
    }

    @SuppressWarnings("unchecked")
    @SuppressLint("NonConstantResourceId")
    public void updateListViewData() {
        Calendar actualSortingInterval = mDatabaseActivity.getActualSortingInterval();
        int actualSortingLevel = mDatabaseActivity.getActualSortingLevel();
        switch (mDatabaseActivity.getActualSelectedSensor()) {
            case R.id.pedometer:
                dataLoading(true);
                Date[] intervals = getIntervals(actualSortingInterval, actualSortingLevel);
                mTestbedDatabase.selectRecordsBetweenTimeStamp(
                        mTestbedDevice,
                        BluetoothLeService.STEPS_DATA,
                        intervals,
                        mSortBy, mSortOrder, databaseResult -> {
                            if (databaseResult instanceof DatabaseResult.Success) {
                                List<Record> selectedRecords =
                                        ((DatabaseResult.Success<List<Record>>) databaseResult).data;
                                mTestbedDatabase.selectFirstRecordLessThanTimeStamp(mTestbedDevice,
                                        BluetoothLeService.STEPS_DATA, intervals[0], databaseResult1 -> {
                                            Record lastRecordBeforeInterval = null;
                                            if (databaseResult1 instanceof DatabaseResult.Success) {
                                                lastRecordBeforeInterval =
                                                        ((DatabaseResult.Success<Record>) databaseResult1).data;
                                            } else if (databaseResult1 instanceof DatabaseResult.Error) {
                                                lastRecordBeforeInterval = new Record();
                                            }
                                            mRecords = sortSteps(lastRecordBeforeInterval, selectedRecords);
                                            setData(selectedRecords);
                                            dataAvailable(true);
                                        });
                            } else if (databaseResult instanceof DatabaseResult.Error) {
                                dataAvailable(false);
                            }
                        });
                break;

            case R.id.heart_rate:
                dataLoading(true);
                mTestbedDatabase.selectRecordsBetweenTimeStamp(mTestbedDevice,
                        BluetoothLeService.HEART_RATE_DATA,
                        getIntervals(actualSortingInterval, actualSortingLevel),
                        mSortBy, mSortOrder, databaseResult -> {
                            if (databaseResult instanceof DatabaseResult.Success) {
                                mRecords = ((DatabaseResult.Success<List<Record>>) databaseResult).data;
                                setData(mRecords);
                                dataAvailable(true);
                            } else if (databaseResult instanceof DatabaseResult.Error) {
                                dataAvailable(false);
                            }
                        });
                break;

            case R.id.temperature:
                dataLoading(true);
                mTestbedDatabase.selectRecordsBetweenTimeStamp(mTestbedDevice,
                        BluetoothLeService.TEMPERATURE_DATA,
                        getIntervals(actualSortingInterval, actualSortingLevel),
                        mSortBy, mSortOrder, databaseResult -> {
                            if (databaseResult instanceof DatabaseResult.Success) {
                                mRecords = ((DatabaseResult.Success<List<Record>>) databaseResult).data;
                                setData(mRecords);
                                dataAvailable(true);
                            } else if (databaseResult instanceof DatabaseResult.Error) {
                                dataAvailable(false);
                            }
                        });
                break;
        }
    }

    public void setData(List<Record> rawRecords) {
        mDatabaseActivity.runOnUiThread(() -> {
            mRecodrsAdapter.clearAndAddRecords(rawRecords,
                    mDatabaseActivity.getActualSelectedSensor());
            mTxvActualInterval.setText(formatIntervalLabel());
            mRecodrsAdapter.notifyDataSetChanged();
        });
    }

    @Override
    public void dataAvailable(boolean available) {
        super.dataAvailable(available);
        mDatabaseActivity.runOnUiThread(() -> {
            if (available) {
                mLsvRecords.setVisibility(View.VISIBLE);
                mTxvSortBy.setVisibility(View.VISIBLE);
                mTxvSortOrder.setVisibility(View.VISIBLE);
            } else {
                mLsvRecords.setVisibility(View.INVISIBLE);
                mTxvSortBy.setVisibility(View.INVISIBLE);
                mTxvSortOrder.setVisibility(View.INVISIBLE);
            }
        });
    }

    private void sortingByChanged() {
        if (mSortBy == TestbedDatabase.SORT_BY.TIME) {
            mSortBy = TestbedDatabase.SORT_BY.VALUE;
            mTxvSortBy.setText(getString(R.string.fragment_list_item_sort_by_value));
            mTxvSortBy.setBackground(AppCompatResources.getDrawable(requireContext(), R.color.ColorFei));
        } else {
            mSortBy = TestbedDatabase.SORT_BY.TIME;
            mTxvSortBy.setText(getString(R.string.fragment_list_item_sort_by_time));
            mTxvSortBy.setBackground(AppCompatResources.getDrawable(requireContext(), R.color.ColorVsb));
        }
        updateListViewData();
    }

    private void sortingOrderChanged() {
        if (mSortOrder == TestbedDatabase.SORT_ORDER.ASC) {
            mSortOrder = TestbedDatabase.SORT_ORDER.DESC;
            mTxvSortOrder.setText(getString(R.string.fragment_list_item_sort_order_desc));
            mTxvSortOrder.setBackground(AppCompatResources.getDrawable(requireContext(), R.color.ColorFei));
        } else {
            mSortOrder = TestbedDatabase.SORT_ORDER.ASC;
            mTxvSortOrder.setText(getString(R.string.fragment_list_item_sort_order_asc));
            mTxvSortOrder.setBackground(AppCompatResources.getDrawable(requireContext(), R.color.ColorVsb));
        }
        updateListViewData();
    }
}