package ka170130.pmu.infinityscreen.layout;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.View;

import androidx.annotation.Nullable;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import ka170130.pmu.infinityscreen.AppBarAndStatusHelper;
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
    }

    private static final float MINIMUM_AREA_WIDTH = 100;
    private static final float MINIMUM_AREA_HEIGHT = 100;
    private static final float STROKE_WIDTH = 3;
    private static final float TEXT_SIZE = 40;

    private Paint paintPrimary;
    private Paint paintAccent;
    private Paint paintHighlight;

    private List<DeviceRepresentation> devices;

    private float areaWidth;
    private float areaHeight;
    private DeviceRepresentation.Position areaOrigin;

    private float realToViewFactor = 1;

    public DeviceLayoutView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);

        devices = new ArrayList<>();

        // Setup Paints
        Resources.Theme theme = context.getTheme();
        int color;

        paintPrimary = new Paint();
        color = AppBarAndStatusHelper.resolveRefColor(theme, R.attr.colorNeutral);
        paintPrimary.setColor(color);
        paintPrimary.setStyle(Paint.Style.STROKE);
        paintPrimary.setTextAlign(Paint.Align.CENTER);

        paintAccent = new Paint();
        color = AppBarAndStatusHelper.resolveRefColor(theme, R.attr.colorNeutral);
        paintAccent.setColor(color);
        paintAccent.setStyle(Paint.Style.STROKE);
        paintAccent.setTextAlign(Paint.Align.CENTER);

        paintHighlight = new Paint();
        color = AppBarAndStatusHelper.resolveRefColor(theme, R.attr.colorNeutral);
        paintHighlight.setColor(color);
        paintHighlight.setStyle(Paint.Style.FILL);

        setupDummyDevices(this);
    }

    public void registerDevice(DeviceRepresentation device) {
        devices.add(device);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        recalculateArea();
        recalculateFactor();
        recalculateDevices();
        adjustPaint();

        for (DeviceRepresentation device : devices) {
            drawDevice(canvas, device);
        }
    }

    private void drawDevice(Canvas canvas, DeviceRepresentation device) {
        float left = device.getRepPosition().x;
        float top = device.getRepPosition().y;
        float right = left + device.getRepWidth();
        float bottom = top + device.getRepHeight();

        RectF rect = new RectF(left, top, right, bottom);

        canvas.drawRect(rect, paintPrimary);

        float textX = (left + right) / 2;
        float textY = (top + bottom) / 2;
        textY += paintPrimary.getTextSize() / 3;
//        canvas.drawPoint(
//                textX,
//                textY,
//                paintPrimary
//        );
        canvas.drawText(
                String.valueOf(device.getNumberId()),
                textX,
                textY,
                paintPrimary
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
            origin.x = devices.get(0).getPosition().x;
            origin.y = devices.get(0).getPosition().y;

            terminal.x = devices.get(0).getPosition().x;
            terminal.y = devices.get(0).getPosition().y;
        }

        for (DeviceRepresentation device : devices) {
            float left = device.getRepPosition().x;
            float top = device.getRepPosition().y;
            float right = left + device.getRepWidth();
            float bottom = top + device.getRepHeight();

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

        float widthFactor = viewWidth / areaWidth;
        float heightFactor = viewHeight / areaHeight;

        realToViewFactor = Math.min(widthFactor, heightFactor);
    }

    private void recalculateDevices() {
        for (DeviceRepresentation device : devices) {
            device.setRepWidth(device.getWidth() * realToViewFactor);
            device.setRepHeight(device.getHeight() * realToViewFactor);

            DeviceRepresentation.Position actual = device.getPosition();
            DeviceRepresentation.Position position = new DeviceRepresentation.Position();
            position.x = (actual.x - areaOrigin.x) * realToViewFactor;
            position.y = (actual.y - areaOrigin.y) * realToViewFactor;
            device.setRepPosition(position);
        }
    }

    private void adjustPaint() {
        paintPrimary.setStrokeWidth(STROKE_WIDTH * realToViewFactor);
        paintPrimary.setTextSize(TEXT_SIZE * realToViewFactor);

        paintAccent.setStrokeWidth(STROKE_WIDTH * realToViewFactor);
        paintAccent.setTextSize(TEXT_SIZE * realToViewFactor);
    }
}
