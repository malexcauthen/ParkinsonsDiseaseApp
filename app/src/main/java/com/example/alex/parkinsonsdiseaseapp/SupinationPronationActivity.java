package com.example.alex.parkinsonsdiseaseapp;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.Toast;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.TimeZone;

public class SupinationPronationActivity extends AppCompatActivity implements SensorEventListener{
    private SensorManager sm;
    private Sensor mAcc;
    private Sensor gyro;
    private BufferedWriter out = null;
    private Calendar cal;
    private String Afile;
    private String Gfile;
    private String date;

    //will contain the accelerometer sensor data
    List<String> a = new ArrayList<>();
    List<String> g = new ArrayList<>();

    // Storage Permissions
    private static final int REQUEST_EXTERNAL_STORAGE = 1;
    private static String[] PERMISSIONS_STORAGE = {
            Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.WRITE_EXTERNAL_STORAGE
    };

    public static void verifyStoragePermissions(Activity activity) {
        // Check if we have write permission
        int permission = ActivityCompat.checkSelfPermission(activity, Manifest.permission.WRITE_EXTERNAL_STORAGE);

        if (permission != PackageManager.PERMISSION_GRANTED) {
            // We don't have permission so prompt the user
            ActivityCompat.requestPermissions(
                    activity,
                    PERMISSIONS_STORAGE,
                    REQUEST_EXTERNAL_STORAGE
            );
        }
    }

    protected void onResume(){
        super.onResume();
        sm.registerListener(this, mAcc, SensorManager.SENSOR_DELAY_FASTEST);
        sm.registerListener(this, gyro, SensorManager.SENSOR_DELAY_FASTEST);
    }

    protected void onPause(){
        super.onPause();
        sm.unregisterListener(this);
    }

