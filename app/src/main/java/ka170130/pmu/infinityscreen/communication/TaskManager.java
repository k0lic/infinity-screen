package ka170130.pmu.infinityscreen.communication;

import androidx.lifecycle.ViewModelProvider;

import java.net.InetAddress;
import java.net.Socket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import ka170130.pmu.infinityscreen.MainActivity;
import ka170130.pmu.infinityscreen.viewmodels.ConnectionViewModel;
import ka170130.pmu.infinityscreen.containers.Message;

public class TaskManager {

    public static final int DEFAULT_PORT = 8888;
    public static final int MULTICAST_PORT = 8889;
    public static final String MULTICAST_ADDRESS = "224.61.70.52";

    private static final int THREAD_POOL_COUNT = 4;

    private MainActivity mainActivity;
    private ConnectionViewModel connectionViewModel;
    private ExecutorService executorService;
    private MessageHandler messageHandler;

    private InetAddress defaultAddress;

    public TaskManager(MainActivity mainActivity) {
        this.mainActivity = mainActivity;
        connectionViewModel = new ViewModelProvider(mainActivity).get(ConnectionViewModel.class);
        executorService = Executors.newFixedThreadPool(THREAD_POOL_COUNT);
        messageHandler = new MessageHandler(
                this,
                mainActivity.getConnectionManager(),
                connectionViewModel
        );
    }

    public MainActivity getMainActivity() {
        return mainActivity;
    }

    public void setDefaultAddress(InetAddress defaultAddress) {
        this.defaultAddress = defaultAddress;
    }

    public void runServerTask() {
        executorService.submit(new ServerTask(this));
    }

    public void runReceiverTask(Socket socket) {
        executorService.submit(
                new ReceiverTask(this, connectionViewModel, messageHandler, socket)
        );
    }

    public void runSenderTask(Message message) {
        runSenderTask(defaultAddress, message);
    }

    public void runSenderTask(InetAddress address, Message message) {
        executorService.submit(new SenderTask(address, message));
    }

    public void runMulticastServerTask() {
        executorService.submit(new MulticastServerTask(messageHandler));
    }

    public void runMulticastTask(Message message) {
        executorService.submit(new MulticastTask(message));
    }
}
