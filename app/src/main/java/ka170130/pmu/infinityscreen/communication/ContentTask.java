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
import java.util.concurrent.Semaphore;

import ka170130.pmu.infinityscreen.MainActivity;
import ka170130.pmu.infinityscreen.containers.FileContentPackage;
import ka170130.pmu.infinityscreen.containers.Message;
import ka170130.pmu.infinityscreen.viewmodels.MediaViewModel;
import ka170130.pmu.infinityscreen.viewmodels.UdpViewModel;

public class ContentTask implements Runnable {

    private static final int SEMAPHORE_KEY = -83;

    private TaskManager taskManager;
    private MediaViewModel mediaViewModel;
    private UdpViewModel udpViewModel;

    public ContentTask(TaskManager taskManager, int numberOfClients) {
        this.taskManager = taskManager;

        mediaViewModel =
                new ViewModelProvider(taskManager.getMainActivity()).get(MediaViewModel.class);
        udpViewModel =
                new ViewModelProvider(taskManager.getMainActivity()).get(UdpViewModel.class);
        udpViewModel.setNumberOfClients(numberOfClients);
        udpViewModel.addSemaphore(SEMAPHORE_KEY, new Semaphore(0));
    }

    @Override
    public void run() {
        SystemClock.sleep(100);

        ArrayList<String> contentUriList = mediaViewModel.getSelectedMedia().getValue();
        ContentResolver contentResolver = taskManager.getMainActivity().getContentResolver();

        Log.d(MainActivity.LOG_TAG, "contentUriList.size() = " + contentUriList.size());

        Iterator<String> iterator = contentUriList.iterator();
        int fileIndex = -1;
        while (iterator.hasNext()) {
            Uri contentUri = Uri.parse(iterator.next());
            fileIndex++;

            InputStream inputStream = null;
            try {
                inputStream = contentResolver.openInputStream(contentUri);

                int packageId = FileContentPackage.INIT_PACKAGE_ID;
                int clientPermits = 1 - udpViewModel.getNumberOfClients();
                Log.d(MainActivity.LOG_TAG, "ClientPermits: " + clientPermits);

                int bufSize = Message.MESSAGE_MAX_SIZE - Message.JIC_BUFFER;
                byte[] buf = new byte[bufSize];
                int len = 0;

                while (len != -1) {
                    len = inputStream.read(buf);
                    boolean lastPackage = len == -1;

                    udpViewModel.addSemaphore(packageId, new Semaphore(clientPermits));

                    byte[] copy = null;
                    if (!lastPackage) {
                        copy = new byte[len];
                        System.arraycopy(buf, 0, copy, 0, len);
                    }

                    FileContentPackage content =
                            new FileContentPackage(fileIndex, packageId, lastPackage, copy);
                    Log.d(MainActivity.LOG_TAG, "BroadcastConfirmationTask: " + packageId);
                    taskManager.runBroadcastConfirmationTask(
                            Message.newContentMessage(content),
                            udpViewModel.getSemaphore(SEMAPHORE_KEY),
                            udpViewModel.getSemaphore(packageId)
                    );
                    Log.d(MainActivity.LOG_TAG, "Sending content for file#" + fileIndex + " of length " + ((copy == null ? 0 : copy.length) / 1024f) + " KB");

                    udpViewModel.getSemaphore(SEMAPHORE_KEY).acquire();

                    udpViewModel.removeSemaphore(packageId);
                    packageId++;
                }
            } catch (IOException | InterruptedException e) {
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

        udpViewModel.reset();
        Log.d(MainActivity.LOG_TAG, "Content task done");
    }
}
