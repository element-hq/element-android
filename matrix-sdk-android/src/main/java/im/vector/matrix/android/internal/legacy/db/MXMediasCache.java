/*
 * Copyright 2015 OpenMarket Ltd
 * Copyright 2018 New Vector Ltd
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package im.vector.matrix.android.internal.legacy.db;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.ExifInterface;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.support.annotation.Nullable;
import android.text.TextUtils;
import android.webkit.MimeTypeMap;
import android.widget.ImageView;

import im.vector.matrix.android.internal.legacy.HomeServerConnectionConfig;
import im.vector.matrix.android.internal.legacy.crypto.MXEncryptedAttachments;
import im.vector.matrix.android.internal.legacy.listeners.IMXMediaDownloadListener;
import im.vector.matrix.android.internal.legacy.listeners.IMXMediaUploadListener;
import im.vector.matrix.android.internal.legacy.listeners.MXMediaDownloadListener;
import im.vector.matrix.android.internal.legacy.network.NetworkConnectivityReceiver;
import im.vector.matrix.android.internal.legacy.rest.callback.ApiCallback;
import im.vector.matrix.android.internal.legacy.rest.callback.SimpleApiCallback;
import im.vector.matrix.android.internal.legacy.rest.client.MediaScanRestClient;
import im.vector.matrix.android.internal.legacy.rest.model.crypto.EncryptedFileInfo;
import im.vector.matrix.android.internal.legacy.util.ContentManager;
import im.vector.matrix.android.internal.legacy.util.ContentUtils;
import im.vector.matrix.android.internal.legacy.util.Log;
import im.vector.matrix.android.internal.legacy.util.MXOsHandler;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.concurrent.RejectedExecutionException;

public class MXMediasCache {

    private static final String LOG_TAG = MXMediasCache.class.getSimpleName();

    /**
     * The medias folders.
     */
    // Put the previous folders used for cache here. Every time the cache management change (change of id format, etc.),
    // append the current cache folder to this list, and change value of MXMEDIA_STORE_FOLDER (typically increment the value)
    private static final List<String> sPreviousMediaCacheFolders = Arrays.asList(
            "MXMediaStore",
            "MXMediaStore2"
    );

    private static final String MXMEDIA_STORE_FOLDER = "MXMediaStore3";
    private static final String MXMEDIA_STORE_MEMBER_THUMBNAILS_FOLDER = "MXMemberThumbnailsStore";
    private static final String MXMEDIA_STORE_IMAGES_FOLDER = "Images";
    private static final String MXMEDIA_STORE_OTHERS_FOLDER = "Others";
    private static final String MXMEDIA_STORE_TMP_FOLDER = "tmp";
    private static final String MXMEDIA_STORE_SHARE_FOLDER = "share";

    /**
     * The content manager
     */
    private ContentManager mContentManager;

    /**
     * The medias folders list.
     */
    private File mMediasFolderFile;
    private File mImagesFolderFile;
    private File mOthersFolderFile;
    private File mThumbnailsFolderFile;

    // This folder will contain decrypted media files
    private File mTmpFolderFile;

    // This folder will contain decrypted media files, for file sharing
    private File mShareFolderFile;

    // track the network updates
    private final NetworkConnectivityReceiver mNetworkConnectivityReceiver;

    // the background thread
    static HandlerThread mDecryptingHandlerThread = null;
    static MXOsHandler mDecryptingHandler = null;
    static android.os.Handler mUIHandler = null;

    private MediaScanRestClient mMediaScanRestClient;

    /**
     * Constructor
     *
     * @param contentManager              the content manager.
     * @param networkConnectivityReceiver the network connectivity receiver
     * @param userID                      the account user Id.
     * @param context                     the context
     */
    public MXMediasCache(ContentManager contentManager, NetworkConnectivityReceiver networkConnectivityReceiver, String userID, Context context) {
        mContentManager = contentManager;
        mNetworkConnectivityReceiver = networkConnectivityReceiver;

        File mediaBaseFolderFile;

        // Clear previous cache
        for (String previousMediaCacheFolder : sPreviousMediaCacheFolders) {
            mediaBaseFolderFile = new File(context.getApplicationContext().getFilesDir(), previousMediaCacheFolder);

            if (mediaBaseFolderFile.exists()) {
                ContentUtils.deleteDirectory(mediaBaseFolderFile);
            }
        }

        mediaBaseFolderFile = new File(context.getApplicationContext().getFilesDir(), MXMEDIA_STORE_FOLDER);

        if (!mediaBaseFolderFile.exists()) {
            mediaBaseFolderFile.mkdirs();
        }

        // create the dir tree
        mMediasFolderFile = new File(mediaBaseFolderFile, userID);
        mImagesFolderFile = new File(mMediasFolderFile, MXMEDIA_STORE_IMAGES_FOLDER);
        mOthersFolderFile = new File(mMediasFolderFile, MXMEDIA_STORE_OTHERS_FOLDER);
        mTmpFolderFile = new File(mMediasFolderFile, MXMEDIA_STORE_TMP_FOLDER);

        if (mTmpFolderFile.exists()) {
            ContentUtils.deleteDirectory(mTmpFolderFile);
        }
        mTmpFolderFile.mkdirs();

        mShareFolderFile = new File(mMediasFolderFile, MXMEDIA_STORE_SHARE_FOLDER);

        if (mShareFolderFile.exists()) {
            ContentUtils.deleteDirectory(mShareFolderFile);
        }
        mShareFolderFile.mkdirs();

        mThumbnailsFolderFile = new File(mediaBaseFolderFile, MXMEDIA_STORE_MEMBER_THUMBNAILS_FOLDER);

        // use the same thread for all the sessions
        if (null == mDecryptingHandlerThread) {
            mDecryptingHandlerThread = new HandlerThread("MXMediaDecryptingBackgroundThread", Thread.MIN_PRIORITY);
            mDecryptingHandlerThread.start();
            mDecryptingHandler = new MXOsHandler(mDecryptingHandlerThread.getLooper());
            mUIHandler = new Handler(Looper.getMainLooper());
        }
    }

    /**
     * Returns the mediasFolder files.
     * Creates it if it does not exist
     *
     * @return the medias folder file.
     */
    private File getMediasFolderFile() {
        if (!mMediasFolderFile.exists()) {
            mMediasFolderFile.mkdirs();
        }

        return mMediasFolderFile;
    }

    /**
     * Returns the folder file for a dedicated mimetype.
     * Creates it if it does not exist
     *
     * @param mimeType the media mimetype.
     * @return the folder file.
     */
    private File getFolderFile(String mimeType) {
        File file;

        //
        if ((null == mimeType) || mimeType.startsWith("image/")) {
            file = mImagesFolderFile;
        } else {
            file = mOthersFolderFile;
        }

        if (!file.exists()) {
            file.mkdirs();
        }

        return file;
    }

    /**
     * Returns the thumbnails folder.
     * Creates it if it does not exist
     *
     * @return the thumbnails folder file.
     */
    private File getThumbnailsFolderFile() {
        if (!mThumbnailsFolderFile.exists()) {
            mThumbnailsFolderFile.mkdirs();
        }

        return mThumbnailsFolderFile;
    }

    /**
     * Compute the medias cache size
     *
     * @param context  the context
     * @param callback the asynchronous callback
     */
    public static void getCachesSize(final Context context, final ApiCallback<Long> callback) {
        AsyncTask<Void, Void, Long> task = new AsyncTask<Void, Void, Long>() {
            @Override
            protected Long doInBackground(Void... params) {
                return ContentUtils.getDirectorySize(context,
                        new File(context.getApplicationContext().getFilesDir(), MXMEDIA_STORE_FOLDER),
                        1);
            }

            @Override
            protected void onPostExecute(Long result) {
                Log.d(LOG_TAG, "## getCachesSize() : " + result);
                if (null != callback) {
                    callback.onSuccess(result);
                }
            }
        };
        try {
            task.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
        } catch (Exception e) {
            Log.e(LOG_TAG, "## getCachesSize() : failed " + e.getMessage(), e);
            task.cancel(true);
        }
    }

    /**
     * Remove medias older than ts
     *
     * @param ts             the ts
     * @param filePathToKeep set of files to keep
     * @return length of deleted files
     */
    public long removeMediasBefore(long ts, Set<String> filePathToKeep) {
        long length = 0;

        length += removeMediasBefore(getMediasFolderFile(), ts, filePathToKeep);
        length += removeMediasBefore(getThumbnailsFolderFile(), ts, filePathToKeep);

        return length;
    }

    /**
     * Recursive method to remove older messages
     *
     * @param folder         the base folder
     * @param aTs            the ts
     * @param filePathToKeep set of files to keep
     * @return length of deleted files
     */
    private long removeMediasBefore(File folder, long aTs, Set<String> filePathToKeep) {
        long length = 0;
        File[] files = folder.listFiles();

        if (null != files) {
            for (int i = 0; i < files.length; i++) {
                File file = files[i];

                if (!file.isDirectory()) {

                    if (!filePathToKeep.contains(file.getPath())) {
                        long ts = ContentUtils.getLastAccessTime(file);
                        if (ts < aTs) {
                            length += file.length();
                            file.delete();
                        }
                    }
                } else {
                    length += removeMediasBefore(file, aTs, filePathToKeep);
                }
            }
        }

        return length;
    }

    /**
     * Clear the medias caches.
     */
    public void clear() {
        ContentUtils.deleteDirectory(getMediasFolderFile());

        ContentUtils.deleteDirectory(mThumbnailsFolderFile);

        // clear the media cache
        MXMediaDownloadWorkerTask.clearBitmapsCache();

        // cancel pending uploads.
        MXMediaUploadWorkerTask.cancelPendingUploads();
    }

    /**
     * The thumbnails cached is not cleared when logging out a session
     * because many sessions share the same thumbnails.
     * This method must be called when performing an application logout
     * i.e. logging out of all sessions.
     *
     * @param applicationContext the application context
     */
    public static void clearThumbnailsCache(Context applicationContext) {
        ContentUtils.deleteDirectory(new File(new File(applicationContext.getApplicationContext().getFilesDir(), MXMediasCache.MXMEDIA_STORE_FOLDER),
                MXMEDIA_STORE_MEMBER_THUMBNAILS_FOLDER));
    }

    /**
     * Provide the thumbnail file.
     *
     * @param url  the thumbnail url/
     * @param size the thumbnail size.
     * @return the File if it exits.
     */
    @Nullable
    public File thumbnailCacheFile(String url, int size) {
        // We use the download task id to define a cache id
        String thumbnailCacheId = mContentManager.downloadTaskIdForMatrixMediaContent(url);

        if (null != thumbnailCacheId) {
            if (size > 0) {
                thumbnailCacheId += "_w_" + size + "_h_" + size;
            }
            String filename = MXMediaDownloadWorkerTask.buildFileName(thumbnailCacheId, "image/jpeg");

            try {
                File file = new File(getThumbnailsFolderFile(), filename);

                if (file.exists()) {
                    return file;
                }
            } catch (Exception e) {
                Log.e(LOG_TAG, "thumbnailCacheFile failed " + e.getMessage(), e);
            }
        }

        return null;
    }

    /**
     * Return the cache file name for a media defined by its URL and its mime type.
     *
     * @param url      the media URL
     * @param width    the media width
     * @param height   the media height
     * @param mimeType the media mime type
     * @return the media file it is found
     */
    @Nullable
    private File mediaCacheFile(String url, int width, int height, String mimeType) {
        // sanity check
        if (null == url) {
            return null;
        }

        String filename;
        if (url.startsWith("file:")) {
            filename = url;
        } else {
            // We use the download task id to define a cache id
            String cacheId = mContentManager.downloadTaskIdForMatrixMediaContent(url);
            if (null != cacheId) {
                if ((width > 0) && (height > 0)) {
                    cacheId += "_w_" + width + "_h_" + height;
                }
                filename = MXMediaDownloadWorkerTask.buildFileName(cacheId, mimeType);
            } else {
                return null;
            }
        }

        try {
            // already a local file
            if (filename.startsWith("file:")) {
                Uri uri = Uri.parse(filename);
                filename = uri.getLastPathSegment();
            }

            File file = new File(getFolderFile(mimeType), filename);

            if (file.exists()) {
                return file;
            }

        } catch (Exception e) {
            Log.e(LOG_TAG, "mediaCacheFile failed " + e.getMessage(), e);
        }

        return null;
    }

    /**
     * Tells if a media is cached
     *
     * @param url      the url
     * @param mimeType the mimetype
     * @return true if the media is cached
     */
    public boolean isMediaCached(String url, String mimeType) {
        return isMediaCached(url, -1, -1, mimeType);
    }

    /**
     * Tells if a media is cached
     *
     * @param url      the media URL
     * @param width    the media width
     * @param height   the media height
     * @param mimeType the media mime type
     * @return the media file is cached
     */
    public boolean isMediaCached(String url, int width, int height, String mimeType) {
        return null != mediaCacheFile(url, width, height, mimeType);
    }

    /**
     * Create a temporary decrypted copy of a media.
     * It must be released when it is not used anymore with clearTmpDecryptedMediaCache().
     *
     * @param url               the media url
     * @param mimeType          the media mime type
     * @param encryptedFileInfo the encryption information
     * @param callback          the asynchronous callback
     * @return true if the file is cached
     */
    public boolean createTmpDecryptedMediaFile(String url,
                                               String mimeType,
                                               EncryptedFileInfo encryptedFileInfo,
                                               ApiCallback<File> callback) {
        return createTmpDecryptedMediaFile(url,
                -1,
                -1,
                mimeType,
                encryptedFileInfo,
                callback);
    }

    /**
     * Create a temporary decrypted copy of a media.
     * It must be released when it is not used anymore with clearTmpDecryptedMediaCache().
     *
     * @param url               the media URL
     * @param width             the media width
     * @param height            the media height
     * @param mimeType          the media mime type
     * @param encryptedFileInfo the encryption information
     * @param callback          the asynchronous callback
     * @return true if the file is cached
     */
    public boolean createTmpDecryptedMediaFile(String url,
                                               int width,
                                               int height,
                                               String mimeType,
                                               final EncryptedFileInfo encryptedFileInfo,
                                               final ApiCallback<File> callback) {
        final File file = mediaCacheFile(url, width, height, mimeType);

        if (null != file) {
            mDecryptingHandler.post(new Runnable() {
                @Override
                public void run() {
                    final File tmpFile = new File(mTmpFolderFile, file.getName());

                    // create it only if it does not exist yet
                    if (!tmpFile.exists()) {
                        try {
                            InputStream fis = new FileInputStream(file);

                            if (null != encryptedFileInfo) {
                                InputStream is = MXEncryptedAttachments.decryptAttachment(fis, encryptedFileInfo);
                                fis.close();
                                fis = is;
                            }

                            FileOutputStream fos = new FileOutputStream(tmpFile);
                            byte[] buf = new byte[2048];
                            int len;
                            while ((len = fis.read(buf)) != -1) {
                                fos.write(buf, 0, len);
                            }

                            fis.close();
                            fos.close();
                        } catch (Exception e) {
                            Log.e(LOG_TAG, "## createTmpDecryptedMediaFile() failed " + e.getMessage(), e);
                        }
                    }

                    mUIHandler.post(new Runnable() {
                        @Override
                        public void run() {
                            callback.onSuccess(tmpFile);
                        }
                    });
                }
            });
        }
        return (null != file);
    }

    /**
     * Clear the temporary decrypted media cache folder
     */
    public void clearTmpDecryptedMediaCache() {
        Log.d(LOG_TAG, "clearTmpDecryptedMediaCache()");

        if (mTmpFolderFile.exists()) {
            ContentUtils.deleteDirectory(mTmpFolderFile);
        }

        if (!mTmpFolderFile.exists()) {
            mTmpFolderFile.mkdirs();
        }
    }

    /**
     * Move a decrypted media file to the /share folder, to avoid this file to be deleted if in the /tmp folder
     *
     * @param fileToMove The file to move
     * @param filename   the filename, without path
     * @return The copied file in the Share folder location
     */
    public File moveToShareFolder(final File fileToMove,
                                  final String filename) {
        File dstFile = new File(mShareFolderFile, filename);

        if (dstFile.exists()) {
            if (!dstFile.delete()) {
                Log.w(LOG_TAG, "Unable to delete file");
            }
        }

        if (!fileToMove.renameTo(dstFile)) {
            Log.w(LOG_TAG, "Unable to rename file");

            // Return the original file
            return fileToMove;
        }

        return dstFile;
    }

    /**
     * Clear the temporary shared decrypted media cache folder
     */
    public void clearShareDecryptedMediaCache() {
        Log.d(LOG_TAG, "clearShareDecryptedMediaCache()");

        if (mShareFolderFile.exists()) {
            ContentUtils.deleteDirectory(mShareFolderFile);
        }

        if (!mShareFolderFile.exists()) {
            mShareFolderFile.mkdirs();
        }
    }

    /**
     * Save a bitmap to the local cache
     * it could be used for unsent media to allow them to be resent.
     *
     * @param bitmap          the bitmap to save
     * @param defaultFileName the filename is provided, if null, a filename will be generated
     * @return the media cache URL
     */
    public String saveBitmap(Bitmap bitmap, String defaultFileName) {
        String filename = "file" + System.currentTimeMillis() + ".jpg";
        String cacheURL = null;

        try {
            if (null != defaultFileName) {
                File file = new File(getFolderFile(null), defaultFileName);
                file.delete();

                filename = Uri.fromFile(file).getLastPathSegment();
            }

            File file = new File(getFolderFile(null), filename);
            FileOutputStream fos = new FileOutputStream(file.getPath());

            // We got an java.lang.IllegalStateException: Can't compress a recycled bitmap
            if (bitmap.isRecycled()) {
                Log.w(LOG_TAG, "Trying to compress a recycled Bitmap. Create a copy first.");
                bitmap = Bitmap.createBitmap(bitmap);
            }

            bitmap.compress(Bitmap.CompressFormat.JPEG, 100, fos);

            fos.flush();
            fos.close();

            cacheURL = Uri.fromFile(file).toString();
        } catch (Exception e) {
            Log.e(LOG_TAG, "saveBitmap failed " + e.getMessage(), e);
        }

        return cacheURL;
    }

    /**
     * Save a media to the local cache
     * it could be used for unsent media to allow them to be resent.
     *
     * @param stream          the file stream to save
     * @param defaultFileName the filename is provided, if null, a filename will be generated
     * @param mimeType        the mime type.
     * @return the media cache URL
     */
    public String saveMedia(InputStream stream, String defaultFileName, String mimeType) {
        String filename = defaultFileName;

        if (null == filename) {
            filename = "file" + System.currentTimeMillis();

            if (null != mimeType) {
                String extension = MimeTypeMap.getSingleton().getExtensionFromMimeType(mimeType);

                if (null == extension) {
                    if (mimeType.lastIndexOf("/") >= 0) {
                        extension = mimeType.substring(mimeType.lastIndexOf("/") + 1);
                    }
                }

                if (!TextUtils.isEmpty(extension)) {
                    filename += "." + extension;
                }
            }
        }

        String cacheURL = null;

        try {
            File file = new File(getFolderFile(mimeType), filename);

            // if the file exits, delete it
            if (file.exists()) {
                file.delete();
            }

            FileOutputStream fos = new FileOutputStream(file.getPath());

            try {
                byte[] buf = new byte[1024 * 32];

                int len;
                while ((len = stream.read(buf)) != -1) {
                    fos.write(buf, 0, len);
                }
            } catch (Exception e) {
                Log.e(LOG_TAG, "saveMedia failed " + e.getMessage(), e);
            }

            fos.flush();
            fos.close();
            stream.close();

            cacheURL = Uri.fromFile(file).toString();
        } catch (Exception e) {
            Log.e(LOG_TAG, "saveMedia failed " + e.getMessage(), e);

        }

        return cacheURL;
    }

    /**
     * Replace a media cache by a file content.
     *
     * @param mediaUrl the mediaUrl
     * @param mimeType the mimeType.
     * @param fileUrl  the file which replaces the cached media.
     */
    public void saveFileMediaForUrl(String mediaUrl,
                                    String fileUrl,
                                    String mimeType) {
        saveFileMediaForUrl(mediaUrl,
                fileUrl,
                -1,
                -1,
                mimeType);
    }

    /**
     * Replace a media cache by a file content.
     * MediaUrl is the same model as the one used in loadBitmap.
     *
     * @param mediaUrl the mediaUrl
     * @param fileUrl  the file which replaces the cached media.
     * @param width    the expected image width
     * @param height   the expected image height
     * @param mimeType the mimeType.
     */
    public void saveFileMediaForUrl(String mediaUrl,
                                    String fileUrl,
                                    int width,
                                    int height,
                                    String mimeType) {
        saveFileMediaForUrl(mediaUrl,
                fileUrl,
                width,
                height,
                mimeType,
                false);
    }

    /**
     * Copy or Replace a media cache by a file content.
     * MediaUrl is the same model as the one used in loadBitmap.
     *
     * @param mediaUrl   the mediaUrl
     * @param fileUrl    the file which replaces the cached media.
     * @param width      the expected image width
     * @param height     the expected image height
     * @param mimeType   the mimeType.
     * @param keepSource keep the source file
     */
    public void saveFileMediaForUrl(String mediaUrl,
                                    String fileUrl,
                                    int width,
                                    int height,
                                    String mimeType,
                                    boolean keepSource) {
        // We use the download task id to define a cache id
        String cacheId = mContentManager.downloadTaskIdForMatrixMediaContent(mediaUrl);
        if (null != cacheId) {
            if ((width > 0) && (height > 0)) {
                cacheId += "_w_" + width + "_h_" + height;
            }
            String filename = MXMediaDownloadWorkerTask.buildFileName(cacheId, mimeType);

            try {
                // delete the current content
                File destFile = new File(getFolderFile(mimeType), filename);

                if (destFile.exists()) {
                    try {
                        destFile.delete();
                    } catch (Exception e) {
                        Log.e(LOG_TAG, "saveFileMediaForUrl delete failed " + e.getMessage(), e);
                    }
                }

                Uri uri = Uri.parse(fileUrl);
                File srcFile = new File(uri.getPath());

                if (keepSource) {
                    InputStream in = new FileInputStream(srcFile);
                    OutputStream out = new FileOutputStream(destFile);

                    // Transfer bytes from in to out
                    byte[] buf = new byte[1024];
                    int len;
                    while ((len = in.read(buf)) > 0) {
                        out.write(buf, 0, len);
                    }
                    in.close();
                    out.close();
                } else {
                    srcFile.renameTo(destFile);
                }

            } catch (Exception e) {
                Log.e(LOG_TAG, "saveFileMediaForUrl failed " + e.getMessage(), e);
            }
        }
    }

    /**
     * Load an avatar thumbnail.
     * The imageView image is updated when the bitmap is loaded or downloaded.
     *
     * @param hsConfig  the home server config.
     * @param imageView Ihe imageView to update with the image.
     * @param url       the image url
     * @param side      the avatar thumbnail side
     * @return a download identifier if the image is not cached else null.
     */
    public String loadAvatarThumbnail(HomeServerConnectionConfig hsConfig,
                                      ImageView imageView,
                                      String url,
                                      int side) {
        return loadBitmap(imageView.getContext(),
                hsConfig,
                imageView,
                url,
                side,
                side,
                0,
                ExifInterface.ORIENTATION_UNDEFINED,
                null,
                getThumbnailsFolderFile(),
                null);
    }

    /**
     * Load an avatar thumbnail.
     * The imageView image is updated when the bitmap is loaded or downloaded.
     *
     * @param hsConfig       the home server config.
     * @param imageView      Ihe imageView to update with the image.
     * @param url            the image url
     * @param side           the avatar thumbnail side
     * @param aDefaultAvatar the avatar to use when the Url is not reachable.
     * @return a download identifier if the image is not cached else null.
     */
    public String loadAvatarThumbnail(HomeServerConnectionConfig hsConfig,
                                      ImageView imageView,
                                      String url,
                                      int side,
                                      Bitmap aDefaultAvatar) {
        return loadBitmap(imageView.getContext(),
                hsConfig,
                imageView,
                url,
                side,
                side,
                0,
                ExifInterface.ORIENTATION_UNDEFINED,
                null,
                getThumbnailsFolderFile(),
                aDefaultAvatar,
                null);
    }

    /**
     * Tells if the avatar is cached
     *
     * @param url  the avatar url to test
     * @param size the thumbnail size
     * @return true if the avatar bitmap is cached.
     */
    public boolean isAvatarThumbnailCached(String url, int size) {
        boolean isCached = false;

        // We use the download task id to define a cache id
        String thumbnailCacheId = mContentManager.downloadTaskIdForMatrixMediaContent(url);
        if (null != thumbnailCacheId) {
            if (size > 0) {
                thumbnailCacheId += "_w_" + size + "_h_" + size;
            }
            isCached = MXMediaDownloadWorkerTask.isMediaCached(thumbnailCacheId);

            if (!isCached) {
                try {
                    isCached = (new File(getThumbnailsFolderFile(), MXMediaDownloadWorkerTask.buildFileName(thumbnailCacheId, "image/jpeg"))).exists();
                } catch (Throwable t) {
                    Log.e(LOG_TAG, "## isAvatarThumbnailCached() : failed " + t.getMessage(), t);
                }
            }
        }

        return isCached;
    }

    /**
     * Tells if the media URL is unreachable.
     *
     * @param url the url to test.
     * @return true if the media URL is unreachable.
     */
    public static boolean isMediaUrlUnreachable(String url) {
        return MXMediaDownloadWorkerTask.isMediaUrlUnreachable(url);
    }

    /**
     * Load a bitmap from the url.
     * The imageView image is updated when the bitmap is loaded or downloaded.
     *
     * @param hsConfig       The home server config.
     * @param imageView      The imageView to update with the image.
     * @param url            the image url
     * @param rotationAngle  the rotation angle (degrees)
     * @param orientation    the orientation (ExifInterface.ORIENTATION_XXX value)
     * @param mimeType       the mimeType.
     * @param encryptionInfo the encryption file info
     * @return a download identifier if the image is not cached else null.
     */
    public String loadBitmap(HomeServerConnectionConfig hsConfig,
                             ImageView imageView,
                             String url,
                             int rotationAngle,
                             int orientation,
                             String mimeType,
                             EncryptedFileInfo encryptionInfo) {
        return loadBitmap(hsConfig,
                imageView,
                url,
                -1,
                -1,
                rotationAngle,
                orientation,
                mimeType,
                encryptionInfo);
    }

    /**
     * Load a bitmap from the url.
     * The imageView image is updated when the bitmap is loaded or downloaded.
     *
     * @param hsConfig       The home server config.
     * @param context        The context
     * @param url            the image url
     * @param rotationAngle  the rotation angle (degrees)
     * @param orientation    the orientation (ExifInterface.ORIENTATION_XXX value)
     * @param mimeType       the mimeType.
     * @param encryptionInfo the encryption file info
     * @return a download identifier if the image is not cached.
     */
    public String loadBitmap(Context context,
                             HomeServerConnectionConfig hsConfig,
                             String url,
                             int rotationAngle,
                             int orientation,
                             String mimeType,
                             EncryptedFileInfo encryptionInfo) {
        return loadBitmap(context,
                hsConfig,
                null,
                url,
                -1,
                -1,
                rotationAngle,
                orientation,
                mimeType,
                getFolderFile(mimeType),
                encryptionInfo);
    }

    /**
     * Load a bitmap from an url.
     * The imageView image is updated when the bitmap is loaded or downloaded.
     * The width/height parameters are optional. If they are positive, download a thumbnail.
     * rotationAngle is set to Integer.MAX_VALUE when undefined : the EXIF metadata must be checked.
     *
     * @param hsConfig       The home server config.
     * @param imageView      The imageView to fill when the image is downloaded
     * @param url            the image url
     * @param width          the expected image width
     * @param height         the expected image height
     * @param rotationAngle  the rotation angle (degrees)
     * @param orientation    the orientation (ExifInterface.ORIENTATION_XXX value)
     * @param mimeType       the mimeType.
     * @param encryptionInfo the encryption file info
     * @return a download identifier if the image is not cached
     */
    public String loadBitmap(HomeServerConnectionConfig hsConfig,
                             ImageView imageView,
                             String url,
                             int width,
                             int height,
                             int rotationAngle,
                             int orientation,
                             String mimeType,
                             EncryptedFileInfo encryptionInfo) {
        return loadBitmap(imageView.getContext(),
                hsConfig,
                imageView,
                url,
                width,
                height,
                rotationAngle,
                orientation,
                mimeType,
                getFolderFile(mimeType),
                encryptionInfo);
    }

    // some tasks have been stacked because there are too many running ones.
    private final List<MXMediaDownloadWorkerTask> mSuspendedTasks = new ArrayList<>();

    /**
     * Check whether a download is in progress for the content at a Matrix media content URI
     * (in the form of "mxc://..."). Returns the identifier of the download task if any.
     *
     * @param contentUrl the matrix media url
     * @return the download ID if there is a pending download or null
     */
    @Nullable
    public String downloadIdFromUrl(String contentUrl) {
        // Check and resolve the provided URL, the resulting URL is used as download identifier.
        String downloadId = mContentManager.downloadTaskIdForMatrixMediaContent(contentUrl);

        if (null != downloadId && null != MXMediaDownloadWorkerTask.getMediaDownloadWorkerTask(downloadId)) {
            return downloadId;
        }

        return null;
    }

    /**
     * Download a media.
     *
     * @param context        the application context
     * @param hsConfig       the home server config.
     * @param url            the media url
     * @param mimeType       the media mimetype
     * @param encryptionInfo the encryption information
     * @return the download identifier if there is a pending download else null
     */
    public String downloadMedia(Context context,
                                HomeServerConnectionConfig hsConfig,
                                String url,
                                String mimeType,
                                EncryptedFileInfo encryptionInfo) {
        return downloadMedia(context,
                hsConfig,
                url,
                mimeType,
                encryptionInfo,
                null);
    }

    /**
     * Download a media.
     *
     * @param context        the application context
     * @param hsConfig       the home server config.
     * @param url            the media url
     * @param mimeType       the media mimetype
     * @param encryptionInfo the encryption information
     * @param listener       the encryption information
     * @return the download identifier if there is a pending download else null
     */
    public String downloadMedia(Context context,
                                HomeServerConnectionConfig hsConfig,
                                String url,
                                String mimeType,
                                EncryptedFileInfo encryptionInfo,
                                IMXMediaDownloadListener listener) {
        // sanity checks
        if ((null == mimeType) || (null == context)) {
            return null;
        }

        // Check the provided URL
        String downloadId = mContentManager.downloadTaskIdForMatrixMediaContent(url);

        // Return if the media url is not valid, or if the media is already downloaded
        if (null == downloadId || isMediaCached(url, mimeType)) {
            return null;
        }

        // is the media downloading?
        MXMediaDownloadWorkerTask task = MXMediaDownloadWorkerTask.getMediaDownloadWorkerTask(downloadId);
        if (null != task) {
            task.addDownloadListener(listener);
            return downloadId;
        }

        // Download it in background
        String downloadableUrl = mContentManager.getDownloadableUrl(url, null != encryptionInfo);
        task = new MXMediaDownloadWorkerTask(context,
                hsConfig,
                mNetworkConnectivityReceiver,
                getFolderFile(mimeType),
                downloadableUrl,
                downloadId,
                0,
                mimeType,
                encryptionInfo,
                mMediaScanRestClient,
                mContentManager.isAvScannerEnabled());
        task.addDownloadListener(listener);

        // avoid crash if there are too many running task
        try {
            task.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
        } catch (RejectedExecutionException e) {
            // too many tasks have been launched
            synchronized (mSuspendedTasks) {
                task.cancel(true);
                // create a new task from the existing one
                task = new MXMediaDownloadWorkerTask(task);
                mSuspendedTasks.add(task);
                // privacy
                //Log.e(LOG_TAG, "Suspend the task " + task.getUrl());
                Log.e(LOG_TAG, "Suspend the task ", e);
            }

        } catch (Exception e) {
            Log.e(LOG_TAG, "downloadMedia failed " + e.getMessage(), e);
            synchronized (mSuspendedTasks) {
                task.cancel(true);
            }
        }

        return downloadId;
    }

    /**
     * Start any suspended task
     */
    private void launchSuspendedTask() {
        synchronized (mSuspendedTasks) {
            // some task have been suspended because there were too many running ones ?
            if (!mSuspendedTasks.isEmpty()) {
                MXMediaDownloadWorkerTask task = mSuspendedTasks.get(0);

                // privacy
                //Log.d(LOG_TAG, "Restart the task " + task.getUrl());
                Log.d(LOG_TAG, "Restart a task ");

                // avoid crash if there are too many running task
                try {
                    task.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
                    mSuspendedTasks.remove(task);
                } catch (RejectedExecutionException e) {
                    task.cancel(true);

                    mSuspendedTasks.remove(task);
                    // create a new task from the existing one
                    task = new MXMediaDownloadWorkerTask(task);
                    mSuspendedTasks.add(task);

                    // privacy
                    //Log.d(LOG_TAG, "Suspend again the task " + task.getUrl() + " - " + task.getStatus());
                    Log.d(LOG_TAG, "Suspend again the task " + task.getStatus());
                } catch (Exception e) {
                    Log.e(LOG_TAG, "Try to Restart a task fails " + e.getMessage(), e);
                }
            }
        }
    }

    /**
     * The default bitmap to use when the media cannot be retrieved.
     */
    private static Bitmap mDefaultBitmap = null;

    /**
     * Load a bitmap from an url.
     * The imageView image is updated when the bitmap is loaded or downloaded.
     * The width/height parameters are optional. If they are positive, download a thumbnail.
     * <p>
     * The rotation angle is checked first.
     * If rotationAngle is set to Integer.MAX_VALUE, check the orientation is defined to a valid value.
     * If the orientation is defined, request the properly oriented image to the server
     *
     * @param context        the context
     * @param hsConfig       the home server config
     * @param imageView      the imageView to fill when the image is downloaded
     * @param url            the image url
     * @param width          the expected image width
     * @param height         the expected image height
     * @param rotationAngle  the rotation angle (degrees)
     * @param orientation    the orientation (ExifInterface.ORIENTATION_XXX value)
     * @param mimeType       the mimeType.
     * @param folderFile     the folder where the media should be stored
     * @param encryptionInfo the encryption file information.
     * @return a download identifier if the image is not cached
     */
    public String loadBitmap(Context context,
                             HomeServerConnectionConfig hsConfig,
                             final ImageView imageView,
                             String url,
                             int width,
                             int height,
                             int rotationAngle,
                             int orientation,
                             String mimeType,
                             File folderFile,
                             EncryptedFileInfo encryptionInfo) {
        return loadBitmap(context,
                hsConfig,
                imageView,
                url,
                width,
                height,
                rotationAngle,
                orientation,
                mimeType,
                folderFile,
                null,
                encryptionInfo);
    }

    /**
     * Load a bitmap from an url.
     * The imageView image is updated when the bitmap is loaded or downloaded.
     * The width/height parameters are optional. If they are positive, download a thumbnail.
     * <p>
     * The rotation angle is checked first.
     * If rotationAngle is set to Integer.MAX_VALUE, check the orientation is defined to a valid value.
     * If the orientation is defined, request the properly oriented image to the server
     *
     * @param context        the context
     * @param hsConfig       the home server config
     * @param imageView      the imageView to fill when the image is downloaded
     * @param url            the image url
     * @param width          the expected image width
     * @param height         the expected image height
     * @param rotationAngle  the rotation angle (degrees)
     * @param orientation    the orientation (ExifInterface.ORIENTATION_XXX value)
     * @param mimeType       the mimeType.
     * @param folderFile     the folder where the media should be stored
     * @param aDefaultBitmap the default bitmap to use when the url media cannot be retrieved.
     * @param encryptionInfo the file encryption info
     * @return a download identifier if the image is not cached
     */
    public String loadBitmap(Context context,
                             HomeServerConnectionConfig hsConfig,
                             final ImageView imageView,
                             String url,
                             int width,
                             int height,
                             int rotationAngle,
                             int orientation,
                             String mimeType,
                             File folderFile,
                             Bitmap aDefaultBitmap,
                             EncryptedFileInfo encryptionInfo) {
        // Check invalid bitmap size
        if ((0 == width) || (0 == height)) {
            return null;
        }

        if (null == mDefaultBitmap) {
            mDefaultBitmap = BitmapFactory.decodeResource(context.getResources(), android.R.drawable.ic_menu_gallery);
        }

        final Bitmap defaultBitmap = (null == aDefaultBitmap) ? mDefaultBitmap : aDefaultBitmap;

        // Check whether the url is valid
        String downloadId = mContentManager.downloadTaskIdForMatrixMediaContent(url);
        if (null == downloadId) {
            // Nothing to do
            if (null != imageView) {
                imageView.setImageBitmap(defaultBitmap);
            }
            return null;
        }

        // Resolve the provided URL.
        // Note: it is not possible to resize an encrypted image.
        String downloadableUrl;
        if (null == encryptionInfo && width > 0 && height > 0) {
            downloadableUrl = mContentManager.getDownloadableThumbnailUrl(url, width, height, ContentManager.METHOD_SCALE);
            downloadId += "_w_" + width + "_h_" + height;
        } else {
            downloadableUrl = mContentManager.getDownloadableUrl(url, null != encryptionInfo);
        }

        // the thumbnail params are ignored when encrypted
        if ((null == encryptionInfo)
                && (rotationAngle == Integer.MAX_VALUE)
                && (orientation != ExifInterface.ORIENTATION_UNDEFINED)
                && (orientation != ExifInterface.ORIENTATION_NORMAL)) {
            if (downloadableUrl.contains("?")) {
                downloadableUrl += "&apply_orientation=true";
            } else {
                downloadableUrl += "?apply_orientation=true";
            }
            downloadId += "_apply_orientation";
        }

        final String fDownloadId = downloadId;

        if (null != imageView) {
            imageView.setTag(fDownloadId);
        }

        // if the mime type is not provided, assume it is a jpeg file
        if (null == mimeType) {
            mimeType = "image/jpeg";
        }

        boolean isCached = MXMediaDownloadWorkerTask.bitmapForURL(context.getApplicationContext(),
                folderFile, downloadableUrl, downloadId, rotationAngle, mimeType, encryptionInfo, new SimpleApiCallback<Bitmap>() {
                    @Override
                    public void onSuccess(Bitmap bitmap) {
                        if (null != imageView) {
                            if (TextUtils.equals(fDownloadId, (String) imageView.getTag())) {
                                // display it
                                imageView.setImageBitmap((null != bitmap) ? bitmap : defaultBitmap);
                            }
                        }
                    }
                });

        if (isCached) {
            downloadId = null;
        } else {
            MXMediaDownloadWorkerTask currentTask = MXMediaDownloadWorkerTask.getMediaDownloadWorkerTask(downloadId);

            if (null != currentTask) {
                if (null != imageView) {
                    currentTask.addImageView(imageView);
                }
            } else {
                // Download it in background
                MXMediaDownloadWorkerTask task = new MXMediaDownloadWorkerTask(context,
                        hsConfig,
                        mNetworkConnectivityReceiver,
                        folderFile,
                        downloadableUrl,
                        downloadId,
                        rotationAngle,
                        mimeType,
                        encryptionInfo,
                        mMediaScanRestClient,
                        mContentManager.isAvScannerEnabled());

                if (null != imageView) {
                    task.addImageView(imageView);
                }

                task.setDefaultBitmap(defaultBitmap);

                // check at the end of the download, if a suspended task can be launched again.
                task.addDownloadListener(new MXMediaDownloadListener() {
                    @Override
                    public void onDownloadComplete(String downloadId) {
                        launchSuspendedTask();
                    }
                });


                // avoid crash if there are too many running task
                try {
                    task.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
                } catch (RejectedExecutionException e) {
                    // too many tasks have been launched
                    synchronized (mSuspendedTasks) {
                        task.cancel(true);
                        // create a new task from the existing one
                        task = new MXMediaDownloadWorkerTask(task);
                        mSuspendedTasks.add(task);
                        // privacy
                        //Log.e(LOG_TAG, "Suspend the task " + task.getUrl());
                        Log.e(LOG_TAG, "Suspend a task", e);
                    }

                } catch (Exception e) {
                    Log.e(LOG_TAG, "loadBitmap failed " + e.getMessage(), e);
                }
            }
        }

        return downloadId;
    }

    /**
     * Returns the download progress (percentage).
     *
     * @param downloadId the downloadId provided by loadBitmap;
     * @return the download progress
     */
    public int getProgressValueForDownloadId(String downloadId) {
        MXMediaDownloadWorkerTask currentTask = MXMediaDownloadWorkerTask.getMediaDownloadWorkerTask(downloadId);

        if (null != currentTask) {
            return currentTask.getProgress();
        }
        return -1;
    }

    /**
     * Returns the download  stats for a dedicated download id.
     *
     * @param downloadId the downloadId provided by loadBitmap;
     * @return the download stats
     */
    @Nullable
    public IMXMediaDownloadListener.DownloadStats getStatsForDownloadId(String downloadId) {
        MXMediaDownloadWorkerTask task = MXMediaDownloadWorkerTask.getMediaDownloadWorkerTask(downloadId);

        if (null != task) {
            return task.getDownloadStats();
        }

        return null;
    }

    /**
     * Add a download listener for an downloadId.
     *
     * @param downloadId The uploadId.
     * @param listener   the download listener.
     */
    public void addDownloadListener(String downloadId, IMXMediaDownloadListener listener) {
        MXMediaDownloadWorkerTask task = MXMediaDownloadWorkerTask.getMediaDownloadWorkerTask(downloadId);

        if (null != task) {
            task.addDownloadListener(listener);
        }
        // Else consider calling listener.onDownloadComplete(downloadId) ?
    }

    /**
     * Cancel a download.
     *
     * @param downloadId the download id.
     */
    public void cancelDownload(String downloadId) {
        MXMediaDownloadWorkerTask task = MXMediaDownloadWorkerTask.getMediaDownloadWorkerTask(downloadId);

        if (null != task) {
            task.cancelDownload();
        }
    }

    /**
     * Upload a file
     *
     * @param contentStream the stream to upload
     * @param filename      the dst filename
     * @param mimeType      the mimetype
     * @param uploadId      the upload id
     * @param listener      the upload progress listener
     */
    public void uploadContent(InputStream contentStream,
                              String filename,
                              String mimeType,
                              String uploadId,
                              IMXMediaUploadListener listener) {
        try {
            new MXMediaUploadWorkerTask(mContentManager,
                    contentStream,
                    mimeType,
                    uploadId,
                    filename,
                    listener)
                    .executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
        } catch (Exception e) {
            // cannot start the task
            if (null != listener) {
                listener.onUploadError(uploadId, -1, null);
            }
        }
    }

    /**
     * Returns the upload progress (percentage) for a dedicated uploadId
     *
     * @param uploadId The uploadId.
     * @return the upload percentage. -1 means there is no pending upload.
     */
    public int getProgressValueForUploadId(String uploadId) {
        MXMediaUploadWorkerTask task = MXMediaUploadWorkerTask.getMediaUploadWorkerTask(uploadId);

        if (null != task) {
            return task.getProgress();
        }

        return -1;
    }

    /**
     * Returns the upload stats for a dedicated uploadId
     *
     * @param uploadId The uploadId.
     * @return the upload stats
     */
    public IMXMediaUploadListener.UploadStats getStatsForUploadId(String uploadId) {
        MXMediaUploadWorkerTask task = MXMediaUploadWorkerTask.getMediaUploadWorkerTask(uploadId);

        if (null != task) {
            return task.getStats();
        }

        return null;
    }


    /**
     * Add an upload listener for an uploadId.
     *
     * @param uploadId The uploadId.
     * @param listener the upload listener
     */
    public void addUploadListener(String uploadId, IMXMediaUploadListener listener) {
        MXMediaUploadWorkerTask task = MXMediaUploadWorkerTask.getMediaUploadWorkerTask(uploadId);

        if (null != task) {
            task.addListener(listener);
        }
    }

    /**
     * Cancel an upload.
     *
     * @param uploadId the upload Id
     */
    public void cancelUpload(String uploadId) {
        MXMediaUploadWorkerTask task = MXMediaUploadWorkerTask.getMediaUploadWorkerTask(uploadId);

        if (null != task) {
            task.cancelUpload();
        }
    }

    /**
     * Set MediaScan rest client
     *
     * @param mediaScanRestClient
     */
    public void setMediaScanRestClient(MediaScanRestClient mediaScanRestClient) {
        mMediaScanRestClient = mediaScanRestClient;
    }
}

