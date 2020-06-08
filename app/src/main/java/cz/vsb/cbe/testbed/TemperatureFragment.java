package cz.vsb.cbe.testbed;

import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CompoundButton;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.TimePicker;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;

import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

import cz.vsb.cbe.testbed.sql.TestbedDatabase;
import cz.vsb.cbe.testbed.sql.TestbedDatabase.Record;


public class TemperatureFragment extends Fragment {

    ListView records;
    DatabaseListAdapter recordsAdapter;

    private EditText startDate, startTime, endDate, endTime;
    private Switch dateSwitch;
    private TextView total, median, mean, stdDeviation;

    private Calendar startDateAndTime, endDateAndTime;

    private TestbedDevice testbedDevice;

    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.fragment_temperature, container, false);

        testbedDevice = getArguments().getParcelable(DatabaseActivity.TESTBED_DEVICE);

        startDateAndTime = Calendar.getInstance();
        endDateAndTime = Calendar.getInstance();

        startDate = view.findViewById(R.id.fragment_temperature_etx_start_date);
        startDate.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                new DatePickerDialog(getContext(), new DatePickerDialog.OnDateSetListener() {
                    @Override
                    public void onDateSet(DatePicker view, int year, int month, int dayOfMonth) {
                        startDateAndTime.set(Calendar.YEAR, year);
                        startDateAndTime.set(Calendar.MONTH, month);
                        startDateAndTime.set(Calendar.DAY_OF_MONTH, dayOfMonth);
                        SimpleDateFormat dateFormat = new SimpleDateFormat("dd. MM. YYYY");
                        startDate.setText(dateFormat.format(startDateAndTime.getTime()));
                        funkce();
                    }
                }, startDateAndTime.get(Calendar.YEAR), startDateAndTime.get(Calendar.MONTH), startDateAndTime.get(Calendar.DAY_OF_MONTH)).show();
            }
        });

        endDate = view.findViewById(R.id.fragment_temperature_etx_end_date);
        endDate.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                new DatePickerDialog(getContext(), new DatePickerDialog.OnDateSetListener() {
                    @Override
                    public void onDateSet(DatePicker view, int year, int month, int dayOfMonth) {
                        endDateAndTime.set(Calendar.YEAR, year);
                        endDateAndTime.set(Calendar.MONTH, month);
                        endDateAndTime.set(Calendar.DAY_OF_MONTH, dayOfMonth);
                        SimpleDateFormat dateFormat = new SimpleDateFormat("dd. MM. YYYY");
                        endDate.setText(dateFormat.format(endDateAndTime.getTime()));
                        funkce();
                    }
                }, endDateAndTime.get(Calendar.YEAR), endDateAndTime.get(Calendar.MONTH), endDateAndTime.get(Calendar.DAY_OF_MONTH)).show();
            }
        });

        startTime = view.findViewById(R.id.fragment_temperature_etx_start_time);
        startTime.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                new TimePickerDialog(getContext(), new TimePickerDialog.OnTimeSetListener() {
                    @Override
                    public void onTimeSet(TimePicker view, int hourOfDay, int minute) {
                        startDateAndTime.set(Calendar.HOUR_OF_DAY, hourOfDay);
                        startDateAndTime.set(Calendar.MINUTE, minute);
                        startDateAndTime.set(Calendar.SECOND, 0);
                        SimpleDateFormat dateFormat = new SimpleDateFormat("HH:mm:ss");
                        startTime.setText(dateFormat.format(startDateAndTime.getTime()));
                        funkce();
                    }
                }, startDateAndTime.get(Calendar.HOUR), startDateAndTime.get(Calendar.MINUTE), true).show();
            }
        });

        endTime = view.findViewById(R.id.fragment_temperature_etx_end_time);
        endTime.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                new TimePickerDialog(getContext(), new TimePickerDialog.OnTimeSetListener() {
                    @Override
                    public void onTimeSet(TimePicker view, int hourOfDay, int minute) {
                        endDateAndTime.set(Calendar.HOUR_OF_DAY, hourOfDay);
                        endDateAndTime.set(Calendar.MINUTE, minute);
                        endDateAndTime.set(Calendar.SECOND, 59);
                        SimpleDateFormat dateFormat = new SimpleDateFormat("HH:mm:ss");
                        endTime.setText(dateFormat.format(endDateAndTime.getTime()));
                        funkce();
                    }
                }, endDateAndTime.get(Calendar.HOUR), endDateAndTime.get(Calendar.MINUTE), true).show();
            }
        });

        records = view.findViewById(R.id.fragment_temeprature_lsv);
        recordsAdapter = new DatabaseListAdapter(getLayoutInflater(), "%3.2f", getContext());
        records.setAdapter(recordsAdapter);

        dateSwitch = view.findViewById(R.id.fragment_temperature_sw_now);
        dateSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if(isChecked) {
                    endDate.setEnabled(false);
                    endTime.setEnabled(false);
                    dateSwitch.setText("nyní");
                } else {
                    endDate.setEnabled(true);
                    endTime.setEnabled(true);
                    dateSwitch.setText("data níže");
                }
                funkce();

            }
        });

        total = view.findViewById(R.id.fragment_temperature_txv_total);
        median = view.findViewById(R.id.fragment_temperature_txv_median);
        mean = view.findViewById(R.id.fragment_temperature_txv_mean);
        stdDeviation = view.findViewById(R.id.fragment_temperature_txv_std_deviation);

        return view;
    }

    private boolean isNotEmpty(EditText editText) {
        if (editText.getText().toString().trim().length() > 0)
            return true;
        else
            return false;
    }

    public void funkce(){

        TestbedDatabase.OnSelectTemperatureData onSelectTemperatureData = new TestbedDatabase.OnSelectTemperatureData() {
            @Override
            public void onSelectSuccess(List<Record> records) {
                float values [] = new float [records.size()];
                double stdHelp = 0;
                float sum = 0;
                int index = 0;
                recordsAdapter.clear();
                recordsAdapter.notifyDataSetChanged();
                for(Record record : records){
                    recordsAdapter.addItem(record.getId(), record.getValue(), record.getStringTimeStamp());
                    values[index] = record.getValue();
                    sum = sum + record.getValue();

                    index ++;
                }

                recordsAdapter.notifyDataSetChanged();
                total.setText(String.valueOf(values.length));
                Arrays.sort(values);
                if (values.length % 2 == 0)
                    median.setText(String.format("%3.2f",(values[values.length/2] + values[values.length/2 -1])/2));
                else
                    median.setText(String.format("%3.2f",values[values.length/2]));

                mean.setText(String.format("%3.2f",sum/records.size()));


                for(Record record : records){

                    stdHelp = stdHelp + Math.pow(record.getValue() - sum/records.size(), 2);
                }

                stdDeviation.setText(String.format("%3.2f",Math.sqrt(stdHelp/records.size())));

            }

            @Override
            public void onSelectFailed() {

            }
        };


        if(dateSwitch.isChecked()) {
            if (isNotEmpty(startDate) && isNotEmpty(startTime)) {
                if (startDateAndTime.getTimeInMillis() <= System.currentTimeMillis()){
                    TestbedDatabase.getInstance(getContext()).selectTemperatureData(testbedDevice, startDateAndTime.getTime(), new Date(System.currentTimeMillis()), onSelectTemperatureData);
                }
                else
                    Toast.makeText(getContext(), "Datum nemá smysl tady že", Toast.LENGTH_SHORT).show();
            }
        }
        else {
            if (isNotEmpty(startDate) && isNotEmpty(startTime) && isNotEmpty(endDate) && isNotEmpty(endTime)) {
                if (startDateAndTime.getTimeInMillis() <= endDateAndTime.getTimeInMillis() && endDateAndTime.getTimeInMillis() <= System.currentTimeMillis()) {
                    TestbedDatabase.getInstance(getContext()).selectTemperatureData(testbedDevice, startDateAndTime.getTime(), endDateAndTime.getTime(), onSelectTemperatureData);
                }
                else
                    Toast.makeText(getContext(), "Datum nemá smysl", Toast.LENGTH_SHORT).show();
            }
        }

    }


    @Override
    public void onDetach() {
        super.onDetach();
        //mujAsync.cancel(true)
    }


}
