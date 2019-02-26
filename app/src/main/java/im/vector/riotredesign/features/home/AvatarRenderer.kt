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
import com.bumptech.glide.request.target.SimpleTarget
import com.bumptech.glide.request.transition.Transition
import im.vector.matrix.android.api.Matrix
import im.vector.matrix.android.api.MatrixPatterns
import im.vector.matrix.android.api.session.room.model.RoomMember
import im.vector.matrix.android.api.session.room.model.RoomSummary
import im.vector.riotredesign.R
import im.vector.riotredesign.core.glide.GlideApp
import im.vector.riotredesign.core.glide.GlideRequest

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
        buildGlideRequest(imageView.context, name, avatarUrl).into(imageView)
    }

    fun load(context: Context, avatarUrl: String?, name: String?, callback: Callback) {
        if (name.isNullOrEmpty()) {
            return
        }
        buildGlideRequest(context, name, avatarUrl).into(CallbackTarget(callback))
    }

    private fun buildGlideRequest(context: Context, name: String, avatarUrl: String?): GlideRequest<Drawable> {
        val resolvedUrl = Matrix.getInstance().currentSession.contentUrlResolver().resolveFullSize(avatarUrl)
        val avatarColor = ContextCompat.getColor(context, R.color.pale_teal)
        val isNameUserId = MatrixPatterns.isUserId(name)
        val firstLetterIndex = if (isNameUserId) 1 else 0
        val firstLetter = name[firstLetterIndex].toString().toUpperCase()
        val fallbackDrawable = TextDrawable.builder().buildRound(firstLetter, avatarColor)
        return GlideApp
                .with(context)
                .load(resolvedUrl)
                .placeholder(fallbackDrawable)
                .apply(RequestOptions.circleCropTransform())
    }

    interface Callback {
        fun onDrawableUpdated(drawable: Drawable?)
        fun onDestroy()
    }

    private class CallbackTarget(private val callback: Callback) : SimpleTarget<Drawable>() {

        override fun onDestroy() {
            callback.onDestroy()
        }

        override fun onResourceReady(resource: Drawable, transition: Transition<in Drawable>?) {
            callback.onDrawableUpdated(resource)
        }

        override fun onLoadStarted(placeholder: Drawable?) {
            callback.onDrawableUpdated(placeholder)
        }

        override fun onLoadFailed(errorDrawable: Drawable?) {
            callback.onDrawableUpdated(errorDrawable)
        }
    }


}