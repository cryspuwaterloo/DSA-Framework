package ca.uwaterloo.crysp.libdsaclient.dsa;

import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.os.IBinder;
import android.os.RemoteException;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import android.util.Log;

import ca.uwaterloo.crysp.sharingmodeservice.ISharingModeServiceInterface;

public class DSAClientService extends Service {
    private static final String TAG = "DSAClientService";

    private ISharingModeServiceInterface smsInterface = null;
    private SharingServiceConnection conn = new SharingServiceConnection();

    private int sharingStatus;

    // DSABroadcastReceiver receiver;
    private BroadcastReceiver broadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            int status = intent.getIntExtra(DSAConstant.EXTRA_FIELD_STATUS,
                    DSAConstant.SHARING_STATUS_NO_SHARING);
            Log.d(TAG, "Receive DSA broadcast: " + status);
            sharingStatus = status;
            // Local broadcasting
            broadcastResultLocally(status);
        }
    };

    public DSAClientService() {
    }

    @Override
    public IBinder onBind(Intent intent) {
        // TODO: Return the communication channel to the service.
        throw new UnsupportedOperationException("Not yet implemented");
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        super.onStartCommand(intent, flags, startId);
        if(intent != null && intent.getAction() != null) {
            if(intent.getAction().equals(DSAConstant.ACTION_UPDATE_IA_RESULT)) {
                // SEND IA RESULT TO
                int result = intent.getIntExtra(DSAConstant.EXTRA_FIELD_IA_RESULT, -1);
                double score = intent.getDoubleExtra(DSAConstant.EXTRA_FIELD_IA_SCORE, -1);
                try {
                    smsInterface.sendIAResult(result, score);
                } catch (RemoteException e) {
                    e.printStackTrace();
                }
            }
            if(intent.getAction().equals(DSAConstant.ACTION_INITIALIZE_SHARING_STATUS) && smsInterface != null) {
                try {
                    int result = smsInterface.getSharingStatus();
                    broadcastResultLocally(result);
                } catch (RemoteException e) {
                    Log.e(TAG, "Not connected");
                }

            }
        }
        return START_NOT_STICKY;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        configureReceiver();
        Log.d(TAG, "Configured");
        sharingStatus = DSAConstant.SHARING_STATUS_NO_SHARING;
        final Intent sharingIntent = new Intent();
        sharingIntent.setComponent(new ComponentName("ca.uwaterloo.crysp.sharingmodeservice",
                "ca.uwaterloo.crysp.sharingmodeservice.services.SharingModeService"));
        bindService(sharingIntent, conn, BIND_AUTO_CREATE);

    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        unbindService(conn);
        smsInterface = null;
        unregisterReceiver(broadcastReceiver);
    }

    private void configureReceiver() {
        IntentFilter filter = new IntentFilter();
        filter.addAction(DSAConstant.REMOTE_SERVICE_NAME);
        registerReceiver(broadcastReceiver, filter);
    }


    private void broadcastResultLocally(int status) {
        Intent bIntent = new Intent();
        bIntent.setAction(DSAConstant.ACTION_ACQUIRE_SHARING_STATUS);
        bIntent.putExtra(DSAConstant.EXTRA_FIELD_STATUS,
                status);
        LocalBroadcastManager.getInstance(getApplicationContext()).sendBroadcast(bIntent);
        Log.d(TAG, "Local broadcast");
    }

    private class SharingServiceConnection implements ServiceConnection {

        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            smsInterface = ISharingModeServiceInterface.Stub.asInterface(service);
            Log.d(TAG, "Service connected");
            try {
                int result = smsInterface.getSharingStatus();
                broadcastResultLocally(result);

            } catch (RemoteException e) {
                e.printStackTrace();
            }
            Log.d(TAG, "Broadcast current sharing status");

        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            Log.d(TAG, "Service disconnected");
        }
    }
}
