package com.digantjagtap.assignment1mobilecomputing;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.DatabaseErrorHandler;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;
import android.database.sqlite.SQLiteOpenHelper;
import android.os.Environment;
import android.widget.Toast;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * Created by Mihir on 9/30/2016.
 */
public class DBHelper extends SQLiteOpenHelper{
    private static final String DB_NAME = Environment.getExternalStorageDirectory() + File.separator + "my.db";
    private static DBHelper dbHelper;
    private String tableName;
    private String COL2 = "Timestamp";
    private String COL3 = "xValue";
    private String COL4 = "yValue";
    private String COL5 = "zValue";
    private static SQLiteDatabase db = null;
    private Context context;


    private DBHelper(Context context) {
        super(context, DB_NAME, null, 1);
        this.context = context;
    }
    public void setTableName(String tableName) {
        this.tableName = "[" + tableName + "]";
    }

    public static DBHelper getInstance(Context context, String tableName) throws IOException, SQLiteException {
        if (dbHelper == null){
            dbHelper = new DBHelper(context.getApplicationContext());
            dbHelper.setTableName(tableName);
            db = dbHelper.getWritableDatabase();
            dbHelper.onCreateTable();
        }
        return dbHelper;
    }

    public void onCreateTable() throws SQLiteException {
        Toast.makeText(context.getApplicationContext(), "onCreateTable", Toast.LENGTH_SHORT).show();
        db.execSQL("create table " + tableName + " ("
                + " Timestamp text, "
                + " xValue real, "
                + " yValue real, "
                + " zValue real)" );
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        Toast.makeText(context.getApplicationContext(), "on create database  " , Toast.LENGTH_SHORT).show();

    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        //Toast.makeText(getApplicationContext(), "inside onUpgrade", Toast.LENGTH_LONG).show();
        db.execSQL("Drop table if exists" + tableName);
        onCreate(db);
    }

    public boolean insertAccelValues(float x, float y, float z) {

        ContentValues contentValues = new ContentValues();
        contentValues.put(COL2,new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date()));
        contentValues.put(COL3, x);
        contentValues.put(COL4, y);
        contentValues.put(COL5, z);
        long result = db.insert(tableName, null, contentValues);
        if (result == -1) {
            return false;
        } else {
            return true;
        }

        //String insertQuery = "INSERT INTO " + tableName + "(Timestamp, xValue, yValue, zValue) values(CURRENT_TIMESTAMP," + x +","+ y +","+ z + ")";
        //db.execSQL(insertQuery);
    }

    public Acceleromter getAccelValues() {

        Cursor cursor = null;
        String selectQuery = "SELECT  * FROM " + tableName + " ORDER BY TimeStamp DESC";;
        try {
            cursor = db.rawQuery(selectQuery, null);
            db.setTransactionSuccessful(); //commit your changes
        } catch (Exception e) {
            //report problem
        }
        Acceleromter acceleromter = new Acceleromter();
        float[] x = new float[10];
        float[] y = new float[10];
        float[] z = new float[10];

        int i = 0;

        try {
            //put the data from database into x, y and z arrays
            if (cursor.moveToFirst()) {
                do {
                    // get the data into array, or class variable
                    x[i] = cursor.getFloat(cursor.getColumnIndex("xValue"));
                    y[i] = cursor.getFloat(cursor.getColumnIndex("yValue"));
                    z[i] = cursor.getFloat(cursor.getColumnIndex("zValue"));
                    i++;
                    cursor.moveToNext();
                    //count++;
                } while (i < 10);
            }
            cursor.close();
        }catch(Exception e){
            //report problem
        } finally {
            acceleromter.xValues = x;
            acceleromter.yValues = y;
            acceleromter.zValues = z;
        }

        return acceleromter;
    }


}
