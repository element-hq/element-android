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

import im.vector.matrix.android.internal.legacy.rest.model.crypto.EncryptedFileInfo;

public class ImageInfo {
    public String mimetype;
    public Integer w;
    public Integer h;
    public Long size;
    public Integer rotation;

    // ExifInterface.ORIENTATION_XX values
    public Integer orientation;

    public ThumbnailInfo thumbnailInfo;
    public String thumbnailUrl;
    public EncryptedFileInfo thumbnail_file;

    /**
     * Make a deep copy.
     *
     * @return the copy
     */
    public ImageInfo deepCopy() {
        ImageInfo copy = new ImageInfo();
        copy.mimetype = mimetype;
        copy.w = w;
        copy.h = h;
        copy.size = size;
        copy.rotation = rotation;
        copy.orientation = orientation;

        if (null != thumbnail_file) {
            copy.thumbnail_file = thumbnail_file.deepCopy();
        }

        copy.thumbnailUrl = thumbnailUrl;

        if (null != thumbnailInfo) {
            copy.thumbnailInfo = thumbnailInfo.deepCopy();
        }

        return copy;
    }
}



