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

import ka170130.pmu.infinityscreen.AppBarAndStatusHelper;
import ka170130.pmu.infinityscreen.MainActivity;
import ka170130.pmu.infinityscreen.R;
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
        AppBarAndStatusHelper.hideHostCard(binding.appBarAndStatus);

        // Find Devices Button
        binding.findDevicesButton.setOnClickListener(view -> {
            // TODO: replace dummy code with real code
            navController.navigate(HomeFragmentDirections.actionDeviceListFragment());
        });

        return  binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        navController = Navigation.findNavController(view);
    }
}