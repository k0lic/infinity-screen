package ka170130.pmu.infinityscreen.media;

import android.Manifest;
import android.content.ContentResolver;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.MediaMetadataRetriever;
import android.media.ThumbnailUtils;
import android.net.Uri;
import android.os.Build;
import android.provider.BaseColumns;
import android.provider.MediaStore;
import android.util.Log;
import android.util.Size;
import android.webkit.MimeTypeMap;

import java.io.File;
import java.io.IOException;

import ka170130.pmu.infinityscreen.MainActivity;
import ka170130.pmu.infinityscreen.helpers.Callback;
import ka170130.pmu.infinityscreen.helpers.PermissionsHelper;

public class MediaManager {

    private MainActivity mainActivity;

    public MediaManager(MainActivity mainActivity) {
        this.mainActivity = mainActivity;
    }

    public void getVideoThumbnail(Uri uri, Size size, Callback<Bitmap> callback) {
        String[] permissions = { Manifest.permission.READ_EXTERNAL_STORAGE };
        PermissionsHelper.request(permissions, s -> {
            Bitmap bitmap = null;
            ContentResolver contentResolver = mainActivity.getContentResolver();

            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    bitmap = contentResolver.loadThumbnail(uri, size, null);
                } else {
                    MediaMetadataRetriever mmr = new MediaMetadataRetriever();
                    mmr.setDataSource(mainActivity, uri);
                    bitmap = mmr.getFrameAtTime();
                }
            } catch (IOException e) {
                Log.d(MainActivity.LOG_TAG, e.toString());
                e.printStackTrace();
            }

            callback.invoke(bitmap);
        });
    }

    public boolean isVideo(String mimeType) {
        return mimeType != null && mimeType.startsWith("video");
    }

    public boolean isImage(String mimeType) {
        return mimeType != null && mimeType.startsWith("image");
    }

    public String getMimeType(Uri uri) {
        String mimeType = null;
        if (ContentResolver.SCHEME_CONTENT.equals(uri.getScheme())) {
            ContentResolver cr = mainActivity.getApplicationContext().getContentResolver();
            mimeType = cr.getType(uri);
        } else {
            String fileExtension = MimeTypeMap.getFileExtensionFromUrl(uri
                    .toString());
            mimeType = MimeTypeMap.getSingleton().getMimeTypeFromExtension(
                    fileExtension.toLowerCase());
        }
        return mimeType;
    }
}
