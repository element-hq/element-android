/*
 * Copyright 2023, 2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package im.vector.app.features.home

import com.airbnb.mvrx.MavericksState
import im.vector.app.features.home.room.list.UnreadCounterBadgeView

data class NewHomeDetailViewState(
        val spacesNotificationCounterBadgeState: UnreadCounterBadgeView.State = UnreadCounterBadgeView.State.Count(count = 0, highlighted = false),
) : MavericksState
