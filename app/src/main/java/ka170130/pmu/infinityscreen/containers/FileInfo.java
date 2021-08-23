package ka170130.pmu.infinityscreen.containers;

import android.net.Uri;
import android.util.Size;

import java.io.Serializable;

public class FileInfo implements Serializable {

    public static final int VIDEO_PACKAGE_THRESHOLD = 0;

    public enum FileType {
        IMAGE,
        VIDEO
    }

    public enum PlaybackStatus {
        PLAY,   // show image/play video
        PAUSE,  // show image/pause video
        WAIT    // show buffering dialog
    }

    private String mimeType;
    private FileType fileType;
    private long fileSize;
    private int width;
    private int height;
    private String extension;

    private int nextPackage;

    private PlaybackStatus playbackStatus;
    private String contentUri;
    private boolean downloaded;

    public FileInfo(
            String mimeType,
            FileType fileType,
            long fileSize,
            int width,
            int height,
            String extension,
            int nextPackage,
            PlaybackStatus playbackStatus,
            String contentUri,
            boolean downloaded
    ) {
        this.mimeType = mimeType;
        this.fileType = fileType;
        this.fileSize = fileSize;
        this.width = width;
        this.height = height;
        this.extension = extension;
        this.nextPackage = nextPackage;
        this.playbackStatus = playbackStatus;
        this.contentUri = contentUri;
        this.downloaded = downloaded;
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

    public String getExtension() {
        return extension;
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

    public String getContentUri() {
        return contentUri;
    }

    public void setContentUri(String contentUri) {
        this.contentUri = contentUri;
    }

    public boolean isDownloaded() {
        return downloaded;
    }

    public void setDownloaded(boolean downloaded) {
        this.downloaded = downloaded;
    }
}
