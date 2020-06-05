package cz.vsb.cbe.tesdbed;

import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ListView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;

import cz.vsb.cbe.tesdbed.sql.TestbedDatabaseHelper;


public class PedometerFragment extends Fragment {

    DatabaseListAdapter databaseListAdapter;
    SQLiteDatabase readableDatabase;

    ListView listView;


    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.fragment_pedometer, container, false);

        listView = view.findViewById(R.id.pedoList);
        databaseListAdapter = new DatabaseListAdapter(getLayoutInflater(), "%4.0f", getContext());

        //listView.setAdapter(databaseListAdapter);


        return view;
    }

    public void updateUi (DatabaseListAdapter databaseListAdapter){
        listView.setAdapter(databaseListAdapter);
    }

    public void toustuj(){
        Toast.makeText(getContext(), "PedometerFragment.class.getSimpleName(),", Toast.LENGTH_SHORT).show();
    }





}
