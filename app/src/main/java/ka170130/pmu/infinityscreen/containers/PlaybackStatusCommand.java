package ka170130.pmu.infinityscreen.containers;

import java.io.Serializable;

public class PlaybackStatusCommand implements Serializable {

    public static final long NO_TIMESTAMP = 0;

    private int fileIndex;
    private FileInfo.PlaybackStatus playbackStatus;
    private long timestamp;

    public PlaybackStatusCommand(int fileIndex, FileInfo.PlaybackStatus playbackStatus) {
        this.fileIndex = fileIndex;
        this.playbackStatus = playbackStatus;
        this.timestamp = NO_TIMESTAMP;
    }

    public PlaybackStatusCommand(int fileIndex, FileInfo.PlaybackStatus playbackStatus, long timestamp) {
        this.fileIndex = fileIndex;
        this.playbackStatus = playbackStatus;
        this.timestamp = timestamp;
    }

    public int getFileIndex() {
        return fileIndex;
    }

    public FileInfo.PlaybackStatus getPlaybackStatus() {
        return playbackStatus;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }
}
