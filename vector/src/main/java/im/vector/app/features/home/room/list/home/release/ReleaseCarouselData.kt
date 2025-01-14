/*
 * Copyright 2022-2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package im.vector.app.features.home.room.list.home.release

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes

class ReleaseCarouselData(
        val items: List<Item>
) {
    data class Item(
            @StringRes val title: Int,
            @StringRes val body: Int,
            @DrawableRes val image: Int,
    )
}
