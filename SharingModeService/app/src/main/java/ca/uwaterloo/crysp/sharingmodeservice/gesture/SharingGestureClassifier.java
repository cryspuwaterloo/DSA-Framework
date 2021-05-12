package ca.uwaterloo.crysp.sharingmodeservice.gesture;

import android.util.Log;

import org.apache.commons.math3.stat.StatUtils;
import org.tensorflow.lite.Interpreter;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class SharingGestureClassifier {
    private static final String TAG = "SGClassifier";
    public static final int NEGATIVE_DETECTION = 0;
    public static final int HANDOVER_DETECTION = 1;


    private List<Integer> decisionBuffer;
    private int decisionWindow;
    private int positiveWindow;
    private Interpreter model;

    public SharingGestureClassifier(File modelFile, int totalWindow, int positiveWindow) {
        this.decisionWindow = totalWindow;
        this.positiveWindow = positiveWindow;
        this.decisionBuffer = new ArrayList<>();
        model = new Interpreter(modelFile);
    }

    private String showFloatArray(float[] value) {
        String result = "";
        for(float v: value) {
            result += v + ", ";
        }
        return result;
    }

    public void reset() {
        decisionBuffer.clear();
    }


    public int infer(HashMap<String, WindowedData> windowData) {
        double[] features = extractFeatures(windowData);
        float[][] result = new float[][]{new float[2]};
        // Log.i(TAG, "Feature extracted: " + features.length);
        try{
            float[] f_features = new float[features.length];
            for(int i = 0; i < features.length; ++i) f_features[i] = (float) features[i];
            // Log.i(TAG, "Features: " + showFloatArray(f_features));
            model.run(new float[][]{f_features}, result);
        } catch (Exception e) {
            e.printStackTrace();
        }
        Log.i(TAG, "Result: " + result[0][0] + "vs" + result[0][1]);
        if (result[0][1] >= 0.80) decisionBuffer.add(HANDOVER_DETECTION);
        else decisionBuffer.add(NEGATIVE_DETECTION);

        if (decisionBuffer.size() >= decisionWindow) {
            int count = 0;
            for(int i = decisionBuffer.size() - decisionWindow; i < decisionBuffer.size(); ++i) {
                if (decisionBuffer.get(i) == HANDOVER_DETECTION) {
                    count += 1;
                }
            }
            if (count >= positiveWindow && decisionBuffer.get(decisionBuffer.size() - 1) == HANDOVER_DETECTION)
                return HANDOVER_DETECTION;
        }
        return NEGATIVE_DETECTION;
    }

    /*
    feature input function:
    "acc": [x], [y], [z], [g]


     */

    public double[] extractFeatures(HashMap<String, WindowedData> windowData) {
        WindowedData lacc = windowData.get(MotionManager.SENSOR_LINACC);
        WindowedData gyro = windowData.get(MotionManager.SENSOR_GYRO);
        // Note this is hardcoded function extractor （87 features）
        return new double[] {
                StatUtils.mean(lacc.x), StatUtils.mean(lacc.y),
                StatUtils.mean(lacc.z), StatUtils.mean(lacc.g),
                MathHelper.median(lacc.x), MathHelper.median(lacc.y),
                MathHelper.median(lacc.z), MathHelper.median(lacc.g),
                StatUtils.max(lacc.x), StatUtils.max(lacc.y),
                StatUtils.max(lacc.z), StatUtils.max(lacc.g),
                StatUtils.percentile(lacc.x, 25), StatUtils.percentile(lacc.y, 25),
                StatUtils.percentile(lacc.z, 25), StatUtils.percentile(lacc.g, 25),
                StatUtils.percentile(lacc.x, 75), StatUtils.percentile(lacc.y, 75),
                StatUtils.percentile(lacc.z, 75), StatUtils.percentile(lacc.g, 75),
                MathHelper.std(lacc.x), MathHelper.std(lacc.y),
                MathHelper.std(lacc.z), MathHelper.std(lacc.g),
                MathHelper.mad(lacc.x), MathHelper.mad(lacc.y),
                MathHelper.mad(lacc.z), MathHelper.mad(lacc.g),
                MathHelper.range(lacc.x), MathHelper.range(lacc.y),
                MathHelper.range(lacc.z), MathHelper.range(lacc.g),
                StatUtils.sum(lacc.x) / 50, StatUtils.sum(lacc.y) / 50,
                StatUtils.sum(lacc.z) / 50, StatUtils.sum(lacc.g) / 50,
                MathHelper.scusum(lacc.x, 0.0004), MathHelper.scusum(lacc.y, 0.0004),
                MathHelper.scusum(lacc.z, 0.0004), MathHelper.scusum(lacc.g, 0.0004),
                MathHelper.rms(lacc.x), MathHelper.rms(lacc.y),
                MathHelper.rms(lacc.z), MathHelper.rms(lacc.g),
                MathHelper.entropy(lacc.x), MathHelper.entropy(lacc.y),
                MathHelper.entropy(lacc.z), MathHelper.entropy(lacc.g),
                MathHelper.cov(lacc.x, lacc.y), MathHelper.cov(lacc.x, lacc.z),
                MathHelper.cov(lacc.y, lacc.z),
                StatUtils.mean(gyro.x), StatUtils.mean(gyro.y), StatUtils.mean(gyro.z),
                MathHelper.median(gyro.x), MathHelper.median(gyro.y), MathHelper.median(gyro.z),
                StatUtils.max(gyro.x), StatUtils.max(gyro.y), StatUtils.max(gyro.z),
                StatUtils.percentile(gyro.x, 25), StatUtils.percentile(gyro.y, 25),
                StatUtils.percentile(gyro.z, 25),
                StatUtils.percentile(gyro.x, 75), StatUtils.percentile(gyro.y, 75),
                StatUtils.percentile(gyro.z, 75),
                MathHelper.std(gyro.x), MathHelper.std(gyro.y), MathHelper.std(gyro.z),
                MathHelper.mad(gyro.x), MathHelper.mad(gyro.y), MathHelper.mad(gyro.z),
                MathHelper.range(gyro.x), MathHelper.range(gyro.y), MathHelper.range(gyro.z),
                StatUtils.sum(gyro.x) / 50, StatUtils.sum(gyro.y) / 50, StatUtils.sum(gyro.z) / 50,
                MathHelper.rms(gyro.x), MathHelper.rms(gyro.y), MathHelper.rms(gyro.z),
                MathHelper.entropy(gyro.x), MathHelper.entropy(gyro.y), MathHelper.entropy(gyro.z),
                MathHelper.cov(gyro.x, gyro.y), MathHelper.cov(gyro.x, gyro.z),
                MathHelper.cov(gyro.y, gyro.z),
        };

    }

}
