package com.example.facedetection.face_detection.detector.tracking;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Paint.Cap;
import android.graphics.Paint.Join;
import android.graphics.Paint.Style;
import android.graphics.RectF;
import android.util.Pair;

import com.example.facedetection.face_detection.camera.ImageUtils;
import com.example.facedetection.face_detection.detector.Recognition;

import java.util.LinkedList;
import java.util.List;

/**
 * A tracker that handles non-max suppression and matches existing objects to new detections.
 */
public class MultiBoxTracker {
    private static final float TEXT_SIZE_DIP = 18;
    private static final float MIN_SIZE = 16.0f;
    private final List<TrackedRecognition> trackedObjects = new LinkedList<TrackedRecognition>();
    private final Paint boxPaint = new Paint();
    private Matrix frameToCanvasMatrix;
    private int frameWidth;
    private int frameHeight;
    private int sensorOrientation;

    private static class TrackedRecognition {
        RectF location;
        float detectionConfidence;
        int color;
        String title;
    }

    public MultiBoxTracker(final Context context) {
        //Set the box frame
        boxPaint.setStyle(Style.STROKE);
        boxPaint.setStrokeWidth(10.0f);
        boxPaint.setStrokeCap(Cap.ROUND);
        boxPaint.setStrokeJoin(Join.ROUND);
        boxPaint.setStrokeMiter(100);
    }

    public synchronized void setFrameConfiguration(final int width, final int height, final int sensorOrientation) {
        frameWidth = width;
        frameHeight = height;
        this.sensorOrientation = sensorOrientation;
    }

    public synchronized void trackResults(Recognition results) {
        processResults(results);
    }

    private Matrix getFrameToCanvasMatrix() {
        return frameToCanvasMatrix;
    }

    //Draw a square frame
    public synchronized void draw(final Canvas canvas) {
        final boolean rotated = sensorOrientation % 180 == 90;
        final float multiplier = Math.min(
                        canvas.getHeight() / (float) (rotated ? frameWidth : frameHeight),
                        canvas.getWidth() / (float) (rotated ? frameHeight : frameWidth));

        frameToCanvasMatrix = ImageUtils.getTransformationMatrix(
                        frameWidth,
                        frameHeight,
                        (int) (multiplier * (rotated ? frameHeight : frameWidth)),
                        (int) (multiplier * (rotated ? frameWidth : frameHeight)),
                        sensorOrientation,
                        false);

        for (final TrackedRecognition recognition : trackedObjects) {
            final RectF trackedPos = new RectF(recognition.location);

            getFrameToCanvasMatrix().mapRect(trackedPos);
            boxPaint.setColor(recognition.color);

            float cornerSize = Math.min(trackedPos.width(), trackedPos.height()) / 8.0f;
            canvas.drawRoundRect(trackedPos, cornerSize, cornerSize, boxPaint);
        }
    }

    private void processResults(Recognition result) {

        trackedObjects.clear(); //cleans a square face that has been identified face

        if (result.getLocation() == null) {
            return;
        }

        final Matrix rgbFrameToScreen = new Matrix(getFrameToCanvasMatrix());
        final RectF detectionFrameRect = new RectF(result.getLocation());
        final RectF detectionScreenRect = new RectF();
        rgbFrameToScreen.mapRect(detectionScreenRect, detectionFrameRect);

        if (detectionFrameRect.width() < MIN_SIZE || detectionFrameRect.height() < MIN_SIZE)
            return;

        final List<Pair<Float, Recognition>> rectsToTrack = new LinkedList<Pair<Float, Recognition>>();
        rectsToTrack.add(new Pair<Float, Recognition>(result.getDistance(), result));

        //add a square frame to the list to the draw
        for (final Pair<Float, Recognition> potential : rectsToTrack) {

            final TrackedRecognition trackedRecognition = new TrackedRecognition();
            trackedRecognition.detectionConfidence = potential.first;
            trackedRecognition.location = new RectF(potential.second.getLocation());
            trackedRecognition.title = potential.second.getTitle();

            if (potential.second.getColor() != null)
                trackedRecognition.color = potential.second.getColor();

            trackedObjects.add(trackedRecognition);
        }
    }
}
