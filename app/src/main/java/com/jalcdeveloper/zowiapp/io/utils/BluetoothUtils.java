package com.jalcdeveloper.zowiapp.io.utils;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.os.ParcelUuid;
import android.util.Log;

import java.net.UnknownHostException;
import java.util.Set;

public class BluetoothUtils {

    private static final String TAG = BluetoothUtils.class.getSimpleName();
    public final static String UUID_SPP_DEVICE = "00001101-0000-1000-8000-00805F9B34FB";

    public static BluetoothDevice findSerial(BluetoothAdapter bluetoothAdapter) throws UnknownHostException{

        if(bluetoothAdapter != null){

            Set<BluetoothDevice> pariedDevices = bluetoothAdapter.getBondedDevices();
            if(pariedDevices.size() > 0){

                for(BluetoothDevice device: pariedDevices){
                    Log.d(TAG, device.getName() + ": " + device.getAddress());

                    for(ParcelUuid uuid: device.getUuids()){
                        if(uuid.toString().equalsIgnoreCase(UUID_SPP_DEVICE)){
                            Log.d(TAG, "Device found serial in: " + device.getName());
                            return device;
                        }
                    }

                }
            }
        }
        throw new UnknownHostException("No Bluetooth Device found");
    }

}
