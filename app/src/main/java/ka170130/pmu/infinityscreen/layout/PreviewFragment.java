package ka170130.pmu.infinityscreen.layout;

import android.content.res.AssetFileDescriptor;
import android.graphics.Matrix;
import android.graphics.Rect;
import android.graphics.SurfaceTexture;
import android.graphics.drawable.Drawable;
import android.media.MediaPlayer;
import android.os.Build;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;

import android.os.Handler;
import android.util.Log;
import android.view.GestureDetector;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.view.Surface;
import android.view.TextureView;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;

import java.io.IOException;
import java.io.InputStream;
import java.net.InetAddress;
import java.util.ArrayList;

import ka170130.pmu.infinityscreen.MainActivity;
import ka170130.pmu.infinityscreen.R;
import ka170130.pmu.infinityscreen.containers.Message;
import ka170130.pmu.infinityscreen.containers.TransformInfo;
import ka170130.pmu.infinityscreen.containers.TransformUpdate;
import ka170130.pmu.infinityscreen.databinding.FragmentHomeBinding;
import ka170130.pmu.infinityscreen.databinding.FragmentPreviewBinding;
import ka170130.pmu.infinityscreen.helpers.LogHelper;
import ka170130.pmu.infinityscreen.helpers.StateChangeHelper;
import ka170130.pmu.infinityscreen.play.FullScreenFragment;
import ka170130.pmu.infinityscreen.play.PlayFragment;
import ka170130.pmu.infinityscreen.viewmodels.LayoutViewModel;
import ka170130.pmu.infinityscreen.viewmodels.MediaViewModel;
import ka170130.pmu.infinityscreen.viewmodels.StateViewModel;

public class PreviewFragment extends FullScreenFragment {

    private static final String[] ASSETS = {
            "lines-texture-diagonal.jpg",
            "DonutsForBreakfast4.png",
            "mountains.jpg"
    };

    private FragmentPreviewBinding binding;
    private StateViewModel stateViewModel;
    private LayoutViewModel layoutViewModel;
    private MediaViewModel mediaViewModel;

    private LayoutManager layoutManager;

    private GestureDetector gestureDetector;
    private ScaleGestureDetector scaleGestureDetector;

    private TransformInfo currentTransform;
    private int currentDrawableWidth = 0;
    private int currentDrawableHeight = 0;

    private long lastInteraction = 0;

    private Handler handler;
    private Runnable autoHideControls = new Runnable() {
        @Override
        public void run() {
            // Check if controls are visible
            if (binding.topContainer.getVisibility() == View.VISIBLE) {
                // Check for inactivity
                if (System.currentTimeMillis() - lastInteraction > PlayFragment.CONTROLS_TIMEOUT) {
                    // hide controls
                    hideControls();
                }
            }

            handler.postDelayed(autoHideControls, PlayFragment.AUTO_HIDE_CHECK_INTERVAL);
        }
    };

