package ka170130.pmu.infinityscreen.layout;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.util.Log;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;

import androidx.annotation.Nullable;

import java.util.ArrayList;
import java.util.List;

import ka170130.pmu.infinityscreen.MainActivity;
import ka170130.pmu.infinityscreen.containers.DeviceRepresentation;
import ka170130.pmu.infinityscreen.helpers.AppBarAndStatusHelper;
import ka170130.pmu.infinityscreen.R;

// TODO: everything
public class DeviceLayoutView extends View {

    // TODO: remove setupDummyDevices
    public static void setupDummyDevices(DeviceLayoutView deviceLayoutView) {
        DeviceRepresentation.Position position = new DeviceRepresentation.Position(-30, -50);
        DeviceRepresentation device;

        float widths[] = {70, 70, 70, 135, 70};
        float heights[] = {100, 100, 100, 90, 100};
        float posX[] = {80, 150, 220, 80, 215};
        float posY[] = {72, 72, 72, 172, 172};

        for (int i = 0; i < 5; i++) {
            position = new DeviceRepresentation.Position(posX[i], posY[i]);
            device = new DeviceRepresentation( 1 + i, widths[i], heights[i], position);

            deviceLayoutView.registerDevice(device);
        }

        deviceLayoutView.setSelf(3);
    }

    private static final float MINIMUM_AREA_WIDTH = 10;
    private static final float MINIMUM_AREA_HEIGHT = 10;
    private static final float BUFFER_FACTOR = 0.1f;
    private static final float STROKE_WIDTH = 0.2f;
    private static final float TEXT_SIZE = 2.5f;

    private Paint paintPrimary;
    private Paint paintPrimaryText;
    private Paint paintAccent;
    private Paint paintAccentText;
    private Paint paintHighlight;
    private Paint paintBackground;

    private List<DeviceRepresentation> devices;
    private int self;

    private float areaWidth;
    private float areaHeight;
    private DeviceRepresentation.Position areaOrigin;

    private float buffer;
    private float autoMarginLeft = 0;
    private float autoMarginTop = 0;

    private float realToViewFactor = 1;

    private GestureDetector detector;
    // TODO: remove placeholder counter code
    private int counter = 0;

