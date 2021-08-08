package ka170130.pmu.infinityscreen.communication;

import android.util.Log;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;

import ka170130.pmu.infinityscreen.MainActivity;
import ka170130.pmu.infinityscreen.containers.Message;

public class BroadcastTask implements Runnable {

    private Message message;

    public BroadcastTask(Message message) {
        this.message = message;
    }

    @Override
    public void run() {
        SenderTask.lock.lock();

        // port number should be irrelevant since the socket is not used for receiving
        try (DatagramSocket socket = new DatagramSocket(TaskManager.BROADCAST_PORT + 1)) {
            socket.setBroadcast(true);

            byte[] bytes = message.getBytes();
            DatagramPacket packet = new DatagramPacket(
                    bytes,
                    bytes.length,
                    // TODO: get this address programmatically?
                    InetAddress.getByName("192.168.49.255"),
                    TaskManager.BROADCAST_PORT
            );

            socket.send(packet);
            Log.d(MainActivity.LOG_TAG, "Broadcast message " + message.getMessageType().toString() + " sent");
        } catch (Exception e) {
            Log.d(MainActivity.LOG_TAG, e.toString());
            e.printStackTrace();
        } finally {
            SenderTask.lock.unlock();
        }
    }
}
