package ka170130.pmu.infinityscreen.viewmodels;

import androidx.lifecycle.SavedStateHandle;
import androidx.lifecycle.ViewModel;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Semaphore;

public class UdpViewModel extends ViewModel implements Resettable {

    private static final String NUMBER_OF_CLIENTS_KEY = "udp-number-of-client-key";
    private static final String SEMAPHORE_MAP_KEY = "udp-semaphore-map-key";

    private SavedStateHandle savedStateHandle;

    private Integer numberOfClients;
    private Map<Integer, Semaphore> semaphoreMap;

    public UdpViewModel(SavedStateHandle savedStateHandle) {
        this.savedStateHandle = savedStateHandle;

        numberOfClients = savedStateHandle.get(NUMBER_OF_CLIENTS_KEY);

        semaphoreMap = savedStateHandle.get(SEMAPHORE_MAP_KEY);
        if (semaphoreMap == null) {
            clearSemaphoreMap();
        }
    }

    @Override
    public void reset() {
        setNumberOfClients(null);
        clearSemaphoreMap();
    }

    public Integer getNumberOfClients() {
        return numberOfClients;
    }

    public void setNumberOfClients(Integer numberOfClients) {
        this.numberOfClients = numberOfClients;
        savedStateHandle.set(NUMBER_OF_CLIENTS_KEY, numberOfClients);
    }

    public Map<Integer, Semaphore> getSemaphoreMap() {
        return semaphoreMap;
    }

    public void setSemaphoreMap(Map<Integer, Semaphore> semaphoreMap) {
        this.semaphoreMap = semaphoreMap;
        savedStateHandle.set(SEMAPHORE_MAP_KEY, semaphoreMap);
    }

    public void clearSemaphoreMap() {
        setSemaphoreMap(new HashMap<>());
    }

    public void addSemaphore(Integer key, Semaphore semaphore) {
        semaphoreMap.put(key, semaphore);
    }

    public Semaphore getSemaphore(Integer key) {
        return semaphoreMap.get(key);
    }

    public void removeSemaphore(Integer key) {
        semaphoreMap.remove(key);
    }
}
