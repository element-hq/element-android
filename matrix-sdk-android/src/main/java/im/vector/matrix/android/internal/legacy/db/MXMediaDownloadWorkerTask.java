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
import android.net.Uri;
import android.os.AsyncTask;
import android.support.annotation.Nullable;
import android.support.v4.util.LruCache;
import android.text.TextUtils;
import android.util.Pair;
import android.webkit.MimeTypeMap;
import android.widget.ImageView;

import com.google.gson.JsonElement;
import com.google.gson.JsonParser;

import im.vector.matrix.android.internal.legacy.HomeServerConnectionConfig;
import im.vector.matrix.android.internal.legacy.RestClient;
import im.vector.matrix.android.internal.legacy.crypto.MXEncryptedAttachments;
import im.vector.matrix.android.internal.legacy.listeners.IMXMediaDownloadListener;
import im.vector.matrix.android.internal.legacy.network.NetworkConnectivityReceiver;
import im.vector.matrix.android.internal.legacy.rest.callback.ApiCallback;
import im.vector.matrix.android.internal.legacy.rest.callback.SimpleApiCallback;
import im.vector.matrix.android.internal.legacy.rest.client.MediaScanRestClient;
import im.vector.matrix.android.internal.legacy.rest.model.EncryptedMediaScanBody;
import im.vector.matrix.android.internal.legacy.rest.model.EncryptedMediaScanEncryptedBody;
import im.vector.matrix.android.internal.legacy.rest.model.MatrixError;
import im.vector.matrix.android.internal.legacy.rest.model.crypto.EncryptedBodyFileInfo;
import im.vector.matrix.android.internal.legacy.rest.model.crypto.EncryptedFileInfo;
import im.vector.matrix.android.internal.legacy.ssl.CertUtil;
import im.vector.matrix.android.internal.legacy.util.ImageUtils;
import im.vector.matrix.android.internal.legacy.util.JsonUtils;
import im.vector.matrix.android.internal.legacy.util.Log;
import org.matrix.olm.OlmPkEncryption;
import org.matrix.olm.OlmPkMessage;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.lang.ref.WeakReference;
import java.net.HttpURLConnection;
import java.net.URL;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.X509TrustManager;

/**
 * This class manages the media downloading in background.
 * <p>
 * JsonElement:  Error message if not null.
 */
class MXMediaDownloadWorkerTask extends AsyncTask<Void, Void, JsonElement> {

    private static final String LOG_TAG = MXMediaDownloadWorkerTask.class.getSimpleName();

    /**
     * Pending media URLs
     */
    private static final Map<String, MXMediaDownloadWorkerTask> sPendingDownloadById = new HashMap<>();

    /**
     * List of unreachable media urls.
     */
    private static final List<String> sUnreachableUrls = new ArrayList<>();

    // avoid sync on "this" because it might differ if there is a timer.
    private static final Object sSyncObject = new Object();

    /**
     * The medias cache
     */
    private static LruCache<String, Bitmap> sBitmapByDownloadIdCache = null;

    /**
     * The downloaded media callbacks.
     */
    private final List<IMXMediaDownloadListener> mDownloadListeners = new ArrayList<>();

    /**
     * The ImageView list to refresh when the media is downloaded.
     */
    private final List<WeakReference<ImageView>> mImageViewReferences;

    /**
     * The media URL.
     */
    private String mUrl;

    /**
     * The download identifier based on the original matrix content url for this media.
     */
    private String mDownloadId;

    /**
     * Tells if the anti-virus scanner is enabled.
     */
    private boolean mIsAvScannerEnabled;

    /**
     * The media mime type
     */
    private String mMimeType;

    /**
     * The application context
     */
    private Context mApplicationContext;

    /**
     * The directory in which the media must be stored.
     */
    private File mDirectoryFile;

    /**
     * The rotation to apply.
     */
    private int mRotation;

    /**
     * The download stats.
     */
    private IMXMediaDownloadListener.DownloadStats mDownloadStats;

    /**
     * Tells the download has been cancelled.
     */
    private boolean mIsDownloadCancelled;

    /**
     * Tells if the download has been completed
     */
    private boolean mIsDone;

    /**
     * The home server config.
     */
    private final HomeServerConnectionConfig mHsConfig;

    /**
     * The bitmap to use when the URL is unreachable.
     */
    private Bitmap mDefaultBitmap;

    /**
     * the encrypted file information
     */
    private final EncryptedFileInfo mEncryptedFileInfo;

