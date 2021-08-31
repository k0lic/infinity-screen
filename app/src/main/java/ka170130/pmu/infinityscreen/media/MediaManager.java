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

import androidx.lifecycle.ViewModelProvider;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.InetAddress;
import java.util.Iterator;

import ka170130.pmu.infinityscreen.MainActivity;
import ka170130.pmu.infinityscreen.containers.FileContentPackage;
import ka170130.pmu.infinityscreen.containers.FileInfo;
import ka170130.pmu.infinityscreen.containers.Message;
import ka170130.pmu.infinityscreen.containers.PeerInetAddressInfo;
import ka170130.pmu.infinityscreen.containers.PlaybackStatusCommand;
import ka170130.pmu.infinityscreen.helpers.Callback;
import ka170130.pmu.infinityscreen.helpers.LogHelper;
import ka170130.pmu.infinityscreen.helpers.PermissionsHelper;
import ka170130.pmu.infinityscreen.sync.SyncInfo;
import ka170130.pmu.infinityscreen.viewmodels.ConnectionViewModel;
import ka170130.pmu.infinityscreen.viewmodels.SyncViewModel;

public class MediaManager {

    private static final long[] DEFERRED_DELAYS = {
            1000
    };

    private static final int DEFAULT_THUMBNAIL_SIZE = 200;

    private MainActivity mainActivity;
    private ConnectionViewModel connectionViewModel;
    private SyncViewModel syncViewModel;

    private int deferredPicker;

    public MediaManager(MainActivity mainActivity) {
        this.mainActivity = mainActivity;
        this.connectionViewModel =
                new ViewModelProvider(mainActivity).get(ConnectionViewModel.class);
        this.syncViewModel =
                new ViewModelProvider(mainActivity).get(SyncViewModel.class);

        this.deferredPicker = 0;
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
                    LogHelper.log("Video Thumbnail Size: " + size.getWidth() + " " + size.getHeight());

                    // Handle (0,0) size
                    int width = size.getWidth();
                    int height = size.getHeight();
                    if (width == 0) {
                        width = DEFAULT_THUMBNAIL_SIZE;
                    }
                    if (height == 0) {
                        height =DEFAULT_THUMBNAIL_SIZE;
                    }
                    Size nonZeroSize = new Size(width, height);

                    bitmap = contentResolver.loadThumbnail(uri, nonZeroSize, null);
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
        // Authorize Deferred Play
        long now = System.currentTimeMillis();
        long ownTimestamp = now + getDeferredDelay();
        PlaybackStatusCommand command = new PlaybackStatusCommand(
                fileIndex,
                FileInfo.PlaybackStatus.DEFERRED_PLAY
        );

        // Send to all - but change timestamp for everyone
        Iterator<PeerInetAddressInfo> iterator =
                connectionViewModel.getGroupList().getValue().iterator();
        while (iterator.hasNext()) {
            PeerInetAddressInfo next = iterator.next();

            // Adjust timestamp according to Sync information
            SyncInfo syncInfo = syncViewModel.getSyncInfoListElement(next.getDeviceName());
            command.setTimestamp(ownTimestamp + syncInfo.getClockDiff());

            // Send PLAYBACK_STATUS_COMMAND message to peer
            Message message = Message.newPlaybackStatusCommandMessage(command);
            mainActivity.getTaskManager().runSenderTask(next.getInetAddress(), message);
        }

        // Adjust timestamp for self
        command.setTimestamp(ownTimestamp);

        // Send PLAYBACK_STATUS_COMMAND message to self
        InetAddress hostAddress = connectionViewModel.getHostDevice().getValue().getInetAddress();
        Message message = Message.newPlaybackStatusCommandMessage(command);
        mainActivity.getTaskManager().runSenderTask(hostAddress, message);
    }

    public void requestPause(int fileIndex) throws IOException {
        // Authorize Pause
        Message message = Message.newPlaybackStatusCommandMessage(new PlaybackStatusCommand(
                fileIndex,
                FileInfo.PlaybackStatus.PAUSE
        ));
        mainActivity.getTaskManager().sendToAllInGroup(message, true);
    }

    private long getDeferredDelay() {
        return DEFERRED_DELAYS[deferredPicker];
    }

    private void increaseDeferredDelay() {
        if (deferredPicker + 1 < DEFERRED_DELAYS.length) {
            deferredPicker++;
        }
    }

    private void decreaseDeferredDelay() {
        if (deferredPicker - 1 >= 0) {
            deferredPicker--;
        }
    }
}
