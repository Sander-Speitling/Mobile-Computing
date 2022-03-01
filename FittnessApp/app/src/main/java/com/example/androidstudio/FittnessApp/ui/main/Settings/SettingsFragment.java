package com.example.androidstudio.FittnessApp.ui.main.Settings;

import static android.app.Activity.RESULT_OK;

import android.app.AlertDialog;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;

import androidx.activity.OnBackPressedCallback;
import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.navigation.fragment.NavHostFragment;

import com.example.androidstudio.FittnessApp.MainActivity;
import com.example.androidstudio.FittnessApp.R;

import de.hsfl.tjwa.blheartrateconnection.HeartSensorController;
import de.hsfl.tjwa.blheartrateconnection.scan.LeDeviceScanActivity;

public class SettingsFragment extends Fragment implements View.OnClickListener {


    // Shared Preferences Keys
    private static String TAG = "settingsFragment";
    private static String NAME = "NAME";
    private static String AGE = "AGE";
    private static String WEIGHT = "WEIGHT";
    private static String HEIGHT = "HEIGHT";
    private static String EMAIL = "EMAIL";
    private static String MALE = "MALE";
    private static String FEMALE = "FEMALE";
    private static String DIVERS = "DIVERS";
    private static String STATUS = "STATUS";

    private static int request_Code = 1;

    // XML Objects
    Button btButton;
    EditText nameInput,  ageInput, weightInput, heightInput, emailInput, fitnessStatus;
    RadioButton genderInputM, genderInputF, genderInputD;
    TextView connectionState;


    //Heartsensor Objects
    HeartSensorController permissionHeartSensorController;
    BluetoothDevice selectedHeartRateSensor;


    /**
     * Setzt das entsprechende xml Layout, holt sich die xml-Objekte über deren IDs
     * anschließend werden die zuletzt gespeicherten Nutzerdaten geladen und gesetzt.
     * Sollte der HeartRateController beim Starten der Mainactivity sich mit einem Gerät verbunden haben, so wird die
     * Statusanzeige auf "Verbunden" gesetzt.
     */
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        Log.d(TAG, "onCreateView():  ");

        View view = inflater.inflate(R.layout.settings_fragments, container, false);

        nameInput = (EditText) view.findViewById(R.id.nameInput);
        ageInput = (EditText) view.findViewById(R.id.ageInput);
        weightInput = (EditText) view.findViewById(R.id.weightInput);
        heightInput = (EditText) view.findViewById(R.id.heightInput);
        emailInput = (EditText) view.findViewById(R.id.emailInput);
        fitnessStatus = (EditText) view.findViewById(R.id.fittnessStatusInput);
        connectionState = (TextView) view.findViewById(R.id.connectionState);
        genderInputM = (RadioButton) view.findViewById(R.id.genderM);
        genderInputF = (RadioButton) view.findViewById(R.id.genderF);
        genderInputD = (RadioButton) view.findViewById(R.id.genderD);

        btButton = (Button) view.findViewById(R.id.btButton);
        btButton.setOnClickListener(this);

        // Laden der Benutzerdaten
        loadUserFromPref();

        // "Verbunden" falls er mit Verbunden ist
        if (((MainActivity) getActivity()).getIsConnected()) {
            connectionState.setText("Verbunden");
        }