    /**
     * Network updates tracker
     */
    private final NetworkConnectivityReceiver mNetworkConnectivityReceiver;

    /**
     * Rest client to retrieve public antivirus server key
     */
    @Nullable
    private MediaScanRestClient mMediaScanRestClient;

    /**
     * Download constants
     */
    private static final int DOWNLOAD_TIME_OUT = 10 * 1000;
    private static final int DOWNLOAD_BUFFER_READ_SIZE = 1024 * 32;

    //==============================================================================================================
    // static methods
    //==============================================================================================================

    /**
     * Clear the internal cache.
     */
    public static void clearBitmapsCache() {
        if (null != sBitmapByDownloadIdCache) {
            sBitmapByDownloadIdCache.evictAll();
        }

        // Clear the list of unreachable Urls, to retry to download it on next access
        synchronized (sUnreachableUrls) {
            sUnreachableUrls.clear();
        }
    }

    /**
     * Check if there is a pending download with the provided id.
     *
     * @param downloadId The identifier to check
     * @return the dedicated MXMediaDownloadWorkerTask if it exists.
     */
    public static MXMediaDownloadWorkerTask getMediaDownloadWorkerTask(String downloadId) {
        if (sPendingDownloadById.containsKey(downloadId)) {
            MXMediaDownloadWorkerTask task;
            synchronized (sPendingDownloadById) {
                task = sPendingDownloadById.get(downloadId);
            }
            return task;
        } else {
            return null;
        }
    }

    /**
     * Generate an unique ID for a string
     *
     * @param input the string
     * @return the unique ID
     */
    private static String uniqueId(String input) {
        String uniqueId = null;

        try {
            MessageDigest mDigest = MessageDigest.getInstance("SHA1");
            byte[] result = mDigest.digest(input.getBytes());
            StringBuffer sb = new StringBuffer();

            for (int i = 0; i < result.length; i++) {
                sb.append(Integer.toString((result[i] & 0xff) + 0x100, 16).substring(1));
            }

            uniqueId = sb.toString();
        } catch (Exception e) {
            Log.e(LOG_TAG, "uniqueId failed " + e.getMessage(), e);
        }

        if (null == uniqueId) {
            uniqueId = "" + Math.abs(input.hashCode() + (System.currentTimeMillis() + "").hashCode());
        }

        return uniqueId;
    }

    /**
     * Build a filename from a download Id
     *
     * @param downloadId the media url
     * @param mimeType   the mime type;
     * @return the cache filename
     */
    static String buildFileName(String downloadId, String mimeType) {
        String name = "file_" + uniqueId(downloadId);

        if (!TextUtils.isEmpty(mimeType)) {
            String fileExtension = MimeTypeMap.getSingleton().getExtensionFromMimeType(mimeType);

            // some devices don't support .jpeg files
            if ("jpeg".equals(fileExtension)) {
                fileExtension = "jpg";
            }

            if (null != fileExtension) {
                name += "." + fileExtension;
            }
        }

        return name;
    }

    /**
     * Tell if the media is cached with the provided cache identifier
     *
     * @param mediaCacheId
     * @return true if a media is cached with this identifier
     */
    public static boolean isMediaCached(String mediaCacheId) {
        boolean res = false;

        if ((null != sBitmapByDownloadIdCache)) {
            synchronized (sSyncObject) {
                res = (null != sBitmapByDownloadIdCache.get(mediaCacheId));
            }
        }

        return res;
    }

    /**
     * Tells if the media URL is unreachable.
     *
     * @param url the url to test.
     * @return true if the media URL is unreachable.
     */
    public static boolean isMediaUrlUnreachable(String url) {
        boolean res = true;

        if (!TextUtils.isEmpty(url)) {
            synchronized (sUnreachableUrls) {
                res = sUnreachableUrls.contains(url);
            }
        }

        return res;
    }

