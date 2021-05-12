package ca.uwaterloo.crysp.sharingmodeservice.gesture;

import android.util.Log;

import java.util.ArrayList;
import java.util.List;


class MotionRecord {
    public float x;
    public float y;
    public float z;
    public float g;
    public long timestamp;
    public int timeIndex;

    public MotionRecord(float[] data, long timestamp, int timeIndex) {
        this.x = data[0];
        this.y = data[1];
        this.z = data[2];
        if (data.length > 3) {
            this.g = data[3];
        } else {
            this.g = 0;
        }
        this.timestamp = timestamp;
        this.timeIndex = timeIndex;
    }

    public float[] get3Axes() {
        return new float[]{x, y, z};
    }

    public double getX() { return (double) x;}

    public double getY() { return (double) y;}

    public double getZ() { return (double) z;}

    public double getG() { return (double) g;}

}


public class MotionData {
    private List<MotionRecord> records;
    private int capacity;
    private String type;
    private boolean calculate_g;

    public MotionData(int maxSize, String name, boolean need_g) {
        records = new ArrayList<>();
        capacity = maxSize;
        type = name;
        calculate_g = need_g;
    }

    public void reset() {
        records.clear();
    }

    public int size() {
        return records.size();
    }

    public int getCapacity() {
        return capacity;
    }

    public void add(float[] item, long timestamp, int index) {
        // check previous data point
        int prevIndex = 0;
        if (records.size() != 0) {
            prevIndex = get(-1).timeIndex;
            float[] prevItem = get(-1).get3Axes();

            // filling missing data points
            if (index > prevIndex + 1) {
                float[] step = linearStep(prevItem, item, index - prevIndex);
                for(int i = prevIndex + 1; i < index; ++i) {
                    // linear interpolation
                    float[] tmpItem = linearInterpolation(prevItem, step, i - prevIndex);
                    addItem(tmpItem, timestamp, i);
                    Log.i(type, String.format("%d [Interpolation]: %.4f, %.4f, %.4f",
                            i, tmpItem[0], tmpItem[1], tmpItem[2]));
                }

            }

            if (index >= prevIndex + 1) {
                addItem(item, timestamp, index);
                // Log.i(type, String.format("%d: %.4f, %.4f, %.4f",
                //        index, item[0], item[1], item[2]));
            } else {
                // if index is duplicate, just ignore the new one
                Log.e(type, String.format("Index issue, prev: %d, now: %d", prevIndex, index));
            }

        } else {
            // capture the first data point
            for(int i = 0; i < index; ++i) {
                // filling the previous moments with the same value
                addItem(item, timestamp, i);
                // Log.i(type, String.format("%d [FillFront]: %.4f, %.4f, %.4f",
                //        i, item[0], item[1], item[2]));
            }
            addItem(item, timestamp, index);
            // Log.i(type, String.format("%d: %.4f, %.4f, %.4f",
            //        index, item[0], item[1], item[2]));
        }

        if(records.size() > capacity) {
            this.records.remove(0);
        }
    }

    public void addItem(float[] item, long timestamp, int index) {
        if (calculate_g) {
            this.records.add(new MotionRecord(getDataWithG(item), timestamp, index));
        } else {
            this.records.add(new MotionRecord(item, timestamp, index));
        }

        if(records.size() > capacity) {
            this.records.remove(0);
        }
    }

    public MotionRecord get(int k) {
        // check exceptional situations
        if (k < 0) return this.records.get(this.records.size() + k);
        return this.records.get(k);
    }

    public static float[] linearStep(float[] startPoint, float[] endPoint, int interval) {
        return new float[]{(endPoint[0] - startPoint[0]) / interval,
                (endPoint[1] - startPoint[1]) / interval,
                (endPoint[2] - startPoint[2]) / interval};

    }

    public static float[] linearInterpolation(float[] startPoint, float[] step, int i) {
        return new float[]{startPoint[0] + step[0] * i,
        startPoint[1] + step[1] * i,
        startPoint[2] + step[2] * i};
    }

    public static float[] getDataWithG(float[] data) {
        float g = MathHelper.srss(data);
        return new float[]{data[0], data[1], data[2], g};
    }

    public boolean isAvailable(int index) {
        if(records.size() == 0) return false;
        return get(-1).timeIndex > index;
    }

}
