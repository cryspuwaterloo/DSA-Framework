package ca.uwaterloo.crysp.sharingmodeservice.services;

import android.content.Context;
import android.content.Intent;
import android.util.Log;

import java.util.ArrayList;
import java.util.List;

public class SharingStatusManager {
    private static final String TAG = "StatusManager";


    public static final int SHARING_STATUS_UNAVAILABLE = 0;
    public static final int SHARING_STATUS_NO_SHARING = 1;
    public static final int SHARING_STATUS_GESTURE_DETECTED = 4;
    public static final int SHARING_STATUS_SHARING_CONFIRMED =5;
    public static final int SHARING_STATUS_RETURN_DETECTED = 8;
    public static final int SHARING_STATUS_RETURN_CONFIRMED = 9;

    public static final int IA_STATUS_UNAVAILABLE = -1;
    public static final int IA_STATUS_OWNER = 1;
    public static final int IA_STATUS_NON_OWNER = 0;

    private static final long VALID_TIME_INTERVAL = 60000; // 60 seconds

    private int sharingStatus;
    private int IAStatus;
    private long lastIAUpdateTime = 0;

    private List<Integer> historyResult;

    public SharingStatusManager() {
        sharingStatus = SHARING_STATUS_NO_SHARING;
        IAStatus = IA_STATUS_UNAVAILABLE;
        historyResult = new ArrayList<>();
    }

    public int getSharingStatus() {
        return sharingStatus;
    }

    public void setSharingStatus(int status) {
        historyResult.clear();
        sharingStatus = status;
    }

    private int obtainIAStatus() {
        int sumOfResult = 0;
        for(int i = 0; i < 5; ++i) {
            sumOfResult += historyResult.get(historyResult.size() - i - 1);
        }
        if (sumOfResult >= 4) return IA_STATUS_OWNER;
        else return IA_STATUS_NON_OWNER;
    }

    public void updateIAResult(Context context, int result) {
        long time = System.currentTimeMillis();
        if(time - lastIAUpdateTime < VALID_TIME_INTERVAL) {
            IAStatus = IA_STATUS_UNAVAILABLE;
            historyResult.clear();
        }
        historyResult.add(result);
        if(historyResult.size() < 5) return;

        int aresult = obtainIAStatus();

        if (sharingStatus == SHARING_STATUS_NO_SHARING) {
            if (aresult == IA_STATUS_NON_OWNER) {
                Log.d(TAG, "Probably not the owner");
            }
        } else if (sharingStatus == SHARING_STATUS_GESTURE_DETECTED) {
            if (aresult == IA_STATUS_NON_OWNER ) {
                Log.d(TAG, "Sharing confirmed");
                Intent intent = new Intent(context, SharingModeService.class);
                intent.setAction(SharingModeService.ACTION_CONFIRM_SHARING);
                context.startService(intent);
            } else if (aresult == IA_STATUS_OWNER) {
                Log.e(TAG, "No sharing happens");
            }
        } else if (sharingStatus == SHARING_STATUS_SHARING_CONFIRMED) {
            if (aresult == IA_STATUS_OWNER) {
                Log.d(TAG, "Return confirmed");
                Intent intent = new Intent(context, SharingModeService.class);
                intent.setAction(SharingModeService.RESULT_IMPLICIT_AUTHENTICATION_SUCCESS);
                context.startService(intent);
            }
        } else if (sharingStatus == SHARING_STATUS_RETURN_DETECTED) {
            if (aresult == IA_STATUS_OWNER) {
                Log.d(TAG, "Return confirmed");
                Intent intent = new Intent(context, SharingModeService.class);
                intent.setAction(SharingModeService.RESULT_IMPLICIT_AUTHENTICATION_SUCCESS);
                context.startService(intent);
            } else if (aresult == IA_STATUS_NON_OWNER) {
                Log.d(TAG, "Not returned");
                Intent intent = new Intent(context, SharingModeService.class);
                intent.setAction(SharingModeService.RESULT_IMPLICIT_AUTHENTICATION_FAILURE);
                context.startService(intent);
            }
        } else if (sharingStatus == SHARING_STATUS_RETURN_CONFIRMED) {
            if (aresult == IA_STATUS_OWNER) {
                Log.d(TAG, "Still user using");
                sharingStatus = SHARING_STATUS_NO_SHARING;
            }
        }
    }

}
