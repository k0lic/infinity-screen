package ka170130.pmu.infinityscreen.layout;

import java.util.ArrayList;

import ka170130.pmu.infinityscreen.containers.TransformInfo;

public interface LayoutGenerator {
    public static final float DEFAULT_ASPECT_RATIO = 1.78f;                 // 16 : 9 aspect ratio

    void generate(ArrayList<TransformInfo> transformList, TransformInfo viewport);
}
