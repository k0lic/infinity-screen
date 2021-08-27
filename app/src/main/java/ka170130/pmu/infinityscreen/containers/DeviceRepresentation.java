package ka170130.pmu.infinityscreen.containers;

import java.io.Serializable;

public class DeviceRepresentation {

    private int numberId;

    private float width;
    private float height;
    private Position position;

    private float repWidth;
    private float repHeight;
    private Position repPosition;

    public DeviceRepresentation(int numberId, float width, float height, Position position) {
        this.numberId = numberId;
        this.width = width;
        this.height = height;
        this.position = position;

        // placeholder values - to be overwritten
        this.repWidth = width;
        this.repHeight = height;
        this.repPosition = new Position(position.x, position.y);
    }

    public DeviceRepresentation(TransformInfo transformInfo) {
        numberId = transformInfo.getNumberId();
        width = (float) transformInfo.getScreenWidth();
        height = (float) transformInfo.getScreenHeight();
        position = transformInfo.getPosition();

        // placeholder values - to be overwritten
        repWidth = width;
        repHeight = height;
        repPosition = new Position(position.x, position.y);
    }

    public int getNumberId() {
        return numberId;
    }

    public float getWidth() {
        return width;
    }

    public void setWidth(float width) {
        this.width = width;
    }

    public float getHeight() {
        return height;
    }

    public void setHeight(float height) {
        this.height = height;
    }

    public Position getPosition() {
        return position;
    }

    public void setPosition(Position position) {
        this.position = position;
    }

    public float getRepWidth() {
        return repWidth;
    }

    public void setRepWidth(float repWidth) {
        this.repWidth = repWidth;
    }

    public float getRepHeight() {
        return repHeight;
    }

    public void setRepHeight(float repHeight) {
        this.repHeight = repHeight;
    }

    public Position getRepPosition() {
        return repPosition;
    }

    public void setRepPosition(Position repPosition) {
        this.repPosition = repPosition;
    }

    public static class Position implements Serializable {
        public float x;
        public float y;

        public Position() {
            this.x = 0;
            this.y = 0;
        }

        public Position(float x, float y) {
            this.x = x;
            this.y = y;
        }

        public Position(Position other) {
            this.x = other.x;
            this.y = other.y;
        }
    }
}
