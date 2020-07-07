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

package im.vector.riotx.attachmentviewer

import android.content.Context
import android.view.View

sealed class AttachmentInfo {
    data class Image(val url: String, val data: Any?) : AttachmentInfo()
    data class AnimatedImage(val url: String, val data: Any?) : AttachmentInfo()
    data class Video(val url: String, val data: Any) : AttachmentInfo()
    data class Audio(val url: String, val data: Any) : AttachmentInfo()
    data class File(val url: String, val data: Any) : AttachmentInfo()

    fun bind() {
    }
}

interface AttachmentSourceProvider {

    fun getItemCount(): Int

    fun getAttachmentInfoAt(position: Int): AttachmentInfo

    fun loadImage(holder: ZoomableImageViewHolder, info: AttachmentInfo.Image)

    fun loadImage(holder: AnimatedImageViewHolder, info: AttachmentInfo.AnimatedImage)

    fun overlayViewAtPosition(context: Context, position: Int) : View?
}
