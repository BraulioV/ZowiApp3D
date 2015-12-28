package com.jalcdeveloper.zowiapp.ui;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.support.v7.app.AlertDialog;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import com.jalcdeveloper.zowiapp.R;
import com.jalcdeveloper.zowiapp.ZowiApp;
import com.jalcdeveloper.zowiapp.io.Zowi;
import com.jalcdeveloper.zowiapp.ui.dialogs.BluetoothListDialog;

import java.io.IOException;
import java.util.Set;

public class ConnectActivity extends ImmersiveActivity
        implements DialogInterface.OnClickListener{

    private static String TAG = ConnectActivity.class.getSimpleName();
    private Button buttonConnect;
    private TextView textMessages;
    private BluetoothAdapter mBluetoothAdapter;
    private BluetoothReceiver mBluetoothReceiver;
    private BluetoothListDialog bluetoothListDialog;
    private Zowi zowi;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_connect);

        zowi = ((ZowiApp)getApplication()).zowi;

        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        mBluetoothReceiver = new BluetoothReceiver();

        textMessages = (TextView) findViewById(R.id.text_messages);
        buttonConnect = (Button) findViewById(R.id.button_connect);
        buttonConnect.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                connectOnClick();
            }
        });

        AlertDialog.Builder builder = new AlertDialog.Builder(this)
                .setTitle("Title")
                .setCancelable(true)
                .setNegativeButton("Cancel",
                        new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialogInterface, int i) {
                                bluetoothListDialog.dismiss();
                            }
                        });

        bluetoothListDialog = new BluetoothListDialog(this, builder, this);


    }

    @Override
    protected void onResume() {
        super.onResume();

        float dpi = getResources().getDisplayMetrics().density;

        registerReceiver(mBluetoothReceiver,
                new IntentFilter(BluetoothDevice.ACTION_FOUND));
    }

    @Override
    protected void onPause() {
        super.onPause();
        unregisterReceiver(mBluetoothReceiver);
    }

    private void connectOnClick(){

        bluetoothListDialog.dismiss();
        bluetoothListDialog.clear();

        if(mBluetoothAdapter.isEnabled()){

            textMessages.setText("Buscando a Zowi...");

            // Bluetooth Paried devices
            Set<BluetoothDevice> pariedDevices = mBluetoothAdapter.getBondedDevices();
            if(pariedDevices.size() > 0){
                for(BluetoothDevice device : pariedDevices){
                    bluetoothListDialog.addItem(device.getName(), device.getAddress());
                }
            }

            mBluetoothAdapter.startDiscovery();
            bluetoothListDialog.show();

        }else{
            textMessages.setText("Activa el Bluetooth para encontrar a Zowi.");
            Log.e(TAG, "Error bluetooth is disabled");
        }

    }

    @Override
    public void onClick(DialogInterface dialogInterface, int i) {
        textMessages.setText("Connectando " + bluetoothListDialog.getItemKey(i));
        Log.d(TAG, "Selected: " + bluetoothListDialog.getItemKey(i));
        try {
            zowi.connect(bluetoothListDialog.getItemValue(i));
            startActivity(new Intent(this, BasicControlActivity.class));
        }catch (IOException ioEx){
            textMessages.setText("Oh! No hemos podido conectar con Zowi");
            Log.e(TAG, "Error: " + ioEx.getMessage());
        }
    }

    private class BluetoothReceiver extends BroadcastReceiver{
        @Override
        public void onReceive(Context context, Intent intent) {

            if(BluetoothDevice.ACTION_FOUND.equals(intent.getAction())){

                BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                Log.d(TAG, "Blueooth device found : " + device.getName() + " : " + device.getAddress());
                if(device.getName() != null && device.getAddress()!=null) {
                    bluetoothListDialog.addItem(device.getName(), device.getAddress());
                }
            }
        }
    }

}
