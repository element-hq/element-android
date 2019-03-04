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

package im.vector.riotredesign.features.home

import android.content.Context
import android.graphics.drawable.Drawable
import android.widget.ImageView
import androidx.annotation.UiThread
import androidx.annotation.WorkerThread
import androidx.core.content.ContextCompat
import com.amulyakhare.textdrawable.TextDrawable
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.request.RequestOptions
import com.bumptech.glide.request.target.DrawableImageViewTarget
import com.bumptech.glide.request.target.Target
import im.vector.matrix.android.api.Matrix
import im.vector.matrix.android.api.MatrixPatterns
import im.vector.matrix.android.api.session.content.ContentUrlResolver
import im.vector.matrix.android.api.session.room.model.RoomMember
import im.vector.matrix.android.api.session.room.model.RoomSummary
import im.vector.riotredesign.R
import im.vector.riotredesign.core.glide.GlideApp
import im.vector.riotredesign.core.glide.GlideRequest
import im.vector.riotredesign.core.glide.GlideRequests

/**
 * This helper centralise ways to retrieve avatar into ImageView or even generic Target<Drawable>
 */
object AvatarRenderer {

    @UiThread
    fun render(roomMember: RoomMember, imageView: ImageView) {
        render(roomMember.avatarUrl, roomMember.displayName, imageView)
    }

    @UiThread
    fun render(roomSummary: RoomSummary, imageView: ImageView) {
        render(roomSummary.avatarUrl, roomSummary.displayName, imageView)
    }

    @UiThread
    fun render(avatarUrl: String?, name: String?, imageView: ImageView) {
        render(imageView.context, GlideApp.with(imageView), avatarUrl, name, imageView.height, DrawableImageViewTarget(imageView))
    }

    @UiThread
    fun render(context: Context,
               glideRequest: GlideRequests,
               avatarUrl: String?,
               name: String?,
               size: Int,
               target: Target<Drawable>) {
        if (name.isNullOrEmpty()) {
            return
        }
        val placeholder = buildPlaceholderDrawable(context, name)
        buildGlideRequest(glideRequest, avatarUrl, size)
                .placeholder(placeholder)
                .into(target)
    }

    @WorkerThread
    fun getCachedOrPlaceholder(context: Context,
                               glideRequest: GlideRequests,
                               avatarUrl: String?,
                               text: String,
                               size: Int): Drawable {
        val future = buildGlideRequest(glideRequest, avatarUrl, size).onlyRetrieveFromCache(true).submit()
        return try {
            future.get()
        } catch (exception: Exception) {
            buildPlaceholderDrawable(context, text)
        }
    }

    // PRIVATE API *********************************************************************************

    private fun buildGlideRequest(glideRequest: GlideRequests, avatarUrl: String?, size: Int): GlideRequest<Drawable> {
        val resolvedUrl = Matrix.getInstance().currentSession
                .contentUrlResolver()
                .resolveThumbnail(avatarUrl, size, size, ContentUrlResolver.ThumbnailMethod.SCALE)

        return glideRequest
                .load(resolvedUrl)
                .apply(RequestOptions.circleCropTransform())
                .diskCacheStrategy(DiskCacheStrategy.DATA)
    }

    private fun buildPlaceholderDrawable(context: Context, text: String): Drawable {
        val avatarColor = ContextCompat.getColor(context, R.color.pale_teal)
        return if (text.isEmpty()) {
            TextDrawable.builder().buildRound("", avatarColor)
        } else {
            val isUserId = MatrixPatterns.isUserId(text)
            val firstLetterIndex = if (isUserId) 1 else 0
            val firstLetter = text[firstLetterIndex].toString().toUpperCase()
            TextDrawable.builder().buildRound(firstLetter, avatarColor)
        }

    }

}