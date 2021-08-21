package ka170130.pmu.infinityscreen.play;

import android.content.ContentResolver;
import android.content.Context;
import android.graphics.Matrix;
import android.graphics.SurfaceTexture;
import android.graphics.drawable.Drawable;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;

import android.util.Log;
import android.util.Size;
import android.view.LayoutInflater;
import android.view.Surface;
import android.view.TextureView;
import android.view.View;
import android.view.ViewGroup;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;

import ka170130.pmu.infinityscreen.MainActivity;
import ka170130.pmu.infinityscreen.R;
import ka170130.pmu.infinityscreen.containers.FileInfo;
import ka170130.pmu.infinityscreen.containers.Message;
import ka170130.pmu.infinityscreen.containers.PeerInetAddressInfo;
import ka170130.pmu.infinityscreen.containers.TransformInfo;
import ka170130.pmu.infinityscreen.databinding.FragmentHomeBinding;
import ka170130.pmu.infinityscreen.databinding.FragmentPlayBinding;
import ka170130.pmu.infinityscreen.helpers.LogHelper;
import ka170130.pmu.infinityscreen.helpers.StateChangeHelper;
import ka170130.pmu.infinityscreen.media.FileSelectionWaitFragmentDirections;
import ka170130.pmu.infinityscreen.viewmodels.LayoutViewModel;
import ka170130.pmu.infinityscreen.viewmodels.MediaViewModel;
import ka170130.pmu.infinityscreen.viewmodels.StateViewModel;

public class PlayFragment extends FullScreenFragment {

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

    private MediaPlayer mediaPlayer;
    private Uri currentContent;
    private MediaPlayerState mediaPlayerState;

