package ka170130.pmu.infinityscreen.sync;

import java.io.Serializable;

public class ClockResponse implements Serializable {

    private String deviceAddress;
    private long timestamp1;
    private long timestamp2;

    public ClockResponse(String deviceAddress, long timestamp1, long timestamp2) {
        this.deviceAddress = deviceAddress;
        this.timestamp1 = timestamp1;
        this.timestamp2 = timestamp2;
    }

    public String getDeviceAddress() {
        return deviceAddress;
    }

    public long getTimestamp1() {
        return timestamp1;
    }

    public long getTimestamp2() {
        return timestamp2;
    }
}
