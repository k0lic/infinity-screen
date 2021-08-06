package ka170130.pmu.infinityscreen.containers;

import java.io.Serializable;
import java.net.InetAddress;

public class PeerInetAddressInfo extends PeerInfo {

    private InetAddress inetAddress;

    public PeerInetAddressInfo(
            String deviceName,
            int status,
            String deviceAddress,
            InetAddress inetAddress
    ) {
        super(deviceName, status, deviceAddress);
        this.inetAddress = inetAddress;
    }

    public PeerInetAddressInfo(PeerInfo info, InetAddress inetAddress) {
        super(info.getDeviceName(), info.getStatus(), info.getDeviceAddress());
        this.inetAddress = inetAddress;
    }

    public InetAddress getInetAddress() {
        return inetAddress;
    }
}
