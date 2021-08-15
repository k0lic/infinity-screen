package ka170130.pmu.infinityscreen.communication;

import android.content.ContentResolver;
import android.net.Uri;
import android.os.SystemClock;
import android.util.Log;

import androidx.lifecycle.ViewModelProvider;
import androidx.lifecycle.ViewModelStoreOwner;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.Semaphore;

import ka170130.pmu.infinityscreen.MainActivity;
import ka170130.pmu.infinityscreen.containers.FileContentPackage;
import ka170130.pmu.infinityscreen.containers.Message;
import ka170130.pmu.infinityscreen.containers.PeerInetAddressInfo;
import ka170130.pmu.infinityscreen.helpers.LogHelper;
import ka170130.pmu.infinityscreen.io.ReadTask;
import ka170130.pmu.infinityscreen.viewmodels.ConnectionViewModel;
import ka170130.pmu.infinityscreen.viewmodels.MediaViewModel;
import ka170130.pmu.infinityscreen.viewmodels.UdpViewModel;

public class ContentTask implements Runnable {

    private static final int SEMAPHORE_KEY = -83;

    private TaskManager taskManager;
    private ConnectionViewModel connectionViewModel;
    private MediaViewModel mediaViewModel;
    private UdpViewModel udpViewModel;

    public ContentTask(TaskManager taskManager, int numberOfClients) {
        this.taskManager = taskManager;

        MainActivity mainActivity = taskManager.getMainActivity();
        connectionViewModel = new ViewModelProvider(mainActivity).get(ConnectionViewModel.class);
        mediaViewModel = new ViewModelProvider(mainActivity).get(MediaViewModel.class);
        udpViewModel = new ViewModelProvider(mainActivity).get(UdpViewModel.class);
        udpViewModel.setNumberOfClients(numberOfClients);
        udpViewModel.addSemaphore(SEMAPHORE_KEY, new Semaphore(0));
    }

    @Override
    public void run() {
        SystemClock.sleep(100);

        ArrayList<String> contentUriList = mediaViewModel.getSelectedMedia().getValue();
        ContentResolver contentResolver = taskManager.getMainActivity().getContentResolver();

        LogHelper.log("contentUriList.size() = " + contentUriList.size());

        Iterator<String> iterator = contentUriList.iterator();
        int fileIndex = -1;
        while (iterator.hasNext()) {
            Uri contentUri = Uri.parse(iterator.next());
            fileIndex++;

//            InputStream inputStream = null;
            try {
//                inputStream = contentResolver.openInputStream(contentUri);
                ReadTask.ReadCommand command = new ReadTask.ReadCommand(contentUri, 0);
                taskManager.getReadTask().enqueue(command);

                int packageId = FileContentPackage.INIT_PACKAGE_ID;
                int clientPermits = 1 - udpViewModel.getNumberOfClients();
                LogHelper.log("ClientPermits: " + clientPermits);

//                int bufSize = Message.MESSAGE_MAX_SIZE - Message.JIC_BUFFER;
//                byte[] buf = new byte[bufSize];
//                int len = 0;
                ReadTask.ReadResult readResult = null;

                while (readResult == null || readResult.getContent() != null) {
                    readResult = taskManager.getReadTask().take();
//                    len = inputStream.read(buf);
                    boolean lastPackage = readResult.getContent() == null;
//                    boolean lastPackage = len == -1;

                    udpViewModel.addSemaphore(packageId, new Semaphore(clientPermits));

//                    byte[] copy = null;
//                    if (!lastPackage) {
//                        copy = new byte[len];
//                        System.arraycopy(buf, 0, copy, 0, len);
//                    }

                    FileContentPackage content =
                            new FileContentPackage(fileIndex, packageId, lastPackage, readResult.getContent());
//                            new FileContentPackage(fileIndex, packageId, lastPackage, copy);
//                    Log.d(MainActivity.LOG_TAG, "BroadcastConfirmationTask: " + packageId);
//                    taskManager.runBroadcastConfirmationTask(
//                            Message.newContentMessage(content),
//                            udpViewModel.getSemaphore(SEMAPHORE_KEY),
//                            udpViewModel.getSemaphore(packageId)
//                    );
                    taskManager.sendToAllInGroup(Message.newContentMessage(content), false);
                    LogHelper.log("Sending content for file#" + fileIndex + " of length " + ((readResult.getContent() == null ? 0 : readResult.getContent().length) / 1024f) + " KB");

                    udpViewModel.getSemaphore(packageId).acquire();
//                    udpViewModel.getSemaphore(SEMAPHORE_KEY).acquire();
//
                    udpViewModel.removeSemaphore(packageId);
                    packageId++;
                }
            } catch (IOException | InterruptedException e) {
                LogHelper.error(e);
            } finally {
//                if (inputStream != null) {
//                    try {
//                        inputStream.close();
//                    } catch (IOException e) {
//                        LogHelper.error(e);
//                    }
//                }
            }
        }

        udpViewModel.reset();
        LogHelper.log("Content task done");
    }
}