    public PlayFragment() {
        // Required empty public constructor
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        stateViewModel = new ViewModelProvider(mainActivity).get(StateViewModel.class);
        layoutViewModel = new ViewModelProvider(mainActivity).get(LayoutViewModel.class);
        mediaViewModel = new ViewModelProvider(mainActivity).get(MediaViewModel.class);

        mediaPlayer = new MediaPlayer();
        mediaPlayer.setLooping(false);
        mediaPlayer.setOnCompletionListener(mp -> {
            mediaPlayerState = MediaPlayerState.COMPLETED;
        });

        currentContent = null;
        mediaPlayerState = MediaPlayerState.IDLE;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        binding = FragmentPlayBinding.inflate(inflater, container, false);

        // Previous Button
        binding.previousButton.setOnClickListener(view -> handleIndexChange(-1));

        // Next Button
        binding.nextButton.setOnClickListener(view -> handleIndexChange(1));

        // Listen for File Info change
        mediaViewModel.getCurrentFileInfo().observe(getViewLifecycleOwner(), fileInfo -> {
            if (fileInfo == null) {
                return;
            }

            LogHelper.log("Active FileInfo: " + fileInfo.getFileType().toString()
                    + " " + fileInfo.getWidth()
                    + " " + fileInfo.getHeight()
                    + " " + fileInfo.getExtension()
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

        // TODO: remove this code - replace with popup menu or something
        binding.menuButton.setOnClickListener(view -> {
            StateChangeHelper.requestStateChange(
                    mainActivity, connectionViewModel, StateViewModel.AppState.FILE_SELECTION);
        });

        // Set media player surface
        binding.textureView.setSurfaceTextureListener(new TextureView.SurfaceTextureListener() {
            @Override
            public void onSurfaceTextureAvailable(@NonNull SurfaceTexture surfaceTexture, int i, int i1) {
                Surface surface = new Surface(surfaceTexture);
                mediaPlayer.setSurface(surface);
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
                mediaViewModel.reset();
                navController.navigate(PlayFragmentDirections.actionPlayFragmentPop());
            }
        });

        return  binding.getRoot();
    }

    @Override
    public void onResume() {
        super.onResume();

        // sync App State - necessary for the Back button to work
//        StateChangeHelper.requestStateChange(
//                mainActivity, connectionViewModel, StateViewModel.AppState.PLAY);
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

        // stop media player - if it is active
        if (mediaPlayerState != MediaPlayerState.IDLE) {
            mediaPlayer.reset();
            mediaPlayerState = MediaPlayerState.IDLE;
            currentContent = null;
        }

        if (fileInfo.getPlaybackStatus() != FileInfo.PlaybackStatus.WAIT) {
            binding.bufferingLayout.setVisibility(View.INVISIBLE);

            Uri uri = fetchUri(fileInfo);
            if (uri.equals(currentContent)) {
                // skip -  content already set
                return;
            }

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

            currentContent = uri;
        } else {
            binding.bufferingLayout.setVisibility(View.VISIBLE);
            binding.imageView.setImageDrawable(null);

            currentContent = null;
        }
    }

    private void handleVideo(FileInfo fileInfo) {
        binding.textureView.setVisibility(View.VISIBLE);
        binding.imageView.setVisibility(View.INVISIBLE);

        FileInfo.PlaybackStatus status = fileInfo.getPlaybackStatus();
        if (status == FileInfo.PlaybackStatus.PLAY) {
            if (mediaPlayerState == MediaPlayerState.PLAYING) {
                // skip
                return;
            }

            binding.bufferingLayout.setVisibility(View.INVISIBLE);

            Uri uri = fetchUri(fileInfo);
            if (uri == null) {
                return;
            }

            // reset media player if necessary
            if (!uri.equals(currentContent)) {
                mediaPlayer.reset();

                currentContent = null;
                mediaPlayerState = MediaPlayerState.IDLE;
            }

            try {
                switch (mediaPlayerState) {
                    case IDLE:
                        // set content
                        if (!mediaPlayer.isPlaying()) {
                            mediaPlayer.setDataSource(mainActivity, uri);
                            mediaPlayer.setOnPreparedListener(MediaPlayer::start);
                            mediaPlayer.prepareAsync();
                        }

                        // set matrix
                        TransformInfo self = layoutViewModel.getSelfAuto().getValue();
                        TransformInfo viewport = layoutViewModel.getViewport().getValue();

                        Size videoDimens = mainActivity.getMediaManager().getVideoDimensions(uri);
                        int videoWidth = videoDimens.getWidth();
                        int videoHeight = videoDimens.getHeight();
                        LogHelper.log("Video: w: " + videoWidth + " h: " + videoHeight);

                        Matrix matrix = mainActivity.getLayoutManager()
                                .getVideoMatrix(self, viewport, videoWidth, videoHeight);
                        binding.textureView.setTransform(matrix);

                        mediaPlayerState = MediaPlayerState.PLAYING;
                        break;
                    case PAUSED:
                    case COMPLETED:
                        mediaPlayer.start();
                        mediaPlayerState = MediaPlayerState.PLAYING;
                        break;
                    case STOPPED:
                        mediaPlayer.setOnPreparedListener(MediaPlayer::start);
                        mediaPlayer.prepareAsync();
                        mediaPlayerState = MediaPlayerState.PLAYING;
                        break;
                }

                currentContent = uri;
            } catch (IOException e) {
                LogHelper.error(e);
            }
        } else if (status == FileInfo.PlaybackStatus.PAUSE) {
            binding.bufferingLayout.setVisibility(View.INVISIBLE);

            Uri uri = fetchUri(fileInfo);
            // reset media player if necessary
            if (!uri.equals(currentContent)) {
                mediaPlayer.reset();

                currentContent = null;
                mediaPlayerState = MediaPlayerState.IDLE;
            }

            try {
                switch (mediaPlayerState) {
                    case IDLE:
                        mediaPlayer.setDataSource(mainActivity, uri);
                        mediaPlayer.prepare();
                        mediaPlayerState = MediaPlayerState.PAUSED;
                        break;
                    case PLAYING:
                        if (mediaPlayer.isPlaying()) {
                            mediaPlayer.pause();
                        }
                        mediaPlayerState = MediaPlayerState.PAUSED;
                        break;
                }

                currentContent = uri;
            } catch (IOException e) {
                LogHelper.error(e);
            }
        } else {
            binding.bufferingLayout.setVisibility(View.VISIBLE);

            currentContent = null;
        }
    }

    private Uri fetchUri(FileInfo fileInfo) {
        Boolean isHost = connectionViewModel.getIsHost().getValue();
        Uri uri = null;

        // fetch image uri
        if (isHost) {
            ArrayList<String> selectedUris = mediaViewModel.getSelectedMedia().getValue();
            Integer index = mediaViewModel.getCurrentFileIndex().getValue();
            uri = Uri.parse(selectedUris.get(index));
        } else {
            uri = Uri.parse(fileInfo.getContentUri());
        }

        return uri;
    }
}