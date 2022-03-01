package de.hsfl.tjwa.blheartrateconnection.procedure;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.util.Log;

import androidx.activity.result.ActivityResult;
import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import de.hsfl.tjwa.blheartrateconnection.scan.LeDeviceScanActivity;

/**
 * Controller
 * Project: BluetoothHeartSensor
 *
 * Steuerklasse für das Projekt BluetoothHeartSensor.
 *
 *
 * @author Tjorben Wade
 * @version 1.3 (06.11.2020)
 */
public class Controller extends StateMachine implements  BluetoothController {

    private static final String TAG = "bthsController";

    //Requests
    private final int REQUEST_SCAN_DEVICE = 42;
    private final int REQUEST_ENABLE_BT = 242;
    private final int REQUEST_PERMISSION_ACCESS_FINE_LOCATION = 142;

    //SharedPreferences
    private final String PREFS_KEY = "LE_BTHS_SHARED_PREFERENCES";
    private final String PREFS_DEVICE_KEY = "LE_LATEST_DEVICE_ADDRESS";

    private Activity activity;
    private OnBluetoothListener mUiListener = null;

    private BluetoothDevice mDevice;
    private BluetoothAdapter mBluetoothAdapter;
    private HeartRateGattCallback gattCallback;

    private ActivityResultLauncher<Intent> bluetoothDialogLauncher;
    private ActivityResultLauncher<Intent> deviceListLauncher;

    public static SmMessage[] messageIndex = SmMessage.values();
    public enum SmMessage {
        CONNECT_LOADED_DEVICE, UI_CONNECT_NEW_DEVICE, UI_STOP,                    // from UI
        CO_INIT, BLUETOOTH_INIT,             // to Controller
        BLUETOOTH_ENABLED, PERMISSION_GRANTED, ENABLE_BLUETOOTH_TIMEOUT, GRANT_PERMISSION_TIMEOUT,
        GATT_CONNECTED, GATT_DISCONNECTED, HEART_RATE_RECEIVED //from Callback
    }

    private enum State {
        START, WAIT_FOR_BLUETOOTH, WAIT_FOR_PERMISSION, IDLE, CONNECTED, NO_BLUETOOTH
    }

    private State state = State.START;        // the state variable

    public Controller() {
        Log.d(TAG, "Controller()");
    }

    public static BluetoothController getBluetoothController() {
        Log.d(TAG, "getBluetoothController()");
        return new Controller();
    }

    private void setState(State state) {
        Log.d(TAG, "setState(): " + state.name());
        this.state = state;
        mUiListener.onStateChanged(state.toString());
    }

    @Override
    public void init(Activity a) {
        Log.d(TAG, "init()");
        this.activity = a;
        sendSmMessage(SmMessage.CO_INIT.ordinal(), 0, 0, null);

        deviceListLauncher = ((AppCompatActivity)activity).registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                new ActivityResultCallback<ActivityResult>() {
                    @Override
                    public void onActivityResult(ActivityResult result) {
                        //Ergebnis von Gerätesuche-Activity
                        if (result.getData() != null && result.getData().hasExtra(LeDeviceScanActivity.SELECTED_DEVICE)) {
                            BluetoothDevice device = (BluetoothDevice)result.getData().getExtras().get(LeDeviceScanActivity.SELECTED_DEVICE);
                            if (device != null) {
                                Log.d(TAG, "Selected Device: " + device.getName());
                                //Gerät laden und verbinden
                                Controller.this.mDevice = device;
                                sendSmMessage(SmMessage.CONNECT_LOADED_DEVICE.ordinal(), 0, 0, null);
                            }else{
                                mUiListener.onError(ERROR_NO_SELECTED_DEVICE, "no selected device");
                            }
                        }else{
                            mUiListener.onError(ERROR_NO_SELECTED_DEVICE, "no selected device");
                        }
                    }
                }
        );

