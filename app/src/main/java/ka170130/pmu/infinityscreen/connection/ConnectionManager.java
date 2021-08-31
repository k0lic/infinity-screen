package ka170130.pmu.infinityscreen.connection;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.pm.PackageManager;
import android.net.wifi.WifiManager;
import android.net.wifi.p2p.WifiP2pConfig;
import android.net.wifi.p2p.WifiP2pManager;
import android.util.Log;

import ka170130.pmu.infinityscreen.MainActivity;
import ka170130.pmu.infinityscreen.containers.PeerInfo;
import ka170130.pmu.infinityscreen.helpers.LogHelper;
import ka170130.pmu.infinityscreen.helpers.PermissionsHelper;

public class ConnectionManager {

    private final MainActivity mainActivity;
    private WifiManager wifiManager;
    private WifiP2pManager manager;
    private WifiP2pManager.Channel channel;

    public ConnectionManager(
            MainActivity mainActivity
    ) {
        this.mainActivity = mainActivity;
    }

    public WifiManager getWifiManager() {
        return wifiManager;
    }

    public WifiP2pManager getManager() {
        return manager;
    }

    public WifiP2pManager.Channel getChannel() {
        return channel;
    }

    public MainActivity.Pair<Boolean, String> initP2p() {
        MainActivity.Pair<Boolean, String> pair = new MainActivity.Pair<>(false, "");

        // Device capability definition check
        if (!mainActivity.getPackageManager().hasSystemFeature(PackageManager.FEATURE_WIFI_DIRECT)) {
            pair.second = "Wi-Fi Direct is not supported by this device.";
            return pair;
        }

        // Hardware capability check
        wifiManager = (WifiManager) mainActivity.getApplicationContext()
                .getSystemService(Context.WIFI_SERVICE);
        if (wifiManager == null) {
            pair.second = "Cannot get Wi-Fi system service.";
            return pair;
        }

        if (!wifiManager.isP2pSupported()) {
            pair.second = "Wi-Fi Direct is not supported by the hardware or Wi-Fi is off.";
            return pair;
        }

        manager = (WifiP2pManager) mainActivity.getSystemService(Context.WIFI_P2P_SERVICE);
        if (manager == null) {
            pair.second = "Cannot get Wi-Fi Direct system service.";
            return pair;
        }

        channel = manager.initialize(mainActivity, mainActivity.getMainLooper(), new WifiP2pManager.ChannelListener() {
            @Override
            public void onChannelDisconnected() {
                // TODO: try to restore channel?
                LogHelper.log("UhOh Channel Disconnected! :( Sadge");
            }
        });
        if (channel == null) {
            pair.second = "Cannot initialize Wi-Fi Direct.";
            return pair;
        }

        pair.first = true;
        return pair;
    }

    @SuppressLint("MissingPermission")
    public void discoverPeers() {
        String[] permissions = { Manifest.permission.ACCESS_FINE_LOCATION };
        PermissionsHelper.request(permissions, s -> {
            LogHelper.log("Discovering Peers");
            manager.discoverPeers(channel, new WifiP2pManager.ActionListener() {
                @Override
                public void onSuccess() {
                    LogHelper.log("Discover Peers SUCCEEDED");
                }

                @Override
                public void onFailure(int reason) {
                    LogHelper.log("Discover Peers FAILED");
                }
            });
        });
    }

    @SuppressLint("MissingPermission")
    public void connect(PeerInfo device) {
        String[] permissions = { Manifest.permission.ACCESS_FINE_LOCATION };
        PermissionsHelper.request(permissions, s -> {
            LogHelper.log("Connecting");

            WifiP2pConfig config = new WifiP2pConfig();
            config.deviceAddress = device.getDeviceAddress();

            manager.connect(channel, config, new WifiP2pManager.ActionListener() {
                @Override
                public void onSuccess() {
                    LogHelper.log("Connection to " + device.getDeviceName() + " SUCCEEDED");
                }

                @Override
                public void onFailure(int reason) {
                    LogHelper.log("Connection to " + device.getDeviceName() + " FAILED");
                }
            });
        });
    }

    public void disconnect() {
        manager.removeGroup(channel, new WifiP2pManager.ActionListener() {
            @Override
            public void onSuccess() {
                LogHelper.log("Disconnect SUCCESSFUL");
            }

            @Override
            public void onFailure(int reason) {
                LogHelper.log("Disconnect FAILED");
            }
        });
    }
}
