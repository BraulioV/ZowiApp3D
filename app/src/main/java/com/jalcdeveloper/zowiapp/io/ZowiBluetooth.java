package com.jalcdeveloper.zowiapp.io;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Context;
import android.util.Log;

import com.jalcdeveloper.zowiapp.io.utils.BluetoothUtils;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.UUID;

public class ZowiBluetooth implements Zowi {

    private static final String TAG = ZowiBluetooth.class.getSimpleName();

    private BluetoothAdapter mBluetoothAdapter;
    private BluetoothSocket mBluetoothSocket;
    private BluetoothDevice mDevice;
    private OutputStream outputStream;
    private InputStream inputStream;

    private int speed = Zowi.NORMAL_SPEED;
    private int dir = Zowi.FORWARD_DIR;

    private Context mContext;
    private SerialCommand serialCommand;
    private MoveListener moveListener;
    private RequestListener requestListener;

    public ZowiBluetooth(Context context) {

        this.mContext = context;
        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();

    }

    @Override
    public void connect(String address) throws IOException {

        Log.d(TAG, "Connect Zowi Bluetooth");

        resetConnection();

        try {
            mDevice = mBluetoothAdapter.getRemoteDevice(address);
        }catch (IllegalArgumentException ex){
            Log.e(TAG, "ERROR: Device may have been removed");
        }

        //if(mDevice != null){
        //    mDevice = BluetoothUtils.findSerial(mBluetoothAdapter);
        //}

        mBluetoothSocket = mDevice.createInsecureRfcommSocketToServiceRecord(
                UUID.fromString(BluetoothUtils.UUID_SPP_DEVICE));

        mBluetoothAdapter.cancelDiscovery();
        mBluetoothSocket.connect();

        outputStream = mBluetoothSocket.getOutputStream();
        inputStream = mBluetoothSocket.getInputStream();

        serialCommand = new SerialCommand(mContext, inputStream, outputStream);
        serialCommand.start();

        serialCommand.addListener(""+ZowiProtocol.FINAL_ACK_COMMAND, new AckFinalReceived());
        serialCommand.addListener(""+ZowiProtocol.ACK_COMMAND, new AckReceived());
        serialCommand.addListener(""+ZowiProtocol.BATTERY_COMMAND, new BatteryReceived());
        serialCommand.addListener(""+ZowiProtocol.PROGRAMID_COMMAND, new ProgramIdReceived());

    }

    private void resetConnection() throws IOException {
        if(inputStream != null){
            inputStream.close();
            inputStream = null;
        }

        if(outputStream != null){
            outputStream.close();
            outputStream = null;
        }

        if(mBluetoothSocket != null){
            mBluetoothSocket.close();
            mBluetoothSocket = null;
        }
    }

    @Override
    public void disconnect() throws IOException {
        resetConnection();
    }

    @Override
    public void home() throws IOException {

        String command = String.format(
                "" + ZowiProtocol.MOVE_COMMAND +
                        ZowiProtocol.SEPARATOR +
                        ZowiProtocol.MOVE_STOP_OPTION +
                        ZowiProtocol.FINAL);

        serialCommand.write(command.getBytes());

    }

    @Override
    public void walk(float steps, int time, int dir) throws IOException{

        char direction = ZowiProtocol.MOVE_WALK_FORWARD_OPTION;

        if(dir == Zowi.BACKWARD_DIR){
            direction = ZowiProtocol.MOVE_WALK_BACKWARD_OPTION;
        }

        String command = String.format(
                "" + ZowiProtocol.MOVE_COMMAND +
                        ZowiProtocol.SEPARATOR +
                        direction +
                        ZowiProtocol.SEPARATOR +
                        "%d" +
                        ZowiProtocol.FINAL, time);

        serialCommand.write(command.getBytes());

    }

    @Override
    public void turn(float steps, int time, int dir) throws IOException {

        char direction = ZowiProtocol.MOVE_TURN_RIGHT_OPTION;

        if(dir == Zowi.LEFT_DIR){
            direction = ZowiProtocol.MOVE_TURN_LEFT_OPTION;
        }

        String command = String.format(
                "" + ZowiProtocol.MOVE_COMMAND +
                        ZowiProtocol.SEPARATOR +
                        direction +
                        ZowiProtocol.SEPARATOR +
                        "%d" +
                        ZowiProtocol.FINAL, time);

        serialCommand.write(command.getBytes());

    }

    @Override
    public void updown(float steps, int time, int h) throws IOException {


        String command = String.format(
                "" + ZowiProtocol.MOVE_COMMAND +
                        ZowiProtocol.SEPARATOR +
                        ZowiProtocol.MOVE_UPDOWN_OPTION +
                        ZowiProtocol.SEPARATOR +
                        "%d" +
                        ZowiProtocol.SEPARATOR +
                        "%d" +
                        ZowiProtocol.FINAL, time, h);

        serialCommand.write(command.getBytes());

    }

