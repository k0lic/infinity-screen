package ka170130.pmu.infinityscreen.sync;

import java.io.Serializable;

import ka170130.pmu.infinityscreen.helpers.LogHelper;

public class SyncInfo implements Serializable {

    public static final String LOG_TAG = "sync-info-log-tag";

    public static final long MAXIMUM_ROUND_TRIP_TIME = 100;

    private static final int COUNT_THRESHOLD = 10;
    private static final double FACTOR = 0.15;

    private String deviceName;
    private long clockDiff;
    private int count;

    public SyncInfo(String deviceName) {
        this.deviceName = deviceName;
        clockDiff = 0;
        count = 0;
    }

    public String getDeviceName() {
        return deviceName;
    }

    public long getClockDiff() {
        return clockDiff;
    }

    public void update(long latency) {
        if (count < COUNT_THRESHOLD) {
            // calculate average
            long total = this.clockDiff * count;
            total += latency;
            long average = total / (count + 1);
            this.clockDiff = average;
        } else {
            // smooth out noise
            this.clockDiff = Math.round(latency * FACTOR + this.clockDiff * (1 - FACTOR));
        }

        LogHelper.log(SyncInfo.LOG_TAG, "UPDATED LATENCY for " + deviceName + " is " + this.clockDiff);

        count++;
    }

    public void setClockDiff(long clockDiff) {
        this.clockDiff = clockDiff;
    }
}
