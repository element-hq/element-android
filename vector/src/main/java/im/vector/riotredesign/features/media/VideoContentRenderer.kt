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

package im.vector.riotredesign.features.media

import android.os.Parcelable
import android.widget.ImageView
import android.widget.VideoView
import im.vector.matrix.android.api.Matrix
import kotlinx.android.parcel.Parcelize

object VideoContentRenderer {

    @Parcelize
    data class Data(
            val filename: String,
            val videoUrl: String?,
            val thumbnailMediaData: ImageContentRenderer.Data
    ) : Parcelable

    fun render(data: Data, thumbnailView: ImageView, videoView: VideoView) {
        val contentUrlResolver = Matrix.getInstance(videoView.context).currentSession!!.contentUrlResolver()
        val resolvedUrl = contentUrlResolver.resolveFullSize(data.videoUrl)
        videoView.setVideoPath(resolvedUrl)
        videoView.start()
    }

}