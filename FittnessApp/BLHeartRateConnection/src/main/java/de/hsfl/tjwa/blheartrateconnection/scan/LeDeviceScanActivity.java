package de.hsfl.tjwa.blheartrateconnection.scan;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothManager;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanResult;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.ListView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import de.hsfl.tjwa.blheartrateconnection.R;

/**
 * DeviceScanActivity
 *
 * Die Activity scannt nach Bluetooth LE-Geräten. Das Scannen startet, wenn die
 * Activity geladen wird und stoppt nach 10 Sekunden. Durch den bToogleSearch-Button
 * kann das Scannen erneut gestartet werden.
 *
 * Wenn ein Bluetooth-LE-Gerät in der Liste ausgewählt wurde, liefert die Activity ein
 * Ergebnis zurück.
 *
 * Zum Scannen nach LE-Geräten wird seit API 23 die Permission ACCESS_FINE_LOCATION benötigt, da LE-Beacons
 * häufig mit dem Standort verknüpft werden.
 * Zum Nachlesen:
 *      - https://developer.android.com/guide/topics/connectivity/bluetooth-le#permissions
 *      - https://stackoverflow.com/questions/41716452/why-location-permission-are-required-for-ble-scan-in-android-marshmallow-onwards
 *
 * @author Tjorben Wade
 * @version 1.0 (15.12.2019)
 */
public class LeDeviceScanActivity extends AppCompatActivity {

    public static final String SELECTED_DEVICE = "SELECTED_DEVICE";

    //Button
    private Button bToggleSearch;

    private BluetoothLeScanner leScanner;
    private boolean isScanning;
    private Handler handler;

    //Scannen stoppen nach
    private static final long SCAN_PERIOD = 10000;
    private LeDeviceListAdapter leDeviceListAdapter;

    BluetoothAdapter btAdapter;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        handler = new Handler();
        setContentView(R.layout.activity_device_scan);

        //Suchenbutton
        bToggleSearch = findViewById(R.id.bToggleSearch);
        bToggleSearch.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                scanLeDevice(!isScanning);
            }
        });

        //Zugreifen auf Bluetooth LE Scanner Instanz
        final BluetoothManager bluetoothManager =
                (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);

        btAdapter = BluetoothAdapter.getDefaultAdapter();
        if (btAdapter == null) {
            Log.d("ScanActivity", "no Bluetooth available");
        } else {
            leScanner = bluetoothManager.getAdapter().getBluetoothLeScanner();
        }

        if (leScanner == null) {
            Toast.makeText(this, R.string.bluetooth_not_available, Toast.LENGTH_LONG).show();
            setResult(RESULT_CANCELED);
            finish();
        }else{
            //Vorbereiten des List-Adapters
            leDeviceListAdapter = new LeDeviceListAdapter(this);
            ListView listViewBLEDevices = findViewById(R.id.lvBLEDevices);
            listViewBLEDevices.setAdapter(leDeviceListAdapter);

            //Beim Klicken auf ein Item werden die Geräte Daten an die Parent-Activity zurückgegeben
            listViewBLEDevices.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                @Override
                public void onItemClick(AdapterView<?> adapterView, View view, int position, long l) {
                    scanLeDevice(false);
                    Intent data = new Intent();
                    data.putExtra(SELECTED_DEVICE, leDeviceListAdapter.getDevice(position));
                    setResult(RESULT_OK, data);
                    finish();
                }
            });
            scanLeDevice(true);
        }
    }


    /**
     * Starten/Stoppen des Scannen nach BLE Geräten
     * Ein gestarteter Scan wird nach SCAN_PERIOD ms gestoppt
     *
     * @param enable true um Scan zu starten / false um zu Stoppen
     */
    private void scanLeDevice(final boolean enable) {
        if (enable) {
            //Löschen der Liste
            leDeviceListAdapter.clear();
            //Scan nach festgelegter Zeit stoppen
            handler.postDelayed(rTimeout, SCAN_PERIOD);

            isScanning = true;
            //Ändern des Button Textes
            bToggleSearch.setText(getString(R.string.stop_search));
            leScanner.startScan(leScanCallback);
        } else {
            handler.removeCallbacks(rTimeout);
            isScanning = false;
            bToggleSearch.setText(getString(R.string.start_search));
            leScanner.stopScan(leScanCallback);
        }
    }

    /**
     * Stoppt das Scannen
     */
    private Runnable rTimeout = new Runnable() {
        @Override
        public void run() {
            scanLeDevice(false);
        }
    };

    /**
     * Wird aufgerufen, wenn ein Bluetooth LE fähiges Gerät gefunden wurde
     */
    private ScanCallback leScanCallback = new ScanCallback() {
        @Override
        public void onScanResult(int callbackType, final ScanResult result) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    leDeviceListAdapter.addDevice(result.getDevice());
                    leDeviceListAdapter.notifyDataSetChanged();
                }
            });
        }
    };
}
