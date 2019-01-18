/*
 *
 *  * Copyright 2019 New Vector Ltd
 *  *
 *  * Licensed under the Apache License, Version 2.0 (the "License");
 *  * you may not use this file except in compliance with the License.
 *  * You may obtain a copy of the License at
 *  *
 *  *     http://www.apache.org/licenses/LICENSE-2.0
 *  *
 *  * Unless required by applicable law or agreed to in writing, software
 *  * distributed under the License is distributed on an "AS IS" BASIS,
 *  * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  * See the License for the specific language governing permissions and
 *  * limitations under the License.
 *
 */

package im.vector.riotredesign.features.home

import androidx.core.content.ContextCompat
import android.widget.ImageView
import com.amulyakhare.textdrawable.TextDrawable
import com.bumptech.glide.request.RequestOptions
import im.vector.matrix.android.api.session.room.model.RoomMember
import im.vector.matrix.android.api.session.room.model.RoomSummary
import im.vector.riotredesign.R
import im.vector.riotredesign.core.extensions.firstCharAsString
import im.vector.riotredesign.core.glide.GlideApp

private const val MEDIA_URL = "https://matrix.org/_matrix/media/v1/download/"
private const val MXC_PREFIX = "mxc://"

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
        val resolvedUrl = avatarUrl?.replace(MXC_PREFIX, MEDIA_URL)
        val avatarColor = ContextCompat.getColor(imageView.context, R.color.pale_teal)
        val fallbackDrawable = TextDrawable.builder().buildRound(name.firstCharAsString().toUpperCase(), avatarColor)

        GlideApp
                .with(imageView)
                .load(resolvedUrl)
                .placeholder(fallbackDrawable)
                .apply(RequestOptions.circleCropTransform())
                .into(imageView)
    }


}