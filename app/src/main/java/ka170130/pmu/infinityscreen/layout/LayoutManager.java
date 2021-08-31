package ka170130.pmu.infinityscreen.layout;

import android.graphics.Matrix;
import android.graphics.Point;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Display;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;

import androidx.constraintlayout.widget.ConstraintSet;
import androidx.lifecycle.ViewModelProvider;

import java.io.IOException;
import java.net.InetAddress;
import java.util.ArrayList;
import java.util.Iterator;

import ka170130.pmu.infinityscreen.MainActivity;
import ka170130.pmu.infinityscreen.containers.AccelerometerInfo;
import ka170130.pmu.infinityscreen.containers.DeviceRepresentation;
import ka170130.pmu.infinityscreen.containers.Message;
import ka170130.pmu.infinityscreen.containers.TransformInfo;
import ka170130.pmu.infinityscreen.containers.TransformUpdate;
import ka170130.pmu.infinityscreen.helpers.LogHelper;
import ka170130.pmu.infinityscreen.viewmodels.ConnectionViewModel;
import ka170130.pmu.infinityscreen.viewmodels.LayoutViewModel;
import ka170130.pmu.infinityscreen.viewmodels.SyncViewModel;

public class LayoutManager {

    private static final double INCH_TO_CM = 2.54;
    public static final int PIXEL_COUNT_WIDTH_INDEX = 0;
    public static final int PIXEL_COUNT_HEIGHT_INDEX = 1;

    private MainActivity mainActivity;
    private ConnectionViewModel connectionViewModel;
    private LayoutViewModel layoutViewModel;
    private SyncViewModel syncViewModel;

    private LayoutGenerator layoutGenerator;

    public LayoutManager(MainActivity mainActivity) {
        this.mainActivity = mainActivity;

        connectionViewModel = new ViewModelProvider(mainActivity).get(ConnectionViewModel.class);
        layoutViewModel = new ViewModelProvider(mainActivity).get(LayoutViewModel.class);
        syncViewModel = new ViewModelProvider(mainActivity).get(SyncViewModel.class);

        layoutGenerator = new MaximalRectanglesLayoutGenerator();
    }

    public Matrix getMatrix(
            TransformInfo self,
            TransformInfo viewport,
            int drawableWidth,
            int drawableHeight
    ) {
        Integer[] pixelCount = getPixelCount();

        Matrix matrix = new Matrix();
        // scale from original image size to viewport contain size
        float widthRatio = pixelCount[PIXEL_COUNT_WIDTH_INDEX] / (float) drawableWidth;
        widthRatio *= viewport.getScreenWidth() / self.getScreenWidth();
        float heightRatio = pixelCount[PIXEL_COUNT_HEIGHT_INDEX] / (float) drawableHeight;
        heightRatio *= viewport.getScreenHeight() / self.getScreenHeight();
        float minRatio = Math.min(widthRatio, heightRatio);
        matrix.postScale(minRatio, minRatio);

        // translate
        float horizontal = -drawableWidth * widthRatio;
        horizontal *=
                (self.getPosition().x - viewport.getPosition().x) / viewport.getScreenWidth();
        float vertical = -drawableHeight * heightRatio;
        vertical *=
                (self.getPosition().y - viewport.getPosition().y) / viewport.getScreenHeight();
        matrix.postTranslate(horizontal, vertical);

        LogHelper.log("Matrix: wr: " + minRatio + " hr: " + minRatio
                + " dx: " + horizontal + " dy: " + vertical);
        return matrix;
    }

