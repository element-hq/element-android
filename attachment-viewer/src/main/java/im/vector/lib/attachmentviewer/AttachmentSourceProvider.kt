/*
 * Copyright (c) 2020 New Vector Ltd
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

package im.vector.lib.attachmentviewer

import android.content.Context
import android.view.View

sealed class AttachmentInfo(open val uid: String) {
    data class Image(override val uid: String, val url: String, val data: Any?) : AttachmentInfo(uid)
    data class AnimatedImage(override val uid: String, val url: String, val data: Any?) : AttachmentInfo(uid)
    data class Video(override val uid: String, val url: String, val data: Any, val thumbnail: Image?) : AttachmentInfo(uid)
//    data class Audio(override val uid: String, val url: String, val data: Any) : AttachmentInfo(uid)
//    data class File(override val uid: String, val url: String, val data: Any) : AttachmentInfo(uid)
}

interface AttachmentSourceProvider {

    fun getItemCount(): Int

    fun getAttachmentInfoAt(position: Int): AttachmentInfo

    fun loadImage(target: ImageLoaderTarget, info: AttachmentInfo.Image)

    fun loadImage(target: ImageLoaderTarget, info: AttachmentInfo.AnimatedImage)

    fun loadVideo(target: VideoLoaderTarget, info: AttachmentInfo.Video)

    fun overlayViewAtPosition(context: Context, position: Int): View?

    fun clear(id: String)
}