    public PreviewFragment() {
        // Required empty public constructor
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        stateViewModel = new ViewModelProvider(mainActivity).get(StateViewModel.class);
        layoutViewModel = new ViewModelProvider(mainActivity).get(LayoutViewModel.class);
        mediaViewModel = new ViewModelProvider(mainActivity).get(MediaViewModel.class);

        layoutManager = mainActivity.getLayoutManager();

        // TODO
        gestureDetector = new GestureDetector(mainActivity, new GestureDetector.SimpleOnGestureListener() {
            @Override
            public boolean onDown(MotionEvent e) {
                // ignore?
                return true;
            }

            // Toggle Controls visibility
            @Override
            public boolean onSingleTapUp(MotionEvent event) {
                if (binding.topContainer.getVisibility() == View.VISIBLE) {
                    hideControls();
                } else {
                    showControls();
                }
                return true;
            }

            // Translate/Zoom image
            @Override
            public boolean onScroll(MotionEvent e1, MotionEvent e2, float distanceX, float distanceY) {
                TransformInfo self = layoutViewModel.getSelfAuto().getValue();

                // e1 is DownEvent, e2 is MoveEvent
                if (e2.getPointerCount() == 1) {
                    // Translate Scroll
                    layoutManager.performTranslateEvent(self, distanceX, distanceY);
                    layoutViewModel.updateTransform(self);
//                    layoutManager.updateTransformList(self);
                }

                return true;
            }
        });

//                new GestureDetector.OnGestureListener() {
//            @Override
//            public boolean onDown(MotionEvent event) {
//                LogHelper.log("onDown: " + event.toString());
//                return true;
//            }
//
//            @Override
//            public boolean onFling(MotionEvent event1, MotionEvent event2,
//                                   float velocityX, float velocityY) {
//                LogHelper.log("onFling: " + event1.toString() + event2.toString());
//                return true;
//            }
//
//            @Override
//            public void onLongPress(MotionEvent event) {
//                LogHelper.log("onLongPress: " + event.toString());
//            }
//
//            @Override
//            public boolean onScroll(MotionEvent event1, MotionEvent event2, float distanceX,
//                                    float distanceY) {
//                LogHelper.log("onScroll: " + event1.toString() + event2.toString());
//                return true;
//            }
//
//            @Override
//            public void onShowPress(MotionEvent event) {
//                LogHelper.log("onShowPress: " + event.toString());
//            }
//
//            // Toggle Controls visibility
//            @Override
//            public boolean onSingleTapUp(MotionEvent event) {
//                LogHelper.log("onSingleTapUp: " + event.toString());
//                return true;
//            }
//        });

        scaleGestureDetector = new ScaleGestureDetector(mainActivity, new ScaleGestureDetector.OnScaleGestureListener() {
            @Override
            public boolean onScale(ScaleGestureDetector detector) {
                TransformInfo self = layoutViewModel.getSelfAuto().getValue();

                layoutManager.performZoomEvent(
                        self,
                        detector.getScaleFactor(),
                        detector.getFocusX(),
                        detector.getFocusY()
                );
                layoutViewModel.updateTransform(self);

                return true;
            }

            @Override
            public boolean onScaleBegin(ScaleGestureDetector detector) {
                // ignore
                return true;
            }

            @Override
            public void onScaleEnd(ScaleGestureDetector detector) {
                // ignore
            }
        });

        handler = new Handler(mainActivity.getMainLooper());
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        binding = FragmentPreviewBinding.inflate(inflater, container, false);

        // Previous Button
        binding.previousButton.setOnClickListener(view -> {
            markInteraction();
            handleIndexChange(ASSETS.length - 1);
        });

        // Next Button
        binding.nextButton.setOnClickListener(view -> {
            markInteraction();
            handleIndexChange(1);
        });

        // Back Button
        binding.backButton.setOnClickListener(view -> {
            markInteraction();

            Rect rect = binding.imageView.getDrawable().copyBounds();
            LogHelper.log("RECT: " + rect.left + " " + rect.right + " " + rect.top + " " + rect.bottom);

            StateChangeHelper.requestStateChange(
                    mainActivity, connectionViewModel, StateViewModel.AppState.LAYOUT);
        });

        // Menu Button
        binding.menuButton.setOnClickListener(view -> {
            markInteraction();
            // TODO: pop up menu?
        });

        // React to Image View touch events
        binding.imageView.setOnTouchListener((v, event) -> {
            markInteraction();

            boolean touch = gestureDetector.onTouchEvent(event);
            touch = scaleGestureDetector.onTouchEvent(event) || touch;
            return touch;
        });

        // Listen for File Index change
        mediaViewModel.getCurrentFileIndex().observe(getViewLifecycleOwner(), index -> {
            try (InputStream inputStream = mainActivity.getAssets().open(getAsset(index))) {
                // set content
                Drawable drawable = Drawable.createFromStream(inputStream, null);
                binding.imageView.setImageDrawable(drawable);

                // set matrix
                TransformInfo self = layoutViewModel.getSelfAuto().getValue();
                currentTransform = new TransformInfo(self);
                TransformInfo viewport = layoutViewModel.getViewport().getValue();

                if (self == null || viewport == null) {
                    return;
                }

                int drawableWidth = drawable.getIntrinsicWidth();
                currentDrawableWidth = drawableWidth;
                int drawableHeight = drawable.getIntrinsicHeight();
                currentDrawableHeight = drawableHeight;

                Matrix matrix = layoutManager.getMatrix(self, viewport, drawableWidth, drawableHeight);
                binding.imageView.setImageMatrix(matrix);
            } catch (IOException e) {
                LogHelper.error(e);
            }
        });

        // Listen for Transform Info changes
        layoutViewModel.getSelfAuto().observe(getViewLifecycleOwner(), transform -> {
            if (currentDrawableWidth == 0 || currentDrawableHeight == 0) {
                // skip
                return;
            }

            // Check if relevant data has changed
            if (currentTransform == null ||
                    transform.getScreenWidth() != currentTransform.getScreenWidth() ||
                    transform.getScreenHeight() != currentTransform.getScreenHeight() ||
                    transform.getPosition().x != currentTransform.getPosition().x ||
                    transform.getPosition().y != currentTransform.getPosition().y
            ) {
                currentTransform = new TransformInfo(transform);

                // set matrix
                TransformInfo viewport = layoutViewModel.getViewport().getValue();

                Matrix matrix = layoutManager.getMatrix(
                        transform, viewport, currentDrawableWidth, currentDrawableHeight);
                binding.imageView.setImageMatrix(matrix);
            }
        });

        // Initiate Auto Hide Controls
        handler.post(autoHideControls);

        // Initialize Transform Update Count
        layoutViewModel.setUpdateCount(0);

        // Listen for App State change
        stateViewModel.getState().observe(getViewLifecycleOwner(), state -> {
            if (state == StateViewModel.AppState.LAYOUT) {
                // Report updated Own Transform to host
                try {
                    TransformInfo self = layoutViewModel.getSelfAuto().getValue();

                    InetAddress hostAddress =
                            connectionViewModel.getHostDevice().getValue().getInetAddress();
                    Message message = Message.newTransformUpdateMessage(self);
                    mainActivity.getTaskManager().runSenderTask(hostAddress, message);
                } catch (IOException e) {
                    LogHelper.error(e);
                }

                navController.navigate(PreviewFragmentDirections.actionPop());
            }
        });

        return  binding.getRoot();
    }

