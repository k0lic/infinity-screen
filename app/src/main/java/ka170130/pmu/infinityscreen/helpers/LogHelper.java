package ka170130.pmu.infinityscreen.helpers;

import android.util.Log;

import java.io.File;
import java.io.IOException;

import ka170130.pmu.infinityscreen.MainActivity;
import ka170130.pmu.infinityscreen.communication.TaskManager;
import ka170130.pmu.infinityscreen.io.WriteTask;

public class LogHelper {

    private static MainActivity mainActivity;
    private static File file;

    public static void init(MainActivity mainActivity) {
        LogHelper.mainActivity = mainActivity;

        String fileName =
                "infinity-screen-logs-"
                        + System.currentTimeMillis()
                        + ".txt";
        file = new File(
                mainActivity.getApplicationContext().getExternalFilesDir("InfinityScreen"),
                fileName
        );

        File dirs = new File(file.getParent());
        if (!dirs.exists()) {
            dirs.mkdirs();
        }

        try {
            file.createNewFile();
        } catch (IOException e) {
            Log.d(MainActivity.LOG_TAG, e.toString());
            e.printStackTrace();
        }
    }

    public static void log(String message) {
        Log.d(MainActivity.LOG_TAG, message);

        try {
            String s = message + "\n";
            WriteTask.WriteCommand command =
                    new WriteTask.WriteCommand(file, false, s.getBytes());
            mainActivity.getTaskManager().getWriteTask().enqueue(command);
        } catch (InterruptedException e) {
            Log.d(MainActivity.LOG_TAG, e.toString());
            e.printStackTrace();
        }
    }

    public static void error(Exception e) {
        log(e.toString());
        e.printStackTrace();
    }
}
