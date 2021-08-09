package ka170130.pmu.infinityscreen.helpers;

import android.content.res.Resources;
import android.graphics.drawable.GradientDrawable;
import android.net.wifi.p2p.WifiP2pDevice;
import android.util.TypedValue;
import android.view.View;

import androidx.appcompat.widget.Toolbar;

import ka170130.pmu.infinityscreen.R;
import ka170130.pmu.infinityscreen.containers.PeerInfo;
import ka170130.pmu.infinityscreen.databinding.AppBarAndStatusBinding;

public class AppBarAndStatusHelper {

    public static String getStatusText(
            PeerInfo device,
            Resources resources
    ) {
        switch (device.getStatus()) {
            case WifiP2pDevice.AVAILABLE:
                return resources.getString(R.string.device_status_available);
            case WifiP2pDevice.INVITED:
                return resources.getString(R.string.device_status_invited);
            case WifiP2pDevice.CONNECTED:
                return resources.getString(R.string.device_status_connected);
            case WifiP2pDevice.FAILED:
                return resources.getString(R.string.device_status_failed);
            case WifiP2pDevice.UNAVAILABLE:
                return resources.getString(R.string.device_status_unavailable);
            default:
                return resources.getString(R.string.device_status_unknown);
        }
    }

    public static void setupCardShapes(AppBarAndStatusBinding binding) {
        binding.topDeviceCard.setBackgroundResource(R.drawable.one_rounded_corner_bottom_right);
        binding.topHostCard.setBackgroundResource(R.drawable.one_rounded_corner_bottom_left);
    }

    public static void refreshDeviceCard(
            AppBarAndStatusBinding binding,
            PeerInfo device,
            Resources resources,
            Resources.Theme theme
    ) {
        setDeviceCardContent(binding, device, resources);

        if (device.getStatus() == WifiP2pDevice.CONNECTED) {
            setDeviceCardStyleConnected(binding, theme);
        } else {
            setDeviceCardStyleAvailable(binding, theme);
        }
    }

    public static void inflateMenu(
            AppBarAndStatusBinding binding,
            int menuId,
            Toolbar.OnMenuItemClickListener listener
    ) {
        binding.topAppBar.inflateMenu(menuId);
        binding.topAppBar.setOnMenuItemClickListener(listener);
    }

    public static void setDeviceCardContent(
            AppBarAndStatusBinding binding,
            PeerInfo device,
            Resources resources
    ) {
        binding.topDeviceName.setText(device.getDeviceName());
        binding.topDeviceStatus.setText(getStatusText(device, resources));
    }

    public static void setHostCardContent(
            AppBarAndStatusBinding binding,
            PeerInfo device
    ) {
        binding.topHostName.setText(device.getDeviceName());
    }

    public static void hideHostDeviceName(
            AppBarAndStatusBinding binding
    ) {
        binding.topHostName.setVisibility(View.GONE);
    }

    public static void showHostDeviceName(
            AppBarAndStatusBinding binding
    ) {
        binding.topHostName.setVisibility(View.VISIBLE);
    }

    public static void hideHostCard(AppBarAndStatusBinding binding) {
        binding.topHostCard.setVisibility(View.GONE);
    }

    public static void showHostCard(AppBarAndStatusBinding binding, Resources.Theme theme) {
        int bgColor = resolveRefColor(theme, R.attr.colorNeutral);
        ((GradientDrawable) binding.topHostCard.getBackground()).setColor(bgColor);
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

        ((GradientDrawable) binding.topDeviceCard.getBackground()).setColor(bgColor);
        binding.topDeviceName.setTextColor(textColor);
        binding.topDeviceStatus.setTextColor(textColor);
    }

    public static int resolveRefColor(
            Resources.Theme theme,
            int resId
    ) {
        TypedValue typedValue = new TypedValue();
        theme.resolveAttribute(resId, typedValue, true);
        return typedValue.data;
    }
}
