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

public class SenderTask implements Runnable {

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

        Log.d(MainActivity.LOG_TAG, "Sending message: " + message.getMessageType().toString());

        Socket socket = new Socket();
        OutputStream outputStream = null;

        try {
            socket.bind(null);
            socket.connect((new InetSocketAddress(address, TaskManager.DEFAULT_PORT)), 500);

            outputStream = socket.getOutputStream();

            outputStream.write(message.getBytes());
        } catch (Exception e) {
            Log.d(MainActivity.LOG_TAG, e.toString());
            e.printStackTrace();
        } finally {
            if (outputStream != null) {
                try {
                    outputStream.close();
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

            lock.unlock();
        }
    }
}
