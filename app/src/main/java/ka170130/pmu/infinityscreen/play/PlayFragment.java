package ka170130.pmu.infinityscreen.play;

import android.content.ContentResolver;
import android.graphics.Matrix;
import android.graphics.SurfaceTexture;
import android.graphics.drawable.Drawable;
import android.media.MediaDataSource;
import android.media.MediaPlayer;
import android.media.session.PlaybackState;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.ViewModelProvider;

import android.os.Handler;
import android.util.Size;
import android.view.LayoutInflater;
import android.view.Surface;
import android.view.TextureView;
import android.view.View;
import android.view.ViewGroup;

import com.google.android.exoplayer2.MediaItem;
import com.google.android.exoplayer2.Player;
import com.google.android.exoplayer2.SimpleExoPlayer;
import com.google.android.exoplayer2.drm.DrmSessionEventListener;
import com.google.android.exoplayer2.source.BaseMediaSource;
import com.google.android.exoplayer2.source.MediaPeriod;
import com.google.android.exoplayer2.source.MediaSource;
import com.google.android.exoplayer2.source.MediaSourceEventListener;
import com.google.android.exoplayer2.upstream.Allocator;
import com.google.android.exoplayer2.upstream.TransferListener;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.InetAddress;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;

import ka170130.pmu.infinityscreen.R;
import ka170130.pmu.infinityscreen.communication.TaskManager;
import ka170130.pmu.infinityscreen.containers.FileInfo;
import ka170130.pmu.infinityscreen.containers.Message;
import ka170130.pmu.infinityscreen.containers.PeerInetAddressInfo;
import ka170130.pmu.infinityscreen.containers.PlaybackStatusCommand;
import ka170130.pmu.infinityscreen.containers.TransformInfo;
import ka170130.pmu.infinityscreen.databinding.FragmentPlayBinding;
import ka170130.pmu.infinityscreen.helpers.LogHelper;
import ka170130.pmu.infinityscreen.helpers.StateChangeHelper;
import ka170130.pmu.infinityscreen.viewmodels.LayoutViewModel;
import ka170130.pmu.infinityscreen.viewmodels.MediaViewModel;
import ka170130.pmu.infinityscreen.viewmodels.StateViewModel;

public class PlayFragment extends FullScreenFragment {

    private static final long CONTROLS_TIMEOUT = 10_000;
    private static final long AUTO_HIDE_CHECK_INTERVAL = 500;

    private enum MediaPlayerState {
        IDLE,
        PLAYING,
        PAUSED,
        STOPPED,
        COMPLETED
    }

    private FragmentPlayBinding binding;
    private StateViewModel stateViewModel;
    private LayoutViewModel layoutViewModel;
    private MediaViewModel mediaViewModel;

//    private MediaPlayer mediaPlayer;
    private SimpleExoPlayer exoPlayer;

    private String currentContent;
    private MediaPlayerState mediaPlayerState;

    private long lastInteraction = 0;

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

        handler = new Handler(mainActivity.getMainLooper());
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        binding = FragmentPlayBinding.inflate(inflater, container, false);

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

