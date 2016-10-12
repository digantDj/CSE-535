package com.digantjagtap.assignment1mobilecomputing;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Toast;

import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.security.SecureRandom;
import java.security.cert.X509Certificate;

import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;


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

    Button buttonEnable, buttonDisable;

    Button buttonUpload;
    Button buttonDownload;

    LinearLayout base;
    FrameLayout fLayout;
    Intent accelerometerService;
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
    float[] emptyX = new float[0];
    float[] emptyY = new float[0];
    float[] emptyZ = new float[0];
    int i;

    String[] verLabels = new String[]{"+10", "+8", "+6", "+4", "+2", "0", "-2", "-4", "-6", "-8", "-10"};
    String[] horLabels = new String[]{"0", "2", "4", "6", "8", "10"};


    boolean graphRunning = false;
    DBHelper myDB;
    MyReceiver myReceiver;
    private Handler handler;

    int serverResponseCode = 0;


    public static final String DATABASE_NAME = "my.db";
    public static final String DATABASE_LOCATION = Environment.getExternalStorageDirectory() + File.separator + DATABASE_NAME;


    //@TargetApi(Build.VERSION_CODES.JELLY_BEAN_MR1)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        gv = new GraphView(this, xValues, yValues, zValues, "Accelermeter Data", horLabels, verLabels, GraphView.LINE);
        fLayout  = (FrameLayout) findViewById(R.id.frame);

        handler = new Handler();

        patientID = (EditText) findViewById(R.id.patientId);
        age = (EditText) findViewById(R.id.age);
        patientName = (EditText) findViewById(R.id.patientName);
        group = (RadioGroup) findViewById(R.id.sex);

        buttonRun = (Button)findViewById(R.id.buttonRun);
        buttonStop = (Button)findViewById(R.id.buttonStop);
        buttonEnable = (Button)findViewById(R.id.buttonEnable);
        buttonDisable = (Button)findViewById(R.id.buttonDisable);

        buttonRun.setOnClickListener(this);
        buttonStop.setOnClickListener(this);


        buttonDownload = (Button)findViewById(R.id.buttonDownload);
        buttonUpload = (Button)findViewById(R.id.buttonUpload);

        buttonUpload.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                new Thread(new Runnable() {
                    public void run() {
                        runOnUiThread(new Runnable() {
                            public void run() {
                                // When uploading, no other operation can be done, so other buttons disabled.
                                buttonStop.setEnabled(false);
                                buttonUpload.setEnabled(false);
                                buttonDownload.setEnabled(false);
                                buttonRun.setEnabled(false);
                                Toast.makeText(MainActivity.this,"Begin Upload",Toast.LENGTH_LONG).show();
                            }
                        });

                        uploadFile(DATABASE_LOCATION, "https://impact.asu.edu/CSE535Fall16Folder/UploadToServerGPS.php", DATABASE_NAME, buttonStop, buttonUpload, buttonDownload, buttonRun);
                    }
                }).start();
            }
        });

        buttonDownload.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                new Thread(new Runnable() {
                    public void run() {
                        runOnUiThread(new Runnable() {
                            public void run() {
                                // When downloading, no other operation can be done, so other buttons disabled.
                                buttonStop.setEnabled(false);
                                buttonUpload.setEnabled(false);
                                buttonDownload.setEnabled(false);
                                buttonRun.setEnabled(false);
                                //Toast.makeText(MainActivity.this,"Begin Download",Toast.LENGTH_LONG).show();
                            }
                        });
                        downloadFile(DATABASE_LOCATION, "https://impact.asu.edu/CSE535Fall16Folder/UploadToServerGPS.php", DATABASE_NAME, buttonStop, buttonUpload, buttonDownload, buttonRun, gv, fLayout);
                    }
                }).start();
            }
        });


        buttonEnable.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                patientIDText = patientID.getText().toString();
                ageText = age.getText().toString();
                patientNameText = patientName.getText().toString();
                int index = group.getCheckedRadioButtonId();
                sexText = ((RadioButton)findViewById(index)).getText().toString();
                tableName = patientIDText + "_" + ageText + "_" + patientNameText + "_" + sexText;

                try{
                    try {
                        myReceiver = new MyReceiver();
                        myDB = DBHelper.getInstance(MainActivity.this);
                        myDB.setTableName(tableName);
                        myDB.onCreateTable();
                        IntentFilter intentFilter = new IntentFilter();
                        intentFilter.addAction(AccelerometerService.MY_ACTION);
                        registerReceiver(myReceiver, intentFilter);
                        accelerometerService = new Intent(MainActivity.this, AccelerometerService.class);
                        startService(accelerometerService);
                        buttonRun.setEnabled(true);
                        graphRunning=false;
                    }
                    catch (SQLiteException e) {
                        Toast.makeText(MainActivity.this,"SQLiteException", Toast.LENGTH_LONG).show();
                        Toast.makeText(MainActivity.this, e.getMessage(), Toast.LENGTH_LONG).show();
                    } catch (IOException e) {
                        e.printStackTrace();
                    } finally {

                    }
                }catch (SQLException e){

                    Toast.makeText(MainActivity.this, e.getMessage(), Toast.LENGTH_LONG).show();
                }
            }
        });


        buttonDisable.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                stopService(accelerometerService);
            }
        });

        gv = new GraphView(this, xValues, yValues, zValues, "Accelermeter Data", horLabels, verLabels, GraphView.LINE);
        fLayout  = (FrameLayout) findViewById(R.id.frame);

        handler = new Handler();

    }

    private Runnable mUpdate = new Runnable() {
        public void run() {
            if(graphRunning) {

                Accelerometer acceleromter = myDB.getAccelValues();
                xValues = acceleromter.xValues;
                yValues = acceleromter.yValues;
                zValues = acceleromter.zValues;

                gv.setValues(xValues, yValues, zValues);

                gv.invalidate();
                handler.postDelayed(this, 1000);
            }
        }
    };


    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case  R.id.buttonRun: {
                if (gv.getParent() == null) {
                    fLayout.addView(gv);
                }
                if (graphRunning) {
                    return;
                }
                graphRunning=true;
                handler.post(mUpdate);
                buttonRun.setEnabled(false);
                buttonDownload.setEnabled(false);
                buttonUpload.setEnabled(false);
                break;
            }

            case R.id.buttonStop: {
                // Called when Stop Button pressed

                if (!graphRunning) {
                    return;
                }
                stopService(accelerometerService);
                fLayout.removeView(gv);
                graphRunning=false;
                buttonRun.setEnabled(true);
                buttonDownload.setEnabled(true);
                buttonUpload.setEnabled(true);

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

            } catch (SQLiteException e) {
                //report problem
            } finally {
            }
        }
    }

    // Reference: http://androidexample.com/Upload_File_To_Server_-_Android_Example/index.php?view=article_discription&aid=83&aaid=106
    public int uploadFile(final String sourceFileUri, String strDestinationUri, String fileName, final Button stopButton, final Button uploadButton, final Button downloadButton, final Button runButton) {
        HttpsURLConnection conn = null;
        DataOutputStream dos = null;
        String lineEnd = "\r\n";
        String twoHyphens = "--";
        String boundary = "***";
        int bytesRead, bytesAvailable, bufferSize;
        byte[] buffer;
        int maxBufferSize = 1 * 1024 * 1024;
        File sourceFile = new File(sourceFileUri);

        if (!sourceFile.isFile()) {
            Log.e("uploadFile", "Source File not exist :"
                    + sourceFileUri);

            runOnUiThread(new Runnable() {
                public void run() {
                    Toast.makeText(MainActivity.this, "Source File not exist :"
                            + sourceFileUri, Toast.LENGTH_LONG).show();
                    stopButton.setEnabled(true);
                    uploadButton.setEnabled(true);
                    downloadButton.setEnabled(true);
                    runButton.setEnabled(true);
                }
            });

            return 0;

        } else {
            //Upload the database file from device to server if it exists
            try {

                // open a URL connection to the Servlet
                FileInputStream fileInputStream = new FileInputStream(sourceFile);

                // Create a trust manager that does not validate certificate chains
                TrustManager[] trustAllCerts = new TrustManager[]{
                        new X509TrustManager() {
                            public X509Certificate[] getAcceptedIssuers() {
                                return null;
                            }

                            public void checkClientTrusted(
                                    X509Certificate[] certs, String authType) {
                            }

                            public void checkServerTrusted(
                                    X509Certificate[] certs, String authType) {
                            }
                        }
                };

                // Install the all-trusting trust manager
                try {
                    SSLContext sc = SSLContext.getInstance("SSL");
                    sc.init(null, trustAllCerts, new SecureRandom());
                    HttpsURLConnection.setDefaultSSLSocketFactory(sc.getSocketFactory());
                } catch (Exception e) {
                    e.printStackTrace();

                    runOnUiThread(new Runnable() {
                        public void run() {
                            Toast.makeText(MainActivity.this, "Error in File Upload", Toast.LENGTH_LONG).show();
                            stopButton.setEnabled(true);
                            uploadButton.setEnabled(true);
                            downloadButton.setEnabled(true);
                            runButton.setEnabled(true);
                        }
                    });
                    Log.e("File upload Exception", "Exception : " + e.getMessage(), e);
                }

                URL url = new URL(strDestinationUri);

                // Open a HTTP  connection to  the URL
                conn = (HttpsURLConnection) url.openConnection();
                conn.setDoInput(true); // Allow Inputs
                conn.setDoOutput(true); // Allow Outputs
                conn.setUseCaches(false); // Don't use a Cached Copy
                conn.setRequestMethod("POST");
                conn.setRequestProperty("Connection", "Keep-Alive");
                conn.setRequestProperty("ENCTYPE", "multipart/form-data");
                conn.setRequestProperty("Content-Type", "multipart/form-data;boundary=" + boundary);
                conn.setRequestProperty("uploaded_file", fileName);

                dos = new DataOutputStream(conn.getOutputStream());

                dos.writeBytes(twoHyphens + boundary + lineEnd);
                dos.writeBytes("Content-Disposition: form-data; name=\"uploaded_file\";filename=\""
                        + fileName + "\"" + lineEnd);

                dos.writeBytes(lineEnd);

                // create a buffer of  maximum size
                bytesAvailable = fileInputStream.available();

                bufferSize = Math.min(bytesAvailable, maxBufferSize);
                buffer = new byte[bufferSize];

                // read file data into bytes in the buffer.
                bytesRead = fileInputStream.read(buffer, 0, bufferSize);

                // write the buffer data in the required data format
                while (bytesRead > 0) {

                    dos.write(buffer, 0, bufferSize);
                    bytesAvailable = fileInputStream.available();
                    bufferSize = Math.min(bytesAvailable, maxBufferSize);
                    bytesRead = fileInputStream.read(buffer, 0, bufferSize);

                }

                // send multipart form data necessary after file data.
                dos.writeBytes(lineEnd);
                dos.writeBytes(twoHyphens + boundary + twoHyphens + lineEnd);

                // Responses from the server (code and message)
                serverResponseCode = conn.getResponseCode();
                String serverResponseMessage = conn.getResponseMessage();

                Log.i("File Upload", "HTTP Response: "
                        + serverResponseMessage + ": " + serverResponseCode);

                if (serverResponseCode == 200) {

                    runOnUiThread(new Runnable() {
                        public void run() {
                            Toast.makeText(MainActivity.this, "File Upload Completed", Toast.LENGTH_LONG).show();
                            // Enable the other buttons after upload is complete.
                            stopButton.setEnabled(true);
                            uploadButton.setEnabled(true);
                            downloadButton.setEnabled(true);
                            runButton.setEnabled(true);
                        }
                    });
                }

                //close the streams
                fileInputStream.close();
                dos.flush();
                dos.close();

            } catch (Exception e) {
                e.printStackTrace();

                runOnUiThread(new Runnable() {
                    public void run() {
                        Toast.makeText(MainActivity.this, "Error in File Upload" , Toast.LENGTH_LONG).show();
                        // Enable the other buttons after upload is complete.
                        stopButton.setEnabled(true);
                        uploadButton.setEnabled(true);
                        downloadButton.setEnabled(true);
                        runButton.setEnabled(true);
                    }
                });
                Log.e("File upload Exception", "Exception : " + e.getMessage(), e);
            } finally{

            }
            return serverResponseCode;

        }
    }

    public void downloadFile(final String sourceFileUri, String strDestinationUri, String fileName, final Button stopButton, final Button uploadButton, final Button downloadButton, final Button runButton, final GraphView gv, final FrameLayout base) {
        try {

            // Create a trust manager that does not validate certificate chains
            TrustManager[] trustAllCerts = new TrustManager[]{
                    new X509TrustManager() {
                        public X509Certificate[] getAcceptedIssuers() {
                            return null;
                        }

                        public void checkClientTrusted(
                                X509Certificate[] certs, String authType) {
                        }

                        public void checkServerTrusted(
                                X509Certificate[] certs, String authType) {
                        }
                    }
            };

            // Install the all-trusting trust manager
            try {
                SSLContext sc = SSLContext.getInstance("SSL");
                sc.init(null, trustAllCerts, new SecureRandom());
                HttpsURLConnection.setDefaultSSLSocketFactory(sc.getSocketFactory());
            } catch (Exception e) {
                e.printStackTrace();

                runOnUiThread(new Runnable() {
                    public void run() {
                        Toast.makeText(MainActivity.this, "Error in File Download", Toast.LENGTH_LONG).show();
                        stopButton.setEnabled(false);
                        uploadButton.setEnabled(true);
                        downloadButton.setEnabled(true);
                        runButton.setEnabled(true);
                    }
                });
            }

            // Read the same DB file that we had uploaded to the same folder
            URL url = new URL("https://impact.asu.edu/CSE535Fall16Folder/my.db");
            // Open a connection to that URL.
            HttpsURLConnection ucon = (HttpsURLConnection) url.openConnection();
            InputStream is = ucon.getInputStream();
            BufferedInputStream bis = new BufferedInputStream(is);
            // Read data in the form of bytes till end of file.
            ByteArrayOutputStream buffer = new ByteArrayOutputStream(50);
            int current = 0;
            while ((current = bis.read()) != -1) {
                buffer.write((byte) current);
            }
            // Open the same DB file in the local and over write it with downloaded DB file from server
            FileOutputStream fos = new FileOutputStream(new File(Environment.getExternalStorageDirectory() + "/" + "my.db"));
            // Write the bytes to the same local DB file.
            fos.write(buffer.toByteArray());
            fos.close();
            runOnUiThread(new Runnable() {
                public void run() {
                    Toast.makeText(MainActivity.this, "File download complete", Toast.LENGTH_LONG).show();
                    // Enable the other buttons after download is complete.
                    stopButton.setEnabled(true);
                    uploadButton.setEnabled(true);
                    downloadButton.setEnabled(true);
                    runButton.setEnabled(true);
                    // Plot the values from the downloaded DB on the graph.
                    base.removeView(gv);
                    // Downloaded values fetched here from the DB.
                    Accelerometer result = myDB.getAccelValues();
                    gv.setValues(result.xValues, result.yValues, result.zValues);
                    gv.invalidate();
                    base.addView(gv);
                }
            });

        } catch (Exception e) {
            e.printStackTrace();

            runOnUiThread(new Runnable() {
                public void run() {
                    Toast.makeText(MainActivity.this, "Error in File Download", Toast.LENGTH_LONG).show();
                    // Enable the other buttons after download is complete.
                    stopButton.setEnabled(true);
                    uploadButton.setEnabled(true);
                    downloadButton.setEnabled(true);
                    runButton.setEnabled(true);
                }
            });

            Log.e("File Download Exception", "error: " + e.getMessage(), e);
        }finally{

        }
    }

}

