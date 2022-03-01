package de.hsfl.tjwa.blheartrateconnection.procedure;

import android.app.Activity;
import android.bluetooth.BluetoothDevice;
import android.content.Intent;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

public interface BluetoothController {
    int ERROR_NO_STORED_DEVICE = 42001;
    int ERROR_NO_BLUETOOTH = 42002;
    int ERROR_BLUETOOTH_TIMEOUT = 42003;
    int ERROR_LOCATION_TIMEOUT = 42004;
    int ERROR_NO_SELECTED_DEVICE = 42005;
    int NO_ERROR = 42200;

    void init(Activity a);
    void setListener(OnBluetoothListener onBluetoothListener);     // falls BT von einer Activity aus genutzt wird
    void start(boolean useLastDevice);
    void start(BluetoothDevice device);
    void stop();
    //void onActivityResult(int requestCode, int resultCode, @Nullable Intent data);
    void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults);
}
