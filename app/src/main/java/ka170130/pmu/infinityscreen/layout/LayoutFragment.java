package ka170130.pmu.infinityscreen.layout;

import android.os.Bundle;

import androidx.annotation.Nullable;
import androidx.lifecycle.ViewModelProvider;

import android.os.Handler;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.google.android.exoplayer2.text.span.TextAnnotation;

import java.io.IOException;
import java.net.InetAddress;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

import ka170130.pmu.infinityscreen.R;
import ka170130.pmu.infinityscreen.connection.ConnectionAwareFragment;
import ka170130.pmu.infinityscreen.containers.DeviceRepresentation;
import ka170130.pmu.infinityscreen.containers.Message;
import ka170130.pmu.infinityscreen.containers.PeerInetAddressInfo;
import ka170130.pmu.infinityscreen.containers.TransformInfo;
import ka170130.pmu.infinityscreen.containers.TransformUpdate;
import ka170130.pmu.infinityscreen.databinding.FragmentLayoutBinding;
import ka170130.pmu.infinityscreen.helpers.AppBarAndStatusHelper;
import ka170130.pmu.infinityscreen.helpers.LogHelper;
import ka170130.pmu.infinityscreen.helpers.StateChangeHelper;
import ka170130.pmu.infinityscreen.viewmodels.LayoutViewModel;
import ka170130.pmu.infinityscreen.viewmodels.StateViewModel;
import ka170130.pmu.infinityscreen.viewmodels.SyncViewModel;

public class LayoutFragment extends ConnectionAwareFragment {

    private static int DEFERRED_UPDATE_DELAY = 200;

    private FragmentLayoutBinding binding;
    private StateViewModel stateViewModel;
    private LayoutViewModel layoutViewModel;
    private SyncViewModel syncViewModel;

    private LayoutManager layoutManager;

    private Handler handler;
    private final Runnable deferredLayoutUpdate = new Runnable() {
        @Override
        public void run() {
            ArrayList<TransformInfo> transformList = layoutViewModel.getTransformList().getValue();
            ArrayList<TransformInfo> updatedList = new ArrayList<>();

            Iterator<DeviceRepresentation> repIt =
                    binding.deviceLayoutView.getDevices().iterator();
            // Iterate through Device Representation list
            while (repIt.hasNext()) {
                DeviceRepresentation next = repIt.next();

                boolean updated = false;
                Iterator<TransformInfo> transformIt = transformList.iterator();
                // Iterate through Transform Info list
                while (transformIt.hasNext() && !updated) {
                    TransformInfo transform = transformIt.next();
                    // Match by Number ID
                    if (transform.getNumberId() == next.getNumberId()) {
                        // Update element
                        TransformInfo update = new TransformInfo(
                                transform.getDeviceName(),
                                next.getNumberId(),
                                transform.getOrientation(),     // TODO: orientation from DeviceRepresentation
                                next.getWidth(),
                                next.getHeight()
                        );
                        update.setPosition(new DeviceRepresentation.Position(next.getPosition()));

                        updatedList.add(update);

                        updated = true;
                    }
                }
            }

            // Report update
            Boolean isHost = connectionViewModel.getIsHost().getValue();
            if (isHost) {
                // Update list
                layoutViewModel.setTransformList(updatedList);
            } else {
                InetAddress hostAddress =
                        connectionViewModel.getHostDevice().getValue().getInetAddress();

                // Send List Update to host
                try {
                    Message transformListUpdate = Message.newTransformListUpdateMessage(
                            new TransformUpdate(updatedList, false));
                    mainActivity.getTaskManager().
                            runSenderTask(hostAddress, transformListUpdate);
                } catch (IOException e) {
                    LogHelper.error(e);
                }
            }
        }
    };
    private Runnable deferredViewportUpdate = new Runnable() {
        @Override
        public void run() {
            TransformInfo viewport = layoutViewModel.getViewport().getValue();
            DeviceRepresentation viewportRep = binding.deviceLayoutView.getViewport();

            TransformInfo update = new TransformInfo(
                    viewport.getDeviceName(),
                    viewport.getNumberId(),
                    viewport.getOrientation(),
                    viewportRep.getWidth(),
                    viewportRep.getHeight()
            );
            update.setPosition(new DeviceRepresentation.Position(viewportRep.getPosition()));

            // Report update
            Boolean isHost = connectionViewModel.getIsHost().getValue();
            if (isHost) {
                // Update viewport
                layoutViewModel.setViewport(update);
            } else {
                InetAddress hostAddress =
                        connectionViewModel.getHostDevice().getValue().getInetAddress();

                // Send Viewport Update to host
                try {
                    ArrayList<TransformInfo> oneElementList = new ArrayList<>();
                    oneElementList.add(update);

                    Message viewportUpdate = Message.newViewportUpdateMessage(
                            new TransformUpdate(oneElementList, false));
                    mainActivity.getTaskManager().
                            runSenderTask(hostAddress, viewportUpdate);
                } catch (IOException e) {
                    LogHelper.error(e);
                }
            }
        }
    };

