package ca.uwaterloo.crysp.libdsaclient;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.os.Bundle;

import androidx.databinding.BindingAdapter;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import androidx.appcompat.app.AppCompatActivity;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Toast;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.List;

import ca.uwaterloo.crysp.libdsaclient.ia.TouchFeatures;
import ca.uwaterloo.crysp.libdsaclient.ia.TrainingSet;
import ca.uwaterloo.crysp.libdsaclient.dsa.DSAClientService;
import ca.uwaterloo.crysp.libdsaclient.dsa.DSAConstant;

public class SecureActivity extends AppCompatActivity {
    private static final String BASE_TAG = "Itus";
    public static int state = 0;
    final int TRAINING = 0;
    final int TESTING = 1;
    long threshold = 30;
    public final int minTrain = 100;
    boolean verbose = true;

    TouchFeatures tf;
    TrainingSet ts;

    int fvSize;
    public static boolean goodToBlock = true;
    public static boolean sharing = false;
    private boolean topActivity = false;


    // utility related variable
    // auto hidden view list
    private List<View> autoHiddenViews;
    private List<View> autoDisableViews;


    private BroadcastReceiver broadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            Log.d(BASE_TAG, "Localbroadcast received");
            if(!topActivity) {
                Log.d(BASE_TAG, "Not for me");
                return;
            }
            if (intent != null && intent.getAction() != null) {
                if (intent.getAction().equals(DSAConstant.ACTION_ACQUIRE_SHARING_STATUS)) {

                    int status = intent.getIntExtra(DSAConstant.EXTRA_FIELD_STATUS,
                            DSAConstant.SHARING_STATUS_UNAVAILABLE);
                    Log.d(BASE_TAG, "received status: " + status);
                    boolean tmpSharing = false;
                    switch (status) {
                        case DSAConstant.SHARING_STATUS_NO_SHARING:
                            goodToBlock = true;
                            tmpSharing = false;
                            break;
                        case DSAConstant.SHARING_STATUS_GESTURE_DETECTED:
                            Log.d(BASE_TAG, "Hand gesture detected");
                            goodToBlock = false;
                            tmpSharing = true;
                            break;
                        case DSAConstant.SHARING_STATUS_SHARING_CONFIRMED:
                            Toast.makeText(getApplicationContext(),
                                    "Sharing confirmed",
                                    Toast.LENGTH_SHORT).show();
                            goodToBlock = false;
                            tmpSharing = true;
                            break;
                        case DSAConstant.SHARING_STATUS_RETURN_DETECTED:
                            Log.d(BASE_TAG, "Return gesture detected");
                            tmpSharing = true;
                            break;
                        case DSAConstant.SHARING_STATUS_RETURN_CONFIRMED:
                            Toast.makeText(getApplicationContext(),
                                    "Return confirmed",
                                    Toast.LENGTH_SHORT).show();
                            goodToBlock = true;
                            tmpSharing = false;
                            break;
                    }
                    if (tmpSharing != sharing) {
                        onSharingStatusChanged(tmpSharing);
                    }
                }
            }
        }
    };

    public void onSharingStatusChanged(boolean status) {
        sharing = status;
        Log.d(BASE_TAG, "Sharing status changed: " + status);
        if(status) {
            disableViews();
            hideViews();
        } else {
            recoverViews();
        }
    }


    public void hideViews() {
        for(View view: autoHiddenViews) {
            view.setVisibility(View.GONE);
        }
    }

    public void disableViews() {
        for(View view: autoDisableViews) {
            view.setEnabled(false);
        }
    }

    public void recoverViews() {
        for(View view: autoHiddenViews) {
            view.setVisibility(View.GONE);
        }

        for(View view: autoDisableViews) {
            view.setEnabled(true);
        }
    }


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // initialize utility variables
        autoDisableViews = new ArrayList<>();
        autoHiddenViews = new ArrayList<>();


        // DSA start service
        Intent intent = new Intent(this, DSAClientService.class);
        startService(intent);

        // DSA local broadcast
        LocalBroadcastManager.getInstance(this).registerReceiver(broadcastReceiver,
                new IntentFilter(DSAConstant.ACTION_ACQUIRE_SHARING_STATUS));
    }

    private void updateMode(int mode) {
        state = mode;
        if (mode == TRAINING) {
            if (verbose)
                Log.d(BASE_TAG, "Trainingset size: " + fvSize + "\n" +
                        "Min trainingset size: " + minTrain);

        }
        else {
            Log.d(BASE_TAG, "TESTING");
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        topActivity = true;
        SharedPreferences settings = getPreferences(MODE_PRIVATE);
        // fvSize = settings.getInt("fvSize", 0);
        verbose = settings.getBoolean("verbose", true);
        threshold = settings.getLong("threshold", 30);
        File file = this.getFileStreamPath("trainingSet");


        if (file != null && file.exists()) {
            FileInputStream fis = null;
            ObjectInputStream in = null;
            try {
                fis = this.openFileInput("trainingSet");
                in = new ObjectInputStream(fis);
                ts = (TrainingSet) in.readObject();
                in.close();
                Log.d(BASE_TAG, "Load training set");
            } catch (Exception ex) {
                Log.e(BASE_TAG, ex.getMessage());
            }
            fvSize = ts.fv.size();
    }
        else {
            ts = new TrainingSet();
            fvSize = 0;
        }
        if(tf == null)
            tf = new TouchFeatures();
        if(fvSize >= minTrain)
            state = TESTING;
        else
            state = TRAINING;

        updateMode(state);

        Intent initIntent = new Intent(this, DSAClientService.class);
        initIntent.setAction(DSAConstant.ACTION_INITIALIZE_SHARING_STATUS);
        startService(initIntent);
    }

    @Override
    public boolean dispatchTouchEvent(MotionEvent ev) {
        boolean rv = tf.procEvent(ev);
        double dist;
        if (rv) {
            if (state == TRAINING) {
                ts.fv.add(tf.fv.getAll());
                fvSize++;
                if (verbose)
                    Log.d(BASE_TAG, "Trainingset size: " + fvSize + "\n" +
                            "Min trainingset size: " + minTrain);
                updatePreferences();
                if (fvSize == minTrain) {
                    state = TESTING;
                    this.updateMode(TESTING);
                    getScaledFeatures();
                }
            }
            else {
                dist =  getDistance(tf.fv.getAll());
                if (verbose)
                    Log.d(BASE_TAG, "Threshold: "+ String.valueOf(threshold)+ "\n" +
                            "Raw score: " + String.valueOf(dist));
                int result = 0;
                if (dist < threshold) {
                    Log.d(BASE_TAG, "Success");
                    result = 1;
                }
                else {
                    if (goodToBlock)
                        Log.d(BASE_TAG,"Failure: block!");
                    else
                        Log.d(BASE_TAG,"Failure: sharing now");
                    result = 0;
                }
                // send IA result first to DSAClientService

                Intent intent = new Intent(this, DSAClientService.class);
                intent.setAction(DSAConstant.ACTION_UPDATE_IA_RESULT);
                intent.putExtra(DSAConstant.EXTRA_FIELD_IA_RESULT,
                        result);
                intent.putExtra(DSAConstant.EXTRA_FIELD_IA_SCORE,
                        dist);
                startService(intent);

            }

        }
        //Log.d("MainActivity", String.valueOf(rv));
        //tv.setText(String.valueOf(rv));



        return super.dispatchTouchEvent(ev);
    }


    private double getDistance(double [] f) {
        double avgDist = 0;
        double minDist = Double.MAX_VALUE;
        double dist = 0;

        for (int i = 0; i < fvSize; i++ ) {
            double [] g = ts.fv.get(i);
            dist = 0;
            for (int j = 0; j < f.length; j++)
                dist += Math.abs(f[j]/ts.fScale[j] - g[j]/ts.fScale[j]);
            dist /= f.length;
            if (dist < minDist)
                minDist = dist;
            avgDist += dist;
        }
        avgDist /= fvSize;
        return Math.floor(minDist);
    }
    private void getScaledFeatures() {
        for (int i = 0; i < ts.fScale.length; i++ )
            ts.fScale[i] = 0;
        for (int i = 0; i < fvSize; i++ )
            for (int j = 0; j < ts.fScale.length; j++)
                ts.fScale[j] += ts.fv.get(i)[j];
        for (int i = 0; i < ts.fScale.length; i++ ) {
            ts.fScale[i]/=fvSize;
            //if (fScale[i] == 0)
            ts.fScale[i] = 1;
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        topActivity = false;
    }

    @Override
    protected void onStop() {
        super.onStop();

        updatePreferences();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        LocalBroadcastManager.getInstance(this).unregisterReceiver(broadcastReceiver);
    }

    public void updatePreferences() {
        SharedPreferences settings = getPreferences(MODE_PRIVATE);
        SharedPreferences.Editor editor = settings.edit();
        editor.putLong("threshold", threshold);
        editor.putBoolean("verbose", verbose);
        if (fvSize > 0) {
            FileOutputStream fos = null;
            ObjectOutputStream out = null;
            try {
                fos = this.openFileOutput("trainingSet", Context.MODE_PRIVATE);
                out = new ObjectOutputStream(fos);
                out.writeObject(ts);
                out.close();
                fos.close();
                Log.d(BASE_TAG, "write training set: " + ts.fv.size());
            } catch (Exception ex) {
                Log.e("FOS", ex.getMessage());
            }
        }
        // Commit the edits!
        editor.commit();
        Log.d(BASE_TAG, "Update preferences");
    }

    public boolean getSharingState(){
        return sharing;
    }


    // auto hiding during sharing
    public void addToSharingHiddenViews(View view) {
        autoHiddenViews.add(view);
    }

    public void addToSharingHiddenViews(List<View> views) {
        autoHiddenViews.addAll(views);
    }

    public void addToSharingDisableViews(View view) {
        autoDisableViews.add(view);
    }

    public void addToSharingDisableViews(List<View> views) {
        autoDisableViews.addAll(views);
    }


}

