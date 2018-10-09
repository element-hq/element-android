/*
 * Copyright 2016 OpenMarket Ltd
 * Copyright 2017 Vector Creations Ltd
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

package im.vector.matrix.android.internal.legacy.rest.model;

import android.text.TextUtils;

import im.vector.matrix.android.internal.legacy.util.JsonUtils;

import java.util.Map;

/**
 * Class representing an URL preview.
 */
public class URLPreview implements java.io.Serializable {

    private static final String OG_DESCRIPTION = "og:description";
    private static final String OG_TITLE = "og:title";
    private static final String OG_TYPE = "og:type";

    private static final String OG_SITE_NAME = "og:site_name";
    private static final String OG_URL = "og:url";

    private static final String OG_IMAGE = "og:image";
    private static final String OG_IMAGE_SIZE = "matrix:image:size";
    private static final String OG_IMAGE_TYPE = "og:image:type";
    private static final String OG_IMAGE_WIDTH = "og:image:width";
    private static final String OG_IMAGE_HEIGHT = "og:image:height";

    /**
     * Global information
     */
    private final String mDescription;
    private final String mTitle;
    private final String mType;

    private final String mSiteName;
    private final String mRequestedURL;

    /**
     * Image information
     */
    private final String mThumbnailURL;
    private final String mThumbnailMimeType;

    private boolean mIsDismissed;

    /**
     * Constructor
     *
     * @param map the constructor parameters
     * @param url the original url, will be used if the map does not contain OG_URL field
     */
    public URLPreview(Map<String, Object> map, String url) {
        mDescription = JsonUtils.getAsString(map, OG_DESCRIPTION);
        mTitle = JsonUtils.getAsString(map, OG_TITLE);
        mType = JsonUtils.getAsString(map, OG_TYPE);

        mSiteName = JsonUtils.getAsString(map, OG_SITE_NAME);

        String requestedUrl = JsonUtils.getAsString(map, OG_URL);

        if (TextUtils.isEmpty(requestedUrl)) {
            // Fallback: use url
            mRequestedURL = url;
        } else {
            mRequestedURL = requestedUrl;
        }

        mThumbnailURL = JsonUtils.getAsString(map, OG_IMAGE);
        mThumbnailMimeType = JsonUtils.getAsString(map, OG_IMAGE_TYPE);
    }


    public String getDescription() {
        return mDescription;
    }

    public String getTitle() {
        return mTitle;
    }

    public String getType() {
        return mType;
    }

    public String getSiteName() {
        return mSiteName;
    }

    public String getRequestedURL() {
        return mRequestedURL;
    }

    public String getThumbnailURL() {
        return mThumbnailURL;
    }

    public String getThumbnailMimeType() {
        return mThumbnailMimeType;
    }

    public boolean IsDismissed() {
        return mIsDismissed;
    }

    public void setIsDismissed(boolean isDismissed) {
        mIsDismissed = isDismissed;
    }
}
