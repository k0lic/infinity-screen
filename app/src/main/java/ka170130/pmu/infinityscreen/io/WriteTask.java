package ka170130.pmu.infinityscreen.io;

import android.net.Uri;
import android.util.Log;

import androidx.lifecycle.ViewModelProvider;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.InetAddress;
import java.util.ArrayList;
import java.util.concurrent.LinkedBlockingQueue;

import ka170130.pmu.infinityscreen.MainActivity;
import ka170130.pmu.infinityscreen.containers.FileOnDeviceReady;
import ka170130.pmu.infinityscreen.containers.Message;
import ka170130.pmu.infinityscreen.helpers.LogHelper;
import ka170130.pmu.infinityscreen.viewmodels.ConnectionViewModel;
import ka170130.pmu.infinityscreen.viewmodels.MediaViewModel;

public class WriteTask implements Runnable {

    private MainActivity mainActivity;
    private ConnectionViewModel connectionViewModel;
    private MediaViewModel mediaViewModel;

    private LinkedBlockingQueue<WriteCommand> queue;

    public WriteTask(MainActivity mainActivity) {
        this.mainActivity = mainActivity;
        connectionViewModel = new ViewModelProvider(mainActivity).get(ConnectionViewModel.class);
        mediaViewModel = new ViewModelProvider(mainActivity).get(MediaViewModel.class);

        queue = new LinkedBlockingQueue<>();
    }

    public void enqueue(WriteCommand command) throws InterruptedException {
        queue.put(command);
    }

    @Override
    public void run() {
        File currentFile = null;

        while (true) {
            try {
                WriteCommand command = queue.take();

                if (!command.file.equals(currentFile)) {
                    currentFile = command.file;
                    // if file was not already created - create new file
                    if (command.create) {
                        // if directory was not already created - create directory
                        File dirs = new File(currentFile.getParent());
                        if (!dirs.exists()) {
                            dirs.mkdirs();
                        }

                        currentFile.createNewFile();
                    }
                }

                // Append content to file
                OutputStream outputStream = new FileOutputStream(currentFile, true);
                outputStream.write(command.content);
                outputStream.close();
            } catch (InterruptedException | IOException e) {
                LogHelper.error(e);
            }
        }
    }

    public static class WriteCommand {

        private File file;
        private boolean create;
        private byte[] content;

        public WriteCommand(File file, boolean create, byte[] content) {
            this.file = file;
            this.create = create;
            this.content = content;
        }
    }
}
