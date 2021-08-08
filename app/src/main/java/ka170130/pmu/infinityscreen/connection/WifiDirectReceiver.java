package ka170130.pmu.infinityscreen.connection;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.NetworkInfo;
import android.net.wifi.p2p.WifiP2pDevice;
import android.net.wifi.p2p.WifiP2pManager;
import android.util.Log;

import androidx.lifecycle.ViewModelProvider;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import ka170130.pmu.infinityscreen.MainActivity;
import ka170130.pmu.infinityscreen.containers.Message;
import ka170130.pmu.infinityscreen.communication.TaskManager;
import ka170130.pmu.infinityscreen.containers.PeerInfo;
import ka170130.pmu.infinityscreen.helpers.PermissionsHelper;
import ka170130.pmu.infinityscreen.viewmodels.ConnectionViewModel;

public class WifiDirectReceiver extends BroadcastReceiver {

    public static IntentFilter intentFilter;

    public static void initializeIntentFilter() {
        if (intentFilter == null) {
            intentFilter = new IntentFilter();
            intentFilter.addAction(WifiP2pManager.WIFI_P2P_STATE_CHANGED_ACTION);
            intentFilter.addAction(WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION);
            intentFilter.addAction(WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION);
            intentFilter.addAction(WifiP2pManager.WIFI_P2P_THIS_DEVICE_CHANGED_ACTION);
        }
    }

    public static IntentFilter getIntentFilter() {
        return intentFilter;
    }

    private MainActivity mainActivity;
    private ConnectionViewModel connectionViewModel;

    private TaskManager taskManager;

    public WifiDirectReceiver(
        MainActivity mainActivity
    ) {
        this.mainActivity = mainActivity;
        connectionViewModel = new ViewModelProvider(mainActivity).get(ConnectionViewModel.class);

        taskManager = mainActivity.getTaskManager();
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();

        if (WifiP2pManager.WIFI_P2P_STATE_CHANGED_ACTION.equals(action)) {
            // Check to see if Wi-Fi is enabled and notify appropriate activity
            Log.d(MainActivity.LOG_TAG, "WIFI_P2P_STATE_CHANGED_ACTION");
            handleStateChange(intent);
        } else if (WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION.equals(action)) {
            // Call WifiP2pManager.requestPeers() to get a list of current peers
            Log.d(MainActivity.LOG_TAG, "WIFI_P2P_PEERS_CHANGED_ACTION");
            handlePeersChange();
        } else if (WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION.equals(action)) {
            // Respond to new connection or disconnections
            Log.d(MainActivity.LOG_TAG, "WIFI_P2P_CONNECTION_CHANGED_ACTION");
            handleConnectionChange(intent);
        } else if (WifiP2pManager.WIFI_P2P_THIS_DEVICE_CHANGED_ACTION.equals(action)) {
            // Respond to this device's wifi state changing
            Log.d(MainActivity.LOG_TAG, "WIFI_P2P_THIS_DEVICE_CHANGED_ACTION");
            handleDeviceChange(intent);
        }
    }

    private void handleStateChange(Intent intent) {
        int state = intent.getIntExtra(WifiP2pManager.EXTRA_WIFI_STATE, -1);
//        connectionViewModel.setWifiEnabled(state == WifiP2pManager.WIFI_P2P_STATE_ENABLED);
    }

    @SuppressLint("MissingPermission")
    private void handlePeersChange() {
        WifiP2pManager manager = mainActivity.getConnectionManager().getManager();

        if (manager == null) {
            return;
        }

        String[] permissions = { Manifest.permission.ACCESS_FINE_LOCATION };
        PermissionsHelper.request(permissions, s -> {
            Log.d(MainActivity.LOG_TAG, "Going to request peers");
            manager.requestPeers(mainActivity.getConnectionManager().getChannel(), peers -> {
                Log.d(MainActivity.LOG_TAG, "Discovered peers: " + peers.getDeviceList().size());
//                Log.d(MainActivity.LOG_TAG, peers.getDeviceList().toString());

                Iterator<WifiP2pDevice> iterator = peers.getDeviceList().iterator();
                List<PeerInfo> peerList = new ArrayList<>();

                while (iterator.hasNext()) {
                    WifiP2pDevice next = iterator.next();
                    peerList.add(new PeerInfo(next));
                }

                connectionViewModel.setDiscoveryList(peerList);
            });
        });
    }

    private void handleConnectionChange(Intent intent) {
        WifiP2pManager manager = mainActivity.getConnectionManager().getManager();

        if (manager == null) {
            return;
        }

        NetworkInfo networkInfo =
                (NetworkInfo) intent.getParcelableExtra(WifiP2pManager.EXTRA_NETWORK_INFO);

        if (networkInfo.isConnected()) {
            manager.requestConnectionInfo(mainActivity.getConnectionManager().getChannel(), info -> {
                Log.d(MainActivity.LOG_TAG, "Connection info available");
                Log.d(MainActivity.LOG_TAG, info.toString());

                taskManager.setDefaultAddress(info.groupOwnerAddress);

                Boolean isHost = connectionViewModel.getIsHost().getValue();
                if (!info.isGroupOwner) {
                    try {
                        PeerInfo self = connectionViewModel.getSelfDevice().getValue();

                        if (isHost) {
                            // request info from the group owner - should only be called once the initial connection is made
                            taskManager.runSenderTask(Message.newRequestInfoMessage(self));
                        } else {
                            // Say HELLO to the group owner
                            taskManager.runSenderTask(Message.newHelloMessage(self));
                        }
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            });
        } else {
            // it's a disconnect
            Log.d(MainActivity.LOG_TAG, "Connection change: DISCONNECT");
            mainActivity.reset();
        }
    }

    private void handleDeviceChange(Intent intent) {
        WifiP2pDevice device = intent.getParcelableExtra(WifiP2pManager.EXTRA_WIFI_P2P_DEVICE);
        PeerInfo self = new PeerInfo(device);
        connectionViewModel.setSelfDevice(self);
    }
}