        bluetoothDialogLauncher = ((AppCompatActivity) activity).registerForActivityResult(new ActivityResultContracts.StartActivityForResult(),
                new ActivityResultCallback<ActivityResult>() {
                    @Override
                    public void onActivityResult(ActivityResult result) {
                        if (result.getResultCode() == Activity.RESULT_OK) {
                            sendSmMessage(SmMessage.BLUETOOTH_ENABLED.ordinal(), 0, 0, null);
                        }
                    }
                }
        );
    }

    @Override
    public void setListener(OnBluetoothListener onBluetoothListener) {
        Log.d(TAG, "setListener()");
        this.mUiListener = onBluetoothListener;
        /*try {
            mUiListener = (OnBluetoothListener) activity;
        } catch (ClassCastException e) {
            throw new ClassCastException(activity.toString()
                    + " must implement OnBluetoothListener !!!!!!! ");
        }*/
    }

    @Override
    public void start(boolean useLatestDevice) {
        if (useLatestDevice) {
            SharedPreferences pref = activity.getSharedPreferences(PREFS_KEY, 0);
            //Prüfen ob ein Gerät gespeichert wurde
            if (pref.contains(PREFS_DEVICE_KEY)) {
                mDevice = mBluetoothAdapter.getRemoteDevice(pref.getString(PREFS_DEVICE_KEY, ""));
                sendSmMessage(SmMessage.CONNECT_LOADED_DEVICE.ordinal(), 0, 0, null);
            }else{
                mUiListener.onError(ERROR_NO_STORED_DEVICE, "no stored bluetooth device");
            }
        }else {
            sendSmMessage(SmMessage.UI_CONNECT_NEW_DEVICE.ordinal(), 0, 0, null);
        }
    }

    @Override
    public void start(BluetoothDevice device) {
        this.mDevice = device;
        sendSmMessage(SmMessage.CONNECT_LOADED_DEVICE.ordinal(), 0, 0, null);
    }

    @Override
    public void stop() {
        sendSmMessage(SmMessage.UI_STOP.ordinal(), 0, 0, null);
    }


    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (requestCode == REQUEST_PERMISSION_ACCESS_FINE_LOCATION) {
            if (grantResults.length > 0
                    && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                sendSmMessage(SmMessage.PERMISSION_GRANTED.ordinal(), 0, 0, null);
            }else{
                Log.d(TAG, "location permission denied");
            }
        }
    }

    //Speichern des momentanen Gerätes
    private void storeLoadedDevice() {
        if (mDevice != null) {
            SharedPreferences pref = activity.getSharedPreferences(PREFS_KEY, 0);
            SharedPreferences.Editor editor = pref.edit();

            editor.putString(PREFS_DEVICE_KEY, mDevice.getAddress());
            editor.apply();
        }
    }

    /**
     * the statemachine
     *
     *   call it only via sendSmMessage()
     *
     * @param message Message
     */
    @Override
    void theBrain(android.os.Message message) {
        SmMessage inputSmMessage = messageIndex[message.what];
        Log.i(TAG, "SM: Message: " + inputSmMessage.name());

        switch ( state ) {
            case START:
                switch (inputSmMessage) {
                    case CO_INIT:
                        Log.d(TAG, "Init Bluetooth");
                        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
                        if (mBluetoothAdapter == null) {
                            Log.d(TAG, "Error: Device does not support Bluetooth!!!");
                            setState(State.NO_BLUETOOTH);
                            break; // case SmMessage CO_INIT
                        }
                        //Übergang in BLUETOOTH_INIT
                    case BLUETOOTH_INIT:
                        if (!mBluetoothAdapter.isEnabled()) {
                            Log.d(TAG, "Bluetooth is not enabled!");

                            //Zeige eine Meldung zum Aktivieren von Bluetooth
                            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                            //activity.startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
                            bluetoothDialogLauncher.launch(enableBtIntent);

                            setTimer(SmMessage.ENABLE_BLUETOOTH_TIMEOUT.ordinal(), 10000);
                            setState(State.WAIT_FOR_BLUETOOTH);
                            break; // case SmMessage BLUETOOTH_INIT
                        }

                        //Bluetooth ist aktiviert
                        sendSmMessage(SmMessage.BLUETOOTH_ENABLED.ordinal(), 0, 0, null);
                        setState(State.WAIT_FOR_BLUETOOTH);
                        break; // case SmMessage BLUETOOTH_INIT
                    default:
                        Log.v(TAG, "CO-SM: not a valid input in this state: " + inputSmMessage);
                        break;
                }
                break; // state START
            case WAIT_FOR_BLUETOOTH:
                switch (inputSmMessage) {
                    case BLUETOOTH_ENABLED:
                        //Timer für Timeout stoppen
                        stopTimer(SmMessage.ENABLE_BLUETOOTH_TIMEOUT.ordinal());
                        //Bluetooth ist aktiviert. Nun soll die Berechtigung überprüft werden.
                        if ( ContextCompat.checkSelfPermission( activity, android.Manifest.permission.ACCESS_FINE_LOCATION ) != PackageManager.PERMISSION_GRANTED ) {
                            ActivityCompat.requestPermissions( activity, new String[] {  android.Manifest.permission.ACCESS_FINE_LOCATION  },
                                    REQUEST_PERMISSION_ACCESS_FINE_LOCATION );

                            Log.d(TAG, "No location permission to scan for ble devices");

                            setTimer(SmMessage.GRANT_PERMISSION_TIMEOUT.ordinal(), 10000);
                            setState(State.WAIT_FOR_PERMISSION);
                            break;
                        }
                        //Berechtigung bereits erteilt
                        sendSmMessage(SmMessage.PERMISSION_GRANTED.ordinal(), 0, 0, null);
                        setState(State.WAIT_FOR_PERMISSION);
                        break; // case SmMessage BLUETOOTH_ENABLED
                    case ENABLE_BLUETOOTH_TIMEOUT:
                        mUiListener.onError(ERROR_BLUETOOTH_TIMEOUT, "bluetooth timeout");

                        sendSmMessage(SmMessage.BLUETOOTH_INIT.ordinal(), 0, 0, null);
                        setState(State.START);

                        break; // case SmMessage ENABLE_BLUETOOTH_TIMEOUT
                    default:
                        Log.v(TAG, "WAIT_FOR_BLUETOOTH-SM: not a valid input in this state: " + inputSmMessage);
                        break;
                }
                break; // state WAIT_FOR_BLUETOOTH
            case WAIT_FOR_PERMISSION:
                switch (inputSmMessage) {
                    case PERMISSION_GRANTED:
                        //Timer für Timeout stoppen
                        stopTimer(SmMessage.GRANT_PERMISSION_TIMEOUT.ordinal());

                        setState(State.IDLE);

                        break; // case SmMessage PERMISSION_GRANTED
                    case GRANT_PERMISSION_TIMEOUT:
                        mUiListener.onError(ERROR_LOCATION_TIMEOUT, "location timeout");

                        sendSmMessage(SmMessage.BLUETOOTH_ENABLED.ordinal(), 0, 0, null);
                        setState(State.WAIT_FOR_BLUETOOTH);

                        break; // case SmMessage GRANT_PERMISSION_TIMEOUT
                    default:
                        Log.v(TAG, "WAIT_FOR_PERMISSION-SM: not a valid input in this state: " + inputSmMessage);
                        break;
                }
                break; // state WAIT_FOR_BLUETOOTH
            case IDLE:
                switch (inputSmMessage) {
                    case UI_CONNECT_NEW_DEVICE:
                        final Intent intent = new Intent(activity, LeDeviceScanActivity.class);
                        //activity.startActivityForResult(intent, REQUEST_SCAN_DEVICE, a);

                        deviceListLauncher.launch(intent);

                        Log.v(TAG, "ui connect new device");
                        break;
                    case CONNECT_LOADED_DEVICE:
                        Log.v(TAG, "connecting to device");
                        gattCallback = new HeartRateGattCallback(this);
                        //Mit dem Gerät verbinden
                        mDevice.connectGatt(activity, true, gattCallback);
                        setState(State.CONNECTED);
                        break;
                    default:
                        Log.v(TAG, "IDLE-SM: not a valid input in this state: " + inputSmMessage);
                        break;
                }
                break; // state IDLE
            case CONNECTED:
                switch (inputSmMessage) {
                    case GATT_CONNECTED:
                        mUiListener.onConnectionChanged(true, mDevice);
                        storeLoadedDevice(); //Speichern des Gerätes

                        break;
                    case HEART_RATE_RECEIVED:
                        int heartRate = (int)message.obj;
                        mUiListener.onHeartRateUpdated(heartRate);
                        break;
                    case UI_STOP:
                        mUiListener.onConnectionChanged(false, mDevice);
                        if (gattCallback != null) {
                            gattCallback.close();
                        }
                        setState(State.IDLE);
                        break;
                    case GATT_DISCONNECTED:
                        Log.v(TAG, "gatt disconnected");
                        mUiListener.onConnectionChanged(false, mDevice);
                        setState(State.IDLE);
                        break;
                    default:
                        Log.v(TAG, "CONNECTED-SM: not a valid input in this state: " + inputSmMessage);
                        break;
                }
                break; // state CONNECTED
            case NO_BLUETOOTH:
                mUiListener.onError(ERROR_NO_BLUETOOTH, "no bluetooth");
                break; // state NO_BLUETOOTH
        }
    }
}
