package ka170130.pmu.infinityscreen.layout;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.view.View;

import androidx.annotation.Nullable;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import ka170130.pmu.infinityscreen.containers.DeviceRepresentation;
import ka170130.pmu.infinityscreen.containers.TransformInfo;
import ka170130.pmu.infinityscreen.helpers.AppBarAndStatusHelper;
import ka170130.pmu.infinityscreen.R;
import ka170130.pmu.infinityscreen.helpers.Callback;
import ka170130.pmu.infinityscreen.helpers.LogHelper;

public class DeviceLayoutView extends View {

    // TODO: remove setupDummyDevices
    public static void setupDummyDevices(DeviceLayoutView deviceLayoutView) {
        DeviceRepresentation.Position position = new DeviceRepresentation.Position(-30, -50);
        DeviceRepresentation device;

        float[] widths = {70, 70, 70, 135, 70};
        float[] heights = {100, 100, 100, 90, 100};
        float[] posX = {80, 150, 220, 80, 215};
        float[] posY = {72, 72, 72, 172, 172};

        for (int i = 0; i < 5; i++) {
            position = new DeviceRepresentation.Position(posX[i], posY[i]);
            device = new DeviceRepresentation( 1 + i, widths[i], heights[i], position, TransformInfo.Orientation.PORTRAIT);

            deviceLayoutView.registerDevice(device);
        }

        position = new DeviceRepresentation.Position(posX[0], posY[0] + 35);
        device = new DeviceRepresentation(-1, 210, 118, position, TransformInfo.Orientation.LANDSCAPE);
        deviceLayoutView.setViewport(device);

        deviceLayoutView.setSelf(3);
    }

    private static final float MINIMUM_AREA_WIDTH = 10;
    private static final float MINIMUM_AREA_HEIGHT = 10;
    private static final float BUFFER_FACTOR = 0.1f;

    private static final float STROKE_WIDTH = 0.2f;
    private static final float TEXT_SIZE = 2.5f;

    private static final int FULL_ALPHA = 255;
    private static final int HALF_ALPHA = 127;
    private static final int WEAK_ALPHA = 64;
    private static final int WEAKEST_ALPHA = 32;

    private static final float SPAN_RATIO = 2;

    private Paint paintPrimary;
    private Paint paintPrimaryText;
    private Paint paintAccent;
    private Paint paintAccentText;
    private Paint paintHighlight;
    private Paint paintBackground;
    private Paint paintViewportBorders;
    private Paint paintViewportBackground;

    private List<DeviceRepresentation> devices;
    private DeviceRepresentation viewport;
    private int self;
    private boolean focusViewport;

    private float areaWidth;
    private float areaHeight;
    private DeviceRepresentation.Position areaOrigin;

    private float buffer;
    private float autoMarginLeft = 0;
    private float autoMarginTop = 0;

    private float realToViewFactor = 1;

    private GestureDetector gestureDetector;
    private ScaleGestureDetector scaleGestureDetector;
    private DeviceRepresentation currentTransform;
    private DeviceRepresentation currentlyScaling;

    private Callback<DeviceRepresentation> deviceCallback;
    private Callback<DeviceRepresentation> viewportCallback;

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

        paintViewportBorders = new Paint();
        color = AppBarAndStatusHelper.resolveRefColor(theme, R.attr.colorNegativePrimary);
        paintViewportBorders.setColor(color);
        paintViewportBorders.setStyle(Paint.Style.STROKE);

        paintViewportBackground = new Paint();
        paintViewportBackground.setColor(color);
        paintViewportBackground.setStyle(Paint.Style.FILL);

        // Focus devices by default
        changeFocus(false);

