package ka170130.pmu.infinityscreen.media;

import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.LinearLayoutManager;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import java.util.ArrayList;

import ka170130.pmu.infinityscreen.MainActivity;
import ka170130.pmu.infinityscreen.R;
import ka170130.pmu.infinityscreen.connection.ConnectionAwareFragment;
import ka170130.pmu.infinityscreen.connection.DeviceConnectedAdapter;
import ka170130.pmu.infinityscreen.containers.Message;
import ka170130.pmu.infinityscreen.databinding.FragmentFileSelectionBinding;
import ka170130.pmu.infinityscreen.databinding.FragmentHomeBinding;
import ka170130.pmu.infinityscreen.helpers.AppBarAndStatusHelper;
import ka170130.pmu.infinityscreen.helpers.FileSelectionHelper;
import ka170130.pmu.infinityscreen.helpers.StateChangeHelper;
import ka170130.pmu.infinityscreen.layout.PreviewFragmentDirections;
import ka170130.pmu.infinityscreen.viewmodels.MediaViewModel;
import ka170130.pmu.infinityscreen.viewmodels.StateViewModel;

public class FileSelectionFragment extends ConnectionAwareFragment {

    private static final int NUM_OF_COLUMNS = 3;

    private FragmentFileSelectionBinding binding;
    private StateViewModel stateViewModel;
    private MediaViewModel mediaViewModel;

    private String[] mimeTypes;

    public FileSelectionFragment() {
        // Required empty public constructor
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        stateViewModel = new ViewModelProvider(mainActivity).get(StateViewModel.class);
        mediaViewModel = new ViewModelProvider(mainActivity).get(MediaViewModel.class);

        String[] mimeTypes = { "image/*", "video/*" };
        this.mimeTypes = mimeTypes;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        binding = FragmentFileSelectionBinding.inflate(inflater, container, false);

        // Setup Status Cards
        setupStatusCards(binding.appBarAndStatus);

        // Inflate Top App Bar Menu
        AppBarAndStatusHelper.inflateMenu(binding.appBarAndStatus, R.menu.app_bar_menu, item -> {
            if (item.getItemId() == R.id.option_disconnect) {
                Boolean isHost = connectionViewModel.getIsHost().getValue();
                if (isHost) {
                    // disconnect all
                    mainActivity.getTaskManager().runBroadcastTask(Message.newDisconnectMessage());
                } else {
                    // disconnect self
                    mainActivity.getConnectionManager().disconnect();
                }
                return true;
            }

            return false;
        });

        // Select Files
        binding.selectFilesButton.setOnClickListener(view -> {
            FileSelectionHelper.request(mimeTypes, stringList -> {
                mediaViewModel.addToSelectedMedia(stringList);
            });
        });

        // Clear All Files
        binding.clearAllButton.setOnClickListener(view -> {
            mediaViewModel.clearSelectedMedia();
        });

        // Recycler View
        MediaAdapter mediaAdapter = new MediaAdapter(
                mainActivity,
                position -> {
                    mediaViewModel.removeFromSelectedMedia(position);
                }
        );
        binding.recyclerView.setHasFixedSize(false);
        binding.recyclerView.setAdapter(mediaAdapter);
        binding.recyclerView.setLayoutManager(new GridLayoutManager(mainActivity, NUM_OF_COLUMNS));

        // React to File Selection Change
        mediaViewModel.getSelectedMedia().observe(getViewLifecycleOwner(), stringList -> {
            if (stringList.size() == 0) {
                // disable play/clear buttons and show empty label
                binding.playButton.setEnabled(false);
                binding.clearAllButton.setEnabled(false);
                binding.fileSelectionEmptyLabel.setVisibility(View.VISIBLE);
            } else {
                // enable play/clear buttons and hide empty label
                binding.playButton.setEnabled(true);
                binding.clearAllButton.setEnabled(true);
                binding.fileSelectionEmptyLabel.setVisibility(View.GONE);
            }

            // update recycler view
            mediaAdapter.setStringList(stringList);
        });

        // Play Selected Files
        binding.playButton.setOnClickListener(view -> {
            StateChangeHelper.requestStateChange(
                    mainActivity, connectionViewModel, StateViewModel.AppState.PLAY);
//            navController.navigate(FileSelectionFragmentDirections.actionPlayFragment());
        });

        // Listen for App State change
        stateViewModel.getState().observe(getViewLifecycleOwner(), state -> {
            if (state == StateViewModel.AppState.LAYOUT) {
                navController.navigateUp();
            } else if (state == StateViewModel.AppState.PLAY) {
                navController.navigate(FileSelectionFragmentDirections.actionPlayFragment());
            }
        });

        return  binding.getRoot();
    }

    @Override
    public void onResume() {
        super.onResume();

        // sync App State - necessary for the Back button to work
//        StateChangeHelper.requestStateChange(
//                mainActivity, connectionViewModel, StateViewModel.AppState.FILE_SELECTION);
    }
}