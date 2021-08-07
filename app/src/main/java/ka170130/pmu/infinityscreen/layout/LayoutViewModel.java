package ka170130.pmu.infinityscreen.layout;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.SavedStateHandle;

import java.util.ArrayList;

import ka170130.pmu.infinityscreen.containers.TransformInfo;

public class LayoutViewModel {

    private SavedStateHandle savedStateHandle;

    private MutableLiveData<TransformInfo> selfTransform;
    private MutableLiveData<ArrayList<TransformInfo>> transformList;

    public LayoutViewModel(SavedStateHandle savedStateHandle) {
        this.savedStateHandle = savedStateHandle;

        selfTransform = new MutableLiveData<>();
        transformList = new MutableLiveData<>(new ArrayList<>());
    }

    public LiveData<TransformInfo> getSelfTransform() {
        return selfTransform;
    }

    public void setSelfTransform(TransformInfo selfTransform) {
        this.selfTransform.postValue(selfTransform);
    }

    public LiveData<ArrayList<TransformInfo>> getTransformList() {
        return transformList;
    }

    public void setTransformList(ArrayList<TransformInfo> transformList) {
        this.transformList.postValue(transformList);
    }
}
