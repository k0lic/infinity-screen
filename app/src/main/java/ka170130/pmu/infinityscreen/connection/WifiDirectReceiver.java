package ka170130.pmu.infinityscreen.connection;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.wifi.p2p.WifiP2pDevice;
import android.net.wifi.p2p.WifiP2pManager;
import android.util.Log;

import androidx.lifecycle.ViewModelProvider;

import ka170130.pmu.infinityscreen.MainActivity;

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

    public WifiDirectReceiver(
        MainActivity mainActivity
    ) {
        this.mainActivity = mainActivity;

        connectionViewModel = new ViewModelProvider(mainActivity).get(ConnectionViewModel.class);
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();

        if (WifiP2pManager.WIFI_P2P_STATE_CHANGED_ACTION.equals(action)) {
            // Check to see if Wi-Fi is enabled and notify appropriate activity
            Log.d(MainActivity.LOG_TAG, "WIFI_P2P_STATE_CHANGED_ACTION");
            handleStateChange();
        } else if (WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION.equals(action)) {
            // Call WifiP2pManager.requestPeers() to get a list of current peers
            Log.d(MainActivity.LOG_TAG, "WIFI_P2P_PEERS_CHANGED_ACTION");
            handlePeersChange();
        } else if (WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION.equals(action)) {
            // Respond to new connection or disconnections
            Log.d(MainActivity.LOG_TAG, "WIFI_P2P_CONNECTION_CHANGED_ACTION");
            handleConnectionChange();
        } else if (WifiP2pManager.WIFI_P2P_THIS_DEVICE_CHANGED_ACTION.equals(action)) {
            // Respond to this device's wifi state changing
            Log.d(MainActivity.LOG_TAG, "WIFI_P2P_THIS_DEVICE_CHANGED_ACTION");
            handleDeviceChange(intent);
        }
    }

    private void handleStateChange() {
        // TODO
    }

    private void handlePeersChange() {
        // TODO
    }

    private void handleConnectionChange() {
        // TODO
    }

    private void handleDeviceChange(Intent intent) {
        WifiP2pDevice device = intent.getParcelableExtra(WifiP2pManager.EXTRA_WIFI_P2P_DEVICE);
        connectionViewModel.setSelfDevice(device);
    }
}