    public DeviceLayoutView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);

        devices = new ArrayList<>();

        // Setup Paints
        Resources.Theme theme = context.getTheme();
        int color;

        paintPrimary = new Paint();
        color = AppBarAndStatusHelper.resolveRefColor(theme, R.attr.colorLayoutPrimary);
        paintPrimary.setColor(color);
        paintPrimary.setStyle(Paint.Style.STROKE);

        paintPrimaryText = new Paint();
        paintPrimaryText.setColor(color);
        paintPrimaryText.setStyle(Paint.Style.FILL);
        paintPrimaryText.setTextAlign(Paint.Align.CENTER);

        paintAccent = new Paint();
        color = AppBarAndStatusHelper.resolveRefColor(theme, R.attr.colorLayoutAccent);
        paintAccent.setColor(color);
        paintAccent.setStyle(Paint.Style.STROKE);

        paintAccentText = new Paint();
        paintAccentText.setColor(color);
        paintAccentText.setStyle(Paint.Style.FILL);
        paintAccentText.setTextAlign(Paint.Align.CENTER);

        paintHighlight = new Paint();
        color = AppBarAndStatusHelper.resolveRefColor(theme, R.attr.colorLayoutHighlight);
        paintHighlight.setColor(color);
        paintHighlight.setStyle(Paint.Style.FILL);

        paintBackground = new Paint();
        color = AppBarAndStatusHelper.resolveRefColor(theme, R.attr.colorLayoutBackground);
        paintBackground.setColor(color);
        paintBackground.setStyle(Paint.Style.FILL);

        // TODO: implement OnGestureListener methods
        // Setup Gesture Detector
        detector = new GestureDetector(context, new GestureDetector.OnGestureListener() {
            @Override
            public boolean onDown(MotionEvent e) {
                counter++;
                invalidate();
                return true;
            }

            @Override
            public void onShowPress(MotionEvent e) {

            }

            @Override
            public boolean onSingleTapUp(MotionEvent e) {
                return false;
            }

            @Override
            public boolean onScroll(MotionEvent e1, MotionEvent e2, float distanceX, float distanceY) {
                return false;
            }

            @Override
            public void onLongPress(MotionEvent e) {

            }

            @Override
            public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
                return false;
            }
        });
    }

    public void registerDevice(DeviceRepresentation device) {
        devices.add(device);
    }

    public void clearDevices() {
        devices = new ArrayList<>();
    }

    public void setSelf(int num) {
        self = num;
    }

    public void redraw() {
        invalidate();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        recalculateArea();
        recalculateFactor();
        recalculateDevices();
        adjustPaint();

        DeviceRepresentation ownDevice = null;
        for (DeviceRepresentation device : devices) {
            // for 0 all devices are visible
            if (counter % (devices.size() + 1) == device.getNumberId()) {
                continue;
            }

            if (device.getNumberId() == self) {
                ownDevice = device;
                continue;
            }

            drawDevice(canvas, device);
        }

        // Paint own device last
        if (ownDevice != null) {
            drawDevice(canvas, ownDevice);
        }
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        boolean result = detector.onTouchEvent(event);
        return result;
    }

    private void drawDevice(Canvas canvas, DeviceRepresentation device) {
        // Paint selection
        Paint paintBorders = device.getNumberId() == self ? paintAccent : paintPrimary;
        Paint paintFill = device.getNumberId() == self ? paintHighlight : paintBackground;
        Paint paintText = device.getNumberId() == self ? paintAccentText : paintPrimaryText;

        // Calculate Edges
        float fix = paintBorders.getStrokeWidth();
        float left = device.getRepPosition().x + buffer + autoMarginLeft + fix;
        float top = device.getRepPosition().y + buffer + autoMarginTop + fix;
        float right = left + device.getRepWidth() - fix;
        float bottom = top + device.getRepHeight() - fix;

        RectF rect = new RectF(left, top, right, bottom);

        // Fill Rectangle
        canvas.drawRect(rect, paintFill);

        // Draw Rectangle Borders
        canvas.drawRect(rect, paintBorders);

        // Draw Text
        float textX = (left + right) / 2;
        float textY = (top + bottom) / 2;
        textY += paintText.getTextSize() / 3;
        canvas.drawText(
                String.valueOf(device.getNumberId()),
                textX,
                textY,
                paintText
        );
    }

    private void recalculateArea() {
        // Top Left corner of area
        DeviceRepresentation.Position origin = new DeviceRepresentation.Position(
                0, 0);
        // Bottom Right corner of area
        DeviceRepresentation.Position terminal = new DeviceRepresentation.Position(
                0, 0);

        if (devices.size() > 0) {
            DeviceRepresentation device = devices.get(0);

            origin.x = device.getPosition().x;
            origin.y = device.getPosition().y;

            terminal.x = device.getPosition().x + device.getWidth();
            terminal.y = device.getPosition().y + device.getHeight();
        }

        for (DeviceRepresentation device : devices) {
            float left = device.getPosition().x;
            float top = device.getPosition().y;
            float right = left + device.getWidth();
            float bottom = top + device.getHeight();

            origin.x = Math.min(left, origin.x);
            origin.y = Math.min(top, origin.y);

            terminal.x = Math.max(right, terminal.x);
            terminal.y = Math.max(bottom, terminal.y);
        }

        areaOrigin = origin;
        areaWidth = Math.max(terminal.x - origin.x, MINIMUM_AREA_WIDTH);
        areaHeight = Math.max(terminal.y - origin.y, MINIMUM_AREA_HEIGHT);
    }

    private void recalculateFactor() {
        int viewWidth = this.getWidth();
        int viewHeight = this.getHeight();

        // A buffer width border around the layout - moving the content from the edges
        buffer = BUFFER_FACTOR * Math.min(viewWidth, viewHeight);

        float widthFactor = (viewWidth - 2 * buffer) / areaWidth;
        float heightFactor = (viewHeight - 2 * buffer) / areaHeight;

        realToViewFactor = Math.min(widthFactor, heightFactor);
    }

    private void recalculateDevices() {
        float xMax = 0;
        float yMax = 0;

        Log.d(MainActivity.LOG_TAG, "Device Representations:");
        for (DeviceRepresentation device : devices) {
            device.setRepWidth(device.getWidth() * realToViewFactor);
            device.setRepHeight(device.getHeight() * realToViewFactor);

            DeviceRepresentation.Position actual = device.getPosition();
            DeviceRepresentation.Position position = new DeviceRepresentation.Position();
            position.x = (actual.x - areaOrigin.x) * realToViewFactor;
            position.y = (actual.y - areaOrigin.y) * realToViewFactor;
            device.setRepPosition(position);

            xMax = Math.max(position.x + device.getRepWidth(), xMax);
            yMax = Math.max(position.y + device.getRepHeight(), yMax);

            Log.d(
                    MainActivity.LOG_TAG,
                    device.getNumberId()
                            + " w: " + device.getWidth()
                            + " h: " + device.getHeight()
                            + " pos: (" + device.getPosition().x
                            + ", " + device.getPosition().y
                            + ") rw: " + device.getRepWidth()
                            + " rh: " + device.getRepHeight()
                            + " rpos: (" + device.getRepPosition().x
                            + ", " + device.getRepPosition().y
                            + ")"
            );
        }

        // Calculate Auto Margins so content is centered
        int viewWidth = this.getWidth();
        int viewHeight = this.getHeight();

        autoMarginLeft = Math.max((viewWidth - xMax - 2 * buffer) / 2, 0);
        autoMarginTop = Math.max((viewHeight - yMax - 2 * buffer) / 2, 0);
    }

    private void adjustPaint() {
        paintPrimary.setStrokeWidth(STROKE_WIDTH * realToViewFactor);
        paintAccent.setStrokeWidth(STROKE_WIDTH * realToViewFactor);

        paintPrimaryText.setTextSize(TEXT_SIZE * realToViewFactor);
        paintAccentText.setTextSize(TEXT_SIZE * realToViewFactor);
    }
}
