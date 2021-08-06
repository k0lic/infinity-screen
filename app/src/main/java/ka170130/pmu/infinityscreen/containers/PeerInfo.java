package ka170130.pmu.infinityscreen.containers;

import android.net.wifi.p2p.WifiP2pDevice;

import java.io.Serializable;

public class PeerInfo implements Serializable {

    private String deviceName;
    private int status;
    private String deviceAddress;

    public PeerInfo(String deviceName, int status, String deviceAddress) {
        this.deviceName = deviceName;
        this.status = status;
        this.deviceAddress = deviceAddress;
    }

    public PeerInfo(WifiP2pDevice device) {
        this.deviceName = device.deviceName;
        this.status = device.status;
        this.deviceAddress = device.deviceAddress;
    }

    public String getDeviceName() {
        return deviceName;
    }

    public int getStatus() {
        return status;
    }

    public void setStatus(int status) {
        this.status = status;
    }

    public String getDeviceAddress() {
        return deviceAddress;
    }
}
