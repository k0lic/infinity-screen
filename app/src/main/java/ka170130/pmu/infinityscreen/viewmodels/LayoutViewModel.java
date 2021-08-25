package ka170130.pmu.infinityscreen.viewmodels;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MediatorLiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.SavedStateHandle;
import androidx.lifecycle.ViewModel;

import java.util.ArrayList;
import java.util.Iterator;

import ka170130.pmu.infinityscreen.containers.TransformInfo;
import ka170130.pmu.infinityscreen.helpers.ThreadHelper;

public class LayoutViewModel extends ViewModel implements Resettable {

    private static final String SELF_KEY = "layout-self-key";
    private static final String TRANSFORM_LIST_KEY = "layout-transform-list-key";
    private static final String VIEWPORT_KEY = "layout-viewport-key";
    private static final String GENERATOR_EXECUTED_KEY = "layout-generator-executed-key";

    private SavedStateHandle savedStateHandle;

    private MutableLiveData<TransformInfo> selfTransform;
    private MutableLiveData<ArrayList<TransformInfo>> transformList;
    private MediatorLiveData<TransformInfo> selfAuto;

    private MutableLiveData<TransformInfo> viewport;

    private Boolean layoutGeneratorExecuted;

    public LayoutViewModel(SavedStateHandle savedStateHandle) {
        this.savedStateHandle = savedStateHandle;

        selfTransform = savedStateHandle.getLiveData(SELF_KEY, null);
//        selfTransform = new MutableLiveData<>();
        transformList = savedStateHandle.getLiveData(TRANSFORM_LIST_KEY, new ArrayList<>());
//        transformList = new MutableLiveData<>(new ArrayList<>());

        selfAuto = new MediatorLiveData<>();
        selfAuto.addSource(transformList, this::refreshSelfAuto);

        viewport = savedStateHandle.getLiveData(VIEWPORT_KEY, null);

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
                selfAuto.setValue(next);
                return;
            }
        }

        selfAuto.setValue(self);
    }

    @Override
    public void reset() {
        setTransformList(new ArrayList<>());
        setViewport(null);
        setLayoutGeneratorExecuted(false);
    }

    public LiveData<TransformInfo> getSelfTransform() {
        return selfTransform;
    }

    public void setSelfTransform(TransformInfo selfTransform) {
        ThreadHelper.runOnMainThread(() -> savedStateHandle.set(SELF_KEY, selfTransform));
//        this.selfTransform.postValue(selfTransform);
    }

    public LiveData<ArrayList<TransformInfo>> getTransformList() {
        return transformList;
    }

    public void setTransformList(ArrayList<TransformInfo> transformList) {
        ThreadHelper.runOnMainThread(() -> savedStateHandle.set(TRANSFORM_LIST_KEY, transformList));
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
                transformInfo.getScreenWidth(),
                transformInfo.getScreenHeight()
        );

        list.add(toAdd);
        setTransformList(list);
    }

    public LiveData<TransformInfo> getSelfAuto() {
        return selfAuto;
    }

    public LiveData<TransformInfo> getViewport() {
        return viewport;
    }

    public void setViewport(TransformInfo viewport) {
        ThreadHelper.runOnMainThread(() -> savedStateHandle.set(VIEWPORT_KEY, viewport));
    }

    public Boolean getLayoutGeneratorExecuted() {
        return layoutGeneratorExecuted;
    }

    public void setLayoutGeneratorExecuted(Boolean layoutGeneratorExecuted) {
        this.layoutGeneratorExecuted = layoutGeneratorExecuted;
        savedStateHandle.set(GENERATOR_EXECUTED_KEY, layoutGeneratorExecuted);
    }
}
