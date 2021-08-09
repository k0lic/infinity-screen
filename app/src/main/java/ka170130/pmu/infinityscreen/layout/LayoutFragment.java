package ka170130.pmu.infinityscreen.layout;

import android.os.Bundle;

import androidx.annotation.Nullable;
import androidx.lifecycle.ViewModelProvider;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import java.util.Iterator;

import ka170130.pmu.infinityscreen.MainActivity;
import ka170130.pmu.infinityscreen.R;
import ka170130.pmu.infinityscreen.connection.ConnectionAwareFragment;
import ka170130.pmu.infinityscreen.containers.DeviceRepresentation;
import ka170130.pmu.infinityscreen.containers.Message;
import ka170130.pmu.infinityscreen.containers.TransformInfo;
import ka170130.pmu.infinityscreen.databinding.FragmentLayoutBinding;
import ka170130.pmu.infinityscreen.helpers.AppBarAndStatusHelper;
import ka170130.pmu.infinityscreen.helpers.StateChangeHelper;
import ka170130.pmu.infinityscreen.viewmodels.LayoutViewModel;
import ka170130.pmu.infinityscreen.viewmodels.StateViewModel;

public class LayoutFragment extends ConnectionAwareFragment {

    private FragmentLayoutBinding binding;
    private StateViewModel stateViewModel;
    private LayoutViewModel layoutViewModel;

    private LayoutManager layoutManager;

    public LayoutFragment() {
        // Required empty public constructor
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        stateViewModel = new ViewModelProvider(mainActivity).get(StateViewModel.class);
        layoutViewModel = new ViewModelProvider(mainActivity).get(LayoutViewModel.class);

        layoutManager = new LayoutManager(mainActivity);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        binding = FragmentLayoutBinding.inflate(inflater, container, false);

        // Setup Status Cards
        setupStatusCards(binding.appBarAndStatus);

        // Inflate Top App Bar Menu
        AppBarAndStatusHelper.inflateMenu(binding.appBarAndStatus, R.menu.app_bar_menu, item -> {
            if (item.getItemId() == R.id.option_disconnect) {
                Boolean isHost = connectionViewModel.getIsHost().getValue();

                if (isHost) {
                    // disconnect all
                    mainActivity.getTaskManager().runBroadcastTask(Message.newDisconnectMessage());
                } else {
                    // disconnect self
                    mainActivity.getConnectionManager().disconnect();
                }
                return true;
            }

            return false;
        });

        // Calculate Transform Info
        TransformInfo selfTransform = layoutManager.calculateSelfTransform();
        layoutViewModel.setSelfTransform(selfTransform);
        layoutManager.reportSelfTransform(selfTransform);

        // Attach Host Transform List listener
        layoutViewModel.getTransformList().observe(getViewLifecycleOwner(),
                list -> layoutManager.hostTransformListListener(list));

        // Listen for Layout Change - update and redraw DeviceLayoutView on change
        layoutViewModel.getTransformList().observe(getViewLifecycleOwner(), list -> {
            Log.d(MainActivity.LOG_TAG, "Update and Redraw DeviceLayoutView");

            DeviceLayoutView deviceLayoutView = binding.deviceLayoutView;

            // set device list
            deviceLayoutView.clearDevices();
            Iterator<TransformInfo> iterator = list.iterator();
            while (iterator.hasNext()) {
                TransformInfo next = iterator.next();
                DeviceRepresentation deviceRep = new DeviceRepresentation(next);
                deviceLayoutView.registerDevice(deviceRep);
            }

            // set self index
            TransformInfo selfAuto = layoutViewModel.getSelfAuto().getValue();
            if (selfAuto != null) {
                deviceLayoutView.setSelf(selfAuto.getNumberId());
            }

            // redraw component
            deviceLayoutView.redraw();
        });

        layoutViewModel.getSelfAuto().observe(getViewLifecycleOwner(), auto -> {
            if (auto == null) {
                return;
            }

            binding.deviceNumber.setText(String.valueOf(auto.getNumberId()));

            // redraw component :(
            binding.deviceLayoutView.setSelf(auto.getNumberId());
            binding.deviceLayoutView.redraw();
        });

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