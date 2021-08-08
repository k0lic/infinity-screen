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
    private Socket socket;
    private InetAddress inetAddress;

    public ReceiverTask(
            TaskManager taskManager,
            ConnectionViewModel connectionViewModel,
            Socket socket
    ) {
        this.taskManager = taskManager;
        this.connectionManager = taskManager.getMainActivity().getConnectionManager();
        this.connectionViewModel = connectionViewModel;
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
            handleMessage(message);
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (inputStream != null) {
                try {
                    inputStream.close();
                } catch (IOException e) {
                    // catch logic
                    e.printStackTrace();
                }
            }

            if (socket != null) {
                if (socket.isConnected()) {
                    try {
                        socket.close();
                    } catch (IOException e) {
                        // catch logic
                        e.printStackTrace();
                    }
                }
            }
        }
    }

    private void handleMessage(Message message) {
        Log.d(MainActivity.LOG_TAG, "Message received: " + message.getMessageType().toString());
        try {
            switch (message.getMessageType()) {
                case HELLO:
                    handleHelloMessage(message);
                    break;
                case PEER_INFO:
                    handlePeerInfoMessage(message);
                    break;
                case REQUEST_INFO:
                    handleRequestInfoMessage(message);
                    break;
                case HOST_ACK:
                    handleHostAckMessage(message);
                    break;
                case DISCONNECT:
                    handleDisconnectMessage();
                    break;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // handle HELLO message
    private void handleHelloMessage(Message message) throws IOException, ClassNotFoundException {
        PeerInfo receivedInfo = (PeerInfo) message.extractObject();
        PeerInetAddressInfo moreInfo =
                new PeerInetAddressInfo(receivedInfo, inetAddress);

        Boolean isHost = connectionViewModel.getIsHost().getValue();
        if (isHost) {
            // remember the peer
            rememberPeer(moreInfo);
        } else {
            // forward the peer info to the host
            PeerInetAddressInfo host = connectionViewModel.getHostDevice().getValue();
            Message forward = Message.newPeerInfoMessage(moreInfo);
            taskManager.runSenderTask(host.getInetAddress(), forward);
        }
    }

    // handle PEER_INFO message
    private void handlePeerInfoMessage(Message message) throws IOException, ClassNotFoundException {
        PeerInetAddressInfo peerInfo = (PeerInetAddressInfo) message.extractObject();
        rememberPeer(peerInfo);
    }

    // handle REQUEST_INFO message
    private void handleRequestInfoMessage(Message message) throws IOException, ClassNotFoundException {
        // remember host device
        rememberHost(message);

        // respond with HELLO message
        PeerInfo self = connectionViewModel.getSelfDevice().getValue();
        Message response = Message.newHelloMessage(self);
        taskManager.runSenderTask(inetAddress, response);
    }

    // handle HOST_ACK message
    private void handleHostAckMessage(Message message) throws IOException, ClassNotFoundException {
        rememberHost(message);
    }

    // handle DISCONNECT message
    private void handleDisconnectMessage() {
        connectionManager.disconnect();
    }

    private void rememberPeer(PeerInetAddressInfo peerInfo) throws IOException {
        connectionViewModel.selectDevice(peerInfo);

        // respond with HOST_ACK
        PeerInfo self = connectionViewModel.getSelfDevice().getValue();
        Message response = Message.newHostAckMessage(self);
        taskManager.runSenderTask(peerInfo.getInetAddress(), response);
    }

    private void rememberHost(Message message) throws IOException, ClassNotFoundException {
        PeerInfo receivedInfo = (PeerInfo) message.extractObject();
        PeerInetAddressInfo hostInfo =
                new PeerInetAddressInfo(receivedInfo, inetAddress);
        connectionViewModel.setHostDevice(hostInfo);
    }
}
