package ka170130.pmu.infinityscreen.communication;

import android.util.Log;

import java.net.DatagramPacket;
import java.net.DatagramSocket;

import ka170130.pmu.infinityscreen.MainActivity;
import ka170130.pmu.infinityscreen.containers.Message;

public class BroadcastServerTask implements Runnable {

    private MessageHandler messageHandler;

    public BroadcastServerTask(MessageHandler messageHandler) {
        this.messageHandler = messageHandler;
    }

    @Override
    public void run() {
        try (DatagramSocket socket = new DatagramSocket(TaskManager.BROADCAST_PORT)) {
            socket.setBroadcast(true);

            byte[] buf = new byte[1024];
            DatagramPacket packet = new DatagramPacket(buf, buf.length);

            while (true) {
                socket.receive(packet);

                Message message = new Message(packet.getData());
                messageHandler.handleMessage(message, packet.getAddress());
            }
        } catch (Exception e) {
            Log.d(MainActivity.LOG_TAG, e.toString());
            e.printStackTrace();
        }
    }
}
