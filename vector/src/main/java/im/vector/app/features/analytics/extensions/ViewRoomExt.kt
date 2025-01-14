/*
 * Copyright 2022-2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package im.vector.app.features.analytics.extensions

import im.vector.app.features.analytics.plan.ViewRoom
import org.matrix.android.sdk.api.extensions.orFalse
import org.matrix.android.sdk.api.session.room.model.RoomSummary
import org.matrix.android.sdk.api.session.room.model.RoomType

fun RoomSummary?.toAnalyticsViewRoom(trigger: ViewRoom.Trigger?, selectedSpace: RoomSummary? = null, viaKeyboard: Boolean? = null): ViewRoom {
    val activeSpace = selectedSpace?.toActiveSpace() ?: ViewRoom.ActiveSpace.Home

    return ViewRoom(
            isDM = this?.isDirect.orFalse(),
            isSpace = this?.roomType == RoomType.SPACE,
            trigger = trigger,
            activeSpace = activeSpace,
            viaKeyboard = viaKeyboard
    )
}

private fun RoomSummary.toActiveSpace(): ViewRoom.ActiveSpace {
    return if (isPublic) ViewRoom.ActiveSpace.Public else ViewRoom.ActiveSpace.Private
}
