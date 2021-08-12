package ka170130.pmu.infinityscreen.play;

import android.graphics.Matrix;
import android.graphics.drawable.Drawable;
import android.net.Uri;
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

import java.io.IOException;
import java.util.ArrayList;

import ka170130.pmu.infinityscreen.MainActivity;
import ka170130.pmu.infinityscreen.R;
import ka170130.pmu.infinityscreen.containers.FileInfo;
import ka170130.pmu.infinityscreen.containers.Message;
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
                binding.textureView.setVisibility(View.INVISIBLE);
                binding.imageView.setVisibility(View.VISIBLE);

//                Boolean isHost = connectionViewModel.getIsHost().getValue();
//                if (!isHost) {
//                    return;
//                }

                // set content
                Log.d(MainActivity.LOG_TAG, "Active FileInfo: " + fileInfo.getFileType().toString() + " " + fileInfo.getWidth() + " " + fileInfo.getHeight() + " " + fileInfo.getExtension() + " " + fileInfo.getPlaybackStatus() + " " + fileInfo.getContentUri());
                if (fileInfo.getPlaybackStatus() != FileInfo.PlaybackStatus.WAIT) {
//                    Uri uri = Uri.parse(fileInfo.getContentUri());
//                    binding.imageView.setImageURI(uri);
                    Boolean isHost = connectionViewModel.getIsHost().getValue();
                    Uri uri = null;

                    if (isHost) {
                        ArrayList<String> selectedUris = mediaViewModel.getSelectedMedia().getValue();
                        Integer index = mediaViewModel.getCurrentFileIndex().getValue();
                        uri = Uri.parse(selectedUris.get(index));
                    } else {
                        uri = Uri.parse(fileInfo.getContentUri());
                    }
                    binding.imageView.setImageURI(uri);
                }

                // set matrix
                TransformInfo self = layoutViewModel.getSelfAuto().getValue();
                TransformInfo viewport = layoutViewModel.getViewport().getValue();

                int drawableWidth = fileInfo.getWidth();
                int drawableHeight = fileInfo.getHeight();

                Matrix matrix = mainActivity.getLayoutManager()
                        .getMatrix(self, viewport, drawableWidth, drawableHeight);
                binding.imageView.setImageMatrix(matrix);
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
                        .runBroadcastTask(Message.newFileIndexUpdateMessage(index));
            } else {
                mainActivity.getTaskManager()
                        .runBroadcastTask(Message.newFileIndexUpdateRequestMessage(index));
            }
        } catch (IOException e) {
            Log.d(MainActivity.LOG_TAG, e.toString());
            e.printStackTrace();
        }
    }
}