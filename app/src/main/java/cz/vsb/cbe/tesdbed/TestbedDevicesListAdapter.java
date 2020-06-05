package cz.vsb.cbe.tesdbed;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;

import cz.vsb.cbe.tesdbed.sql.TestbedDatabase;

public class TestbedDevicesListAdapter extends BaseAdapter {

    private static final int EXCELLENT_SIGNAL_LIMIT = -70;
    private static final int GOOD_SIGNAL_LIMIT = -85;
    private static final int POOR_SIGNAL_LIMIT = -100;

    private LayoutInflater LayoutInflater;
    private List<TestbedDevice> TestbedDevices;
    private Context Context;
    private int storedStatus;

    public TestbedDevicesListAdapter(LayoutInflater inflater, Context context) {
        LayoutInflater = inflater ;
        TestbedDevices = new ArrayList<>();
        Context = context;
    }

    public void setDevice(TestbedDevice testbedDevice){
        boolean existAtLeastOneTestBedDevice = false;
        int position = -1;
        for(int index = 0; index < TestbedDevices.size(); index++) {
            if (TestbedDevices.get(index).getBluetoothDevice().getAddress().equals(testbedDevice.getBluetoothDevice().getAddress())) {
                existAtLeastOneTestBedDevice = true;
                position = index;
                break;
            }
        }

        if(existAtLeastOneTestBedDevice && position != -1)
            TestbedDevices.set(position, testbedDevice);
        else
            TestbedDevices.add(testbedDevice);
    }

    public void removeDevices(List<TestbedDevice> testbedDevices){
            TestbedDevices.removeAll(testbedDevices);
    }

    public TestbedDevice getDevice(int position) {
        return TestbedDevices.get(position);
    }

    public List<TestbedDevice> getDevices(){
        return TestbedDevices;
    }

    public void clear() {
        TestbedDevices.clear();
    }

    @Override
    public int getCount() {
        return TestbedDevices.size();
    }

    @Override
    public Object getItem(int i) {
        return TestbedDevices.get(i);
    }

    @Override
    public long getItemId(int i) {
        return i;
    }

