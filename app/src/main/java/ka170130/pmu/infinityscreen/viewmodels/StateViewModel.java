package ka170130.pmu.infinityscreen.viewmodels;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.SavedStateHandle;
import androidx.lifecycle.ViewModel;

import ka170130.pmu.infinityscreen.helpers.ThreadHelper;

public class StateViewModel extends ViewModel {

    private static final String STATE_KEY = "state-state-key";

    public enum AppState {
        LAYOUT,
        PREVIEW,
        FILE_SELECTION,
        FILE_WAIT,
        PLAY
    }

    private SavedStateHandle savedStateHandle;

    private MutableLiveData<AppState> state;

    public StateViewModel(SavedStateHandle savedStateHandle) {
        this.savedStateHandle = savedStateHandle;

        state = savedStateHandle.getLiveData(STATE_KEY, null);
    }

    public void reset() {
        setState(null);
    }

    public LiveData<AppState> getState() {
        return state;
    }

    public void setState(AppState state) {
        ThreadHelper.runOnMainThread(() -> savedStateHandle.set(STATE_KEY, state));
    }
}
