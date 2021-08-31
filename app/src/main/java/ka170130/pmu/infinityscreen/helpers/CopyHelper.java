package ka170130.pmu.infinityscreen.helpers;

import java.util.ArrayList;
import java.util.Iterator;

import ka170130.pmu.infinityscreen.containers.TransformInfo;

public class CopyHelper {
    public static TransformInfo getCopy(TransformInfo transform) {
        TransformInfo copy = null;
        if (transform != null) {
            copy = new TransformInfo(transform);
        }
        return copy;
    }

    public static ArrayList<TransformInfo> getCopy(ArrayList<TransformInfo> list) {
        ArrayList<TransformInfo> listCopy = null;

        if (list != null) {
            listCopy = new ArrayList<>();
            Iterator<TransformInfo> iterator = list.iterator();
            while (iterator.hasNext()) {
                TransformInfo next = iterator.next();
                TransformInfo copy = getCopy(next);
                listCopy.add(copy);
            }
        }

        return listCopy;
    }
}