        // TODO: remove this code - replace with popup menu or something
        binding.menuButton.setOnClickListener(view -> {
            StateChangeHelper.requestStateChange(
                    mainActivity, connectionViewModel, StateViewModel.AppState.FILE_SELECTION);
            markInteraction();
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

        // Listen for File Index change
        mediaViewModel.getCurrentFileIndex().observe(getViewLifecycleOwner(), index -> {
            mainActivity.getTaskManager().getReadTask().changeFocus(index);
        });

        // Listen for Media Completion (and more?)
        exoPlayer.addListener(new Player.Listener() {
            @Override
            public void onPlaybackStateChanged(int playbackState) {
                // Detect completion
                if (playbackState == Player.STATE_ENDED) {
                    mediaPlayerState = MediaPlayerState.COMPLETED;

                    Integer fileIndex = mediaViewModel.getCurrentFileIndex().getValue();
                    mediaViewModel.setFileInfoListElementPlaybackStatus(
                            fileIndex, FileInfo.PlaybackStatus.PAUSE);
                }
            }
        });

        // Automatically hide controls after inactivity
        handler.post(autoHideControls);

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
                PeerInetAddressInfo host = connectionViewModel.getHostDevice().getValue();
                mainActivity.getTaskManager().runSenderTask(
                        host.getInetAddress(), Message.newFileIndexUpdateRequestMessage(index));
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
            if (currentContent == null) {
                // in that case we should automatically Pause to prevent auto play when going through the gallery
                mediaViewModel.setFileInfoListElementPlaybackStatus(
                        fileInfo.getIndex(), FileInfo.PlaybackStatus.PAUSE);
                return;
            }

            if (mediaPlayerState == MediaPlayerState.PLAYING) {
                // skip
                return;
            }

            try {
                switch (mediaPlayerState) {
                    case IDLE:
                        LogHelper.log("IDLE to PLAYING");
                        // set content
                        setVideoContent(isHost, contentDescriptor, fileInfo);

                        // set matrix
                        setVideoMatrix(fileInfo);

                        if (!exoPlayer.isPlaying()) {
//                            mediaPlayer.setOnPreparedListener(MediaPlayer::start);
//                            mediaPlayer.prepareAsync();
                            exoPlayer.prepare();
                            exoPlayer.play();
                        }

                        mediaPlayerState = MediaPlayerState.PLAYING;
                        break;
                    case COMPLETED:
                        LogHelper.log("COMPLETED to PLAYING");
                        exoPlayer.seekTo(0);
                        exoPlayer.play();
                        mediaPlayerState = MediaPlayerState.PLAYING;
                        break;
                    case PAUSED:
                        LogHelper.log("PAUSED to PLAYING");
//                        mediaPlayer.start();
                        exoPlayer.play();
                        mediaPlayerState = MediaPlayerState.PLAYING;
                        break;
                    case STOPPED:
                        LogHelper.log("STOPPED to PLAYING");
//                        mediaPlayer.setOnPreparedListener(MediaPlayer::start);
//                        mediaPlayer.prepareAsync();
                        exoPlayer.prepare();
                        exoPlayer.play();
                        mediaPlayerState = MediaPlayerState.PLAYING;
                        break;
                }

                // Set Pause Drawable
                binding.playButton.setImageResource(R.drawable.outline_pause_24);

                currentContent = contentDescriptor;
            } catch (IOException e) {
                LogHelper.error(e);
            }
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

//                        mediaPlayer.prepare();
                        exoPlayer.prepare();
                        mediaPlayerState = MediaPlayerState.PAUSED;
                        break;
                    case PLAYING:
                        LogHelper.log("PLAYING to PAUSED");
                        if (exoPlayer.isPlaying()) {
//                            mediaPlayer.pause();
                            exoPlayer.pause();
                        }
                        mediaPlayerState = MediaPlayerState.PAUSED;
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
//            mediaPlayer.setDataSource(mainActivity, uri);
            MediaItem mediaItem = MediaItem.fromUri(uri);
            exoPlayer.setMediaItem(mediaItem);
        } else {
            // file is streamed over WiFi P2P
            PeerInetAddressInfo host = connectionViewModel.getHostDevice().getValue();
            String hostAddress = host.getInetAddress().getHostAddress();
            int fileIndex = mediaViewModel.getCurrentFileIndex().getValue();

            StringBuilder proxyPath = new StringBuilder("http://");
            proxyPath.append(hostAddress);
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

//            mediaPlayer.setDataSource(path);
            Uri uri = Uri.parse(path);
            MediaItem mediaItem = MediaItem.fromUri(uri);
            exoPlayer.setMediaItem(mediaItem);
        }
    }

    private void resetExoPlayer() {
        exoPlayer.pause();
        exoPlayer.stop();
        exoPlayer.clearMediaItems();
    }

    private void requestPlay() {
        Boolean isHost = connectionViewModel.getIsHost().getValue();
        Integer fileIndex = mediaViewModel.getCurrentFileIndex().getValue();

        try {
            if (isHost) {
                mainActivity.getMediaManager().requestPlay(fileIndex);
            } else {
                // send message to host requesting Play
                PeerInetAddressInfo host = connectionViewModel.getHostDevice().getValue();
                InetAddress hostAddress = host.getInetAddress();
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
                PeerInetAddressInfo host = connectionViewModel.getHostDevice().getValue();
                InetAddress hostAddress = host.getInetAddress();
                PlaybackStatusCommand command =
                        new PlaybackStatusCommand(fileIndex, FileInfo.PlaybackStatus.PAUSE);

                mainActivity.getTaskManager().runSenderTask(
                        hostAddress, Message.newPlaybackStatusRequestMessage(command));
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
}