    // need a special function since the video is automatically stretched to fit screen
    public Matrix getVideoMatrix(
            TransformInfo self,
            TransformInfo viewport,
            int drawableWidth,
            int drawableHeight
    ) {
        Integer[] pixelCount = getPixelCount();

        // scale from screen size to original video size
        float fixWidth = drawableWidth / (float) pixelCount[PIXEL_COUNT_WIDTH_INDEX];
        float fixHeight = drawableHeight / (float) pixelCount[PIXEL_COUNT_HEIGHT_INDEX];

        Matrix matrix = new Matrix();
        // scale from original video size to viewport contain size
        float widthRatio = pixelCount[PIXEL_COUNT_WIDTH_INDEX] / (float) drawableWidth;
        widthRatio *= viewport.getScreenWidth() / self.getScreenWidth();
        float heightRatio = pixelCount[PIXEL_COUNT_HEIGHT_INDEX] / (float) drawableHeight;
        heightRatio *= viewport.getScreenHeight() / self.getScreenHeight();
        float minRatio = Math.min(widthRatio, heightRatio);
        matrix.postScale(minRatio * fixWidth, minRatio * fixHeight);

        // translate
        float horizontal = -pixelCount[PIXEL_COUNT_WIDTH_INDEX] * widthRatio * fixWidth;
        horizontal *=
                (self.getPosition().x - viewport.getPosition().x) / viewport.getScreenWidth();
        float vertical = -pixelCount[PIXEL_COUNT_HEIGHT_INDEX] * heightRatio * fixHeight;
        vertical *=
                (self.getPosition().y - viewport.getPosition().y) / viewport.getScreenHeight();
        matrix.postTranslate(horizontal, vertical);

        LogHelper.log("Matrix: wr: " + minRatio * fixWidth + " hr: " + minRatio * fixHeight
                + " dx: " + horizontal + " dy: " + vertical);
        return matrix;
    }

    public void performTranslateEvent(
            TransformInfo transform,
            float distanceX,
            float distanceY
    ) {
        Integer[] pixelCount = getPixelCount();

//        DisplayMetrics dm = new DisplayMetrics();
//        mainActivity.getWindowManager().getDefaultDisplay().getMetrics(dm);

        double deltaX = distanceX / pixelCount[PIXEL_COUNT_WIDTH_INDEX];
        deltaX *= transform.getScreenWidth();
//        double deltaX = distanceX / dm.xdpi * INCH_TO_CM;
        double deltaY = distanceY / pixelCount[PIXEL_COUNT_HEIGHT_INDEX];
        deltaY *= transform.getScreenHeight();
//        double deltaY = distanceY / dm.ydpi * INCH_TO_CM;

        DeviceRepresentation.Position newPosition = new DeviceRepresentation.Position(0, 0);
        newPosition.x = (float) (transform.getPosition().x + deltaX);
        newPosition.y = (float) (transform.getPosition().y + deltaY);

        transform.setPosition(newPosition);
    }

    public void performAccelerometerEvent(
            TransformInfo transform,
            TransformInfo viewport,
            AccelerometerInfo accelerometerInfo,
            long now
    ) {
        // Calculate elapsed time from last accelerometer event
        float deltaTime = (now - accelerometerInfo.lastMovement) / 1000f;

        // Calculate movement
        float deltaX = accelerometerInfo.xVel * deltaTime +
                accelerometerInfo.xAccel * deltaTime * deltaTime / 2;
        float deltaY = accelerometerInfo.yVel * deltaTime +
                accelerometerInfo.yAccel * deltaTime * deltaTime / 2;

        Integer[] pixelCount = getPixelCount();
        // Ignore small movements
        if (Math.abs(deltaX) > AccelerometerInfo.MOVE_THRESHOLD) {
            // Calculate movement in CM
            float delta = deltaX * AccelerometerInfo.SENSITIVITY;
            delta /= pixelCount[PIXEL_COUNT_WIDTH_INDEX];
            delta *= transform.getScreenWidth();

            transform.getPosition().x += delta;
        }

        if (Math.abs(deltaY) > AccelerometerInfo.MOVE_THRESHOLD) {
            // Calculate movement in CM
            float delta = deltaY * AccelerometerInfo.SENSITIVITY;
            delta /= pixelCount[PIXEL_COUNT_HEIGHT_INDEX];
            delta *= transform.getScreenHeight();

            transform.getPosition().y += delta;
        }

        // Restrict movement
        float xMax = viewport.getPosition().x + (float) viewport.getScreenWidth();
        float yMax = viewport.getPosition().y + (float) viewport.getScreenHeight();
        float xMin = viewport.getPosition().x - (float) transform.getScreenWidth();
        float yMin = viewport.getPosition().y - (float) transform.getScreenHeight();

        if (transform.getPosition().x > xMax) {
            transform.getPosition().x = xMax;
        } else if (transform.getPosition().x < xMin) {
            transform.getPosition().x = xMin;
        }

        if (transform.getPosition().y > yMax) {
            transform.getPosition().y = yMax;
        } else if (transform.getPosition().y < yMin) {
            transform.getPosition().y = yMin;
        }

        // Update velocity
        accelerometerInfo.xVel += accelerometerInfo.xAccel * deltaTime;
        accelerometerInfo.yVel += accelerometerInfo.yAccel * deltaTime;

        // Dampen velocity to combat drift
        accelerometerInfo.xVel *= AccelerometerInfo.VELOCITY_DECAY;
        accelerometerInfo.yVel *= AccelerometerInfo.VELOCITY_DECAY;

        // Update last movement timestamp
        accelerometerInfo.lastMovement = now;
    }

