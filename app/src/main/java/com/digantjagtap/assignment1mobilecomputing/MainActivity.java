package com.digantjagtap.assignment1mobilecomputing;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;
import android.os.Handler;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.RadioGroup;
import android.widget.Toast;

import java.io.IOException;

import static android.widget.Toast.*;


/**
 * MainActivity consists of Patient ID, Age, Patient Name and Sex. It has two buttons, Run and Stop
 * Run Button will plot a graph in a FrameLayout and will keep on updating it until Stop is pressed.
 *
 * @author Digant Jagtap, Mihir Bhatt, Deepika  Krishnakumar, Purushotham Kaushik
 *
 */

public class MainActivity extends AppCompatActivity implements View.OnClickListener {

    Button buttonRun;
    Button buttonStop;
    Button buttonSubmit;
    LinearLayout base;
    FrameLayout fLayout;

    SQLiteDatabase db;
    EditText patientID;
    EditText age;
    EditText patientName;
    RadioGroup group;

    String patientIDText = null;
    String ageText = null;
    String patientNameText = null;
    String sexText = null;
    String tableName = null;

    GraphView gv;
    float[] xValues = new float[10];
    float[] yValues = new float[10];
    float[] zValues = new float[10];
    float[] values = new float[50];
    float[] oldValues = new float[50];
    float[] emptyX, emptyY, emptyZ = new float[0];
    int i;
    String[] horLabels = new String[]{"0", "1", "2", "3", "4", "5"};
    String[] verLabels = new String[]{"5", "4", "3", "2","1","0"};

    boolean graphRunning;
    DBHelper myDB;
    MyReceiver myReceiver;
    private Handler handler;


    //@TargetApi(Build.VERSION_CODES.JELLY_BEAN_MR1)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        patientID = (EditText) findViewById(R.id.patientId);
        age = (EditText) findViewById(R.id.age);
        patientName = (EditText) findViewById(R.id.patientName);
        group = (RadioGroup) findViewById(R.id.sex);

        buttonRun = (Button)findViewById(R.id.buttonRun);
        buttonStop = (Button)findViewById(R.id.buttonStop);
        buttonSubmit = (Button)findViewById(R.id.buttonSubmit);
        buttonRun.setOnClickListener(this);
        buttonStop.setOnClickListener(this);



        buttonSubmit.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                patientIDText = patientID.getText().toString();
                ageText = age.getText().toString();
                patientNameText = patientName.getText().toString();
                int index = group.getCheckedRadioButtonId();
                if (index == 0) {
                    sexText = "Male";
                } else {
                    sexText = "Female";
                }
                tableName = patientIDText + "_" + ageText + "_" + patientNameText + "_" + sexText;
                Toast.makeText(MainActivity.this, tableName, LENGTH_SHORT).show();


                try{
                    try {
                        myReceiver = new MyReceiver();
                        myDB = DBHelper.getInstance(MainActivity.this, tableName);
                        IntentFilter intentFilter = new IntentFilter();
                        intentFilter.addAction(AccelerometerService.MY_ACTION);
                        registerReceiver(myReceiver, intentFilter);
                        Intent startAccelerometerService = new Intent(MainActivity.this, AccelerometerService.class);
                        startService(startAccelerometerService);

                    }
                    catch (SQLiteException e) {
                        Toast.makeText(MainActivity.this,"SQLiteException", Toast.LENGTH_LONG).show();
                        Toast.makeText(MainActivity.this, e.getMessage(), Toast.LENGTH_LONG).show();
                    } catch (IOException e) {
                        e.printStackTrace();
                    } finally {
                       // db.endTransaction();
                    }
                }catch (SQLException e){

                    Toast.makeText(MainActivity.this, e.getMessage(), Toast.LENGTH_LONG).show();
                }
            }
        });



        gv = new GraphView(this, xValues, yValues, zValues, "Matlab UI", horLabels, verLabels, GraphView.LINE);
        fLayout  = (FrameLayout) findViewById(R.id.frame);
        fLayout.addView(gv);
        graphRunning = true;
        handler = new Handler();
    }

    private Runnable mUpdate = new Runnable() {
        public void run() {
            if(graphRunning) {

                Acceleromter acceleromter = myDB.getAccelValues();
                xValues = acceleromter.xValues;
                yValues = acceleromter.yValues;
                zValues = acceleromter.zValues;

                gv.setValues(xValues, yValues, zValues);

                //gv.setValues(values);
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
                handler.postDelayed(this, 1000);
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
                gv.setValues(emptyX, emptyY, emptyZ);
                gv.invalidate();
                break;
            }
        }
    }

    private class MyReceiver extends BroadcastReceiver {

        @Override
        public void onReceive(Context arg0, Intent arg1) {

            float xValue = arg1.getFloatExtra("xValue", 0);
            float yValue = arg1.getFloatExtra("yValue", 0);
            float zValue = arg1.getFloatExtra("zValue", 0);

            try {
                //insert the data into database
                boolean isInsertDone = myDB.insertAccelValues(xValue, yValue, zValue);
                if (!isInsertDone) {
                    Toast.makeText(MainActivity.this, "Unable to insert", Toast.LENGTH_SHORT).show();
                }
               // db.execSQL("insert into "+TABLE_NAME+"(timeStamp, x_pos, y_pos, z_pos) values ('" + times + "', '" + datapassed1 + "', '" + datapassed2 + "', '" + datapassed3 + "' );");
            } catch (SQLiteException e) {
                //report problem
            } finally {
            }
        }
    }

}

