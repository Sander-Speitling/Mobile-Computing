package de.hsfl.tjwa.blheartrateconnection.scan;

import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import java.util.ArrayList;

import de.hsfl.tjwa.blheartrateconnection.R;

/**
 * Created by Gorb98 on 20.11.2019.
 * Project: BluetoothHerzsensor
 */
public class LeDeviceListAdapter extends BaseAdapter {
    private Context context;
    private ArrayList<BluetoothDevice> mLeDevices;

    public LeDeviceListAdapter(Context context) {
        super();
        this.context = context;
        mLeDevices = new ArrayList<>();
    }

    public void addDevice(BluetoothDevice device) {
        if(!mLeDevices.contains(device)) {
            mLeDevices.add(device);
        }
    }

    public BluetoothDevice getDevice(int position) {
        return mLeDevices.get(position);
    }

    public void clear() {
        mLeDevices.clear();
    }

    @Override
    public int getCount() {
        return mLeDevices.size();
    }

    @Override
    public Object getItem(int i) {
        return mLeDevices.get(i);
    }

    @Override
    public long getItemId(int i) {
        return i;
    }

    @Override
    public View getView(int i, View view, ViewGroup viewGroup) {
        DeviceViewHolder deviceView;

        if (view == null) {
            //Layout listitem_device.xml übernehmen
            view = LayoutInflater.from(context).
                    inflate(R.layout.listitem_device, viewGroup, false);

            //Textfelder zuordnen
            deviceView = new DeviceViewHolder(view);
            view.setTag(deviceView);
        } else {
            //Vorhandene LayoutZuordnung laden
            deviceView = (DeviceViewHolder) view.getTag();
        }

        //Gerätename und Adresse auf Textfeldern zeigen
        BluetoothDevice device = mLeDevices.get(i);

        final String deviceName = device.getName();
        if (deviceName != null && deviceName.length() > 0) {
            //Ein Gerätename ist vorhanden
            deviceView.deviceName.setText(deviceName);
        } else {
            //Kein Gerätename bekannt
            deviceView.deviceName.setText(context.getString(R.string.unknown_device));
        }
        deviceView.deviceAddress.setText(device.getAddress());
        return view;
    }

    public class DeviceViewHolder {
        TextView deviceAddress;
        TextView deviceName;

        public DeviceViewHolder(View itemView) {
            deviceAddress = itemView.findViewById(R.id.device_address);
            deviceName = itemView.findViewById(R.id.device_name);
        }
    }
}