package ka170130.pmu.infinityscreen.viewmodels;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.SavedStateHandle;
import androidx.lifecycle.ViewModel;

import java.util.Date;

import ka170130.pmu.infinityscreen.helpers.ThreadHelper;

public class StateViewModel extends ViewModel implements Resettable {

    private static final String STATE_KEY = "state-state-key";
    private static final String LAST_UPDATE_KEY = "state-last-update-key";

    public enum AppState {
        CONNECTION,
        LAYOUT,
        PREVIEW,
        FILE_SELECTION,
        PLAY
    }

    private SavedStateHandle savedStateHandle;

    private MutableLiveData<AppState> state;
    private Date lastUpdate;

    public StateViewModel(SavedStateHandle savedStateHandle) {
        this.savedStateHandle = savedStateHandle;

        state = savedStateHandle.getLiveData(STATE_KEY, null);

        Long time = savedStateHandle.get(LAST_UPDATE_KEY);
        if (time == null) {
            lastUpdate = new Date();
        } else {
            lastUpdate = new Date(time);
        }
    }

    @Override
    public void reset() {
        setState(null);
    }

    public LiveData<AppState> getState() {
        return state;
    }

    public void setState(AppState state) {
        ThreadHelper.runOnMainThread(() -> savedStateHandle.set(STATE_KEY, state));
    }

    public Date getLastUpdate() {
        return lastUpdate;
    }

    public void setLastUpdate(Date lastUpdate) {
        this.lastUpdate = lastUpdate;
        savedStateHandle.set(LAST_UPDATE_KEY, lastUpdate.getTime());
    }
}
