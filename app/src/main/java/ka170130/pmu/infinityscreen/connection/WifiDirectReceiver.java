package ka170130.pmu.infinityscreen.connection;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.NetworkInfo;
import android.net.wifi.p2p.WifiP2pDevice;
import android.net.wifi.p2p.WifiP2pInfo;
import android.net.wifi.p2p.WifiP2pManager;
import android.util.Log;

import androidx.lifecycle.ViewModelProvider;

import java.util.Iterator;

import ka170130.pmu.infinityscreen.MainActivity;
import ka170130.pmu.infinityscreen.communication.TaskManager;
import ka170130.pmu.infinityscreen.helpers.PermissionsHelper;

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

    private ConnectionManager connectionManager;
    private ConnectionViewModel connectionViewModel;

    private TaskManager taskManager;

    public WifiDirectReceiver(
        MainActivity mainActivity
    ) {
        connectionManager = mainActivity.getConnectionManager();
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
        connectionViewModel.setWifiEnabled(state == WifiP2pManager.WIFI_P2P_STATE_ENABLED);
    }

    @SuppressLint("MissingPermission")
    private void handlePeersChange() {
        WifiP2pManager manager = connectionManager.getManager();

        if (manager == null) {
            return;
        }

        String[] permissions = { Manifest.permission.ACCESS_FINE_LOCATION };
        PermissionsHelper.request(permissions, s -> {
            Log.d(MainActivity.LOG_TAG, "Going to request peers");
            manager.requestPeers(connectionManager.getChannel(), peers -> {
                Log.d(MainActivity.LOG_TAG, "Discovered peers:");
                Log.d(MainActivity.LOG_TAG, peers.getDeviceList().toString());
                connectionViewModel.setDiscoveryList(peers.getDeviceList());
            });
        });
    }

    private void handleConnectionChange(Intent intent) {
        // TODO
        WifiP2pManager manager = connectionManager.getManager();

        if (manager == null) {
            return;
        }

        NetworkInfo networkInfo =
                (NetworkInfo) intent.getParcelableExtra(WifiP2pManager.EXTRA_NETWORK_INFO);

        if (networkInfo.isConnected()) {
            manager.requestConnectionInfo(connectionManager.getChannel(), info -> {
                Log.d(MainActivity.LOG_TAG, "Connection info available");
                Log.d(MainActivity.LOG_TAG, info.toString());
                // TODO
                taskManager.setInfo(info);
            });
        } else {
            // it's a disconnect
            Log.d(MainActivity.LOG_TAG, "Connection change: DISCONNECT");
            // TODO
        }
    }

    private void handleDeviceChange(Intent intent) {
        WifiP2pDevice device = intent.getParcelableExtra(WifiP2pManager.EXTRA_WIFI_P2P_DEVICE);
        connectionViewModel.setSelfDevice(device);
    }
}
