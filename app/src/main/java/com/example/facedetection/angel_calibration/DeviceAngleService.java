package com.example.facedetection.angel_calibration;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.IBinder;

import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import com.example.facedetection.R;

import java.util.Timer;
import java.util.TimerTask;

/**
 * Created by Noy davidyan on 28/10/2021.
 */

public class DeviceAngleService extends Service implements SensorEventListener {

    boolean isAngelInRightPosition;
    private int counterTimeInt = 3;
    private float angleFloat;

    private final float[] accelerometerReading = new float[3];
    private final float[] magnetometerReading = new float[3];

    final private int AXIS_X = 0;
    final private int AXIS_Y = 1;
    final private int FIXED_PERCENT = 90;
    final private int MAX_ANGLE_VALUE = 110;
    final private int MIN_ANGLE_VALUE = 70;
    final private int MINUTES_WAITING = 3;

    String messageToDisplayStr;
    public static String ANGLE_SENSORS_ACTION = "com.example.facedetector.angel_calibration.DeviceAngleService.ANGLE_SENSORS_ACTION";

    processStatusEnum processStatus = processStatusEnum.IS_NOT_RIGHT_POSITION;

    private SensorManager sensorManager; //define the sensor variables

    public enum processStatusEnum {
        IS_NOT_RIGHT_POSITION,
        IN_PROGRESS,
        FIXED
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        return super.onStartCommand(intent, flags, startId);
    }

    @Override
    public void onCreate() {

        messageToDisplayStr = getApplicationContext().getResources().getString(R.string.wait_3_seconds);

        //initialize sensor object
        sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);

        Sensor accelerometer = (sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER));
        if (accelerometer != null)
            sensorManager.registerListener(this, accelerometer, SensorManager.SENSOR_DELAY_NORMAL, SensorManager.SENSOR_DELAY_UI);

        Sensor magneticField = sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD);
        if (magneticField != null)
            sensorManager.registerListener(this, magneticField, SensorManager.SENSOR_DELAY_NORMAL, SensorManager.SENSOR_DELAY_UI);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        stopSelf();

        if (sensorManager != null)
            sensorManager.unregisterListener(this);  // Don't receive any more updates from either sensor.
    }

    @Override
    public IBinder onBind(Intent arg0) {
        return null;
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
    }

    @Override
    public void onSensorChanged(SensorEvent sensorEvent) {

        if (sensorEvent.sensor.getType() == Sensor.TYPE_ACCELEROMETER)
            System.arraycopy(sensorEvent.values, 0, accelerometerReading, 0, accelerometerReading.length);
        else if (sensorEvent.sensor.getType() == Sensor.TYPE_MAGNETIC_FIELD)
            System.arraycopy(sensorEvent.values, 0, magnetometerReading, 0, magnetometerReading.length);


        angleFloat = (float) (Math.atan2(accelerometerReading[AXIS_X], accelerometerReading[AXIS_Y]) / (Math.PI / 180) + FIXED_PERCENT);  //Angle calculation

        if (angleFloat >= MIN_ANGLE_VALUE && angleFloat <= MAX_ANGLE_VALUE)
            startTimer();
        else {
            messageToDisplayStr = getApplicationContext().getResources().getString(R.string.place_the_device);
            processStatus = processStatusEnum.IS_NOT_RIGHT_POSITION;
        }

        if (processStatus.equals(processStatusEnum.FIXED))
            isAngelInRightPosition = true;
        else
            isAngelInRightPosition = false;

        //Initializes a bundle intent and publishes it to anyone who listener it
        Intent intent = new Intent(ANGLE_SENSORS_ACTION);

        intent.putExtra("isAngelInRightPosition", isAngelInRightPosition);
        intent.putExtra("messageToDisplayStr", messageToDisplayStr);

        LocalBroadcastManager.getInstance(this).sendBroadcast(intent);
    }

    private void startTimer() {

        //For execution once time when the angle is calibrated
        if (processStatus.equals(processStatusEnum.IS_NOT_RIGHT_POSITION)) {
            Timer timer = new Timer();
            counterTimeInt = MINUTES_WAITING;
            processStatus = processStatusEnum.IN_PROGRESS;

            timer.scheduleAtFixedRate(new TimerTask() {
                @Override
                public void run() {
                    if (processStatus.equals(processStatusEnum.IN_PROGRESS)) {

                        if (counterTimeInt == 0) {
                            messageToDisplayStr = getApplicationContext().getResources().getString(R.string.continue_to_next_step);
                            processStatus = processStatusEnum.FIXED;
                            timer.cancel();

                        } else {
                            messageToDisplayStr = getApplicationContext().getResources().getString(R.string.time_left) + ": " + counterTimeInt;
                            --counterTimeInt;
                        }
                    } else {
                        if (timer != null) {
                            timer.cancel();
                            counterTimeInt = MINUTES_WAITING;
                        }
                    }
                }
            }, 1000, 1000);
        }
    }
}
