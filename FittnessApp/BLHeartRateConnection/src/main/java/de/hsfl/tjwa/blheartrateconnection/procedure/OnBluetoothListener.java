package de.hsfl.tjwa.blheartrateconnection.procedure;

import android.bluetooth.BluetoothDevice;

public interface OnBluetoothListener {
    void onHeartRateUpdated(int heartRate); //Neue Herzrate empfangen
    void onStateChanged(String strState); //Status der Steuerklasse hat sich geändert
    void onConnectionChanged(boolean connected, BluetoothDevice device); //Status der Verbindung zum Gatt hat sich geändert
    void onError(int code, String message);
}
