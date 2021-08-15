package ka170130.pmu.infinityscreen.play;

import android.content.ContentResolver;
import android.graphics.Matrix;
import android.graphics.drawable.Drawable;
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
import android.view.LayoutInflater;
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
import ka170130.pmu.infinityscreen.helpers.StateChangeHelper;
import ka170130.pmu.infinityscreen.media.FileSelectionWaitFragmentDirections;
import ka170130.pmu.infinityscreen.viewmodels.LayoutViewModel;
import ka170130.pmu.infinityscreen.viewmodels.MediaViewModel;
import ka170130.pmu.infinityscreen.viewmodels.StateViewModel;

public class PlayFragment extends FullScreenFragment {

    private FragmentPlayBinding binding;
    private StateViewModel stateViewModel;
    private LayoutViewModel layoutViewModel;
    private MediaViewModel mediaViewModel;

    public PlayFragment() {
        // Required empty public constructor
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        stateViewModel = new ViewModelProvider(mainActivity).get(StateViewModel.class);
        layoutViewModel = new ViewModelProvider(mainActivity).get(LayoutViewModel.class);
        mediaViewModel = new ViewModelProvider(mainActivity).get(MediaViewModel.class);
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

            if (fileInfo.getFileType() == FileInfo.FileType.IMAGE) {
               handleImage(fileInfo);
            }

            // TODO: handle FileType.VIDEO
        });

        // TODO: remove this code - replace with popup menu or something
        binding.menuButton.setOnClickListener(view -> {
            StateChangeHelper.requestStateChange(
                    mainActivity, connectionViewModel, StateViewModel.AppState.FILE_SELECTION);
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
            Log.d(MainActivity.LOG_TAG, e.toString());
            e.printStackTrace();
        }
    }

    private void handleImage(FileInfo fileInfo) {
        binding.textureView.setVisibility(View.INVISIBLE);
        binding.imageView.setVisibility(View.VISIBLE);

        Log.d(MainActivity.LOG_TAG,
                "Active FileInfo: " + fileInfo.getFileType().toString()
                        + " " + fileInfo.getWidth()
                        + " " + fileInfo.getHeight()
                        + " " + fileInfo.getExtension()
                        + " " + fileInfo.getPlaybackStatus()
                        + " " + fileInfo.getContentUri()
        );
        if (fileInfo.getPlaybackStatus() != FileInfo.PlaybackStatus.WAIT) {
            binding.bufferingLayout.setVisibility(View.INVISIBLE);

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

                    Log.d(MainActivity.LOG_TAG, "TESTING: w: " + drawableWidth + " h: " + drawableHeight);
                    inputStream.close();
                } catch (IOException e) {
                    Log.d(MainActivity.LOG_TAG, e.toString());
                    e.printStackTrace();
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
        } else {
            binding.bufferingLayout.setVisibility(View.VISIBLE);

            binding.imageView.setImageDrawable(null);
        }
    }
}