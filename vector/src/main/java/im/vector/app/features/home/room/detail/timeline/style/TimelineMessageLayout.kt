/*
 * Copyright 2022-2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package im.vector.app.features.home.room.detail.timeline.style

import android.os.Parcelable
import im.vector.app.R
import kotlinx.parcelize.Parcelize

sealed interface TimelineMessageLayout : Parcelable {

    val layoutRes: Int
    val showAvatar: Boolean
    val showDisplayName: Boolean
    val showTimestamp: Boolean

    @Parcelize
    data class Default(
            override val showAvatar: Boolean,
            override val showDisplayName: Boolean,
            override val showTimestamp: Boolean,
            // Keep defaultLayout generated on epoxy items
            override val layoutRes: Int = 0,
    ) : TimelineMessageLayout

    @Parcelize
    data class Bubble(
            override val showAvatar: Boolean,
            override val showDisplayName: Boolean,
            override val showTimestamp: Boolean = true,
            val addTopMargin: Boolean = false,
            val isIncoming: Boolean,
            val isPseudoBubble: Boolean,
            val cornersRadius: CornersRadius,
            val timestampInsideMessage: Boolean,
            val addMessageOverlay: Boolean,
            override val layoutRes: Int = if (isIncoming) {
                R.layout.item_timeline_event_bubble_incoming_base
            } else {
                R.layout.item_timeline_event_bubble_outgoing_base
            },
    ) : TimelineMessageLayout {

        @Parcelize
        data class CornersRadius(
                val topStartRadius: Float,
                val topEndRadius: Float,
                val bottomStartRadius: Float,
                val bottomEndRadius: Float,
        ) : Parcelable
    }
}
