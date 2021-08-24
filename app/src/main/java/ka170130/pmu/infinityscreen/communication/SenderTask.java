package ka170130.pmu.infinityscreen.communication;

import android.util.Log;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.concurrent.locks.ReentrantLock;

import ka170130.pmu.infinityscreen.MainActivity;
import ka170130.pmu.infinityscreen.containers.Message;
import ka170130.pmu.infinityscreen.helpers.LogHelper;

public class SenderTask implements Runnable {

    private static final int TIMEOUT = 5000;

    public static final ReentrantLock lock = new ReentrantLock();

    private InetAddress address;
    private Message message;

    public SenderTask(InetAddress address, Message message) {
        this.address = address;
        this.message = message;
    }

    @Override
    public void run() {
        lock.lock();

        LogHelper.log(Message.LOG_TAG,
                "Sending message: " + message.getMessageType().toString());

        Socket socket = new Socket();
        OutputStream outputStream = null;

        try {
            socket.bind(null);
            socket.connect((new InetSocketAddress(address, TaskManager.DEFAULT_PORT)), TIMEOUT);

            outputStream = socket.getOutputStream();

            outputStream.write(message.getBytes());
            LogHelper.log(Message.LOG_TAG,
                    "Message sent: " + message.getMessageType().toString());
        } catch (Exception e) {
            LogHelper.error(e);
        } finally {
            if (outputStream != null) {
                try {
                    outputStream.close();
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

            lock.unlock();
        }
    }
}
