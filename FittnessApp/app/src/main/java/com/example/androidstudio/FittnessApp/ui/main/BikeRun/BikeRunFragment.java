package com.example.androidstudio.FittnessApp.ui.main.BikeRun;




import android.Manifest;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import com.example.androidstudio.FittnessApp.MainActivity;
import com.example.androidstudio.FittnessApp.R;


import androidx.activity.OnBackPressedCallback;
import androidx.core.app.ActivityCompat;
import androidx.fragment.app.Fragment;
import androidx.navigation.fragment.NavHostFragment;

import android.content.Context;

import de.hsfl.tjwa.blheartrateconnection.HeartSensorController;


public class BikeRunFragment extends Fragment implements View.OnClickListener, LocationListener {
    private static final String TAG = "hsfBikeRunFragment";

    //erstellen der Buttons und Textviews
    private Button startStopButton;
    private Button resetButton;
    private Button bikeRun2Button;

    private TextView velocity;
    private TextView averageVelocity;
    private TextView bpm;
    private TextView averageBPM;
    private TextView calories;
    private TextView distance;
    private TextView duration;

    //erstellen der Variablen
    HeartSensorController heartSensorController;

    String age;
    String weight;
    String gender;

    private float speed;
    private int addedSpeed = 0;
    private double caloriesBurnt = 0;
    private int addedBPM = 0;
    private double distanceTraveled = 0;
    Location lastLocation;

    private int seconds = 0;
    private boolean timerStarted = false;

    LocationManager locationManager;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        Log.d(TAG, "onCreateView():  ");

        //setzen des Layouts
        View view = inflater.inflate(R.layout.bike_fragment, container, false);

        //laden der benötigten Daten aus den SharedPreferences
        SharedPreferences preferences = this.getActivity().getSharedPreferences("SHARED_PREFS", Context.MODE_PRIVATE);

        age = preferences.getString("AGE", "");
        weight = preferences.getString("WEIGHT", "");
        gender = preferences.getString("GENDER", "");

        //initialisieren des heartSensors aus der MainActivity
        //wenn kein Herzratensensor vebunden ist werden Werte simuliert, um die Funktion der App aufrecht zu erhalten
        heartSensorController = ((MainActivity)getActivity()).getHeartSensorController();
        if (heartSensorController.isConnected()) {
            heartSensorController.startBluetooth(true);
        } else {
            heartSensorController.startSimulation(1000);
        }

        //initialisieren der Buttons und Textviews
        startStopButton = (Button) view.findViewById(R.id.startStop);
        resetButton = (Button) view.findViewById(R.id.reset);
        bikeRun2Button = (Button) view.findViewById(R.id.backToBikeRun2);

        velocity = (TextView) view.findViewById(R.id.velocity);
        averageVelocity = (TextView) view.findViewById(R.id.averageVelocity);
        bpm = (TextView) view.findViewById(R.id.bpm);
        averageBPM = (TextView) view.findViewById(R.id.averageBPM);
        calories = (TextView) view.findViewById(R.id.calories);
        distance = (TextView) view.findViewById(R.id.distance);
        duration = (TextView) view.findViewById(R.id.duration);

        startStopButton.setOnClickListener(this);
        resetButton.setOnClickListener(this);
        bikeRun2Button.setOnClickListener(this);

        //starten des Timers
        startTimer();

        locationManager = (LocationManager) getActivity().getSystemService(Context.LOCATION_SERVICE);

