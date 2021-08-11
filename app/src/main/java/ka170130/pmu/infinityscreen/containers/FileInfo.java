package ka170130.pmu.infinityscreen.containers;

import android.util.Size;

import java.io.Serializable;

public class FileInfo implements Serializable {

    public enum FileType {
        IMAGE,
        VIDEO
    }

    private FileType fileType;
    private int width;
    private int height;

    // TODO: content - maybe byte[] or temp files

    public FileInfo(FileType fileType, int width, int height) {
        this.fileType = fileType;
        this.width = width;
        this.height = height;
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
}