    /**
     * Search a cached bitmap from an url.
     * rotationAngle is set to Integer.MAX_VALUE when undefined : the EXIF metadata must be checked.
     *
     * @param baseFile       the base file
     * @param url            the actual media url
     * @param downloadId     the predefined id of the download task for this content
     * @param aRotation      the bitmap rotation
     * @param mimeType       the mime type
     * @param encryptionInfo the encryption information
     * @return true if the bitmap is cached
     */
    static boolean bitmapForURL(final Context context,
                                final File baseFile,
                                final String url,
                                final String downloadId,
                                final int aRotation,
                                final String mimeType,
                                final EncryptedFileInfo encryptionInfo,
                                final ApiCallback<Bitmap> callback) {
        if (TextUtils.isEmpty(url)) {
            Log.d(LOG_TAG, "bitmapForURL : null url");
            return false;
        }

        if (null == sBitmapByDownloadIdCache) {
            int lruSize = Math.min(20 * 1024 * 1024, (int) Runtime.getRuntime().maxMemory() / 8);

            Log.d(LOG_TAG, "bitmapForURL  lruSize : " + lruSize);

            sBitmapByDownloadIdCache = new LruCache<String, Bitmap>(lruSize) {
                @Override
                protected int sizeOf(String key, Bitmap bitmap) {
                    return bitmap.getRowBytes() * bitmap.getHeight(); // size in bytes
                }
            };
        }

        // the image is downloading in background
        if (null != getMediaDownloadWorkerTask(downloadId)) {
            return false;
        }

        // the url is invalid
        if (isMediaUrlUnreachable(url)) {
            return false;
        }

        final Bitmap cachedBitmap;

        synchronized (sSyncObject) {
            cachedBitmap = sBitmapByDownloadIdCache.get(downloadId);
        }

        if (null != cachedBitmap) {
            MXMediasCache.mUIHandler.post(new Runnable() {
                @Override
                public void run() {
                    callback.onSuccess(cachedBitmap);
                }
            });
            return true;
        }

        // invalid basefile
        if (null == baseFile) {
            return false;
        }

        // check if the image has not been saved in file system
        String filename = null;

        // the url is a file one
        if (url.startsWith("file:")) {
            // try to parse it
            try {
                Uri uri = Uri.parse(url);
                filename = uri.getPath();
            } catch (Exception e) {
                Log.e(LOG_TAG, "bitmapForURL #1 : " + e.getMessage(), e);
            }

            // cannot extract the filename -> sorry
            if (null == filename) {
                return false;
            }
        }

        // not a valid file name
        if (null == filename) {
            filename = buildFileName(downloadId, mimeType);
        }

        final String fFilename = filename;
        final File file = filename.startsWith(File.separator) ? new File(filename) : new File(baseFile, filename);

        if (!file.exists()) {
            return false;
        }

        MXMediasCache.mDecryptingHandler.post(new Runnable() {
            @Override
            public void run() {
                Bitmap bitmap = null;
                int rotation = aRotation;

                try {

                    InputStream fis = new FileInputStream(file);

                    if (null != encryptionInfo) {
                        InputStream decryptedIs = MXEncryptedAttachments.decryptAttachment(fis, encryptionInfo);
                        fis.close();
                        fis = decryptedIs;
                    }

                    // read the metadata
                    if (Integer.MAX_VALUE == rotation) {
                        rotation = ImageUtils.getRotationAngleForBitmap(context, Uri.fromFile(file));
                    }

                    if (null != fis) {
                        BitmapFactory.Options options = new BitmapFactory.Options();
                        options.inPreferredConfig = Bitmap.Config.ARGB_8888;

                        try {
                            bitmap = BitmapFactory.decodeStream(fis, null, options);
                        } catch (OutOfMemoryError error) {
                            System.gc();
                            Log.e(LOG_TAG, "bitmapForURL() : Out of memory 1 " + error, error);
                        }

                        //  try again
                        if (null == bitmap) {
                            try {
                                bitmap = BitmapFactory.decodeStream(fis, null, options);
                            } catch (OutOfMemoryError error) {
                                Log.e(LOG_TAG, "bitmapForURL() Out of memory 2 " + error, error);
                            }
                        }

                        if (null != bitmap) {
                            synchronized (sSyncObject) {
                                if (0 != rotation) {
                                    try {
                                        android.graphics.Matrix bitmapMatrix = new android.graphics.Matrix();
                                        bitmapMatrix.postRotate(rotation);

                                        Bitmap transformedBitmap = Bitmap.createBitmap(bitmap,
                                                0, 0, bitmap.getWidth(), bitmap.getHeight(), bitmapMatrix, false);

                                        // Bitmap.createBitmap() can return the same bitmap, so do not recycle it if it is the case
                                        if (transformedBitmap != bitmap) {
                                            bitmap.recycle();
                                        }

                                        bitmap = transformedBitmap;
                                    } catch (OutOfMemoryError ex) {
                                        Log.e(LOG_TAG, "bitmapForURL rotation error : " + ex.getMessage(), ex);
                                    }
                                }

                                // cache only small images
                                // caching large images does not make sense
                                // it would replace small ones.
                                // let assume that the application must be faster when showing the chat history.
                                if ((bitmap.getWidth() < 1000) && (bitmap.getHeight() < 1000)) {
                                    sBitmapByDownloadIdCache.put(downloadId, bitmap);
                                }
                            }
                        }

                        fis.close();
                    }

                } catch (FileNotFoundException e) {
                    Log.d(LOG_TAG, "bitmapForURL() : " + fFilename + " does not exist");
                } catch (Exception e) {
                    Log.e(LOG_TAG, "bitmapForURL() " + e, e);

                }

                final Bitmap fBitmap = bitmap;
                MXMediasCache.mUIHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        callback.onSuccess(fBitmap);
                    }
                });
            }
        });

        return true;
    }

    //==============================================================================================================
    // class methods
    //==============================================================================================================

    /**
     * MXMediaDownloadWorkerTask creator
     *
     * @param appContext                  the context
     * @param hsConfig                    the home server config
     * @param networkConnectivityReceiver the network connectivity receiver
     * @param directoryFile               the directory in which the media must be stored
     * @param url                         the media url
     * @param downloadId                  the predefined id of the download task for this content
     * @param rotation                    the rotation angle (degrees), use 0 by default
     * @param mimeType                    the mime type.
     * @param encryptedFileInfo           the encryption information
     * @param mediaScanRestClient         the media scan rest client
     * @param isAvScannerEnabled          tell whether an anti-virus scanner is enabled
     */
    public MXMediaDownloadWorkerTask(Context appContext,
                                     HomeServerConnectionConfig hsConfig,
                                     NetworkConnectivityReceiver networkConnectivityReceiver,
                                     File directoryFile,
                                     String url,
                                     String downloadId,
                                     int rotation,
                                     String mimeType,
                                     EncryptedFileInfo encryptedFileInfo,
                                     @Nullable MediaScanRestClient mediaScanRestClient,
                                     boolean isAvScannerEnabled) {
        mApplicationContext = appContext;
        mHsConfig = hsConfig;
        mNetworkConnectivityReceiver = networkConnectivityReceiver;
        mDirectoryFile = directoryFile;
        mUrl = url;
        mDownloadId = downloadId;
        mRotation = rotation;
        mMimeType = mimeType;
        mEncryptedFileInfo = encryptedFileInfo;
        mMediaScanRestClient = mediaScanRestClient;
        mIsAvScannerEnabled = isAvScannerEnabled;

        mImageViewReferences = new ArrayList<>();

        synchronized (sPendingDownloadById) {
            sPendingDownloadById.put(downloadId, this);
        }
    }

    /**
     * MXMediaDownloadWorkerTask creator
     *
     * @param task another bitmap task
     */
    public MXMediaDownloadWorkerTask(MXMediaDownloadWorkerTask task) {
        mApplicationContext = task.mApplicationContext;
        mHsConfig = task.mHsConfig;
        mNetworkConnectivityReceiver = task.mNetworkConnectivityReceiver;
        mDirectoryFile = task.mDirectoryFile;
        mUrl = task.mUrl;
        mDownloadId = task.mDownloadId;
        mRotation = task.mRotation;
        mMimeType = task.mMimeType;
        mEncryptedFileInfo = task.mEncryptedFileInfo;
        mIsAvScannerEnabled = task.mIsAvScannerEnabled;
        mMediaScanRestClient = task.mMediaScanRestClient;

        mImageViewReferences = task.mImageViewReferences;

        synchronized (sPendingDownloadById) {
            sPendingDownloadById.put(mDownloadId, this);
        }
    }

    /**
     * Cancels the current download.
     */
    public synchronized void cancelDownload() {
        mIsDownloadCancelled = true;
    }

    /**
     * @return tells if the current download has been cancelled.
     */
    public synchronized boolean isDownloadCancelled() {
        return mIsDownloadCancelled;
    }

    /**
     * @return the media URL.
     */
    public String getUrl() {
        return mUrl;
    }

    /**
     * Add an imageView to the list to refresh when the bitmap is downloaded.
     *
     * @param imageView an image view instance to refresh.
     */
    public void addImageView(ImageView imageView) {
        mImageViewReferences.add(new WeakReference<>(imageView));
    }

    /**
     * Set the default bitmap to use when the Url is unreachable.
     *
     * @param aBitmap the bitmap.
     */
    public void setDefaultBitmap(Bitmap aBitmap) {
        mDefaultBitmap = aBitmap;
    }

    /**
     * Add a download listener.
     *
     * @param listener the listener to add.
     */
    public void addDownloadListener(IMXMediaDownloadListener listener) {
        if (null != listener) {
            mDownloadListeners.add(listener);
        }
    }

    /**
     * Returns the download progress.
     *
     * @return the download progress
     */
    public int getProgress() {
        if (null != mDownloadStats) {
            return mDownloadStats.mProgress;
        }

        return -1;
    }

    /**
     * @return the download stats
     */
    public IMXMediaDownloadListener.DownloadStats getDownloadStats() {
        return mDownloadStats;
    }

    /**
     * @return true if the current task is an image one.
     */
    private boolean isBitmapDownloadTask() {
        return null != mMimeType && mMimeType.startsWith("image/");
    }

    /**
     * Push the download progress.
     *
     * @param startDownloadTime the start download time.
     */
    private void updateAndPublishProgress(long startDownloadTime) {
        mDownloadStats.mElapsedTime = (int) ((System.currentTimeMillis() - startDownloadTime) / 1000);

        if (mDownloadStats.mFileSize > 0) {
            if (mDownloadStats.mDownloadedSize >= mDownloadStats.mFileSize) {
                mDownloadStats.mProgress = 99;
            } else {
                mDownloadStats.mProgress = (int) (mDownloadStats.mDownloadedSize * 100L / mDownloadStats.mFileSize);
            }
        } else {
            mDownloadStats.mProgress = -1;
        }

        // avoid zero div
        if (System.currentTimeMillis() != startDownloadTime) {
            mDownloadStats.mBitRate = (int) (mDownloadStats.mDownloadedSize * 1000L / (System.currentTimeMillis() - startDownloadTime) / 1024);
        } else {
            mDownloadStats.mBitRate = -1;
        }

        if ((0 != mDownloadStats.mBitRate) && (mDownloadStats.mFileSize > 0) && (mDownloadStats.mFileSize > mDownloadStats.mDownloadedSize)) {
            mDownloadStats.mEstimatedRemainingTime = (mDownloadStats.mFileSize - mDownloadStats.mDownloadedSize) / 1024 / mDownloadStats.mBitRate;
        } else {
            mDownloadStats.mEstimatedRemainingTime = -1;
        }

        Log.d(LOG_TAG, "updateAndPublishProgress " + this + " : " + mDownloadStats.mProgress);

        publishProgress();
    }

    /**
     * Download and decode media in background.
     *
     * @param params
     * @return JsonElement if an error occurs
     */
    @Override
    protected JsonElement doInBackground(Void... params) {
        JsonElement jsonElementResult = null;

        MatrixError defaultError = new MatrixError();
        defaultError.errcode = MatrixError.UNKNOWN;

        // Note: No need for access token here

        try {
            URL url = new URL(mUrl);
            Log.d(LOG_TAG, "MXMediaDownloadWorkerTask " + this + " starts");

            mDownloadStats = new IMXMediaDownloadListener.DownloadStats();
            // don't known yet
            mDownloadStats.mEstimatedRemainingTime = -1;

            InputStream stream = null;

            int filelen = -1;
            HttpURLConnection connection = null;

            try {
                connection = (HttpURLConnection) url.openConnection();

                if (RestClient.getUserAgent() != null) {
                    connection.setRequestProperty("User-Agent", RestClient.getUserAgent());
                }

                if (mHsConfig != null && connection instanceof HttpsURLConnection) {
                    // Add SSL Socket factory.
                    HttpsURLConnection sslConn = (HttpsURLConnection) connection;
                    try {
                        Pair<SSLSocketFactory, X509TrustManager> pair = CertUtil.newPinnedSSLSocketFactory(mHsConfig);
                        sslConn.setSSLSocketFactory(pair.first);
                        sslConn.setHostnameVerifier(CertUtil.newHostnameVerifier(mHsConfig));
                    } catch (Exception e) {
                        Log.e(LOG_TAG, "doInBackground SSL exception " + e.getMessage(), e);
                    }
                }

                // add a timeout to avoid infinite loading display.
                float scale = (null != mNetworkConnectivityReceiver) ? mNetworkConnectivityReceiver.getTimeoutScale() : 1.0f;
                connection.setReadTimeout((int) (DOWNLOAD_TIME_OUT * scale));

                if (mIsAvScannerEnabled && null != mEncryptedFileInfo) {
                    // POST the encryption info to let the av scanner decrypt and scan the content.
                    connection.setRequestMethod("POST");
                    connection.setRequestProperty("Content-Type", "application/json; charset=UTF-8");
                    connection.setDoOutput(true);
                    connection.setUseCaches(false);

                    EncryptedMediaScanBody encryptedMediaScanBody = new EncryptedMediaScanBody();
                    encryptedMediaScanBody.encryptedFileInfo = mEncryptedFileInfo;

                    String data = JsonUtils.getCanonicalizedJsonString(encryptedMediaScanBody);

                    // Encrypt the data, if antivirus server supports it
                    String publicServerKey = getAntivirusServerPublicKey();

                    if (publicServerKey == null) {
                        // Error
                        throw new Exception("Unable to get public key");
                    } else if (!TextUtils.isEmpty(publicServerKey)) {
                        OlmPkEncryption olmPkEncryption = new OlmPkEncryption();

                        olmPkEncryption.setRecipientKey(publicServerKey);

                        OlmPkMessage message = olmPkEncryption.encrypt(data);

                        EncryptedMediaScanEncryptedBody encryptedMediaScanEncryptedBody = new EncryptedMediaScanEncryptedBody();
                        encryptedMediaScanEncryptedBody.encryptedBodyFileInfo = new EncryptedBodyFileInfo(message);

                        data = JsonUtils.getCanonicalizedJsonString(encryptedMediaScanEncryptedBody);
                    }
                    // Else: no public key on this server, do not encrypt data

                    OutputStream outputStream = connection.getOutputStream();
                    try {
                        outputStream.write(data.getBytes("UTF-8"));
                    } catch (Exception e) {
                        Log.e(LOG_TAG, "doInBackground Failed to serialize encryption info " + e.getMessage(), e);
                    } finally {
                        outputStream.close();
                    }
                }

                filelen = connection.getContentLength();
                stream = connection.getInputStream();
            } catch (Exception e) {
                Log.e(LOG_TAG, "bitmapForURL : fail to open the connection " + e.getMessage(), e);
                defaultError.error = e.getLocalizedMessage();

                // In case of 403, revert the key
                if (connection.getResponseCode() == 403 && mMediaScanRestClient != null) {
                    mMediaScanRestClient.resetServerPublicKey();
                }

                InputStream errorStream = connection.getErrorStream();

                if (null != errorStream) {
                    try {
                        BufferedReader streamReader = new BufferedReader(new InputStreamReader(errorStream, "UTF-8"));
                        StringBuilder responseStrBuilder = new StringBuilder();

                        String inputStr;

                        while ((inputStr = streamReader.readLine()) != null) {
                            responseStrBuilder.append(inputStr);
                        }

                        jsonElementResult = new JsonParser().parse(responseStrBuilder.toString());
                    } catch (Exception ee) {
                        Log.e(LOG_TAG, "bitmapForURL : Error parsing error " + ee.getMessage(), ee);
                    }
                }

                // privacy
                //Log.d(LOG_TAG, "MediaWorkerTask " + mUrl + " does not exist");
                Log.d(LOG_TAG, "MediaWorkerTask an url does not exist");

                // If some medias are not found,
                // do not try to reload them until the next application launch.
                // We mark this url as unreachable.
                // We can do this only if the av scanner is disabled or if the media is unencrypted,
                // (because the same url is used for all encrypted media when the av scanner is enabled).
                if (!mIsAvScannerEnabled || null == mEncryptedFileInfo) {
                    synchronized (sUnreachableUrls) {
                        sUnreachableUrls.add(mUrl);
                    }
                }
            }

            dispatchDownloadStart();

            // failed to open the remote stream without having exception
            if ((null == stream) && (null == jsonElementResult)) {
                jsonElementResult = new JsonParser().parse("Cannot open " + mUrl);

                // if some medias are not found
                // do not try to reload them until the next application launch.
                // We mark this url as unreachable.
                // We can do this only if the av scanner is disabled or if the media is unencrypted,
                // (because the same url is used for all encrypted media when the av scanner is enabled).
                if (!mIsAvScannerEnabled || null == mEncryptedFileInfo) {
                    synchronized (sUnreachableUrls) {
                        sUnreachableUrls.add(mUrl);
                    }
                }
            }

            // test if the download has not been cancelled
            if (!isDownloadCancelled() && (null == jsonElementResult)) {
                final long startDownloadTime = System.currentTimeMillis();

                String filename = buildFileName(mDownloadId, mMimeType) + ".tmp";
                FileOutputStream fos = new FileOutputStream(new File(mDirectoryFile, filename));

                mDownloadStats.mDownloadId = mDownloadId;
                mDownloadStats.mProgress = 0;
                mDownloadStats.mDownloadedSize = 0;
                mDownloadStats.mFileSize = filelen;
                mDownloadStats.mElapsedTime = 0;
                mDownloadStats.mEstimatedRemainingTime = -1;
                mDownloadStats.mBitRate = 0;

                // Publish progress every 100ms
                final Timer refreshTimer = new Timer();

                refreshTimer.scheduleAtFixedRate(new TimerTask() {
                    @Override
                    public void run() {
                        if (!mIsDone) {
                            updateAndPublishProgress(startDownloadTime);
                        }
                    }
                }, new Date(), 100);

                try {
                    byte[] buf = new byte[DOWNLOAD_BUFFER_READ_SIZE];
                    int len;
                    while (!isDownloadCancelled() && (len = stream.read(buf)) != -1) {
                        fos.write(buf, 0, len);
                        mDownloadStats.mDownloadedSize += len;
                    }

                    if (!isDownloadCancelled()) {
                        mDownloadStats.mProgress = 100;
                    }
                } catch (OutOfMemoryError outOfMemoryError) {
                    Log.e(LOG_TAG, "doInBackground: out of memory", outOfMemoryError);
                    defaultError.error = outOfMemoryError.getLocalizedMessage();
                } catch (Exception e) {
                    Log.e(LOG_TAG, "doInBackground fail to read image " + e.getMessage(), e);
                    defaultError.error = e.getLocalizedMessage();
                }

                mIsDone = true;

                close(stream);
                fos.flush();
                fos.close();

                refreshTimer.cancel();

                if ((null != connection) && (connection instanceof HttpsURLConnection)) {
                    connection.disconnect();
                }

                // the file has been successfully downloaded
                if (mDownloadStats.mProgress == 100) {
                    try {
                        File originalFile = new File(mDirectoryFile, filename);
                        String newFileName = buildFileName(mDownloadId, mMimeType);
                        File newFile = new File(mDirectoryFile, newFileName);
                        if (newFile.exists()) {
                            // Or you could throw here.
                            mApplicationContext.deleteFile(newFileName);
                        }
                        originalFile.renameTo(newFile);
                    } catch (Exception e) {
                        Log.e(LOG_TAG, "doInBackground : renaming error " + e.getMessage(), e);
                        defaultError.error = e.getLocalizedMessage();
                    }
                }
            }

            if (mDownloadStats.mProgress == 100) {
                Log.d(LOG_TAG, "The download " + this + " is done.");
            } else {
                if (null != jsonElementResult) {
                    Log.d(LOG_TAG, "The download " + this + " failed : mErrorAsJsonElement " + jsonElementResult.toString());
                } else {
                    Log.d(LOG_TAG, "The download " + this + " failed.");
                }
            }
        } catch (Exception e) {
            Log.e(LOG_TAG, "Unable to download media " + this, e);
            defaultError.error = e.getMessage();
        }

        // build a JSON from the error
        if (!TextUtils.isEmpty(defaultError.error)) {
            jsonElementResult = JsonUtils.getGson(false).toJsonTree(defaultError);
        }

        // remove the task from the loading one
        synchronized (sPendingDownloadById) {
            sPendingDownloadById.remove(mDownloadId);
        }

        return jsonElementResult;
    }

    /**
     * Get the public key of the antivirus server
     *
     * @return either empty string if server does not provide the public key, null in case of error, or the public server key
     */
    @Nullable
    private String getAntivirusServerPublicKey() {
        if (mMediaScanRestClient == null) {
            // Error
            Log.e(LOG_TAG, "Mandatory mMediaScanRestClient is null");
            return null;
        }

        // Make async request sync with a CountDownLatch
        // It is easier than adding a method to get the server public key synchronously with Call<T>.execute()
        final CountDownLatch latch = new CountDownLatch(1);
        final String[] publicServerKey = new String[1];

        mMediaScanRestClient.getServerPublicKey(new ApiCallback<String>() {
            @Override
            public void onSuccess(String serverPublicKey) {
                publicServerKey[0] = serverPublicKey;
                latch.countDown();
            }

            @Override
            public void onNetworkError(Exception e) {
                latch.countDown();
            }

            @Override
            public void onMatrixError(MatrixError e) {
                latch.countDown();
            }

            @Override
            public void onUnexpectedError(Exception e) {
                latch.countDown();
            }
        });

        try {
            latch.await(30, TimeUnit.SECONDS);
        } catch (InterruptedException ie) {

        }

        return publicServerKey[0];
    }

    /**
     * Close the stream.
     *
     * @param stream the stream to close.
     */
    private void close(InputStream stream) {
        try {
            stream.close();
        } catch (Exception e) {
            Log.e(LOG_TAG, "close error " + e.getMessage(), e);
        }
    }

    @Override
    protected void onProgressUpdate(Void... aVoid) {
        super.onProgressUpdate();
        dispatchOnDownloadProgress(mDownloadStats);
    }

    // Once complete, see if ImageView is still around and set bitmap.
    @Override
    protected void onPostExecute(JsonElement jsonElementError) {
        if (null != jsonElementError) {
            dispatchOnDownloadError(jsonElementError);
        } else if (isDownloadCancelled()) {
            dispatchDownloadCancel();
        } else {
            dispatchOnDownloadComplete();

            // image download
            // update the linked ImageViews.
            if (isBitmapDownloadTask()) {
                // retrieve the bitmap from the file s
                if (!bitmapForURL(mApplicationContext, mDirectoryFile, mUrl, mDownloadId, mRotation, mMimeType, mEncryptedFileInfo,
                        new SimpleApiCallback<Bitmap>() {
                            @Override
                            public void onSuccess(Bitmap bitmap) {
                                setBitmap((null == bitmap) ? mDefaultBitmap : bitmap);
                            }
                        })) {
                    setBitmap(mDefaultBitmap);
                }
            }
        }
    }

    /**
     * Set the bitmap in a referenced imageview
     *
     * @param bitmap the bitmap
     */
    private void setBitmap(Bitmap bitmap) {
        // update the imageViews image
        if (bitmap != null) {
            for (WeakReference<ImageView> weakRef : mImageViewReferences) {
                final ImageView imageView = weakRef.get();

                if (imageView != null && TextUtils.equals(mDownloadId, (String) imageView.getTag())) {
                    imageView.setImageBitmap(bitmap);
                }
            }
        }
    }


    //==============================================================================================================
    // Dispatchers
    //==============================================================================================================

    /**
     * Dispatch start event to the callbacks.
     */
    private void dispatchDownloadStart() {
        for (IMXMediaDownloadListener callback : mDownloadListeners) {
            try {
                callback.onDownloadStart(mDownloadId);
            } catch (Exception e) {
                Log.e(LOG_TAG, "dispatchDownloadStart error " + e.getMessage(), e);
            }
        }
    }

    /**
     * Dispatch stats update to the callbacks.
     *
     * @param stats the new stats value
     */
    private void dispatchOnDownloadProgress(IMXMediaDownloadListener.DownloadStats stats) {
        for (IMXMediaDownloadListener callback : mDownloadListeners) {
            try {
                callback.onDownloadProgress(mDownloadId, stats);
            } catch (Exception e) {
                Log.e(LOG_TAG, "dispatchOnDownloadProgress error " + e.getMessage(), e);
            }
        }
    }

    /**
     * Dispatch error message.
     *
     * @param jsonElement the Json error
     */
    private void dispatchOnDownloadError(JsonElement jsonElement) {
        for (IMXMediaDownloadListener callback : mDownloadListeners) {
            try {
                callback.onDownloadError(mDownloadId, jsonElement);
            } catch (Exception e) {
                Log.e(LOG_TAG, "dispatchOnDownloadError error " + e.getMessage(), e);
            }
        }
    }

    /**
     * Dispatch end of download
     */
    private void dispatchOnDownloadComplete() {
        for (IMXMediaDownloadListener callback : mDownloadListeners) {
            try {
                callback.onDownloadComplete(mDownloadId);
            } catch (Exception e) {
                Log.e(LOG_TAG, "dispatchOnDownloadComplete error " + e.getMessage(), e);
            }
        }
    }

    /**
     * Dispatch download cancel
     */
    private void dispatchDownloadCancel() {
        for (IMXMediaDownloadListener callback : mDownloadListeners) {
            try {
                callback.onDownloadCancel(mDownloadId);
            } catch (Exception e) {
                Log.e(LOG_TAG, "dispatchDownloadCancel error " + e.getMessage(), e);
            }
        }
    }
}
