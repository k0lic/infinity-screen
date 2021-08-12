package ka170130.pmu.infinityscreen.containers;

import java.io.Serializable;

public class FileContentPackage implements Serializable {

    private int fileIndex;
    private byte[] content;
    private boolean lastPackage;

    public FileContentPackage(int fileIndex, byte[] content, boolean lastPackage) {
        this.fileIndex = fileIndex;
        this.content = content;
        this.lastPackage = lastPackage;
    }

    public int getFileIndex() {
        return fileIndex;
    }

    public byte[] getContent() {
        return content;
    }

    public boolean isLastPackage() {
        return lastPackage;
    }
}
