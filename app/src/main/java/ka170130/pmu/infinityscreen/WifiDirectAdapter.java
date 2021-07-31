package ka170130.pmu.infinityscreen;

import android.net.wifi.p2p.WifiP2pDevice;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.Collection;
import java.util.List;

import ka170130.pmu.infinityscreen.databinding.WifiDirectViewHolderLayoutBinding;

public class WifiDirectAdapter extends RecyclerView.Adapter<WifiDirectAdapter.WifiDirectViewHolder> {

    public interface Callback<T> {
        void invoke(T arg);
    }

    private List<WifiP2pDevice> devices;
    private Callback<WifiP2pDevice> deviceCallback;

    public WifiDirectAdapter(Callback<WifiP2pDevice> deviceCallback) {
        this.deviceCallback = deviceCallback;
    }

    @NonNull
    @Override
    public WifiDirectViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater layoutInflater = LayoutInflater.from(parent.getContext());
        WifiDirectViewHolderLayoutBinding binding = WifiDirectViewHolderLayoutBinding.inflate(
                layoutInflater,
                parent,
                false);
        return new WifiDirectViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull WifiDirectViewHolder holder, int position) {
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

    public class WifiDirectViewHolder extends RecyclerView.ViewHolder {

        private WifiDirectViewHolderLayoutBinding binding;
        private  WifiP2pDevice device;

        public WifiDirectViewHolder(@NonNull WifiDirectViewHolderLayoutBinding binding) {
            super(binding.getRoot());
            this.binding = binding;

            binding.getRoot().setOnClickListener(view -> {
                deviceCallback.invoke(device);
            });
        }

        public void bind(WifiP2pDevice device) {
            this.device = device;
            binding.name.setText(device.deviceName);
        }
    }
}
