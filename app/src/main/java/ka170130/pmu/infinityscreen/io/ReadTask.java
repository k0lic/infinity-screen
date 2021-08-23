package ka170130.pmu.infinityscreen.io;

import android.content.ContentResolver;
import android.net.Uri;
import android.util.Log;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.Semaphore;

import ka170130.pmu.infinityscreen.MainActivity;
import ka170130.pmu.infinityscreen.containers.FileContentPackage;
import ka170130.pmu.infinityscreen.containers.Message;
import ka170130.pmu.infinityscreen.helpers.LogHelper;

public class ReadTask implements Runnable {

    private static final int CAPACITY = 3;

    private MainActivity mainActivity;

    private ArrayList<Source> sources;
    private int focus;

    private Semaphore semaphore;
    private ArrayBlockingQueue<ReadResult> resultQueue;

    public ReadTask(MainActivity mainActivity) {
        this.mainActivity = mainActivity;

        sources = new ArrayList<>();
        focus = 0;

        semaphore = new Semaphore(0);
        resultQueue = new ArrayBlockingQueue<>(CAPACITY);
    }

    public void reset() {
        resultQueue.clear();
        sources.clear();
        focus = 0;
    }

    public void registerSources(List<MainActivity.Pair<Integer, String>> imageList) {
        Iterator<MainActivity.Pair<Integer, String>> iterator = imageList.iterator();

        while (iterator.hasNext()) {
            MainActivity.Pair<Integer, String> next = iterator.next();
            int fileIndex = next.first;
            Uri imageUri = Uri.parse(next.second);

            sources.add(new Source(
                    imageUri,
                    fileIndex,
                    0,
                    FileContentPackage.INIT_PACKAGE_ID,
                    false
            ));
        }

        semaphore.release();
    }

    public ArrayList<Source> getSources() {
        return sources;
    }

    public ArrayBlockingQueue<ReadResult> getResultQueue() {
        return resultQueue;
    }

    public void changeFocus(int focus) {
        this.focus = focus;
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
                // if no sources are left to read, block thread
                while (sources.size() == 0) {
                    semaphore.acquire();
                }

                // use focused source
                int index = 0;
                while (index < sources.size() && sources.get(index).fileIndex != focus) {
                    index++;
                }
                if (index == sources.size() || sources.get(index).isDone()) {
                    index = 0;
                }

                // get first source that is not yet done
                while (index < sources.size() && sources.get(index).isDone()) {
                    index++;
                }

                // if all sources are done then clear the sources list and block on the semaphore
                if (index == sources.size()) {
                    sources.clear();
                    continue;
                }

                Source source = sources.get(index);

                // open new input stream if necessary
                if (!source.contentUri.equals(currentUri) || currentPosition > source.position) {
                    if (inputStream != null) {
                        inputStream.close();
                    }

                    currentUri = source.contentUri;
                    inputStream = contentResolver.openInputStream(currentUri);
                    currentPosition = 0;
                }

                // skip to requested position
                if (currentPosition < source.position) {
                    inputStream.skip(source.position - currentPosition);
                    currentPosition = source.position;
                }

                int len = inputStream.read(buf);
                byte[] copy = null;
                if (len != -1) {
                    copy = new byte[len];
                    System.arraycopy(buf, 0, copy, 0, len);
                    currentPosition += len;
                }

                // forward read content
                ReadResult result = new ReadResult(source.fileIndex, source.packageId, copy);
                resultQueue.put(result);

                // update source
                source.setPosition(currentPosition);
                source.setPackageId(source.packageId + 1);
                if (len == -1) {
                    source.setDone(true);
                }
            } catch (InterruptedException | IOException e) {
                LogHelper.error(e);
            }
        }
    }

    public static class Source {

        private Uri contentUri;
        private int fileIndex;
        private int position;
        private int packageId;
        private boolean done;

        public Source(Uri contentUri, int fileIndex, int position, int packageId, boolean done) {
            this.contentUri = contentUri;
            this.fileIndex = fileIndex;
            this.position = position;
            this.packageId = packageId;
            this.done = done;
        }

        public Uri getContentUri() {
            return contentUri;
        }

        public int getFileIndex() {
            return fileIndex;
        }

        public int getPosition() {
            return position;
        }

        public void setPosition(int position) {
            this.position = position;
        }

        public int getPackageId() {
            return packageId;
        }

        public void setPackageId(int packageId) {
            this.packageId = packageId;
        }

        public boolean isDone() {
            return done;
        }

        public void setDone(boolean done) {
            this.done = done;
        }
    }

    public static class ReadResult {

        private int fileIndex;
        private int packageId;
        private byte[] content;

        public ReadResult(int fileIndex, int packageId, byte[] content) {
            this.fileIndex = fileIndex;
            this.packageId = packageId;
            this.content = content;
        }

        public int getFileIndex() {
            return fileIndex;
        }

        public int getPackageId() {
            return packageId;
        }

        public byte[] getContent() {
            return content;
        }
    }
}
