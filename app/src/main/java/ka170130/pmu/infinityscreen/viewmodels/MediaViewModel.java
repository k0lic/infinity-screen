package ka170130.pmu.infinityscreen.viewmodels;

import android.net.Uri;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MediatorLiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.Observer;
import androidx.lifecycle.SavedStateHandle;
import androidx.lifecycle.ViewModel;

import java.util.ArrayList;

import ka170130.pmu.infinityscreen.containers.FileInfo;
import ka170130.pmu.infinityscreen.helpers.ThreadHelper;

public class MediaViewModel extends ViewModel implements Resettable {

    private static final String SELECTED_KEY = "media-selected-key";
    private static final String FILE_INFO_KEY = "media-file-info-key";
    private static final String CURRENT_INDEX_KEY = "media-current-file-index-key";
    private static final String CURRENT_INFO_KEY = "media-current-file-info-key";

    private SavedStateHandle savedStateHandle;

    private MutableLiveData<ArrayList<String>> selectedMedia;
    private MutableLiveData<ArrayList<FileInfo>> fileInfoList;
    private MutableLiveData<Integer> currentFileIndex;
    private MediatorLiveData<FileInfo> currentFileInfo;

    public MediaViewModel(SavedStateHandle savedStateHandle) {
        this.savedStateHandle = savedStateHandle;

        selectedMedia = savedStateHandle.getLiveData(SELECTED_KEY, new ArrayList<>());
        fileInfoList = savedStateHandle.getLiveData(FILE_INFO_KEY, new ArrayList<>());
        currentFileIndex = savedStateHandle.getLiveData(CURRENT_INDEX_KEY, 0);

        currentFileInfo = new MediatorLiveData<>();
        currentFileInfo.addSource(fileInfoList, fileInfos -> refreshCurrentFileInfo());
        currentFileInfo.addSource(currentFileIndex, index -> refreshCurrentFileInfo());
    }

    private void refreshCurrentFileInfo() {
        ArrayList<FileInfo> fileInfos = fileInfoList.getValue();
        Integer index = currentFileIndex.getValue();

        if (index < 0 || index >= fileInfos.size()) {
            currentFileInfo.setValue(null);
            return;
        }

        currentFileInfo.setValue(fileInfos.get(index));
    }

    @Override
    public void reset() {
        clearSelectedMedia();
        clearFileInfoList();
        setCurrentFileIndex(0);
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

    public LiveData<ArrayList<FileInfo>> getFileInfoList() {
        return fileInfoList;
    }

    public void setFileInfoList(ArrayList<FileInfo> fileInfoList) {
        ThreadHelper.runOnMainThread(() -> savedStateHandle.set(FILE_INFO_KEY, fileInfoList));
    }

    public void clearFileInfoList() {
        setFileInfoList(new ArrayList<>());
    }

    public LiveData<Integer> getCurrentFileIndex() {
        return currentFileIndex;
    }

    public void setCurrentFileIndex(Integer index) {
        ThreadHelper.runOnMainThread(() -> savedStateHandle.set(CURRENT_INDEX_KEY, index));
    }

    public LiveData<FileInfo> getCurrentFileInfo() {
        return currentFileInfo;
    }
}