    @Override
    public void moonwalker(float steps, int time, int h, int dir) throws IOException {

        char direction = ZowiProtocol.MOVE_MOONWALKER_RIGHT_OPTION;

        if(dir == Zowi.LEFT_DIR){
            direction = ZowiProtocol.MOVE_MOONWALKER_LEFT_OPTION;
        }

        String command = String.format(
                "" + ZowiProtocol.MOVE_COMMAND +
                        ZowiProtocol.SEPARATOR +
                        direction +
                        ZowiProtocol.SEPARATOR +
                        "%d" +
                        ZowiProtocol.SEPARATOR +
                        "%d" +
                        ZowiProtocol.FINAL, time, h);

        serialCommand.write(command.getBytes());

    }

    @Override
    public void swing(float steps, int time, int h) throws IOException {

        String command = String.format(
                "" + ZowiProtocol.MOVE_COMMAND +
                        ZowiProtocol.SEPARATOR +
                        ZowiProtocol.MOVE_SWING_OPTION +
                        ZowiProtocol.SEPARATOR +
                        "%d" +
                        ZowiProtocol.SEPARATOR +
                        "%d" +
                        ZowiProtocol.FINAL, time, h);

        serialCommand.write(command.getBytes());

    }

    @Override
    public void crusaito(float steps, int time, int h, int dir) throws IOException {

        String direction = ZowiProtocol.MOVE_CRUSAITO_RIGHT_OPTION;

        if(dir == Zowi.LEFT_DIR){
            direction = ZowiProtocol.MOVE_CRUSAITO_LEFT_OPTION;
        }

        String command = String.format(
                "" + ZowiProtocol.MOVE_COMMAND +
                        ZowiProtocol.SEPARATOR +
                        direction +
                        ZowiProtocol.SEPARATOR +
                        "%d" +
                        ZowiProtocol.SEPARATOR +
                        "%d" +
                        ZowiProtocol.FINAL, time, h);

        serialCommand.write(command.getBytes());

    }

    @Override
    public void jump(float steps, int time) throws IOException {

        String command = String.format(
                "" + ZowiProtocol.MOVE_COMMAND +
                        ZowiProtocol.SEPARATOR +
                        ZowiProtocol.MOVE_JUMP_OPTION +
                        ZowiProtocol.SEPARATOR +
                        "%d" +
                        ZowiProtocol.FINAL, time);

        serialCommand.write(command.getBytes());
    }

    @Override
    public void setRequestListener(RequestListener listener) {
        this.requestListener = listener;
    }

    @Override
    public void batteryRequest() {

        String command = String.format(
                "" + ZowiProtocol.BATTERY_COMMAND +
                        ZowiProtocol.FINAL);

        serialCommand.write(command.getBytes());
    }

    @Override
    public void programIdRequest() {

        String command = String.format(
                "" + ZowiProtocol.PROGRAMID_COMMAND +
                        ZowiProtocol.FINAL);

        serialCommand.write(command.getBytes());

    }

    @Override
    public void setSpeed(int speed) {
        this.speed = speed;
    }

    @Override
    public void setDirection(int dir) {
        this.dir = dir;
    }

    @Override
    public int getSpeed() {
        return this.speed;
    }

    @Override
    public int getDirection() {
        return this.dir;
    }

    @Override
    public void setMoveListener(MoveListener listener) {
        this.moveListener = listener;
    }

    /**
     *
     */
    private class AckReceived implements SerialCommand.SerialCommandListener {
        @Override
        public void onReviced(String[] argumentList) {
            if(moveListener != null) moveListener.onAck();
        }
    }

    /**
     *
     */
    private class AckFinalReceived implements SerialCommand.SerialCommandListener{
        @Override
        public void onReviced(String[] argumentList) {
            if(moveListener != null) moveListener.onFinishAck();
        }
    }

    /**
     *
     */
    private class BatteryReceived implements SerialCommand.SerialCommandListener{
        @Override
        public void onReviced(String[] argumentList) {
            if(argumentList.length >= 1) {
                Log.i(TAG, argumentList[1]);
                if (requestListener != null)
                    requestListener.onResponse(ZowiProtocol.BATTERY_COMMAND, argumentList[1]);
            }else{
                Log.e(TAG, "error battery received incorrect format!");
            }
        }
    }

    /**
     *
     */
    private class ProgramIdReceived implements SerialCommand.SerialCommandListener{
        @Override
        public void onReviced(String[] argumentList) {
            if(argumentList.length >= 1) {
                Log.i(TAG, argumentList[1]);
                if(requestListener != null)
                    requestListener.onResponse(ZowiProtocol.PROGRAMID_COMMAND, argumentList[1]);
            }
        }
    }
}
