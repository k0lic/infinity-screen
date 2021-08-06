package ka170130.pmu.infinityscreen.media;

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
import ka170130.pmu.infinityscreen.databinding.FragmentFileSelectionBinding;
import ka170130.pmu.infinityscreen.databinding.FragmentFileSelectionWaitBinding;

public class FileSelectionWaitFragment extends ConnectionAwareFragment {

    private FragmentFileSelectionWaitBinding binding;

    public FileSelectionWaitFragment() {
        // Required empty public constructor
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        binding = FragmentFileSelectionWaitBinding.inflate(inflater, container, false);

        // Setup Status Cards
        setupStatusCards(binding.appBarAndStatus);

        // TODO

        return  binding.getRoot();
    }
}