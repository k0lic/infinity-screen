package ka170130.pmu.infinityscreen;

import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.provider.DocumentsContract;
import android.provider.MediaStore;

public class FilePathFromUri {

    /*
    This method can parse out the real local file path from a file URI.
    */
    public static String getUriRealPath(Context ctx, ContentResolver contentResolver, Uri uri)
    {
        String ret = "";
        if( isAboveKitKat() )
        {
            // Android sdk version number bigger than 19.
            ret = getUriRealPathAboveKitkat(ctx, contentResolver, uri);
        }else
        {
            // Android sdk version number smaller than 19.
            ret = getImageRealPath(contentResolver, uri, null);
        }
        return ret;
    }
    /*
    This method will parse out the real local file path from the file content URI.
    The method is only applied to android sdk version number that is bigger than 19.
    */
    private static String getUriRealPathAboveKitkat(Context ctx, ContentResolver contentResolver, Uri uri)
    {
        String ret = "";
        if(ctx != null && uri != null) {
            if(isContentUri(uri))
            {
                if(isGooglePhotoDoc(uri.getAuthority()))
                {
                    ret = uri.getLastPathSegment();
                }else {
                    ret = getImageRealPath(contentResolver, uri, null);
                }
            }else if(isFileUri(uri)) {
                ret = uri.getPath();
            }else if(isDocumentUri(ctx, uri)){
                // Get uri related document id.
                String documentId = DocumentsContract.getDocumentId(uri);
                // Get uri authority.
                String uriAuthority = uri.getAuthority();
                if(isMediaDoc(uriAuthority))
                {
                    String idArr[] = documentId.split(":");
                    if(idArr.length == 2)
                    {
                        // First item is document type.
                        String docType = idArr[0];
                        // Second item is document real id.
                        String realDocId = idArr[1];
                        // Get content uri by document type.
                        Uri mediaContentUri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI;
                        if("image".equals(docType))
                        {
                            mediaContentUri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI;
                        }else if("video".equals(docType))
                        {
                            mediaContentUri = MediaStore.Video.Media.EXTERNAL_CONTENT_URI;
                        }else if("audio".equals(docType))
                        {
                            mediaContentUri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI;
                        }
                        // Get where clause with real document id.
                        String whereClause = MediaStore.Images.Media._ID + " = " + realDocId;
                        ret = getImageRealPath(contentResolver, mediaContentUri, whereClause);
                    }
                }else if(isDownloadDoc(uriAuthority))
                {
                    // Build download uri.
                    Uri downloadUri = Uri.parse("content://downloads/public_downloads");
                    // Append download document id at uri end.
                    Uri downloadUriAppendId = ContentUris.withAppendedId(downloadUri, Long.valueOf(documentId));
                    ret = getImageRealPath(contentResolver, downloadUriAppendId, null);
                }else if(isExternalStoreDoc(uriAuthority))
                {
                    String idArr[] = documentId.split(":");
                    if(idArr.length == 2)
                    {
                        String type = idArr[0];
                        String realDocId = idArr[1];
                        if("primary".equalsIgnoreCase(type))
                        {
                            ret = Environment.getExternalStorageDirectory() + "/" + realDocId;
                        }
                    }
                }
            }
        }
        return ret;
    }
    /* Check whether current android os version is bigger than kitkat or not. */
    private static boolean isAboveKitKat()
    {
        boolean ret = false;
        ret = Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT;
        return ret;
    }
    /* Check whether this uri represent a document or not. */
    private static boolean isDocumentUri(Context ctx, Uri uri)
    {
        boolean ret = false;
        if(ctx != null && uri != null) {
            ret = DocumentsContract.isDocumentUri(ctx, uri);
        }
        return ret;
    }
    /* Check whether this uri is a content uri or not.
     *  content uri like content://media/external/images/media/1302716
     *  */
    private static boolean isContentUri(Uri uri)
    {
        boolean ret = false;
        if(uri != null) {
            String uriSchema = uri.getScheme();
            if("content".equalsIgnoreCase(uriSchema))
            {
                ret = true;
            }
        }
        return ret;
    }
    /* Check whether this uri is a file uri or not.
     *  file uri like file:///storage/41B7-12F1/DCIM/Camera/IMG_20180211_095139.jpg
     * */
    private static boolean isFileUri(Uri uri)
    {
        boolean ret = false;
        if(uri != null) {
            String uriSchema = uri.getScheme();
            if("file".equalsIgnoreCase(uriSchema))
            {
                ret = true;
            }
        }
        return ret;
    }
    /* Check whether this document is provided by ExternalStorageProvider. Return true means the file is saved in external storage. */
    private static boolean isExternalStoreDoc(String uriAuthority)
    {
        boolean ret = false;
        if("com.android.externalstorage.documents".equals(uriAuthority))
        {
            ret = true;
        }
        return ret;
    }
    /* Check whether this document is provided by DownloadsProvider. return true means this file is a downloaed file. */
    private static boolean isDownloadDoc(String uriAuthority)
    {
        boolean ret = false;
        if("com.android.providers.downloads.documents".equals(uriAuthority))
        {
            ret = true;
        }
        return ret;
    }
    /*
    Check if MediaProvider provide this document, if true means this image is created in android media app.
    */
    private static boolean isMediaDoc(String uriAuthority)
    {
        boolean ret = false;
        if("com.android.providers.media.documents".equals(uriAuthority))
        {
            ret = true;
        }
        return ret;
    }
    /*
    Check whether google photos provide this document, if true means this image is created in google photos app.
    */
    private static boolean isGooglePhotoDoc(String uriAuthority)
    {
        boolean ret = false;
        if("com.google.android.apps.photos.content".equals(uriAuthority))
        {
            ret = true;
        }
        return ret;
    }
    /* Return uri represented document file real local path.*/
    private static String getImageRealPath(ContentResolver contentResolver, Uri uri, String whereClause)
    {
        String ret = "";
        // Query the uri with condition.
        Cursor cursor = contentResolver.query(uri, null, whereClause, null, null);
        if(cursor!=null)
        {
            boolean moveToFirst = cursor.moveToFirst();
            if(moveToFirst)
            {
                // Get columns name by uri type.
                String columnName = MediaStore.Images.Media.DATA;
                if( uri==MediaStore.Images.Media.EXTERNAL_CONTENT_URI )
                {
                    columnName = MediaStore.Images.Media.DATA;
                }else if( uri==MediaStore.Audio.Media.EXTERNAL_CONTENT_URI )
                {
                    columnName = MediaStore.Audio.Media.DATA;
                }else if( uri==MediaStore.Video.Media.EXTERNAL_CONTENT_URI )
                {
                    columnName = MediaStore.Video.Media.DATA;
                }
                // Get column index.
                int imageColumnIndex = cursor.getColumnIndex(columnName);
                // Get column value which is the uri related file local path.
                ret = cursor.getString(imageColumnIndex);
            }
        }
        return ret;
    }
}
