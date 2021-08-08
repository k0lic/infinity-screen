package ka170130.pmu.infinityscreen.layout;

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
import android.view.WindowManager;

import ka170130.pmu.infinityscreen.MainActivity;
import ka170130.pmu.infinityscreen.R;
import ka170130.pmu.infinityscreen.databinding.FragmentHomeBinding;
import ka170130.pmu.infinityscreen.databinding.FragmentPreviewBinding;
import ka170130.pmu.infinityscreen.helpers.StateChangeHelper;
import ka170130.pmu.infinityscreen.play.FullScreenFragment;
import ka170130.pmu.infinityscreen.viewmodels.StateViewModel;

public class PreviewFragment extends FullScreenFragment {

    private FragmentPreviewBinding binding;
    private StateViewModel stateViewModel;

    public PreviewFragment() {
        // Required empty public constructor
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        stateViewModel = new ViewModelProvider(mainActivity).get(StateViewModel.class);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        binding = FragmentPreviewBinding.inflate(inflater, container, false);

        // TODO: everything
        binding.backButton.setOnClickListener(view -> {
            StateChangeHelper.requestStateChange(
                    mainActivity, connectionViewModel, StateViewModel.AppState.LAYOUT);
//            navController.navigateUp();
        });

        // Listen for App State change
        stateViewModel.getState().observe(getViewLifecycleOwner(), state -> {
            if (state == StateViewModel.AppState.LAYOUT) {
                navController.navigate(PreviewFragmentDirections.actionPop());
            }
        });

        return  binding.getRoot();
    }

    @Override
    public void onResume() {
        super.onResume();

        // sync App State - necessary for the Back button to work
//        StateChangeHelper.requestStateChange(
//                mainActivity, connectionViewModel, StateViewModel.AppState.PREVIEW);
    }
}