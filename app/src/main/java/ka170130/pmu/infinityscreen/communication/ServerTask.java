package ka170130.pmu.infinityscreen.communication;

import android.util.Log;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

import ka170130.pmu.infinityscreen.MainActivity;

public class ServerTask implements Runnable {

    private TaskManager taskManager;

    public ServerTask(TaskManager taskManager) {
        this.taskManager = taskManager;
    }

    @Override
    public void run() {
        Log.d(MainActivity.LOG_TAG, "ServerTask up and running");
        ServerSocket serverSocket = null;

        try {
            serverSocket = new ServerSocket(TaskManager.DEFAULT_PORT);

            while (true) {
                Socket client = serverSocket.accept();
                taskManager.runReceiverTask(client);
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (serverSocket != null) {
                try {
                    serverSocket.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }
}
