package ka170130.pmu.infinityscreen;

import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.DialogFragment;
import androidx.lifecycle.ViewModelProvider;

import android.content.Context;
import android.location.LocationManager;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;

import ka170130.pmu.infinityscreen.communication.TaskManager;
import ka170130.pmu.infinityscreen.connection.ConnectionManager;
import ka170130.pmu.infinityscreen.viewmodels.ConnectionViewModel;
import ka170130.pmu.infinityscreen.connection.WifiDirectReceiver;
import ka170130.pmu.infinityscreen.databinding.ActivityMainBinding;
import ka170130.pmu.infinityscreen.dialogs.FinishDialog;
import ka170130.pmu.infinityscreen.dialogs.SettingsPanelDialog;
import ka170130.pmu.infinityscreen.helpers.PermissionsHelper;
import ka170130.pmu.infinityscreen.viewmodels.LayoutViewModel;
import ka170130.pmu.infinityscreen.viewmodels.StateViewModel;

public class MainActivity extends AppCompatActivity {

    public final static String LOG_TAG = "default-log-tag";
    private final static String MULTICAST_LOCK = "infinity-screen-multicast";

    private ActivityMainBinding binding;

    private ConnectionViewModel connectionViewModel;
    private LayoutViewModel layoutViewModel;
    private StateViewModel stateViewModel;

    private ConnectionManager connectionManager;
    private WifiDirectReceiver receiver;
    private TaskManager taskManager;

    public WifiDirectReceiver getReceiver() {
        return receiver;
    }

    public ConnectionManager getConnectionManager() {
        return connectionManager;
    }

    public TaskManager getTaskManager() {
        return taskManager;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        connectionViewModel = new ViewModelProvider(this).get(ConnectionViewModel.class);
        layoutViewModel = new ViewModelProvider(this).get(LayoutViewModel.class);
        stateViewModel = new ViewModelProvider(this).get(StateViewModel.class);

        // Initialize Helpers
        PermissionsHelper.init(this);

        // Setup Connection Manager
        connectionManager = new ConnectionManager(this);

        // Initialize P2P connection
        Pair<Boolean, String> init = connectionManager.initP2p();
        if (!init.first) {
            // Close Application - Device does not meet the specifications
            DialogFragment dialog = new FinishDialog(init.second);
            dialog.show(getSupportFragmentManager(), "FinishDialog");
            return;
        }

        // Turn on WiFi
        connectionManager.getWifiManager().setWifiEnabled(true);
        askForWiFi();

        // Setup Task Manager
        taskManager = new TaskManager(this);

        // Setup Multicast
        WifiManager.MulticastLock multicastLock =
                connectionManager.getWifiManager().createMulticastLock(MULTICAST_LOCK);
        multicastLock.acquire();

        // Setup Server Tasks
        taskManager.runServerTask();
        taskManager.runMulticastServerTask();

        // Setup Wifi Direct Broadcast Receiver
        WifiDirectReceiver.initializeIntentFilter();
        receiver = new WifiDirectReceiver(this);

        // Discover Peers - Become Discoverable
        connectionManager.discoverPeers();
    }

    public void reset() {
        connectionViewModel.reset();
        layoutViewModel.reset();
        stateViewModel.reset();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (receiver != null) {
            registerReceiver(receiver, WifiDirectReceiver.getIntentFilter());
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (receiver != null) {
            unregisterReceiver(receiver);
        }
    }

    public boolean askForWiFi() {
        WifiManager wifiManager = connectionManager.getWifiManager();

        if (wifiManager.getWifiState() != WifiManager.WIFI_STATE_ENABLED
                && wifiManager.getWifiState() != WifiManager.WIFI_STATE_ENABLING
        ) {
            String message = getResources().getString(R.string.dialog_settings_wifi);
            DialogFragment dialog = new SettingsPanelDialog(message, Settings.ACTION_WIFI_SETTINGS);
            dialog.show(getSupportFragmentManager(), "WiFiSettingsPanelDialog");
            return false;
        }

        return true;
    }

    public boolean askForGps() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            LocationManager locationManager = (LocationManager) getApplicationContext()
                    .getSystemService(Context.LOCATION_SERVICE);
            if (!locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
                String message = getResources().getString(R.string.dialog_settings_gps);
                DialogFragment dialog =
                        new SettingsPanelDialog(message, Settings.ACTION_LOCATION_SOURCE_SETTINGS);
                dialog.show(getSupportFragmentManager(), "GPSSettingsPanelDialog");
                return false;
            }
        }

        return true;
    }

    public boolean checkPeerDiscovery() {
        boolean possible = askForWiFi();

        if (possible) {
            possible = askForGps();
        }

        return possible;
    }

    public static class Pair<T, U> {
        public T first;
        public U second;

        public Pair(T first, U second) {
            this.first = first;
            this.second = second;
        }
    }
}