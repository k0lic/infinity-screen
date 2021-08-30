package ka170130.pmu.infinityscreen.play;

import android.content.ContentResolver;
import android.content.Context;
import android.graphics.Matrix;
import android.graphics.SurfaceTexture;
import android.graphics.drawable.Drawable;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.ViewModelProvider;

import android.os.Handler;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.Surface;
import android.view.TextureView;
import android.view.View;
import android.view.ViewGroup;
import android.widget.PopupMenu;
import android.widget.Toast;

import com.google.android.exoplayer2.MediaItem;
import com.google.android.exoplayer2.Player;
import com.google.android.exoplayer2.SimpleExoPlayer;
import com.google.android.material.slider.Slider;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.InetAddress;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.TreeSet;

import ka170130.pmu.infinityscreen.R;
import ka170130.pmu.infinityscreen.communication.TaskManager;
import ka170130.pmu.infinityscreen.containers.AccelerometerInfo;
import ka170130.pmu.infinityscreen.containers.DeviceRepresentation;
import ka170130.pmu.infinityscreen.containers.FileInfo;
import ka170130.pmu.infinityscreen.containers.Message;
import ka170130.pmu.infinityscreen.containers.PlaybackStatusCommand;
import ka170130.pmu.infinityscreen.containers.TransformInfo;
import ka170130.pmu.infinityscreen.databinding.FragmentPlayBinding;
import ka170130.pmu.infinityscreen.helpers.LogHelper;
import ka170130.pmu.infinityscreen.helpers.StateChangeHelper;
import ka170130.pmu.infinityscreen.helpers.TimeHelper;
import ka170130.pmu.infinityscreen.viewmodels.LayoutViewModel;
import ka170130.pmu.infinityscreen.viewmodels.MediaViewModel;
import ka170130.pmu.infinityscreen.viewmodels.StateViewModel;

public class PlayFragment extends FullScreenFragment {

    public static final long CONTROLS_TIMEOUT = 10_000;
    public static final long AUTO_HIDE_CHECK_INTERVAL = 500;

    private static final long MINIMUM_DEFERRED_DELAY = 10;
    private static final long SEEK_TO_DELAY = 100;
    private static final long SLIDER_UPDATE_DELAY = 250;

    private static final float SPEED_THRESHOLD = 0.1f;
    private static final float SPEED_DECAY = 0.01f;
    private static final float MOVEMENT_FACTOR = 0.0001f;

    private enum MediaPlayerState {
        IDLE,
        PREPARING,
        PLAYING,
        PAUSED,
        STOPPED,
        COMPLETED
    }

    private FragmentPlayBinding binding;
    private StateViewModel stateViewModel;
    private LayoutViewModel layoutViewModel;
    private MediaViewModel mediaViewModel;

    private PopupMenu popupMenu;

    private SimpleExoPlayer exoPlayer;

    private String currentContent;
    private int currentImageWidth;
    private int currentImageHeight;
    private MediaPlayerState mediaPlayerState;

    private long lastInteraction = 0;
    private boolean durationSet = false;

    private AccelerometerInfo accelerometerInfo;

    private SensorManager sensorManager;
    private Sensor accelerometer;
    private SensorEventListener accelerometerListener = new SensorEventListener() {
        @Override
        public void onSensorChanged(SensorEvent event) {
            // Get timestamp
            long now = System.currentTimeMillis();

            // Fetch RAW values
            float x = event.values[0];
            float y = -event.values[1];

            // Calibrate
            if (accelerometerInfo.calibrationLeft > 0) {
                accelerometerInfo.offset[0] += x;
                accelerometerInfo.offset[1] += y;

                accelerometerInfo.calibrationLeft--;
                accelerometerInfo.calibrationCount++;

                if (accelerometerInfo.calibrationLeft == 0) {
                    accelerometerInfo.offset[0] /= accelerometerInfo.calibrationCount;
                    accelerometerInfo.offset[1] /= accelerometerInfo.calibrationCount;
                }

                accelerometerInfo.lastMovement = now;
                return;
            }

            // Filter values
            x -= accelerometerInfo.offset[0];
            y -= accelerometerInfo.offset[1];

            accelerometerInfo.xAccel = x * AccelerometerInfo.FILTER_FACTOR +
                    accelerometerInfo.xAccel * (1 - AccelerometerInfo.FILTER_FACTOR);
            accelerometerInfo.yAccel = y * AccelerometerInfo.FILTER_FACTOR +
                    accelerometerInfo.yAccel * (1 - AccelerometerInfo.FILTER_FACTOR);

            // Fetch transform information
            TransformInfo transform = layoutViewModel.getSelfAuto().getValue();
            TransformInfo viewport = layoutViewModel.getViewport().getValue();

            // Perform translation
            mainActivity.getLayoutManager().performAccelerometerEvent(
                    transform,
                    viewport,
                    accelerometerInfo,
                    now
            );

            // Perform transform update
            layoutViewModel.updateTransform(transform, false);
        }

        @Override
        public void onAccuracyChanged(Sensor sensor, int accuracy) {
            // ignore
        }
    };

