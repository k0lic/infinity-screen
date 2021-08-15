package ka170130.pmu.infinityscreen.layout;

import android.graphics.Matrix;
import android.graphics.Point;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Display;
import android.view.Window;
import android.view.WindowManager;

import androidx.lifecycle.ViewModelProvider;

import java.net.InetAddress;
import java.util.ArrayList;

import ka170130.pmu.infinityscreen.MainActivity;
import ka170130.pmu.infinityscreen.containers.DeviceRepresentation;
import ka170130.pmu.infinityscreen.containers.Message;
import ka170130.pmu.infinityscreen.containers.TransformInfo;
import ka170130.pmu.infinityscreen.helpers.LogHelper;
import ka170130.pmu.infinityscreen.viewmodels.ConnectionViewModel;
import ka170130.pmu.infinityscreen.viewmodels.LayoutViewModel;

public class LayoutManager {

    private static final double INCH_TO_CM = 2.54;
    public static final int PIXEL_COUNT_WIDTH_INDEX = 0;
    public static final int PIXEL_COUNT_HEIGHT_INDEX = 1;

    private MainActivity mainActivity;
    private ConnectionViewModel connectionViewModel;
    private LayoutViewModel layoutViewModel;

    public LayoutManager(MainActivity mainActivity) {
        this.mainActivity = mainActivity;

        connectionViewModel = new ViewModelProvider(mainActivity).get(ConnectionViewModel.class);
        layoutViewModel = new ViewModelProvider(mainActivity).get(LayoutViewModel.class);
    }

    public Matrix getMatrix(
            TransformInfo self,
            TransformInfo viewport,
            int drawableWidth,
            int drawableHeight
    ) {
        Integer[] pixelCount = getPixelCount();

        Matrix matrix = new Matrix();
        float widthRatio =
                pixelCount[LayoutManager.PIXEL_COUNT_WIDTH_INDEX] / (float) drawableWidth;
        widthRatio *= viewport.getScreenWidth() / self.getScreenWidth();
        float heightRatio =
                pixelCount[LayoutManager.PIXEL_COUNT_HEIGHT_INDEX] / (float) drawableHeight;
        heightRatio *= viewport.getScreenHeight() / self.getScreenHeight();
        float ratio = Math.min(widthRatio, heightRatio);
        matrix.postScale(ratio, ratio);

        float horizontal = -drawableWidth * widthRatio;
        horizontal *=
                (self.getPosition().x - viewport.getPosition().x) / viewport.getScreenWidth();
        float vertical = -drawableHeight * heightRatio;
        vertical *=
                (self.getPosition().y - viewport.getPosition().y) / viewport.getScreenHeight();
        matrix.postTranslate(horizontal, vertical);

        LogHelper.log("Matrix: wr: " + widthRatio + " hr: " + heightRatio
                + " dx: " + horizontal + " dy: " + vertical);
        return matrix;
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
            TransformInfo viewport = simpleLayoutGenerator(list);
            layoutViewModel.setLayoutGeneratorExecuted(true);
            layoutViewModel.setViewport(viewport);
        }

        // Broadcast Transform List Update
        try {
            mainActivity.getTaskManager()
                    .sendToAllInGroup(Message.newTransformListUpdateMessage(list), false);
//            mainActivity.getTaskManager()
//                    .runBroadcastTask(Message.newTransformListUpdateMessage(list));
        } catch (Exception e) {
            LogHelper.error(e);
        }
    }

    public void hostViewportListener(TransformInfo viewport) {
        // Broadcast Viewport Update
        try {
            mainActivity.getTaskManager()
                    .sendToAllInGroup(Message.newViewportUpdateMessage(viewport), false);
//                    .runBroadcastTask(Message.newViewportUpdateMessage(viewport));
        } catch (Exception e) {
            LogHelper.error(e);
        }
    }

    public TransformInfo calculateSelfTransform() {
        String deviceAddress = connectionViewModel.getSelfDevice().getValue().getDeviceAddress();

        Integer[] pixelCount = getPixelCount();

        DisplayMetrics dm = new DisplayMetrics();
        mainActivity.getWindowManager().getDefaultDisplay().getMetrics(dm);
        double screenWidth = pixelCount[PIXEL_COUNT_WIDTH_INDEX] / dm.xdpi * INCH_TO_CM;
        double screenHeight = pixelCount[PIXEL_COUNT_HEIGHT_INDEX] / dm.ydpi * INCH_TO_CM;
        LogHelper.log("Screen dimensions : (" + screenWidth + ", " + screenHeight + ")");

        return new TransformInfo(
                deviceAddress,
                0,  // dummy value - will be overwritten
                screenWidth,
                screenHeight
        );
    }

    // place all devices in order, side by side, no rotation
    private TransformInfo simpleLayoutGenerator(ArrayList<TransformInfo> transformInfos) {
        double x = transformInfos.get(0).getScreenWidth();
        for (int i = 1; i < transformInfos.size(); i++) {
            TransformInfo info = transformInfos.get(i);
            info.setPosition(new DeviceRepresentation.Position((float) x, 0));
            x += info.getScreenWidth();
        }

        double viewportWidth = x;
        double viewportHeight = x / 1.78;

        return new TransformInfo(
                "viewport", -1, viewportWidth, viewportHeight);
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
