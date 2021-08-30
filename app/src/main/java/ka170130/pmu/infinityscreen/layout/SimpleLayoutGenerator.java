package ka170130.pmu.infinityscreen.layout;

import java.util.ArrayList;

import ka170130.pmu.infinityscreen.containers.DeviceRepresentation;
import ka170130.pmu.infinityscreen.containers.TransformInfo;

public class SimpleLayoutGenerator implements LayoutGenerator {

    // place all devices in order, side by side, no rotation
    @Override
    public void generate(
            ArrayList<TransformInfo> transformList,
            TransformInfo viewport
    ) {
        double x = transformList.get(0).getScreenWidth();
        for (int i = 1; i < transformList.size(); i++) {
            TransformInfo info = transformList.get(i);
            info.setPosition(new DeviceRepresentation.Position((float) x, 0));
            x += info.getScreenWidth();
        }

        double viewportWidth = x;
        double viewportHeight = x / DEFAULT_ASPECT_RATIO;
        viewport.setScreenWidth(viewportWidth);
        viewport.setScreenHeight(viewportHeight);
        viewport.setOrientation(TransformInfo.Orientation.LANDSCAPE);
        viewport.getPosition().x = 0;
        viewport.getPosition().y = 0;
    }
}