        // Setup Gesture Detectors
        gestureDetector = new GestureDetector(context, new GestureDetector.SimpleOnGestureListener() {
            @Override
            public boolean onDown(MotionEvent e) {
                // Get clicked transform
                float x = e.getX();
                float y = e.getY();
                currentTransform = getClickedTransform(x, y);

                return true;
            }

            @Override
            public boolean onScroll(MotionEvent e1, MotionEvent e2, float distanceX, float distanceY) {
                // e1 is DownEvent, e2 is MoveEvent
                if (e2.getPointerCount() == 1) {
                    // Translate Scroll
                    if (currentTransform != null) {
                        // Update position
                        DeviceRepresentation.Position newPosition = new DeviceRepresentation.Position();
                        newPosition.x = currentTransform.getRepPosition().x - distanceX;
                        newPosition.y = currentTransform.getRepPosition().y - distanceY;
                        currentTransform.setRepPosition(newPosition);

                        // Recalculate
                        recalculateTransformInverse(currentTransform);

                        // Callback
                        invokeCallback(currentTransform);
                    }
                }

                return true;
            }
        });

        gestureDetector.setOnDoubleTapListener(new GestureDetector.OnDoubleTapListener() {
            @Override
            public boolean onSingleTapConfirmed(MotionEvent e) {
                // ignore
                return true;
            }

            // Rotate
            @Override
            public boolean onDoubleTap(MotionEvent e) {
                if (currentTransform != null) {
                    // Update Orientation
                    TransformInfo.Orientation oldOrientation = currentTransform.getOrientation();
                    TransformInfo.Orientation newOrientation = TransformInfo.Orientation.PORTRAIT;
                    switch (oldOrientation) {
                        case PORTRAIT:
                            newOrientation = TransformInfo.Orientation.LANDSCAPE;
                            break;
                        case LANDSCAPE:
                            newOrientation = TransformInfo.Orientation.PORTRAIT;
                            break;
                    }
                    currentTransform.setOrientation(newOrientation);

                    // Swap dimensions
                    float width = currentTransform.getRepHeight();
                    float height = currentTransform.getRepWidth();
                    currentTransform.setRepWidth(width);
                    currentTransform.setRepHeight(height);

                    // Recalculate
                    recalculateTransformInverse(currentTransform);

                    // Callback
                    invokeCallback(currentTransform);
                }

                return true;
            }

            @Override
            public boolean onDoubleTapEvent(MotionEvent e) {
                // ignore
                return true;
            }
        });

        scaleGestureDetector = new ScaleGestureDetector(context, new ScaleGestureDetector.OnScaleGestureListener() {
            @Override
            public boolean onScale(ScaleGestureDetector detector) {
                if (currentlyScaling != null) {
                    // Translate to center
                    DeviceRepresentation.Position newPosition = new DeviceRepresentation.Position();
                    newPosition.x = currentlyScaling.getRepPosition().x +
                            currentlyScaling.getRepWidth() * 0.5f;
                    newPosition.y = currentlyScaling.getRepPosition().y +
                            currentlyScaling.getRepHeight() * 0.5f;

                    // Calculate Scale Factors
                    float scaleFactor = detector.getScaleFactor();
                    float spanX = detector.getCurrentSpanX();
                    float spanY = detector.getCurrentSpanY();
                    float horizontalFactor = 1f;
                    float verticalFactor = 1f;
                    if (spanY == 0 || spanX / spanY > SPAN_RATIO) {
                        horizontalFactor = scaleFactor;
                        verticalFactor = 1f;
                    } else if (spanX == 0 || spanY / spanX > SPAN_RATIO) {
                        horizontalFactor = 1f;
                        verticalFactor = scaleFactor;
                    } else {
                        horizontalFactor = scaleFactor;
                        verticalFactor = scaleFactor;
                    }

                    // Update dimensions
                    float width = currentlyScaling.getRepWidth() * horizontalFactor;
                    float height = currentlyScaling.getRepHeight() * verticalFactor;
                    currentlyScaling.setRepWidth(width);
                    currentlyScaling.setRepHeight(height);

                    // Translate to center inverse
                    newPosition.x -= currentlyScaling.getRepWidth() * 0.5f;
                    newPosition.y -= currentlyScaling.getRepHeight() * 0.5f;

                    // Update position
                    currentlyScaling.setRepPosition(newPosition);

                    // Recalculate
                    recalculateTransformInverse(currentlyScaling);

                    //  Callback
                    invokeCallback(currentlyScaling);
                }

                return true;
            }

            @Override
            public boolean onScaleBegin(ScaleGestureDetector detector) {
                // Get clicked transform
                float x = detector.getFocusX();
                float y = detector.getFocusY();
                currentlyScaling = getClickedTransform(x, y);

                return true;
            }

            @Override
            public void onScaleEnd(ScaleGestureDetector detector) {
                currentlyScaling = null;
            }
        });