    private Handler handler;
    private Runnable autoHideControls = new Runnable() {
        @Override
        public void run() {
            // Check if controls are visible
            if (binding.menuButton.getVisibility() == View.VISIBLE) {
                // Check for inactivity
                if (System.currentTimeMillis() - lastInteraction > CONTROLS_TIMEOUT) {
                    // hide controls
                    hideControls();
                }
            }

            handler.postDelayed(autoHideControls, AUTO_HIDE_CHECK_INTERVAL);
        }
    };
    private Runnable deferredPlay = new Runnable() {
        @Override
        public void run() {
            exoPlayer.play();

            // Update Media Player State
            mediaPlayerState = MediaPlayerState.PLAYING;
            LogHelper.log("PREPARING to PLAYING");

            // Set Pause Drawable
            binding.playButton.setImageResource(R.drawable.outline_pause_24);
        }
    };
    private Runnable deferredSeekToRequest = new Runnable() {
        @Override
        public void run() {
            long position = Math.round(binding.slider.getValue());
            requestSeekTo(position, false);
        }
    };
    private Runnable updateSliderAndCurrentTimeLabel = new Runnable() {
        @Override
        public void run() {
            if (durationSet) {
                // Update Slider and Current Time Label
                long position = exoPlayer.getCurrentPosition();

                if (position >= binding.slider.getValueFrom()
                        && position <= binding.slider.getValueTo()
                ) {
                    binding.slider.setValue(position);
                }

                binding.currentTime.setText(TimeHelper.format(position));
            }

            handler.postDelayed(updateSliderAndCurrentTimeLabel, SLIDER_UPDATE_DELAY);
        }
    };

    public PlayFragment() {
        // Required empty public constructor
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        stateViewModel = new ViewModelProvider(mainActivity).get(StateViewModel.class);
        layoutViewModel = new ViewModelProvider(mainActivity).get(LayoutViewModel.class);
        mediaViewModel = new ViewModelProvider(mainActivity).get(MediaViewModel.class);

//        mediaPlayer = new MediaPlayer();
//        mediaPlayer.setLooping(false);
//        mediaPlayer.setOnCompletionListener(mp -> {
//            mediaPlayerState = MediaPlayerState.COMPLETED;
//        });
        exoPlayer = new SimpleExoPlayer.Builder(mainActivity).build();

        currentContent = null;
        mediaPlayerState = MediaPlayerState.IDLE;

        accelerometerInfo = new AccelerometerInfo();

        sensorManager = (SensorManager) mainActivity.getSystemService(Context.SENSOR_SERVICE);
        accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_LINEAR_ACCELERATION);

        handler = new Handler(mainActivity.getMainLooper());
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        binding = FragmentPlayBinding.inflate(inflater, container, false);

        Boolean isHost = connectionViewModel.getIsHost().getValue();

        // Play/Pause button
        binding.playButton.setOnClickListener(view -> {
            if (mediaPlayerState == MediaPlayerState.PLAYING) {
                requestPause();
            } else {
                requestPlay();
            }

            markInteraction();
        });

        // Previous Button
        binding.previousButton.setOnClickListener(view -> {
            handleIndexChange(-1);
            markInteraction();
        });

        // Next Button
        binding.nextButton.setOnClickListener(view -> {
            handleIndexChange(1);
            markInteraction();
        });

