package ka170130.pmu.infinityscreen.layout;

import android.content.res.AssetFileDescriptor;
import android.graphics.Matrix;
import android.graphics.Rect;
import android.graphics.SurfaceTexture;
import android.graphics.drawable.Drawable;
import android.media.MediaPlayer;
import android.os.Build;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.Surface;
import android.view.TextureView;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;

import java.io.IOException;
import java.io.InputStream;

import ka170130.pmu.infinityscreen.MainActivity;
import ka170130.pmu.infinityscreen.R;
import ka170130.pmu.infinityscreen.containers.TransformInfo;
import ka170130.pmu.infinityscreen.databinding.FragmentHomeBinding;
import ka170130.pmu.infinityscreen.databinding.FragmentPreviewBinding;
import ka170130.pmu.infinityscreen.helpers.StateChangeHelper;
import ka170130.pmu.infinityscreen.play.FullScreenFragment;
import ka170130.pmu.infinityscreen.viewmodels.LayoutViewModel;
import ka170130.pmu.infinityscreen.viewmodels.StateViewModel;

public class PreviewFragment extends FullScreenFragment {

    private FragmentPreviewBinding binding;
    private StateViewModel stateViewModel;
    private LayoutViewModel layoutViewModel;

    private LayoutManager layoutManager;

    public PreviewFragment() {
        // Required empty public constructor
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        stateViewModel = new ViewModelProvider(mainActivity).get(StateViewModel.class);
        layoutViewModel = new ViewModelProvider(mainActivity).get(LayoutViewModel.class);

        layoutManager = mainActivity.getLayoutManager();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        binding = FragmentPreviewBinding.inflate(inflater, container, false);

        try (InputStream inputStream =
                        mainActivity.getAssets().open("DonutsForBreakfast4.png")) {
            // set content
            Drawable drawable = Drawable.createFromStream(inputStream, null);
            binding.imageView.setImageDrawable(drawable);

            // set matrix
            TransformInfo self = layoutViewModel.getSelfAuto().getValue();
            TransformInfo viewport = layoutViewModel.getViewport().getValue();

            int drawableWidth = drawable.getIntrinsicWidth();
            int drawableHeight = drawable.getIntrinsicHeight();

            Matrix matrix = layoutManager.getMatrix(self, viewport, drawableWidth, drawableHeight);
            binding.imageView.setImageMatrix(matrix);
        } catch (Exception e) {
            Log.d(MainActivity.LOG_TAG, e.toString());
            e.printStackTrace();
        }

        binding.backButton.setOnClickListener(view -> {
            Rect rect = binding.imageView.getDrawable().copyBounds();
            Log.d(MainActivity.LOG_TAG, "RECT: " + rect.left + " " + rect.right + " " + rect.top + " " + rect.bottom);

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