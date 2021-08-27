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
import android.widget.PopupMenu;

import java.io.IOException;
import java.io.InputStream;
import java.net.InetAddress;
import java.util.ArrayList;
import java.util.Iterator;

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

    private PopupMenu popupMenu;

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

        gestureDetector = new GestureDetector(mainActivity, new GestureDetector.SimpleOnGestureListener() {
            @Override
            public boolean onDown(MotionEvent e) {
                // ignore
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
                }

                return true;
            }
        });

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

        Boolean isHost = connectionViewModel.getIsHost().getValue();

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
            popupMenu.show();
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

        // Initialize Popup Menu
        popupMenu = new PopupMenu(mainActivity, binding.menuButton);
        popupMenu.getMenuInflater().inflate(R.menu.preview_app_bar_menu, popupMenu.getMenu());
        popupMenu.setOnMenuItemClickListener(menuItem -> {
            markInteraction();

            switch (menuItem.getItemId()) {
                case R.id.option_reset_self:
                    ArrayList<TransformInfo> transformList =
                            layoutViewModel.getTransformList().getValue();
                    TransformInfo self = layoutViewModel.getSelfAuto().getValue();
                    ArrayList<TransformInfo> backupList =
                            layoutViewModel.getBackupTransformList().getValue();

                    // Get backup for own device
                    Iterator<TransformInfo> backupIt = backupList.iterator();
                    while (backupIt.hasNext()) {
                        TransformInfo next = backupIt.next();

                        if (next.getDeviceName().equals(self.getDeviceName())) {
                            self = next;
                        }
                    }

                    // Replace own device transform info with backup
                    Iterator<TransformInfo> transformIt = transformList.iterator();
                    while (transformIt.hasNext()) {
                        TransformInfo next = transformIt.next();

                        if (next.getDeviceName().equals(self.getDeviceName())) {
                            transformList.remove(next);
                            transformList.add(self);

                            layoutViewModel.setTransformList(transformList);
                            return true;
                        }
                    }
                    return true;
                case R.id.option_disconnect:
                    if (isHost) {
                        // disconnect all
                        mainActivity.getTaskManager()
                                .sendToAllInGroup(Message.newDisconnectMessage(), true);
                    } else {
                        // disconnect self
                        mainActivity.getConnectionManager().disconnect();
                    }
                    return true;
            }

            return false;
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

        acquireFullScreen();
    }

    private void showControls() {
        binding.topContainer.setVisibility(View.VISIBLE);
        binding.controlsLayout.setVisibility(View.VISIBLE);
    }

    private void markInteraction() {
        lastInteraction = System.currentTimeMillis();
    }
}