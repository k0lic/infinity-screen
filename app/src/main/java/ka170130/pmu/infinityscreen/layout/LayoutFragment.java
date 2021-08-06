package ka170130.pmu.infinityscreen.layout;

import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import ka170130.pmu.infinityscreen.MainActivity;
import ka170130.pmu.infinityscreen.R;
import ka170130.pmu.infinityscreen.connection.ConnectionAwareFragment;
import ka170130.pmu.infinityscreen.databinding.FragmentHomeBinding;
import ka170130.pmu.infinityscreen.databinding.FragmentLayoutBinding;

public class LayoutFragment extends ConnectionAwareFragment {

    private FragmentLayoutBinding binding;

    public LayoutFragment() {
        // Required empty public constructor
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        binding = FragmentLayoutBinding.inflate(inflater, container, false);

        // Setup Status Cards
        setupStatusCards(binding.appBarAndStatus);

        // TODO: everything
        binding.previewButton.setOnClickListener(view -> {
            navController.navigate(LayoutFragmentDirections.actionPreviewFragment());
        });

        binding.continueButton.setOnClickListener(view -> {
            navController.navigate(LayoutFragmentDirections.actionFileSelectionFragment());
        });

        return  binding.getRoot();
    }
}