/*
 * Copyright 2019-2025 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package im.vector.app.features.home

import androidx.annotation.StringRes
import im.vector.app.R

enum class RoomListDisplayMode(@StringRes val titleRes: Int) {
    NOTIFICATIONS(R.string.bottom_action_notification),
    PEOPLE(R.string.bottom_action_people_x),
    ROOMS(R.string.bottom_action_rooms),
    FILTERED(/* Not used */ 0)
}
