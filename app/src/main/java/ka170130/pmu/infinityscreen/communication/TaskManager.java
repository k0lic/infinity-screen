package ka170130.pmu.infinityscreen.communication;

import android.net.wifi.p2p.WifiP2pInfo;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class TaskManager {

    private static final int THREAD_POOL_COUNT = 4;

    private ExecutorService executorService;
    private WifiP2pInfo info;

    public TaskManager() {
        executorService = Executors.newFixedThreadPool(THREAD_POOL_COUNT);
    }

    public void setInfo(WifiP2pInfo info) {
        this.info = info;
    }

    private void runReceiverTask() {
        // TODO
    }

    private void runSenderTask(Message message) {
        executorService.submit(new SenderTask(info, message));
    }
}
