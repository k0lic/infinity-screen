package ka170130.pmu.infinityscreen.layout;

import android.graphics.Point;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Display;
import android.view.WindowManager;

import androidx.lifecycle.ViewModelProvider;

import java.net.InetAddress;
import java.util.ArrayList;

import ka170130.pmu.infinityscreen.MainActivity;
import ka170130.pmu.infinityscreen.containers.DeviceRepresentation;
import ka170130.pmu.infinityscreen.containers.Message;
import ka170130.pmu.infinityscreen.containers.TransformInfo;
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
            Log.d(MainActivity.LOG_TAG, e.toString());
            e.printStackTrace();
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
                    .runBroadcastTask(Message.newTransformListUpdateMessage(list));
        } catch (Exception e) {
            Log.d(MainActivity.LOG_TAG, e.toString());
            e.printStackTrace();
        }
    }

    public void hostViewportListener(TransformInfo viewport) {
        // Broadcast Viewport Update
        try {
            mainActivity.getTaskManager()
                    .runBroadcastTask(Message.newViewportUpdateMessage(viewport));
        } catch (Exception e) {
            Log.d(MainActivity.LOG_TAG, e.toString());
            e.printStackTrace();
        }
    }

    public TransformInfo calculateSelfTransform() {
        String deviceAddress = connectionViewModel.getSelfDevice().getValue().getDeviceAddress();

        Integer[] pixelCount = getPixelCount();

        DisplayMetrics dm = new DisplayMetrics();
        mainActivity.getWindowManager().getDefaultDisplay().getMetrics(dm);
        double screenWidth = pixelCount[PIXEL_COUNT_WIDTH_INDEX] / dm.xdpi * INCH_TO_CM;
        double screenHeight = pixelCount[PIXEL_COUNT_HEIGHT_INDEX] / dm.ydpi * INCH_TO_CM;
        Log.d(MainActivity.LOG_TAG,
                "Screen dimensions : (" + screenWidth + ", " + screenHeight + ")");

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

        try {
            Point realSize = new Point();
            Display.class
                    .getMethod("getRealSize", Point.class)
                    .invoke(display, realSize);
            pixelCount[PIXEL_COUNT_WIDTH_INDEX] = realSize.x;
            pixelCount[PIXEL_COUNT_HEIGHT_INDEX] = realSize.y;
        } catch (Exception e) {
            Log.d(MainActivity.LOG_TAG, e.toString());
            e.printStackTrace();
        }

        Log.d(
                MainActivity.LOG_TAG,
                "Display pixel count : (" +
                        pixelCount[PIXEL_COUNT_WIDTH_INDEX] + ", " +
                        pixelCount[PIXEL_COUNT_HEIGHT_INDEX] + ")"
        );
        return pixelCount;
    }
}
