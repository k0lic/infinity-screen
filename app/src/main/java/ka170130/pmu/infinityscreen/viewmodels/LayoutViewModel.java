package ka170130.pmu.infinityscreen.viewmodels;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MediatorLiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.SavedStateHandle;
import androidx.lifecycle.ViewModel;

import java.util.ArrayList;
import java.util.Iterator;

import ka170130.pmu.infinityscreen.containers.TransformInfo;
import ka170130.pmu.infinityscreen.helpers.CopyHelper;
import ka170130.pmu.infinityscreen.helpers.ThreadHelper;

public class LayoutViewModel extends ViewModel implements Resettable {

    private static final String SELF_KEY = "layout-self-key";
    private static final String TRANSFORM_LIST_KEY = "layout-transform-list-key";
    private static final String BACKUP_TRANSFORM_LIST_KEY = "layout-backup-transform-list-key";
    private static final String VIEWPORT_KEY = "layout-viewport-key";
    private static final String BACKUP_VIEWPORT_KEY = "layout-backup-viewport-key";
    private static final String UPDATE_COUNT_KEY = "layout-update-count-key";
    private static final String GENERATOR_EXECUTED_KEY = "layout-generator-executed-key";

    private SavedStateHandle savedStateHandle;

    private MutableLiveData<TransformInfo> selfTransform;
    private MutableLiveData<ArrayList<TransformInfo>> transformList;
    private MutableLiveData<ArrayList<TransformInfo>> backupTransformList;
    private MediatorLiveData<TransformInfo> selfAuto;

    private MutableLiveData<TransformInfo> viewport;
    private MutableLiveData<TransformInfo> backupViewport;

    private Integer updateCount;

    private Boolean layoutGeneratorExecuted;

    public LayoutViewModel(SavedStateHandle savedStateHandle) {
        this.savedStateHandle = savedStateHandle;

        selfTransform = savedStateHandle.getLiveData(SELF_KEY, null);
//        selfTransform = new MutableLiveData<>();
        transformList = savedStateHandle.getLiveData(TRANSFORM_LIST_KEY, new ArrayList<>());
//        transformList = new MutableLiveData<>(new ArrayList<>());
        backupTransformList = savedStateHandle.getLiveData(BACKUP_TRANSFORM_LIST_KEY, new ArrayList<>());

        selfAuto = new MediatorLiveData<>();
        selfAuto.addSource(transformList, this::refreshSelfAuto);

        viewport = savedStateHandle.getLiveData(VIEWPORT_KEY, null);
        backupViewport = savedStateHandle.getLiveData(BACKUP_VIEWPORT_KEY, null);

        updateCount = savedStateHandle.get(UPDATE_COUNT_KEY);
        if (updateCount == null) {
            updateCount = 0;
        }

        layoutGeneratorExecuted = savedStateHandle.get(GENERATOR_EXECUTED_KEY);
        if (layoutGeneratorExecuted == null) {
            layoutGeneratorExecuted = false;
        }
    }

    private void refreshSelfAuto(ArrayList<TransformInfo> list) {
        TransformInfo self = selfTransform.getValue();
        if (self == null) {
            selfAuto.setValue(null);
            return;
        }

        String ownName = self.getDeviceName();

        Iterator<TransformInfo> iterator = list.iterator();
        while (iterator.hasNext()) {
            TransformInfo next = iterator.next();
            if (next.getDeviceName().equals(ownName)) {
                selfAuto.setValue(CopyHelper.getCopy(next));
                return;
            }
        }

        selfAuto.setValue(CopyHelper.getCopy(self));
    }

    @Override
    public void reset() {
        setTransformList(new ArrayList<>());
        setBackupTransformList(new ArrayList<>());
        setViewport(null);
        setBackupViewport(null);
        setUpdateCount(0);
        setLayoutGeneratorExecuted(false);
    }

    public LiveData<TransformInfo> getSelfTransform() {
        return selfTransform;
    }

    public void setSelfTransform(TransformInfo selfTransform) {
        TransformInfo copy = CopyHelper.getCopy(selfTransform);
        ThreadHelper.runOnMainThread(() -> savedStateHandle.set(SELF_KEY, copy));
//        this.selfTransform.postValue(selfTransform);
    }

