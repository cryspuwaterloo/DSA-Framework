package ca.uwaterloo.crysp.sharingmodeservice.gesture;

import android.app.PendingIntent;

public interface DetectionCallback {
    void onDetected(int DetectionResult);
}
