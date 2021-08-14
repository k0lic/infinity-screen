package ka170130.pmu.infinityscreen.containers;

import java.io.Serializable;

public class FileContentPackage implements Serializable {

    public static final int INIT_PACKAGE_ID = 1;

    private int fileIndex;
    private int packageId;
    private boolean lastPackage;
    private byte[] content;

    public FileContentPackage(int fileIndex, int packageId, boolean lastPackage, byte[] content) {
        this.fileIndex = fileIndex;
        this.packageId = packageId;
        this.lastPackage = lastPackage;
        this.content = content;
    }

    public int getFileIndex() {
        return fileIndex;
    }

    public int getPackageId() {
        return packageId;
    }

    public boolean isLastPackage() {
        return lastPackage;
    }

    public byte[] getContent() {
        return content;
    }
}
