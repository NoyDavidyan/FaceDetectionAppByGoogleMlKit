package com.example.facedetection.face_detection.detector;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Bitmap;
import android.graphics.Bitmap.Config;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.RectF;
import android.os.Bundle;
import android.util.Size;
import android.view.View;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import com.example.facedetection.R;
import com.example.facedetection.angel_calibration.DeviceAngleService;
import com.example.facedetection.angel_calibration.MainActivity;
import com.example.facedetection.face_detection.camera.CameraActivity;
import com.example.facedetection.face_detection.camera.ImageUtils;
import com.example.facedetection.face_detection.detector.tracking.MultiBoxTracker;
import com.example.facedetection.face_detection.detector.tracking.OverlayView;
import com.example.facedetection.face_detection.detector.tracking.OverlayView.DrawCallback;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.mlkit.vision.common.InputImage;
import com.google.mlkit.vision.face.Face;
import com.google.mlkit.vision.face.FaceDetection;
import com.google.mlkit.vision.face.FaceDetector;
import com.google.mlkit.vision.face.FaceDetectorOptions;

import java.util.List;

/**
 * An activity that uses a TensorFlowMultiBoxDetector and ObjectTracker to detect and then track
 * objects.
 */
public class DetectorActivity extends CameraActivity {

    private static final boolean MAINTAIN_ASPECT = false;

    private static final Size DESIRED_PREVIEW_SIZE = new Size(640, 480);
    OverlayView trackingOverlay;
    Button nextBtn;
    TextView messageTv;
    public static final Integer SENSOR_ORIENTATION = 270;

    private boolean computingDetection = false;

    private Matrix frameToCropTransform;
    private Matrix cropToFrameTransform;

    private MultiBoxTracker tracker;
    // Face detector
    private FaceDetector faceDetector;

    private Bitmap rgbFrameBitmap = null;
    private Bitmap croppedBitmap = null;
    private Bitmap cropCopyBitmap = null;

    BroadcastReceiver faceDetectorReceiver;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Real-time contour detection of multiple faces
        FaceDetectorOptions options = new FaceDetectorOptions.Builder()
                .setPerformanceMode(FaceDetectorOptions.PERFORMANCE_MODE_FAST)
                .setContourMode(FaceDetectorOptions.LANDMARK_MODE_NONE)
                .setClassificationMode(FaceDetectorOptions.CLASSIFICATION_MODE_NONE)
                .build();


