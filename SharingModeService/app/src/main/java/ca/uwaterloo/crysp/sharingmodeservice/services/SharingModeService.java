package ca.uwaterloo.crysp.sharingmodeservice.services;

import android.app.ActivityManager;
import android.app.KeyguardManager;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.app.admin.DevicePolicyManager;
import android.app.usage.UsageStats;
import android.app.usage.UsageStatsManager;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.hardware.biometrics.BiometricPrompt;
import android.os.Build;
import android.os.IBinder;
import android.os.RemoteException;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.biometric.BiometricManager;
import androidx.core.app.NotificationCompat;
import androidx.core.content.ContextCompat;

import java.security.Key;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.SortedMap;
import java.util.Timer;
import java.util.TimerTask;
import java.util.TreeMap;
import java.util.concurrent.Executor;

import ca.uwaterloo.crysp.sharingmodeservice.ISharingModeServiceInterface;
import ca.uwaterloo.crysp.sharingmodeservice.KeyguardActivity;
import ca.uwaterloo.crysp.sharingmodeservice.MainActivity;
import ca.uwaterloo.crysp.sharingmodeservice.R;
import ca.uwaterloo.crysp.sharingmodeservice.SecondFragment;

public class SharingModeService extends Service {
    private static final String TAG = "Sharing Mode Service";
    public static final String CHANNEL_ID = "SharingModeChannel";
    public static final String CHANNEL_NAME = "Sharing Mode Channel";
    public static final String SETTING_PACKAGE = "com.android.settings";
    public static final String  LAUNCHER_PACKAGE = "com.google.android.apps.nexuslauncher";
    private static final long MIN_TIME_INTERVAL = 1000000;
    private static final int FOREGROUND_CHECK_INTERVAL = 200;

    private static int NOTIFY_ID = 2022;
    private static int FOREGROUND_ID = 2023;
    private static boolean locking = false;
    private static boolean authenticating = false;

    public static final String ACTION_SET_LOCK_MODE = "SET_LOCK_MODE";
    public static final String ACTION_IMPLICIT_LOCK_MODE = "IMPLICIT_LOCK_MODE";
    public static final String ACTION_CAPTURE_KEYGUARD_ESCAPER = "CAPTURE_ESCAPER";
    public static final String RESULT_AUTHENTICATION_SUCCESS = "AUTHENTICATION_SUCCESS";
    public static final String RESULT_AUTHENTICATION_FAILURE = "AUTHENTICATION_FAILURE";
    public static final String RESULT_IMPLICIT_AUTHENTICATION_SUCCESS = "IA_SUCCESS";
    public static final String RESULT_IMPLICIT_AUTHENTICATION_FAILURE = "IA_FAILURE";

    public static final String ACTION_START_SHARING = "START SHARING";
    public static final String ACTION_CONFIRM_SHARING = "CONFIRM SHARING";
    public static final String ACTION_CANCEL_SHARING = "CANCEL SHARING";
    public static final String ACTION_RETURN_DEVICE = "RETURN DEVICE";
    public static final String ACTION_CONFIRM_RETURN = "CONFIRM RETURN";


    private NotificationManager notificationManager;
    private NotificationCompat.Builder notificationBuilder;
    private BroadcastReceiver mReceiver;
    private PhoneUnlockedReceiver unlockReceiver;
    private boolean unLockReceiverEnabled;
    private DevicePolicyManager dpm;
    private ComponentName dars;
    SharingStatusManager ssm;

    private Timer foregroundChecker;
    private TimerTask foregroundCheckerTask;

    private static String currentLockApp;
    private int currentFilter = 0;

    public SharingModeService() {
    }

    @Override
    public IBinder onBind(Intent intent) {
        // TODO: Return the communication channel to the service.
        Log.i(TAG, "onBind");
        return new ISharingModeServiceInterface.Stub() {
            @Override
            public int getSharingStatus() throws RemoteException {
                return ssm.getSharingStatus();
            }

            @Override
            public int sendIAResult(int result, double score) throws RemoteException {
                Log.d(TAG, "receive IA result: " + result + "," + score);
                ssm.updateIAResult(getBaseContext(), result);
                return 0;
            }
        };
    }

