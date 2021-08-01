package ka170130.pmu.infinityscreen;

import android.content.res.Resources;
import android.util.TypedValue;
import android.view.View;

import ka170130.pmu.infinityscreen.databinding.AppBarAndStatusBinding;

public class AppBarAndStatusHelper {

    public static void hideHostCard(AppBarAndStatusBinding binding) {
        binding.hostCard.setVisibility(View.GONE);
    }

    public static void setDeviceCardStyleAvailable(
            AppBarAndStatusBinding binding,
            Resources.Theme theme
    ) {
        setDeviceCardStyle(
                binding,
                theme,
                R.attr.colorDeviceAvailable,
                R.attr.colorOnDeviceAvailable);
    }

    public static void setDeviceCardStyleConnected(
            AppBarAndStatusBinding binding,
            Resources.Theme theme
    ) {
        setDeviceCardStyle(
                binding,
                theme,
                R.attr.colorDeviceConnected,
                R.attr.colorOnDeviceConnected);
    }

    public static void setDeviceCardStyle(
            AppBarAndStatusBinding binding,
            Resources.Theme theme,
            int bgResId,
            int textResId
    ) {
        int bgColor = resolveRefColor(theme, bgResId);
        int textColor = resolveRefColor(theme, textResId);

        binding.deviceCard.setBackgroundColor(bgColor);
        binding.deviceName.setTextColor(textColor);
        binding.deviceStatus.setTextColor(textColor);
    }

    private static int resolveRefColor(Resources.Theme theme, int resId) {
        TypedValue typedValue = new TypedValue();
        theme.resolveAttribute(resId, typedValue, true);
        return typedValue.data;
    }
}
