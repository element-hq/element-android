/* 
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
package im.vector.matrix.android.internal.legacy.rest.model.message;

import android.net.Uri;

import im.vector.matrix.android.internal.legacy.util.Log;

import java.io.File;

public  class LocationMessage extends Message {
    private static final String LOG_TAG = "LocationMessage";
    public ThumbnailInfo thumbnail_info;
    public String geo_uri;
    public String thumbnail_url;

    public LocationMessage() {
        msgtype = MSGTYPE_LOCATION;
    }

    /**
     * Make a deep copy
     * @return the copy
     */
    public LocationMessage deepCopy() {
        LocationMessage copy = new LocationMessage();
        copy.msgtype = msgtype;
        copy.body = body;
        copy.geo_uri = geo_uri;
        copy.thumbnail_url = thumbnail_url;

        if (null != thumbnail_info) {
            copy.thumbnail_info = thumbnail_info.deepCopy();
        }

        return copy;
    }

    public boolean isLocalThumbnailContent() {
        return (null != thumbnail_url) && (thumbnail_url.startsWith("file://"));
    }

    /**
     * Checks if the media Urls are still valid.
     * The media Urls could define a file path.
     * They could have been deleted after a media cache cleaning.
     */
    public void checkMediaUrls() {
        if ((thumbnail_url != null) && thumbnail_url.startsWith("file://")) {
            try {
                File file = new File(Uri.parse(thumbnail_url).getPath());

                if (!file.exists()) {
                    thumbnail_url = null;
                }
            } catch (Exception e) {
                Log.e(LOG_TAG, "## checkMediaUrls() failed " + e.getMessage(), e);
            }
        }
    }
}