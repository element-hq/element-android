/*
 * Copyright 2019 New Vector Ltd
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package im.vector.matrix.android.api.session.room.model.message

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class ImageInfo(
        @Json(name = "mimetype") val mimeType: String,
        @Json(name = "w") val width: Int = 0,
        @Json(name = "h") val height: Int = 0,
        @Json(name = "size") val size: Int = 0,
        @Json(name = "rotation") val rotation: Int = 0,
        @Json(name = "orientation") val orientation: Int = 0,
        @Json(name = "thumbnail_info") val thumbnailInfo: ThumbnailInfo? = null,
        @Json(name = "thumbnail_url") val thumbnailUrl: String? = null
)