        // Slider
        binding.slider.addOnSliderTouchListener(new Slider.OnSliderTouchListener() {
            @Override
            public void onStartTrackingTouch(@NonNull Slider slider) {
                // Pause if not already paused
                if (mediaPlayerState == MediaPlayerState.PLAYING
                        || mediaPlayerState == MediaPlayerState.PREPARING
                ) {
                    requestPause();
                }
            }

            @Override
            public void onStopTrackingTouch(@NonNull Slider slider) {
                // ignore
            }
        });

        binding.slider.addOnChangeListener(new Slider.OnChangeListener() {
            @Override
            public void onValueChange(@NonNull Slider slider, float value, boolean fromUser) {
                // Only react to changes made by user - ignore changes made by the video progressing
                if (fromUser) {
                    handler.removeCallbacks(deferredSeekToRequest);

                    // Only notify group about Seek To Change after some time has passed without new value
                    // This is to prevent from spamming changes when the slider is being dragged
                    handler.postDelayed(deferredSeekToRequest, SEEK_TO_DELAY);

                    // Set current position
                    long position = Math.round(value);
                    binding.currentTime.setText(TimeHelper.format(position));

                    // Seek now only on this device
                    exoPlayer.seekTo(position);
                }
            }
        });

        // Menu Button
        binding.menuButton.setOnClickListener(view -> {
            markInteraction();

            Boolean accelerometerActive = mediaViewModel.getAccelerometerActive().getValue();
            Menu menu = popupMenu.getMenu();
            if (accelerometerActive) {
                menu.findItem(R.id.option_activate_accelerometer).setVisible(false);
                menu.findItem(R.id.option_deactivate_accelerometer).setVisible(true);
                menu.findItem(R.id.option_reset_self).setVisible(false);
            } else {
                menu.findItem(R.id.option_activate_accelerometer).setVisible(true);
                menu.findItem(R.id.option_deactivate_accelerometer).setVisible(false);
                menu.findItem(R.id.option_reset_self).setVisible(true);
            }

            popupMenu.show();
        });

        // Block accidental background clicks
        binding.controlsLayout.setOnClickListener(view -> markInteraction());

        // Background click
        binding.frameLayout.setOnClickListener(view -> {
            // toggle control buttons visibility
            int currentVisibility = binding.menuButton.getVisibility();
            if (currentVisibility == View.VISIBLE) {
                hideControls();
            } else {
                showControls();
            }

            markInteraction();
        });

        // Listen for Accelerometer activation/deactivation
        mediaViewModel.getAccelerometerActive().observe(getViewLifecycleOwner(), active -> {
            if (active) {
                // Activate Accelerometer Listener
                activateAccelerometerListener();
            } else {
                // Deactivate Accelerometer Listener
                deactivateAccelerometerListener();
            }
        });

        // Listen for File Info change
        mediaViewModel.getCurrentFileInfo().observe(getViewLifecycleOwner(), fileInfo -> {
            if (fileInfo == null) {
                return;
            }

            LogHelper.log("Active FileInfo: " + fileInfo.getFileType().toString()
                    + " " + fileInfo.getWidth()
                    + " " + fileInfo.getHeight()
                    + " " + fileInfo.getPlaybackStatus()
                    + " " + fileInfo.getContentUri()
            );

            switch (fileInfo.getFileType()) {
                case IMAGE:
                    handleImage(fileInfo);
                    break;
                case VIDEO:
                    handleVideo(fileInfo);
                    break;
            }
        });

        // Listen for Seek To Position change
        mediaViewModel.getCurrentTimestamp().observe(getViewLifecycleOwner(), timestamp -> {
            exoPlayer.seekTo(timestamp);

            // Change state from COMPLETED
            if (mediaPlayerState == MediaPlayerState.COMPLETED) {
                LogHelper.log("COMPLETED to PAUSED");
                mediaPlayerState = MediaPlayerState.PAUSED;
            }
        });

        // Listen for File Index change
        mediaViewModel.getCurrentFileIndex().observe(getViewLifecycleOwner(), index -> {
            mainActivity.getTaskManager().getReadTask().changeFocus(index);
        });

