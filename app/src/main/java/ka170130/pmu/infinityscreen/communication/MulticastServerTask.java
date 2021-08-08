package ka170130.pmu.infinityscreen.communication;

import android.util.Log;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.MulticastSocket;
import java.util.Arrays;

import ka170130.pmu.infinityscreen.MainActivity;
import ka170130.pmu.infinityscreen.containers.Message;

public class MulticastServerTask implements Runnable {

    private MessageHandler messageHandler;

    public MulticastServerTask(MessageHandler messageHandler) {
        this.messageHandler = messageHandler;
    }

//    @Override
//    public void run() {
//        MulticastSocket socket = null;
//        InetAddress group = null;
//
//        try {
//            socket = new MulticastSocket(TaskManager.MULTICAST_PORT);
//            group = InetAddress.getByName(TaskManager.MULTICAST_ADDRESS);
//            socket.joinGroup(group);
//
//            byte[] buf = new byte[1024];
//            DatagramPacket packet = new DatagramPacket(buf, buf.length);
//
//            while(true)
//            {
//                socket.receive(packet);
//
//                Message message = new Message(packet.getData());
//                messageHandler.handleMessage(message, packet.getAddress());
//            }
//        } catch (Exception e) {
//            Log.d(MainActivity.LOG_TAG, e.toString());
//            e.printStackTrace();
//        } finally {
//            if (socket != null) {
//                if (group != null) {
//                    try {
//                        socket.leaveGroup(group);
//                    } catch (IOException e) {
//                        Log.d(MainActivity.LOG_TAG, e.toString());
//                        e.printStackTrace();
//                    }
//                }
//                socket.close();
//            }
//        }
//    }

    @Override
    public void run() {
        try (DatagramSocket socket = new MulticastSocket(TaskManager.MULTICAST_PORT)) {
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
