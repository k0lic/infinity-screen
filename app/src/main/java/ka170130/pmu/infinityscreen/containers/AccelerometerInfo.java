package ka170130.pmu.infinityscreen.containers;

public class AccelerometerInfo {

    public static final int CALIBRATION_COUNT = 10;
    public static final float MOVE_THRESHOLD = 0.003f;
    public static final float VELOCITY_DECAY = 0.99f;
    public static final float SENSITIVITY = 10_000;
    public static final float FILTER_FACTOR = 1f;

    public long lastMovement;

    public float xAccel;
    public float yAccel;

    public float xVel;
    public float yVel;

    public int calibrationLeft;
    public int calibrationCount;
    public float[] offset;

    public AccelerometerInfo() {
        reset();
    }

    public void reset() {
        lastMovement = 0;
        xAccel = yAccel = 0;
        xVel = yVel = 0;

        calibrationLeft = CALIBRATION_COUNT;
        calibrationCount = 0;
        offset = new float[2];
        offset[0] = 0;
        offset[1] = 0;
    }
}
