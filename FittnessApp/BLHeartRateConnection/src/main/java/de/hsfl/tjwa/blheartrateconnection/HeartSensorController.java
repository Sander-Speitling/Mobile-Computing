package de.hsfl.tjwa.blheartrateconnection;

import android.app.Activity;
import android.bluetooth.BluetoothDevice;
import android.content.Intent;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import java.util.Timer;
import java.util.TimerTask;
import de.hsfl.tjwa.blheartrateconnection.procedure.BluetoothController;
import de.hsfl.tjwa.blheartrateconnection.procedure.Controller;
import de.hsfl.tjwa.blheartrateconnection.procedure.OnBluetoothListener;

/**
 * HeartSensorController
 * Hauptklasse des BLHeartRateConnection Projektes
 *
 * Stellt die Verbindung zum Projekt BluetoothHeartRate her
 * und stellt Herzdaten bereit.
 *
 * @author Tjorben Wade
 * @version 1.4 (06.11.2020)
 */
public class HeartSensorController implements OnBluetoothListener {

    //nicht geteilt
    private Activity activity;
    private BluetoothDevice bluetoothDevice;
    private BluetoothController bluetoothController;
    private boolean useLatestDevice;
    private boolean connect;
    //geteilte Daten
    private MutableLiveData<Integer> errorCode;
    private MutableLiveData<String> errorString;
    private MutableLiveData<Integer> heartRate;
    private MutableLiveData<Boolean> connectionState;
    private MutableLiveData<BluetoothDevice> connectedDevice;

    //Simulation
    private boolean simulate;
    private Timer simulationTimer;

    public HeartSensorController(Activity activity) {
        this.activity = activity;
        this.errorCode = new MutableLiveData<>();
        this.errorCode.setValue(BluetoothController.NO_ERROR);
        this.errorString = new MutableLiveData<>();
        this.heartRate = new MutableLiveData<>();
        this.connectionState = new MutableLiveData<>();
        this.connectionState.setValue(false);
        this.connectedDevice = new MutableLiveData<>();
    }

    /**
     * Startet eine Simulation einer Verbindung mit einem Herzsensor
     */
    public void startSimulation(int updatePeriod) {
        stopAll();
        this.simulate = true;
        this.errorCode.setValue(BluetoothController.NO_ERROR);
        this.errorString.setValue(null);

        this.simulationTimer = new Timer();
        TimerTask timerTask = new TimerTask() {
            @Override
            public void run() {
                simulateHeartRate(55, 150);
            }
        };
        this.simulationTimer.scheduleAtFixedRate(timerTask, 0, updatePeriod);
        this.connectionState.setValue(true);
    }

    /**
     * Startet den Prozess zum Verbindungsaufbau mit einem Bluetooth Herzsensor
     * @param useLatestDevice Letztes verbundenes Gerät nutzen
     */
    public void startBluetooth(boolean useLatestDevice) {
        this.useLatestDevice = useLatestDevice;
        this.startBluetooth(null);
    }

    /**
     * Startet den Prozess zum Verbindungsaufbau mit einem Bluetooth Herzsensor
     * @param bluetoothDevice Bluetoothgerät, mit dem eine Verbindung aufgebaut werden soll
     */
    public void startBluetooth(BluetoothDevice bluetoothDevice) {
        stopAll();
        this.connect = true;
        this.simulate = false;
        this.errorCode.setValue(BluetoothController.NO_ERROR);
        this.errorString.setValue(null);
        this.bluetoothDevice = bluetoothDevice;

        this.bluetoothController = Controller.getBluetoothController();
        this.bluetoothController.init(activity);
        this.bluetoothController.setListener(this);
    }

    /**
     * Simuliert Herzraten
     */
    private void simulateHeartRate(int min, int max) {
        if (this.heartRate.getValue() == null || this.heartRate.getValue() <= 0) {
            this.heartRate.postValue(randRange(min, max));
        }else{
            int maxSub = Math.min(this.heartRate.getValue() - 55, 7);
            int maxAdd = Math.min(max - this.heartRate.getValue(), 7);
            this.heartRate.postValue(this.heartRate.getValue() + randRange(-maxSub, maxAdd));
        }
    }

