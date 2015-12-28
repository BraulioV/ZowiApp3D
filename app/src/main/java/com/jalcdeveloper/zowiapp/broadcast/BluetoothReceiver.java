package com.jalcdeveloper.zowiapp.broadcast;

import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.support.v7.app.AlertDialog;
import android.util.Log;

import com.jalcdeveloper.zowiapp.ui.dialogs.BluetoothListDialog;

public class BluetoothReceiver extends BroadcastReceiver
                                implements DialogInterface.OnClickListener{

    private static final String TAG = BluetoothReceiver.class.getSimpleName();
    private Context mContext;
    private BluetoothListDialog bluetoothListDialog;

    public BluetoothReceiver( Context context) {

        super();

        this.mContext = context;

        AlertDialog.Builder builder = new AlertDialog.Builder(context)
                .setTitle("Title")
                .setCancelable(true)
                .setNegativeButton("Cancel",
                        new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialogInterface, int i) {
                                bluetoothListDialog.dismiss();
                            }
                        });

        bluetoothListDialog = new BluetoothListDialog(context,
                builder, this);

    }

    @Override
    public void onReceive(Context context, Intent intent) {

        if(BluetoothDevice.ACTION_FOUND.equals(intent.getAction())){

            BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
            Log.d(TAG, "Blueooth device found : " + device.getName());
            bluetoothListDialog.addItem(device.getName(), device.getAddress());
        }
    }
    
    @Override
    public void onClick(DialogInterface dialogInterface, int i) {


    }
}