    public LayoutFragment() {
        // Required empty public constructor
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        stateViewModel = new ViewModelProvider(mainActivity).get(StateViewModel.class);
        layoutViewModel = new ViewModelProvider(mainActivity).get(LayoutViewModel.class);
        syncViewModel = new ViewModelProvider(mainActivity).get(SyncViewModel.class);

        layoutManager = mainActivity.getLayoutManager();

        handler = new Handler(mainActivity.getMainLooper());
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        binding = FragmentLayoutBinding.inflate(inflater, container, false);

        Boolean isHost = connectionViewModel.getIsHost().getValue();

        // Setup Status Cards
        setupStatusCards(binding.appBarAndStatus);

        // Inflate Top App Bar Menu
        AppBarAndStatusHelper.inflateMenu(binding.appBarAndStatus, R.menu.layout_app_bar_menu, item -> {
            switch (item.getItemId()) {
                case R.id.option_reset:
                    if (isHost) {
                        layoutViewModel.restoreBackups();
                    } else {
                        // Fetch backups
                        ArrayList<TransformInfo> backupTransformList =
                                layoutViewModel.getBackupTransformList().getValue();
                        TransformInfo backupViewport =
                                layoutViewModel.getBackupViewport().getValue();
                        ArrayList<TransformInfo> backupViewportList = new ArrayList<>();
                        backupViewportList.add(backupViewport);

                        InetAddress hostAddress =
                                connectionViewModel.getHostDevice().getValue().getInetAddress();
                        try {
                            // Send updates to host
                            Message transformListUpdate = Message.newTransformListUpdateMessage(
                                    new TransformUpdate(backupTransformList, false));
                            mainActivity.getTaskManager().
                                    runSenderTask(hostAddress, transformListUpdate);
                            Message viewportUpdate = Message.newViewportUpdateMessage(
                                    new TransformUpdate(backupViewportList, false));
                            mainActivity.getTaskManager().
                                    runSenderTask(hostAddress, viewportUpdate);
                        } catch (IOException e) {
                            LogHelper.error(e);
                        }
                    }
                    return true;
                case R.id.option_disconnect:
                    if (isHost) {
                        // disconnect all
                        mainActivity.getTaskManager()
                                .sendToAllInGroup(Message.newDisconnectMessage(), true);
                    } else {
                        // disconnect self
                        mainActivity.getConnectionManager().disconnect();
                    }
                    return true;
            }

            return false;
        });

        // Change Layout Mode Button
        binding.changeLayoutModeButton.setOnClickListener(view -> {
            // Change Focus in Device Layout View
            boolean focusViewport = binding.deviceLayoutView.isFocusViewport();
            binding.deviceLayoutView.changeFocus(!focusViewport);

            // Redraw component
            binding.deviceLayoutView.redraw();

            // Change Image Button Drawable
            int drawableId = focusViewport ?
                    R.drawable.crop_free_24 :
                    R.drawable.phone_android_24;
            binding.changeLayoutModeButton.setImageResource(drawableId);
        });

        // Preview Button
        binding.previewButton.setOnClickListener(view -> {
            StateChangeHelper.requestStateChange(
                    mainActivity, connectionViewModel, StateViewModel.AppState.PREVIEW);
        });

        // Continue Button
        binding.continueButton.setOnClickListener(view -> {
            StateChangeHelper.requestStateChange(
                    mainActivity, connectionViewModel, StateViewModel.AppState.FILE_SELECTION);
        });

        // Calculate Transform Info
        TransformInfo selfTransform = layoutManager.calculateSelfTransform();
        layoutViewModel.setSelfTransform(selfTransform);
        layoutManager.reportSelfTransform(selfTransform);

        // Initialize SyncViewModel SyncInfoList
        Collection<PeerInetAddressInfo> group = connectionViewModel.getGroupList().getValue();
        ArrayList<String> deviceNameList = new ArrayList<>();
        Iterator<PeerInetAddressInfo> groupIter = group.iterator();
        while (groupIter.hasNext()) {
            PeerInetAddressInfo next = groupIter.next();
            deviceNameList.add(next.getDeviceName());
        }
        syncViewModel.initSyncInfoList(deviceNameList);

        // Attach Host Listeners - broadcast layout changes
        if (isHost) {
            layoutViewModel.getTransformList().observe(getViewLifecycleOwner(),
                    list -> layoutManager.hostTransformListListener(list));

            layoutViewModel.getBackupTransformList().observe(getViewLifecycleOwner(),
                    list -> layoutManager.hostBackupTransformListListener(list));

            layoutViewModel.getViewport().observe(getViewLifecycleOwner(),
                    viewport -> layoutManager.hostViewportListener(viewport));

            layoutViewModel.getBackupViewport().observe(getViewLifecycleOwner(),
                    viewport -> layoutManager.hostBackupViewportListener(viewport));
        }

        // Device Layout View Callbacks
        binding.deviceLayoutView.setDeviceCallback(device -> {
            handler.removeCallbacks(deferredLayoutUpdate);
            handler.postDelayed(deferredLayoutUpdate, DEFERRED_UPDATE_DELAY);

            binding.deviceLayoutView.redraw();
        });

        binding.deviceLayoutView.setViewportCallback(device -> {
            handler.removeCallbacks(deferredViewportUpdate);
            handler.postDelayed(deferredViewportUpdate, DEFERRED_UPDATE_DELAY);

            binding.deviceLayoutView.redraw();
        });

        // Listen for Layout Change - update and redraw DeviceLayoutView on change
        layoutViewModel.getTransformList().observe(getViewLifecycleOwner(), list -> {
            LogHelper.log("Update and Redraw DeviceLayoutView");

            DeviceLayoutView deviceLayoutView = binding.deviceLayoutView;

            // set device list
            deviceLayoutView.clearDevices();
            Iterator<TransformInfo> iterator = list.iterator();
            while (iterator.hasNext()) {
                TransformInfo next = iterator.next();
                DeviceRepresentation deviceRep = new DeviceRepresentation(next);
                deviceLayoutView.registerDevice(deviceRep);
            }

            // redraw component
            deviceLayoutView.redraw();
        });

        layoutViewModel.getViewport().observe(getViewLifecycleOwner(), viewport -> {
            if (viewport == null) {
                return;
            }

            // Repackage viewport
            DeviceRepresentation viewportRep = new DeviceRepresentation(viewport);
            binding.deviceLayoutView.setViewport(viewportRep);

            // redraw component
            binding.deviceLayoutView.redraw();
        });

        layoutViewModel.getSelfAuto().observe(getViewLifecycleOwner(), auto -> {
            if (auto == null) {
                return;
            }

            binding.deviceNumber.setText(String.valueOf(auto.getNumberId()));

            if (binding.deviceLayoutView.getSelf() != auto.getNumberId()) {
                // redraw component
                binding.deviceLayoutView.setSelf(auto.getNumberId());
                binding.deviceLayoutView.redraw();
            }
        });

        // Listen for App State change
        stateViewModel.getState().observe(getViewLifecycleOwner(), state -> {
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