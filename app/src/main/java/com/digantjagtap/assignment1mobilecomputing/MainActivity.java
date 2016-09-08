package com.digantjagtap.assignment1mobilecomputing;

import android.annotation.TargetApi;
import android.graphics.Color;
import android.os.Build;
import android.os.Handler;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.view.Gravity;
import android.support.v7.app.ActionBar.LayoutParams;


public class MainActivity extends AppCompatActivity implements View.OnClickListener {

    Button buttonRun;
    Button buttonStop;
    LinearLayout base;

    GraphView gv;
    float[] values = new float[50];
    float[] oldValues = new float[50];
    float[] emptyValues = new float[0];
    int i;
    String[] horLabels = new String[]{"0", "1", "2", "3", "4", "5"};
    String[] verLabels = new String[]{"5", "4", "3", "2","1","0"};

    boolean graphRunning;

    private Handler handler;


    @TargetApi(Build.VERSION_CODES.JELLY_BEAN_MR1)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        buttonRun = (Button)findViewById(R.id.buttonRun);
        buttonStop = (Button)findViewById(R.id.buttonStop);
        buttonRun.setOnClickListener(this);
        buttonStop.setOnClickListener(this);

        gv = new GraphView(this, values, "Matlab UI", horLabels, verLabels, GraphView.LINE);
        base = (LinearLayout) findViewById(R.id.base);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);
        params.setMargins(30,20,30,40);
        params.gravity = Gravity.CENTER;
        gv.setLayoutParams(params);
        base.addView(gv);
        graphRunning = true;
        handler = new Handler();
    }

    private Runnable mUpdate = new Runnable() {
        public void run() {
            if(graphRunning) {
            gv.setValues(values);
            gv.invalidate();
            for(i = 0;i<40;i++)   {
                    oldValues[i+10] = values[i];
                }
            for (i = 10; i < 50; i++) {
                    values[i] = oldValues[i];
                }

            for(i = 0;i<10;i++)   {
                values[i] = (float)Math.random()*5;
            }
                handler.postDelayed(this, 200);
            }
        }
    };


    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case  R.id.buttonRun: {
                // Called when Run Button pressed
                graphRunning=true;
                for (i = 0; i < 50; i++) {
                    values[i] = (float)Math.random()*5;
                }
                handler.post(mUpdate);
                buttonRun.setEnabled(false);
                break;
            }

            case R.id.buttonStop: {
                // Called when Stop Button pressed
                graphRunning=false;
                buttonRun.setEnabled(true);
                gv.setValues(emptyValues);
                gv.invalidate();
                break;
            }
        }
    }



}

