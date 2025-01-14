/*
 * Copyright 2021-2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package im.vector.app.core.ui.views

import android.content.Context
import android.util.AttributeSet
import androidx.appcompat.widget.AppCompatImageView
import androidx.core.view.isVisible
import im.vector.app.R
import im.vector.lib.strings.CommonStrings
import org.matrix.android.sdk.api.session.presence.model.PresenceEnum
import org.matrix.android.sdk.api.session.presence.model.UserPresence

/**
 * Custom ImageView to dynamically render Presence state in multiple screens.
 */
class PresenceStateImageView @JvmOverloads constructor(
        context: Context,
        attrs: AttributeSet? = null,
        defStyleAttr: Int = 0
) : AppCompatImageView(context, attrs, defStyleAttr) {

    fun render(showPresence: Boolean = true, userPresence: UserPresence?) {
        isVisible = showPresence && userPresence != null

        when (userPresence?.presence) {
            PresenceEnum.ONLINE -> {
                setImageResource(R.drawable.ic_presence_online)
                contentDescription = context.getString(CommonStrings.a11y_presence_online)
            }
            PresenceEnum.UNAVAILABLE -> {
                setImageResource(R.drawable.ic_presence_away)
                contentDescription = context.getString(CommonStrings.a11y_presence_unavailable)
            }
            PresenceEnum.OFFLINE -> {
                setImageResource(R.drawable.ic_presence_offline)
                contentDescription = context.getString(CommonStrings.a11y_presence_offline)
            }
            PresenceEnum.BUSY -> {
                setImageResource(R.drawable.ic_presence_busy)
                contentDescription = context.getString(CommonStrings.a11y_presence_busy)
            }
            null -> Unit
        }
    }
}
