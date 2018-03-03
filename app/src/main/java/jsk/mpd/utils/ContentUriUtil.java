package jsk.mpd.utils;

import android.content.ContentUris;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.os.storage.StorageManager;
import android.provider.DocumentsContract;
import android.provider.MediaStore;
import android.util.Log;

import java.lang.reflect.Method;

import static android.content.Context.STORAGE_SERVICE;

/**
 * Created by ALi on 2017/12/26.
 */

public class ContentUriUtil {

    private static final String TAG = "ContentUriUtil";
    private static final String mntPre = "/storage/";
    private static final String uriPre = "content://com.android.externalstorage.documents/document/";
    private static final String[] mediaTypeAudio = {
            ".aac", ".ac3", ".ape", ".asf", ".flac", ".m4a", ".mka", ".mp2",
            ".mp3", ".ogg", ".pcm", ".rm", ".wav", ".wma", ".eac3"
    };

    private static final String[] mediaTypeVideo = {
             ".mp4", ".3gp",
            ".amv", ".asx", ".avi", ".dat", ".evo", ".f4v", ".flv", ".m4v",
            ".mkv", ".mov", ".mpeg", ".mpg", ".mts", ".iso", ".rmvb", ".swf",
            ".ts", ".tp", ".trp", ".vob", ".webm", ".wmv", "m2ts"
    };

    public static String getPath(final Context context, final Uri uri) {

        final boolean isKitKat = Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT;

        Log.i(TAG, "uri: "+ uri);
        // DocumentProvider
        if (isKitKat && DocumentsContract.isDocumentUri(context, uri)) {
            Log.i(TAG, "is document");
            // ExternalStorageProvider
            if (isExternalStorageDocument(uri)) {
                final String docId = DocumentsContract.getDocumentId(uri);
                final String[] split = docId.split(":");
                final String type = split[0];

                Log.i(TAG, "type: "+type);

                //if ("primary".equalsIgnoreCase(type))
                //{
                    //return Environment.getExternalStorageDirectory() + "/" + split[1];
                //}
                final String path = mntPre + type + "/" + split[1];
                /*final String[] split1 = path.split("/");

                String rPath = "";
                if(split1.length > 0) {
                    for (int j = 0; j < split1.length-1; j++) {
                        rPath += (split1[j]+"/");
                    }
                }*/

                return path;
                // TODO handle non-primary volumes
            }
            // DownloadsProvider
            else if (isDownloadsDocument(uri)) {

                final String id = DocumentsContract.getDocumentId(uri);
                final Uri contentUri = ContentUris.withAppendedId(
                        Uri.parse("content://downloads/public_downloads"), Long.valueOf(id));

                return getDataColumn(context, contentUri, null, null);
            }
            // MediaProvider
            else if (isMediaDocument(uri)) {
                final String docId = DocumentsContract.getDocumentId(uri);
                final String[] split = docId.split(":");
                final String type = split[0];

                Uri contentUri = null;
                if ("image".equals(type)) {
                    contentUri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI;
                } else if ("video".equals(type)) {
                    contentUri = MediaStore.Video.Media.EXTERNAL_CONTENT_URI;
                } else if ("audio".equals(type)) {
                    contentUri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI;
                }

                final String selection = "_id=?";
                final String[] selectionArgs = new String[] {
                        split[1]
                };

                return getDataColumn(context, contentUri, selection, selectionArgs);
            }
        }
        // MediaStore (and general)
        else if ("content".equalsIgnoreCase(uri.getScheme())) {

            // Return the remote address
            if (isGooglePhotosUri(uri))
                return uri.getLastPathSegment();

            return getDataColumn(context, uri, null, null);
        }
        // File
        else if ("file".equalsIgnoreCase(uri.getScheme())) {
            return uri.getPath();
        }

        return null;
    }
    /**
     **
     * Get the value of the data column for this Uri. This is useful for
     * MediaStore Uris, and other file-based ContentProviders.
     *
     * @param context The context.
     * @param uri The Uri to query.
     * @param selection (Optional) Filter used in the query.
     * @param selectionArgs (Optional) Selection arguments used in the query.
     * @return The value of the _data column, which is typically a file path.
     */
    public static String getDataColumn(Context context, Uri uri, String selection,
                                       String[] selectionArgs) {

        Cursor cursor = null;
        final String column = "_data";
        final String[] projection = {
                column
        };

        try {
            cursor = context.getContentResolver().query(uri, projection, selection, selectionArgs,
                    null);
            if (cursor != null && cursor.moveToFirst()) {
                final int index = cursor.getColumnIndexOrThrow(column);
                return cursor.getString(index);
            }
        } finally {
            if (cursor != null)
                cursor.close();
        }
        return null;
    }


