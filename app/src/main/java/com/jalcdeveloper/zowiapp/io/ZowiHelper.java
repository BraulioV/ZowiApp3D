package com.jalcdeveloper.zowiapp.io;

import android.os.Handler;
import android.os.Message;
import android.util.Log;

public class ZowiHelper implements Zowi.MoveListener {

    private static final String TAG = ZowiHelper.class.getSimpleName();
    private static ZowiHandler handler;
    private static Thread thread;
    private static Zowi zowi;

    public static final int NONE_ACTION = 0;
    public static final int WALK_ACTION = 1;
    public static final int TURN_ACTION = 2;
    public static final int STOP_ACTION = 3;
    public static final int JUMP_ACTION = 4;
    public static final int MOONWALKER_ACTION = 5;
    public static final int SWING_ACTION = 6;
    public static final int CRUSAITO_ACTION = 7;

    /**
     * Signals manage Thread
     */
    static AckSignal ackSignal = new AckSignal();
    static MessageSignal messageSignal = new MessageSignal();

    public ZowiHelper(Zowi zowiParam) {
        super();
        handler = new ZowiHandler();
        zowi = zowiParam;
        zowi.setMoveListener(this);
    }

    @Override
    public void onAck() {
        Log.d(TAG, "Ack");
    }

    @Override
    public void onFinishAck() {
        Log.d(TAG, "ACK finish");
        ackSignal.doNotify();

    }

    public void walk(Zowi zowi, int speed, int dir){

        zowi.setSpeed(speed);
        zowi.setDirection(dir);
        Message message = handler.obtainMessage(WALK_ACTION, zowi);
        handler.sendMessage(message);

    }

    public void turn(Zowi zowi, int speed, int dir){

        zowi.setSpeed(speed);
        zowi.setDirection(dir);
        Message message = handler.obtainMessage(TURN_ACTION, zowi);
        handler.sendMessage(message);

    }

    public void stop(Zowi zowi){

        Message message = handler.obtainMessage(STOP_ACTION, zowi);
        handler.sendMessage(message);

    }

    public void jump(Zowi zowi, int speed){

        zowi.setSpeed(speed);
        Message message = handler.obtainMessage(JUMP_ACTION, zowi);
        handler.sendMessage(message);
    }

    public void moonWalker(Zowi zowi, int speed, int dir){

        zowi.setSpeed(speed);
        zowi.setDirection(dir);
        Message message = handler.obtainMessage(MOONWALKER_ACTION, zowi);
        handler.sendMessage(message);

    }

    public void swing(Zowi zowi, int speed){

        zowi.setSpeed(speed);
        Message message = handler.obtainMessage(SWING_ACTION, zowi);
        handler.sendMessage(message);

    }

    public void crusaito(Zowi zowi, int speed, int dir){

        zowi.setSpeed(speed);
        zowi.setDirection(dir);
        Message message = handler.obtainMessage(CRUSAITO_ACTION, zowi);
        handler.sendMessage(message);
    }

    private static class ZowiHandler extends Handler implements Runnable{

        Zowi zowi;
        int action = NONE_ACTION;

        public ZowiHandler() {
            thread = new Thread(this);
            thread.setName("ZowiHandler");
            thread.setPriority(Thread.NORM_PRIORITY);
            thread.start();
        }

        @Override
        public void run() {

            while (!Thread.currentThread().isInterrupted()){
                    try {

                        if (action == NONE_ACTION)
                                messageSignal.doWait();

                            switch (action) {
                                case WALK_ACTION:
                                    zowi.walk(1, zowi.getSpeed(), zowi.getDirection());
                                    break;
                                case STOP_ACTION:
                                    zowi.home();
                                    action = NONE_ACTION;
                                    break;
                                case TURN_ACTION:
                                    zowi.turn(1, zowi.getSpeed(), zowi.getDirection());
                                    break;
                                case JUMP_ACTION:
                                    zowi.jump(1, zowi.getSpeed());
                                    break;
                                case MOONWALKER_ACTION:
                                    zowi.moonwalker(1, zowi.getSpeed(), Zowi.ANGLE_H, zowi.getDirection());
                                    break;
                                case SWING_ACTION:
                                    zowi.swing(1, zowi.getSpeed(), Zowi.ANGLE_H);
                                    break;
                                case CRUSAITO_ACTION:
                                    zowi.crusaito(1,zowi.getSpeed(),Zowi.ANGLE_H, zowi.getDirection());
                                    break;
                            }

                            ackSignal.doWait(2000);

                    }catch (Exception ex){
                        Log.e(TAG, ex.getMessage());
                        Thread.currentThread().interrupt();
                        action = NONE_ACTION;
                    }
            }
        }

        @Override
        public void handleMessage(Message msg) {
            zowi = (Zowi) msg.obj;
            action = msg.what;
            messageSignal.doNotify();
        }
    }

}
