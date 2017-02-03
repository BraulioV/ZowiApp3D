package com.jalcdeveloper.zowiapp.ui;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.TextView;

import com.jalcdeveloper.zowiapp.R;
import com.jalcdeveloper.zowiapp.ZowiApp;
import com.jalcdeveloper.zowiapp.io.Zowi;
import com.jalcdeveloper.zowiapp.io.ZowiHelper;
import com.jalcdeveloper.zowiapp.io.ZowiProtocol;

import java.util.Timer;
import java.util.TimerTask;

// Sensores de movimiento
import android.hardware.SensorManager;
import android.hardware.Sensor;
import android.content.Context;
// clases para poder captar cambios en los sensores
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
// valor absoluto
import java.lang.Math;

public class BasicControlActivity extends ImmersiveActivity implements SensorEventListener {

    private static final String TAG = BasicControlActivity.class.getSimpleName();

    private ImageButton buttonWalkForward;
    private ImageButton buttonWalkBackward;
    private ImageButton buttonTurnLeft;
    private ImageButton buttonTurnRight;
    private ImageButton buttonJump;
    private ImageButton buttonMoonwalkerRight;
    private ImageButton buttonMoonwalkerLeft;
    private ImageButton buttonSwing;
    private ImageButton buttonCrusaitoRight;
    private ImageButton buttonCrusaitoLeft;
    private TextView textBattery;
    private Button speak;

    // sensores de movimiento
    private SensorManager mSensorManager;
    private Sensor mSensor;
    private Sensor aSensor;
    // vector para guardar los valores devueltos por el sensor de rotación y los previos

    private float[] orientacion = new float[3];

    // Vector para almacenar la matriz de rotación
    private float[] matriz_de_rotacion = new float[16];
    // timestamp del último movimiento detectado
    private int last_move = -1;
    private int last_last_move = -1;

    private Zowi zowi;
    private ZowiHelper zowiHelper;
    private Timer batteryTimer;

