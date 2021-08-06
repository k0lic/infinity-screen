package ka170130.pmu.infinityscreen.connection;

import android.Manifest;
import android.annotation.SuppressLint;
import android.net.wifi.p2p.WifiP2pManager;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import androidx.recyclerview.widget.LinearLayoutManager;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import java.util.ArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import ka170130.pmu.infinityscreen.MainActivity;
import ka170130.pmu.infinityscreen.R;
import ka170130.pmu.infinityscreen.databinding.FragmentDeviceListBinding;
import ka170130.pmu.infinityscreen.databinding.FragmentHomeBinding;
import ka170130.pmu.infinityscreen.helpers.PermissionsHelper;

public class DeviceListFragment extends Fragment {

    private FragmentDeviceListBinding binding;
    private MainActivity mainActivity;
    private ConnectionViewModel connectionViewModel;
    private NavController navController;

    public DeviceListFragment() {
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
        binding = FragmentDeviceListBinding.inflate(inflater, container, false);

        // TODO
        // Available Recycler View
        DeviceAdapter availableAdapter = new DeviceAdapter(
                mainActivity,
                false,
                device -> {
                    mainActivity.getConnectionManager().connect(device);
                }
        );
        binding.availableRecyclerView.setHasFixedSize(false);
        binding.availableRecyclerView.setAdapter(availableAdapter);
        binding.availableRecyclerView.setLayoutManager(new LinearLayoutManager(mainActivity));

        connectionViewModel.getAvailableList().observe(getViewLifecycleOwner(), collection -> {
            availableAdapter.setDevices(new ArrayList<>(collection));
        });

        // Connected Recycler View
        DeviceAdapter connectedAdapter = new DeviceAdapter(
                mainActivity,
                true,
                null
        );
        binding.connectedRecyclerView.setHasFixedSize(false);
        binding.connectedRecyclerView.setAdapter(connectedAdapter);
        binding.connectedRecyclerView.setLayoutManager(new LinearLayoutManager(mainActivity));

        connectionViewModel.getGroupList().observe(getViewLifecycleOwner(), collection -> {
            connectedAdapter.setDevices(new ArrayList<>(collection));
        });

        // Continue Button
        binding.continueButton.setOnClickListener(view -> {
            // TODO: replace dummy code with real code
            navController.navigate(DeviceListFragmentDirections.actionLayoutFragment());
        });

        return  binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        navController = Navigation.findNavController(view);
    }
}