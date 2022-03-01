package com.example.androidstudio.FittnessApp.ui.main.WindSurf;



import android.Manifest;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import androidx.activity.OnBackPressedCallback;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.preference.PreferenceManager;
import android.view.View;
import android.widget.ImageView;
import androidx.core.app.ActivityCompat;
import androidx.fragment.app.Fragment;
import androidx.navigation.fragment.NavHostFragment;
import android.os.Handler;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import com.example.androidstudio.FittnessApp.R;

import org.osmdroid.config.Configuration;
import org.osmdroid.util.GeoPoint;

import java.util.ArrayList;
import java.util.List;


public class SurfFragment extends Fragment implements View.OnClickListener, LocationListener , SensorEventListener{
    private static final String TAG = "hsfSurfFragment";
    private Button startBtn; // start stop buttun
    private Button resBtn;   // reset buttun
    ///
    Location lastloc;        // location of the starting point
    String where;            // get letter assigned to the kurse ( W N E W )
    String whereav;          //   get letter assigned to the avarage kurse
    double avaragekurs;      //   save the avarage kurse degree

    double locchngdtimes;    //  location changed times to calculate the avarage kurse
    double middleazimuth;     //  avarage azimuth also to calculate the middle kurse



    //****************************  compass variables

    private GeoPoint point;          // geo point  for the distance calculation
    private GeoPoint startpoint;     // geo point to get the starting point langitude and latitude
    double lon;                      // variable to save the longitude of the starting point
    double lat ;                     //  same as the prieviuos




    private List<GeoPoint> geoPoints; // arraylist to save  the geo points in
    private Context ctx;              // context for the class

    ImageView compass_img;              // compass image view
    TextView txt_compass;                // compass text
    int mAzimuth;                        // variable for the azimuth formula
    private SensorManager mSensorManager;  // sensor manager for the compass
    private Sensor mRotationV, mAccelerometer, mMagnetometer;      // sensors also for the compass
    boolean haveSensor = false, haveSensor2 = false;                 // same
    float[] rMat = new float[9];                                      // float arrays for the compass also
    float[] orientation = new float[3];                                // same
    private float[] mLastAccelerometer = new float[3];                // array to save the accelerometer last output
    private float[] mLastMagnetometer = new float[3];                  // array to save the LastMagnetometer last output
    private boolean mLastAccelerometerSet = false;
    private boolean mLastMagnetometerSet = false;
    //***********************************End compass
    private TextView speed1 ;              // text view to shaw the cuurent speed
    private TextView avkurs;                //  text view to show the avarage kurse
    private TextView weg;                    // text view to show the distnce to the start point
    private TextView timepassed;             // text view to show the time since the start
    private float speed2 ;                     // float to convert speed
    double  wegtraveled = 0 ;                  // distence
    private int sec = 0;                        // seconds
    boolean timeStart = false;
    LocationManager locManager;                 //  location manager

    private Button surfFragment2;
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        Log.d(TAG, "onCreateView():  ");


        View view = inflater.inflate(R.layout.surf_fragment, container, false);


        //***************************************** start the compass code by initilizing the sensors and the img and txt view
        mSensorManager = (SensorManager)  getActivity().getSystemService(Context.SENSOR_SERVICE);

        compass_img = (ImageView) view.findViewById(R.id.img_compass);
        txt_compass = (TextView) view.findViewById(R.id.txt_azimuth);
        locchngdtimes = 0 ;



        //*********************    initilizing the geo points and context and the points array list
        ctx = getContext().getApplicationContext();
        Configuration.getInstance().load(ctx, PreferenceManager.getDefaultSharedPreferences(ctx));
        point = new GeoPoint(52.106701, 10.198094);
        geoPoints = new ArrayList<>(); //  this would be filled later in the onlocationchanged method
        //********




// initilizing the buttuns and the other textviews
        startBtn = (Button) view.findViewById(R.id.startbtn1);
        avkurs = (TextView)  view.findViewById(R.id.avaregespeedText);
        weg = (TextView) view.findViewById(R.id.destanceText);
        timepassed = (TextView) view.findViewById(R.id.durationsurf);
        resBtn = (Button) view.findViewById(R.id.resetbtn);
        speed1 = (TextView) view.findViewById(R.id.txtspeed);
        surfFragment2 = (Button) view.findViewById(R.id.surf2);
        startBtn.setOnClickListener(this);
        resBtn.setOnClickListener(this);
        surfFragment2.setOnClickListener(this);

