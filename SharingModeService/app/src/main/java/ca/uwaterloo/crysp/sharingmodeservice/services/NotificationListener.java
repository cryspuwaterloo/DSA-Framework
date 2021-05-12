package ca.uwaterloo.crysp.sharingmodeservice.services;

import android.content.Intent;
import android.os.IBinder;
import android.service.notification.NotificationListenerService;
import android.service.notification.StatusBarNotification;
import android.util.Log;

public class NotificationListener extends NotificationListenerService {
    private static final String TAG = "NotificationListener";
    private static final String SERVICE_NAME = "ca.uwaterloo.crysp.sharingmodeservice.services.SharingModeService";

    public static final class NotificationCode {
        public static final int SENSITIVE_APP_CODE = 1;
        public static final int INSENSITIVE_APP_CODE = 2;
        public static final int UNKNOWN_APP_CODE = 3;
    }

    @Override
    public IBinder onBind(Intent i) {
        return super.onBind(i);
    }

    @Override
    public void onNotificationPosted(StatusBarNotification sbn) {
        // super.onNotificationPosted(sbn);

        if (!sbn.getPackageName().equals(getPackageName())) {
            Log.i(TAG, sbn.getPackageName());
            if (sbn.getPackageName().equals("com.google.android.gm")) {
                cancelNotification(sbn.getKey());
                Log.d(TAG, "cancel gmail");
            }
            Intent intent = new Intent(SERVICE_NAME);
            intent.putExtra("Notification Name", sbn.getPackageName());
            sendBroadcast(intent);
        }
    }

    @Override
    public void onNotificationRemoved(StatusBarNotification sbn) {
        // super.onNotificationRemoved(sbn);
        Log.i(TAG, "something removed");
    }


}