        FaceDetector detector = FaceDetection.getClient(options);
        faceDetector = detector;
    }

    @Override
    public synchronized void onStart() {
        super.onStart();

        IntentFilter intentFilter = new IntentFilter(DeviceAngleService.ANGLE_SENSORS_ACTION);

        faceDetectorReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {

                if (intent.getBooleanExtra("isAngelInRightPosition", false) == false) {

                    Toast.makeText(DetectorActivity.this, getString(R.string.shift_desired_angle), Toast.LENGTH_LONG).show();

                    if (faceDetectorReceiver != null)
                        LocalBroadcastManager.getInstance(DetectorActivity.this).unregisterReceiver(faceDetectorReceiver);

                    startActivity(new Intent(DetectorActivity.this, MainActivity.class));
                }
            }
        };
        LocalBroadcastManager.getInstance(this).registerReceiver(faceDetectorReceiver, intentFilter);
    }

    @Override
    public synchronized void onStop() {
        super.onStop();

        if (faceDetectorReceiver != null)
            LocalBroadcastManager.getInstance(this).unregisterReceiver(faceDetectorReceiver);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
    }

    @Override
    public void onPreviewSizeChosen(final Size size, final int rotation) {

        nextBtn = findViewById(R.id.next_btn);
        messageTv = findViewById(R.id.messageTv);
        trackingOverlay = findViewById(R.id.tracking_overlay);

        tracker = new MultiBoxTracker(this);
        previewWidth = size.getWidth();
        previewHeight = size.getHeight();

        rgbFrameBitmap = Bitmap.createBitmap(previewWidth, previewHeight, Config.ARGB_8888);

        int cropH = (int) (previewHeight / 2.0);
        int cropW = (int) (previewWidth / 2.0);

        croppedBitmap = Bitmap.createBitmap(cropW, cropH, Config.ARGB_8888);

        frameToCropTransform = ImageUtils.getTransformationMatrix(previewWidth, previewHeight, cropW, cropH,
                rotation, MAINTAIN_ASPECT);

        cropToFrameTransform = new Matrix();

        frameToCropTransform.invert(cropToFrameTransform);

        tracker.setFrameConfiguration(previewWidth, previewHeight, SENSOR_ORIENTATION);

        nextBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Toast.makeText(DetectorActivity.this, getApplicationContext().getResources().getString(R.string.identification_succeeded), Toast.LENGTH_SHORT).show();
            }
        });

        //Displays images all the time by a camera
        trackingOverlay.addCallback(
                new DrawCallback() {
                    @Override
                    public void drawCallback(final Canvas canvas) {
                        tracker.draw(canvas);
                    }
                });
    }


    @Override
    protected void processImage() {

        trackingOverlay.postInvalidate();

        // No mutex needed as this method is not reentrant.
        if (computingDetection) {
            readyForNextImage();

        } else {
            computingDetection = true;

            rgbFrameBitmap.setPixels(getRgbBytes(), 0, previewWidth, 0, 0, previewWidth, previewHeight);

            readyForNextImage();

            final Canvas canvas = new Canvas(croppedBitmap);
            canvas.drawBitmap(rgbFrameBitmap, frameToCropTransform, null);

            InputImage image = InputImage.fromBitmap(croppedBitmap, 0);
            faceDetector.process(image).addOnSuccessListener(new OnSuccessListener<List<Face>>() {
                @Override
                public void onSuccess(List<Face> faces) {
                    if (faces.size() == 0) {
                        updateResults(new Recognition());
                        displayDetectedSuccess(false);
                        return;
                    }

                    runInBackground(new Runnable() {
                        @Override
                        public void run() {
                            if (faces != null && faces.size() > 0) {
                                Face faceDetected = faces.get(0);

                                if (faceDetected != null) {
                                    onFacesDetected(faceDetected);
                                    displayDetectedSuccess(true);
                                }
                            }
                        }
                    });
                }
            });
        }
    }

    @Override
    protected int getLayoutId() {
        return R.layout.camera_connection_fragment_tracking;
    }

    @Override
    protected Size getDesiredPreviewFrameSize() {
        return DESIRED_PREVIEW_SIZE;
    }

    @Override
    public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
    }

    private void updateResults(Recognition mappedRecognitions) {

        tracker.trackResults(mappedRecognitions);
        trackingOverlay.postInvalidate();
        computingDetection = false;
    }

    private void onFacesDetected(Face faceDetection) {

        cropCopyBitmap = Bitmap.createBitmap(croppedBitmap);

        final RectF boundingBox = new RectF(faceDetection.getBoundingBox());
        if (boundingBox != null) {
            cropToFrameTransform.mapRect(boundingBox);// maps crop coordinates to original image founded

            Matrix flip = new Matrix(); //Identifies the location of the face and draws around them
            flip.postScale(1, -1, previewWidth / 2.0f, previewHeight / 2.0f); // by the sensorOrientation = 270
            flip.mapRect(boundingBox);

            Float distance = -1f;
            RectF location = boundingBox;
            final Recognition result = new Recognition("0", "", distance, location, Color.BLUE, null, null);

            updateResults(result);
        }
    }

    private void displayDetectedSuccess(boolean isFaceDetected) {

        DetectorActivity.this.runOnUiThread(
                new Runnable() {
                    @Override
                    public void run() {
                        if (isFaceDetected)
                            messageTv.setText(getApplicationContext().getResources().getString(R.string.identification_succeeded));
                        else
                            messageTv.setText(getApplicationContext().getResources().getString(R.string.looking_for_face));

                        nextBtn.setEnabled(isFaceDetected);
                    }
                });
    }
}
