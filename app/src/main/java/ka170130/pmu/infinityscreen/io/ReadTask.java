package ka170130.pmu.infinityscreen.io;

import android.content.ContentResolver;
import android.net.Uri;
import android.util.Log;

import java.io.IOException;
import java.io.InputStream;
import java.util.concurrent.ArrayBlockingQueue;

import ka170130.pmu.infinityscreen.MainActivity;
import ka170130.pmu.infinityscreen.containers.Message;

public class ReadTask implements Runnable {

    private static final int CAPACITY = 3;

    private MainActivity mainActivity;
    private ArrayBlockingQueue<ReadCommand> queue;
    private ArrayBlockingQueue<ReadResult> resultQueue;

    public ReadTask(MainActivity mainActivity) {
        this.mainActivity = mainActivity;

        queue = new ArrayBlockingQueue<>(CAPACITY);
        resultQueue = new ArrayBlockingQueue<>(CAPACITY);
    }

    public boolean enqueue(ReadCommand command) {
        return queue.offer(command);
    }

    public ReadResult take() throws InterruptedException {
        return resultQueue.take();
    }

    @Override
    public void run() {
        ContentResolver contentResolver = mainActivity.getContentResolver();

        Uri currentUri = null;
        InputStream inputStream = null;
        int currentPosition = 0;

        int bufSize = Message.MESSAGE_MAX_SIZE - Message.JIC_BUFFER;
        byte[] buf = new byte[bufSize];

        while (true) {
            try {
                ReadCommand command = queue.take();

                // create new input stream if uri different than current is requested
                // reset is not supported, so we create new input stream if we are ahead of desired content
                if (!command.contentUri.equals(currentUri) || currentPosition > command.skip) {
                    if (inputStream != null) {
                        inputStream.close();
                    }

                    currentUri = command.contentUri;
                    inputStream = contentResolver.openInputStream(currentUri);
                    currentPosition = 0;
                }

                if (currentPosition < command.skip) {
                    inputStream.skip(command.skip - currentPosition);
                    currentPosition = command.skip;
                }

                int len = inputStream.read(buf);
                byte[] copy = null;
                if (len != -1) {
                    copy = new byte[len];
                    System.arraycopy(buf, 0, copy, 0, len);
                    currentPosition += len;

                    // TODO: remove and code something smart
                    ReadCommand nextCommand = new ReadCommand(currentUri, currentPosition);
                    queue.offer(nextCommand);
                }

                ReadResult result = new ReadResult(copy);
                resultQueue.put(result);

            } catch (InterruptedException | IOException e) {
                Log.d(MainActivity.LOG_TAG, e.toString());
                e.printStackTrace();
            }
        }
    }

    public static class ReadCommand {

        private Uri contentUri;
        private int skip;

        public ReadCommand(Uri contentUri, int skip) {
            this.contentUri = contentUri;
            this.skip = skip;
        }

        public Uri getContentUri() {
            return contentUri;
        }

        public int getSkip() {
            return skip;
        }
    }

    public static class ReadResult {

        private byte[] content;

        public ReadResult(byte[] content) {
            this.content = content;
        }

        public byte[] getContent() {
            return content;
        }
    }
}
