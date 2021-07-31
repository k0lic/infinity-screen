package ka170130.pmu.infinityscreen;

import android.Manifest;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.NetworkInfo;
import android.net.wifi.WifiManager;
import android.net.wifi.p2p.WifiP2pDevice;
import android.net.wifi.p2p.WifiP2pDeviceList;
import android.net.wifi.p2p.WifiP2pManager;
import android.util.Log;

import androidx.core.app.ActivityCompat;
import androidx.lifecycle.ViewModelProvider;

import java.util.Iterator;
import java.util.function.Consumer;

public class WifiDirectBroadcastReceiver extends BroadcastReceiver {

    private WifiManager wifiManager;
    private WifiP2pManager manager;
    private WifiP2pManager.Channel channel;
    private MainActivity mainActivity;
    private WifiDeviceViewModel wifiDeviceViewModel;

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

        wifiDeviceViewModel = new ViewModelProvider(mainActivity).get(WifiDeviceViewModel.class);
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();

        if (WifiP2pManager.WIFI_P2P_STATE_CHANGED_ACTION.equals(action)) {
            // Check to see if Wi-Fi is enabled and notify appropriate activity
            Log.d(MainActivity.LOG_TAG, "WIFI_P2P_STATE_CHANGED_ACTION");

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
            Log.d(MainActivity.LOG_TAG, "WIFI_P2P_PEERS_CHANGED_ACTION");

            if (manager != null) {
                // Permission should already be granted, this code removes annoying red code color
                if (ActivityCompat.checkSelfPermission(mainActivity, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                    return;
                }
                if (ActivityCompat.checkSelfPermission(mainActivity, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                    return;
                }

                manager.requestPeers(channel, peers -> {
                    Log.d(MainActivity.LOG_TAG, "peer list:");

                    Iterator<WifiP2pDevice> iterator = peers.getDeviceList().iterator();
                    StringBuilder stringBuilder = new StringBuilder();
                    while (iterator.hasNext()) {
                        WifiP2pDevice next = iterator.next();
                        if (stringBuilder.length() > 0) {
                            stringBuilder.append(", ");
                        }
                        stringBuilder.append(next.deviceName);
                    }
                    Log.d(MainActivity.LOG_TAG, stringBuilder.toString());

                    wifiDeviceViewModel.setDiscoveryList(peers.getDeviceList());
                });
            }
        } else if (WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION.equals(action)) {
            // Respond to new connection or disconnections
            Log.d(MainActivity.LOG_TAG, "WIFI_P2P_CONNECTION_CHANGED_ACTION");

            if (manager == null) {
                return;
            }

            NetworkInfo networkInfo =
                    (NetworkInfo) intent.getParcelableExtra(WifiP2pManager.EXTRA_NETWORK_INFO);

            if (networkInfo.isConnected()) {
                manager.requestConnectionInfo(channel, mainActivity.getConnectionInfoListener());
            } else {
                // it's a disconnect
                Log.d(MainActivity.LOG_TAG, "Disconnected");
            }
        } else if (WifiP2pManager.WIFI_P2P_THIS_DEVICE_CHANGED_ACTION.equals(action)) {
            // Respond to this device's wifi state changing
            Log.d(MainActivity.LOG_TAG, "WIFI_P2P_THIS_DEVICE_CHANGED_ACTION");
        }
    }
}