    public void performZoomEvent(
            TransformInfo transform,
            float scaleFactor,
            float focusX,
            float focusY
    ) {
        Integer[] pixelCount = getPixelCount();
        double deltaX =
                focusX / pixelCount[PIXEL_COUNT_WIDTH_INDEX] * transform.getScreenWidth();
        double deltaY =
                focusY / pixelCount[PIXEL_COUNT_HEIGHT_INDEX] * transform.getScreenHeight();

        // change screen width and height
        double screenWidth = transform.getScreenWidth();
        double screenHeight = transform.getScreenHeight();

        screenWidth /= scaleFactor;
        screenHeight /= scaleFactor;

        transform.setScreenWidth(screenWidth);
        transform.setScreenHeight(screenHeight);

        // change position
        deltaX -= focusX / pixelCount[PIXEL_COUNT_WIDTH_INDEX] * transform.getScreenWidth();
        deltaY -= focusY / pixelCount[PIXEL_COUNT_HEIGHT_INDEX] * transform.getScreenHeight();

        DeviceRepresentation.Position newPosition = new DeviceRepresentation.Position(0, 0);
        newPosition.x = (float) (transform.getPosition().x + deltaX);
        newPosition.y = (float) (transform.getPosition().y + deltaY);

        transform.setPosition(newPosition);
    }

    public void reportSelfTransform(TransformInfo selfTransform) {
        try {
            Boolean isHost = connectionViewModel.getIsHost().getValue();

            if (isHost) {
                // Add Self Transform to Transform List
                layoutViewModel.addTransform(selfTransform);
            } else {
                // Send Transform Info to Host
                InetAddress hostAddress =
                        connectionViewModel.getHostDevice().getValue().getInetAddress();
                mainActivity.getTaskManager()
                        .runSenderTask(hostAddress, Message.newTransformMessage(selfTransform));
            }
        } catch (Exception e) {
            LogHelper.error(e);
        }
    }

    public void hostTransformListListener(ArrayList<TransformInfo> list) {
        // count the host too
        int groupSize = connectionViewModel.getGroupList().getValue().size() + 1;

        // ignore non-full lists
        if (list.size() < groupSize) {
            return;
        }

        // Execute Layout generation
        if (!layoutViewModel.getLayoutGeneratorExecuted()) {
            // Initialize Viewport Transform
            TransformInfo viewport = new TransformInfo(
                    "viewport",
                    -1,
                    TransformInfo.Orientation.LANDSCAPE,                // to be overwritten
                    0,                                       // to be overwritten
                    0                                       // to be overwritten
            );

            // Execute Layout Generation
            layoutGenerator.generate(list, viewport);
            layoutViewModel.setLayoutGeneratorExecuted(true);

            // Save Layout
            layoutViewModel.setTransformList(list);
            layoutViewModel.setBackupTransformList(list);
            layoutViewModel.setViewport(viewport);
            layoutViewModel.setBackupViewport(viewport);
        }

        // Broadcast Transform List Update
        try {
            TransformUpdate transformUpdate = new TransformUpdate(list, false);
            mainActivity.getTaskManager().sendToAllInGroup(
                    Message.newTransformListUpdateMessage(transformUpdate), false);
        } catch (Exception e) {
            LogHelper.error(e);
        }
    }

