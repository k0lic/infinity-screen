package ka170130.pmu.infinityscreen.containers;

import java.io.Serializable;
import java.util.ArrayList;

public class TransformUpdate implements Serializable {

    private ArrayList<TransformInfo> transformInfoList;
    private boolean backup;

    public TransformUpdate(ArrayList<TransformInfo> transformInfoList, boolean backup) {
        this.transformInfoList = transformInfoList;
        this.backup = backup;
    }

    public ArrayList<TransformInfo> getTransformInfoList() {
        return transformInfoList;
    }

    public boolean isBackup() {
        return backup;
    }
}
