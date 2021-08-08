package ka170130.pmu.infinityscreen.layout;

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

import ka170130.pmu.infinityscreen.MainActivity;
import ka170130.pmu.infinityscreen.R;
import ka170130.pmu.infinityscreen.connection.ConnectionAwareFragment;
import ka170130.pmu.infinityscreen.connection.DeviceListFragmentDirections;
import ka170130.pmu.infinityscreen.containers.Message;
import ka170130.pmu.infinityscreen.containers.PeerInetAddressInfo;
import ka170130.pmu.infinityscreen.databinding.FragmentHomeBinding;
import ka170130.pmu.infinityscreen.databinding.FragmentLayoutBinding;
import ka170130.pmu.infinityscreen.helpers.StateChangeHelper;
import ka170130.pmu.infinityscreen.viewmodels.StateViewModel;

public class LayoutFragment extends ConnectionAwareFragment {

    private FragmentLayoutBinding binding;
    private StateViewModel stateViewModel;

    public LayoutFragment() {
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
        binding = FragmentLayoutBinding.inflate(inflater, container, false);

        // Setup Status Cards
        setupStatusCards(binding.appBarAndStatus);

        // TODO: everything
        binding.previewButton.setOnClickListener(view -> {
            StateChangeHelper.requestStateChange(
                    mainActivity, connectionViewModel, StateViewModel.AppState.PREVIEW);
//            navController.navigate(LayoutFragmentDirections.actionPreviewFragment());
        });

        binding.continueButton.setOnClickListener(view -> {
            StateChangeHelper.requestStateChange(
                    mainActivity, connectionViewModel, StateViewModel.AppState.FILE_SELECTION);
//            navController.navigate(LayoutFragmentDirections.actionFileSelectionFragment());
        });

        // Listen for App State change
        stateViewModel.getState().observe(getViewLifecycleOwner(), state -> {
            Boolean isHost = connectionViewModel.getIsHost().getValue();

            if (state == StateViewModel.AppState.CONNECTION) {
                navController.navigateUp();
            } else if (state == StateViewModel.AppState.PREVIEW) {
                navController.navigate(LayoutFragmentDirections.actionPreviewFragment());
            } else if (state == StateViewModel.AppState.FILE_SELECTION) {
                if (isHost) {
                    navController.navigate(LayoutFragmentDirections.actionFileSelectionFragment());
                } else {
                    navController.navigate(LayoutFragmentDirections.actionFileSelectionWaitFragment());
                }
            }
        });

        return  binding.getRoot();
    }

    @Override
    public void onResume() {
        super.onResume();

        // sync App State - necessary for the Back button to work
//        StateChangeHelper.requestStateChange(
//                mainActivity, connectionViewModel, StateViewModel.AppState.LAYOUT);
    }
}