        // Listen for Movement
        layoutViewModel.getSelfAuto().observe(getViewLifecycleOwner(), transform -> {
            FileInfo fileInfo = mediaViewModel.getCurrentFileInfo().getValue();
            String contentDescriptor = fetchContentDescriptor(fileInfo);

            if (contentDescriptor == null) {
                // skip
                return;
            }

            if (!contentDescriptor.equals(currentContent)) {
                // skip
                return;
            }

            // React to device movement
            switch (fileInfo.getFileType()) {
                case IMAGE:
                    TransformInfo viewport = layoutViewModel.getViewport().getValue();

                    Matrix matrix = mainActivity.getLayoutManager()
                            .getMatrix(transform, viewport, currentImageWidth, currentImageHeight);
                    binding.imageView.setImageMatrix(matrix);
                    break;
                case VIDEO:
                    setVideoMatrix(fileInfo);
                    break;
            }
        });

        // Listen for Exo Player Playback State change
        exoPlayer.addListener(new Player.Listener() {
            @Override
            public void onPlaybackStateChanged(int playbackState) {
                // Detect Completion
                if (playbackState == Player.STATE_ENDED) {
                    exoPlayer.pause();

                    mediaPlayerState = MediaPlayerState.COMPLETED;

                    Integer fileIndex = mediaViewModel.getCurrentFileIndex().getValue();
                    mediaViewModel.setFileInfoListElementPlaybackStatus(
                            fileIndex, FileInfo.PlaybackStatus.PAUSE);
                }
                // Detect Ready
                else if (playbackState == Player.STATE_READY) {
                    if (!durationSet) {
                        long duration = exoPlayer.getDuration();

                        // Set video duration
                        binding.totalTime.setText(TimeHelper.format(duration));

                        // Set Slider config
                        binding.slider.setValueFrom(0);
                        binding.slider.setValueTo(duration);

                        durationSet = true;
                    }
                }
            }
        });

        // Automatically hide controls after inactivity
        handler.post(autoHideControls);

        // Automatically update Slider and Current Time Label
        handler.post(updateSliderAndCurrentTimeLabel);

