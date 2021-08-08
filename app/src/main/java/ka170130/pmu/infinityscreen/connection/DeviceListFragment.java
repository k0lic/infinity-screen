package ka170130.pmu.infinityscreen.connection;

import android.os.Bundle;

import androidx.recyclerview.widget.LinearLayoutManager;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import java.util.ArrayList;

import ka170130.pmu.infinityscreen.containers.Message;
import ka170130.pmu.infinityscreen.containers.PeerInetAddressInfo;
import ka170130.pmu.infinityscreen.databinding.FragmentDeviceListBinding;

public class DeviceListFragment extends ConnectionAwareFragment {

    private FragmentDeviceListBinding binding;

    public DeviceListFragment() {
        // Required empty public constructor
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        binding = FragmentDeviceListBinding.inflate(inflater, container, false);

        // Setup Status Cards
        setupStatusCards(binding.appBarAndStatus);

        // Discover Peers - in case peers were not discovered once MainActivity was created
        mainActivity.getConnectionManager().discoverPeers();

        // Available Recycler View
        DeviceAvailableAdapter availableAdapter = new DeviceAvailableAdapter(
                mainActivity,
                device -> {
                    if (device == null) {
                        return;
                    }

                    mainActivity.getConnectionManager().connect(device);
                }
        );
        binding.availableRecyclerView.setHasFixedSize(false);
        binding.availableRecyclerView.setAdapter(availableAdapter);
        binding.availableRecyclerView.setLayoutManager(new LinearLayoutManager(mainActivity));

        connectionViewModel.getAvailableList().observe(getViewLifecycleOwner(), collection -> {
            availableAdapter.setDevices(new ArrayList<>(collection));

            if (collection.size() == 0) {
                binding.availableEmpty.setVisibility(View.VISIBLE);
            } else {
                binding.availableEmpty.setVisibility(View.GONE);
            }
        });

        // Connected Recycler View
        DeviceConnectedAdapter connectedAdapter = new DeviceConnectedAdapter(
                mainActivity,
                device -> {
                    if (device == null) {
                        return;
                    }

                    mainActivity.getTaskManager()
                            .runSenderTask(device.getInetAddress(), Message.newDisconnectMessage());

                    connectionViewModel.unselectDevice(device);
                }
        );
        binding.connectedRecyclerView.setHasFixedSize(false);
        binding.connectedRecyclerView.setAdapter(connectedAdapter);
        binding.connectedRecyclerView.setLayoutManager(new LinearLayoutManager(mainActivity));

        connectionViewModel.getGroupList().observe(getViewLifecycleOwner(), collection -> {
            connectedAdapter.setDevices(new ArrayList<>(collection));

            binding.connectedCount.setText(String.valueOf(collection.size()));

            if (collection.size() == 0) {
                binding.connectedEmpty.setVisibility(View.VISIBLE);
            } else {
                binding.connectedEmpty.setVisibility(View.GONE);
            }
        });

        // Continue Button
        binding.continueButton.setOnClickListener(view -> {
            // TODO: replace dummy code with real code
            navController.navigate(DeviceListFragmentDirections.actionLayoutFragment());
        });

        return  binding.getRoot();
    }

    // Don't react to connection changes

}