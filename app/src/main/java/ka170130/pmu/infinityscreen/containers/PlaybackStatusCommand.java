package ka170130.pmu.infinityscreen.containers;

import java.io.Serializable;

public class PlaybackStatusCommand implements Serializable {

    private int fileIndex;
    private FileInfo.PlaybackStatus playbackStatus;

    public PlaybackStatusCommand(int fileIndex, FileInfo.PlaybackStatus playbackStatus) {
        this.fileIndex = fileIndex;
        this.playbackStatus = playbackStatus;
    }

    public int getFileIndex() {
        return fileIndex;
    }

    public FileInfo.PlaybackStatus getPlaybackStatus() {
        return playbackStatus;
    }
}
