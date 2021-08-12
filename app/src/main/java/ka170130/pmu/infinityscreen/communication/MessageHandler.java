package ka170130.pmu.infinityscreen.communication;

import android.net.Uri;
import android.util.Log;

import androidx.lifecycle.ViewModelProvider;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.InetAddress;
import java.util.ArrayList;

import ka170130.pmu.infinityscreen.MainActivity;
import ka170130.pmu.infinityscreen.connection.ConnectionManager;
import ka170130.pmu.infinityscreen.containers.FileContentPackage;
import ka170130.pmu.infinityscreen.containers.FileInfo;
import ka170130.pmu.infinityscreen.containers.FileOnDeviceReady;
import ka170130.pmu.infinityscreen.containers.Message;
import ka170130.pmu.infinityscreen.containers.PeerInetAddressInfo;
import ka170130.pmu.infinityscreen.containers.PeerInfo;
import ka170130.pmu.infinityscreen.containers.PlaybackStatusCommand;
import ka170130.pmu.infinityscreen.containers.TransformInfo;
import ka170130.pmu.infinityscreen.viewmodels.ConnectionViewModel;
import ka170130.pmu.infinityscreen.viewmodels.LayoutViewModel;
import ka170130.pmu.infinityscreen.viewmodels.MediaViewModel;
import ka170130.pmu.infinityscreen.viewmodels.StateViewModel;

public class MessageHandler {

    private TaskManager taskManager;
    private ConnectionManager connectionManager;
    private ConnectionViewModel connectionViewModel;
    private StateViewModel stateViewModel;
    private LayoutViewModel layoutViewModel;
    private MediaViewModel mediaViewModel;

