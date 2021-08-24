package ka170130.pmu.infinityscreen.communication;

import android.util.Log;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

import ka170130.pmu.infinityscreen.MainActivity;
import ka170130.pmu.infinityscreen.containers.Message;
import ka170130.pmu.infinityscreen.helpers.LogHelper;

public class ServerTask implements Runnable {

    private TaskManager taskManager;

    public ServerTask(TaskManager taskManager) {
        this.taskManager = taskManager;
    }

    @Override
    public void run() {
        LogHelper.log("ServerTask up and running");
        ServerSocket serverSocket = null;

        try {
            serverSocket = new ServerSocket(TaskManager.DEFAULT_PORT);

            while (true) {
                Socket client = serverSocket.accept();
                LogHelper.log(Message.LOG_TAG,
                        "ServerTask client " + client.getInetAddress().getHostName() + " accepted");
                taskManager.runReceiverTask(client);
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
