package ka170130.pmu.infinityscreen.layout;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.Random;

import ka170130.pmu.infinityscreen.containers.DeviceRepresentation;
import ka170130.pmu.infinityscreen.containers.TransformInfo;
import ka170130.pmu.infinityscreen.helpers.CopyHelper;
import ka170130.pmu.infinityscreen.helpers.LogHelper;

public class MaximalRectanglesLayoutGenerator implements LayoutGenerator{

    private static final int ITERATIONS = 1000;
    private static final int BOX_UPDATE_PERIOD = 10;
    private static final double BOX_UPDATE_STEP = 1;
    private static final double INITIAL_EXPAND_FACTOR = 1.5;

    private static final double BASE_POINTS_FOR_AREA = 15;
    private static final double BASE_POINTS_FOR_OVERLAP = 35;

    private Random random;

    private ArrayList<TransformInfo> bestLayout;
    private TransformInfo bestViewport;
    private double bestScore;

    private FreeRect currentBox;

    private double totalAreaOfDevices;

    public MaximalRectanglesLayoutGenerator() {
        random = new Random();
    }

    @Override
    public void generate(ArrayList<TransformInfo> transformList, TransformInfo viewport) {
        init(transformList, viewport);

        // Perform the Maximal Rectangles Algorithm multiple times, with different boxes and shuffles
        for (int i = 0; i < ITERATIONS; i++) {
            shuffle(transformList);

            boolean success = singleIteration(transformList, viewport);
            if (!success) {
                break;
            }

            // Update best solution if current solution is better than the previously best one
            double score = getViewportScore(transformList, viewport);
            if (score > bestScore) {
                rememberBest(transformList, viewport, score);
            }

            // Update Box
            if ((i + 1) % BOX_UPDATE_PERIOD == 0) {
                double boxWidth = currentBox.getWidth() - BOX_UPDATE_STEP;
                double boxHeight = currentBox.getHeight() - BOX_UPDATE_STEP;
                currentBox = new FreeRect(0, boxHeight, 0, boxWidth);

                LogHelper.log("New Box: " + boxWidth + " " + boxHeight);
            }
        }

        // Drain solution list into return value
        transformList.clear();
        Iterator<TransformInfo> iterator = bestLayout.iterator();
        while (iterator.hasNext()) {
            TransformInfo next = iterator.next();
            transformList.add(next);
        }

        // Copy viewport solution into return value
        viewport.setScreenWidth(bestViewport.getScreenWidth());
        viewport.setScreenHeight(bestViewport.getScreenHeight());
        viewport.setOrientation(bestViewport.getOrientation());
        viewport.setPosition(bestViewport.getPosition());
    }

