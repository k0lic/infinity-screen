package ka170130.pmu.infinityscreen.connection;

import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;

import android.util.Log;
import android.view.View;

import ka170130.pmu.infinityscreen.MainActivity;
import ka170130.pmu.infinityscreen.databinding.AppBarAndStatusBinding;
import ka170130.pmu.infinityscreen.helpers.AppBarAndStatusHelper;
import ka170130.pmu.infinityscreen.helpers.LogHelper;
import ka170130.pmu.infinityscreen.viewmodels.ConnectionViewModel;

public class ConnectionAwareFragment extends Fragment {

    protected MainActivity mainActivity;
    protected ConnectionViewModel connectionViewModel;
    protected NavController navController;

    public ConnectionAwareFragment() {
        // Required empty public constructor
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mainActivity = (MainActivity) requireActivity();
        connectionViewModel = new ViewModelProvider(mainActivity).get(ConnectionViewModel.class);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        navController = Navigation.findNavController(view);

        // Listen for connection
        connectionViewModel.getConnectionStatus().observe(getViewLifecycleOwner(), status -> {
            // Navigate to HomeFragment if disconnected
            if (status == ConnectionViewModel.ConnectionStatus.NOT_CONNECTED) {
                LogHelper.log("ConnectionStatus.NOT_CONNECTED detected");
                navController.navigate(HomeFragmentDirections.globalHomeFragment());
            }
        });
    }

    protected void setupStatusCards(AppBarAndStatusBinding binding) {
        // Set Status Card Background drawables
        AppBarAndStatusHelper.setupCardShapes(binding);

        // Set Host Card Background drawable color
        AppBarAndStatusHelper.showHostCard(binding, mainActivity.getTheme());

        connectionViewModel.getSelfDevice().observe(getViewLifecycleOwner(), device -> {
            if (device == null) {
                return;
            }

            // Set Device Card Content and Style
            AppBarAndStatusHelper
                    .refreshDeviceCard(binding, device, getResources(), mainActivity.getTheme());
        });

        connectionViewModel.getIsHost().observe(getViewLifecycleOwner(), isHost -> {
            if (isHost) {
                AppBarAndStatusHelper.hideHostDeviceName(binding);
            } else {
                AppBarAndStatusHelper.showHostDeviceName(binding);
            }
        });

        connectionViewModel.getHostDevice().observe(getViewLifecycleOwner(), device -> {
            if (device == null) {
                return;
            }

            AppBarAndStatusHelper.setHostCardContent(binding, device);
        });
    }
}