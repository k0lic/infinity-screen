package ka170130.pmu.infinityscreen.viewmodels;

import androidx.lifecycle.SavedStateHandle;
import androidx.lifecycle.ViewModel;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import ka170130.pmu.infinityscreen.sync.SyncInfo;

public class SyncViewModel extends ViewModel implements Resettable {

    private static final String SYNC_LIST_KEY = "sync-sync-list-key";

    private SavedStateHandle savedStateHandle;

    private ArrayList<SyncInfo> syncInfoList;

    public SyncViewModel(SavedStateHandle savedStateHandle) {
        this.savedStateHandle = savedStateHandle;

        syncInfoList = savedStateHandle.get(SYNC_LIST_KEY);
        if (syncInfoList == null) {
            syncInfoList = new ArrayList<>();
        }
    }

    @Override
    public void reset() {
        setSyncInfoList(new ArrayList<>());
    }

    public ArrayList<SyncInfo> getSyncInfoList() {
        return syncInfoList;
    }

    public void setSyncInfoList(ArrayList<SyncInfo> syncInfoList) {
        this.syncInfoList = syncInfoList;
        savedStateHandle.set(SYNC_LIST_KEY, syncInfoList);
    }

    public void initSyncInfoList(List<String> deviceAddressList) {
        ArrayList<SyncInfo> list = new ArrayList<>();

        Iterator<String> iterator = deviceAddressList.iterator();
        while (iterator.hasNext()) {
            String deviceAddress = iterator.next();
            list.add(new SyncInfo(deviceAddress));
        }

        setSyncInfoList(list);
    }

    public void updateSyncInfoListElement(String deviceAddress, long clockDiff) {
        ArrayList<SyncInfo> list = syncInfoList;

        Iterator<SyncInfo> iterator = list.iterator();
        boolean updated = false;
        // Find element with matching address
        while (iterator.hasNext() && !updated) {
            SyncInfo next = iterator.next();
            if (next.getDeviceAddress().equals(deviceAddress)) {
                // Update element with matching address
                next.update(clockDiff);
                updated = true;
            }
        }

        if (updated) {
            // Necessary to update the SavedStateHandle
            setSyncInfoList(list);
        }
    }

    public SyncInfo getSyncInfoListElement(String deviceAddress) {
        Iterator<SyncInfo> iterator = syncInfoList.iterator();
        while (iterator.hasNext()) {
            SyncInfo next = iterator.next();
            if (next.getDeviceAddress().equals(deviceAddress)) {
                return next;
            }
        }

        return null;
    }
}
