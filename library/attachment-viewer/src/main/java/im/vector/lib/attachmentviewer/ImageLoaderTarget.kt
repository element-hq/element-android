/*
 * Copyright 2020-2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package im.vector.lib.attachmentviewer

import android.graphics.drawable.Animatable
import android.graphics.drawable.Drawable
import android.widget.ImageView
import android.widget.LinearLayout
import androidx.core.view.isVisible
import androidx.core.view.updateLayoutParams

interface ImageLoaderTarget {

    fun contextView(): ImageView

    fun onResourceLoading(uid: String, placeholder: Drawable?)

    fun onLoadFailed(uid: String, errorDrawable: Drawable?)

    fun onResourceCleared(uid: String, placeholder: Drawable?)

    fun onResourceReady(uid: String, resource: Drawable)
}

internal class DefaultImageLoaderTarget(val holder: AnimatedImageViewHolder, private val contextView: ImageView) :
        ImageLoaderTarget {
    override fun contextView(): ImageView {
        return contextView
    }

    override fun onResourceLoading(uid: String, placeholder: Drawable?) {
        if (holder.boundResourceUid != uid) return
        holder.views.imageLoaderProgress.isVisible = true
    }

    override fun onLoadFailed(uid: String, errorDrawable: Drawable?) {
        if (holder.boundResourceUid != uid) return
        holder.views.imageLoaderProgress.isVisible = false
        holder.views.imageView.setImageDrawable(errorDrawable)
    }

    override fun onResourceCleared(uid: String, placeholder: Drawable?) {
        if (holder.boundResourceUid != uid) return
        holder.views.imageView.setImageDrawable(placeholder)
    }

    override fun onResourceReady(uid: String, resource: Drawable) {
        if (holder.boundResourceUid != uid) return
        holder.views.imageLoaderProgress.isVisible = false
        // Glide mess up the view size :/
        holder.views.imageView.updateLayoutParams {
            width = LinearLayout.LayoutParams.MATCH_PARENT
            height = LinearLayout.LayoutParams.MATCH_PARENT
        }
        holder.views.imageView.setImageDrawable(resource)
        if (resource is Animatable) {
            resource.start()
        }
    }

    internal class ZoomableImageTarget(val holder: ZoomableImageViewHolder, private val contextView: ImageView) : ImageLoaderTarget {
        override fun contextView() = contextView

        override fun onResourceLoading(uid: String, placeholder: Drawable?) {
            if (holder.boundResourceUid != uid) return
            holder.views.imageLoaderProgress.isVisible = true
            holder.views.touchImageView.setImageDrawable(placeholder)
        }

        override fun onLoadFailed(uid: String, errorDrawable: Drawable?) {
            if (holder.boundResourceUid != uid) return
            holder.views.imageLoaderProgress.isVisible = false
            holder.views.touchImageView.setImageDrawable(errorDrawable)
        }

        override fun onResourceCleared(uid: String, placeholder: Drawable?) {
            if (holder.boundResourceUid != uid) return
            holder.views.touchImageView.setImageDrawable(placeholder)
        }

        override fun onResourceReady(uid: String, resource: Drawable) {
            if (holder.boundResourceUid != uid) return
            holder.views.imageLoaderProgress.isVisible = false
            // Glide mess up the view size :/
            holder.views.touchImageView.updateLayoutParams {
                width = LinearLayout.LayoutParams.MATCH_PARENT
                height = LinearLayout.LayoutParams.MATCH_PARENT
            }
            holder.views.touchImageView.setImageDrawable(resource)
        }
    }
}