    private boolean singleIteration(ArrayList<TransformInfo> transformList, TransformInfo viewport) {
        ArrayList<FreeRect> freeRects = new ArrayList<>();
        freeRects.add(new FreeRect(currentBox));

        for (int i = 0; i < transformList.size(); i++) {
            TransformInfo transform = transformList.get(i);

            // Maximal Rectangles Top-Left - place rectangle on the top-most, left-most position
            // Find best Free Rectangle
            int freeRectIndex = -1;
            boolean rotate = false;
            double minBottom = 0, minRight = 0;             // dummy values, will be overwritten
            for (int j = 0; j < freeRects.size(); j++) {
                FreeRect rect = freeRects.get(j);

                // Check without rotation
                if (rect.getWidth() >= transform.getScreenWidth() &&
                        rect.getHeight() >= transform.getScreenHeight()) {
                    double bottom = rect.top + transform.getScreenHeight();
                    double right = rect.left + transform.getScreenWidth();

                    if (freeRectIndex == -1 || (bottom < minBottom || bottom == minBottom && right < minRight)) {
                        freeRectIndex = j;
                        rotate = false;
                        minBottom = bottom;
                        minRight = right;
                    }
                }

                // Check with rotation
                if (rect.getWidth() >= transform.getScreenHeight() &&
                        rect.getHeight() >= transform.getScreenWidth()) {
                    double bottom = rect.top + transform.getScreenWidth();
                    double right = rect.left + transform.getScreenHeight();

                    if (freeRectIndex == -1 || (bottom < minBottom || bottom == minBottom && right < minRight)) {
                        freeRectIndex = j;
                        rotate = true;
                        minBottom = bottom;
                        minRight = right;
                    }
                }
            }

            if (freeRectIndex == -1) {
                return false;
            }

            // Perform placement
            if (rotate) {
                transform.rotate();
            }
            DeviceRepresentation.Position newPosition = new DeviceRepresentation.Position(
                    (float) freeRects.get(freeRectIndex).left,
                    (float) freeRects.get(freeRectIndex).top
            );
            transform.setPosition(newPosition);

            // Subdivide Free Rectangles with newly placed rectangle
            FreeRect bounds = new FreeRect(transform);
            ArrayList<FreeRect> newFreeRects = new ArrayList<>();
            for (int j = 0; j < freeRects.size(); j++) {
                FreeRect rect = freeRects.get(j);

                ArrayList<FreeRect> additions = subdivide(rect, bounds);
                if (additions.size() > 0) {
                    newFreeRects.addAll(additions);
                } else {
                    newFreeRects.add(rect);
                }
            }
            freeRects = newFreeRects;

            // Remove subset Free Rectangles
            for (int j = 0; j < freeRects.size();) {
                FreeRect rect = freeRects.get(j);
                boolean removed = false;

                for (int k = 0; k < freeRects.size() && !removed; k++) {
                    if (k == j) {
                        continue;
                    }

                    // Check if one rectangle is fully contained inside the other
                    if (rect.isInside(freeRects.get(k))) {
                        freeRects.remove(j);
                        removed = true;
                    }
                }

                if (!removed) {
                    j++;
                }
            }
        }

        // Viewport should most likely end where a device ends - no reason to end in the middle of device
        // Set initial viewport
        viewport.setScreenWidth(currentBox.getWidth());
        viewport.setScreenHeight(currentBox.getHeight());
        viewport.setPosition(new DeviceRepresentation.Position());  // (0, 0)
        TransformInfo bestViewport = CopyHelper.getCopy(viewport);
        double bestScore = -1;

        // Get all Right and Bottom Edges of devices
        ArrayList<Double> rightEdges = new ArrayList<>();
        ArrayList<Double> bottomEdges = new ArrayList<>();
        Iterator<TransformInfo> iterator = transformList.iterator();
        while (iterator.hasNext()) {
            TransformInfo next = iterator.next();
            rightEdges.add(next.getPosition().x + next.getScreenWidth());
            bottomEdges.add(next.getPosition().y + next.getScreenHeight());
        }

        // Try all viewport combinations starting at (0, 0) and ending at device edge while retaining 16:9 aspect ratio
        for (int i = 0; i < rightEdges.size(); i++) {
            double width = rightEdges.get(i);
            double height = width / LayoutGenerator.DEFAULT_ASPECT_RATIO;
            viewport.setScreenWidth(width);
            viewport.setScreenHeight(height);

            double score = getViewportScore(transformList, viewport);
            if (score > bestScore) {
                bestViewport = CopyHelper.getCopy(viewport);
                bestScore = score;
            }
        }

        for (int i = 0; i < bottomEdges.size(); i++) {
            double height = bottomEdges.get(i);
            double width = height * LayoutGenerator.DEFAULT_ASPECT_RATIO;
            viewport.setScreenWidth(width);
            viewport.setScreenHeight(height);

            double score = getViewportScore(transformList, viewport);
            if (score > bestScore) {
                bestViewport = CopyHelper.getCopy(viewport);
                bestScore = score;
            }
        }

        // Save best viewport
        viewport.setScreenWidth(bestViewport.getScreenWidth());
        viewport.setScreenHeight(bestViewport.getScreenHeight());
        viewport.setPosition(bestViewport.getPosition());

        return true;
    }

    private void init(ArrayList<TransformInfo> transformList, TransformInfo viewport) {
        totalAreaOfDevices = 0;
        Iterator<TransformInfo> iterator = transformList.iterator();
        while (iterator.hasNext()) {
            TransformInfo next = iterator.next();

            double nextArea = next.getScreenWidth() * next.getScreenHeight();
            totalAreaOfDevices += nextArea;
        }

        double boxHeight = Math.sqrt(totalAreaOfDevices / LayoutGenerator.DEFAULT_ASPECT_RATIO);
        double boxWidth = boxHeight * LayoutGenerator.DEFAULT_ASPECT_RATIO;
        boxHeight = boxWidth;
        boxHeight *= INITIAL_EXPAND_FACTOR;
        boxWidth *= INITIAL_EXPAND_FACTOR;
        currentBox = new FreeRect(0, boxHeight, 0, boxWidth);

        // Use SimpleLayoutGenerator as initial solution
        LayoutGenerator backupGenerator = new SimpleLayoutGenerator();
        backupGenerator.generate(transformList, viewport);
        double score = getViewportScore(transformList, viewport);
        rememberBest(transformList, viewport, score);
    }

