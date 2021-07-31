package ka170130.pmu.infinityscreen;

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

public class WifiDeviceViewModel extends ViewModel {

    private SavedStateHandle savedStateHandle;

    private MutableLiveData<Collection<WifiP2pDevice>> discoveryList;
    private MutableLiveData<Collection<WifiP2pDevice>> selectedList;
    private MediatorLiveData<Collection<WifiP2pDevice>> availableList;

    public WifiDeviceViewModel(SavedStateHandle savedStateHandle) {
        this.savedStateHandle = savedStateHandle;

        discoveryList = new MutableLiveData<>(new ArrayList<>());
        selectedList = new MutableLiveData<>(new ArrayList<>());

        availableList = new MediatorLiveData<>();
        availableList.addSource(discoveryList, list -> {
            refreshAvailableList();
        });
        availableList.addSource(selectedList, list -> {
            refreshAvailableList();
        });
    }

    private void refreshAvailableList() {
        List<WifiP2pDevice> devices = new ArrayList<>();

        Collection<WifiP2pDevice> dList = discoveryList.getValue();
        Iterator<WifiP2pDevice> dit = dList.iterator();
        Collection<WifiP2pDevice> sList = selectedList.getValue();

        while (dit.hasNext()) {
            WifiP2pDevice next = dit.next();
            if (!sList.contains(next)) {
                devices.add(next);
            }
        }

        availableList.setValue(devices);
    }

    public LiveData<Collection<WifiP2pDevice>> getDiscoveryList() {
        return discoveryList;
    }

    public void setDiscoveryList(Collection<WifiP2pDevice> discoveryList) {
        this.discoveryList.setValue(discoveryList);
    }

    public LiveData<Collection<WifiP2pDevice>> getSelectedList() {
        return selectedList;
    }

    public void setSelectedList(Collection<WifiP2pDevice> selectedList) {
        this.selectedList.setValue(selectedList);
    }

    public void selectDevice(WifiP2pDevice device) {
        Collection<WifiP2pDevice> devices = selectedList.getValue();
        if (!devices.contains(device)) {
            devices.add(device);
        }
        setSelectedList(devices);
    }

    public void unselectDevice(WifiP2pDevice device) {
        Collection<WifiP2pDevice> devices = selectedList.getValue();
        if (devices.contains(device)) {
            devices.remove(device);
        }
        setSelectedList(devices);
    }

    public LiveData<Collection<WifiP2pDevice>> getAvailableList() {
        return availableList;
    }
}
