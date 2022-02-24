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
import androidx.constraintlayout.widget.ConstraintLayout
import dagger.hilt.android.AndroidEntryPoint
import im.vector.app.R
import im.vector.app.databinding.TypingMessageLayoutBinding
import im.vector.app.features.home.AvatarRenderer
import im.vector.app.features.home.room.typing.TypingHelper
import org.matrix.android.sdk.api.session.room.sender.SenderInfo
import javax.inject.Inject

@AndroidEntryPoint
class TypingMessageView @JvmOverloads constructor(
        context: Context,
        attrs: AttributeSet? = null,
        defStyleAttr: Int = 0) : ConstraintLayout(context, attrs, defStyleAttr) {

    val views: TypingMessageLayoutBinding

    @Inject
    lateinit var typingHelper: TypingHelper

    init {
        inflate(context, R.layout.typing_message_layout, this)
        views = TypingMessageLayoutBinding.bind(this)
    }

    fun render(typingUsers: List<SenderInfo>, avatarRenderer: AvatarRenderer) {
        views.usersName.text = typingHelper.getNotificationTypingMessage(typingUsers)
        views.avatars.render(typingUsers, avatarRenderer)
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        removeAllViews()
    }
}