    @Override
    public View getView (int position, View view, ViewGroup parent){
        ViewHolder viewHolder ;
        // General ListView optimization code .
        if (view == null) {
            view = LayoutInflater.inflate(R.layout.list_item_device, null);
            viewHolder = new ViewHolder();
            viewHolder.name = (TextView) view.findViewById(R.id.devices_list_txv_name);
            viewHolder.databaseIcon = (ImageView) view.findViewById(R.id.devices_list_imv_database);
            viewHolder.id = (TextView) view.findViewById(R.id.devices_list_txv_id);
            viewHolder.signalStrenght = (ImageView) view.findViewById(R.id.device_list_imv_signal_strenght);
            viewHolder.rssi = (TextView) view.findViewById(R.id.device_list_txv_rssi);
            viewHolder.pedometer = (ImageView) view.findViewById(R.id.devices_list_imv_pedometer);
            viewHolder.heartRateMeter = (ImageView) view.findViewById(R.id.devices_list_imv_heart_rate_meter);
            viewHolder.thermometer= (ImageView) view.findViewById(R.id.devices_list_imv_thermometer);
            viewHolder.macAddress = (TextView) view.findViewById(R.id.devices_list_txv_mac_address);
            view.setTag(viewHolder);
        }else{
            viewHolder = (ViewHolder) view.getTag();
        }
        TestbedDevice item = TestbedDevices.get(position);
        viewHolder.name.setText(item.getBluetoothDevice().getName());
        if(item.getDeviceId() == 0x1FFFF) {
            viewHolder.databaseIcon.setImageDrawable(Context.getDrawable(android.R.color.transparent));
            viewHolder.id.setText(null);
        } else {
            if (item.getStoredStatus() == TestbedDevice.STORED_CONSISTENTLY)
                viewHolder.databaseIcon.setImageDrawable(Context.getDrawable(R.drawable.ic_database_known));
            else if (item.getStoredStatus() == TestbedDevice.STORED_BUT_MODIFIED)
                viewHolder.databaseIcon.setImageDrawable((Context.getDrawable(R.drawable.ic_database_crashed)));
            else
                viewHolder.databaseIcon.setImageDrawable(Context.getDrawable(R.drawable.ic_database_unknown));
            String id = String.format("%04X", item.getDeviceId());
            id = id.replace("B","b");
            id = id.replace("D","d");
            viewHolder.id.setText(id);
        }

        if (item.getRssi() > EXCELLENT_SIGNAL_LIMIT){
            viewHolder.signalStrenght.setImageDrawable(Context.getDrawable(R.drawable.ic_signal_excelent));
            viewHolder.rssi.setText(item.getRssi() + " dBm");
        } else if (item.getRssi() <= EXCELLENT_SIGNAL_LIMIT && item.getRssi() >= GOOD_SIGNAL_LIMIT){
            viewHolder.signalStrenght.setImageDrawable(Context.getDrawable(R.drawable.ic_signal_good));
            viewHolder.rssi.setText(item.getRssi() + " dBm");
        } else if (item.getRssi() < GOOD_SIGNAL_LIMIT && item.getRssi() >= POOR_SIGNAL_LIMIT){
            viewHolder.signalStrenght.setImageDrawable(Context.getDrawable(R.drawable.ic_signal_poor));
            viewHolder.rssi.setText(item.getRssi() + " dBm");
        } else {
            viewHolder.signalStrenght.setImageDrawable(Context.getDrawable(R.drawable.ic_singnal_no_signal));
            viewHolder.rssi.setText(item.getRssi() + " dBm");
        }

        viewHolder.macAddress.setText(item.getBluetoothDevice().getAddress());
        switch (item.getAvailableSensors()) {
                case 0b000: {
                    viewHolder.pedometer.setImageDrawable(Context.getDrawable(R.drawable.ic_pedometer_no_available));
                    viewHolder.heartRateMeter.setImageDrawable(Context.getDrawable(R.drawable.ic_heart_rate_meter_no_available));
                    viewHolder.thermometer.setImageDrawable(Context.getDrawable(R.drawable.ic_thermometer_no_available));
                }
                break;

                case 0b001: {
                    viewHolder.pedometer.setImageDrawable(Context.getDrawable(R.drawable.ic_pedometer_no_available));
                    viewHolder.heartRateMeter.setImageDrawable(Context.getDrawable(R.drawable.ic_heart_rate_meter_no_available));
                    viewHolder.thermometer.setImageDrawable(Context.getDrawable(R.drawable.ic_thermometer_available));
                }
                break;

                case 0b010: {
                    viewHolder.pedometer.setImageDrawable(Context.getDrawable(R.drawable.ic_pedometer_no_available));
                    viewHolder.heartRateMeter.setImageDrawable(Context.getDrawable(R.drawable.ic_heart_rate_meter_available));
                    viewHolder.thermometer.setImageDrawable(Context.getDrawable(R.drawable.ic_thermometer_no_available));
                }
                break;

                case 0b011: {
                    viewHolder.pedometer.setImageDrawable(Context.getDrawable(R.drawable.ic_pedometer_no_available));
                    viewHolder.heartRateMeter.setImageDrawable(Context.getDrawable(R.drawable.ic_heart_rate_meter_available));
                    viewHolder.thermometer.setImageDrawable(Context.getDrawable(R.drawable.ic_thermometer_available));
                }
                break;

                case 0b100: {
                    viewHolder.pedometer.setImageDrawable(Context.getDrawable(R.drawable.ic_pedometer_available));
                    viewHolder.heartRateMeter.setImageDrawable(Context.getDrawable(R.drawable.ic_heart_rate_meter_no_available));
                    viewHolder.thermometer.setImageDrawable(Context.getDrawable(R.drawable.ic_thermometer_no_available));
                }
                break;

                case 0b101: {
                    viewHolder.pedometer.setImageDrawable(Context.getDrawable(R.drawable.ic_pedometer_available));
                    viewHolder.heartRateMeter.setImageDrawable(Context.getDrawable(R.drawable.ic_heart_rate_meter_no_available));
                    viewHolder.thermometer.setImageDrawable(Context.getDrawable(R.drawable.ic_thermometer_available));
                }
                break;

                case 0b110: {
                    viewHolder.pedometer.setImageDrawable(Context.getDrawable(R.drawable.ic_pedometer_available));
                    viewHolder.heartRateMeter.setImageDrawable(Context.getDrawable(R.drawable.ic_heart_rate_meter_available));
                    viewHolder.thermometer.setImageDrawable(Context.getDrawable(R.drawable.ic_thermometer_no_available));
                }
                break;

                case 0b111: {
                    viewHolder.pedometer.setImageDrawable(Context.getDrawable(R.drawable.ic_pedometer_available));
                    viewHolder.heartRateMeter.setImageDrawable(Context.getDrawable(R.drawable.ic_heart_rate_meter_available));
                    viewHolder.thermometer.setImageDrawable(Context.getDrawable(R.drawable.ic_thermometer_available));
                }
                break;

                default: {
                    viewHolder.pedometer.setImageDrawable(Context.getDrawable(android.R.color.transparent));
                    viewHolder.heartRateMeter.setImageDrawable(Context.getDrawable(android.R.color.transparent));
                    viewHolder.thermometer.setImageDrawable(Context.getDrawable(android.R.color.transparent));
                }
                break;
            }

        //viewHolder.rssi.setch

        return view;
    }

    static class ViewHolder {
        TextView name;
        ImageView databaseIcon;
        TextView id;
        ImageView signalStrenght;
        TextView rssi;
        ImageView pedometer;
        ImageView heartRateMeter;
        ImageView thermometer;
        TextView macAddress;

    }
}