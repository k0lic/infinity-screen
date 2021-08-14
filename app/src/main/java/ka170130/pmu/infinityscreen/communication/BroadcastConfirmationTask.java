package ka170130.pmu.infinityscreen.communication;

import android.util.Log;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

import ka170130.pmu.infinityscreen.MainActivity;
import ka170130.pmu.infinityscreen.containers.Message;

public class BroadcastConfirmationTask implements Runnable {

    private static final int TIMEOUT = 100;
    private static final int MAX_RETRIES = 20;

    private Message message;
    private Semaphore master;
    private Semaphore clientSem;

    public BroadcastConfirmationTask(Message message, Semaphore master, Semaphore clientSem) {
        this.message = message;
        this.master = master;
        this.clientSem = clientSem;
    }

    @Override
    public void run() {
        SenderTask.lock.lock();

        // port number should be irrelevant since the socket is not used for receiving
        try (DatagramSocket socket = new DatagramSocket(TaskManager.BROADCAST_PORT + 2)) {
            socket.setBroadcast(true);

            byte[] bytes = message.getBytes();
            DatagramPacket packet = new DatagramPacket(
                    bytes,
                    bytes.length,
                    // TODO: get this address programmatically?
                    InetAddress.getByName("192.168.49.255"),
                    TaskManager.BROADCAST_PORT
            );

            boolean confirmation = false;

            int retryCount = 0;
            while (!confirmation && retryCount < MAX_RETRIES) {
                socket.send(packet);

                Log.d(MainActivity.LOG_TAG, "Try Acquire: " + retryCount);
                confirmation = clientSem.tryAcquire(TIMEOUT, TimeUnit.MILLISECONDS);
                retryCount++;
            }

            if (confirmation) {
                // success
                Log.d(MainActivity.LOG_TAG, "Broadcast message " + message.getMessageType().toString() + " sent with confirmation");
            } else {
                // failure
                Log.d(MainActivity.LOG_TAG, "Broadcast message " + message.getMessageType().toString() + " failed to confirm ======================================= FAILED FAILED FAILED");
            }

            master.release();
        } catch (Exception e) {
            Log.d(MainActivity.LOG_TAG, e.toString());
            e.printStackTrace();
        } finally {
            SenderTask.lock.unlock();
        }
    }
}
