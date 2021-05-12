package ca.uwaterloo.crysp.sharingmodeservice.gesture;

// convert inconsistent timestamp into standard reference time line


public class ReferenceTimeLine {
    private static final int S_TO_MS = 1000;

    private long t0;
    private int fs;
    private double interval;

    private boolean initialized;


    private double indexToDTime(int index) {
        return interval * index;
    }

    public boolean isInitialized() {
        return initialized;
    }

    public ReferenceTimeLine(int samplingRate) {
        fs = samplingRate;
        interval = S_TO_MS / fs;
    }

    public void set(int samplingRate, long startTime) {
        fs = samplingRate;
        t0 = startTime;
        interval = S_TO_MS / fs;
        initialized = true;
    }

    public void reset(int samplingRate) {
        fs = samplingRate;
        interval = S_TO_MS / fs;
        initialized = false;
    }

    public int timeToIndex(long time) {
        int curDTime = (int)(time - t0);
        return (int) Math.round(curDTime / interval);
    }

}