    private ArrayList<FreeRect> subdivide(FreeRect rect, FreeRect bounds) {
        ArrayList<FreeRect> list = new ArrayList<>();

        double overlap = rect.getOverlap(bounds);
        if (overlap == 0) {
            return list;
        }

        if (bounds.top > rect.top && bounds.top < rect.bottom) {
            list.add(new FreeRect(rect.top, bounds.top, rect.left, rect.right));
        }

        if (bounds.bottom > rect.top && bounds.bottom < rect.bottom) {
            list.add(new FreeRect(bounds.bottom, rect.bottom, rect.left, rect.right));
        }

        if (bounds.left > rect.left && bounds.left < rect.right) {
            list.add(new FreeRect(rect.top, rect.bottom, rect.left, bounds.left));
        }

        if (bounds.right > rect.left && bounds.right < rect.right) {
            list.add(new FreeRect(rect.top, rect.bottom, bounds.right, rect.right));
        }

        return list;
    }

    private void rememberBest(ArrayList<TransformInfo> transformList, TransformInfo viewport, double score) {
        bestLayout = CopyHelper.getCopy(transformList);
        bestViewport = CopyHelper.getCopy(viewport);
        bestScore = score;
    }

    private double getViewportScore(ArrayList<TransformInfo> transformList, TransformInfo viewport) {
        double area = viewport.getScreenWidth() * viewport.getScreenHeight();
        double overlap = 0;
        FreeRect viewportRect = new FreeRect(viewport);
        Iterator<TransformInfo> iterator = transformList.iterator();
        while (iterator.hasNext()) {
            TransformInfo next = iterator.next();
            FreeRect nextRect = new FreeRect(next);
            overlap += viewportRect.getOverlap(nextRect);
        }

        double pointsForArea = (overlap / totalAreaOfDevices) * BASE_POINTS_FOR_AREA;

        double overlapRatio = overlap / area;
        double pointsForOverlap = overlapRatio * overlapRatio * BASE_POINTS_FOR_OVERLAP;

        return pointsForArea + pointsForOverlap;
    }

    private void shuffle(ArrayList<TransformInfo> transformList) {
        ArrayList<TransformInfo> copy = CopyHelper.getCopy(transformList);
        transformList.clear();

        // Add back to list in random order
        while (copy.size() > 0) {
            int index = random.nextInt(copy.size());

            transformList.add(copy.get(index));
            copy.remove(index);
        }
    }

    private class FreeRect {
        double top;
        double bottom;
        double left;
        double right;

        public FreeRect(double top, double bottom, double left, double right) {
            this.top = top;
            this.bottom = bottom;
            this.left = left;
            this.right = right;
        }

        public FreeRect(FreeRect other) {
            this.top = other.top;
            this.bottom = other.bottom;
            this.left = other.left;
            this.right = other.right;
        }

        public FreeRect(TransformInfo transform) {
            this.top = transform.getPosition().y;
            this.bottom = transform.getPosition().y + transform.getScreenHeight();
            this.left = transform.getPosition().x;
            this.right = transform.getPosition().x + transform.getScreenWidth();
        }

        public double getOverlap(FreeRect other) {
            double xOverlap = Math.max(0, Math.min(right, other.right) - Math.max(left, other.left));
            double yOverlap = Math.max(0, Math.min(bottom, other.bottom) - Math.max(top, other.top));
            return xOverlap * yOverlap;
        }

        public boolean isInside(FreeRect other) {
            return left >= other.left && left <= other.right &&
                    right >= other.left && right <= other.right &&
                    top >= other.top && top <= other.bottom &&
                    bottom >= other.top && bottom <= other.bottom;
        }

        public double getWidth() {
            return right - left;
        }

        public double getHeight() {
            return bottom - top;
        }
    }
}
