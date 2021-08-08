package ka170130.pmu.infinityscreen.communication;

import android.util.Log;

import java.io.IOException;
import java.net.InetAddress;

import ka170130.pmu.infinityscreen.MainActivity;
import ka170130.pmu.infinityscreen.connection.ConnectionManager;
import ka170130.pmu.infinityscreen.containers.Message;
import ka170130.pmu.infinityscreen.containers.PeerInetAddressInfo;
import ka170130.pmu.infinityscreen.containers.PeerInfo;
import ka170130.pmu.infinityscreen.viewmodels.ConnectionViewModel;
import ka170130.pmu.infinityscreen.viewmodels.StateViewModel;

public class MessageHandler {

    private TaskManager taskManager;
    private ConnectionManager connectionManager;
    private ConnectionViewModel connectionViewModel;
    private StateViewModel stateViewModel;

    public MessageHandler(
            TaskManager taskManager,
            ConnectionManager connectionManager,
            ConnectionViewModel connectionViewModel,
            StateViewModel stateViewModel
    ) {
        this.taskManager = taskManager;
        this.connectionManager = connectionManager;
        this.connectionViewModel = connectionViewModel;
        this.stateViewModel = stateViewModel;
    }

    public void handleMessage(Message message, InetAddress inetAddress) {
        Log.d(MainActivity.LOG_TAG, "Message received: " + message.getMessageType().toString());
        try {
            switch (message.getMessageType()) {
                case TEST:
                    handleTestMessage(inetAddress);
                    break;
                case HELLO:
                    handleHelloMessage(message, inetAddress);
                    break;
                case PEER_INFO:
                    handlePeerInfoMessage(message);
                    break;
                case REQUEST_INFO:
                    handleRequestInfoMessage(message, inetAddress);
                    break;
                case HOST_ACK:
                    handleHostAckMessage(message, inetAddress);
                    break;
                case DISCONNECT:
                    handleDisconnectMessage();
                    break;
                case STATE_CHANGE_REQUEST:
                    handleStateChangeRequestMessage(message);
                    break;
                case STATE_CHANGE:
                    handleStateChangeMessage(message);
                    break;
                case REQUEST_TRANSFORM:
                    handleRequestTransformMessage();
                    break;
                case TRANSFORM:
                    handleTransformMessage();
                    break;
                case TRANSFORM_LIST_UPDATE:
                    handleTransformListUpdateMessage();
                    break;
            }
        } catch (Exception e) {
            Log.d(MainActivity.LOG_TAG, e.toString());
            e.printStackTrace();
        }
    }

    // handle TEST message
    private void handleTestMessage(InetAddress inetAddress) {
        // do nothing
        String hostName = inetAddress == null ? "<NULL>" : inetAddress.getHostName();
        Log.d(MainActivity.LOG_TAG, "TEST message received from " + hostName);
    }

    // handle HELLO message
    private void handleHelloMessage(Message message, InetAddress inetAddress) throws IOException, ClassNotFoundException {
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
    private void handleRequestInfoMessage(Message message, InetAddress inetAddress) throws IOException, ClassNotFoundException {
        // remember host device
        rememberHost(message, inetAddress);

        // respond with HELLO message
        PeerInfo self = connectionViewModel.getSelfDevice().getValue();
        Message response = Message.newHelloMessage(self);
        taskManager.runSenderTask(inetAddress, response);
    }

    // handle HOST_ACK message
    private void handleHostAckMessage(Message message, InetAddress inetAddress) throws IOException, ClassNotFoundException {
        rememberHost(message, inetAddress);
    }

    // handle DISCONNECT message
    private void handleDisconnectMessage() {
        connectionManager.disconnect();
    }

    // handle STATE_CHANGE_REQUEST message
    private void handleStateChangeRequestMessage(Message message) throws IOException, ClassNotFoundException {
        StateViewModel.AppState state = (StateViewModel.AppState) message.extractObject();
        // TODO: check if change request should be granted
        taskManager.runBroadcastTask(Message.newStateChangeMessage(state));
    }

    // handle STATE_CHANGE message
    private void handleStateChangeMessage(Message message) throws IOException, ClassNotFoundException {
        StateViewModel.AppState state = (StateViewModel.AppState) message.extractObject();
        stateViewModel.setState(state);
    }

    // handle REQUEST_TRANSFORM message
    private void handleRequestTransformMessage() {
        // TODO
    }

    // handle TRANSFORM message
    private void handleTransformMessage() {
        // TODO
    }

    // handle TRANSFORM_LIST_UPDATE message
    private void handleTransformListUpdateMessage() {
        // TODO
    }

    private void rememberPeer(PeerInetAddressInfo peerInfo) throws IOException {
        connectionViewModel.selectDevice(peerInfo);

        // respond with HOST_ACK
        PeerInfo self = connectionViewModel.getSelfDevice().getValue();
        Message response = Message.newHostAckMessage(self);
        taskManager.runSenderTask(peerInfo.getInetAddress(), response);
    }

    private void rememberHost(Message message, InetAddress inetAddress) throws IOException, ClassNotFoundException {
        PeerInfo receivedInfo = (PeerInfo) message.extractObject();
        PeerInetAddressInfo hostInfo =
                new PeerInetAddressInfo(receivedInfo, inetAddress);
        connectionViewModel.setHostDevice(hostInfo);
    }
}