    public void onAccuracyChanged(Sensor sensor, int accuracy){
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_supinationpronation);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        sm = (SensorManager)getSystemService(SENSOR_SERVICE);

        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        final Button start = (Button) findViewById(R.id.startButton);
        start.setOnClickListener(new View.OnClickListener() {
            public void onClick(View view) {
                try {
                    if (start.getText().toString().equals("Start")) {
                        startRecording();
                    } else {
                        stopRecording();
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        });
    }

    public void onSensorChanged(SensorEvent e) {
        cal = Calendar.getInstance(TimeZone.getDefault());
        SimpleDateFormat sdf = new SimpleDateFormat("HH:mm:ss.SSS");
        String output = sdf.format(cal.getTime());
        //record sensor data
        if(e.sensor.getType() == Sensor.TYPE_ACCELEROMETER) {
            output += "," + e.values[0] + "," + e.values[1] + "," + e.values[2];
            output += "\n";
            a.add(output);
        }

        if(e.sensor.getType() == Sensor.TYPE_GYROSCOPE) {
            output += "," + e.values[0] + "," + e.values[1] + "," + e.values[2];
            output += "\n";
            g.add(output);
        }
    }

    protected void startRecording() throws IOException {
        Button start;

        //checks to make sure the phone has the sensors that we are recording from
        if(sm.getDefaultSensor(Sensor.TYPE_ACCELEROMETER) != null) {
            mAcc = sm.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
            sm.registerListener(this, mAcc, SensorManager.SENSOR_DELAY_FASTEST);
        }

        if(sm.getDefaultSensor(Sensor.TYPE_GYROSCOPE) != null) {
            gyro = sm.getDefaultSensor(Sensor.TYPE_GYROSCOPE);
            sm.registerListener(this, gyro, SensorManager.SENSOR_DELAY_FASTEST);
        }

        a.clear();
        g.clear();

        Toast.makeText(SupinationPronationActivity.this, "The test has begun.", Toast.LENGTH_SHORT).show();
        start = (Button)findViewById(R.id.startButton);
        start.setText("Stop");
    }

    private void showEmailOption(){
        AlertDialog.Builder helpBuilder = new AlertDialog.Builder(this);
        helpBuilder.setTitle("Email");
        helpBuilder.setMessage("Would you like to email the test data?");
        helpBuilder.setPositiveButton("Yes",
                new DialogInterface.OnClickListener() {

                    public void onClick(DialogInterface dialog, int which) {
                        //save the data
                        sendEmail();
                    }
                });

        helpBuilder.setNegativeButton("No",
                new DialogInterface.OnClickListener() {

                    public void onClick(DialogInterface dialog, int which) {
                        // Do nothing but close the dialog
                        a.clear();
                        g.clear();
                    }
                });

        // Remember, create doesn't show the dialog
        AlertDialog helpDialog = helpBuilder.create();
        helpDialog.setCancelable(false);
        helpDialog.setCanceledOnTouchOutside(false);
        helpDialog.show();

    }

    private void showSimplePopUp() {

        AlertDialog.Builder helpBuilder = new AlertDialog.Builder(this);
        helpBuilder.setTitle("New Supination Pronation File");
        helpBuilder.setMessage("Save supination pronation file?");
        helpBuilder.setPositiveButton("Save",
                new DialogInterface.OnClickListener() {

                    public void onClick(DialogInterface dialog, int which) {
                        //save the data
                        String rootpath;
                        String folderpath;
                        String filepath;
                        File F;

                        //make directories if they do not exist
                        rootpath = Environment.getExternalStorageDirectory().getPath();
                        F = new File(rootpath, "Parkinsons");

                        if (!F.exists()) {
                            F.mkdirs();
                        }

                        folderpath = rootpath + "/Parkinsons";
                        F = new File(folderpath, "SupinationPronation");
                        if (!F.exists()) {
                            F.mkdirs();
                        }

                        filepath = folderpath + "/SupinationPronation";

                        //file name is the current date and time
                        cal = Calendar.getInstance(TimeZone.getDefault());
                        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd_hh:mm:ss");
                        String output = sdf.format(cal.getTime());
                        date = output;

                        F = new File(filepath, output);
                        if (!F.exists()) {
                            F.mkdirs();
                        }

                        filepath += "/" + output;

                        F = new File(filepath, output + "_SP_A.csv");
                        Afile = output + "_SP_A.csv";

                        FileOutputStream fos = null;
                        try {
                            fos = new FileOutputStream(F);
                        } catch (FileNotFoundException e) {
                            e.printStackTrace();
                        }
                        out = new BufferedWriter(new OutputStreamWriter(fos));

                        for (int i = 0; i < a.size(); i++) {
                            try {
                                out.write(a.get(i));
                            } catch (IOException e1) {
                                e1.printStackTrace();
                            }
                        }

                        try {
                            if (out != null) {
                                out.flush();
                                out.close();
                            }
                        } catch (IOException e) {
                            e.printStackTrace();
                        }

                        a.clear();

                        //file name is the current date and time
                        F = new File(filepath, output + "_SP_G.csv");
                        Gfile = output + "_SP_G.csv";

                        fos = null;
                        try {
                            fos = new FileOutputStream(F);
                        } catch (FileNotFoundException e) {
                            e.printStackTrace();
                        }
                        out = new BufferedWriter(new OutputStreamWriter(fos));

                        for( int i = 0; i < g.size(); i++){
                            try {
                                out.write(g.get(i));
                            } catch (IOException e1) {
                                e1.printStackTrace();
                            }
                        }

                        try {
                            if (out != null) {
                                out.flush();
                                out.close();
                            }
                        } catch (IOException e) {
                            e.printStackTrace();
                        }

                        g.clear();

                        Toast.makeText(SupinationPronationActivity.this, "File saved.", Toast.LENGTH_SHORT).show();

                        showEmailOption();
                    }
                });

        helpBuilder.setNegativeButton("Discard",
                new DialogInterface.OnClickListener() {

                    public void onClick(DialogInterface dialog, int which) {
                        // Do nothing but close the dialog
                        a.clear();
                        g.clear();
                    }
                });

        // Remember, create doesn't show the dialog
        AlertDialog helpDialog = helpBuilder.create();
        helpDialog.setCancelable(false);
        helpDialog.setCanceledOnTouchOutside(false);
        helpDialog.show();
    }


    protected void stopRecording() throws IOException{
        Button start;

        sm.unregisterListener(this);
        Toast.makeText(SupinationPronationActivity.this, "The test has stopped.", Toast.LENGTH_SHORT).show();
        start = (Button) findViewById(R.id.startButton);
        start.setText("Start");

        verifyStoragePermissions(this);

        showSimplePopUp();
    }

    protected void sendEmail() {
        String[] TO = {""};
        String[] CC = {""};
        String rootpath;
        ArrayList <Uri> uris = new ArrayList<>();
        Intent emailIntent = new Intent(Intent.ACTION_SEND_MULTIPLE);

        emailIntent.setData(Uri.parse("mailto:"));
        emailIntent.setType("text/plain");
        emailIntent.putExtra(Intent.EXTRA_EMAIL, TO);
        emailIntent.putExtra(Intent.EXTRA_CC, CC);
        emailIntent.putExtra(Intent.EXTRA_SUBJECT, "Supination Pronation Test Data");

        rootpath = Environment.getExternalStorageDirectory().getPath();

        uris.add(Uri.parse("file://" + rootpath + "/Parkinsons/SupinationPronation/" + date + "/" + Afile));
        uris.add(Uri.parse("file://" + rootpath + "/Parkinsons/SupinationPronation/" + date + "/" + Gfile));
        uris.add(Uri.parse("file://" + rootpath + "/Parkinsons/Configuration/Configuration_A.csv"));
        uris.add(Uri.parse("file://" + rootpath + "/Parkinsons/Configuration/Configuration_G.csv"));

        emailIntent.putParcelableArrayListExtra(Intent.EXTRA_STREAM, uris);

        //emailIntent.putExtra(Intent.EXTRA_TEXT, "Email message goes here");

        try {
            startActivity(Intent.createChooser(emailIntent, "Send mail..."));
            finish();
        } catch (android.content.ActivityNotFoundException ex) {
            Toast.makeText(SupinationPronationActivity.this, "There is no email client installed.", Toast.LENGTH_SHORT).show();
        }
    }
}