    public void lockApp(String packageName, boolean isExplicit) {
        currentLockApp = packageName;
        locking = true;
        currentFilter = notificationManager.getCurrentInterruptionFilter();
        notificationManager.setInterruptionFilter(NotificationManager.INTERRUPTION_FILTER_NONE);
        notificationManager.cancelAll();
        if (isExplicit) {
            Toast.makeText(this,
                    "Set " + packageName + " as locked",
                    Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        super.onStartCommand(intent, flags, startId);
        if (intent != null && intent.getAction() != null) {
            switch(intent.getAction()) {
                case ACTION_IMPLICIT_LOCK_MODE:
                    if (!locking) {
                        lockApp(getForegroundApp(), false);
                        Log.e("LatencyAnalysis", "Global sharing enabled:" +
                                System.currentTimeMillis());
                        ssm.setSharingStatus(SharingStatusManager.SHARING_STATUS_GESTURE_DETECTED);
                        broadcastSharingStatus(SharingStatusManager.SHARING_STATUS_GESTURE_DETECTED);
                    } else {
                        ssm.setSharingStatus(SharingStatusManager.SHARING_STATUS_RETURN_DETECTED);
                        broadcastSharingStatus(SharingStatusManager.SHARING_STATUS_RETURN_DETECTED);
                    }
                    break;
                case ACTION_SET_LOCK_MODE:
                    lockApp(intent.getStringExtra("app"), true);

                    ssm.setSharingStatus(SharingStatusManager.SHARING_STATUS_SHARING_CONFIRMED);
                    broadcastSharingStatus(SharingStatusManager.SHARING_STATUS_SHARING_CONFIRMED);
                    break;
                case ACTION_CONFIRM_SHARING:
                    ssm.setSharingStatus(SharingStatusManager.SHARING_STATUS_SHARING_CONFIRMED);
                    broadcastSharingStatus(SharingStatusManager.SHARING_STATUS_SHARING_CONFIRMED);
                    break;
                case ACTION_CANCEL_SHARING:
                    authenticating = true;
                    startLockScreen();
                    break;
                case ACTION_RETURN_DEVICE:
                    ssm.setSharingStatus(SharingStatusManager.SHARING_STATUS_RETURN_DETECTED);
                    broadcastSharingStatus(SharingStatusManager.SHARING_STATUS_RETURN_DETECTED);
                    break;
                case ACTION_CONFIRM_RETURN:
                    locking = false;
                    ssm.setSharingStatus(SharingStatusManager.SHARING_STATUS_NO_SHARING);
                    broadcastSharingStatus(SharingStatusManager.SHARING_STATUS_NO_SHARING);
                    break;
                case ACTION_CAPTURE_KEYGUARD_ESCAPER:
                    Log.e(TAG, "CATCH YOU");
                    locking = true;
                    authenticating = false;
                    returnToLockedApp();
                    break;
                case RESULT_AUTHENTICATION_SUCCESS:
                    locking = false;
                    authenticating = false;
                    Toast.makeText(this, "Unlocked", Toast.LENGTH_SHORT)
                            .show();
                    notificationManager.setInterruptionFilter(currentFilter);
                    ssm.setSharingStatus(SharingStatusManager.SHARING_STATUS_NO_SHARING);
                    broadcastSharingStatus(SharingStatusManager.SHARING_STATUS_NO_SHARING);
                    // returnToLockedApp();
                    if(unLockReceiverEnabled) {
                        unregisterReceiver(unlockReceiver);
                        unLockReceiverEnabled = false;
                    }
                    break;
                case RESULT_IMPLICIT_AUTHENTICATION_SUCCESS:
                    locking = false;
                    authenticating = false;
                    Toast.makeText(this, "IA Unlocked", Toast.LENGTH_SHORT)
                            .show();
                    notificationManager.setInterruptionFilter(currentFilter);
                    ssm.setSharingStatus(SharingStatusManager.SHARING_STATUS_NO_SHARING);
                    broadcastSharingStatus(SharingStatusManager.SHARING_STATUS_NO_SHARING);
                    // returnToLockedApp();
                    break;
                case RESULT_AUTHENTICATION_FAILURE:
                    locking = true;
                    authenticating = false;
                    returnToLockedApp();
                    break;
                case RESULT_IMPLICIT_AUTHENTICATION_FAILURE:
                    locking = true;
                    authenticating = false;
                    break;
            }
        }

        return START_STICKY;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        Intent intent = new Intent(this, GestureDetectionService.class);
        startService(intent);
        ssm = new SharingStatusManager();
        // Enable dpm
        dpm = (DevicePolicyManager)getSystemService(Context.DEVICE_POLICY_SERVICE);
        unlockReceiver = new PhoneUnlockedReceiver();

        // Create notification listener
        mReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                String package_name = intent.getStringExtra("Notification Name");
                Log.i(TAG, package_name);
            }
        };
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction("ca.uwaterloo.crysp.sharingmodeservice.services.SharingModeService");
        registerReceiver(mReceiver, intentFilter);

        // Create foreground services
        notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        String channelId = Build.VERSION.SDK_INT >= Build.VERSION_CODES.O ?
                createNotificationChannel(notificationManager) : "";
        notificationBuilder = new NotificationCompat.Builder(this, channelId);
        Notification notification = notificationBuilder.setOngoing(true)
                .setContentText(getForegroundApp())
                .setSmallIcon(R.drawable.ic_stat_share)
                .setCategory(NotificationCompat.CATEGORY_SERVICE)
                .build();

        startForeground(FOREGROUND_ID, notification);

        // Start foreground checker
        foregroundChecker = new Timer();
        foregroundCheckerTask = new TimerTask() {
            @Override
            public void run() {

                String runningApp = getForegroundApp();
                String showText;
                if(locking) {
                    showText = "[Sharing] " + runningApp;
                    notificationBuilder.setContentText(showText).setContentIntent(createCurrentIntent(runningApp))
                            .setColor(getResources().getColor(R.color.colorSharing));
                } else {
                    showText = "[Normal] " + runningApp;
                    notificationBuilder.setContentText(showText).setContentIntent(createCurrentIntent(runningApp))
                            .setColor(getResources().getColor(R.color.colorNormal));
                }

                notificationManager.notify(FOREGROUND_ID, notificationBuilder.build());
                if(authenticating && isEscapingFromKeyguard()) {
                    Log.d(TAG, "Detect escaping from keyguard");
                    authenticating = false;

                }
                if(locking && (!runningApp.equals(currentLockApp)
                       && !(runningApp.equals(SETTING_PACKAGE) && authenticating))) {
                    // showHomeScreen();
                    Log.d(TAG, "Attempt to use other apps");
                    returnToLockedApp();
                }


                /*if(runningApp.equals(SETTING_PACKAGE) && !isDeviceLocked() && authenticating) {
                    Log.d(TAG, "Escape from keyguard");
                    returnToLockedApp();
                }*/
                // Log.d(TAG, "result" + KeyguardActivity.isDeviceLocked());
            }
        };
        foregroundChecker.scheduleAtFixedRate(foregroundCheckerTask,
                0, FOREGROUND_CHECK_INTERVAL);


    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        unregisterReceiver(mReceiver);
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private String createNotificationChannel(NotificationManager notificationManager){
        NotificationChannel channel = new NotificationChannel(CHANNEL_ID,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_HIGH);
        // omitted the LED color
        channel.setImportance(NotificationManager.IMPORTANCE_NONE);
        channel.setLockscreenVisibility(Notification.VISIBILITY_PRIVATE);
        notificationManager.createNotificationChannel(channel);
        return CHANNEL_ID;
    }

    public static class RecentUseComparator implements Comparator<UsageStats> {
        @Override
        public int compare(UsageStats lhs, UsageStats rhs) {
            return (lhs.getLastTimeUsed() > rhs.getLastTimeUsed()) ? -1 : (lhs.getLastTimeUsed() == rhs.getLastTimeUsed()) ? 0 : 1;
        }
    }

    public List<UsageStats> getSortedForegroundList() {
        UsageStatsManager usm = (UsageStatsManager)
                this.getSystemService(Context.USAGE_STATS_SERVICE);
        long time = System.currentTimeMillis();
        List<UsageStats> appList = usm.queryUsageStats(UsageStatsManager.INTERVAL_BEST,
                time - MIN_TIME_INTERVAL, time);
        if (appList != null && appList.size() > 0) {
            RecentUseComparator ruc = new RecentUseComparator();
            Collections.sort(appList, ruc);
        }
        return appList;

    }

    public String getForegroundApp() {
        String currentApp = "NULL";
        if (Build.VERSION.SDK_INT > Build.VERSION_CODES.LOLLIPOP) {
            List<UsageStats> appList = getSortedForegroundList();
            if(appList.get(0).getPackageName().equals(getPackageName())) {
                return appList.get(1).getPackageName();
            } else return appList.get(0).getPackageName();
        } else {
            ActivityManager am = (ActivityManager) this.getSystemService(Context.ACTIVITY_SERVICE);
            List<ActivityManager.RunningAppProcessInfo> tasks = am.getRunningAppProcesses();
            currentApp = tasks.get(0).processName;
        }
        // Log.i("Notification", currentApp);
        return currentApp;
    }

    public boolean isEscapingFromKeyguard() {
        List<UsageStats> appList = getSortedForegroundList();
        if(appList.get(0).getPackageName().equals(SETTING_PACKAGE)
                && appList.get(1).getPackageName().equals(LAUNCHER_PACKAGE)) {
            return true;
        }
        else return false;
    }

    public void returnToLockedApp () {
        Intent i = getPackageManager()
                .getLaunchIntentForPackage(currentLockApp)
                .setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP | Intent.FLAG_ACTIVITY_CLEAR_TOP
                              |  Intent.FLAG_ACTIVITY_NEW_TASK
                ).addCategory(Intent.CATEGORY_LAUNCHER);;
        if (i != null) {
            Log.d(TAG, "Change to " + currentLockApp);
            PendingIntent pendingIntent =
                    PendingIntent.getActivity(this, 0, i, 0);
            try {
                pendingIntent.send();
            } catch (PendingIntent.CanceledException e) {
                e.printStackTrace();
            }
            // startLockScreen();
        } else {
            Log.e(TAG, "Cannot start " + currentLockApp);
        }

    }

