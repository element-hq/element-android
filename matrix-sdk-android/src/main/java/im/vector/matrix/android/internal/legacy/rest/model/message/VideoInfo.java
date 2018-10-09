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

public class VideoInfo {
    public Integer h;
    public Integer w;
    public String mimetype;
    public Long duration;
    public Long size;
    public String thumbnail_url;
    public ThumbnailInfo thumbnail_info;

    public EncryptedFileInfo thumbnail_file;

    /**
     * Make a deep copy.
     *
     * @return the copy
     */
    public VideoInfo deepCopy() {
        VideoInfo copy = new VideoInfo();
        copy.h = h;
        copy.w = w;
        copy.mimetype = mimetype;
        copy.duration = duration;
        copy.thumbnail_url = thumbnail_url;

        if (null != thumbnail_info) {
            copy.thumbnail_info = thumbnail_info.deepCopy();
        }

        if (null != thumbnail_file) {
            copy.thumbnail_file = thumbnail_file.deepCopy();
        }

        return copy;
    }
}
