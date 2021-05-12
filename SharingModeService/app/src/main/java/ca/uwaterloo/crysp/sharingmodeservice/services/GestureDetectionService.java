package ca.uwaterloo.crysp.sharingmodeservice.services;

import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.res.AssetManager;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.IBinder;
import android.os.SystemClock;
import android.util.Log;
import android.widget.Toast;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Timer;
import java.util.TimerTask;

import ca.uwaterloo.crysp.sharingmodeservice.gesture.DetectionCallback;
import ca.uwaterloo.crysp.sharingmodeservice.gesture.MotionManager;
import ca.uwaterloo.crysp.sharingmodeservice.gesture.SharingGestureClassifier;
import ca.uwaterloo.crysp.sharingmodeservice.gesture.ThresActCallback;

public class GestureDetectionService extends Service {
    private static final String TAG = "GestureDetection";
    private static final int SEC_TO_USEC = 1000000;
    private static final long NANO_TO_MILLI = 1000000;

    private static final int MODE_FIXED_FREQUENCY = 0;
    private static final int MODE_ADAPTIVE_FREQUENCY = 1;

    private static final int DEFAULT_HIGH_FS = 50;
    private static final int DEFAULT_LOW_FS = 10;
    private static final float DEFAULT_HIGH_THRESHOLD = 0.0f;
    private static final float DEFAULT_LOW_THRESHOLD = 0.0f;
    private static final long DEFAULT_GESTURE_DETECTION_INTERVAL = 3000;

    private static final String DEFAULT_MODEL_FILENAME = "handover.tflite";


    private SensorManager sensorManager;
    private Sensor accSensor;
    private Sensor laccSensor;
    private Sensor gyroSensor;

    private MotionManager motionManager;
    private Timer t;

    private long lastDetection = 0;
    private int lastDetectionType = SharingGestureClassifier.NEGATIVE_DETECTION;
    private long detectionInterval = DEFAULT_GESTURE_DETECTION_INTERVAL;

    private int mode=MODE_FIXED_FREQUENCY; // current sensing mode


    private boolean debugOneTimeSignal = true;
    // Utils

    private long getMillisecondTimestamp(long originTimestamp) {
        return System.currentTimeMillis() + ((originTimestamp -
                SystemClock.elapsedRealtimeNanos())/NANO_TO_MILLI);
    }

    private File createFileFromInputStream(InputStream inputStream, File f) {

        try{
            OutputStream outputStream = new FileOutputStream(f);
            byte buffer[] = new byte[1024];
            int length = 0;

            while((length=inputStream.read(buffer)) > 0) {
                outputStream.write(buffer,0,length);
            }

            outputStream.close();
            inputStream.close();

            return f;
        } catch (IOException e) {
            //Logging exception
            e.printStackTrace();
        }

        return null;
    }


    // Sensor event listeners
    public SensorEventListener laccSensorListener = new SensorEventListener() {
        boolean initialized = true;

        @Override
        public void onSensorChanged(SensorEvent event) {
            if (event == null) {
                initialized = true;
                return;
            }

            motionManager.addRawData(MotionManager.SENSOR_LINACC,
                    event.values,
                    getMillisecondTimestamp(event.timestamp));
        }

        @Override
        public void onAccuracyChanged(Sensor sensor, int accuracy) {

        }
    };

    public SensorEventListener accSensorListener = new SensorEventListener() {
        boolean initialized = true;

        @Override
        public void onSensorChanged(SensorEvent event) {
            if (event == null) {
                initialized = true;
                return;
            }

            motionManager.addRawData(MotionManager.SENSOR_ACC, event.values,
                    getMillisecondTimestamp(event.timestamp));
        }

        @Override
        public void onAccuracyChanged(Sensor sensor, int accuracy) {

        }
    };

    public SensorEventListener gyroSensorListener = new SensorEventListener() {
        boolean initialized = true;

        @Override
        public void onSensorChanged(SensorEvent event) {
            if (event == null) {
                initialized = true;
                return;
            }

            motionManager.addRawData(MotionManager.SENSOR_GYRO, event.values,
                    getMillisecondTimestamp(event.timestamp));
        }

        @Override
        public void onAccuracyChanged(Sensor sensor, int accuracy) {

        }
    };


    public GestureDetectionService() {
    }

    @Override
    public IBinder onBind(Intent intent) {
        // TODO: Return the communication channel to the service.
        return null;
    }

