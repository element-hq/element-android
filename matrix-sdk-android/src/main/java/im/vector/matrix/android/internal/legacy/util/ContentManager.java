/*
 * Copyright 2016 OpenMarket Ltd
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

package im.vector.matrix.android.internal.legacy.util;

import android.support.annotation.Nullable;

import im.vector.matrix.android.internal.legacy.HomeServerConnectionConfig;
import im.vector.matrix.android.internal.legacy.RestClient;

/**
 * Class for accessing content from the current session.
 */
public class ContentManager {
    private static final String LOG_TAG = ContentManager.class.getSimpleName();

    public static final String MATRIX_CONTENT_URI_SCHEME = "mxc://";

    public static final String METHOD_CROP = "crop";
    public static final String METHOD_SCALE = "scale";

    public static final String URI_PREFIX_CONTENT_API = "/_matrix/media/v1/";

    public static final String MATRIX_CONTENT_IDENTICON_PREFIX = "identicon/";

    // HS config
    private final HomeServerConnectionConfig mHsConfig;

    // the unsent events Manager
    private final UnsentEventsManager mUnsentEventsManager;

    // AV scanner handling
    private boolean mIsAvScannerEnabled;
    private String mDownloadUrlPrefix;

    /**
     * Default constructor.
     *
     * @param hsConfig            the HomeserverConnectionConfig to use
     * @param unsentEventsManager the unsent events manager
     */
    public ContentManager(HomeServerConnectionConfig hsConfig, UnsentEventsManager unsentEventsManager) {
        mHsConfig = hsConfig;
        mUnsentEventsManager = unsentEventsManager;
        // The AV scanner is disabled by default
        configureAntiVirusScanner(false);
    }

    /**
     * Configure the anti-virus scanner.
     * If the anti-virus server url is different than the home server url,
     * it must be provided in HomeServerConnectionConfig.
     * The home server url is considered by default.
     *
     * @param isEnabled true to enable the anti-virus scanner, false otherwise.
     */
    public void configureAntiVirusScanner(boolean isEnabled) {
        mIsAvScannerEnabled = isEnabled;
        if (isEnabled) {
            mDownloadUrlPrefix = mHsConfig.getAntiVirusServerUri().toString() + "/" + RestClient.URI_API_PREFIX_PATH_MEDIA_PROXY_UNSTABLE;
        } else {
            mDownloadUrlPrefix = mHsConfig.getHomeserverUri().toString() + URI_PREFIX_CONTENT_API;
        }
    }

    public boolean isAvScannerEnabled() {
        return mIsAvScannerEnabled;
    }

    /**
     * @return the hs config.
     */
    public HomeServerConnectionConfig getHsConfig() {
        return mHsConfig;
    }

    /**
     * @return the unsent events manager
     */
    public UnsentEventsManager getUnsentEventsManager() {
        return mUnsentEventsManager;
    }

    /**
     * Compute the identicon URL for an userId.
     *
     * @param userId the user id.
     * @return the url
     */
    public static String getIdenticonURL(String userId) {
        // sanity check
        if (null != userId) {
            String urlEncodedUser = null;
            try {
                urlEncodedUser = java.net.URLEncoder.encode(userId, "UTF-8");
            } catch (Exception e) {
                Log.e(LOG_TAG, "## getIdenticonURL() : java.net.URLEncoder.encode failed " + e.getMessage(), e);
            }

            return ContentManager.MATRIX_CONTENT_URI_SCHEME + MATRIX_CONTENT_IDENTICON_PREFIX + urlEncodedUser;
        }

        return null;
    }

    /**
     * Check whether an url is a valid matrix content url.
     *
     * @param contentUrl the content URL (in the form of "mxc://...").
     * @return true if contentUrl is valid.
     */
    public static boolean isValidMatrixContentUrl(String contentUrl) {
        return (null != contentUrl && contentUrl.startsWith(MATRIX_CONTENT_URI_SCHEME));
    }

