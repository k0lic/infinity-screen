package ka170130.pmu.infinityscreen.connection;

import android.net.wifi.p2p.WifiP2pDevice;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

import ka170130.pmu.infinityscreen.MainActivity;
import ka170130.pmu.infinityscreen.databinding.ViewHolderDeviceBinding;
import ka170130.pmu.infinityscreen.helpers.AppBarAndStatusHelper;
import ka170130.pmu.infinityscreen.helpers.Callback;

public class DeviceAdapter extends RecyclerView.Adapter<DeviceAdapter.DeviceViewHolder> {

    private MainActivity mainActivity;
    private boolean hideActionButton;
    private Callback<WifiP2pDevice> callback;

    private List<WifiP2pDevice> devices = new ArrayList<>();

    public DeviceAdapter(
            MainActivity mainActivity,
            boolean hideActionButton,
            Callback<WifiP2pDevice> callback
    ) {
        this.mainActivity = mainActivity;
        this.hideActionButton = hideActionButton;
        this.callback = callback;
    }

    @NonNull
    @Override
    public DeviceViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater layoutInflater = LayoutInflater.from(parent.getContext());
        ViewHolderDeviceBinding binding = ViewHolderDeviceBinding.inflate(
                layoutInflater,
                parent,
                false
        );
        return new DeviceViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull DeviceViewHolder holder, int position) {
        holder.bind(devices.get(position));
    }

    @Override
    public int getItemCount() {
        return devices.size();
    }

    public void setDevices(List<WifiP2pDevice> devices) {
        this.devices = devices;
        notifyDataSetChanged();
    }

    public class DeviceViewHolder extends RecyclerView.ViewHolder {

        private ViewHolderDeviceBinding binding;
        private WifiP2pDevice device;

        public DeviceViewHolder(ViewHolderDeviceBinding binding) {
            super(binding.getRoot());
            this.binding = binding;

            if (hideActionButton) {
                binding.actionButton.setVisibility(View.GONE);
            } else {
                binding.actionButton.setOnClickListener(view -> {
                    callback.invoke(device);
                });
            }
        }

        public void bind(WifiP2pDevice device) {
            this.device = device;

            binding.deviceName.setText(device.deviceName);
            binding.deviceStatus.setText(AppBarAndStatusHelper.getStatusText(
                    device,
                    DeviceAdapter.this.mainActivity.getResources()
            ));
        }
    }
}
