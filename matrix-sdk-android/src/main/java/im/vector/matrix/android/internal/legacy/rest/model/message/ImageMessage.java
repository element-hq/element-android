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

import android.media.ExifInterface;

import im.vector.matrix.android.internal.legacy.crypto.MXEncryptedAttachments;
import im.vector.matrix.android.internal.legacy.rest.model.crypto.EncryptedFileInfo;

public class ImageMessage extends MediaMessage {
    public ImageInfo info;
    public String url;

    // encrypted medias
    // url and thumbnailUrl are replaced by their dedicated file
    public EncryptedFileInfo file;

    public ImageMessage() {
        msgtype = MSGTYPE_IMAGE;
    }

    /**
     * Make a deep copy of this ImageMessage.
     * FIXME Remove this?
     * @return the copy
     */
    public ImageMessage deepCopy() {
        ImageMessage copy = new ImageMessage();
        copy.msgtype = msgtype;
        copy.body = body;
        copy.url = url;

        if (null != file) {
            copy.file = file.deepCopy();
        }

        return copy;
    }

    @Override
    public String getUrl() {
        if (null != url) {
            return url;
        } else if (null != file) {
            return file.url;
        } else {
            return null;
        }
    }

    @Override
    public void setUrl(MXEncryptedAttachments.EncryptionResult encryptionResult, String contentUrl) {
        if (null != encryptionResult) {
            file = encryptionResult.mEncryptedFileInfo;
            file.url = contentUrl;
            url = null;
        } else {
            url = contentUrl;
        }
    }

    @Override
    public String getThumbnailUrl() {
        if (null != info) {
            if (null != info.thumbnail_file) {
                return info.thumbnail_file.url;
            } else {
                return info.thumbnailUrl;
            }
        }
        return null;
    }

    @Override
    public void setThumbnailUrl(MXEncryptedAttachments.EncryptionResult encryptionResult, String url) {
        if (null != encryptionResult) {
            info.thumbnail_file = encryptionResult.mEncryptedFileInfo;
            info.thumbnail_file.url = url;
            info.thumbnailUrl = null;
        } else {
            info.thumbnailUrl = url;
        }
    }

    @Override
    public String getMimeType() {
        if (null != file) {
            return file.mimetype;
        } else if (null != info) {
            return info.mimetype;
        } else {
            return null;
        }
    }

    /**
     * @return the rotation angle. Integer.MAX_VALUE if not defined.
     */
    public int getRotation() {
        if ((null != info) && (null != info.rotation)) {
            return info.rotation;
        } else {
            return Integer.MAX_VALUE;
        }
    }

    /**
     * @return the image orientation. ExifInterface.ORIENTATION_UNDEFINED if not defined.
     */
    public int getOrientation() {
        if ((null != info) && (null != info.orientation)) {
            return info.orientation;
        } else {
            return ExifInterface.ORIENTATION_UNDEFINED;
        }
    }
}
