package ca.uwaterloo.crysp.sharingmodeservice.gesture;

import android.app.PendingIntent;
import android.util.Log;

import java.io.File;
import java.util.HashMap;

class WindowedData {
    double[] x;
    double[] y;
    double[] z;
    double[] g;

    public WindowedData(double[] x, double[] y, double[] z, double[] g) {
        this.x = x;
        this.y = y;
        this.z = z;
        this.g = g;
    }
}


public class MotionManager {
    /*
     * Constants
     */
    private static final int MAX_BUFFER_SIZE = 2000;  // Only keep the recent 2000 samples
    public static final String SENSOR_ACC = "acc";
    public static final String SENSOR_GYRO = "gyro";
    public static final String SENSOR_LINACC = "linacc";
    public static final String SENSOR_MAGN = "magn";
    public static final String[] SUPPORTED_SENSORS = {
            SENSOR_ACC, SENSOR_GYRO, SENSOR_LINACC, SENSOR_MAGN
    };
    public static final String[] DETECTION_SENSORS = {
            SENSOR_GYRO, SENSOR_LINACC
    };

    private static final String THRESHOLD_SENSOR = SENSOR_ACC;

    private static final String TAG = "MotionManager";
    private static final int MIN_ACTIVATION_SAMPLES = 400;
    private static final int MIN_DEACTIVATION_SAMPLES = 20;
    private static final int WINDOW_SIZE = 100;
    private static final float GRAVITY_ACCELERATION = 9.81f;


    /*
     * Core variables
     */

    private HashMap<String, MotionData> data;
    private ReferenceTimeLine rtl;
    private int fs;
    private float lowThreshold;
    private int lowFS;
    private float highThreshold;
    private int highFS;
    private boolean fixedMode;
    private SharingGestureClassifier sgclf;

    /*
     * Controller variables
     */
    private boolean activated=false;
    private int activationCounter;
    private int detectionIndex;
    private static final int INTERVAL = 50;

    private ThresActCallback thresActCallback;
    private DetectionCallback detectionCallback;

    public boolean isActivated() {
        return activated;
    }

    public int getFs() {
        return fs;
    }


    public MotionManager(int lowSamplingRate, int highSamplingRate,
                         float lowTh, float highTh, boolean fixed, File model,
                         ThresActCallback callback, DetectionCallback dCallback) {
        data = new HashMap<>();
        for (String sensor: SUPPORTED_SENSORS) {
            boolean need_g = false;
            if (sensor.equals(SENSOR_ACC) || sensor.equals(SENSOR_LINACC)) {
                need_g = true;
            }
            data.put(sensor, new MotionData(MAX_BUFFER_SIZE, sensor, need_g));
        }
        sgclf = new SharingGestureClassifier(model, 2, 2);
        fs = lowSamplingRate;
        highFS = highSamplingRate;
        lowFS = lowSamplingRate;
        rtl = new ReferenceTimeLine(fs);
        thresActCallback = callback;
        detectionCallback = dCallback;
        lowThreshold = lowTh;
        highThreshold = highTh;
        fixedMode = fixed;
        activationCounter = 0;
        detectionIndex = 0;
    }


    public void reset(int samplingRate) {
        fs = samplingRate;
        for (String sensor: SUPPORTED_SENSORS) {
            boolean need_g = false;
            if (sensor.equals(SENSOR_ACC) || sensor.equals(SENSOR_LINACC)) {
                need_g = true;
            }
            data.put(sensor, new MotionData(MAX_BUFFER_SIZE, sensor, need_g));
        }
        rtl.reset(samplingRate);
        activationCounter = 0;
        detectionIndex = 0;
    }


    public void setLowThreshold(float threshold) {
        lowThreshold = threshold;
    }

    public void setHighThreshold(float threshold) {
        highThreshold = threshold;
    }


