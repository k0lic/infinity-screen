package ka170130.pmu.infinityscreen.helpers;

import android.os.Handler;
import android.os.Looper;

public class ThreadHelper {

    private static final Handler mainHandler = new Handler(Looper.getMainLooper());

    public static void runOnMainThread(Runnable runnable) {
        mainHandler.post(runnable);
    }
}
