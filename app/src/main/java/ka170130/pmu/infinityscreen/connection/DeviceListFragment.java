package ka170130.pmu.infinityscreen.connection;

import android.os.Bundle;

import androidx.annotation.Nullable;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import java.io.IOException;
import java.util.ArrayList;

import ka170130.pmu.infinityscreen.MainActivity;
import ka170130.pmu.infinityscreen.containers.Message;
import ka170130.pmu.infinityscreen.databinding.FragmentDeviceListBinding;
import ka170130.pmu.infinityscreen.helpers.LogHelper;
import ka170130.pmu.infinityscreen.helpers.StateChangeHelper;
import ka170130.pmu.infinityscreen.viewmodels.StateViewModel;

public class DeviceListFragment extends ConnectionAwareFragment {

    private FragmentDeviceListBinding binding;
    private StateViewModel stateViewModel;

    public DeviceListFragment() {
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
//            mainActivity.getTaskManager().runBroadcastTask(Message.newTestMessage());
//            navController.navigate(DeviceListFragmentDirections.actionLayoutFragment());
            StateChangeHelper.requestStateChange(
                    mainActivity, connectionViewModel, StateViewModel.AppState.LAYOUT);
        });

        // Listen for App State change
        stateViewModel.getState().observe(getViewLifecycleOwner(), state -> {
            String s = state == null ? "<NULL>" : state.toString();
            LogHelper.log("Maybe navigate? " + s);
            if (state == StateViewModel.AppState.LAYOUT) {
                LogHelper.log("YES navigate");
                navController.navigate(DeviceListFragmentDirections.actionLayoutFragment());
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