    private PendingIntent createCurrentIntent(String packageName) {
        Intent mIntent = new Intent(this, getClass());
        mIntent.putExtra("app", packageName);
        if(locking) {
            // mIntent.setAction(ACTION_CANCEL_SHARING);
            mIntent.setAction(RESULT_AUTHENTICATION_SUCCESS);
        } else {
            mIntent.setAction(ACTION_SET_LOCK_MODE);
            // mIntent.setAction(ACTION_IMPLICIT_LOCK_MODE);
        }
        return PendingIntent.getService(this,
                0,
                mIntent,
                PendingIntent.FLAG_UPDATE_CURRENT);
    }


    private void startLockScreen() {
        registerReceiver(unlockReceiver, new IntentFilter("android.intent.action.USER_PRESENT"));
        unLockReceiverEnabled = true;
        dpm.lockNow();
        // Intent intent = new Intent(this, KeyguardActivity.class);
        // intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        // startActivity(intent);
    }

    public class PhoneUnlockedReceiver extends BroadcastReceiver {

        @Override
        public void onReceive(Context context, Intent intent) {

            KeyguardManager keyguardManager = (KeyguardManager)context.getSystemService(Context.KEYGUARD_SERVICE);
            if (keyguardManager.isKeyguardSecure()) {
                Log.d(TAG, "Device is securely unlocked, exist sharing mode");
                //phone was unlocked, do stuff here
                unlockedPhone();
            }
        }
    }


    private void unlockedPhone() {
        Intent intent = new Intent(this, SharingModeService.class);
        intent.setAction(RESULT_AUTHENTICATION_SUCCESS);
        startService(intent);
    }

    public void broadcastSharingStatus(int status) {
        Intent intent = new Intent();
        intent.setAction(getPackageName());
        intent.putExtra("SharingStatus", status);
        sendBroadcast(intent);
        Log.d(TAG, "Broadcast sharing status:" + getPackageName() + "," + status);
    }

}
