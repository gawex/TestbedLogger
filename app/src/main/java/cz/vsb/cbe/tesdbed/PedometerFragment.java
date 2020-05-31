package cz.vsb.cbe.tesdbed;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Color;
import android.os.AsyncTask;
import android.os.Bundle;
import android.text.method.ScrollingMovementMethod;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;


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

        TestbedDbHelper testbedDbHelper = TestbedDbHelper.getInstance(getContext());
        readableDatabase = testbedDbHelper.getReadableDatabase();

        return view;
    }

    public void updateUi (DatabaseListAdapter databaseListAdapter){
        listView.setAdapter(databaseListAdapter);
    }

    public void toustuj(){
        Toast.makeText(getContext(), "PedometerFragment.class.getSimpleName(),", Toast.LENGTH_SHORT).show();
    }





}
