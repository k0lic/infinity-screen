package ka170130.pmu.infinityscreen.io;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

import ka170130.pmu.infinityscreen.communication.TaskManager;
import ka170130.pmu.infinityscreen.helpers.LogHelper;

public class StreamProxyServer implements Runnable {

    private TaskManager taskManager;

    public StreamProxyServer(TaskManager taskManager) {
        this.taskManager = taskManager;
    }

    @Override
    public void run() {
        ServerSocket serverSocket = null;

        try {
            serverSocket = new ServerSocket(TaskManager.PROXY_PORT);

            while (true) {
                Socket client = serverSocket.accept();
                LogHelper.log("StreamProxy client " + client.getInetAddress().getHostName() + " accepted");
                taskManager.runStreamProxyTask(client);
            }
        } catch (IOException e) {
            LogHelper.error(e);
        } finally {
            if (serverSocket != null) {
                try {
                    serverSocket.close();
                } catch (IOException e) {
                    LogHelper.error(e);
                }
            }
        }
    }
}