    @Override
    public void onResume() {
        super.onResume();

        // sync App State - necessary for the Back button to work
//        StateChangeHelper.requestStateChange(
//                mainActivity, connectionViewModel, StateViewModel.AppState.PREVIEW);
    }

    private String getAsset(int index) {
        return ASSETS[index];
    }

    private void handleIndexChange(int delta) {
        Boolean isHost = connectionViewModel.getIsHost().getValue();

        Integer index = mediaViewModel.getCurrentFileIndex().getValue();
        index = (index + delta) % ASSETS.length;

        try {
            if (isHost) {
                mainActivity.getTaskManager()
                        .sendToAllInGroup(Message.newFileIndexUpdateMessage(index), true);
            } else {
                InetAddress hostAddress =
                        connectionViewModel.getHostDevice().getValue().getInetAddress();
                mainActivity.getTaskManager().runSenderTask(
                        hostAddress, Message.newFileIndexUpdateRequestMessage(index));
            }
        } catch (IOException e) {
            LogHelper.error(e);
        }
    }

    private void hideControls() {
        binding.topContainer.setVisibility(View.INVISIBLE);
        binding.controlsLayout.setVisibility(View.INVISIBLE);
    }

    private void showControls() {
        binding.topContainer.setVisibility(View.VISIBLE);
        binding.controlsLayout.setVisibility(View.VISIBLE);
    }

    private void markInteraction() {
        lastInteraction = System.currentTimeMillis();
    }
}