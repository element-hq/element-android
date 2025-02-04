/*
 * Copyright 2022-2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package im.vector.app.features.home.room.detail.timeline.item

import androidx.core.view.isInvisible
import androidx.core.view.isVisible
import com.airbnb.epoxy.EpoxyAttribute
import com.airbnb.epoxy.EpoxyModelClass
import com.airbnb.epoxy.EpoxyModelWithHolder
import im.vector.app.R
import im.vector.app.core.epoxy.VectorEpoxyHolder
import im.vector.app.core.ui.views.TypingMessageView
import im.vector.app.features.home.AvatarRenderer
import org.matrix.android.sdk.api.session.room.sender.SenderInfo

@EpoxyModelClass
abstract class TypingItem : EpoxyModelWithHolder<TypingItem.TypingHolder>() {

    companion object {
        private const val MAX_TYPING_MESSAGE_USERS_COUNT = 4
    }

    @EpoxyAttribute(EpoxyAttribute.Option.DoNotHash)
    lateinit var avatarRenderer: AvatarRenderer

    @EpoxyAttribute
    var users: List<SenderInfo> = emptyList()

    override fun getDefaultLayout(): Int = R.layout.item_typing_users

    override fun bind(holder: TypingHolder) {
        super.bind(holder)

        val typingUsers = users.take(MAX_TYPING_MESSAGE_USERS_COUNT)
        holder.typingView.apply {
            animate().cancel()
            val duration = 100L
            if (typingUsers.isEmpty()) {
                animate().translationY(height.toFloat())
                        .alpha(0f)
                        .setDuration(duration)
                        .withEndAction {
                            isInvisible = true
                        }.start()
            } else {
                isVisible = true

                translationY = height.toFloat()
                alpha = 0f
                render(typingUsers, avatarRenderer)
                animate().translationY(0f)
                        .alpha(1f)
                        .setDuration(duration)
                        .start()
            }
        }
    }

    class TypingHolder : VectorEpoxyHolder() {
        val typingView by bind<TypingMessageView>(R.id.typingMessageView)
    }
}