    public void hostBackupTransformListListener(ArrayList<TransformInfo> list) {
        Iterator<TransformInfo> debugIt = list.iterator();
        int index = 0;
        while (debugIt.hasNext()) {
            TransformInfo next = debugIt.next();
            LogHelper.log(
                    "backup#" + index + " " + next.getPosition().x + " " + next.getPosition().y);
            index++;
        }

        // Broadcast Transform List Update
        try {
            TransformUpdate transformUpdate = new TransformUpdate(list, true);
            mainActivity.getTaskManager().sendToAllInGroup(
                    Message.newTransformListUpdateMessage(transformUpdate), false);
        } catch (IOException e) {
            LogHelper.error(e);
        }
    }

    public void hostViewportListener(TransformInfo viewport) {
        // Broadcast Viewport Update
        try {
            ArrayList<TransformInfo> oneElementList = new ArrayList<>();
            oneElementList.add(viewport);

            TransformUpdate transformUpdate = new TransformUpdate(oneElementList, false);
            mainActivity.getTaskManager().sendToAllInGroup(
                    Message.newViewportUpdateMessage(transformUpdate), false);
        } catch (Exception e) {
            LogHelper.error(e);
        }
    }

    public void hostBackupViewportListener(TransformInfo viewport) {
        // Broadcast Viewport Update
        try {
            ArrayList<TransformInfo> oneElementList = new ArrayList<>();
            oneElementList.add(viewport);

            TransformUpdate transformUpdate = new TransformUpdate(oneElementList, true);
            mainActivity.getTaskManager().sendToAllInGroup(
                    Message.newViewportUpdateMessage(transformUpdate), false);
        } catch (IOException e) {
            LogHelper.error(e);
        }
    }

    public TransformInfo calculateSelfTransform() {
        String deviceName = connectionViewModel.getSelfDevice().getValue().getDeviceName();

        Integer[] pixelCount = getPixelCount();

        DisplayMetrics dm = new DisplayMetrics();
        mainActivity.getWindowManager().getDefaultDisplay().getMetrics(dm);
        double screenWidth = pixelCount[PIXEL_COUNT_WIDTH_INDEX] / dm.xdpi * INCH_TO_CM;
        double screenHeight = pixelCount[PIXEL_COUNT_HEIGHT_INDEX] / dm.ydpi * INCH_TO_CM;
        LogHelper.log("Screen dimensions : (" + screenWidth + ", " + screenHeight + ")");

        return new TransformInfo(
                deviceName,
                0,  // dummy value - will be overwritten
                TransformInfo.Orientation.PORTRAIT,
                screenWidth,
                screenHeight
        );
    }

    public Integer[] getPixelCount() {
        Integer[] pixelCount = {0, 0};

        WindowManager windowManager = mainActivity.getWindowManager();
        Display display = windowManager.getDefaultDisplay();

//        try {
        Point realSize = new Point();
//            Display.class
//                    .getMethod("getRealSize", Point.class)
//                    .invoke(display, realSize);
        display.getRealSize(realSize);
        pixelCount[PIXEL_COUNT_WIDTH_INDEX] = realSize.x;
        pixelCount[PIXEL_COUNT_HEIGHT_INDEX] = realSize.y;
//        } catch (Exception e) {
//            LogHelper.error(e);
//        }

        LogHelper.log(
                "Display pixel count : ("
                        + pixelCount[PIXEL_COUNT_WIDTH_INDEX] + ", "
                        + pixelCount[PIXEL_COUNT_HEIGHT_INDEX] + ")"
        );
        return pixelCount;
    }
}
