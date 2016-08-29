package com.digantjagtap.assignment1mobilecomputing;

import android.os.Handler;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;

public class MainActivity extends AppCompatActivity implements View.OnClickListener {

    Button buttonRun;
    Button buttonStop;
    LinearLayout base;

    GraphView gv;
    float[] values = new float[100];
    String[] horlabels = new String[]{"0", "1", "2", "3", "4", "5", "6","7","8","9","10"};
    String[] verlabels = new String[]{"10","9","8","7", "6", "5", "4", "3", "2","1","0"};

    boolean graphRunning;

    private Handler mHandler;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        buttonRun = (Button)findViewById(R.id.buttonRun);
        buttonStop = (Button)findViewById(R.id.buttonStop);
        buttonRun.setOnClickListener(this);
        buttonStop.setOnClickListener(this);

        gv = new GraphView(this, values, "Matlab UI", horlabels, verlabels, GraphView.LINE);

        base = (LinearLayout) findViewById(R.id.base);
        base.addView(gv);
        graphRunning = true;
        mHandler = new Handler();
       // mHandler.post(mUpdate);
    }

    private Runnable mUpdate = new Runnable() {
        public void run() {
            for (int i = 0; i < 100; i++) {
                values[i] = (float) Math.random()*10;
                System.out.println(values[i]);
            }

            gv.setValues(values);
            base.removeView(gv);
            base.addView(gv);
            if(graphRunning)
            mHandler.postDelayed(this, 100);

        }
    };


    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case  R.id.buttonRun: {
                // Called when Run Button pressed
                graphRunning=true;
                mHandler.post(mUpdate);
                break;
            }

            case R.id.buttonStop: {
                // Called when Stop Button pressed
                graphRunning=false;
                // base.removeView(gv);
               // base.addView(gv);
                break;
            }
        }
    }



}