    @Override
    public void onCreate() {
        super.onCreate();

        sensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
        accSensor = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        laccSensor = sensorManager.getDefaultSensor(Sensor.TYPE_LINEAR_ACCELERATION);
        gyroSensor = sensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE);
        mode = MODE_ADAPTIVE_FREQUENCY;
        // lastDetection = 0;
        // lastDetectionType = 0;
        // detectionInterval = DEFAULT_GESTURE_DETECTION_INTERVAL;

        // create model file

        File modelFile = new File(getFilesDir(), DEFAULT_MODEL_FILENAME);
        if (!modelFile.exists()) {
            AssetManager am = getAssets();
            try {
                InputStream istream = am.open(DEFAULT_MODEL_FILENAME);
                modelFile = createFileFromInputStream(istream, modelFile);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }


        motionManager = new MotionManager(DEFAULT_LOW_FS, DEFAULT_HIGH_FS,
                DEFAULT_LOW_THRESHOLD, DEFAULT_HIGH_THRESHOLD,
                false, modelFile,
                new ThresActCallback() {
                    @Override
                    public void switchFrequency(int fs) {
                        stopSensing();
                        registerSensors(fs);
                    }
                },
                new DetectionCallback() {
                    @Override
                    public void onDetected(int DetectionResult) {
                        if(DetectionResult == SharingGestureClassifier.HANDOVER_DETECTION) {

                            if(lastDetection != 0) {
                                long t = System.currentTimeMillis();
                                if(t - lastDetection >= detectionInterval) {
                                    // React to detection
                                   // Log.i(TAG, "React to handover event!");
                                    Toast.makeText(getBaseContext(), "Hand over detected!",
                                            Toast.LENGTH_SHORT).show();
                                   Log.e("LatencyAnalysis", "Handover signal generated:" +
                                           System.currentTimeMillis());
                                   startService(createLockIntent());
                                }
                                lastDetection = t;
                                lastDetectionType = DetectionResult;
                            } else {
                                long t = System.currentTimeMillis();
                                // React to detection
                                // Log.i(TAG, "React to handover event!");
                                Toast.makeText(getBaseContext(), "Hand over detected!",
                                        Toast.LENGTH_SHORT).show();
                                Log.e("LatencyAnalysis", "Handover signal generated:" +
                                        System.currentTimeMillis());
                                startService(createLockIntent());
                                lastDetection = t;
                                lastDetectionType = DetectionResult;
                            }
                        }
                    }
                });


        startSensing();
    }

    private void startSensing() {
        registerSensors(motionManager.getFs());
    }

    private void stopSensing() {
        sensorManager.unregisterListener(accSensorListener);
        accSensorListener.onSensorChanged(null);
        sensorManager.unregisterListener(laccSensorListener);
        laccSensorListener.onSensorChanged(null);
        sensorManager.unregisterListener(gyroSensorListener);
        gyroSensorListener.onSensorChanged(null);
        Log.d(TAG, "Close all sensors");
        /*if (wakeLock != null && wakeLock.isHeld()){
            try{
                wakeLock.release();
            } catch (Throwable throwable){
                //ignore if releaseing a wakelock causes a problem, mih be already released
            }
        }*/

    }


    /*
    Register Sensors
     */

    private void registerSensors(int fs) {

        Log.i(TAG, "Sensor sampling rate: " + fs);

        if (accSensor != null) {
            sensorManager.registerListener(accSensorListener,
                    accSensor,
                    SEC_TO_USEC/fs);
        }
        if (laccSensor != null ) {
            sensorManager.registerListener(laccSensorListener,
                    laccSensor,
                    SEC_TO_USEC/fs);
        }
        if (gyroSensor != null ) {
            sensorManager.registerListener(gyroSensorListener,
                    gyroSensor,
                    SEC_TO_USEC/fs);
        }
    }


    private Intent createLockIntent() {
        Intent mIntent = new Intent(this, SharingModeService.class);
        mIntent.setAction(SharingModeService.ACTION_IMPLICIT_LOCK_MODE);
        return mIntent;
    }

    // parameter settings
    public void setMode(int mode) throws Exception {
        if (mode != MODE_FIXED_FREQUENCY && mode != MODE_ADAPTIVE_FREQUENCY) {
            throw new Exception("No such mode");
        } else {
            this.mode = mode;
        }
    }

    public int getMode() {
        return mode;
    }

    public void setDetectionInterval(long t) {
        this.detectionInterval = t;
    }

    public long getDetectionInterval() {
        return detectionInterval;
    }
}
