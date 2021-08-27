package ka170130.pmu.infinityscreen.play;

import android.content.pm.ActivityInfo;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.view.Window;
import android.view.WindowInsets;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;

import ka170130.pmu.infinityscreen.MainActivity;
import ka170130.pmu.infinityscreen.connection.ConnectionAwareFragment;
import ka170130.pmu.infinityscreen.containers.TransformInfo;
import ka170130.pmu.infinityscreen.viewmodels.LayoutViewModel;

public abstract class FullScreenFragment extends ConnectionAwareFragment {

    private LayoutViewModel layoutViewModel;

    private boolean active = false;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        layoutViewModel = new ViewModelProvider(mainActivity).get(LayoutViewModel.class);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        layoutViewModel.getSelfAuto().observe(getViewLifecycleOwner(), self -> {
            if (!active || self == null) {
                // skip
                return;
            }

            mainActivity.setRequestedOrientation(getOrientation(self));
        });
    }

    @Override
    public void onResume() {
        super.onResume();

        active = true;

        // Acquire Full Screen
        acquireFullScreen();

        // Force orientation
        TransformInfo transformInfo = layoutViewModel.getSelfAuto().getValue();
        if (transformInfo != null) {
            mainActivity.setRequestedOrientation(getOrientation(transformInfo));
        }
    }

    @Override
    public void onStop() {
        super.onStop();

        active = false;

        // Exit Full Screen
        exitFullScreen();

        // Do not force orientation
        mainActivity.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED);
    }

    protected void acquireFullScreen() {
        // If Fragment is not active, do nothing
        if (!active) {
            return;
        }

        Window window = mainActivity.getWindow();

        if (Build.VERSION.SDK_INT >= 30) {
            window.getInsetsController().hide(
                    WindowInsets.Type.statusBars() | WindowInsets.Type.navigationBars());
        } else {
            window.getDecorView().setSystemUiVisibility(
                    View.SYSTEM_UI_FLAG_IMMERSIVE
                    | View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                    | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                    | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                    | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                    | View.SYSTEM_UI_FLAG_FULLSCREEN);
        }
    }

    protected void exitFullScreen() {
        Window window = mainActivity.getWindow();

        if (Build.VERSION.SDK_INT >= 30) {
            window.getInsetsController().show(
                    WindowInsets.Type.statusBars() | WindowInsets.Type.navigationBars());
        } else {
            window.getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_STABLE);
        }
    }

    private int getOrientation(TransformInfo transformInfo) {
        switch (transformInfo.getOrientation()) {
            case PORTRAIT:
                return ActivityInfo.SCREEN_ORIENTATION_PORTRAIT;
            case LANDSCAPE:
                return ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE;
        }

        return ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED;
    }
}
