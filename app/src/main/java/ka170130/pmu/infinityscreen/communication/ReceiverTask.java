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
import ka170130.pmu.infinityscreen.helpers.LogHelper;
import ka170130.pmu.infinityscreen.viewmodels.ConnectionViewModel;
import ka170130.pmu.infinityscreen.containers.Message;
import ka170130.pmu.infinityscreen.containers.PeerInetAddressInfo;
import ka170130.pmu.infinityscreen.containers.PeerInfo;
import ka170130.pmu.infinityscreen.viewmodels.MediaViewModel;

public class ReceiverTask implements Runnable {

    private MessageHandler messageHandler;

    private Socket socket;
    private InetAddress inetAddress;

    public ReceiverTask(
            MessageHandler messageHandler,
            Socket socket
    ) {
        this.messageHandler = messageHandler;

        this.socket = socket;
        this.inetAddress = socket.getInetAddress();
    }

    @Override
    public void run() {
        LogHelper.log(Message.LOG_TAG,
                "ReceiverTask started for " + inetAddress.getHostName());
        InputStream inputStream = null;

        try {
            inputStream = socket.getInputStream();

            byte[] buf = new byte[Message.MESSAGE_MAX_SIZE];
            byte[] copy = new byte[Message.MESSAGE_MAX_SIZE];
            int total = 0;

            int len = inputStream.read(buf);
            while (len != -1) {
                System.arraycopy(buf, 0, copy, total, len);
                total += len;

                len = inputStream.read(buf);
            }

            byte[] result = new byte[total];
            System.arraycopy(copy, 0, result, 0, total);

            Message message = new Message(result);
            messageHandler.handleMessage(message, inetAddress);
        } catch (Exception e) {
            LogHelper.error(e);
        } finally {
            if (inputStream != null) {
                try {
                    inputStream.close();
                } catch (IOException e) {
                    LogHelper.error(e);
                }
            }

            if (socket != null) {
                if (socket.isConnected()) {
                    try {
                        socket.close();
                    } catch (IOException e) {
                        LogHelper.error(e);
                    }
                }
            }
        }
    }
}
