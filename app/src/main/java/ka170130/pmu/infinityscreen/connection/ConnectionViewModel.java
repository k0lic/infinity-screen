package ka170130.pmu.infinityscreen.connection;

import android.net.wifi.p2p.WifiP2pDevice;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MediatorLiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.SavedStateHandle;
import androidx.lifecycle.ViewModel;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

public class ConnectionViewModel extends ViewModel {

    public enum ConnectionStatus { NOT_CONNECTED, CONNECTED_HOST, CONNECTED_CLIENT };

    private SavedStateHandle savedStateHandle;

    // Device Info
    private MutableLiveData<WifiP2pDevice> selfDevice;
    private MutableLiveData<WifiP2pDevice> hostDevice;
    private MediatorLiveData<ConnectionStatus> connectionStatus;
    private MutableLiveData<WifiP2pDevice> groupOwnerDevice;

    // Peer Lists
    private MutableLiveData<Collection<WifiP2pDevice>> discoveryList;
    private MutableLiveData<Collection<WifiP2pDevice>> groupList;
    private MediatorLiveData<Collection<WifiP2pDevice>> availableList;

    public ConnectionViewModel(SavedStateHandle savedStateHandle) {
        this.savedStateHandle = savedStateHandle;

        // Device Info Initialization
        selfDevice = new MutableLiveData<>();
        hostDevice = new MutableLiveData<>();
        groupOwnerDevice = new MutableLiveData<>();

        connectionStatus = new MediatorLiveData<>();
        connectionStatus.addSource(selfDevice, device -> refreshConnectionStatus());
        connectionStatus.addSource(hostDevice, device -> refreshConnectionStatus());

        // Peer Lists Initialization
        discoveryList = new MutableLiveData<>(new ArrayList<>());
        groupList = new MutableLiveData<>(new ArrayList<>());

        availableList = new MediatorLiveData<>();
        availableList.addSource(discoveryList, list -> refreshAvailableList());
        availableList.addSource(groupList, list -> refreshAvailableList());
    }

    private void refreshConnectionStatus() {
        WifiP2pDevice selfDevice = this.selfDevice.getValue();
        WifiP2pDevice hostDevice = this.hostDevice.getValue();

        if (selfDevice == null || hostDevice == null) {
            connectionStatus.setValue(ConnectionStatus.NOT_CONNECTED);
            return;
        }

        if (selfDevice.deviceAddress != null
                && selfDevice.deviceAddress.equals(hostDevice.deviceAddress)
        ) {
            connectionStatus.setValue(ConnectionStatus.CONNECTED_HOST);
            return;
        }

        connectionStatus.setValue(ConnectionStatus.CONNECTED_CLIENT);
    }

    private void refreshAvailableList() {
        List<WifiP2pDevice> devices = new ArrayList<>();

        Collection<WifiP2pDevice> dList = discoveryList.getValue();
        Iterator<WifiP2pDevice> dit = dList.iterator();
        Collection<WifiP2pDevice> sList = groupList.getValue();

        while (dit.hasNext()) {
            WifiP2pDevice next = dit.next();
            if (!sList.contains(next)) {
                devices.add(next);
            }
        }

        availableList.setValue(devices);
    }

    public LiveData<WifiP2pDevice> getSelfDevice() {
        return selfDevice;
    }

    public void setSelfDevice(WifiP2pDevice selfDevice) {
        this.selfDevice.setValue(selfDevice);
    }

    public LiveData<Collection<WifiP2pDevice>> getDiscoveryList() {
        return discoveryList;
    }

    public void setDiscoveryList(Collection<WifiP2pDevice> discoveryList) {
        this.discoveryList.setValue(discoveryList);
    }

    public LiveData<Collection<WifiP2pDevice>> getGroupList() {
        return groupList;
    }

    public void setGroupList(Collection<WifiP2pDevice> groupList) {
        this.groupList.setValue(groupList);
    }

    public void selectDevice(WifiP2pDevice device) {
        Collection<WifiP2pDevice> devices = groupList.getValue();
        if (!devices.contains(device)) {
            devices.add(device);
        }
        setGroupList(devices);
    }

    public void unselectDevice(WifiP2pDevice device) {
        Collection<WifiP2pDevice> devices = groupList.getValue();
        if (devices.contains(device)) {
            devices.remove(device);
        }
        setGroupList(devices);
    }

    public LiveData<Collection<WifiP2pDevice>> getAvailableList() {
        return availableList;
    }
}
