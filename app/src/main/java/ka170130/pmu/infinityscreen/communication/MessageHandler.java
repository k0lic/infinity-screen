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
import java.util.concurrent.Semaphore;

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
import ka170130.pmu.infinityscreen.helpers.LogHelper;
import ka170130.pmu.infinityscreen.io.WriteTask;
import ka170130.pmu.infinityscreen.viewmodels.ConnectionViewModel;
import ka170130.pmu.infinityscreen.viewmodels.LayoutViewModel;
import ka170130.pmu.infinityscreen.viewmodels.MediaViewModel;
import ka170130.pmu.infinityscreen.viewmodels.StateViewModel;
import ka170130.pmu.infinityscreen.viewmodels.UdpViewModel;

public class MessageHandler {

    private TaskManager taskManager;
    private ConnectionManager connectionManager;
    private ConnectionViewModel connectionViewModel;
    private StateViewModel stateViewModel;
    private LayoutViewModel layoutViewModel;
    private MediaViewModel mediaViewModel;
    private UdpViewModel udpViewModel;

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
        this.udpViewModel =
                new ViewModelProvider(mainActivity).get(UdpViewModel.class);
    }

    public void handleMessage(Message message, InetAddress inetAddress) {
        LogHelper.log("Message received: " + message.getMessageType().toString());
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
                case CONTENT_ACK:
                    handleContentAckMessage(message);
                    break;
                case FILE_READY:
                    handleFileReadyMessage(message);
                    break;
                case PLAYBACK_STATUS_COMMAND:
                    handlePlaybackStatusCommandMessage(message);
                    break;
            }
        } catch (Exception e) {
            LogHelper.error(e);
        }
    }

    // handle TEST message
    private void handleTestMessage(InetAddress inetAddress) {
        // do nothing
        String hostName = inetAddress == null ? "<NULL>" : inetAddress.getHostName();
        LogHelper.log("TEST message received from " + hostName);
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
        taskManager.sendToAllInGroup(Message.newStateChangeMessage(state), true);
//        taskManager.runBroadcastTask(Message.newStateChangeMessage(state));
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

        taskManager.sendToAllInGroup(Message.newFileIndexUpdateMessage(index), true);
//        taskManager.runBroadcastTask(Message.newFileIndexUpdateMessage(index));
    }

    // handle CONTENT message
    private void handleContentMessage(Message message) throws IOException, ClassNotFoundException, InterruptedException {
        Boolean isHost = connectionViewModel.getIsHost().getValue();
        if (isHost) {
            // ignore
            return;
        }

        FileContentPackage contentPackage = (FileContentPackage) message.extractObject();
        int fileIndex = contentPackage.getFileIndex();
        int packageId = contentPackage.getPackageId();

        FileInfo fileInfo = mediaViewModel.getFileInfoList().getValue().get(fileIndex);
        int nextPackage = fileInfo.getNextPackage();

        // check if this is the expected package
        if (nextPackage != packageId) {
            // wrong package - dump
            LogHelper.log("WRONG PACKAGE! Expected packageId " + nextPackage + " but instead received " + packageId);
            return;
        }

        // handle empty content
        InetAddress hostAddress =
                connectionViewModel.getHostDevice().getValue().getInetAddress();
        ArrayList<File> createdFiles = mediaViewModel.getCreatedFiles();
        if (contentPackage.getContent() == null) {
            if (contentPackage.isLastPackage()) {
                mediaViewModel.setFileInfoListElementDownloaded(fileIndex, true);

                // activate content if it was not already activated
                if (fileInfo.getContentUri() == null) {
                    File file = createdFiles.get(fileIndex);
                    activateContent(file, fileIndex, hostAddress);
                }

                File file = createdFiles.get(fileIndex);
                LogHelper.log("Real file size: " + file.length());

                // success
                contentMessageHandled(fileIndex, hostAddress, packageId);
            } else {
                LogHelper.log("Received non-final CONTENT message without content");
            }

            return;
        }

        // handle non-empty content
        File file = createdFiles.get(fileIndex);
        boolean create = false;

        // if file was not already created - create new file
        // (do not create on disk, it will be created on another thread so we can respond sooner)
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
            create = true;

            // remember file
            createdFiles.set(fileIndex, file);
            mediaViewModel.setCreatedFiles(createdFiles);
        }

        // activate video content
        if (fileInfo.getFileType() == FileInfo.FileType.VIDEO
                && fileInfo.getNextPackage() >= FileInfo.VIDEO_PACKAGE_THRESHOLD
        ) {
            activateContent(file, fileIndex, hostAddress);
        }

        // Submit 'Append content to file' task
        WriteTask.WriteCommand command =
                new WriteTask.WriteCommand(file, create, contentPackage.getContent());
        taskManager.getWriteTask().enqueue(command);

        // success
        contentMessageHandled(fileIndex, hostAddress, packageId);
    }

    private void activateContent(
            File file,
            int fileIndex,
            InetAddress hostAddress
    ) throws IOException {
        // Remember file path
        mediaViewModel.setFileInfoListElementContent(fileIndex, file.getAbsolutePath());

        // send message to host to let him know file is ready on this device
        int deviceIndex = layoutViewModel.getSelfAuto().getValue().getNumberId() - 1;

        FileOnDeviceReady fileOnDeviceReady =
                new FileOnDeviceReady(deviceIndex, fileIndex);
        taskManager.runSenderTask(
                hostAddress, Message.newFileReadyMessage(fileOnDeviceReady));
    }

    private void contentMessageHandled(int fileIndex, InetAddress hostAddress, int packageId) throws IOException {
        // increment nextPackage
        mediaViewModel.incrementFileInfoListElementNextPackage(fileIndex);

        // send delivery confirmation
        taskManager.runSenderTask(hostAddress, Message.newContentAckMessage(packageId));
    }

    // handle CONTENT_ACK message
    private void handleContentAckMessage(Message message) throws IOException, ClassNotFoundException {
        Integer packageId = (Integer) message.extractObject();
        Semaphore sem = udpViewModel.getSemaphore(packageId);
        if (sem != null) {
            sem.release();
            LogHelper.log("Semaphore#" + packageId + " released");
        }
    }

    // handle FILE_READY message
    private void handleFileReadyMessage(Message message) throws IOException, ClassNotFoundException {
        FileOnDeviceReady fileOnDeviceReady = (FileOnDeviceReady) message.extractObject();

        boolean readyOnAll = mediaViewModel.readyMatrixUpdate(fileOnDeviceReady);
        if (readyOnAll) {
            PlaybackStatusCommand command = new PlaybackStatusCommand(
                    fileOnDeviceReady.getFileIndex(),
//                    FileInfo.PlaybackStatus.PAUSE
                    FileInfo.PlaybackStatus.PLAY
            );
            taskManager.sendToAllInGroup(
                    Message.newPlaybackStatusCommandMessage(command), true);
//            taskManager.runBroadcastTask(Message.newPlaybackStatusCommandMessage(command));
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
