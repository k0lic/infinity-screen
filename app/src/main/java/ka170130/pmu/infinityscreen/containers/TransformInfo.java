package ka170130.pmu.infinityscreen.containers;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Iterator;

public class TransformInfo implements Serializable {

    public enum Orientation {
        PORTRAIT,
        LANDSCAPE
    }

    private String deviceName;
    private int numberId;

    private Orientation orientation;
    private double screenWidth;
    private double screenHeight;

    private DeviceRepresentation.Position position;

    public TransformInfo(String deviceName, int numberId, Orientation orientation, double screenWidth, double screenHeight) {
        this.deviceName = deviceName;
        this.numberId = numberId;
        this.orientation = orientation;
        this.screenWidth = screenWidth;
        this.screenHeight = screenHeight;

        position = new DeviceRepresentation.Position(); // 0 0
    }

    public TransformInfo(TransformInfo other) {
        this.deviceName = other.getDeviceName();
        this.numberId = other.getNumberId();
        this.orientation = other.orientation;
        this.screenWidth = other.getScreenWidth();
        this.screenHeight = other.getScreenHeight();

        position = new DeviceRepresentation.Position(other.getPosition());
    }

    public String getDeviceName() {
        return deviceName;
    }

    public int getNumberId() {
        return numberId;
    }

    public Orientation getOrientation() {
        return orientation;
    }

    public void setOrientation(Orientation orientation) {
        this.orientation = orientation;
    }

    public double getScreenWidth() {
        return screenWidth;
    }

    public void setScreenWidth(double screenWidth) {
        this.screenWidth = screenWidth;
    }

    public double getScreenHeight() {
        return screenHeight;
    }

    public void setScreenHeight(double screenHeight) {
        this.screenHeight = screenHeight;
    }

    public DeviceRepresentation.Position getPosition() {
        return position;
    }

    public void setPosition(DeviceRepresentation.Position position) {
        this.position = position;
    }

    public void rotate() {
        // Change orientation
        switch (orientation) {
            case PORTRAIT:
                orientation = TransformInfo.Orientation.LANDSCAPE;
                break;
            case LANDSCAPE:
                orientation = TransformInfo.Orientation.PORTRAIT;
                break;
        }

        // Swap dimensions
        double width = screenHeight;
        double height = screenWidth;
        screenWidth = width;
        screenHeight = height;
    }
}