    public void addRawData(String sensor, float[] item, long timeStamp) {
        if(activated) {
            MotionData tmpValue = data.get(sensor);
            int timeIndex;
            if (!rtl.isInitialized()) {
                rtl.set(fs, timeStamp);
                timeIndex = 0;
            } else {
                timeIndex = rtl.timeToIndex(timeStamp);
            }
            tmpValue.add(item, timeStamp, timeIndex);
            data.put(sensor, tmpValue);
            if(data.get(SENSOR_LINACC).isAvailable(detectionIndex)
                    && data.get(SENSOR_GYRO).isAvailable(detectionIndex)) {
                // Log.d(TAG, "Enter detection");

                int st;


                if(detectionIndex < MAX_BUFFER_SIZE) {
                    st = detectionIndex - WINDOW_SIZE;
                } else {
                    st = MAX_BUFFER_SIZE - WINDOW_SIZE - 1;
                }
                long t0 = System.currentTimeMillis();
                int res = sgclf.infer(getAllWindowedData(st, WINDOW_SIZE));
                Log.e(TAG, "Time consumption" + (System.currentTimeMillis() - t0));
                if(res == 1) Log.i(TAG, "Detected a handover " + timeStamp);
                else Log.i(TAG, "Nothing detected " + timeStamp);
                detectionCallback.onDetected(res);

                detectionIndex += INTERVAL;
            }
        } else {
            MotionData tmpValue = data.get(sensor);
            tmpValue.addItem(item, timeStamp, 0);
            data.put(sensor, tmpValue);
        }

        if(!fixedMode && sensor.equals(THRESHOLD_SENSOR)) {
            simpleThresholdActivator(data.get(sensor));
        }


    }


    public void simpleThresholdActivator(MotionData selBuffer) {
        if (activated) {
            if (activationCounter >= MIN_ACTIVATION_SAMPLES) {
                float sumAcceleration = 0.0f;
                for(int i = selBuffer.size() - MIN_DEACTIVATION_SAMPLES; i < selBuffer.size(); ++i) {
                    sumAcceleration += MathHelper.absFLoat(selBuffer.get(i).g - GRAVITY_ACCELERATION);
                }
                if (sumAcceleration/MIN_DEACTIVATION_SAMPLES < lowThreshold) {
                    Log.d(TAG, String.format("Deactivate high frequency mode (value: %f, length: %d)",
                            sumAcceleration/MIN_DEACTIVATION_SAMPLES, activationCounter));
                    activated = false;
                    reset(lowFS);
                    sgclf.reset();
                    thresActCallback.switchFrequency(lowFS);

                }
            }
            activationCounter += 1;

        } else {
            float g = selBuffer.get(-1).g;

            if (MathHelper.absFLoat(g - GRAVITY_ACCELERATION) > highThreshold) {
                activationCounter = 0;
                Log.d(TAG, String.format("Activate high frequency mode (value: %f)", g));
                activated = true;
                reset(highFS);
                thresActCallback.switchFrequency(highFS);
                detectionIndex = WINDOW_SIZE;
            }
        }

    }


    public WindowedData getWindowedSensorData(String sensor, int st, int windowSize) {
        double[] x = new double[windowSize];
        double[] y = new double[windowSize];
        double[] z = new double[windowSize];
        double[] g = new double[windowSize];
        MotionData tmp = data.get(sensor);
        for(int i = 0; i < windowSize; ++i) {
            MotionRecord tmpRecord = tmp.get(st + i);
            x[i] = tmpRecord.getX();
            y[i] = tmpRecord.getY();
            z[i] = tmpRecord.getZ();
            g[i] = tmpRecord.getG();
        }
        return new WindowedData(x, y, z, g);
    }


    public HashMap<String, WindowedData> getAllWindowedData(int st, int windowSize) {
        HashMap<String, WindowedData> result = new HashMap<>();
        for(String sensor: DETECTION_SENSORS) {
            result.put(sensor, getWindowedSensorData(sensor, st, windowSize));
        }
        return result;
    }
}
