package ka170130.pmu.infinityscreen;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.wifi.WifiManager;
import android.net.wifi.p2p.WifiP2pDeviceList;
import android.net.wifi.p2p.WifiP2pManager;
import android.util.Log;

public class WifiDirectBroadcastReceiver extends BroadcastReceiver {

    private WifiManager wifiManager;
    private WifiP2pManager manager;
    private WifiP2pManager.Channel channel;
    private MainActivity mainActivity;

    public WifiDirectBroadcastReceiver(
            WifiManager wifiManager,
            WifiP2pManager manager,
            WifiP2pManager.Channel channel,
            MainActivity mainActivity
    ) {
        super();

        this.wifiManager = wifiManager;
        this.manager = manager;
        this.channel = channel;
        this.mainActivity = mainActivity;
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();

        if (WifiP2pManager.WIFI_P2P_STATE_CHANGED_ACTION.equals(action)) {
            // Check to see if Wi-Fi is enabled and notify appropriate activity
            int state = intent.getIntExtra(WifiP2pManager.EXTRA_WIFI_STATE, -1);
            if (state == WifiP2pManager.WIFI_P2P_STATE_ENABLED) {
                // Wifi P2P is enabled
                Log.d(MainActivity.LOG_TAG, "Wifi P2P is enabled");
            } else {
                // Wi-Fi P2P is not enabled
                Log.d(MainActivity.LOG_TAG, "Wifi P2P is not enabled");
                // Enable Wi-Fi
                if (wifiManager.getWifiState() != WifiManager.WIFI_STATE_ENABLED) {
                    wifiManager.setWifiEnabled(true);
                }
            }
        } else if (WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION.equals(action)) {
            // Call WifiP2pManager.requestPeers() to get a list of current peers
            if (manager != null) {
                manager.requestPeers(channel, peers -> {
                    Log.d(MainActivity.LOG_TAG, "peer list:");
                    Log.d(MainActivity.LOG_TAG, peers.toString());
                });
            }
        } else if (WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION.equals(action)) {
            // Respond to new connection or disconnections
        } else if (WifiP2pManager.WIFI_P2P_THIS_DEVICE_CHANGED_ACTION.equals(action)) {
            // Respond to this device's wifi state changing
        }
    }
}
