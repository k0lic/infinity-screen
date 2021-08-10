package ka170130.pmu.infinityscreen.media;

import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.util.Size;
import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

import ka170130.pmu.infinityscreen.MainActivity;
import ka170130.pmu.infinityscreen.R;
import ka170130.pmu.infinityscreen.databinding.ViewHolderFileBinding;
import ka170130.pmu.infinityscreen.helpers.Callback;

public class MediaAdapter extends RecyclerView.Adapter<MediaAdapter.MediaViewHolder> {

    private MainActivity mainActivity;
    private Callback<Integer> callback;
    private MediaManager mediaManager;

    private List<String> stringList = new ArrayList<>();

    public MediaAdapter(MainActivity mainActivity, Callback<Integer> callback) {
        this.mainActivity = mainActivity;
        this.callback = callback;

        mediaManager = mainActivity.getMediaManager();
    }

    @NonNull
    @Override
    public MediaViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater layoutInflater = LayoutInflater.from(parent.getContext());
        ViewHolderFileBinding binding = ViewHolderFileBinding.inflate(
                layoutInflater,
                parent,
                false
        );
        return new MediaViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull MediaViewHolder holder, int position) {
        holder.bind(position);
    }

    @Override
    public int getItemCount() {
        return stringList.size();
    }

    public void setStringList(List<String> stringList) {
        this.stringList = stringList;
        notifyDataSetChanged();
    }

    public class MediaViewHolder extends RecyclerView.ViewHolder {

        private ViewHolderFileBinding binding;
        private Integer position;

        public MediaViewHolder(ViewHolderFileBinding binding) {
            super(binding.getRoot());
            this.binding = binding;

            binding.closeButton.setOnClickListener(view -> {
                if (position == null) {
                    return;
                }
                callback.invoke(position);
            });
        }

        public void bind(int position) {
            this.position = position;

            // TODO: default thumbnail
            Drawable drawable = mainActivity.getResources().getDrawable(R.mipmap.ic_launcher);
            binding.imageView.setImageDrawable(drawable);

            // get thumbnail
            Uri uri = Uri.parse(stringList.get(position));
            String mimeType = mediaManager.getMimeType(uri);

            if (mediaManager.isImage(mimeType)) {
                binding.imageView.setImageURI(uri);
            } else if (mediaManager.isVideo(mimeType)) {
                mediaManager.getVideoThumbnail(
                        uri,
                        new Size(binding.imageView.getWidth(), binding.imageView.getHeight()),
                        thumbnail -> {
                            if (thumbnail == null) {
                                return;
                            }

                            binding.imageView.setImageBitmap(thumbnail);
                        }
                );
            }
        }
    }
}
