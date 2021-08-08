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
import ka170130.pmu.infinityscreen.databinding.FragmentFileSelectionBinding;
import ka170130.pmu.infinityscreen.databinding.FragmentHomeBinding;
import ka170130.pmu.infinityscreen.helpers.StateChangeHelper;
import ka170130.pmu.infinityscreen.layout.PreviewFragmentDirections;
import ka170130.pmu.infinityscreen.viewmodels.StateViewModel;

public class FileSelectionFragment extends ConnectionAwareFragment {

    private FragmentFileSelectionBinding binding;
    private StateViewModel stateViewModel;

    public FileSelectionFragment() {
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
        binding = FragmentFileSelectionBinding.inflate(inflater, container, false);

        // Setup Status Cards
        setupStatusCards(binding.appBarAndStatus);

        // TODO
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