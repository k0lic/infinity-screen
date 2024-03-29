package ka170130.pmu.infinityscreen;

import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.DialogFragment;
import androidx.lifecycle.ViewModel;
import androidx.lifecycle.ViewModelProvider;

import android.content.Context;
import android.location.LocationManager;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.util.Log;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import ka170130.pmu.infinityscreen.communication.TaskManager;
import ka170130.pmu.infinityscreen.connection.ConnectionManager;
import ka170130.pmu.infinityscreen.helpers.FileSelectionHelper;
import ka170130.pmu.infinityscreen.layout.LayoutManager;
import ka170130.pmu.infinityscreen.media.MediaManager;
import ka170130.pmu.infinityscreen.viewmodels.ConnectionViewModel;
import ka170130.pmu.infinityscreen.connection.WifiDirectReceiver;
import ka170130.pmu.infinityscreen.databinding.ActivityMainBinding;
import ka170130.pmu.infinityscreen.dialogs.FinishDialog;
import ka170130.pmu.infinityscreen.dialogs.SettingsPanelDialog;
import ka170130.pmu.infinityscreen.helpers.PermissionsHelper;
import ka170130.pmu.infinityscreen.viewmodels.LayoutViewModel;
import ka170130.pmu.infinityscreen.viewmodels.MediaViewModel;
import ka170130.pmu.infinityscreen.viewmodels.Resettable;
import ka170130.pmu.infinityscreen.viewmodels.StateViewModel;

public class MainActivity extends AppCompatActivity {

    public final static String LOG_TAG = "default-log-tag";

    private ActivityMainBinding binding;

    private StateViewModel stateViewModel;
    private List<Resettable> resettables;

    private ConnectionManager connectionManager;
    private WifiDirectReceiver receiver;
    private TaskManager taskManager;
    private LayoutManager layoutManager;
    private MediaManager mediaManager;

    public WifiDirectReceiver getReceiver() {
        return receiver;
    }

    public ConnectionManager getConnectionManager() {
        return connectionManager;
    }

    public TaskManager getTaskManager() {
        return taskManager;
    }

    public LayoutManager getLayoutManager() {
        return layoutManager;
    }

    public MediaManager getMediaManager() {
        return mediaManager;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        // Initialize all the Resettable View Models
        resettables = new ArrayList<>();
        resettables.add(new ViewModelProvider(this).get(ConnectionViewModel.class));
        resettables.add(new ViewModelProvider(this).get(LayoutViewModel.class));
        resettables.add(
                stateViewModel = new ViewModelProvider(this).get(StateViewModel.class));
        resettables.add(new ViewModelProvider(this).get(MediaViewModel.class));

        // Initialize Helpers
        PermissionsHelper.init(this);
        FileSelectionHelper.init(this);

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

        // Setup Server Tasks
        taskManager.runServerTask();
        taskManager.runBroadcastServerTask();

        // Setup Wifi Direct Broadcast Receiver
        WifiDirectReceiver.initializeIntentFilter();
        receiver = new WifiDirectReceiver(this);

        // Setup Managers
        layoutManager = new LayoutManager(this);
        mediaManager = new MediaManager(this);

        // Discover Peers - Become Discoverable
        connectionManager.discoverPeers();

        // logging
        stateViewModel.getState().observe(this, state -> {
            String s = state == null ? "<NULL>" : state.toString();
            Log.d(LOG_TAG, "AppState changed to " + s);
        });
    }

    public void reset() {
        Iterator<Resettable> iterator = resettables.iterator();
        while (iterator.hasNext()) {
            Resettable next = iterator.next();
            next.reset();
        }
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