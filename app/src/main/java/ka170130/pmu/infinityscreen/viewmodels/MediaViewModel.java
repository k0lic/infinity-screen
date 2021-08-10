package ka170130.pmu.infinityscreen.viewmodels;

import android.net.Uri;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.SavedStateHandle;
import androidx.lifecycle.ViewModel;

import java.util.ArrayList;

import ka170130.pmu.infinityscreen.helpers.ThreadHelper;

public class MediaViewModel extends ViewModel implements Resettable {

    private static final String SELECTED_KEY = "media-selected-key";

    private SavedStateHandle savedStateHandle;

    private MutableLiveData<ArrayList<String>> selectedMedia;

    public MediaViewModel(SavedStateHandle savedStateHandle) {
        this.savedStateHandle = savedStateHandle;

        selectedMedia = savedStateHandle.getLiveData(SELECTED_KEY, new ArrayList<>());
    }

    @Override
    public void reset() {
        setSelectedMedia(new ArrayList<>());
    }

    public LiveData<ArrayList<String>> getSelectedMedia() {
        return selectedMedia;
    }

    public void setSelectedMedia(ArrayList<String> selectedMedia) {
        ThreadHelper.runOnMainThread(() -> savedStateHandle.set(SELECTED_KEY, selectedMedia));
    }

    public void addToSelectedMedia(ArrayList<String> toAdd) {
        ArrayList<String> selectedMediaValue = selectedMedia.getValue();
        selectedMediaValue.addAll(toAdd);
        setSelectedMedia(selectedMediaValue);
    }

    public void clearSelectedMedia() {
        setSelectedMedia(new ArrayList<>());
    }

    public void removeFromSelectedMedia(int position) {
        ArrayList<String> selectedMediaValue = selectedMedia.getValue();

        if (position < 0 || position >= selectedMediaValue.size()) {
            return;
        }
        selectedMediaValue.remove(position);

        setSelectedMedia(selectedMediaValue);
    }
}
