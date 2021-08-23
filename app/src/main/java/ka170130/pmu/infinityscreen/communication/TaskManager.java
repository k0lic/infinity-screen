package ka170130.pmu.infinityscreen.communication;

import android.util.Log;

import androidx.lifecycle.ViewModelProvider;

import java.net.InetAddress;
import java.net.Socket;
import java.util.Iterator;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Semaphore;

import ka170130.pmu.infinityscreen.MainActivity;
import ka170130.pmu.infinityscreen.containers.PeerInetAddressInfo;
import ka170130.pmu.infinityscreen.io.ReadTask;
import ka170130.pmu.infinityscreen.io.StreamProxyServer;
import ka170130.pmu.infinityscreen.io.StreamProxyTask;
import ka170130.pmu.infinityscreen.io.WriteTask;
import ka170130.pmu.infinityscreen.viewmodels.ConnectionViewModel;
import ka170130.pmu.infinityscreen.containers.Message;
import ka170130.pmu.infinityscreen.viewmodels.StateViewModel;

public class TaskManager {

    public static final int DEFAULT_PORT = 8888;
    public static final int BROADCAST_PORT = 8889;
    public static final int PROXY_PORT = 8900;

    private static final int THREAD_POOL_COUNT = 24;

    private MainActivity mainActivity;
    private ExecutorService executorService;
    private MessageHandler messageHandler;
    private ConnectionViewModel connectionViewModel;

    private InetAddress defaultAddress;

    private ReadTask readTask;
    private WriteTask writeTask;

    public TaskManager(MainActivity mainActivity) {
        this.mainActivity = mainActivity;
//        executorService = Executors.newFixedThreadPool(THREAD_POOL_COUNT);
        executorService = Executors.newCachedThreadPool();

        messageHandler = new MessageHandler(
                this,
                mainActivity
        );

        connectionViewModel = new ViewModelProvider(mainActivity).get(ConnectionViewModel.class);

        readTask = new ReadTask(mainActivity);
        executorService.submit(readTask);
        writeTask = new WriteTask(mainActivity);
        executorService.submit(writeTask);
    }

    public MainActivity getMainActivity() {
        return mainActivity;
    }

    public void setDefaultAddress(InetAddress defaultAddress) {
        this.defaultAddress = defaultAddress;
    }

    public ReadTask getReadTask() {
        return readTask;
    }

    public WriteTask getWriteTask() {
        return writeTask;
    }

    public void runServerTask() {
        executorService.submit(new ServerTask(this));
    }

    public void runReceiverTask(Socket socket) {
        executorService.submit(new ReceiverTask(messageHandler, socket));
    }

    public void runSenderTask(Message message) {
        runSenderTask(defaultAddress, message);
    }

    public void runSenderTask(InetAddress address, Message message) {
        executorService.submit(new SenderTask(address, message));
    }

    public void runBroadcastServerTask() {
        executorService.submit(new BroadcastServerTask(messageHandler));
    }

    public void runBroadcastTask(Message message) {
        executorService.submit(new BroadcastTask(message));
    }

    public void sendToAllInGroup(Message message, boolean toHost) {
        Iterator<PeerInetAddressInfo> iterator =
                connectionViewModel.getGroupList().getValue().iterator();
        while (iterator.hasNext()) {
            PeerInetAddressInfo next = iterator.next();
            runSenderTask(next.getInetAddress(), message);
        }

        if (toHost) {
            PeerInetAddressInfo host = connectionViewModel.getHostDevice().getValue();
            runSenderTask(host.getInetAddress(), message);
        }
    }

    public void runContentTask(int numberOfClients) {
        executorService.submit(new ContentTask(this, numberOfClients));
    }

    public void runBroadcastConfirmationTask(
            Message message,
            Semaphore master,
            Semaphore clientSem
    ) {
        executorService.submit(new BroadcastConfirmationTask(message, master, clientSem));
    }

    public void runStreamProxyServer() {
        executorService.submit(new StreamProxyServer(this));
    }

    public void runStreamProxyTask(Socket socket) {
        executorService.submit(new StreamProxyTask(socket));
    }
}
