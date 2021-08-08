package ka170130.pmu.infinityscreen.connection;

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
import ka170130.pmu.infinityscreen.databinding.FragmentConnectedBinding;
import ka170130.pmu.infinityscreen.databinding.FragmentHomeBinding;
import ka170130.pmu.infinityscreen.helpers.StateChangeHelper;
import ka170130.pmu.infinityscreen.viewmodels.StateViewModel;

public class ConnectedFragment extends ConnectionAwareFragment {

    private FragmentConnectedBinding binding;
    private StateViewModel stateViewModel;

    public ConnectedFragment() {
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
        binding = FragmentConnectedBinding.inflate(inflater, container, false);

        // Setup Status Cards
        setupStatusCards(binding.appBarAndStatus);

        // Setup Host Device Name
        connectionViewModel.getHostDevice().observe(getViewLifecycleOwner(), device -> {
            if (device == null) {
                return;
            }

            binding.connectedToHostName.setText(device.getDeviceName());
        });

        // Listen for Disconnect button click
        binding.disconnectButton.setOnClickListener(view -> {
            mainActivity.getConnectionManager().disconnect();
        });

        // Listen for App State change
        stateViewModel.getState().observe(getViewLifecycleOwner(), state -> {
            if (state == StateViewModel.AppState.LAYOUT) {
                navController.navigate(ConnectedFragmentDirections.actionLayoutFragment());
            }
        });

        return  binding.getRoot();
    }

    @Override
    public void onResume() {
        super.onResume();

        // sync App State - necessary for the Back button to work
//        StateChangeHelper.requestStateChange(
//                mainActivity, connectionViewModel, StateViewModel.AppState.CONNECTION);
    }
}