        ///




//  alert dialog start  when back pressed it asks the user if he is sure
        OnBackPressedCallback callback = new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                Log.d(TAG, "onBackPressed");
                AlertDialog.Builder backAlert = new AlertDialog.Builder(getActivity());
                backAlert.setTitle("Training Stop ?");
                backAlert.setMessage("would you like to stop surfing ?");
                backAlert.setPositiveButton("Yup", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        Log.v(TAG, "going back to home");
                        NavHostFragment.findNavController(getParentFragment()).navigate(R.id.action_action_homeFragment_to_surfFragment_to_homeFragment);
                    }
                });

                backAlert.setNeutralButton("No", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {

                    }
                });

                backAlert.show();
            }
        };
        requireActivity().getOnBackPressedDispatcher().addCallback(getActivity(), callback);
        // the alert dialog code end

        startTimer();   // start the timer

        // initilizing the location manager for the compass
        locManager = (LocationManager) getActivity().getSystemService(Context.LOCATION_SERVICE);










        return view;
    }

    private void startTimer() {   ///  method to start the timer
        final Handler handler = new Handler();
        handler.post(new Runnable() {
            public void run() {
                int hours = sec / 3600;
                int mints = (sec % 3600) / 60;
                int secs = sec % 60;
                String time = String.format("%02d, %02d, %02d", hours, mints, secs);
                timepassed.setText(time);
                if(timeStart){


                    sec++;}  // seconds increaswe every 1000 ms


                handler.postDelayed(this, 1000);
            }


        });
    }

    //********************************compass  method to start the compass
    public void startcompass() {        // first check for sensors in the phones
        if (mSensorManager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR) == null) {
            if ((mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER) == null) || (mSensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD) == null)) {
                txt_compass.setText("no sensor");   //  no sensor alert
            }
            else {   // wher sensor found  then they would be initilized like the folow
                mAccelerometer = mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
                mMagnetometer = mSensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD);
                haveSensor = mSensorManager.registerListener(this, mAccelerometer, SensorManager.SENSOR_DELAY_UI);
                haveSensor2 = mSensorManager.registerListener(this, mMagnetometer, SensorManager.SENSOR_DELAY_UI);
            }
        }
        else{ // start the compass
            mRotationV = mSensorManager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR);
            haveSensor = mSensorManager.registerListener(this, mRotationV, SensorManager.SENSOR_DELAY_UI);
        }
    }


    // this method to stop the compass by stopping the Accelerometer and Magnetometer  sensores
    public void stopCompass() {
        if(haveSensor && haveSensor2){
            mSensorManager.unregisterListener(this,mAccelerometer);
            mSensorManager.unregisterListener(this,mMagnetometer);
        }
        else{
            if(haveSensor)
                mSensorManager.unregisterListener(this,mRotationV);
        }
    }

    private void stopSensor(){ // method to stop the sensores also
        mSensorManager.unregisterListener(this, mAccelerometer);
        mSensorManager.unregisterListener(this, mMagnetometer);

    }



    @Override
    public void onLocationChanged(Location location) {     // this method will be invoked every time the location of the divice change
        if (timeStart) {
            Log.v(TAG, "onLocationChanged");
            getSpeed(location);
            getDistance(location);
            //////////////////////////////////////////////////////////////////////////
            point = new GeoPoint(location);      // assaign the geo point
            geoPoints.add(point);                // saving the geo points in the array list
            if(geoPoints.size() >= 5) {          // make sure the geopoint hase coordinates

                startpoint = geoPoints.get(4);     // take the second set of coordinates
                lon = startpoint.getLongitude();   //  assign the start point longitude and Latitude
                lat = startpoint.getLatitude();
                locchngdtimes ++;                   //  increase the  locationchanged time so we can calculate the middle cource later
                middleazimuth +=mAzimuth ;       //   calculate the middle cource
                avaragekurs =  middleazimuth / locchngdtimes;   // same

                ;}

        }
        // calculate the straight line distence using CalculationByDistance method
        wegtraveled = (CalculationByDistance( lat , lon ,lastloc.getLatitude() , lastloc.getLongitude() ) * 100 ) ;

    }

    public void getSpeed(Location location) { //    get the converted speed from meter per secont to km/h
        speed2 = (location.getSpeed() * 3600 / 1000);
        String convertedSpeed = String.format("%.2f", speed2);
        speed1.setText(convertedSpeed);

    }



    public void onClick(View view){  //   on click view
        switch (view.getId()) {
            case R.id.startbtn1:
                Log.d(TAG, "onClick.startStop");
                if (timeStart == false) {
                    timeStart = true;
                    startBtn.setText("Stop");
                    startBtn.setBackgroundColor(Color.RED);
                    // check for the location permission
                    if (ActivityCompat.checkSelfPermission(getActivity(), Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                        locManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0, SurfFragment.this);

                        startcompass();//r
                        startcompass();
                        /////////////////////////////////////////////////////////////////////////////////////

                        Log.d(TAG,"GPS AN");

                        //start the location update when the permission is given
                        if (ActivityCompat.checkSelfPermission(getActivity(), Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                            locManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0, SurfFragment.this);
                        }




                        //////////////////////////////////////////////////////////////////////////////////////////

                    }


                } else {   //  stop pressed

                    timeStart = false;
                    startBtn.setText("Start");
                    startBtn.setBackgroundColor(Color.BLUE);
                    stopCompass();
                    stopCompass();
                    geoPoints.clear();

                    //////////////////////////////////////////////////////////////////////////////

                    Log.d(TAG,"GPS AUS");
                    locManager.removeUpdates(this); //Entfernen der Locationupdates



                    //////////////////////////////////////////////////////////////////////////////

                }
                break;
            case R.id.surf2:
                Log.d(TAG, "onClick.surf2");

                //navigation zu BikeRun2, nachdem der User bestätigt hat
                AlertDialog.Builder Surf2Alert = new AlertDialog.Builder(getActivity());
                Surf2Alert.setTitle("Wechseln");
                Surf2Alert.setMessage("Soll wirklich zu Windsurf 2 gewechselt werden? Das Training wird gestoppt.");
                Surf2Alert.setPositiveButton("Ja", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        Log.d(TAG, "chaning to surf2");
                        NavHostFragment.findNavController(getParentFragment()).navigate(R.id.action_action_homeFragment_to_surfFragment_to_surfFragment2);
                    }
                });

                Surf2Alert.setNeutralButton("Abbrechen", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        //do nothing
                    }
                });

                Surf2Alert.show();
                break;

            case R.id.resetbtn:   // restart pressed
                Log.d(TAG, "onClick.reset");
                AlertDialog.Builder resetAlert = new AlertDialog.Builder(getActivity());
                resetAlert.setTitle("reset?");
                resetAlert.setMessage("Are you SURE You want to reset ?");
                resetAlert.setPositiveButton("YUP", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        Log.d(TAG, "reset all values");
                        resetall();         // rstart all method
                        wegtraveled=0;
                        lat = 0 ;
                        lon = 0 ;
                        geoPoints.clear();


                    }
                });
                // ask the user if he is sure , he wants to reset all

                resetAlert.setNeutralButton("Cancell", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {

                    }
                });

                resetAlert.show();
                break;

        }
    }



    public void getDistance(Location userloc  ){   // to get the normal traveled distance if needed in the future
        //ausrechnen der Distanz von der letzten location zur aktuellen location und addieren zur insgesamt zurückgelegten Distanz
        if(lastloc != null) {

            //    wegtraveled += userloc.distanceTo(lastloc) / 100.0;
        }
        lastloc =new Location("");
        lastloc= userloc;
        weg.setText("" + wegtraveled );

    }
    // this method uses haversine formula to calculate the straight line distence
    public double CalculationByDistance(double initialLat, double initialLong,
                                        double finalLat, double finalLong){
        int R = 6371; // km (Earth radius)
        double dLat = toRadians(finalLat-initialLat);
        double dLon = toRadians(finalLong-initialLong);
        initialLat = toRadians(initialLat);
        finalLat = toRadians(finalLat);

        double a = Math.sin(dLat/2) * Math.sin(dLat/2) +
                Math.sin(dLon/2) * Math.sin(dLon/2) * Math.cos(initialLat) * Math.cos(finalLat);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1-a));
        return R * c;
    }

    public double toRadians(double deg) {
        return deg * (Math.PI/180);
    }

    //  method to reset every thing to 0
    private void resetall() {
        timeStart = false;
        sec = 0;
        wegtraveled = 0;
        stopSensor();
        stopCompass();
        compass_img.setRotation(0);
        txt_compass.setText("0.0 N");
        mAzimuth = 0;
        speed1.setText("0");
        weg.setText("0");
        startBtn.setText("Start");
        startBtn.setBackgroundColor(Color.BLUE);
    }


    //compass method to get sensors output everytime they are changed

    @Override
    public void onSensorChanged(SensorEvent event) {
        if (event.sensor.getType() == Sensor.TYPE_ROTATION_VECTOR) {
            SensorManager.getRotationMatrixFromVector(rMat, event.values);
            mAzimuth = (int) (Math.toDegrees(SensorManager.getOrientation(rMat, orientation)[0]) + 360) % 360;
        }
        txt_compass.setText(mAzimuth + "°"+ where );

        if (event.sensor.getType() == Sensor.TYPE_ACCELEROMETER) {
            System.arraycopy(event.values, 0, mLastAccelerometer, 0, event.values.length);
            mLastAccelerometerSet = true;
        } else if (event.sensor.getType() == Sensor.TYPE_MAGNETIC_FIELD) {
            System.arraycopy(event.values, 0, mLastMagnetometer, 0, event.values.length);
            mLastMagnetometerSet = true;
        }
        if (mLastAccelerometerSet && mLastMagnetometerSet) {
            SensorManager.getRotationMatrix(rMat, null, mLastAccelerometer, mLastMagnetometer);
            SensorManager.getOrientation(rMat, orientation);
            mAzimuth = (int) (Math.toDegrees(SensorManager.getOrientation(rMat, orientation)[0]) + 360) % 360;
        }


        // // set the rotation of the compass image
        mAzimuth = Math.round(mAzimuth);
        compass_img.setRotation(-mAzimuth);

        avkurs.setText(whereav);
        // calculate the  letter direction by the degrees for the course and the avarage course
        where = "NW";

        if (mAzimuth >= 350 || mAzimuth <= 10)
            where = "N";
        if (mAzimuth < 350 && mAzimuth > 280)
            where = "NW";
        if (mAzimuth <= 280 && mAzimuth > 260)
            where = "W";
        if (mAzimuth <= 260 && mAzimuth > 190)
            where = "SW";
        if (mAzimuth <= 190 && mAzimuth > 170)
            where = "S";
        if (mAzimuth <= 170 && mAzimuth > 100)
            where = "SE";
        if (mAzimuth <= 100 && mAzimuth > 80)
            where = "E";
        if (mAzimuth <= 80 && mAzimuth > 10)
            where = "NE";

        if (avaragekurs >= 350 || avaragekurs <= 10)
            whereav = "N";
        if (avaragekurs < 350 && avaragekurs > 280)
            whereav = "NW";
        if (avaragekurs <= 280 && avaragekurs > 260)
            whereav = "W";
        if (avaragekurs <= 260 && avaragekurs > 190)
            whereav = "SW";
        if (avaragekurs <= 190 && avaragekurs > 170)
            whereav = "S";
        if (avaragekurs <= 170 && avaragekurs > 100)
            whereav = "SE";
        if (avaragekurs <= 100 && avaragekurs > 80)
            whereav = "E";
        if (avaragekurs <= 80 && avaragekurs > 10)
            whereav = "NE";



    }

    // this method needed to be implemented in order for the sensores lesenter to work
    @Override
    public void onAccuracyChanged(Sensor sensor, int i) {

    }

}


