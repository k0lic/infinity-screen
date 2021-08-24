package ka170130.pmu.infinityscreen.containers;

import android.net.Uri;
import android.util.Size;

import java.io.Serializable;

public class FileInfo implements Serializable {

    public static final int VIDEO_PACKAGE_THRESHOLD = 10;

    public enum FileType {
        IMAGE,
        VIDEO
    }

    public enum PlaybackStatus {
        PLAY,           // show image/play video
        DEFERRED_PLAY,  // show image/play video at timestamp
        PAUSE,          // show image/pause video
    WAIT                // show buffering dialog
    }

    private int index;

    private String mimeType;
    private FileType fileType;
    private long fileSize;
    private int width;
    private int height;

    private int nextPackage;

    private PlaybackStatus playbackStatus;
    private long timestamp;
    private String contentUri;

    public FileInfo(
            int index,
            String mimeType,
            FileType fileType,
            long fileSize,
            int width,
            int height,
            int nextPackage,
            PlaybackStatus playbackStatus,
            String contentUri
    ) {
        this.index = index;
        this.mimeType = mimeType;
        this.fileType = fileType;
        this.fileSize = fileSize;
        this.width = width;
        this.height = height;
        this.nextPackage = nextPackage;
        this.playbackStatus = playbackStatus;
        timestamp = 0;
        this.contentUri = contentUri;
    }

    public int getIndex() {
        return index;
    }

    public String getMimeType() {
        return mimeType;
    }

    public FileType getFileType() {
        return fileType;
    }

    public long getFileSize() {
        return fileSize;
    }

    public int getWidth() {
        return width;
    }

    public int getHeight() {
        return height;
    }

    public int getNextPackage() {
        return nextPackage;
    }

    public void setNextPackage(int nextPackage) {
        this.nextPackage = nextPackage;
    }

    public PlaybackStatus getPlaybackStatus() {
        return playbackStatus;
    }

    public void setPlaybackStatus(PlaybackStatus playbackStatus) {
        this.playbackStatus = playbackStatus;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }

    public String getContentUri() {
        return contentUri;
    }

    public void setContentUri(String contentUri) {
        this.contentUri = contentUri;
    }
}
