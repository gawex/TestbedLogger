package cz.vsb.cbe.tesdbed;

import android.graphics.Color;
import android.os.Bundle;
import android.text.method.ScrollingMovementMethod;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;

import java.util.Random;


public class PedometerFragment extends Fragment {

    private TextView pedo;
    private TextView heartRate;
    private TextView temp;

    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {

        View root = inflater.inflate(R.layout.fragment_pedometer, container, false);
        pedo = root.findViewById(R.id.pedo);
        pedo.setMovementMethod(new ScrollingMovementMethod());
        heartRate = root.findViewById(R.id.heart);
        heartRate.setMovementMethod(new ScrollingMovementMethod());
        temp = root.findViewById(R.id.temp);
        temp.setMovementMethod(new ScrollingMovementMethod());

        return root;
    }

    public void updatePedo(String string){
        pedo.setText(pedo.getText() + "\n" + string);
    }

    public void updateHeartRate(String string){
        heartRate.setText(string);
    }

    public void updateTemp(String string){
        temp.setText(string);
        Random rnd = new Random();
        int color = Color.argb(255, rnd.nextInt(256), rnd.nextInt(256), rnd.nextInt(256));
        temp.setTextColor(color);
    }

}
