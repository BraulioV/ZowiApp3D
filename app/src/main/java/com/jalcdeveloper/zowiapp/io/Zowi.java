package com.jalcdeveloper.zowiapp.io;

import java.io.IOException;

public interface Zowi {

    int FORWARD_DIR = 1;
    int BACKWARD_DIR = -1;
    int LEFT_DIR = 1;
    int RIGHT_DIR = -1;

    int ANGLE_H = 25;

    int LOW_SPEED = 1500;
    int NORMAL_SPEED = 1000;
    int FAST_SPEED = 500;

    void connect(String address) throws IOException;

    void disconnect() throws IOException;

    void home() throws IOException;

    void walk(float steps, int time, int dir) throws IOException;

    void turn(float steps, int time, int dir) throws IOException;

    void updown(float steps, int time, int h) throws IOException;

    void moonwalker(float steps, int time, int h, int dir) throws IOException;

    void swing(float steps, int time, int h) throws IOException;

    void crusaito(float steps, int time, int h, int dir) throws IOException;

    void jump(float steps, int time) throws IOException;

    void setSpeed(int speed);

    void setDirection(int dir);

    void setRequestListener(RequestListener listener);

    void batteryRequest();

    void programIdRequest();

    int getSpeed();

    int getDirection();

    void setMoveListener(MoveListener listener);

    interface MoveListener{
        void onAck();
        void onFinishAck();
    }

    interface RequestListener{
        void onResponse(char command, String data);
    }

}
