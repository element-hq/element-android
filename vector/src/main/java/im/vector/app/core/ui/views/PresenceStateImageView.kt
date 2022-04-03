/*
 * Copyright (c) 2021 New Vector Ltd
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
import androidx.appcompat.widget.AppCompatImageView
import androidx.core.view.isVisible
import im.vector.app.R
import org.matrix.android.sdk.api.session.presence.model.PresenceEnum
import org.matrix.android.sdk.api.session.presence.model.UserPresence

/**
 * Custom ImageView to dynamically render Presence state in multiple screens
 */
class PresenceStateImageView @JvmOverloads constructor(
        context: Context,
        attrs: AttributeSet? = null,
        defStyleAttr: Int = 0
) : AppCompatImageView(context, attrs, defStyleAttr) {

    fun render(showPresence: Boolean = true, userPresence: UserPresence?) {
        isVisible = showPresence && userPresence != null

        when (userPresence?.presence) {
            PresenceEnum.ONLINE      -> {
                setImageResource(R.drawable.ic_presence_online)
                contentDescription = context.getString(R.string.a11y_presence_online)
            }
            PresenceEnum.UNAVAILABLE -> {
                setImageResource(R.drawable.ic_presence_offline)
                contentDescription = context.getString(R.string.a11y_presence_unavailable)
            }
            PresenceEnum.OFFLINE     -> {
                setImageResource(R.drawable.ic_presence_offline)
                contentDescription = context.getString(R.string.a11y_presence_offline)
            }
            null                     -> Unit
        }
    }
}
