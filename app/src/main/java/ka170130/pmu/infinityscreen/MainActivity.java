package ka170130.pmu.infinityscreen;

import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;

import ka170130.pmu.infinityscreen.connection.WifiDirectReceiver;
import ka170130.pmu.infinityscreen.databinding.ActivityMainBinding;

public class MainActivity extends AppCompatActivity {

    public final static String LOG_TAG = "default-log-tag";

    private ActivityMainBinding binding;
    private WifiDirectReceiver receiver;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        // Wifi Direct Broadcast Receiver
        WifiDirectReceiver.initializeIntentFilter();
        receiver = new WifiDirectReceiver(this);
    }

    @Override
    protected void onResume() {
        super.onResume();
        registerReceiver(receiver, WifiDirectReceiver.getIntentFilter());
    }

    @Override
    protected void onPause() {
        super.onPause();
        unregisterReceiver(receiver);
    }
}