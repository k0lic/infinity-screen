package ka170130.pmu.infinityscreen.communication;

import androidx.lifecycle.ViewModelProvider;

import java.net.InetAddress;
import java.net.Socket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import ka170130.pmu.infinityscreen.MainActivity;
import ka170130.pmu.infinityscreen.connection.ConnectionViewModel;
import ka170130.pmu.infinityscreen.containers.Message;

public class TaskManager {

    public static final int DEFAULT_PORT = 8888;

    private static final int THREAD_POOL_COUNT = 4;

    private ConnectionViewModel connectionViewModel;
    private ExecutorService executorService;
    private InetAddress defaultAddress;

    public TaskManager(MainActivity mainActivity) {
        connectionViewModel = new ViewModelProvider(mainActivity).get(ConnectionViewModel.class);
        executorService = Executors.newFixedThreadPool(THREAD_POOL_COUNT);
    }

    public void setDefaultAddress(InetAddress defaultAddress) {
        this.defaultAddress = defaultAddress;
    }

    public void runServerTask() {
        executorService.submit(new ServerTask(this));
    }

    public void runReceiverTask(Socket socket) {
        executorService.submit(new ReceiverTask(this, connectionViewModel, socket));
    }

    public void runSenderTask(Message message) {
        runSenderTask(defaultAddress, message);
    }

    public void runSenderTask(InetAddress address, Message message) {
        executorService.submit(new SenderTask(address, message));
    }
}
