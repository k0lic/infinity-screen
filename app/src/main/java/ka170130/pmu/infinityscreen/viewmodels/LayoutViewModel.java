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

public class LayoutViewModel extends ViewModel {

    private static final String SELF_KEY = "layout-self-key";
    private static final String TRANSFORM_LIST_KEY = "layout-transform-list-key";

    private SavedStateHandle savedStateHandle;

    private MutableLiveData<TransformInfo> selfTransform;
    private MutableLiveData<ArrayList<TransformInfo>> transformList;
    private MediatorLiveData<TransformInfo> selfAuto;

    public LayoutViewModel(SavedStateHandle savedStateHandle) {
        this.savedStateHandle = savedStateHandle;

        selfTransform = savedStateHandle.getLiveData(SELF_KEY, null);
//        selfTransform = new MutableLiveData<>();
        transformList = savedStateHandle.getLiveData(TRANSFORM_LIST_KEY, new ArrayList<>());
//        transformList = new MutableLiveData<>(new ArrayList<>());

        selfAuto = new MediatorLiveData<>();
        selfAuto.addSource(transformList, this::refreshSelfAuto);
    }

    private void refreshSelfAuto(ArrayList<TransformInfo> list) {
        TransformInfo self = selfTransform.getValue();
        if (self == null) {
            selfAuto.setValue(null);
            return;
        }

        String myAddress = self.getDeviceAddress();

        Iterator<TransformInfo> iterator = list.iterator();
        while (iterator.hasNext()) {
            TransformInfo next = iterator.next();
            if (next.getDeviceAddress().equals(myAddress)) {
                selfAuto.setValue(next);
                return;
            }
        }

        selfAuto.setValue(self);
    }

    public void reset() {
        setTransformList(new ArrayList<>());
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

    public LiveData<TransformInfo> getSelfAuto() {
        return selfAuto;
    }
}
