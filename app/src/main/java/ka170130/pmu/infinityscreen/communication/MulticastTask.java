package ka170130.pmu.infinityscreen.communication;

import android.util.Log;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.MulticastSocket;

import ka170130.pmu.infinityscreen.MainActivity;
import ka170130.pmu.infinityscreen.containers.Message;

public class MulticastTask implements Runnable {

    private Message message;

    public MulticastTask(Message message) {
        this.message = message;
    }

//    @Override
//    public void run() {
//        SenderTask.lock.lock();
//
//        MulticastSocket socket = null;
//        InetAddress group = null;
//
//        try {
//            socket = new MulticastSocket(TaskManager.MULTICAST_PORT);
//            group = InetAddress.getByName(TaskManager.MULTICAST_ADDRESS);
//            socket.joinGroup(group);
//
//            byte[] bytes = message.getBytes();
//            DatagramPacket packet =
//                    new DatagramPacket(bytes, bytes.length, group, TaskManager.MULTICAST_PORT);
//
//            socket.send(packet);
//            Log.d(MainActivity.LOG_TAG, "Multicast message " + message.getMessageType().toString() + " sent");
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
//
//            SenderTask.lock.unlock();
//        }
//    }

    @Override
    public void run() {
        SenderTask.lock.lock();

        try (DatagramSocket socket = new DatagramSocket(TaskManager.MULTICAST_PORT + 1)) {
            socket.setBroadcast(true);

            byte[] bytes = message.getBytes();
            DatagramPacket packet = new DatagramPacket(
                    bytes,
                    bytes.length,
                    InetAddress.getByName("192.168.49.255"),
//                    InetAddress.getByName("255.255.255.255"),
                    TaskManager.MULTICAST_PORT
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
