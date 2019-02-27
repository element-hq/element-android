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

package im.vector.riotredesign.features.html

import android.content.Context
import android.graphics.drawable.Drawable
import com.google.android.material.chip.ChipDrawable
import im.vector.matrix.android.api.session.user.model.User
import im.vector.riotredesign.R
import im.vector.riotredesign.features.home.AvatarRenderer
import java.lang.ref.WeakReference

object PillDrawableFactory {

    fun create(context: Context, userId: String, user: User?): Drawable {
        val textPadding = context.resources.getDimension(R.dimen.pill_text_padding)

        val chipDrawable = ChipDrawable.createFromResource(context, R.xml.pill_view).apply {
            setText(user?.displayName ?: userId)
            textEndPadding = textPadding
            textStartPadding = textPadding
            setChipMinHeightResource(R.dimen.pill_min_height)
            setChipIconSizeResource(R.dimen.pill_avatar_size)
            setBounds(0, 0, intrinsicWidth, intrinsicHeight)
        }
        val avatarRendererCallback = AvatarRendererChipCallback(chipDrawable)
        AvatarRenderer.load(context, user?.avatarUrl, user?.displayName, 80, avatarRendererCallback)
        return chipDrawable
    }

    private class AvatarRendererChipCallback(chipDrawable: ChipDrawable) : AvatarRenderer.Callback {

        private val weakChipDrawable = WeakReference<ChipDrawable>(chipDrawable)

        override fun onDrawableUpdated(drawable: Drawable?) {
            weakChipDrawable.get()?.apply {
                chipIcon = drawable
                setBounds(0, 0, intrinsicWidth, intrinsicHeight)
            }
        }

    }

}

