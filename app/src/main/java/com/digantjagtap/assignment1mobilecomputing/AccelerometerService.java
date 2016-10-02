package com.digantjagtap.assignment1mobilecomputing;

/**
 * Created by jlee375 on 2016-02-03.
 */
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.os.IBinder;
import android.widget.Toast;

import java.io.IOException;

public class AccelerometerService extends Service implements SensorEventListener{

    private SensorManager accelManage;
    private Sensor senseAccel;
    float accelValuesX;
    float accelValuesY;
    float accelValuesZ;

    public final static String MY_ACTION = "MY_ACTION";
    long lastInsertedTimeStamp = System.currentTimeMillis();


    @Override
    public void onCreate(){

        accelManage = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        senseAccel = accelManage.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);

        accelManage.registerListener(AccelerometerService.this, senseAccel, SensorManager.SENSOR_DELAY_NORMAL);

    }
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        return START_NOT_STICKY;
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }



    @Override
    public void onSensorChanged(SensorEvent sensorEvent) {
        long currentTimeStamp = System.currentTimeMillis();
        if((currentTimeStamp - lastInsertedTimeStamp) > 1000) {
            lastInsertedTimeStamp = currentTimeStamp;
            Sensor mySensor = sensorEvent.sensor;
            if (mySensor.getType() == Sensor.TYPE_ACCELEROMETER) {

                accelValuesX = sensorEvent.values[0];
                accelValuesY = sensorEvent.values[1];
                accelValuesZ = sensorEvent.values[2];

                Intent intent = new Intent();
                intent.setAction(MY_ACTION);

                intent.putExtra("xValue",accelValuesX);
                intent.putExtra("yValue",accelValuesY);
                intent.putExtra("zValue",accelValuesZ);

                sendBroadcast(intent);

            }
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
        // TODO Auto-generated method stub

    }


}
