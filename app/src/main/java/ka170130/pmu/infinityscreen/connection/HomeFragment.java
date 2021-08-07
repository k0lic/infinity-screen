package ka170130.pmu.infinityscreen.connection;

import android.content.Context;
import android.location.LocationManager;
import android.os.Build;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;

import android.provider.Settings;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import ka170130.pmu.infinityscreen.R;
import ka170130.pmu.infinityscreen.containers.PeerInetAddressInfo;
import ka170130.pmu.infinityscreen.containers.PeerInfo;
import ka170130.pmu.infinityscreen.dialogs.SettingsPanelDialog;
import ka170130.pmu.infinityscreen.helpers.AppBarAndStatusHelper;
import ka170130.pmu.infinityscreen.MainActivity;
import ka170130.pmu.infinityscreen.databinding.FragmentHomeBinding;

public class HomeFragment extends Fragment {

    private FragmentHomeBinding binding;
    private MainActivity mainActivity;
    private ConnectionViewModel connectionViewModel;
    private NavController navController;

    public HomeFragment() {
        // Required empty public constructor
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mainActivity = (MainActivity) requireActivity();
        connectionViewModel = new ViewModelProvider(mainActivity).get(ConnectionViewModel.class);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        binding = FragmentHomeBinding.inflate(inflater, container, false);

        // Setup Card Shapes
        AppBarAndStatusHelper.setupCardShapes(binding.appBarAndStatus);

        // Update Device card
        connectionViewModel.getSelfDevice().observe(getViewLifecycleOwner(),
                device -> AppBarAndStatusHelper.refreshDeviceCard(
                        binding.appBarAndStatus,
                        device,
                        getResources(),
                        mainActivity.getTheme()
        ));

        // Host card is not applicable to this fragment since there is no established connection
//        AppBarAndStatusHelper.hideHostCard(binding.appBarAndStatus);

        // Find Devices Button
        binding.findDevicesButton.setOnClickListener(view -> {
            if (mainActivity.checkPeerDiscovery()) {
                // Set View Model Host info
                connectionViewModel.setIsHost(true);
                PeerInfo self = connectionViewModel.getSelfDevice().getValue();
                PeerInetAddressInfo info = new PeerInetAddressInfo(self, null);
                connectionViewModel.setHostDevice(info);

                // navigate to Device List Fragment
                navController.navigate(HomeFragmentDirections.actionDeviceListFragment());
            }
        });

        // Listen for connections
        connectionViewModel.getConnectionStatus().observe(getViewLifecycleOwner(), status -> {
            // ignore for host
            if (connectionViewModel.getIsHost().getValue()) {
                return;
            }

            // if connected navigate to ConnectedFragment
            if (status != ConnectionViewModel.ConnectionStatus.NOT_CONNECTED) {
                navController.navigate(HomeFragmentDirections.actionConnectedFragment());
            }
        });

        return  binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        navController = Navigation.findNavController(view);
    }
}