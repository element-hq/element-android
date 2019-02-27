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
import androidx.core.content.ContextCompat
import com.amulyakhare.textdrawable.TextDrawable
import com.bumptech.glide.request.RequestOptions
import im.vector.matrix.android.api.Matrix
import im.vector.matrix.android.api.MatrixPatterns
import im.vector.matrix.android.api.session.room.model.RoomMember
import im.vector.matrix.android.api.session.room.model.RoomSummary
import im.vector.riotredesign.R
import im.vector.riotredesign.core.glide.GlideApp
import im.vector.riotredesign.core.glide.GlideRequest
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch

object AvatarRenderer {

    fun render(roomMember: RoomMember, imageView: ImageView) {
        render(roomMember.avatarUrl, roomMember.displayName, imageView)
    }

    fun render(roomSummary: RoomSummary, imageView: ImageView) {
        render(roomSummary.avatarUrl, roomSummary.displayName, imageView)
    }

    fun render(avatarUrl: String?, name: String?, imageView: ImageView) {
        if (name.isNullOrEmpty()) {
            return
        }
        val placeholder = buildPlaceholderDrawable(imageView.context, name)
        buildGlideRequest(imageView.context, avatarUrl)
                .placeholder(placeholder)
                .into(imageView)
    }

    fun load(context: Context, avatarUrl: String?, name: String?, size: Int, callback: Callback) {
        if (name.isNullOrEmpty()) {
            return
        }
        val request = buildGlideRequest(context, avatarUrl)
        GlobalScope.launch {
            val placeholder = buildPlaceholderDrawable(context, name)
            callback.onDrawableUpdated(placeholder)
            try {
                val drawable = request.submit(size, size).get()
                callback.onDrawableUpdated(drawable)
            } catch (exception: Exception) {
                callback.onDrawableUpdated(placeholder)
            }
        }
    }

    private fun buildGlideRequest(context: Context, avatarUrl: String?): GlideRequest<Drawable> {
        val resolvedUrl = Matrix.getInstance().currentSession.contentUrlResolver().resolveFullSize(avatarUrl)
        return GlideApp
                .with(context)
                .load(resolvedUrl)
                .apply(RequestOptions.circleCropTransform())
    }

    private fun buildPlaceholderDrawable(context: Context, name: String): Drawable {
        val avatarColor = ContextCompat.getColor(context, R.color.pale_teal)
        val isNameUserId = MatrixPatterns.isUserId(name)
        val firstLetterIndex = if (isNameUserId) 1 else 0
        val firstLetter = name[firstLetterIndex].toString().toUpperCase()
        return TextDrawable.builder().buildRound(firstLetter, avatarColor)
    }

    interface Callback {
        fun onDrawableUpdated(drawable: Drawable?)
    }

}