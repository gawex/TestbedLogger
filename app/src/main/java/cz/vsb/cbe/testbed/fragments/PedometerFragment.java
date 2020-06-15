package cz.vsb.cbe.testbed.fragments;

import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ListView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;

import com.google.android.material.bottomnavigation.BottomNavigationView;

import cz.vsb.cbe.testbed.DatabaseActivity;
import cz.vsb.cbe.testbed.DatabaseListAdapter;
import cz.vsb.cbe.testbed.R;


public class PedometerFragment extends Fragment {




    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.fragment_pedometer, container, false);




        return view;
    }






}
