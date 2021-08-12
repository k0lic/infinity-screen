package ka170130.pmu.infinityscreen.viewmodels;

import android.net.Uri;
import android.util.Log;
import android.util.Pair;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MediatorLiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.Observer;
import androidx.lifecycle.SavedStateHandle;
import androidx.lifecycle.ViewModel;

import java.io.File;
import java.util.ArrayList;
import java.util.Map;

import ka170130.pmu.infinityscreen.MainActivity;
import ka170130.pmu.infinityscreen.containers.FileInfo;
import ka170130.pmu.infinityscreen.containers.FileOnDeviceReady;
import ka170130.pmu.infinityscreen.helpers.ThreadHelper;

public class MediaViewModel extends ViewModel implements Resettable {

    private static final String SELECTED_KEY = "media-selected-key";
    private static final String FILE_INFO_KEY = "media-file-info-key";
    private static final String CURRENT_INDEX_KEY = "media-current-file-index-key";
    private static final String READY_MATRIX_KEY = "media-ready-matrix-key";
    private static final String CREATED_FILES_KEY = "media-created-files-key";
    private static final String CONTENT_TASK_CREATED_KEY = "media-content-task-created-key";

    private SavedStateHandle savedStateHandle;

    private MutableLiveData<ArrayList<String>> selectedMedia;
    private MutableLiveData<ArrayList<FileInfo>> fileInfoList;
    private MutableLiveData<Integer> currentFileIndex;
    private MediatorLiveData<FileInfo> currentFileInfo;

    // host only
    private boolean[][] readyMatrix;
    private Boolean contentTaskCreated;

    // client only
    private ArrayList<File> createdFiles;

    public MediaViewModel(SavedStateHandle savedStateHandle) {
        this.savedStateHandle = savedStateHandle;

        selectedMedia = savedStateHandle.getLiveData(SELECTED_KEY, new ArrayList<>());
        fileInfoList = savedStateHandle.getLiveData(FILE_INFO_KEY, new ArrayList<>());
        currentFileIndex = savedStateHandle.getLiveData(CURRENT_INDEX_KEY, 0);

        currentFileInfo = new MediatorLiveData<>();
        currentFileInfo.addSource(fileInfoList, fileInfos -> refreshCurrentFileInfo());
        currentFileInfo.addSource(currentFileIndex, index -> refreshCurrentFileInfo());

        readyMatrix = savedStateHandle.get(READY_MATRIX_KEY);
        contentTaskCreated = savedStateHandle.get(CONTENT_TASK_CREATED_KEY);
        if (contentTaskCreated == null) {
            contentTaskCreated = false;
        }

        createdFiles =  savedStateHandle.get(CREATED_FILES_KEY);
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
        setReadyMatrix(null);
        setContentTaskCreated(false);
        setCreatedFiles(null);
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

    public boolean isFileInfoListIndexOutOfBounds(int index) {
        ArrayList<FileInfo> fileInfos = fileInfoList.getValue();
        return index < 0 || index >= fileInfos.size();
    }

    public void setFileInfoList(ArrayList<FileInfo> fileInfoList) {
        ThreadHelper.runOnMainThread(() -> savedStateHandle.set(FILE_INFO_KEY, fileInfoList));
    }

    public void clearFileInfoList() {
        setFileInfoList(new ArrayList<>());
    }

    public void setFileInfoListElementContent(int index, String contentUri) {
        if (isFileInfoListIndexOutOfBounds(index)) {
            return;
        }

        ArrayList<FileInfo> fileInfos = fileInfoList.getValue();
        fileInfos.get(index).setContentUri(contentUri);

        setFileInfoList(fileInfos);
    }

    public void setFileInfoListElementPlaybackStatus(int index, FileInfo.PlaybackStatus playbackStatus) {
        if (isFileInfoListIndexOutOfBounds(index)) {
            return;
        }

        ArrayList<FileInfo> fileInfos = fileInfoList.getValue();
        fileInfos.get(index).setPlaybackStatus(playbackStatus);

        setFileInfoList(fileInfos);
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

    public boolean[][] getReadyMatrix() {
        return readyMatrix;
    }

    public boolean getReadyMatrixElement(int fileIndex, int deviceIndex) {
        return readyMatrix[fileIndex][deviceIndex];
    }

    public void setReadyMatrix(boolean[][] readyMatrix) {
        this.readyMatrix = readyMatrix;
        ThreadHelper.runOnMainThread(() -> {
            savedStateHandle.set(READY_MATRIX_KEY, readyMatrix);
        });
    }

    public void setReadyMatrixElement(int fileIndex, int deviceIndex, boolean value) {
        readyMatrix[fileIndex][deviceIndex] = value;
        setReadyMatrix(readyMatrix);
    }

    public boolean readyMatrixUpdate(FileOnDeviceReady fileOnDeviceReady) {
        int deviceIndex = fileOnDeviceReady.getDeviceIndex();
        int fileIndex = fileOnDeviceReady.getFileIndex();

        setReadyMatrixElement(fileIndex, deviceIndex, true);

        // check if file is ready on all devices
        boolean readyOnAll = true;
        for (int i = 0; i < readyMatrix[fileIndex].length && readyOnAll; i++) {
            readyOnAll = readyMatrix[fileIndex][i];
        }

        return readyOnAll;
    }

    public boolean isContentTaskCreated() {
        return contentTaskCreated;
    }

    public void setContentTaskCreated(boolean contentTaskCreated) {
        this.contentTaskCreated = contentTaskCreated;
        ThreadHelper.runOnMainThread(
                () -> savedStateHandle.set(CONTENT_TASK_CREATED_KEY, contentTaskCreated));
    }

    public ArrayList<File> getCreatedFiles() {
        return createdFiles;
    }

    public void setCreatedFiles(ArrayList<File> createdFiles) {
        this.createdFiles = createdFiles;
        ThreadHelper.runOnMainThread(() -> savedStateHandle.set(CREATED_FILES_KEY, createdFiles));
    }
}
