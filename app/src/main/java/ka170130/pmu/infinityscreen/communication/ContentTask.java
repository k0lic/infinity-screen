package ka170130.pmu.infinityscreen.communication;

import android.content.ContentResolver;
import android.net.Uri;
import android.os.SystemClock;
import android.util.Log;

import androidx.lifecycle.ViewModelProvider;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Map;

import ka170130.pmu.infinityscreen.MainActivity;
import ka170130.pmu.infinityscreen.containers.FileContentPackage;
import ka170130.pmu.infinityscreen.containers.Message;
import ka170130.pmu.infinityscreen.viewmodels.MediaViewModel;

public class ContentTask implements Runnable {

    private TaskManager taskManager;
    private MediaViewModel mediaViewModel;

    public ContentTask(TaskManager taskManager) {
        this.taskManager = taskManager;

        mediaViewModel =
                new ViewModelProvider(taskManager.getMainActivity()).get(MediaViewModel.class);
    }

    @Override
    public void run() {
        SystemClock.sleep(500);

        ArrayList<String> contentUriList = mediaViewModel.getSelectedMedia().getValue();
        ContentResolver contentResolver = taskManager.getMainActivity().getContentResolver();

        Iterator<String> iterator = contentUriList.iterator();
        int fileIndex = -1;
        while (iterator.hasNext()) {
            Uri contentUri = Uri.parse(iterator.next());
            fileIndex++;

            InputStream inputStream = null;
            try {
                inputStream = contentResolver.openInputStream(contentUri);

                int bufSize = Message.MESSAGE_MAX_SIZE - Message.JIC_BUFFER;
                byte[] buf = new byte[bufSize];
                int len;

                while ((len = inputStream.read(buf)) != -1) {
                    byte[] copy = new byte[len];
                    System.arraycopy(buf, 0, copy, 0, len);

                    FileContentPackage content =
                            new FileContentPackage(fileIndex, copy, false);
                    taskManager.runBroadcastTask(Message.newContentMessage(content));
                    Log.d(MainActivity.LOG_TAG, "Sending content for file#" + fileIndex + " of length " + (copy.length / 1024f) + " KB");
                    SystemClock.sleep(1);
                }

                FileContentPackage content =
                        new FileContentPackage(fileIndex, null, true);
                taskManager.runBroadcastTask(Message.newContentMessage(content));
                Log.d(MainActivity.LOG_TAG, "Sending final content for file#" + fileIndex);
            } catch (IOException e) {
                Log.d(MainActivity.LOG_TAG, e.toString());
                e.printStackTrace();
            } finally {
                if (inputStream != null) {
                    try {
                        inputStream.close();
                    } catch (IOException e) {
                        Log.d(MainActivity.LOG_TAG, e.toString());
                        e.printStackTrace();
                    }
                }
            }
        }
    }
}