    private static int randRange(int min, int max) {
        return (int) ((Math.random() * (max - min)) + min);
    }

    /**
     * Gibt die per Bluetooth empfangene oder
     * simulierte Herzrate zurück.
     * @return Herzrate
     */
    public LiveData<Integer> getHeartRate() {
        return this.heartRate;
    }

    /**
     * Gibt zurück, ob ein Herzsensor verbunden ist
     * @return Verbindungsstatus
     */
    public Boolean isConnected() {
        return this.connect;
    }

    /**
     * Gibt das verbundene Gerät zurück
     * @return Bluetooth-Gerät (null -> kein Gerät verbunden oder Simulation)
     */
    public LiveData<BluetoothDevice> getConnectedDevice() {
        return this.connectedDevice;
    }

    /**
     * Gibt zurück, ob Herzdaten momentan simuliert werden
     * @return true wenn Daten simuliert werden
     */
    public boolean isSimulating() {
        return this.simulate;
    }


    /**
     * Gibt den letzten Fehler zurück
     * @return Fehlermeldung | null wenn kein Fehler auftritt
     */
    public LiveData<String> getErrorString() {
        return this.errorString;
    }

    /**
     * Gibt den letzten Fehlercode zurück
     * @return Fehlercode (Entspricht einem Code aus BluetoothController)
     */
    public LiveData<Integer> getErrorCode() {
        return this.errorCode;
    }


    @Override
    public void onHeartRateUpdated(int heartRate) {
        this.heartRate.setValue(heartRate);
    }

    @Override
    public void onStateChanged(String strState) {
        if (strState.equals("IDLE") && this.bluetoothController != null && this.connect) {
            this.connect = false;
            if (this.bluetoothDevice == null) {
                this.bluetoothController.start(this.useLatestDevice);
            }else {
                this.bluetoothController.start(this.bluetoothDevice);
            }
        }
    }

    @Override
    public void onConnectionChanged(boolean connected, BluetoothDevice device) {
        this.connectionState.setValue(connected);
        this.connectedDevice.setValue(device);
    }

    @Override
    public void onError(int code, String message) {
        if (code == BluetoothController.ERROR_BLUETOOTH_TIMEOUT) {
            //Toast.makeText(activity, activity.getString(R.string.bluetooth_not_enabled), Toast.LENGTH_LONG).show();
            this.errorString.setValue(activity.getString(R.string.bluetooth_not_enabled));
        }else if (code == BluetoothController.ERROR_LOCATION_TIMEOUT) {
            //Toast.makeText(activity, activity.getString(R.string.missing_permission), Toast.LENGTH_LONG).show();
            this.errorString.setValue(activity.getString(R.string.missing_permission));
        }else if (code == BluetoothController.ERROR_NO_SELECTED_DEVICE) {
            this.errorString.setValue(activity.getString(R.string.no_selected_device));
        }else{
            this.errorString.setValue(message);
            //Toast.makeText(activity, message, Toast.LENGTH_LONG).show();
        }
        this.errorCode.setValue(code);
    }

    /**
     * stoppt Bluetooth-Updates und das Generieren von simulierten Herzraten
     */
    public void stopAll() {
        this.connectionState.setValue(false);
        if (this.bluetoothController != null) {
            this.bluetoothController.stop();
        }
        if (this.simulationTimer != null) {
            this.simulationTimer.cancel();
            this.simulationTimer = null;
        }
    }

    /**
     * Verarbeitet zugeteilte Berechtigungen
     * TODO: in späteren Versionen entfernen
     * @param requestCode von onRequestPermissionsResult() der registrierten Activity
     * @param permissions von onRequestPermissionsResult() der registrierten Activity
     * @param grantResults von onRequestPermissionsResult() der registrierten Activity
     */
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (this.bluetoothController != null) {
            this.bluetoothController.onRequestPermissionsResult(requestCode, permissions, grantResults);
        }
    }
}