        // TODO: comment this line
//        setupDummyDevices(this);
    }

    public List<DeviceRepresentation> getDevices() {
        return devices;
    }

    public DeviceRepresentation getViewport() {
        return viewport;
    }

    public void registerDevice(DeviceRepresentation device) {
        devices.add(device);
    }

    public void clearDevices() {
        devices = new ArrayList<>();
    }

    public void setViewport(DeviceRepresentation viewport) {
        this.viewport = viewport;
    }

    public int getSelf() {
        return self;
    }

    public void setSelf(int num) {
        self = num;
    }

    public void redraw() {
        invalidate();
    }

    public boolean isFocusViewport() {
        return focusViewport;
    }

    public void changeFocus(boolean viewportFocus) {
        focusViewport = viewportFocus;

        int devicesAlpha = focusViewport ? HALF_ALPHA : FULL_ALPHA;
        int viewportBorderAlpha = focusViewport ? FULL_ALPHA : HALF_ALPHA;
        int viewportBackgroundAlpha = focusViewport ? WEAK_ALPHA : WEAKEST_ALPHA;

        paintPrimary.setAlpha(devicesAlpha);
        paintPrimaryText.setAlpha(devicesAlpha);
        paintAccent.setAlpha(devicesAlpha);
        paintAccentText.setAlpha(devicesAlpha);
        paintHighlight.setAlpha(devicesAlpha);
        paintBackground.setAlpha(devicesAlpha);

        paintViewportBorders.setAlpha(viewportBorderAlpha);
        paintViewportBackground.setAlpha(viewportBackgroundAlpha);
    }

    public void setDeviceCallback(Callback<DeviceRepresentation> deviceCallback) {
        this.deviceCallback = deviceCallback;
    }

    public void setViewportCallback(Callback<DeviceRepresentation> viewportCallback) {
        this.viewportCallback = viewportCallback;
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        recalculateArea();
        recalculateFactor();
        adjustPaint();
        recalculateDevices();
        recalculateViewport();

        DeviceRepresentation ownDevice = null;
        // Draw all devices but own
        for (DeviceRepresentation device : devices) {
            if (device.getNumberId() == self) {
                ownDevice = device;
                continue;
            }

            drawDevice(canvas, device);
        }

        // Draw own device last
        if (ownDevice != null) {
            drawDevice(canvas, ownDevice);
        }

        // Draw viewport
        drawViewport(canvas);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        boolean result = gestureDetector.onTouchEvent(event);
        result = scaleGestureDetector.onTouchEvent(event) || result;
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

    private void drawViewport(Canvas canvas) {
        if (viewport == null) {
            return;
        }

        // Paint selection
        Paint paintBorders = paintViewportBorders;
        Paint paintFill = paintViewportBackground;

        // Calculate Edges
        float fix = paintBorders.getStrokeWidth();
        float left = viewport.getRepPosition().x + buffer + autoMarginLeft + fix;
        float top = viewport.getRepPosition().y + buffer + autoMarginTop + fix;
        float right = left + viewport.getRepWidth() - fix;
        float bottom = top + viewport.getRepHeight() - fix;

        RectF rect = new RectF(left, top, right, bottom);

        // Fill Rectangle
        canvas.drawRect(rect, paintFill);

        // Draw Rectangle Borders
        canvas.drawRect(rect, paintBorders);
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

        LogHelper.log("Device Representations:");
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

            LogHelper.log(device.getNumberId()
                    + " w: " + device.getWidth()
                    + " h: " + device.getHeight()
                    + " pos: (" + device.getPosition().x
                    + ", " + device.getPosition().y
                    + ") rw: " + device.getRepWidth()
                    + " rh: " + device.getRepHeight()
                    + " rpos: (" + device.getRepPosition().x
                    + ", " + device.getRepPosition().y
                    + ")");
        }

        // Calculate Auto Margins so content is centered
        int viewWidth = this.getWidth();
        int viewHeight = this.getHeight();

        autoMarginLeft = Math.max((viewWidth - xMax - 2 * buffer) / 2, 0);
        autoMarginTop = Math.max((viewHeight - yMax - 2 * buffer) / 2, 0);
    }

    private void recalculateViewport() {
        if (viewport == null) {
            return;
        }

        viewport.setRepWidth(viewport.getWidth() * realToViewFactor);
        viewport.setRepHeight(viewport.getHeight() * realToViewFactor);

        DeviceRepresentation.Position actual = viewport.getPosition();
        DeviceRepresentation.Position position = new DeviceRepresentation.Position();
        position.x = (actual.x - areaOrigin.x) * realToViewFactor;
        position.y = (actual.y - areaOrigin.y) * realToViewFactor;
        viewport.setRepPosition(position);

        LogHelper.log("Viewport Representation:");
        LogHelper.log(
                viewport.getNumberId()
                        + " w: " + viewport.getWidth()
                        + " h: " + viewport.getHeight()
                        + " pos: (" + viewport.getPosition().x
                        + ", " + viewport.getPosition().y
                        + ") rw: " + viewport.getRepWidth()
                        + " rh: " + viewport.getRepHeight()
                        + " rpos: (" + viewport.getRepPosition().x
                        + ", " + viewport.getRepPosition().y
                        + ")"
        );
    }

    private void adjustPaint() {
        float strokeWidth = STROKE_WIDTH * realToViewFactor;
        float textSize = TEXT_SIZE * realToViewFactor;

        paintPrimary.setStrokeWidth(strokeWidth);
        paintAccent.setStrokeWidth(strokeWidth);
        paintViewportBorders.setStrokeWidth(strokeWidth);

        paintPrimaryText.setTextSize(textSize);
        paintAccentText.setTextSize(textSize);
    }

    private void reportDeviceChange(DeviceRepresentation device) {
        invokeCallback(deviceCallback, device);
    }

    private void reportViewportChange(DeviceRepresentation viewport) {
        invokeCallback(viewportCallback, viewport);
    }

    private void invokeCallback(DeviceRepresentation transform) {
        if (focusViewport) {
            invokeCallback(viewportCallback, transform);
        } else {
            invokeCallback(deviceCallback, transform);
        }
    }

    private void invokeCallback(Callback<DeviceRepresentation> callback, DeviceRepresentation arg) {
        if (callback == null) {
            // skip
            return;
        }

        callback.invoke(arg);
    }

    private DeviceRepresentation getClickedTransform(float x, float y) {
        if (focusViewport) {
            // Check if Viewport was clicked
            if (inside(viewport, x, y)) {
                return viewport;
            }
        } else {
            // Iterate through devices and check if each was clicked
            Iterator<DeviceRepresentation> iterator = devices.iterator();
            while (iterator.hasNext()) {
                DeviceRepresentation next = iterator.next();
                if (inside(next, x, y)) {
                    return next;
                }
            }
        }

        return null;
    }

    private boolean inside(DeviceRepresentation transform, float x, float y) {
        // Paint selection
        Paint paintBorders = focusViewport ?
                paintViewportBorders :
                (transform.getNumberId() == self ? paintAccent : paintPrimary);

        // Calculate Edges
        float fix = paintBorders.getStrokeWidth();
        float left = transform.getRepPosition().x + buffer + autoMarginLeft + fix;
        float top = transform.getRepPosition().y + buffer + autoMarginTop + fix;
        float right = left + transform.getRepWidth() - fix;
        float bottom = top + transform.getRepHeight() - fix;

        // Check if inside
        return x > left && x < right && y > top && y < bottom;
    }

    private void recalculateTransformInverse(DeviceRepresentation transform) {
        transform.setWidth(transform.getRepWidth() / realToViewFactor);
        transform.setHeight(transform.getRepHeight() / realToViewFactor);

        DeviceRepresentation.Position repPosition = transform.getRepPosition();
        DeviceRepresentation.Position position = new DeviceRepresentation.Position();
        position.x = (repPosition.x / realToViewFactor) + areaOrigin.x;
        position.y = (repPosition.y / realToViewFactor) + areaOrigin.y;
        transform.setPosition(position);
    }
}
