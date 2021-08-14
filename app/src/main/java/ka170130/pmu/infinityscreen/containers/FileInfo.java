package ka170130.pmu.infinityscreen.containers;

import android.net.Uri;
import android.util.Size;

import java.io.Serializable;

public class FileInfo implements Serializable {

    public enum FileType {
        IMAGE,
        VIDEO
    }

    public enum PlaybackStatus {
        PLAY,   // show image/play video
        PAUSE,  // show image/pause video
        WAIT    // show buffering dialog
    }

    private FileType fileType;
    private int width;
    private int height;
    private String extension;

    private int nextPackage;

    private PlaybackStatus playbackStatus;
    private String contentUri;

    public FileInfo(
            FileType fileType,
            int width,
            int height,
            String extension,
            PlaybackStatus playbackStatus,
            String contentUri
    ) {
        this.fileType = fileType;
        this.width = width;
        this.height = height;
        this.extension = extension;
        this.nextPackage = FileContentPackage.INIT_PACKAGE_ID;
        this.playbackStatus = playbackStatus;
        this.contentUri = contentUri;
    }

    public FileType getFileType() {
        return fileType;
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
}