    public MessageHandler(
            TaskManager taskManager,
            MainActivity mainActivity
    ) {
        this.taskManager = taskManager;

        this.connectionManager = mainActivity.getConnectionManager();
        this.connectionViewModel =
                new ViewModelProvider(mainActivity).get(ConnectionViewModel.class);
        this.stateViewModel =
                new ViewModelProvider(mainActivity).get(StateViewModel.class);
        this.layoutViewModel =
                new ViewModelProvider(mainActivity).get(LayoutViewModel.class);
        this.mediaViewModel =
                new ViewModelProvider(mainActivity).get(MediaViewModel.class);
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
                    handleTransformMessage(message);
                    break;
                case TRANSFORM_LIST_UPDATE:
                    handleTransformListUpdateMessage(message);
                    break;
                case VIEWPORT_UPDATE:
                    handleViewportUpdateMessage(message);
                    break;
                case FILE_INFO_LIST_UPDATE:
                    handleFileInfoListUpdateMessage(message);
                    break;
                case FILE_INDEX_UPDATE:
                    handleFileIndexUpdateMessage(message);
                    break;
                case FILE_INDEX_UPDATE_REQUEST:
                    handleFileIndexUpdateRequestMessage(message);
                    break;
                case CONTENT:
                    handleContentMessage(message);
                    break;
                case FILE_READY:
                    handleFileReadyMessage(message);
                    break;
                case PLAYBACK_STATUS_COMMAND:
                    handlePlaybackStatusCommandMessage(message);
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
    private void handleTransformMessage(Message message) throws IOException, ClassNotFoundException {
        TransformInfo transformInfo = (TransformInfo) message.extractObject();
        layoutViewModel.addTransform(transformInfo);
    }

    // handle TRANSFORM_LIST_UPDATE message
    private void handleTransformListUpdateMessage(Message message) throws IOException, ClassNotFoundException {
        Boolean isHost = connectionViewModel.getIsHost().getValue();

        // Host should not react to Transform List Updates as he is the one that issues them upon change
        if (!isHost) {
            ArrayList<TransformInfo> list = (ArrayList<TransformInfo>) message.extractObject();
            layoutViewModel.setTransformList(list);
        }
    }

    // handle VIEWPORT_UPDATE message
    private void handleViewportUpdateMessage(Message message) throws IOException, ClassNotFoundException {
        Boolean isHost = connectionViewModel.getIsHost().getValue();

        // Host should not react to Viewport Updates as he is the one that issues them upon change
        if (!isHost) {
            TransformInfo viewport = (TransformInfo) message.extractObject();
            layoutViewModel.setViewport(viewport);
        }
    }

    // handle FILE_INFO_LIST_UPDATE message
    private void handleFileInfoListUpdateMessage(Message message) throws IOException, ClassNotFoundException {
        ArrayList<FileInfo> fileInfos = (ArrayList<FileInfo>) message.extractObject();
        mediaViewModel.setFileInfoList(fileInfos);

        Boolean isHost = connectionViewModel.getIsHost().getValue();
        int numberOfFiles = fileInfos.size();

        if (isHost) {
            int numberOfDevices = connectionViewModel.getGroupList().getValue().size() + 1;
            mediaViewModel.setReadyMatrix(new boolean[numberOfFiles][numberOfDevices]);

            int deviceIndex = layoutViewModel.getSelfAuto().getValue().getNumberId() - 1;
            for (int i = 0; i < numberOfFiles; i++) {
                mediaViewModel.setReadyMatrixElement(i, deviceIndex, true);
            }
        } else {
            ArrayList<File> listOfNulls = new ArrayList<>();
            for (int i = 0; i < numberOfFiles; i++) {
                listOfNulls.add(null);
            }
            mediaViewModel.setCreatedFiles(listOfNulls);
        }
    }

    // handle FILE_INDEX_UPDATE message
    private void handleFileIndexUpdateMessage(Message message) throws IOException, ClassNotFoundException {
        Integer index = (Integer) message.extractObject();
        mediaViewModel.setCurrentFileIndex(index);
    }

    // handle FILE_INDEX_UPDATE_REQUEST message
    private void handleFileIndexUpdateRequestMessage(Message message) throws IOException, ClassNotFoundException {
        Integer index = (Integer) message.extractObject();

        if (mediaViewModel.isFileInfoListIndexOutOfBounds(index)) {
            // ignore
            return;
        }

        taskManager.runBroadcastTask(Message.newFileIndexUpdateMessage(index));
    }

    // handle CONTENT message
    private void handleContentMessage(Message message) throws IOException, ClassNotFoundException {
        Boolean isHost = connectionViewModel.getIsHost().getValue();
        if (isHost) {
            // ignore
            return;
        }

        FileContentPackage contentPackage = (FileContentPackage) message.extractObject();
        int fileIndex = contentPackage.getFileIndex();

        // handle empty content
        ArrayList<File> createdFiles = mediaViewModel.getCreatedFiles();
        if (contentPackage.getContent() == null) {
            if (contentPackage.isLastPackage()) {
                // Remember file as uri
                File file = createdFiles.get(fileIndex);
                Uri uri = Uri.fromFile(file);
                mediaViewModel.setFileInfoListElementContent(fileIndex, uri.toString());

                // send message to host to let him know file is ready on this device
                InetAddress hostAddress =
                        connectionViewModel.getHostDevice().getValue().getInetAddress();
                int deviceIndex = layoutViewModel.getSelfAuto().getValue().getNumberId() - 1;

                FileOnDeviceReady fileOnDeviceReady =
                        new FileOnDeviceReady(deviceIndex, fileIndex);
                taskManager.runSenderTask(
                        hostAddress, Message.newFileReadyMessage(fileOnDeviceReady));
            } else {
                Log.d(MainActivity.LOG_TAG, "Received non-final CONTENT message without content");
            }

            return;
        }

        // handle non-empty content
        File file = createdFiles.get(fileIndex);
        FileInfo fileInfo = mediaViewModel.getFileInfoList().getValue().get(fileIndex);

        // if file was not already created - create new file
        if (file == null) {
            String fileName =
                    "infinity-screen-"
                            + System.currentTimeMillis()
                            + "." + fileInfo.getExtension();
            file = new File(
                    taskManager.getMainActivity().getApplicationContext()
                            .getExternalFilesDir("InfinityScreen"),
                    fileName
            );

            // if directory was not already created - create directory
            File dirs = new File(file.getParent());
            if (!dirs.exists()) {
                dirs.mkdirs();
            }

            file.createNewFile();

            // remember file
            createdFiles.set(fileIndex, file);
            mediaViewModel.setCreatedFiles(createdFiles);
        }

        // Append content to file
        OutputStream outputStream = new FileOutputStream(file, true);
        outputStream.write(contentPackage.getContent());
        outputStream.close();
    }

    // handle FILE_READY message
    private void handleFileReadyMessage(Message message) throws IOException, ClassNotFoundException {
        FileOnDeviceReady fileOnDeviceReady = (FileOnDeviceReady) message.extractObject();

        boolean readyOnAll = mediaViewModel.readyMatrixUpdate(fileOnDeviceReady);
        if (readyOnAll) {
            PlaybackStatusCommand command = new PlaybackStatusCommand(
                    fileOnDeviceReady.getFileIndex(),
                    FileInfo.PlaybackStatus.PLAY
            );
            taskManager.runBroadcastTask(Message.newPlaybackStatusCommandMessage(command));
        }
    }

    // handle PLAYBACK_STATUS_COMMAND message
    private void handlePlaybackStatusCommandMessage(Message message) throws IOException, ClassNotFoundException {
        PlaybackStatusCommand command = (PlaybackStatusCommand) message.extractObject();

        mediaViewModel.setFileInfoListElementPlaybackStatus(command.getFileIndex(), command.getPlaybackStatus());
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