    /**
     * @param uri The Uri to check.
     * @return Whether the Uri authority is ExternalStorageProvider.
     */
    public static boolean isExternalStorageDocument(Uri uri) {
        return "com.android.externalstorage.documents".equals(uri.getAuthority());
    }

    /**
     * @param uri The Uri to check.
     * @return Whether the Uri authority is DownloadsProvider.
     */
    public static boolean isDownloadsDocument(Uri uri) {
        return "com.android.providers.downloads.documents".equals(uri.getAuthority());
    }

    /**
     * @param uri The Uri to check.
     * @return Whether the Uri authority is MediaProvider.
     */
    public static boolean isMediaDocument(Uri uri) {
        return "com.android.providers.media.documents".equals(uri.getAuthority());
    }

    /**
     * @param uri The Uri to check.
     * @return Whether the Uri authority is Google Photos.
     */
    public static boolean isGooglePhotosUri(Uri uri) {
        return "com.google.android.apps.photos.content".equals(uri.getAuthority());
    }

    // 获取主存储卡路径
    public static String getPrimaryStoragePath(Context context) {
        try {
            StorageManager sm = (StorageManager)context.getSystemService(STORAGE_SERVICE);
            Method getVolumePathsMethod = StorageManager.class.getMethod("getVolumePaths");
            String[] paths = (String[]) getVolumePathsMethod.invoke(sm);
            // first element in paths[] is primary storage path
            return paths[0];
        } catch (Exception e) {
            Log.e(TAG, "getPrimaryStoragePath() failed", e);
        }
        return null;
    }

    // 获取次存储卡路径,一般就是外置 TF 卡了. 不过也有可能是 USB OTG 设备...
    // 其实只要判断第二章卡在挂载状态,就可以用了.
    public static String getSecondaryStoragePath(Context context) {
        try {
            StorageManager sm = (StorageManager) context.getSystemService(STORAGE_SERVICE);
            Method getVolumePathsMethod = StorageManager.class.getMethod("getVolumePaths");
            String[] paths = (String[]) getVolumePathsMethod.invoke(sm);
            // second element in paths[] is secondary storage path
            return paths.length <= 1 ? null : paths[1];
        } catch (Exception e) {
            Log.e(TAG, "getSecondaryStoragePath() failed", e);
        }
        return null;
    }

    // 获取存储卡的挂载状态. path 参数传入上两个方法得到的路径
    public static String getStorageState(Context context, String path) {
        try {
            StorageManager sm = (StorageManager) context.getSystemService(STORAGE_SERVICE);
            Method getVolumeStateMethod = StorageManager.class.getMethod("getVolumeState", new Class[] {String.class});
            String state = (String) getVolumeStateMethod.invoke(sm, path);
            return state;
        } catch (Exception e) {
            Log.e(TAG, "getStorageState() failed", e);
        }
        return null;
    }

    public static Uri pathToUri(String path) {
        String uriStr = uriPre;

        uriStr = path.replace(mntPre, uriPre);

        Uri uri = Uri.parse(uriStr);

        return uri;
    }

    public static boolean checkFileTypeAudio(String pathU) {
        String path = pathU.toLowerCase();

        for(String surfix : mediaTypeAudio) {
            if(path.endsWith(surfix)) {
                return true;
            }
        }

        return false;
    }

    public static boolean checkFileTypeVideo(String pathU) {
        String path = pathU.toLowerCase();

        for(String surfix : mediaTypeVideo) {
            if(path.endsWith(surfix)) {
                return true;
            }
        }

        return false;
    }
    public static boolean checkFileType(String pathU) {
        return checkFileTypeAudio(pathU) || checkFileTypeVideo(pathU);
    }
}
