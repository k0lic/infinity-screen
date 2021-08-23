package ka170130.pmu.infinityscreen.communication;

import android.content.ContentResolver;
import android.net.Uri;
import android.os.SystemClock;
import android.util.Log;
import android.util.Pair;

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
import ka170130.pmu.infinityscreen.containers.FileInfo;
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
        ReadTask readTask = taskManager.getReadTask();
        readTask.reset();

        // get all content of IMAGE type
        ArrayList<String> contentUriList = mediaViewModel.getSelectedMedia().getValue();
        ArrayList<FileInfo> fileInfos = mediaViewModel.getFileInfoList().getValue();
        LogHelper.log("contentUriList.size() = " + contentUriList.size());
        LogHelper.log("fileInfos.size() = " + fileInfos.size());

        ArrayList<MainActivity.Pair<Integer, String>> imageList = new ArrayList<>();
        for (int i = 0; i < contentUriList.size() && i < fileInfos.size(); i++) {
            if (fileInfos.get(i).getFileType() == FileInfo.FileType.IMAGE) {
                imageList.add(new MainActivity.Pair<>(i, contentUriList.get(i)));
            }
        }
        readTask.registerSources(imageList);
        LogHelper.log("imageList.size() = " + imageList.size());

        int clientPermits = 1 - udpViewModel.getNumberOfClients();
        LogHelper.log("ClientPermits: " + clientPermits);

        boolean run = readTask.getSources().size() > 0 || readTask.getResultQueue().size() > 0;
        while (run) {
            try {
                ReadTask.ReadResult readResult = readTask.take();
                boolean lastPackage = readResult.getContent() == null;

                udpViewModel.addSemaphore(readResult.getPackageId(), new Semaphore(clientPermits));

                FileContentPackage content = new FileContentPackage(
                        readResult.getFileIndex(),
                        readResult.getPackageId(),
                        lastPackage,
                        readResult.getContent()
                );
                taskManager.sendToAllInGroup(Message.newContentMessage(content), false);
                LogHelper.log("Sending content for file#" + readResult.getFileIndex() + " of length " + ((readResult.getContent() == null ? 0 : readResult.getContent().length) / 1024f) + " KB");

                udpViewModel.getSemaphore(readResult.getPackageId()).acquire();

                udpViewModel.removeSemaphore(readResult.getPackageId());
            } catch (IOException | InterruptedException e) {
                LogHelper.error(e);
            }

            run = readTask.getSources().size() > 0 || readTask.getResultQueue().size() > 0;
        }

        udpViewModel.reset();
        LogHelper.log("Content task done");
    }
}