        // Initialize Popup Menu
        popupMenu = new PopupMenu(mainActivity, binding.menuButton);
        popupMenu.getMenuInflater().inflate(R.menu.play_app_bar_menu, popupMenu.getMenu());
        popupMenu.setOnMenuItemClickListener(menuItem -> {
            markInteraction();

            switch (menuItem.getItemId()) {
                case R.id.option_activate_accelerometer:
                    mediaViewModel.setAccelerometerActive(true);
                    return true;
                case R.id.option_deactivate_accelerometer:
                    mediaViewModel.setAccelerometerActive(false);
                    return true;
                case R.id.option_reset_self:
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

                    layoutViewModel.updateTransform(self, false);
                    return true;
                case R.id.option_file_selection:
                    StateChangeHelper.requestStateChange(
                            mainActivity, connectionViewModel, StateViewModel.AppState.FILE_SELECTION);
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

        // Set media player surface
        binding.textureView.setSurfaceTextureListener(new TextureView.SurfaceTextureListener() {
            @Override
            public void onSurfaceTextureAvailable(@NonNull SurfaceTexture surfaceTexture, int i, int i1) {
                Surface surface = new Surface(surfaceTexture);
//                mediaPlayer.setSurface(surface);
                exoPlayer.setVideoSurface(surface);
            }

            @Override
            public void onSurfaceTextureSizeChanged(@NonNull SurfaceTexture surfaceTexture, int i, int i1) {
                // do nothing
            }

            @Override
            public boolean onSurfaceTextureDestroyed(@NonNull SurfaceTexture surfaceTexture) {
                // do nothing
                return false;
            }

            @Override
            public void onSurfaceTextureUpdated(@NonNull SurfaceTexture surfaceTexture) {
                // do nothing
            }
        });

        // Listen for App State change
        stateViewModel.getState().observe(getViewLifecycleOwner(), state -> {
            if (state == StateViewModel.AppState.FILE_SELECTION) {
//                mediaPlayer.reset();
                resetExoPlayer();
                currentContent = null;
                mediaViewModel.reset();
                navController.navigate(PlayFragmentDirections.actionPlayFragmentPop());
            }
        });

        return  binding.getRoot();
    }

    @Override
    public void onStop() {
        super.onStop();

        deactivateAccelerometerListener();
    }

    private void handleIndexChange(int delta) {
        Boolean isHost = connectionViewModel.getIsHost().getValue();

        Integer index = mediaViewModel.getCurrentFileIndex().getValue();
        index += delta;

        try {
            if (mediaViewModel.isFileInfoListIndexOutOfBounds(index)) {
                // ignore
                return;
            }

            if (isHost) {
                mainActivity.getTaskManager()
                        .sendToAllInGroup(Message.newFileIndexUpdateMessage(index), true);
//                        .runBroadcastTask(Message.newFileIndexUpdateMessage(index));
            } else {
                InetAddress hostAddress =
                        connectionViewModel.getHostDevice().getValue().getInetAddress();
                mainActivity.getTaskManager().runSenderTask(
                        hostAddress, Message.newFileIndexUpdateRequestMessage(index));
//                        .sendToAllInGroup(Message.newFileIndexUpdateRequestMessage(index));
//                        .runBroadcastTask(Message.newFileIndexUpdateRequestMessage(index));
            }
        } catch (IOException e) {
            LogHelper.error(e);
        }
    }

    private void handleImage(FileInfo fileInfo) {
        binding.textureView.setVisibility(View.INVISIBLE);
        binding.imageView.setVisibility(View.VISIBLE);
        disableVideoControls();

        // stop media player - if it is active
        if (mediaPlayerState != MediaPlayerState.IDLE) {
//            mediaPlayer.reset();
            resetExoPlayer();
            mediaPlayerState = MediaPlayerState.IDLE;
            currentContent = null;
        }

        if (fileInfo.getPlaybackStatus() != FileInfo.PlaybackStatus.WAIT) {
            String contentDescriptor = fetchContentDescriptor(fileInfo);
            if (contentDescriptor == null) {
                binding.bufferingLayout.setVisibility(View.VISIBLE);
                return;
            }
            binding.bufferingLayout.setVisibility(View.INVISIBLE);

            if (contentDescriptor.equals(currentContent)) {
                // skip
                return;
            }

            // extract uri from content descriptor (uri/file path)
            Boolean isHost = connectionViewModel.getIsHost().getValue();
            Uri uri = extractUri(isHost, contentDescriptor);

            // set content
            ContentResolver contentResolver =
                    mainActivity.getApplicationContext().getContentResolver();
            int drawableWidth = fileInfo.getWidth();
            int drawableHeight = fileInfo.getHeight();
            // have to extract drawable to insure SDK < 24 devices are compatible
            // ImageView.setImageUri() does not work properly on SDK < 24
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.N) {
                try {
                    InputStream inputStream = contentResolver.openInputStream(uri);

                    Drawable drawable = Drawable.createFromStream(inputStream, null);

                    if (drawable == null) {
                        return;
                    }

                    drawableWidth = drawable.getIntrinsicWidth();
                    drawableHeight = drawable.getIntrinsicHeight();

                    binding.imageView.setImageDrawable(drawable);

                    LogHelper.log("TESTING: w: " + drawableWidth + " h: " + drawableHeight);
                    inputStream.close();
                } catch (IOException e) {
                    LogHelper.error(e);
                }
            } else {
                binding.imageView.setImageURI(uri);
            }

            // set matrix
            TransformInfo self = layoutViewModel.getSelfAuto().getValue();
            TransformInfo viewport = layoutViewModel.getViewport().getValue();

            Matrix matrix = mainActivity.getLayoutManager()
                    .getMatrix(self, viewport, drawableWidth, drawableHeight);
            binding.imageView.setImageMatrix(matrix);

            // Save Drawable information
            currentImageWidth = drawableWidth;
            currentImageHeight = drawableHeight;

            currentContent = contentDescriptor;
        } else {
            binding.bufferingLayout.setVisibility(View.VISIBLE);
            binding.imageView.setImageDrawable(null);

            currentContent = null;
        }
    }

    private void handleVideo(FileInfo fileInfo) {
        binding.textureView.setVisibility(View.VISIBLE);
        binding.imageView.setVisibility(View.INVISIBLE);
        enableVideoControls();

        String contentDescriptor = fetchContentDescriptor(fileInfo);
        // reset media player if necessary
        if (contentDescriptor == null || !contentDescriptor.equals(currentContent)) {
//            mediaPlayer.reset();
            resetExoPlayer();

            currentContent = null;
            mediaPlayerState = MediaPlayerState.IDLE;
        }
        if (contentDescriptor == null) {
            binding.bufferingLayout.setVisibility(View.VISIBLE);
            return;
        }

        Boolean isHost = connectionViewModel.getIsHost().getValue();

        FileInfo.PlaybackStatus status = fileInfo.getPlaybackStatus();
        // PLAY video
        if (status == FileInfo.PlaybackStatus.PLAY) {
            binding.bufferingLayout.setVisibility(View.INVISIBLE);

            // check if we just switched to this video
            if (mediaPlayerState == MediaPlayerState.IDLE || currentContent == null) {
                // in that case we should automatically Pause to prevent auto play when going through the gallery
                mediaViewModel.setFileInfoListElementPlaybackStatus(
                        fileInfo.getIndex(), FileInfo.PlaybackStatus.PAUSE);
                return;
            }

            if (mediaPlayerState == MediaPlayerState.PLAYING
                    || mediaPlayerState == MediaPlayerState.PREPARING
            ) {
                // skip
                return;
            }

            switch (mediaPlayerState) {
                case COMPLETED:
                    LogHelper.log("COMPLETED to PLAYING");
                    exoPlayer.seekTo(0);
                    exoPlayer.play();
                    mediaPlayerState = MediaPlayerState.PLAYING;
                    break;
                case PAUSED:
                    LogHelper.log("PAUSED to PLAYING");
//                    mediaPlayer.start();
                    exoPlayer.play();
                    mediaPlayerState = MediaPlayerState.PLAYING;
                    break;
                case STOPPED:
                    LogHelper.log("STOPPED to PLAYING");
                    exoPlayer.prepare();
                    exoPlayer.play();
                    mediaPlayerState = MediaPlayerState.PLAYING;
                    break;
            }

            // Set Pause Drawable
            binding.playButton.setImageResource(R.drawable.outline_pause_24);

            currentContent = contentDescriptor;
        }
        // DEFERRED PLAY VIDEO
        else if (status == FileInfo.PlaybackStatus.DEFERRED_PLAY) {
            binding.bufferingLayout.setVisibility(View.INVISIBLE);

            // check if we just switched to this video
            if (mediaPlayerState == MediaPlayerState.IDLE || currentContent == null) {
                // in that case we should automatically Pause to prevent auto play when going through the gallery
                mediaViewModel.setFileInfoListElementPlaybackStatus(
                        fileInfo.getIndex(), FileInfo.PlaybackStatus.PAUSE);
                return;
            }

            if (mediaPlayerState == MediaPlayerState.PLAYING
                    || mediaPlayerState == MediaPlayerState.PREPARING
            ) {
                // skip
                return;
            }

            long delay = fileInfo.getTimestamp() - System.currentTimeMillis();
            if (delay < MINIMUM_DEFERRED_DELAY) {
                LogHelper.log("Delay (" + delay + ") is less than MINIMUM_DEFERRED_DELAY(" + MINIMUM_DEFERRED_DELAY + ")! What now?");
                // TODO: handle late deferred handling - notify host and ask for abortion?
                // just request pause
                requestPause();

                Toast.makeText(
                        mainActivity,
                        getResources().getString(R.string.play_error_latency),
                        Toast.LENGTH_SHORT
                ).show();
                return;
            }

            switch (mediaPlayerState) {
                case COMPLETED:
                    LogHelper.log("COMPLETED to PREPARING");
                    handler.postDelayed(deferredPlay, delay);
                    exoPlayer.seekTo(0);
                    mediaPlayerState = MediaPlayerState.PREPARING;
                    break;
                case PAUSED:
                    LogHelper.log("PAUSED to PREPARING");
                    handler.postDelayed(deferredPlay, delay);
                    mediaPlayerState = MediaPlayerState.PREPARING;
                    break;
                case STOPPED:
                    LogHelper.log("STOPPED to PREPARING");
                    handler.postDelayed(deferredPlay, delay);
                    exoPlayer.prepare();
                    mediaPlayerState = MediaPlayerState.PREPARING;
                    break;
            }

            currentContent = contentDescriptor;
        }
        // PAUSE VIDEO
        else if (status == FileInfo.PlaybackStatus.PAUSE) {
            binding.bufferingLayout.setVisibility(View.INVISIBLE);

            try {
                switch (mediaPlayerState) {
                    case IDLE:
                        LogHelper.log("IDLE to PAUSED");
                        // set content
                        setVideoContent(isHost, contentDescriptor, fileInfo);

                        // set matrix
                        setVideoMatrix(fileInfo);

                        exoPlayer.prepare();
                        mediaPlayerState = MediaPlayerState.PAUSED;
                        break;
                    case PLAYING:
                        LogHelper.log("PLAYING to PAUSED");

                        exoPlayer.pause();

                        mediaPlayerState = MediaPlayerState.PAUSED;

                        requestSeekTo(exoPlayer.getCurrentPosition(), true);
                        break;
                    case PREPARING:
                        LogHelper.log("PREPARING to PAUSED");

                        handler.removeCallbacks(deferredPlay);
                        exoPlayer.pause();

                        mediaPlayerState = MediaPlayerState.PAUSED;

                        requestSeekTo(exoPlayer.getCurrentPosition(), true);
                        break;
                }

                // Set Play Drawable
                binding.playButton.setImageResource(R.drawable.outline_play_arrow_24);

                currentContent = contentDescriptor;
            } catch (IOException e) {
                LogHelper.error(e);
            }
        } else {
            binding.bufferingLayout.setVisibility(View.VISIBLE);

            currentContent = null;
        }
    }

    private void setVideoMatrix(FileInfo fileInfo) {
        TransformInfo self = layoutViewModel.getSelfAuto().getValue();
        TransformInfo viewport = layoutViewModel.getViewport().getValue();

        int videoWidth = fileInfo.getWidth();
        int videoHeight = fileInfo.getHeight();
        LogHelper.log("Video: w: " + videoWidth + " h: " + videoHeight);

        Matrix matrix = mainActivity.getLayoutManager()
                .getVideoMatrix(self, viewport, videoWidth, videoHeight);
        binding.textureView.setTransform(matrix);
    }

    private String fetchContentDescriptor(FileInfo fileInfo) {
        if (fileInfo == null) {
            return null;
        }

        Boolean isHost = connectionViewModel.getIsHost().getValue();
        String contentDescriptor = null;

        // fetch content (uri/path)
        if (isHost) {
            ArrayList<String> selectedUris = mediaViewModel.getSelectedMedia().getValue();
            Integer index = mediaViewModel.getCurrentFileIndex().getValue();
            if (index >= 0 && index < selectedUris.size()) {
                contentDescriptor = selectedUris.get(index);
            }
        } else {
            if (fileInfo.getFileType() == FileInfo.FileType.IMAGE) {
                contentDescriptor = fileInfo.getContentUri();
            } else if (fileInfo.getFileType() == FileInfo.FileType.VIDEO) {
                // content descriptor is not used for videos - videos are streamed from host
                // still have to differentiate between files
                contentDescriptor = "dummy#" + fileInfo.getIndex();
            }
        }

        return contentDescriptor;
    }

    private Uri extractUri(Boolean isHost, String contentDescriptor) {
        Uri uri = null;

        if (isHost) {
            // contentDescriptor is uri
            uri = Uri.parse(contentDescriptor);
        } else {
            // contentDescriptor is file absolute path
            File file = new File(contentDescriptor);
            uri = Uri.fromFile(file);
        }

        return uri;
    }

    private void setVideoContent(
            Boolean isHost,
            String contentDescriptor,
            FileInfo fileInfo
    ) throws IOException {
        if (isHost) {
            // file is on local device
            Uri uri = extractUri(isHost, contentDescriptor);
            MediaItem mediaItem = MediaItem.fromUri(uri);
            exoPlayer.setMediaItem(mediaItem);
        } else {
            // file is streamed over WiFi P2P
            InetAddress hostAddress =
                    connectionViewModel.getHostDevice().getValue().getInetAddress();
            int fileIndex = mediaViewModel.getCurrentFileIndex().getValue();

            StringBuilder proxyPath = new StringBuilder("http://");
            proxyPath.append(hostAddress.getHostAddress());
            proxyPath.append(":");
            proxyPath.append(TaskManager.PROXY_PORT);
            proxyPath.append("/");
            proxyPath.append("hello");  // not necessary, url looks weird without
            proxyPath.append("?fileindex=");
            proxyPath.append(fileIndex);
            proxyPath.append("?filesize=");
            proxyPath.append(fileInfo.getFileSize());
            proxyPath.append("?mimetype=");
            proxyPath.append(fileInfo.getMimeType());

            String path = proxyPath.toString();
            LogHelper.log("Proxy path: " + path);

            Uri uri = Uri.parse(path);
            MediaItem mediaItem = MediaItem.fromUri(uri);
            exoPlayer.setMediaItem(mediaItem);
        }
    }

    private void resetExoPlayer() {
        exoPlayer.pause();
        exoPlayer.stop();
        exoPlayer.clearMediaItems();

        durationSet = false;
    }

    private void requestPlay() {
        Boolean isHost = connectionViewModel.getIsHost().getValue();
        Integer fileIndex = mediaViewModel.getCurrentFileIndex().getValue();

        try {
            if (isHost) {
                mainActivity.getMediaManager().requestPlay(fileIndex);
            } else {
                // send message to host requesting Play
                InetAddress hostAddress =
                        connectionViewModel.getHostDevice().getValue().getInetAddress();
                PlaybackStatusCommand command =
                        new PlaybackStatusCommand(fileIndex, FileInfo.PlaybackStatus.PLAY);

                mainActivity.getTaskManager().runSenderTask(
                        hostAddress, Message.newPlaybackStatusRequestMessage(command));
            }
        } catch (IOException e) {
            LogHelper.error(e);
        }
    }

    private void requestPause() {
        Boolean isHost = connectionViewModel.getIsHost().getValue();
        Integer fileIndex = mediaViewModel.getCurrentFileIndex().getValue();

        try {
            if (isHost) {
                mainActivity.getMediaManager().requestPause(fileIndex);
            } else {
                // send message to host requesting Pause
                InetAddress hostAddress =
                        connectionViewModel.getHostDevice().getValue().getInetAddress();
                PlaybackStatusCommand command =
                        new PlaybackStatusCommand(fileIndex, FileInfo.PlaybackStatus.PAUSE);

                mainActivity.getTaskManager().runSenderTask(
                        hostAddress, Message.newPlaybackStatusRequestMessage(command));
            }
        } catch (IOException e) {
            LogHelper.error(e);
        }
    }

    private void requestSeekTo(long timestamp, boolean onlyHost) {
        Boolean isHost = connectionViewModel.getIsHost().getValue();

        try {
            if (isHost) {
                Message message = Message.newSeekToOrderMessage(timestamp);
                mainActivity.getTaskManager().sendToAllInGroup(message, true);
            } else {
                if (onlyHost) {
                    // skip
                    return;
                }

                InetAddress hostAddress =
                        connectionViewModel.getHostDevice().getValue().getInetAddress();

                Message message = Message.newSeekToRequestMessage(timestamp);
                mainActivity.getTaskManager().runSenderTask(hostAddress, message);
            }
        } catch (IOException e) {
            LogHelper.error(e);
        }
    }

    private void markInteraction() {
        lastInteraction = System.currentTimeMillis();
    }

    private void hideControls() {
        binding.menuButton.setVisibility(View.INVISIBLE);
        binding.controlsLayout.setVisibility(View.INVISIBLE);

        acquireFullScreen();
    }

    private void showControls() {
        binding.menuButton.setVisibility(View.VISIBLE);
        binding.controlsLayout.setVisibility(View.VISIBLE);
    }

    private void disableVideoControls() {
        binding.playButton.setVisibility(View.INVISIBLE);
        binding.currentTime.setVisibility(View.INVISIBLE);
        binding.totalTime.setVisibility(View.INVISIBLE);
        binding.slider.setVisibility(View.INVISIBLE);
    }

    private void enableVideoControls() {
        binding.playButton.setVisibility(View.VISIBLE);
        binding.currentTime.setVisibility(View.VISIBLE);
        binding.totalTime.setVisibility(View.VISIBLE);
        binding.slider.setVisibility(View.VISIBLE);
    }

    private void activateAccelerometerListener() {
        if (sensorManager == null) {
            // skip
            return;
        }

        if (accelerometer == null) {
            // Report issue
            Toast.makeText(
                    mainActivity,
                    getResources().getString(R.string.play_error_accelerometer_missing),
                    Toast.LENGTH_SHORT
            ).show();

            // Automatically deactivate
            mediaViewModel.setAccelerometerActive(false);
            return;
        }

        // Register Sensor Listener
        sensorManager.registerListener(
                accelerometerListener,
                accelerometer,
                SensorManager.SENSOR_DELAY_GAME
        );
    }

    private void deactivateAccelerometerListener() {
        if (sensorManager != null) {
            sensorManager.unregisterListener(accelerometerListener);
        }

        accelerometerInfo.reset();
    }
}