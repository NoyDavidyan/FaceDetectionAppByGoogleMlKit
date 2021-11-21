package com.example.facedetection.face_detection.detector;

/**
 * Created by Noy davidyan on 31/10/2021.
 */

import android.graphics.Bitmap;
import android.graphics.RectF;

/** An immutable result returned by a Classifier describing what was recognized. */
public class Recognition {
    /**
     * A unique identifier for what has been recognized. Specific to the class, not the instance of
     * the object.
     */
    private final String id;

    /** Display name for the recognition. */
    private final String title;
    /**
     * A sortable score for how good the recognition is relative to others. Lower should be better.
     */
    private final Float distance;
    private Object extra;

    /** Optional location within the source image for the location of the recognized object. */
    private RectF location;
    private Integer color;
    private Bitmap crop;

    public Recognition(){
        id = null;
        title = null;
        location = null;
        distance = null;
    } // Default constractor


    public Recognition(final String id, final String title, final Float distance,
                       final RectF location, final Integer color, final Object extra, final Bitmap crop) {
        this.id = id;
        this.title = title;
        this.distance = distance;
        this.location = location;
        this.color = color;
        this.extra = extra;
        this.crop = crop;
    }

    public void setExtra(Object extra) {
        this.extra = extra;
    }
    public Object getExtra() {
        return this.extra;
    }

    public void setColor(Integer color) {
        this.color = color;
    }

    public String getId() {
        return id;
    }

    public String getTitle() {
        return title;
    }

    public Float getDistance() { return distance; }

    public RectF getLocation() {
        return new RectF(location);
    }

    public void setLocation(RectF location) {
        this.location = location;
    }

    public Integer getColor() {
        return this.color;
    }

    public void setCrop(Bitmap crop) {
        this.crop = crop;
    }

    @Override
    public String toString() {
        String resultString = "";
        if (id != null) resultString += "[" + id + "] ";

        if (title != null) resultString += title + " ";

        if (distance != null) resultString += String.format("(%.1f%%) ", distance * 100.0f);

        if (location != null)  resultString += location + " ";

        return resultString.trim();
    }
}
