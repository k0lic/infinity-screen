package ka170130.pmu.infinityscreen.connection;

import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import ka170130.pmu.infinityscreen.MainActivity;
import ka170130.pmu.infinityscreen.R;
import ka170130.pmu.infinityscreen.databinding.FragmentConnectedBinding;
import ka170130.pmu.infinityscreen.databinding.FragmentHomeBinding;

public class ConnectedFragment extends ConnectionAwareFragment {

    private FragmentConnectedBinding binding;

    public ConnectedFragment() {
        // Required empty public constructor
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        binding = FragmentConnectedBinding.inflate(inflater, container, false);

        // Setup Status Cards
        setupStatusCards(binding.appBarAndStatus);

        // Listen for Disconnect button click
        binding.disconnectButton.setOnClickListener(view -> {
            mainActivity.getConnectionManager().disconnect();
        });

        return  binding.getRoot();
    }
}