    public LiveData<ArrayList<TransformInfo>> getTransformList() {
        return transformList;
    }

    public void setTransformList(ArrayList<TransformInfo> transformList) {
        ArrayList<TransformInfo> copyList = CopyHelper.getCopy(transformList);
        ThreadHelper.runOnMainThread(() -> savedStateHandle.set(TRANSFORM_LIST_KEY, copyList));
//        this.transformList.postValue(transformList);
    }

    // TODO: maybe guard for concurrent calls?
    public void addTransform(TransformInfo transformInfo) {
        ArrayList<TransformInfo> list = transformList.getValue();
        int size = list.size();

        boolean contains = false;
        Iterator<TransformInfo> iterator = list.iterator();
        while (!contains && iterator.hasNext()) {
            TransformInfo next = iterator.next();
            contains = transformInfo.getDeviceName().equals(next.getDeviceName());
        }

        if (contains) {
            // skip - device already added
            return;
        }

        TransformInfo toAdd = new TransformInfo(
                transformInfo.getDeviceName(),
                size + 1,
                transformInfo.getOrientation(),
                transformInfo.getScreenWidth(),
                transformInfo.getScreenHeight()
        );

        list.add(toAdd);
        setTransformList(list);
    }

    // TODO: maybe guard for concurrent calls?
    public ArrayList<TransformInfo> updateTransform(TransformInfo transformInfo, boolean inc) {
        ArrayList<TransformInfo> list = transformList.getValue();

        boolean updated = false;
        Iterator<TransformInfo> iterator = list.iterator();
        while (!updated && iterator.hasNext()) {
            TransformInfo next = iterator.next();
            if (transformInfo.getDeviceName().equals(next.getDeviceName())) {
                list.remove(next);
                list.add(transformInfo);

                updated = true;
            }
        }

        if (inc) {
            Integer count = getUpdateCount();
            setUpdateCount(count + 1);
        }

        if (updated) {
            setTransformList(list);
            return list;
        }

        return null;
    }

    public LiveData<ArrayList<TransformInfo>> getBackupTransformList() {
        return backupTransformList;
    }

    public void setBackupTransformList(ArrayList<TransformInfo> backupTransformList) {
        ArrayList<TransformInfo> copyList = CopyHelper.getCopy(backupTransformList);
        ThreadHelper.runOnMainThread(
                () -> savedStateHandle.set(BACKUP_TRANSFORM_LIST_KEY, copyList));
    }

    public LiveData<TransformInfo> getSelfAuto() {
        return selfAuto;
    }

    public LiveData<TransformInfo> getViewport() {
        return viewport;
    }

    public void setViewport(TransformInfo viewport) {
        TransformInfo copy = CopyHelper.getCopy(viewport);
        ThreadHelper.runOnMainThread(() -> savedStateHandle.set(VIEWPORT_KEY, copy));
    }

    public LiveData<TransformInfo> getBackupViewport() {
        return backupViewport;
    }

    public void setBackupViewport(TransformInfo backupViewport) {
        TransformInfo copy = CopyHelper.getCopy(backupViewport);
        ThreadHelper.runOnMainThread(
                () -> savedStateHandle.set(BACKUP_VIEWPORT_KEY, copy));
    }

    public void restoreBackups() {
        // Fetch backups
        ArrayList<TransformInfo> backupTransformList = getBackupTransformList().getValue();
        TransformInfo backupViewport = getBackupViewport().getValue();

        // Restore backups
        if (backupTransformList != null) {
            setTransformList(backupTransformList);
        }

        if (backupViewport != null) {
            setViewport(backupViewport);
        }
    }

    public Integer getUpdateCount() {
        return updateCount;
    }

    public void setUpdateCount(Integer updateCount) {
        this.updateCount = updateCount;
        savedStateHandle.set(UPDATE_COUNT_KEY, updateCount);
    }

    public Boolean getLayoutGeneratorExecuted() {
        return layoutGeneratorExecuted;
    }

    public void setLayoutGeneratorExecuted(Boolean layoutGeneratorExecuted) {
        this.layoutGeneratorExecuted = layoutGeneratorExecuted;
        savedStateHandle.set(GENERATOR_EXECUTED_KEY, layoutGeneratorExecuted);
    }
}
