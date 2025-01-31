/*
 * Copyright 2022-2025 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package im.vector.app.features.home.room.list.home.header

import androidx.annotation.StringRes
import im.vector.app.R

enum class HomeRoomFilter(@StringRes val titleRes: Int) {
    ALL(R.string.room_list_filter_all),
    UNREADS(R.string.room_list_filter_unreads),
    FAVOURITES(R.string.room_list_filter_favourites),
    PEOPlE(R.string.room_list_filter_people),
}
