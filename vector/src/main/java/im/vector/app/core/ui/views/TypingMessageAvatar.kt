/*
 * Copyright 2022-2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package im.vector.app.core.ui.views

import android.content.Context
import android.util.AttributeSet
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import im.vector.app.features.home.AvatarRenderer
import org.matrix.android.sdk.api.session.room.sender.SenderInfo
import org.matrix.android.sdk.api.util.toMatrixItem

class TypingMessageAvatar @JvmOverloads constructor(
        context: Context,
        attrs: AttributeSet? = null,
        defStyleAttr: Int = 0
) : LinearLayout(context, attrs, defStyleAttr) {

    companion object {
        const val OVERLAP_FACT0R = -3 // =~ 30% to left
    }

    private val typingAvatarSize by lazy(LazyThreadSafetyMode.NONE) {
        context.resources.getDimension(im.vector.lib.ui.styles.R.dimen.typing_avatar_size).toInt()
    }

    fun render(typingUsers: List<SenderInfo>, avatarRenderer: AvatarRenderer) {
        removeAllViews()
        for ((index, value) in typingUsers.withIndex()) {
            val avatar = ImageView(context)
            avatar.id = View.generateViewId()
            val layoutParams = MarginLayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT)
            if (index != 0) layoutParams.marginStart = typingAvatarSize / OVERLAP_FACT0R
            layoutParams.width = typingAvatarSize
            layoutParams.height = typingAvatarSize
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