        // Überschreiben der OnBackPressed Methode
        OnBackPressedCallback callback = new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                Log.d(TAG, "onBackPressed");
                NavHostFragment.findNavController(getParentFragment()).navigate(R.id.action_settingsFragment_to_homeFragment);
            }
        };
        requireActivity().getOnBackPressedDispatcher().addCallback(getActivity(), callback);
        return view;
    }

    // Startet die LeDeviceScanActivity und schickt einen Requestcode mit, um anschließend in der onActivityResult darauf antworten zu können.
    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.btButton:
                Log.v(TAG, "btButton clicked");
                // In der Activity kann man sich mit möglichen Bluetoothgeräten verbinden
                startActivityForResult(new Intent(getActivity(), LeDeviceScanActivity.class), request_Code);
                break;
        }
    }

    /**
     * Passt der Requestcode zum Aufruf der startActivityForResult, kann man sich aus der intentdata das Device rausholen.
     * Das Device wird anschließend an die MainActivity weitergeleitet, welche dann auch den Bluetoothdienst startet.
     */
    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == request_Code) {
            if (resultCode == RESULT_OK) {
                ((MainActivity)getActivity()).setSelectedHeartRateSensor(data.getParcelableExtra(LeDeviceScanActivity.SELECTED_DEVICE));
                selectedHeartRateSensor = data.getParcelableExtra(LeDeviceScanActivity.SELECTED_DEVICE);

                ((MainActivity)getActivity()).startBluetoothFromFragment();
                connectionState.setText("Verbunden");

                ((MainActivity)getActivity()).setCurrentAdapter(selectedHeartRateSensor.getAddress());
                saveFragmentPrefs();
            }
        }
    }

    // Lädt die Benutzerdaten
    @Override
    public void onResume() {
        super.onResume();
        loadUserFromPref();
    }

    // Speichert die Benutzerdaten
    @Override
    public void onStop() {
        super.onStop();
        saveFragmentPrefs();
    }

    // Speichert die Benutzerdaten
    @Override
    public void onPause() {
        super.onPause();
        saveFragmentPrefs();
    }

    // Holt sich die Permissions
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        permissionHeartSensorController.onRequestPermissionsResult(requestCode, permissions, grantResults);
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
    }

    // Speichert die daten in über Shared Preferences ab, welche anschließend über die Keys wieder abgerufen werden können
    public void saveFragmentPrefs() {
        Log.v(TAG, "Saving contents");
        SharedPreferences sharedPreferences = getActivity().getSharedPreferences("SHARED_PREFS", Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putString(NAME, nameInput.getText().toString());
        editor.putString(AGE, ageInput.getText().toString());
        editor.putString(WEIGHT, weightInput.getText().toString());
        editor.putString(HEIGHT, heightInput.getText().toString());
        editor.putString(EMAIL, emailInput.getText().toString());
        editor.putString(STATUS, fitnessStatus.getText().toString());
        editor.putBoolean(MALE, genderInputM.isChecked());
        editor.putBoolean(DIVERS, genderInputD.isChecked());
        editor.putBoolean(FEMALE, genderInputF.isChecked());
        // Speichert sich nur das Geschlecht ab, welches ausgewählt ist
        if(genderInputM.isChecked()){
            editor.putString("GENDER", "MALE");
        }else if(genderInputF.isChecked()){
            editor.putString("GENDER", "FEMALE");
        }else if(genderInputD.isChecked()){
            editor.putString("GENDER", "DIVERS");
        }
        editor.apply();
    }

    // Setzt die Benutzerdaten in die Entsprechenden Views beim Laden der Daten wieder ein
    public void loadUserFromPref() {
        Log.v(TAG, "Loading Preferences");
        SharedPreferences sharedPreferences = getActivity().getSharedPreferences("SHARED_PREFS", Context.MODE_PRIVATE);
        nameInput.setText(sharedPreferences.getString(NAME, ""));
        ageInput.setText(sharedPreferences.getString(AGE, ""));
        weightInput.setText(sharedPreferences.getString(WEIGHT, ""));
        heightInput.setText(sharedPreferences.getString(HEIGHT, ""));
        emailInput.setText(sharedPreferences.getString(EMAIL, ""));
        fitnessStatus.setText(sharedPreferences.getString(STATUS, ""));
        genderInputM.setChecked(sharedPreferences.getBoolean(MALE, false));
        genderInputF.setChecked(sharedPreferences.getBoolean(FEMALE, false));
        genderInputD.setChecked(sharedPreferences.getBoolean(DIVERS, false));
    }
}

