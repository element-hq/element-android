/*
 * Copyright 2020-2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package im.vector.lib.attachmentviewer

import android.graphics.drawable.Drawable
import android.widget.ImageView
import androidx.core.view.isVisible
import java.io.File

interface VideoLoaderTarget {
    fun contextView(): ImageView

    fun onThumbnailResourceLoading(uid: String, placeholder: Drawable?)

    fun onThumbnailLoadFailed(uid: String, errorDrawable: Drawable?)

    fun onThumbnailResourceCleared(uid: String, placeholder: Drawable?)

    fun onThumbnailResourceReady(uid: String, resource: Drawable)

    fun onVideoFileLoading(uid: String)
    fun onVideoFileLoadFailed(uid: String)
    fun onVideoFileReady(uid: String, file: File)
    fun onVideoURLReady(uid: String, path: String)
}

internal class DefaultVideoLoaderTarget(val holder: VideoViewHolder, private val contextView: ImageView) : VideoLoaderTarget {
    override fun contextView(): ImageView = contextView

    override fun onThumbnailResourceLoading(uid: String, placeholder: Drawable?) {
    }

    override fun onThumbnailLoadFailed(uid: String, errorDrawable: Drawable?) {
    }

    override fun onThumbnailResourceCleared(uid: String, placeholder: Drawable?) {
        if (holder.boundResourceUid != uid) return
        holder.views.videoThumbnailImage.setImageDrawable(placeholder)
    }

    override fun onThumbnailResourceReady(uid: String, resource: Drawable) {
        if (holder.boundResourceUid != uid) return
        holder.views.videoThumbnailImage.setImageDrawable(resource)
    }

    override fun onVideoFileLoading(uid: String) {
        if (holder.boundResourceUid != uid) return
        holder.views.videoThumbnailImage.isVisible = true
        holder.views.videoLoaderProgress.isVisible = true
        holder.views.videoView.isVisible = false
    }

    override fun onVideoFileLoadFailed(uid: String) {
        if (holder.boundResourceUid != uid) return
        holder.videoFileLoadError()
    }

    override fun onVideoFileReady(uid: String, file: File) {
        if (holder.boundResourceUid != uid) return
        arrangeForVideoReady()
        holder.videoReady(file)
    }

    override fun onVideoURLReady(uid: String, path: String) {
        if (holder.boundResourceUid != uid) return
        arrangeForVideoReady()
        holder.videoReady(path)
    }

    private fun arrangeForVideoReady() {
        holder.views.videoThumbnailImage.isVisible = false
        holder.views.videoLoaderProgress.isVisible = false
        holder.views.videoView.isVisible = true
    }
}
