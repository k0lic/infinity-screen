package ka170130.pmu.infinityscreen.connection;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

import ka170130.pmu.infinityscreen.MainActivity;
import ka170130.pmu.infinityscreen.R;
import ka170130.pmu.infinityscreen.containers.PeerInetAddressInfo;
import ka170130.pmu.infinityscreen.containers.PeerInfo;
import ka170130.pmu.infinityscreen.databinding.ViewHolderDeviceConnectedBinding;
import ka170130.pmu.infinityscreen.helpers.AppBarAndStatusHelper;
import ka170130.pmu.infinityscreen.helpers.Callback;

public class DeviceConnectedAdapter extends RecyclerView.Adapter<DeviceConnectedAdapter.DeviceConnectedViewHolder> {

    private MainActivity mainActivity;
    private Callback<PeerInetAddressInfo> callback;

    private List<PeerInetAddressInfo> devices = new ArrayList<>();

    public DeviceConnectedAdapter(
            MainActivity mainActivity,
            Callback<PeerInetAddressInfo> callback
    ) {
        this.mainActivity = mainActivity;
        this.callback = callback;
    }

    @NonNull
    @Override
    public DeviceConnectedViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater layoutInflater = LayoutInflater.from(parent.getContext());
        ViewHolderDeviceConnectedBinding binding = ViewHolderDeviceConnectedBinding.inflate(
                layoutInflater,
                parent,
                false
        );
        return new DeviceConnectedViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull DeviceConnectedViewHolder holder, int position) {
        holder.bind(devices.get(position));
    }

    @Override
    public int getItemCount() {
        return devices.size();
    }

    public void setDevices(List<PeerInetAddressInfo> devices) {
        this.devices = devices;
        notifyDataSetChanged();
    }

    public class DeviceConnectedViewHolder extends RecyclerView.ViewHolder {

        private ViewHolderDeviceConnectedBinding binding;
        private PeerInetAddressInfo device;

        public DeviceConnectedViewHolder(ViewHolderDeviceConnectedBinding binding) {
            super(binding.getRoot());
            this.binding = binding;

            binding.holderActionButton.setOnClickListener(view -> {
                callback.invoke(device);
            });
        }

        public void bind(PeerInetAddressInfo device) {
            this.device = device;

            binding.holderDeviceName.setText(device.getDeviceName());
            binding.holderDeviceStatus.setText(AppBarAndStatusHelper.getStatusText(
                    device,
                    mainActivity.getResources()
            ));
        }
    }
}
