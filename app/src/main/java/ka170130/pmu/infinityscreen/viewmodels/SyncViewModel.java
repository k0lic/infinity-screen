package ka170130.pmu.infinityscreen.viewmodels;

import androidx.lifecycle.SavedStateHandle;
import androidx.lifecycle.ViewModel;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import ka170130.pmu.infinityscreen.sync.SyncInfo;

public class SyncViewModel extends ViewModel implements Resettable {

    private static final String SYNC_LIST_KEY = "sync-sync-list-key";
    private static final String AVERAGE_ROUND_TRIP_KEY = "sync-average-round-trip-time-key";

    private static final double FACTOR = 0.15;

    private SavedStateHandle savedStateHandle;

    private ArrayList<SyncInfo> syncInfoList;
    private Long averageRoundTripTime;

    public SyncViewModel(SavedStateHandle savedStateHandle) {
        this.savedStateHandle = savedStateHandle;

        syncInfoList = savedStateHandle.get(SYNC_LIST_KEY);
        if (syncInfoList == null) {
            syncInfoList = new ArrayList<>();
        }

        averageRoundTripTime = savedStateHandle.get(AVERAGE_ROUND_TRIP_KEY);
        if (averageRoundTripTime == null) {
            averageRoundTripTime = 0L;
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

    public void initSyncInfoList(List<String> deviceNameList) {
        ArrayList<SyncInfo> list = new ArrayList<>();

        Iterator<String> iterator = deviceNameList.iterator();
        while (iterator.hasNext()) {
            String deviceName = iterator.next();
            list.add(new SyncInfo(deviceName));
        }

        setSyncInfoList(list);
    }

    public void updateSyncInfoListElement(String deviceName, long clockDiff) {
        ArrayList<SyncInfo> list = syncInfoList;

        Iterator<SyncInfo> iterator = list.iterator();
        boolean updated = false;
        // Find element with matching address
        while (iterator.hasNext() && !updated) {
            SyncInfo next = iterator.next();
            if (next.getDeviceName().equals(deviceName)) {
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

    public SyncInfo getSyncInfoListElement(String deviceName) {
        Iterator<SyncInfo> iterator = syncInfoList.iterator();
        while (iterator.hasNext()) {
            SyncInfo next = iterator.next();
            if (next.getDeviceName().equals(deviceName)) {
                return next;
            }
        }

        return null;
    }

    public Long getAverageRoundTripTime() {
        return averageRoundTripTime;
    }

    public void setAverageRoundTripTime(Long averageRoundTripTime) {
        this.averageRoundTripTime = averageRoundTripTime;
        savedStateHandle.set(AVERAGE_ROUND_TRIP_KEY, averageRoundTripTime);
    }

    public void updateAverageRoundTripTime(long roundTripTime) {
        long update = Math.round(FACTOR * roundTripTime + (1 - FACTOR) * averageRoundTripTime);
        setAverageRoundTripTime(update);
    }
}
