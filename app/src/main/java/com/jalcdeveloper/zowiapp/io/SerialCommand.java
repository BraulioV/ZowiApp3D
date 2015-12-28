package com.jalcdeveloper.zowiapp.io;

import android.content.Context;
import android.util.Log;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.HashMap;
import java.util.Map;

public class SerialCommand extends Thread {

    private static final String TAG = SerialCommand.class.getSimpleName();
    public static String COMMAND_ACTION = SerialCommand.class.getPackage() + ".COMMAND_ACTION";
    public static String TIMEOUT_ACTION = SerialCommand.class.getPackage() + ".TIMEOUT_ACTION";

    private static final byte START_COMMAND_BYTE = '&';
    private static final byte END_COMMAND_BYTE = '%';
    private static final String DELIMITER_COMMAND = " ";

    enum STATE {
        COMMAND,
        WAIT,
        START_COMMAND,
        END_COMMAND,
    }

    private Map<String, SerialCommandListener> listenerMap = new HashMap<>();
    private OutputStream outputStream;
    private InputStream inputStream;
    private Context mContext;
    private STATE state = STATE.WAIT;

    public SerialCommand(Context context,
                         InputStream inputStream,
                         OutputStream outputStream) {

        this.outputStream = outputStream;
        this.inputStream = inputStream;
        mContext = context;
        setName(SerialCommand.class.getName());
        setPriority(Thread.NORM_PRIORITY);

    }

    @Override
    public void run() {

        byte[] buffer = new byte[1024];
        StringBuffer commandS = new StringBuffer();
        int bytes;

        while (!Thread.currentThread().isInterrupted()){

            try {

                bytes = inputStream.read(buffer);

                for(int i = 0; i<bytes; i++){

                    // &&A%% -- Ack command recived
                    // &&F%% -- Finish Ack command recived
                    // &&I Lo que sea%% -- Ejemplo otro comando
                    switch (state){

                        case WAIT:
                            if(buffer[i] == START_COMMAND_BYTE) state = STATE.START_COMMAND;
                            break;

                        case START_COMMAND:
                            if(buffer[i] == START_COMMAND_BYTE) state = STATE.COMMAND;
                            commandS.delete(0, commandS.length());
                            break;

                        case COMMAND:
                            if(buffer[i] == END_COMMAND_BYTE) {
                                state = STATE.END_COMMAND;
                            }else {
                                commandS.append((char)buffer[i]);
                            }
                            break;

                        case END_COMMAND:
                            if(buffer[i] == END_COMMAND_BYTE){
                                state = STATE.WAIT;

                                String[] arguments = commandS.toString().split(DELIMITER_COMMAND);
                                if(arguments.length > 0) {
                                    SerialCommandListener listener = listenerMap.get(arguments[0]);
                                    if (listener != null) listener.onReviced(arguments);
                                }else{
                                    Log.e(TAG, "Command format error: " + commandS.toString());
                                }
                            }
                            break;

                        default:
                            Log.e(TAG, "Incorrect state");
                    }
                }

            }catch (IOException ioEX){
                Log.e(TAG, ioEX.getMessage());
                break;
            }
        }
    }

    public void addListener(String command, SerialCommandListener listener){
        listenerMap.put(command, listener);
    }

    public void removeListener(String command){
    }

    public void clearListeners(){
        listenerMap.clear();
    }

    public void write(byte[] bytes){
        try {

            // TODO: Start timeout monitor
            Log.d(TAG, new String(bytes));
            outputStream.write(bytes);
            outputStream.flush();

        }catch (IOException ioEx){
            Log.e(TAG, ioEx.getMessage());
        }
    }

    public interface SerialCommandListener{

        void onReviced(String[] argumentList);

    }

}