    // Controlamos si estamos en la actividad de voz o no
    // para activar o desactivar la detección del movimiento
    boolean voice;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_basic_control);

        zowi = ((ZowiApp) getApplication()).zowi;
        zowiHelper = new ZowiHelper(zowi);

        buttonWalkForward = (ImageButton) findViewById(R.id.button_walk_forward);
        buttonWalkBackward = (ImageButton) findViewById(R.id.button_walk_backward);
        buttonTurnLeft = (ImageButton) findViewById(R.id.button_turn_left);
        buttonTurnRight = (ImageButton) findViewById(R.id.button_turn_right);
        buttonJump = (ImageButton) findViewById(R.id.button_jump);
        buttonMoonwalkerLeft = (ImageButton) findViewById(R.id.button_moonwalker_left);
        buttonMoonwalkerRight = (ImageButton) findViewById(R.id.button_moonwalker_right);
        buttonSwing = (ImageButton) findViewById(R.id.button_swing);
        buttonCrusaitoLeft = (ImageButton) findViewById(R.id.button_crusaito_left);
        buttonCrusaitoRight = (ImageButton) findViewById(R.id.button_crusaito_right);
        textBattery = (TextView) findViewById(R.id.text_battery);
        speak = (Button) findViewById(R.id.speech_btn1);

        buttonWalkForward.setOnTouchListener(walkForwardOnTouchListener);
        buttonWalkBackward.setOnTouchListener(walkBackwardOnTouchListener);
        buttonTurnLeft.setOnTouchListener(turnLeftOnTouchListener);
        buttonTurnRight.setOnTouchListener(turnRightOnTouchListener);
        buttonJump.setOnTouchListener(jumpOnTouchListener);
        buttonMoonwalkerLeft.setOnTouchListener(moonwalkerLeftOnTouchListener);
        buttonMoonwalkerRight.setOnTouchListener(moonwalkerRightOnTouchListener);
        buttonSwing.setOnTouchListener(swingOnTouchListener);
        buttonCrusaitoLeft.setOnTouchListener(crusaitoLeftOnTouchListener);
        buttonCrusaitoRight.setOnTouchListener(crusaitoRightOnTouchListener);

        // sensores de movimiento
        mSensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        mSensor = mSensorManager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR);
        aSensor = mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);

        zowi.setRequestListener(requestListener);
        zowi.programIdRequest();

        voice = false;

        speak.setOnClickListener(new View.OnClickListener() {
            public void onClick(View view) {
                Intent speak_ac = new Intent(getApplicationContext(), MainVoiceActivity.class);
                voice = true;
                startActivity(speak_ac);
            }
        });
    }

    @Override
    protected void onResume() {
        /** Timmer Refresh battery Zowi level **/
        batteryTimer = new Timer();
        batteryTimer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                zowi.batteryRequest();
            }
        }, 0, 5000);
        super.onResume();
        mSensorManager.registerListener(this, mSensor, SensorManager.SENSOR_DELAY_NORMAL);
        mSensorManager.registerListener(this, aSensor, SensorManager.SENSOR_DELAY_NORMAL);
        Log.d(TAG,"ON RESUME");
        voice = false;
    }

    @Override
    protected void onPause() {
        batteryTimer.cancel();
        super.onPause();

    }

    // despresiona y para a zowi cuando se detecta un movimiento brusco
    private void stopZowi() {
        this.buttonWalkForward.setPressed(false);
        this.buttonWalkBackward.setPressed(false);
        this.buttonTurnLeft.setPressed(false);
        this.buttonTurnRight.setPressed(false);
    }

    private void performAction(){
        switch (last_move) {
            case 0:
                this.buttonWalkForward.setPressed(true);
                zowiHelper.walk(zowi, Zowi.NORMAL_SPEED, Zowi.FORWARD_DIR);
                break;
            case 1:
                this.buttonWalkBackward.setPressed(true);
                zowiHelper.walk(zowi, Zowi.NORMAL_SPEED, Zowi.BACKWARD_DIR);
                break;
            case 2:
                this.buttonTurnLeft.setPressed(true);
                zowiHelper.turn(zowi, Zowi.NORMAL_SPEED, Zowi.LEFT_DIR);
                break;
            case 3:
                this.buttonTurnRight.setPressed(true);
                zowiHelper.turn(zowi, Zowi.NORMAL_SPEED, Zowi.RIGHT_DIR);
                break;
            case 4:
                break;
            default:
                last_move=4;
                if(last_last_move != -1 && last_last_move != 4){
                    stopZowi();
                    zowiHelper.stop(zowi);
                }
        }
        if(last_last_move != last_move)
            stopZowi();
    }

    // método para escuchar cambios en los valores de los sensores
    @Override
    public void onSensorChanged(SensorEvent event) {

        // Comprobamosque el sensor es el correcto
        if (event.sensor.getType() == Sensor.TYPE_ROTATION_VECTOR && !voice) {
            // Obtenemos la matriz de rotación, y reestablecemos las coordenadas
            // del sensor
            SensorManager.getRotationMatrixFromVector(matriz_de_rotacion,
                    event.values);
            SensorManager
                    .remapCoordinateSystem(matriz_de_rotacion,
                            SensorManager.AXIS_Y, SensorManager.AXIS_X,
                            matriz_de_rotacion);
            // obtenemos la orientación del vector
            SensorManager.getOrientation(matriz_de_rotacion, orientacion);

            // Pasamos de radianes a grados y los ponemos de 0 a 360, para evitar
            // problemas de signo con los grados
            orientacion[0] = (float) (Math.toDegrees(orientacion[0]) + 360) % 360;
            orientacion[1] = (float) (Math.toDegrees(orientacion[1]) + 360) % 360;
            orientacion[2] = (float) (Math.toDegrees(orientacion[2]) + 360) % 360;

            // caminar hacia delante o hacia detrás
            if ((orientacion[2] >= 100 && orientacion[2] <= 180) && (orientacion[1] >= 5 && orientacion[1] <= 60)) {
                //camina hacia delante
                this.last_last_move = last_move;
                this.last_move = 0;
            } else if ((orientacion[2] >= 100 && orientacion[2] <= 180) && (orientacion[1] >= 300 && orientacion[1] <= 350)) {
                //camina hacia atrás
                this.last_last_move = last_move;
                this.last_move = 1;
            } else if ((orientacion[2] >= 185 && orientacion[2] <= 300) &&
                    ((orientacion[1] > 355 && orientacion[1] < 360) || (orientacion[1] > 0 && orientacion[1] < 5))) {
                // rota a la derecha
                this.last_last_move = last_move;
                this.last_move = 3;
            } else if ((orientacion[2] >= 100 && orientacion[2] <= 170) &&
                        ((orientacion[1] > 355 && orientacion[1] < 360) || (orientacion[1] > 0 && orientacion[1] < 5))) {
                    // rota a la izquierda
                    this.last_last_move = last_move;
                    this.last_move = 2;
            } else {
                this.last_last_move = last_move;
                last_move = -1;
            } // detenerse
            performAction();
        }
    }


    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {}

    /**
     *
     **/
    private View.OnTouchListener walkForwardOnTouchListener = new View.OnTouchListener(){
        @Override
        public boolean onTouch(View view, MotionEvent motionEvent) {

            switch (motionEvent.getAction()) {
                case MotionEvent.ACTION_DOWN:
                    zowiHelper.walk(zowi, Zowi.NORMAL_SPEED, Zowi.FORWARD_DIR);
                    break;
                case MotionEvent.ACTION_UP:
                    zowiHelper.stop(zowi);
                    break;
            }
            return false;
        }
    };

    /**
     *
     **/
    private View.OnTouchListener walkBackwardOnTouchListener = new View.OnTouchListener(){
        @Override
        public boolean onTouch(View view, MotionEvent motionEvent) {
            switch (motionEvent.getAction()){

                case MotionEvent.ACTION_DOWN:
                    zowiHelper.walk(zowi, Zowi.NORMAL_SPEED, Zowi.BACKWARD_DIR);
                    break;
                case MotionEvent.ACTION_UP:
                    zowiHelper.stop(zowi);
            }
            return false;
        }
    };

    /**
     *
     **/
    private View.OnTouchListener turnLeftOnTouchListener = new View.OnTouchListener(){
        @Override
        public boolean onTouch(View view, MotionEvent motionEvent) {
            switch (motionEvent.getAction()) {
                case MotionEvent.ACTION_DOWN:
                    zowiHelper.turn(zowi, Zowi.NORMAL_SPEED, Zowi.LEFT_DIR);
                    break;
                case MotionEvent.ACTION_UP:
                    zowiHelper.stop(zowi);
                    break;
            }
            return false;
        }
    };

    /**
     *
     **/
    private View.OnTouchListener turnRightOnTouchListener = new View.OnTouchListener(){
        @Override
        public boolean onTouch(View view, MotionEvent motionEvent) {
            switch (motionEvent.getAction()) {
                case MotionEvent.ACTION_DOWN:
                    zowiHelper.turn(zowi, Zowi.NORMAL_SPEED, Zowi.RIGHT_DIR);
                    break;
                case MotionEvent.ACTION_UP:
                    zowiHelper.stop(zowi);
                    break;
            }
            return false;
        }
    };

    /**
     *
     **/
    private View.OnTouchListener jumpOnTouchListener = new View.OnTouchListener(){
        @Override
        public boolean onTouch(View view, MotionEvent motionEvent) {
            switch (motionEvent.getAction()){
                case MotionEvent.ACTION_DOWN:
                    zowiHelper.jump(zowi, Zowi.FAST_SPEED);
                    break;
                case MotionEvent.ACTION_UP:
                    zowiHelper.stop(zowi);
                    break;
            }
            return false;
        }
    };

    /**
     *
     **/
    private View.OnTouchListener moonwalkerLeftOnTouchListener = new View.OnTouchListener(){
        @Override
        public boolean onTouch(View view, MotionEvent motionEvent) {
            switch (motionEvent.getAction()){
                case MotionEvent.ACTION_DOWN:
                    zowiHelper.moonWalker(zowi, Zowi.NORMAL_SPEED, Zowi.LEFT_DIR);
                    break;
                case MotionEvent.ACTION_UP:
                    zowiHelper.stop(zowi);
                    break;
            }
            return false;
        }
    };

    /**
     *
     **/
    private View.OnTouchListener moonwalkerRightOnTouchListener = new View.OnTouchListener(){
        @Override
        public boolean onTouch(View view, MotionEvent motionEvent) {
            switch (motionEvent.getAction()){
                case MotionEvent.ACTION_DOWN:
                    zowiHelper.moonWalker(zowi, Zowi.NORMAL_SPEED, Zowi.RIGHT_DIR);
                    break;
                case MotionEvent.ACTION_UP:
                    zowiHelper.stop(zowi);
                    break;
            }
            return false;
        }
    };

    /**
     *
     **/
    private View.OnTouchListener swingOnTouchListener = new View.OnTouchListener(){
        @Override
        public boolean onTouch(View view, MotionEvent motionEvent) {
            switch (motionEvent.getAction()){
                case MotionEvent.ACTION_DOWN:
                    zowiHelper.swing(zowi, Zowi.NORMAL_SPEED);
                    break;
                case MotionEvent.ACTION_UP:
                    zowiHelper.stop(zowi);
                    break;
            }
            return false;
        }
    };

    /**
     *
     **/
    private View.OnTouchListener crusaitoLeftOnTouchListener = new View.OnTouchListener(){
        @Override
        public boolean onTouch(View view, MotionEvent motionEvent) {
            switch (motionEvent.getAction()){
                case MotionEvent.ACTION_DOWN:
                    zowiHelper.crusaito(zowi, Zowi.NORMAL_SPEED, Zowi.LEFT_DIR);
                    break;
                case MotionEvent.ACTION_UP:
                    zowiHelper.stop(zowi);
                    break;
            }
            return false;
        }
    };

    /**
     *
     **/
    private View.OnTouchListener crusaitoRightOnTouchListener = new View.OnTouchListener(){
        @Override
        public boolean onTouch(View view, MotionEvent motionEvent) {
            switch (motionEvent.getAction()){
                case MotionEvent.ACTION_DOWN:
                    zowiHelper.crusaito(zowi, Zowi.NORMAL_SPEED, Zowi.RIGHT_DIR);
                    break;
                case MotionEvent.ACTION_UP:
                    zowiHelper.stop(zowi);
                    break;
            }
            return false;
        }
    };

    /**
     * Listener of request Zowi
     */
    Zowi.RequestListener requestListener = new Zowi.RequestListener() {
        @Override
        public void onResponse(char command, final String data) {
            Log.d(TAG, "Commannd Response: " + command + " - " + data);

            switch (command){
                case ZowiProtocol.BATTERY_COMMAND:

                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            textBattery.setText("Battery: " + data);
                        }
                    });
                    break;

            }
        }
    };
}
