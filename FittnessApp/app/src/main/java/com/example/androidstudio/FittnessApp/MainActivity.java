package com.example.androidstudio.FittnessApp;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

import de.hsfl.tjwa.blheartrateconnection.HeartSensorController;

public class MainActivity extends AppCompatActivity {
    private static final String TAG = "MainActivity";

    private HeartSensorController heartSensorController;
    private String currentAdapter = "";
    private BluetoothDevice selectedHeartRateSensor;
    private BluetoothManager bluetoothManager;
    private boolean isConnected = false;
    private static String PREVMACADRESS = "PREVMACADRESS";


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Lädt das Startfragment (home)
        setContentView(R.layout.main_activity);

        // Instanziert den heartSensorController
        this.heartSensorController = new HeartSensorController(this);
        this.bluetoothManager = (BluetoothManager)getSystemService(Context.BLUETOOTH_SERVICE);

        // Fragt den Benutzer über die Lacationpermissions ab, falls noch nciht erteilt
        if (ContextCompat.checkSelfPermission(MainActivity.this,
                Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            if (ActivityCompat.shouldShowRequestPermissionRationale(MainActivity.this,
                    Manifest.permission.ACCESS_FINE_LOCATION)) {
                ActivityCompat.requestPermissions(MainActivity.this,
                        new String[]{ Manifest.permission.ACCESS_FINE_LOCATION }, 1);
            } else {
                ActivityCompat.requestPermissions(MainActivity.this,
                        new String[] { Manifest.permission.ACCESS_FINE_LOCATION }, 1);
            }
        }

        // Überprüft, ob zuletzt eine Verbindung mit einem Bluetoothgerät existierte
        loadFromPrefs();
        if (this.currentAdapter.length() > 0) {
            Log.v(TAG, "STARTING BLUETOOTH");
            selectedHeartRateSensor = bluetoothManager.getAdapter() .getRemoteDevice(currentAdapter);
            this.heartSensorController.startBluetooth(selectedHeartRateSensor);
            this.isConnected = true;

            // Sollte das Gerät kein Herzratensensor sein (oder keine Abrufbare Herzrate), wird eine Simulation gestartet
            if (heartSensorController.getHeartRate().getValue() == null) {
                this.heartSensorController.stopAll();
                    this.heartSensorController.startSimulation(1000);
            }
        }
    }

    // Fragt den Benutzer nach den Permissions für die Locationservices
    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        switch (requestCode) {
            case 1: {
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    if (ContextCompat.checkSelfPermission(MainActivity.this,
                            Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                        Toast.makeText(this, "Permission Granted", Toast.LENGTH_SHORT).show();
                    }
                } else {
                    Toast.makeText(this, "Permission Denied", Toast.LENGTH_SHORT).show();
                }
                return;
            }
        }
    }

    // Speichert die Macadresse
    @Override
    public void onStop() {
        super.onStop();
        saveInPref();
    }

    // Speichert die Macadresse
    @Override
    public void onPause() {
        super.onPause();
        saveInPref();
    }

    // Speichert die Macadresse
    private void saveInPref() {
        SharedPreferences sharedPreferences = getSharedPreferences("SHARED_PREFS", Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putString(PREVMACADRESS, currentAdapter);
        editor.apply();
    }

    // Lädt die Macadresse
    private void loadFromPrefs() {
        SharedPreferences sharedPreferences = getSharedPreferences("SHARED_PREFS", Context.MODE_PRIVATE);
        currentAdapter = sharedPreferences.getString(PREVMACADRESS, "");
    }

    // initialisiert den HeartRateSensor
    public void setSelectedHeartRateSensor(BluetoothDevice selectedHeartRateSensor) {
        this.selectedHeartRateSensor = selectedHeartRateSensor;
    }

    // Übergibt die Macadresse des Aktuell verbundenen Geräts
    public void setCurrentAdapter(String currentAdapter) {
        this.currentAdapter = currentAdapter;
    }

    // Startet die Bluetoothdiesnte -> Auch hier wird eine Simulation gestartet, falls das Gerät keine abrufbare Herzrate hergibt
    public void startBluetoothFromFragment() {
        Log.v(TAG, "STARTING BLUETOOTH");
        this.heartSensorController.startBluetooth(selectedHeartRateSensor);
        this.isConnected = true;
        if (heartSensorController.getHeartRate().getValue() == null) {
            this.heartSensorController.stopAll();
            this.heartSensorController.startSimulation(1000);
        }
    }

    // Gibt den aktuellen herzRatenSensor zurück
    public HeartSensorController getHeartSensorController() {
        return this.heartSensorController;
    }

    // Gibt zurück, ob die Activity einen aktiven Bluetoothdienst am Laufen hat
    public boolean getIsConnected() {
        return this.isConnected;
    }
}