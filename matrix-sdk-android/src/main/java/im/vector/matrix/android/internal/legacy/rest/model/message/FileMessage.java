/* 
 * Copyright 2014 OpenMarket Ltd
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

import android.content.ClipDescription;
import android.text.TextUtils;

import im.vector.matrix.android.internal.legacy.crypto.MXEncryptedAttachments;
import im.vector.matrix.android.internal.legacy.rest.model.crypto.EncryptedFileInfo;
import im.vector.matrix.android.internal.legacy.util.Log;

import android.webkit.MimeTypeMap;

public class FileMessage extends MediaMessage {
    private static final String LOG_TAG = FileMessage.class.getSimpleName();

    public FileInfo info;
    public String url;

    // encrypted medias
    // url and thumbnailUrl are replaced by their dedicated file
    public EncryptedFileInfo file;

    public FileMessage() {
        msgtype = MSGTYPE_FILE;
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

    /**
     * Make a deep copy of this VideoMessage.
     *
     * @return the copy
     */
    public FileMessage deepCopy() {
        FileMessage copy = new FileMessage();
        copy.msgtype = msgtype;
        copy.body = body;
        copy.url = url;

        if (null != info) {
            copy.info = info.deepCopy();
        }

        if (null != file) {
            copy.file = file.deepCopy();
        }

        return copy;
    }

    @Override
    public String getMimeType() {
        if (null != info) {
            // the mimetype was not provided or it's invalid
            // some android application set the mimetype to text/uri-list
            // it should be fixed on application side but we need to patch it on client side.
            if ((TextUtils.isEmpty(info.mimetype) || ClipDescription.MIMETYPE_TEXT_URILIST.equals(info.mimetype)) && (body.indexOf('.') > 0)) {
                // the body should contain the filename so try to extract the mimetype from the extension
                String extension = body.substring(body.lastIndexOf('.') + 1, body.length());

                try {
                    info.mimetype = MimeTypeMap.getSingleton().getMimeTypeFromExtension(extension.toLowerCase());
                } catch (Exception e) {
                    Log.e(LOG_TAG, "## getMimeType() : getMimeTypeFromExtensionfailed " + e.getMessage(), e);
                }
            }

            if (TextUtils.isEmpty(info.mimetype)) {
                info.mimetype = "application/octet-stream";
            }

            return info.mimetype;
        } else {
            return null;
        }
    }
}
