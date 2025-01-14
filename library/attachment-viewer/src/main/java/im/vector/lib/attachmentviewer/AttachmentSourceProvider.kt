/*
 * Copyright 2020-2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
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