    /**
     * Returns the task identifier used to download the content at a Matrix media content URI
     * (in the form of "mxc://...").
     *
     * @param contentUrl the matrix content url.
     * @return the task identifier, or null if the url is invalid..
     */
    @Nullable
    public String downloadTaskIdForMatrixMediaContent(String contentUrl) {
        if (isValidMatrixContentUrl(contentUrl)) {
            // We extract the server name and the media id from the matrix content url
            // to define a unique download task id
            return contentUrl.substring(MATRIX_CONTENT_URI_SCHEME.length());
        }

        // do not allow non-mxc content URLs: we should not be making requests out to whatever
        // http urls people send us
        return null;
    }

    /**
     * Get the actual URL for accessing the full-size image of a Matrix media content URI.
     *
     * @param contentUrl the Matrix media content URI (in the form of "mxc://...").
     * @return the URL to access the described resource, or null if the url is invalid.
     * @deprecated See getDownloadableUrl(contentUrl, isEncrypted).
     */
    @Nullable
    public String getDownloadableUrl(String contentUrl) {
        // Suppose here by default that the content is not encrypted.
        // FIXME this method should be removed as soon as possible
        return getDownloadableUrl(contentUrl, false);
    }

    /**
     * Get the actual URL for accessing the full-size image of a Matrix media content URI.
     *
     * @param contentUrl  the Matrix media content URI (in the form of "mxc://...").
     * @param isEncrypted tell whether the related content is encrypted (This information is
     *                    required when the anti-virus scanner is enabled).
     * @return the URL to access the described resource, or null if the url is invalid.
     */
    @Nullable
    public String getDownloadableUrl(String contentUrl, boolean isEncrypted) {
        if (isValidMatrixContentUrl(contentUrl)) {
            if (!isEncrypted || !mIsAvScannerEnabled) {
                String mediaServerAndId = contentUrl.substring(MATRIX_CONTENT_URI_SCHEME.length());
                return mDownloadUrlPrefix + "download/" + mediaServerAndId;
            } else {
                // In case of encrypted content, a unique url is used when the scanner is enabled
                // The encryption info must be sent in the body of the request.
                return mDownloadUrlPrefix + "download_encrypted";
            }
        }

        // do not allow non-mxc content URLs
        return null;
    }

    /**
     * Get the actual URL for accessing the thumbnail image of a given Matrix media content URI.
     *
     * @param contentUrl the Matrix media content URI (in the form of "mxc://...").
     * @param width      the desired width
     * @param height     the desired height
     * @param method     the desired scale method (METHOD_CROP or METHOD_SCALE)
     * @return the URL to access the described resource, or null if the url is invalid.
     */
    @Nullable
    public String getDownloadableThumbnailUrl(String contentUrl, int width, int height, String method) {
        if (isValidMatrixContentUrl(contentUrl)) {
            String mediaServerAndId = contentUrl.substring(MATRIX_CONTENT_URI_SCHEME.length());

            // ignore the #auto pattern
            if (mediaServerAndId.endsWith("#auto")) {
                mediaServerAndId = mediaServerAndId.substring(0, mediaServerAndId.length() - "#auto".length());
            }

            // Build the thumbnail url.
            String url;
            // Caution: identicon has no thumbnail path.
            if (mediaServerAndId.startsWith(MATRIX_CONTENT_IDENTICON_PREFIX)) {
                // identicon url still go to the media repo since they don’t need virus scanning
                url = mHsConfig.getHomeserverUri().toString() + URI_PREFIX_CONTENT_API;
            } else {
                // Use the current download url prefix to take into account a potential antivirus scanner
                url = mDownloadUrlPrefix + "thumbnail/";
            }

            url += mediaServerAndId;
            url += "?width=" + width;
            url += "&height=" + height;
            url += "&method=" + method;
            return url;
        }

        // do not allow non-mxc content URLs
        return null;
    }
}
