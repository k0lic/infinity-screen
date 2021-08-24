package ka170130.pmu.infinityscreen.sync;

import java.io.Serializable;

import ka170130.pmu.infinityscreen.helpers.LogHelper;

public class SyncInfo implements Serializable {

    public static final String LOG_TAG = "sync-info-log-tag";

    private static final int COUNT_THRESHOLD = 10;
    private static final double FACTOR = 0.15;

    private String deviceAddress;
    private long clockDiff;
    private int count;

    public SyncInfo(String deviceAddress) {
        this.deviceAddress = deviceAddress;
        clockDiff = 0;
        count = 0;
    }

    public String getDeviceAddress() {
        return deviceAddress;
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

        LogHelper.log(SyncInfo.LOG_TAG, "UPDATED LATENCY for " + deviceAddress + " is " + this.clockDiff);

        count++;
    }
}
