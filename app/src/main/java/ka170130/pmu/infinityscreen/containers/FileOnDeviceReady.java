package ka170130.pmu.infinityscreen.containers;

import java.io.Serializable;

public class FileOnDeviceReady implements Serializable {

    private int deviceIndex;
    private int fileIndex;

    public FileOnDeviceReady(int deviceIndex, int fileIndex) {
        this.deviceIndex = deviceIndex;
        this.fileIndex = fileIndex;
    }

    public int getDeviceIndex() {
        return deviceIndex;
    }

    public int getFileIndex() {
        return fileIndex;
    }
}
