/*
 * Copyright 2021-2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package im.vector.app.features.home.room.detail.arguments

import android.os.Parcelable
import im.vector.app.features.home.room.threads.arguments.ThreadTimelineArgs
import im.vector.app.features.share.SharedData
import kotlinx.parcelize.Parcelize

@Parcelize
data class TimelineArgs(
        val roomId: String,
        val eventId: String? = null,
        val sharedData: SharedData? = null,
        val openShareSpaceForId: String? = null,
        val threadTimelineArgs: ThreadTimelineArgs? = null,
        val switchToParentSpace: Boolean = false,
        val isInviteAlreadyAccepted: Boolean = false
) : Parcelable
