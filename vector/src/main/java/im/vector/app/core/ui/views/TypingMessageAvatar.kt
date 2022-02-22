/*
 * Copyright (c) 2022 New Vector Ltd
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

package im.vector.app.core.ui.views

import android.content.Context
import android.util.AttributeSet
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import im.vector.app.core.utils.DimensionConverter
import im.vector.app.features.home.AvatarRenderer
import org.matrix.android.sdk.api.session.room.sender.SenderInfo
import org.matrix.android.sdk.api.util.toMatrixItem

class TypingMessageAvatar @JvmOverloads constructor(
        context: Context,
        attrs: AttributeSet? = null,
        defStyleAttr: Int = 0
) : LinearLayout(context, attrs, defStyleAttr) {

    companion object {
        const val AVATAR_SIZE_DP = 20
        const val OVERLAP_FACT0R = -3 // =~ 30% to left
    }

    fun render(typingUsers: List<SenderInfo>, avatarRenderer: AvatarRenderer) {
        removeAllViews()
        for ((index, value) in typingUsers.withIndex()) {
            val avatar = ImageView(context)
            avatar.id = View.generateViewId()
            val layoutParams = MarginLayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT)
            if (index != 0) layoutParams.marginStart = DimensionConverter(resources).dpToPx(AVATAR_SIZE_DP / OVERLAP_FACT0R)
            layoutParams.width = DimensionConverter(resources).dpToPx(AVATAR_SIZE_DP)
            layoutParams.height = DimensionConverter(resources).dpToPx(AVATAR_SIZE_DP)
            avatar.layoutParams = layoutParams
            avatarRenderer.render(value.toMatrixItem(), avatar)
            addView(avatar)
        }
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        removeAllViews()
    }
}
