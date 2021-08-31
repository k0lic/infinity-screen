package ka170130.pmu.infinityscreen.viewmodels;

import android.net.wifi.p2p.WifiP2pInfo;
import android.util.Log;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MediatorLiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.SavedStateHandle;
import androidx.lifecycle.ViewModel;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.logging.Handler;

import ka170130.pmu.infinityscreen.MainActivity;
import ka170130.pmu.infinityscreen.containers.PeerInetAddressInfo;
import ka170130.pmu.infinityscreen.containers.PeerInfo;
import ka170130.pmu.infinityscreen.helpers.LogHelper;
import ka170130.pmu.infinityscreen.helpers.ThreadHelper;

public class ConnectionViewModel extends ViewModel implements Resettable {

    private static final String SELF_DEVICE_KEY = "connection-self-device-key";
    private static final String HOST_DEVICE_KEY = "connection-host-device-key";
    private static final String IS_HOST_KEY = "connection-is-host-key";
    private static final String GROUP_LIST_KEY = "connection-group-list-key";

    public enum ConnectionStatus { NOT_CONNECTED, CONNECTED_HOST, CONNECTED_CLIENT };

    private SavedStateHandle savedStateHandle;

    // Device Info
    private MutableLiveData<PeerInfo> selfDevice;
    private MutableLiveData<PeerInetAddressInfo> hostDevice;
    private MutableLiveData<Boolean> isHost;
    private MediatorLiveData<ConnectionStatus> connectionStatus;

    // Peer Lists
    private MutableLiveData<Collection<PeerInfo>> discoveryList;
    private MutableLiveData<Collection<PeerInetAddressInfo>> groupList;
    private MediatorLiveData<Collection<PeerInfo>> availableList;

    public ConnectionViewModel(SavedStateHandle savedStateHandle) {
        this.savedStateHandle = savedStateHandle;

        // Device Info Initialization
        selfDevice = savedStateHandle.getLiveData(SELF_DEVICE_KEY, null);
        hostDevice = savedStateHandle.getLiveData(HOST_DEVICE_KEY, null);
        isHost = savedStateHandle.getLiveData(IS_HOST_KEY, false);

        LogHelper.log("Device Info Init: "
                + (selfDevice.getValue() == null ? "<NULL>" : selfDevice.getValue().getDeviceName()) + " "
                + (hostDevice.getValue() == null ? "<NULL>" : hostDevice.getValue().getDeviceName()) + " "
                + isHost.getValue());

        connectionStatus = new MediatorLiveData<>();
        connectionStatus.addSource(selfDevice, device -> refreshConnectionStatus());
        connectionStatus.addSource(hostDevice, device -> refreshConnectionStatus());

        // Peer Lists Initialization
        discoveryList = new MutableLiveData<>(new ArrayList<>());
        groupList = savedStateHandle.getLiveData(GROUP_LIST_KEY, new ArrayList<>());

        availableList = new MediatorLiveData<>();
        availableList.addSource(discoveryList, list -> refreshAvailableList());
        availableList.addSource(groupList, list -> refreshAvailableList());
    }

    private void refreshConnectionStatus() {
        PeerInfo selfDevice = this.selfDevice.getValue();
        PeerInetAddressInfo hostDevice = this.hostDevice.getValue();

        LogHelper.log("Device Info connection refresh: "
                + (selfDevice == null ? "<NULL>" : selfDevice.getDeviceName()) + " "
                + (hostDevice == null ? "<NULL>" : hostDevice.getDeviceName()));

        if (selfDevice == null || hostDevice == null) {
            connectionStatus.setValue(ConnectionStatus.NOT_CONNECTED);
            return;
        }

        if (selfDevice.getDeviceName() != null
                && selfDevice.getDeviceName().equals(hostDevice.getDeviceName())
        ) {
            connectionStatus.setValue(ConnectionStatus.CONNECTED_HOST);
            return;
        }

        connectionStatus.setValue(ConnectionStatus.CONNECTED_CLIENT);
    }

    private void refreshAvailableList() {
        List<PeerInfo> devices = new ArrayList<>();

        Collection<PeerInfo> dList = discoveryList.getValue();
        Iterator<PeerInfo> dit = dList.iterator();
        Collection<PeerInetAddressInfo> group = groupList.getValue();

        while (dit.hasNext()) {
            PeerInfo next = dit.next();
            Iterator<PeerInetAddressInfo> git = group.iterator();

            boolean contains = false;
            while (!contains && git.hasNext()) {
                PeerInetAddressInfo groupMember = git.next();
                contains = next.getDeviceAddress().equals(groupMember.getDeviceAddress());
            }

            if (!contains) {
                devices.add(next);
            }
        }

        availableList.setValue(devices);
    }

    @Override
    public void reset() {
        setHostDevice(null);
        setIsHost(false);
        setGroupList(new ArrayList<>());
    }

    public MediatorLiveData<ConnectionStatus> getConnectionStatus() {
        return connectionStatus;
    }

    public LiveData<Boolean> getIsHost() {
        return isHost;
    }

    public void setIsHost(Boolean isHost) {
        ThreadHelper.runOnMainThread(() -> savedStateHandle.set(IS_HOST_KEY, isHost));
    }

    public LiveData<PeerInfo> getSelfDevice() {
        return selfDevice;
    }

    public void setSelfDevice(PeerInfo selfDevice) {
        ThreadHelper.runOnMainThread(() -> savedStateHandle.set(SELF_DEVICE_KEY, selfDevice));
    }

    public LiveData<PeerInetAddressInfo> getHostDevice() {
        return hostDevice;
    }

    public void setHostDevice(PeerInetAddressInfo hostDevice) {
        ThreadHelper.runOnMainThread(() -> savedStateHandle.set(HOST_DEVICE_KEY, hostDevice));
    }

    public void setDiscoveryList(Collection<PeerInfo> discoveryList) {
        this.discoveryList.postValue(discoveryList);
    }

    public LiveData<Collection<PeerInetAddressInfo>> getGroupList() {
        return groupList;
    }

    public void setGroupList(Collection<PeerInetAddressInfo> groupList) {
        ThreadHelper.runOnMainThread(() -> savedStateHandle.set(GROUP_LIST_KEY, groupList));
    }

    public void selectDevice(PeerInetAddressInfo device) {
        Collection<PeerInetAddressInfo> devices = groupList.getValue();

        boolean contains = false;
        Iterator<PeerInetAddressInfo> iterator = devices.iterator();
        while (!contains && iterator.hasNext()) {
            PeerInetAddressInfo next = iterator.next();
            contains = device.getDeviceAddress().equals(next.getDeviceAddress());
        }

        if (!contains) {
            devices.add(device);
            LogHelper.log("Added to group: " + device.getDeviceName() + " " + device.getDeviceAddress());
        }

        setGroupList(devices);
    }

    public void unselectDevice(PeerInetAddressInfo device) {
        Collection<PeerInetAddressInfo> devices = groupList.getValue();

        Iterator<PeerInetAddressInfo> iterator = devices.iterator();
        while (iterator.hasNext()) {
            PeerInetAddressInfo next = iterator.next();
            if (device.getDeviceAddress().equals(next.getDeviceAddress())) {
                devices.remove(next);
                break;
            }
        }

        setGroupList(devices);
    }

    public LiveData<Collection<PeerInfo>> getAvailableList() {
        return availableList;
    }
}
