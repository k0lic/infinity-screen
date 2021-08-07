package ka170130.pmu.infinityscreen.containers;

import java.io.Serializable;

public class TransformInfo implements Serializable {

    private String deviceAddress;
    private int numberId;

    private double screenWidth;
    private double screenHeight;

    private DeviceRepresentation.Position position;

    public TransformInfo(String deviceAddress, int numberId, double screenWidth, double screenHeight) {
        this.deviceAddress = deviceAddress;
        this.numberId = numberId;
        this.screenWidth = screenWidth;
        this.screenHeight = screenHeight;

        position = new DeviceRepresentation.Position(); // 0 0
    }

    public String getDeviceAddress() {
        return deviceAddress;
    }

    public int getNumberId() {
        return numberId;
    }

    public double getScreenWidth() {
        return screenWidth;
    }

    public double getScreenHeight() {
        return screenHeight;
    }

    public DeviceRepresentation.Position getPosition() {
        return position;
    }

    public void setPosition(DeviceRepresentation.Position position) {
        this.position = position;
    }
}
