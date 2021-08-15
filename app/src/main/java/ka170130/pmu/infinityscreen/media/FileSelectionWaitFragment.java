package ka170130.pmu.infinityscreen.media;

import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import ka170130.pmu.infinityscreen.MainActivity;
import ka170130.pmu.infinityscreen.R;
import ka170130.pmu.infinityscreen.connection.ConnectionAwareFragment;
import ka170130.pmu.infinityscreen.containers.Message;
import ka170130.pmu.infinityscreen.databinding.FragmentFileSelectionBinding;
import ka170130.pmu.infinityscreen.databinding.FragmentFileSelectionWaitBinding;
import ka170130.pmu.infinityscreen.helpers.AppBarAndStatusHelper;
import ka170130.pmu.infinityscreen.helpers.StateChangeHelper;
import ka170130.pmu.infinityscreen.viewmodels.StateViewModel;

public class FileSelectionWaitFragment extends ConnectionAwareFragment {

    private FragmentFileSelectionWaitBinding binding;
    private StateViewModel stateViewModel;

    public FileSelectionWaitFragment() {
        // Required empty public constructor
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        stateViewModel = new ViewModelProvider(mainActivity).get(StateViewModel.class);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        binding = FragmentFileSelectionWaitBinding.inflate(inflater, container, false);

        // Setup Status Cards
        setupStatusCards(binding.appBarAndStatus);

        // Inflate Top App Bar Menu
        AppBarAndStatusHelper.inflateMenu(binding.appBarAndStatus, R.menu.app_bar_menu, item -> {
            if (item.getItemId() == R.id.option_disconnect) {
                Boolean isHost = connectionViewModel.getIsHost().getValue();
                if (isHost) {
                    // disconnect all
                    mainActivity.getTaskManager().
                            sendToAllInGroup(Message.newDisconnectMessage(), true);
//                    mainActivity.getTaskManager().runBroadcastTask(Message.newDisconnectMessage());
                } else {
                    // disconnect self
                    mainActivity.getConnectionManager().disconnect();
                }
                return true;
            }

            return false;
        });

        // TODO

        // Listen for App State change
        stateViewModel.getState().observe(getViewLifecycleOwner(), state -> {
            if (state == StateViewModel.AppState.LAYOUT) {
                navController.navigateUp();
            } else if (state == StateViewModel.AppState.PLAY) {
                navController.navigate(FileSelectionWaitFragmentDirections.actionPlayFragment());
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