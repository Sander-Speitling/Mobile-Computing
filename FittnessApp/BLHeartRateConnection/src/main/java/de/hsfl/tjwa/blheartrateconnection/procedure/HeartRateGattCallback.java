package de.hsfl.tjwa.blheartrateconnection.procedure;

import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.util.Log;

import java.util.UUID;

import static android.bluetooth.BluetoothAdapter.STATE_CONNECTED;
import static android.bluetooth.BluetoothAdapter.STATE_DISCONNECTED;

/**
 * Created by Tjorben Wade on 07.01.2020.
 * Project: BluetoothHeartSensor
 *
 * Orientiert an https://medium.com/@avigezerit/bluetooth-low-energy-on-android-22bc7310387a
 */
public class HeartRateGattCallback extends BluetoothGattCallback {
    private static final String TAG = "bthsGattCallback";
    private Controller mController;
    private BluetoothGatt mGatt;

    private UUID HEART_RATE_SERVICE_UUID = convertFromInteger(0x180D); //HEART RATE SERVICE
    private UUID HEART_RATE_MEASUREMENT_CHAR_UUID = convertFromInteger(0x2A37); //Eigenschaft zum Senden der Herzfrequenzmessung (Characteristic) --> https://www.bluetooth.com/specifications/gatt/characteristics/
    private UUID  HEART_RATE_CONTROL_POINT_CHAR_UUID = convertFromInteger(0x2A39); //Eigenschaft zum Schreiben von Kontrollpunkten auf dem Gatt-Server, um das Verhalten zu steuern (Characteristic)

    /**
     * Kovertiert eine Integer in eine UUID
     * Übernommen von https://medium.com/@avigezerit/bluetooth-low-energy-on-android-22bc7310387a
     * @param i Integer zum konvertieren
     * @return UUID
     */
    public UUID convertFromInteger(int i) {
        final long MSB = 0x0000000000001000L;
        final long LSB = 0x800000805f9b34fbL;
        return new UUID(MSB | ((long) i << 32), LSB);
    }

    public HeartRateGattCallback(Controller controller) {
        this.mController = controller;
    }

    @Override
    public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
        this.mGatt = gatt;
        Log.d(TAG, "CallbackState: " + newState);
        if (newState == STATE_CONNECTED){
            //Wenn eine Verbindung hergestellt wurde, sollen die Services des Gatt-Services herausgefunden werden
            Log.d(TAG, "Gatt Connected");
            gatt.discoverServices();
            mController.obtainMessage(Controller.SmMessage.GATT_CONNECTED.ordinal(), 0, 0, null).sendToTarget();
        }else if (newState == STATE_DISCONNECTED) {
            mGatt.disconnect();
            mGatt.close();
            Log.d(TAG, "Gatt Disconnected");
            mController.obtainMessage(Controller.SmMessage.GATT_DISCONNECTED.ordinal(), 0, 0, null).sendToTarget();
        }
    }

    @Override
    public void onServicesDiscovered(BluetoothGatt gatt, int status){
        //Aktivieren der Benachrichtigungen für die Herzfrequenzmessung
        BluetoothGattCharacteristic characteristic =
                gatt.getService(HEART_RATE_SERVICE_UUID)
                        .getCharacteristic(HEART_RATE_MEASUREMENT_CHAR_UUID);
        gatt.setCharacteristicNotification(characteristic, true);

        //Auf den Deskriptor schreiben um Benachrichtigungen zu aktivieren
        UUID CLIENT_CHARACTERISTIC_CONFIG_UUID = convertFromInteger(0x2902);

        BluetoothGattDescriptor descriptor =
                characteristic.getDescriptor(CLIENT_CHARACTERISTIC_CONFIG_UUID);

        descriptor.setValue(
                BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
        gatt.writeDescriptor(descriptor);
    }

    @Override
    public void onDescriptorWrite(BluetoothGatt gatt, BluetoothGattDescriptor descriptor, int status){
        //Auf die Charakteristik für den Kontrollpunkt schreiben, dass der Sensor mit dem Streamen starten soll
        BluetoothGattCharacteristic characteristic =
                gatt.getService(HEART_RATE_SERVICE_UUID)
                        .getCharacteristic(HEART_RATE_CONTROL_POINT_CHAR_UUID);
        if (characteristic != null) {
            characteristic.setValue(new byte[]{1, 1}); //Wird als Streaming Befehl verstanden
            gatt.writeCharacteristic(characteristic); //Schreiben
        }
    }

    @Override
    public void onCharacteristicChanged(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic) {
        int flag = characteristic.getProperties();
        int format;
        //Format UINT16 (max 65535) oder UINT8 (max 255)
        Log.d(TAG, String.format("Flag: %d", flag));
        if ((flag & 0x01) != 0) {
            format = BluetoothGattCharacteristic.FORMAT_UINT16;
            Log.d(TAG, "Herzratenformat UINT16.");
        } else {
            format = BluetoothGattCharacteristic.FORMAT_UINT8;
            Log.d(TAG, "Herzratenformat UINT8.");
        }
        //Laden der Herzrate von Characteristic unter Angabe des Formats
        int heartRate = characteristic.getIntValue(format, 1);
        Log.d(TAG, String.format("Empfangen der Herzrate: %d", heartRate));
        mController.obtainMessage(Controller.SmMessage.HEART_RATE_RECEIVED.ordinal(), 0, 0, heartRate).sendToTarget();
    }

    public void close() {
        if (this.mGatt != null) {
            mGatt.close();
        }
    }
}
