package com.example.facedetection.angel_calibration;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import com.example.facedetection.R;
import com.example.facedetection.face_detection.detector.DetectorActivity;

/**
 * Created by Noy davidyan on 28/10/2021.
 */

public class MainActivity extends AppCompatActivity {

    TextView messages_tv;
    Button nextBtn;

    BroadcastReceiver receiver;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        messages_tv = findViewById(R.id.messages_tv);
        nextBtn = findViewById(R.id.next_btn);
    }

    @Override
    protected void onStart() {
        super.onStart();

        startService(new Intent(this, DeviceAngleService.class));

        IntentFilter intentFilter = new IntentFilter(DeviceAngleService.ANGLE_SENSORS_ACTION);

        receiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                initCellsScreenByIntentBundle(intent);
            }
        };

        LocalBroadcastManager.getInstance(this).registerReceiver(receiver, intentFilter);
    }

    @Override
    protected void onStop() {
        super.onStop();

        if (receiver != null)
            LocalBroadcastManager.getInstance(this).unregisterReceiver(receiver);
    }

    private void initCellsScreenByIntentBundle(Intent intent) {

        //Extract details
        boolean isAngelInRightPosition = intent.getBooleanExtra("isAngelInRightPosition", false);
        if (isAngelInRightPosition)
            nextBtn.setEnabled(true);
        else
            nextBtn.setEnabled(false);


        String messageReceivedStr = intent.getStringExtra("messageToDisplayStr");
        if (messageReceivedStr != null)
            messages_tv.setText(messageReceivedStr);
        else
            messages_tv.setText(getString(R.string.place_the_device));
    }

    public void nextOnClick(View view) {
        if (hasFrontCamera())
            startActivity(new Intent(this, DetectorActivity.class));
    }

    private boolean hasFrontCamera() {
        if (getApplicationContext().getPackageManager().hasSystemFeature(PackageManager.FEATURE_CAMERA_FRONT))
            return true;
        else {
            Toast.makeText(this, getString(R.string.device_dosnt_have_camera), Toast.LENGTH_LONG).show();
            return false;
        }
    }

}