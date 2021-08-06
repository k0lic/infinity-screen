package ka170130.pmu.infinityscreen.connection;

import android.net.wifi.p2p.WifiP2pInfo;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MediatorLiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.SavedStateHandle;
import androidx.lifecycle.ViewModel;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

import ka170130.pmu.infinityscreen.containers.PeerInetAddressInfo;
import ka170130.pmu.infinityscreen.containers.PeerInfo;

public class ConnectionViewModel extends ViewModel {

    public enum ConnectionStatus { NOT_CONNECTED, CONNECTED_HOST, CONNECTED_CLIENT };

    private SavedStateHandle savedStateHandle;

    // System info
//    private MutableLiveData<Boolean> wifiEnabled; - ONLY SET NEVER READ
//    private MutableLiveData<Boolean> gpsEnabled; - NOT USED

    // Device Info
    private MutableLiveData<PeerInfo> selfDevice;
    private MutableLiveData<PeerInetAddressInfo> hostDevice;
    private MutableLiveData<Boolean> isHost;
    private MediatorLiveData<ConnectionStatus> connectionStatus;
//    private MutableLiveData<PeerInfo> groupOwnerDevice; - NOT USED

    // Connection Info
//    private MutableLiveData<WifiP2pInfo> info; - NOT USED

    // Peer Lists
    private MutableLiveData<Collection<PeerInfo>> discoveryList;
    private MutableLiveData<Collection<PeerInetAddressInfo>> groupList;
    private MediatorLiveData<Collection<PeerInfo>> availableList;

    public ConnectionViewModel(SavedStateHandle savedStateHandle) {
        this.savedStateHandle = savedStateHandle;

        // System Info Initialization
//        wifiEnabled = new MutableLiveData<>();
//        gpsEnabled = new MutableLiveData<>();

        // Device Info Initialization
        selfDevice = new MutableLiveData<>();
        hostDevice = new MutableLiveData<>();
        isHost = new MutableLiveData<>(false);
//        groupOwnerDevice = new MutableLiveData<>();

        connectionStatus = new MediatorLiveData<>();
        connectionStatus.addSource(selfDevice, device -> refreshConnectionStatus());
        connectionStatus.addSource(hostDevice, device -> refreshConnectionStatus());

        // Connection Info Initialization
//        info = new MutableLiveData<>();

        // Peer Lists Initialization
        discoveryList = new MutableLiveData<>(new ArrayList<>());
        groupList = new MutableLiveData<>(new ArrayList<>());

        availableList = new MediatorLiveData<>();
        availableList.addSource(discoveryList, list -> refreshAvailableList());
        availableList.addSource(groupList, list -> refreshAvailableList());
    }

    private void refreshConnectionStatus() {
        PeerInfo selfDevice = this.selfDevice.getValue();
        PeerInetAddressInfo hostDevice = this.hostDevice.getValue();

        if (selfDevice == null || hostDevice == null) {
            connectionStatus.setValue(ConnectionStatus.NOT_CONNECTED);
            return;
        }

        if (selfDevice.getDeviceAddress() != null
                && selfDevice.getDeviceAddress().equals(hostDevice.getDeviceAddress())
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

//    public LiveData<Boolean> getWifiEnabled() {
//        return wifiEnabled;
//    }
//
//    public void setWifiEnabled(boolean wifiEnabled) {
//        this.wifiEnabled.postValue(wifiEnabled);
//    }

//    public LiveData<Boolean> getGpsEnabled() {
//        return gpsEnabled;
//    }
//
//    public void setGpsEnabled(boolean gpsEnabled) {
//        this.gpsEnabled.postValue(gpsEnabled);
//    }

//    public LiveData<WifiP2pInfo> getInfo() {
//        return info;
//    }
//
//    public void setInfo(WifiP2pInfo info) {
//        this.info.postValue(info);
//    }

    public MediatorLiveData<ConnectionStatus> getConnectionStatus() {
        return connectionStatus;
    }

    public LiveData<Boolean> getIsHost() {
        return isHost;
    }

    public void setIsHost(Boolean isHost) {
        this.isHost.postValue(isHost);
    }

    public LiveData<PeerInfo> getSelfDevice() {
        return selfDevice;
    }

    public void setSelfDevice(PeerInfo selfDevice) {
        this.selfDevice.postValue(selfDevice);
    }

    public LiveData<PeerInetAddressInfo> getHostDevice() {
        return hostDevice;
    }

    public void setHostDevice(PeerInetAddressInfo hostDevice) {
        this.hostDevice.postValue(hostDevice);
    }

    public void setDiscoveryList(Collection<PeerInfo> discoveryList) {
        this.discoveryList.postValue(discoveryList);
    }

    public LiveData<Collection<PeerInetAddressInfo>> getGroupList() {
        return groupList;
    }

    public void setGroupList(Collection<PeerInetAddressInfo> groupList) {
        this.groupList.postValue(groupList);
    }

    public void selectDevice(PeerInetAddressInfo device) {
        Collection<PeerInetAddressInfo> devices = groupList.getValue();
        if (!devices.contains(device)) {
            devices.add(device);
        }
        setGroupList(devices);
    }

    public void unselectDevice(PeerInetAddressInfo device) {
        Collection<PeerInetAddressInfo> devices = groupList.getValue();
        if (devices.contains(device)) {
            devices.remove(device);
        }
        setGroupList(devices);
    }

    public LiveData<Collection<PeerInfo>> getAvailableList() {
        return availableList;
    }
}
