/*
 * Copyright 2019-2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package im.vector.app.features.home.room.detail.timeline.action

import android.os.Parcelable
import im.vector.app.features.home.room.detail.timeline.item.MessageInformationData
import kotlinx.parcelize.Parcelize

@Parcelize
data class TimelineEventFragmentArgs(
        val eventId: String,
        val roomId: String,
        val informationData: MessageInformationData,
        val isFromThreadTimeline: Boolean = false
) : Parcelable
