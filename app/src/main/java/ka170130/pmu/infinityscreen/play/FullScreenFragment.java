package ka170130.pmu.infinityscreen.play;

import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.view.Window;
import android.view.WindowInsets;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;

import ka170130.pmu.infinityscreen.MainActivity;
import ka170130.pmu.infinityscreen.connection.ConnectionAwareFragment;

public abstract class FullScreenFragment extends ConnectionAwareFragment {

    private boolean active = false;

    @Override
    public void onResume() {
        super.onResume();

        active = true;
        // Acquire Full Screen
        acquireFullScreen();
    }

    @Override
    public void onStop() {
        super.onStop();

        active = false;
        // Exit Full Screen
        exitFullScreen();
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
}
