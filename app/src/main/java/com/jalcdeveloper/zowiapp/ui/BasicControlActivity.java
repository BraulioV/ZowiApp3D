package com.jalcdeveloper.zowiapp.ui;

import android.content.Intent;
//import android.graphics.Matrix;
import android.opengl.Matrix;
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
    private float[] prev_orientacion = new float[3];
    private double[] diff = new double[3];
    // Vector para almacenar la matriz de rotación
    private float[] matriz_de_rotacion = new float[16];
    private float[] matriz_de_aceleracion = new float[4];
    // timestamp del último movimiento detectado
    private float timestamp;
    private float EPSILON = 2f;
    private int last_move = -1;

    private Zowi zowi;
    private ZowiHelper zowiHelper;
    private Timer batteryTimer;

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
        this.timestamp = 0;

        zowi.setRequestListener(requestListener);
        zowi.programIdRequest();

        speak.setOnClickListener(new View.OnClickListener() {
            public void onClick(View view) {
                Intent speak_ac = new Intent(getApplicationContext(), MainVoiceActivity.class);
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
    }

    @Override
    protected void onPause() {
        batteryTimer.cancel();
        super.onPause();

    }

    // devuelve el máximo elemento entre los tres primeros elementos un array
    private int max(double[] list){
        int ind = -1;
        double max = -9999999;
        for (int i = 0; i < 3; i++) {
            if(list[i] > max) {
                max = Math.abs(list[i]);
                ind = i;
            }
        }
        return ind;
    }

    // despresiona y para a zowi cuando se detecta un movimiento brusco
    private void stopZowi() {
        switch (last_move) {
            case 0:
                this.buttonWalkForward.setPressed(false);
                break;
            case 1:
                this.buttonWalkBackward.setPressed(false);
                break;
            case 2:
                this.buttonTurnLeft.setPressed(false);
                break;
            case 3:
                this.buttonTurnRight.setPressed(false);
                break;
        }
        //zowiHelper.stop(zowi);
    }

    // método para escuchar cambios en los valores de los sensores
    @Override
    public void onSensorChanged(SensorEvent event) {
        // detectamos de qué tipo es el sensor detectado
        if (event.sensor.getType() == Sensor.TYPE_ACCELEROMETER) {

            matriz_de_aceleracion[0] = event.values[0];
            matriz_de_aceleracion[1] = event.values[1];
            matriz_de_aceleracion[2] = event.values[2];
            matriz_de_aceleracion[3] = 0;
        }

        if (event.sensor.getType() == Sensor.TYPE_ROTATION_VECTOR) {
            // detectamos si se ha producido un giro
//            if (this.timestamp != 0) {
                // Obtenemos la matriz de rotación
                SensorManager.getRotationMatrixFromVector(matriz_de_rotacion, event.values);
                float[] sensor_matrix = new float[4];
                float[] result = new float[16];
                Matrix.invertM(result,0,matriz_de_rotacion,0);
                sensor_matrix[0] = 0;
                sensor_matrix[1] = 0;
                sensor_matrix[2] = 0;
                sensor_matrix[3] = 0;
                Matrix.multiplyMV(sensor_matrix,0,result,0,matriz_de_aceleracion,0);

                // Obtenemos la orientación
                SensorManager.getOrientation(result, orientacion);

                Log.d("sensorMatrix[0]", Float.toString(sensor_matrix[0]));
                Log.d("sensorMatrix[1]", Float.toString(sensor_matrix[1]));
                Log.d("sensorMatrix[2]", Float.toString(sensor_matrix[2]));

                /*diff[0] = prev_orientacion[0] - orientacion[0];
                diff[1] = prev_orientacion[1] - orientacion[1];
                diff[2] = prev_orientacion[2] - orientacion[2];

                // Si hacemos el gesto de avanzar hacia delante, el eje que más cambia
                // es el de la Z (2)
                // Si hacemos el gesto de moverse hacia los lados, el eje que más cambia
                // es el de la Y (1)

                int ind = max(diff);
                Log.d(TAG, "Valor de i " + diff[ind]);
                if (Math.abs(diff[ind]) > 0.08) {
                    this.stopZowi();
                    Log.d(TAG, "Ult acc = " + last_move);
                    double angle = Math.toDegrees(orientacion[1]);
                    switch (ind) {
                        case 0:
                            if (angle >= 0) {
                                this.buttonWalkForward.setPressed(true);
                                Log.d(TAG, "entro en el positivo de palante");
                                //                        zowiHelper.walk(zowi, Zowi.NORMAL_SPEED, Zowi.FORWARD_DIR);
                                this.last_move = 0;
                            }
                            break;
                        case 1:
                            Log.d(TAG, "gira hacia el lado");
                            Log.d(TAG, "Valor de angle = " + angle);
                            if (angle >= 0) {
                                this.buttonTurnLeft.setPressed(true);
                                Log.d(TAG, "entro en el positivo de izq");
                                //                            zowiHelper.turn(zowi, Zowi.NORMAL_SPEED, Zowi.LEFT_DIR);
                                this.last_move = 2;
                            } else {
                                this.buttonTurnRight.setPressed(true);
                                Log.d(TAG, "entro en el neg de izq");
                                //                            zowiHelper.turn(zowi, Zowi.NORMAL_SPEED, Zowi.RIGHT_DIR);
                                this.last_move = 3;
                            }

                            // positivo --> izq
                            // negativo --> dcha
                            break;
                        case 2:
                            Log.d(TAG, "\n\n\n\nva recto");
                            double ang = Math.toDegrees(orientacion[2]);
                            Log.d(TAG, "Valor de angZ = " + ang);
                            this.buttonWalkBackward.setPressed(true);
                            Log.d(TAG, "entro en el positivo de palante");
                            //                        zowiHelper.walk(zowi, Zowi.NORMAL_SPEED, Zowi.BACKWARD_DIR);
                            this.last_move = 1;


                            // positivo --> palante
                            // negativo --> patras
                            break;
                    }*/
                }
                /*else{
                    SensorManager.getRotationMatrixFromVector(matriz_de_rotacion, event.values);
                    SensorManager.getOrientation(matriz_de_rotacion, prev_orientacion);
                }*/
//            }

            prev_orientacion = orientacion.clone();
        this.timestamp = event.timestamp;

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