        //zuweisen einer Funktion zum Back button
        //wenn Back gedrückt wird gibt es eine Abfrage ob das Training gestoppt werden soll, wenn ja wird zum homeFragment gewechselt, wenn nein passiert nichts
        OnBackPressedCallback callback = new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                Log.d(TAG, "onBackPressed");
                AlertDialog.Builder backAlert = new AlertDialog.Builder(getActivity());
                backAlert.setTitle("Training Stoppen");
                backAlert.setMessage("Soll das Training wirklich gestoppt werden?");
                backAlert.setPositiveButton("Ja", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        Log.v(TAG, "going back to home");
                        NavHostFragment.findNavController(getParentFragment()).navigate(R.id.action_bikeRunFragment_to_homeFragment);
                    }
                });

                backAlert.setNeutralButton("Abbrechen", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        //do nothing
                    }
                });

                backAlert.show();
            }
        };
        requireActivity().getOnBackPressedDispatcher().addCallback(getActivity(), callback);

        return view;
    }

    //erstellen der Funktion hinter den Buttons
    public void onClick(View view){
        switch (view.getId()) {
            case R.id.startStop:
                Log.d(TAG, "onClick.startStop");

                //zuerst wird abgefragt ob der User seine Daten in den settings angegeben hat, wenn nicht wird er darauf hingewiesen und zum settingsFragment geleitet
                if(age.length() == 0 || weight.length() == 0 || gender.length() == 0) {
                    AlertDialog.Builder dataAlert = new AlertDialog.Builder(getActivity());
                    dataAlert.setTitle("Keine Daten");
                    dataAlert.setMessage("Bitte persönliche Daten ausfüllen");
                    dataAlert.setNeutralButton("Ok", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialogInterface, int i) {
                            NavHostFragment.findNavController(getParentFragment()).navigate(R.id.action_bikeRunFragment_to_settingsFragment);
                        }
                    });
                    dataAlert.show();
                }

                //wenn alle Daten vorhanden sind wird der Timer gestartet/gestoppt und der Text, sowie die Farbe des Buttons wird geändert
                //ebenfalls werden die location Updates angefragt
                if (timerStarted == false) {
                    timerStarted = true;
                    startStopButton.setText("Stop");
                    startStopButton.setBackgroundColor(Color.RED);
                    if (ActivityCompat.checkSelfPermission(getActivity(), Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                        locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0, BikeRunFragment.this);
                    }
                } else {
                    timerStarted = false;
                    startStopButton.setText("Start");
                    startStopButton.setBackgroundColor(Color.GREEN);
                }
                break;

            case R.id.reset:
                Log.d(TAG, "onClick.reset");

                //es wird ein Alert erstellt, der den User fragt ob er das Training wirklich zurücksetzen will, wenn ja werden alle Werte zurückgesetzt
                AlertDialog.Builder resetAlert = new AlertDialog.Builder(getActivity());
                resetAlert.setTitle("Zurücksetzen");
                resetAlert.setMessage("Soll das Training wirklich zurückgesetzt werden?");
                resetAlert.setPositiveButton("Ja", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        Log.d(TAG, "resetting");
                        resetValues();
                    }
                });

                resetAlert.setNeutralButton("Abbrechen", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        //do nothing
                    }
                });

                resetAlert.show();
                break;

            case R.id.backToBikeRun2:
                Log.d(TAG, "onClick.bikeRun2");

                //navigation zu BikeRun2, nachdem der User bestätigt hat
                AlertDialog.Builder bikeRun2Alert = new AlertDialog.Builder(getActivity());
                bikeRun2Alert.setTitle("Wechseln");
                bikeRun2Alert.setMessage("Soll wirklich zu BikeRun 2 gewechselt werden? Das Training wird gestoppt.");
                bikeRun2Alert.setPositiveButton("Ja", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        Log.d(TAG, "chaning to BikeRun2");
                        NavHostFragment.findNavController(getParentFragment()).navigate(R.id.action_bikeRunFragment_to_bikeRun2Fragment);
                    }
                });

                bikeRun2Alert.setNeutralButton("Abbrechen", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        //do nothing
                    }
                });

                bikeRun2Alert.show();
                break;
        }
    }



    //Timer zum anzeigen der Zeit und aktualisieren der Daten im Sekundentakt
    private void startTimer() {
        final Handler handler = new Handler();
        handler.post(new Runnable() {
            public void run () {
                //Aufteilung der Zeit in Sekunden, Minuten und Stunden
                int hours = seconds / 3600;
                int mins = (seconds % 3600) / 60;
                int secs = seconds % 60;
                String time = String.format("%02d, %02d, %02d", hours, mins, secs);
                duration.setText(time);
                if(timerStarted){
                    //erhöhung der Zeit im Sekundentakt, berechnung der durchschnittlichen Werte, Kalorien und setzen der Textviews
                    seconds++;

                    int heartrate = heartSensorController.getHeartRate().getValue();
                    calculateCalories(heartrate);
                    addedBPM += heartrate;
                    int averageHeartrate = addedBPM / seconds;

                    if(speed != 0.0f) {
                        addedSpeed += speed;
                        int averageSpeed = addedSpeed / seconds;
                        averageVelocity.setText("Ø" + String.valueOf(averageSpeed));
                    }
                    bpm.setText(String.valueOf(heartrate));
                    averageBPM.setText(String.valueOf("Ø" + averageHeartrate));
                    calories.setText(String.valueOf(caloriesBurnt));
                }
                handler.postDelayed(this, 1000);
            }
        });
    }



        @Override
        public void onLocationChanged(Location location) {
        //wenn der Timer gestartet ist, werden die Geschwindigkeit und die zurückgelegte Distanz ausgerechnet
            if (timerStarted) {
                if(timerStarted) {
                    getSpeed(location);
                    getDistance(location);
                }
            }
        }

        //ausrechnen der Geschwindigkeit in km/h und anzeigen im Textview

        //kann zu Fehlern bei verwendung der Routen führen, da die Location geupdatet wird obwohl man sich nicht bewegt und die Geschwindkeit trotzdem angegeben wird
        //ist aber dennoch meiner Meinung nach die beste Lösung, da andere Optionen noch ungenauer wären und zu anderen bugs führen würden
        public void getSpeed(Location location) {
            speed = (location.getSpeed() * 3600 / 1000);
            String convertedSpeed = String.format("%.2f", speed);
            velocity.setText(convertedSpeed);
        }

        public void getDistance(Location currentLocation) {
        //ausrechnen der Distanz von der letzten location zur aktuellen location und addieren zur insgesamt zurückgelegten Distanz
            if(lastLocation != null) {
                distanceTraveled += currentLocation.distanceTo(lastLocation) / 1000.0;
            }
            lastLocation = currentLocation;

            distance.setText(String.valueOf(distanceTraveled));
        }


    //berechnen der Kalorien mit Geschwindigkeit, Alter, Gewicht, Herzrate und Geschlecht
    public void calculateCalories(int heartrate){
        //berechnung wird nur ausgeführt, wenn man sich tatsächlich bewegt
        //es werden mehr kalorien verbrannt, wenn man sich schneller bewegt
        //Formel ist sehr ungenau, da es zu viele Faktoren gibt die den Kalorienverbrauch beeinflussen, wie z.B Fahrradzustand, Gelände und Wind
        if(speed != 0) {
            if(speed <= 15){
                caloriesBurnt += 250.0 / 3600.0;
            }else if(speed > 15 && speed <= 18){
                caloriesBurnt += 350.0 / 3600.0;
            }else if(speed > 18 && speed <= 22){
                caloriesBurnt += 500.0 / 3600.0;
            }else if(speed > 22 && speed <= 28){
                caloriesBurnt += 700.0 / 3600.0;
            }else {
                caloriesBurnt += 900.0 / 3600.0;
            }
            if (gender.equals("MALE")) {
                caloriesBurnt += (Integer.parseInt(age) + Integer.parseInt(weight) + heartrate) * 0.0001;
            } else if (gender.equals("FEMALE")) {
                caloriesBurnt -= (Integer.parseInt(age) + Integer.parseInt(weight) + heartrate) * 0.0001;
            }
        }
    }

    //zurücksetzen der Variablen und Textviews
    public void resetValues(){
        timerStarted = false;
        seconds = 0;
        addedSpeed = 0;
        addedBPM = 0;
        caloriesBurnt = 0;
        distanceTraveled = 0;
        lastLocation = null;
        calories.setText("0");
        velocity.setText("0");
        distance.setText("0");
        bpm.setText("0");
        averageVelocity.setText("Ø0");
        averageBPM.setText("Ø0");
        startStopButton.setText("Start");
        startStopButton.setBackgroundColor(Color.GREEN);
    }


}
