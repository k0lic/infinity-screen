package ka170130.pmu.infinityscreen.communication;

import android.util.Log;

import java.io.IOException;
import java.io.InputStream;
import java.net.InetAddress;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;

import ka170130.pmu.infinityscreen.MainActivity;
import ka170130.pmu.infinityscreen.connection.ConnectionManager;
import ka170130.pmu.infinityscreen.viewmodels.ConnectionViewModel;
import ka170130.pmu.infinityscreen.containers.Message;
import ka170130.pmu.infinityscreen.containers.PeerInetAddressInfo;
import ka170130.pmu.infinityscreen.containers.PeerInfo;

public class ReceiverTask implements Runnable {

    private TaskManager taskManager;
    private ConnectionManager connectionManager;
    private ConnectionViewModel connectionViewModel;
    private MessageHandler messageHandler;

    private Socket socket;
    private InetAddress inetAddress;

    public ReceiverTask(
            TaskManager taskManager,
            ConnectionViewModel connectionViewModel,
            MessageHandler messageHandler,
            Socket socket
    ) {
        this.taskManager = taskManager;
        this.connectionManager = taskManager.getMainActivity().getConnectionManager();
        this.connectionViewModel = connectionViewModel;
        this.messageHandler = messageHandler;
        this.socket = socket;

        this.inetAddress = socket.getInetAddress();
    }

    @Override
    public void run() {
        Log.d(MainActivity.LOG_TAG, "ReceiverTask started for " + inetAddress.getHostName());
        InputStream inputStream = null;

        try {
            inputStream = socket.getInputStream();

            int val = 0;
            List<Byte> bytes = new ArrayList<>();

            val = inputStream.read();
            while (val != -1) {
                bytes.add((byte) val);
                val = inputStream.read();
            }

            byte[] byteArray = new byte[bytes.size()];
            for (int i = 0; i < bytes.size(); i++) {
                byteArray[i] = bytes.get(i);
            }

            Message message = new Message(byteArray);
            messageHandler.handleMessage(message, inetAddress);
        } catch (Exception e) {
            Log.d(MainActivity.LOG_TAG, e.toString());
            e.printStackTrace();
        } finally {
            if (inputStream != null) {
                try {
                    inputStream.close();
                } catch (IOException e) {
                    Log.d(MainActivity.LOG_TAG, e.toString());
                    e.printStackTrace();
                }
            }

            if (socket != null) {
                if (socket.isConnected()) {
                    try {
                        socket.close();
                    } catch (IOException e) {
                        Log.d(MainActivity.LOG_TAG, e.toString());
                        e.printStackTrace();
                    }
                }
            }
        }
    }
}
