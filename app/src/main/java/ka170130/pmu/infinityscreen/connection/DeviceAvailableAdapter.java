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
import ka170130.pmu.infinityscreen.containers.PeerInfo;
import ka170130.pmu.infinityscreen.databinding.ViewHolderDeviceAvailableBinding;
import ka170130.pmu.infinityscreen.helpers.AppBarAndStatusHelper;
import ka170130.pmu.infinityscreen.helpers.Callback;

public class DeviceAvailableAdapter extends RecyclerView.Adapter<DeviceAvailableAdapter.DeviceAvailableViewHolder> {

    private MainActivity mainActivity;
    private Callback<PeerInfo> callback;

    private List<PeerInfo> devices = new ArrayList<>();

    public DeviceAvailableAdapter(
            MainActivity mainActivity,
            Callback<PeerInfo> callback
    ) {
        this.mainActivity = mainActivity;
        this.callback = callback;
    }

    @NonNull
    @Override
    public DeviceAvailableViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater layoutInflater = LayoutInflater.from(parent.getContext());
        ViewHolderDeviceAvailableBinding binding = ViewHolderDeviceAvailableBinding.inflate(
                layoutInflater,
                parent,
                false
        );
        return new DeviceAvailableViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull DeviceAvailableViewHolder holder, int position) {
        holder.bind(devices.get(position));
    }

    @Override
    public int getItemCount() {
        return devices.size();
    }

    public void setDevices(List<PeerInfo> devices) {
        this.devices = devices;
        notifyDataSetChanged();
    }

    public class DeviceAvailableViewHolder extends RecyclerView.ViewHolder {

        private ViewHolderDeviceAvailableBinding binding;
        private PeerInfo device;

        public DeviceAvailableViewHolder(ViewHolderDeviceAvailableBinding binding) {
            super(binding.getRoot());
            this.binding = binding;

            binding.holderActionButton.setOnClickListener(view -> {
                callback.invoke(device);
            });
        }

        public void bind(PeerInfo device) {
            this.device = device;

            binding.holderDeviceName.setText(device.getDeviceName());
            binding.holderDeviceStatus.setText(AppBarAndStatusHelper.getStatusText(
                    device,
                    mainActivity.getResources()
            ));
        }
    }
}
