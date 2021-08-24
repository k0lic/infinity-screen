package ka170130.pmu.infinityscreen.media;

import android.Manifest;
import android.content.ContentResolver;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.media.MediaMetadataRetriever;
import android.media.ThumbnailUtils;
import android.net.Uri;
import android.os.Build;
import android.provider.BaseColumns;
import android.provider.MediaStore;
import android.provider.OpenableColumns;
import android.util.Log;
import android.util.Size;
import android.webkit.MimeTypeMap;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;

import ka170130.pmu.infinityscreen.MainActivity;
import ka170130.pmu.infinityscreen.containers.FileContentPackage;
import ka170130.pmu.infinityscreen.containers.FileInfo;
import ka170130.pmu.infinityscreen.containers.Message;
import ka170130.pmu.infinityscreen.containers.PlaybackStatusCommand;
import ka170130.pmu.infinityscreen.helpers.Callback;
import ka170130.pmu.infinityscreen.helpers.LogHelper;
import ka170130.pmu.infinityscreen.helpers.PermissionsHelper;

public class MediaManager {

    private MainActivity mainActivity;

    public MediaManager(MainActivity mainActivity) {
        this.mainActivity = mainActivity;
    }

    public FileInfo fileInfoFromUri(Uri uri, int index) {
        String mimeType = getMimeType(uri);
        FileInfo.FileType fileType = null;
        if (isImage(mimeType)) {
            fileType = FileInfo.FileType.IMAGE;
        } else if (isVideo(mimeType)) {
            fileType = FileInfo.FileType.VIDEO;
        } else {
            fileType =  null;
        }

        long fileSize = 0;
        Cursor returnCursor = mainActivity.getContentResolver()
                .query(uri, null, null, null, null);
        int sizeIndex = returnCursor.getColumnIndex(OpenableColumns.SIZE);

        if (returnCursor.moveToFirst()) {
            fileSize = returnCursor.getLong(sizeIndex);
        }

        int contentWidth = 0;
        int contentHeight = 0;
        if (fileType == FileInfo.FileType.IMAGE) {
            try {
                BitmapFactory.Options options = new BitmapFactory.Options();
                options.inJustDecodeBounds = true;
                BitmapFactory.decodeStream(
                        mainActivity.getContentResolver().openInputStream(uri),
                        null,
                        options
                );
                contentWidth = options.outWidth;
                contentHeight = options.outHeight;
            } catch (FileNotFoundException exception) {
                LogHelper.error(exception);
                return null;
            }
        } else if (fileType == FileInfo.FileType.VIDEO) {
            Size videoDimens = getVideoDimensions(uri);
            contentWidth = videoDimens.getWidth();
            contentHeight = videoDimens.getHeight();
        }

        LogHelper.log("FileInfo (from Uri): ["
                + "mime type: " + mimeType
                + ", type: " + (fileType == null ? "<NULL>" : fileType.toString())
                + ", width: " + contentWidth
                + ", height: " + contentHeight
                + "]"
        );

        FileInfo.PlaybackStatus initialStatus = FileInfo.PlaybackStatus.WAIT;
        if (fileType == FileInfo.FileType.VIDEO) {
            initialStatus = FileInfo.PlaybackStatus.PAUSE;
        }

        return new FileInfo(
                index,
                mimeType,
                fileType,
                fileSize,
                contentWidth,
                contentHeight,
                FileContentPackage.INIT_PACKAGE_ID,
                initialStatus,
                null
        );
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
                LogHelper.error(e);
            }

            callback.invoke(bitmap);
        });
    }

    public Size getVideoDimensions(Uri uri) {
        // get first frame
        MediaMetadataRetriever mmr = new MediaMetadataRetriever();
        mmr.setDataSource(mainActivity, uri);
        Bitmap bitmap = mmr.getFrameAtTime();

        // get size of first frame
        Size size = new Size(bitmap.getWidth(), bitmap.getHeight());
        return size;
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

    public void requestPlay(int fileIndex) throws IOException {
        // TODO: optimize so playback is synchronous, maybe add check(s)
        // Authorize Play
        Message message = Message.newPlaybackStatusCommandMessage(new PlaybackStatusCommand(
                fileIndex,
                FileInfo.PlaybackStatus.PLAY
        ));
        mainActivity.getTaskManager().sendToAllInGroup(message, true);
    }

    public void requestPause(int fileIndex) throws IOException {
        // TODO: optimize so playback is synchronous, maybe add check(s)
        // Authorize Pause
        Message message = Message.newPlaybackStatusCommandMessage(new PlaybackStatusCommand(
                fileIndex,
                FileInfo.PlaybackStatus.PAUSE
        ));
        mainActivity.getTaskManager().sendToAllInGroup(message, true);
    }
}
