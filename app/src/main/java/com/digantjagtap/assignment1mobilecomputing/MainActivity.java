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
import java.net.MalformedURLException;
import java.net.URL;
import java.security.SecureRandom;
import java.security.cert.X509Certificate;

import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

import static android.widget.Toast.LENGTH_SHORT;


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
    Button buttonUpload;
    Button buttonDownload;
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
        buttonSubmit = (Button)findViewById(R.id.buttonSubmit);
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
                                buttonStop.setEnabled(false);
                                buttonUpload.setEnabled(false);
                                buttonDownload.setEnabled(false);
                                buttonRun.setEnabled(false);
                                Toast.makeText(MainActivity.this,"uploading started.....",Toast.LENGTH_LONG).show();
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
                                buttonStop.setEnabled(false);
                                buttonUpload.setEnabled(false);
                                buttonDownload.setEnabled(false);
                                buttonRun.setEnabled(false);
                                Toast.makeText(MainActivity.this,"checking file in server.....",Toast.LENGTH_LONG).show();
                            }
                        });
                        downloadFile(DATABASE_LOCATION, "https://impact.asu.edu/CSE535Fall16Folder/UploadToServerGPS.php", DATABASE_NAME, buttonStop, buttonUpload, buttonDownload, buttonRun, gv, fLayout);
                    }
                }).start();
            }
        });


        buttonSubmit.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                patientIDText = patientID.getText().toString();
                ageText = age.getText().toString();
                patientNameText = patientName.getText().toString();
                int index = group.getCheckedRadioButtonId();
                sexText = ((RadioButton)findViewById(index)).getText().toString();
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

                    }
                }catch (SQLException e){

                    Toast.makeText(MainActivity.this, e.getMessage(), Toast.LENGTH_LONG).show();
                }
            }
        });
    }

    private Runnable mUpdate = new Runnable() {
        public void run() {
            if(graphRunning) {

                Acceleromter acceleromter = myDB.getAccelValues();
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

    //@Begin upload
    public int uploadFile(final String sourceFileUri, String strDestinationUri, String fileName, final Button stopButton, final Button uploadButton, final Button downloadButton, final Button recordButton) {
        final String uploadErrorMsg = "Upload failed.";
        //Referred to http://tinyurl.com/or8wql2
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
                    //dispButton.setEnabled(true);
                    stopButton.setEnabled(true);
                    uploadButton.setEnabled(true);
                    downloadButton.setEnabled(true);
                    recordButton.setEnabled(true);
                }
            });

            return 0;

        } else {
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
                    return 0;
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

                // read file and write it into form...
                bytesRead = fileInputStream.read(buffer, 0, bufferSize);

                while (bytesRead > 0) {

                    dos.write(buffer, 0, bufferSize);
                    bytesAvailable = fileInputStream.available();
                    bufferSize = Math.min(bytesAvailable, maxBufferSize);
                    bytesRead = fileInputStream.read(buffer, 0, bufferSize);

                }

                // send multipart form data necesssary after file data...
                dos.writeBytes(lineEnd);
                dos.writeBytes(twoHyphens + boundary + twoHyphens + lineEnd);

                // Responses from the server (code and message)
                serverResponseCode = conn.getResponseCode();
                String serverResponseMessage = conn.getResponseMessage();

                Log.i("uploadFile", "HTTP Response is : "
                        + serverResponseMessage + ": " + serverResponseCode);

                if (serverResponseCode == 200) {

                    runOnUiThread(new Runnable() {
                        public void run() {

                            String msg = "File Upload Completed.";

                            Toast.makeText(MainActivity.this, msg, Toast.LENGTH_LONG).show();
                            //dispButton.setEnabled(true);
                            stopButton.setEnabled(true);
                            uploadButton.setEnabled(true);
                            downloadButton.setEnabled(true);
                            recordButton.setEnabled(true);
                        }
                    });
                }

                //close the streams //
                fileInputStream.close();
                dos.flush();
                dos.close();

            } catch (MalformedURLException ex) {
                ex.printStackTrace();

                runOnUiThread(new Runnable() {
                    public void run() {
                        Toast.makeText(MainActivity.this, uploadErrorMsg, Toast.LENGTH_LONG).show();
                        //dispButton.setEnabled(true);
                        stopButton.setEnabled(true);
                        uploadButton.setEnabled(true);
                        downloadButton.setEnabled(true);
                        recordButton.setEnabled(true);
                    }
                });

                Log.e("Upload file to server", "error: " + ex.getMessage(), ex);
            } catch (Exception e) {
                e.printStackTrace();

                runOnUiThread(new Runnable() {
                    public void run() {
                        Toast.makeText(MainActivity.this, uploadErrorMsg, Toast.LENGTH_LONG).show();
                        //dispButton.setEnabled(true);
                        stopButton.setEnabled(true);
                        uploadButton.setEnabled(true);
                        downloadButton.setEnabled(true);
                        recordButton.setEnabled(true);
                    }
                });
                Log.e("File upload Exception", "Exception : " + e.getMessage(), e);
            }
            return serverResponseCode;

        } // End else block
    }
    //@End upload

    public void downloadFile(final String sourceFileUri, String strDestinationUri, String fileName, final Button stopButton, final Button uploadButton, final Button downloadButton, final Button recordButton, final GraphView gv, final FrameLayout base) {
        final String downloadErrorMsg = "Download failed.";

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
                        Toast.makeText(MainActivity.this, "Exception occurred", Toast.LENGTH_LONG).show();
                        stopButton.setEnabled(false);
                        uploadButton.setEnabled(true);
                        downloadButton.setEnabled(true);
                        recordButton.setEnabled(true);
                    }
                });
            }

            URL url = new URL("https://impact.asu.edu/CSE535Fall16Folder/my.db");
            /* Open a connection to that URL. */
            HttpsURLConnection ucon = (HttpsURLConnection) url.openConnection();
            /*
            * Define InputStreams to read from the URLConnection.
            */
            InputStream is = ucon.getInputStream();
            BufferedInputStream bis = new BufferedInputStream(is);
            /*
            * Read bytes to the Buffer until there is nothing more to read(-1).
            */
            ByteArrayOutputStream buffer = new ByteArrayOutputStream(50);
            int current = 0;
            while ((current = bis.read()) != -1) {
                buffer.write((byte) current);
            }

            /* Convert the Bytes read to a String. */
            FileOutputStream fos = new FileOutputStream(new File(Environment.getExternalStorageDirectory() + "/" + "my.db"));
            fos.write(buffer.toByteArray());
            fos.close();
            runOnUiThread(new Runnable() {
                public void run() {
                    Toast.makeText(MainActivity.this, "File download complete", Toast.LENGTH_LONG).show();
                    stopButton.setEnabled(true);
                    uploadButton.setEnabled(true);
                    downloadButton.setEnabled(true);
                    recordButton.setEnabled(true);
                    base.removeView(gv);
                    Acceleromter result = myDB.getAccelValues();
                    gv.setValues(result.xValues, result.yValues, result.zValues);
                    gv.invalidate();
                    base.addView(gv);
                }
            });

        } catch (IOException e) {
            e.printStackTrace();

            runOnUiThread(new Runnable() {
                public void run() {
                    Toast.makeText(MainActivity.this, downloadErrorMsg, Toast.LENGTH_LONG).show();
                    stopButton.setEnabled(true);
                    uploadButton.setEnabled(true);
                    downloadButton.setEnabled(true);
                    recordButton.setEnabled(true);
                }
            });

            Log.e("Download file to server", "error: " + e.getMessage(), e);
        } catch (NullPointerException e) {
            e.printStackTrace();

            runOnUiThread(new Runnable() {
                public void run() {
                    Toast.makeText(MainActivity.this, downloadErrorMsg, Toast.LENGTH_LONG).show();
                    stopButton.setEnabled(true);
                    uploadButton.setEnabled(true);
                    downloadButton.setEnabled(true);
                    recordButton.setEnabled(true);
                }
            });

            Log.e("Download file to server", "error: " + e.getMessage(), e);
        } catch (Exception e) {
            e.printStackTrace();

            runOnUiThread(new Runnable() {
                public void run() {
                    Toast.makeText(MainActivity.this, downloadErrorMsg, Toast.LENGTH_LONG).show();
                    stopButton.setEnabled(true);
                    uploadButton.setEnabled(true);
                    downloadButton.setEnabled(true);
                    recordButton.setEnabled(true);
                }
            });

            Log.e("Download file to server", "error: " + e.getMessage(), e);
        }
    }

}

