/*
 * Copyright 2021-2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package im.vector.app.features.onboarding.ftueauth

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import im.vector.lib.core.utils.epoxy.charsequence.EpoxyCharSequence

data class SplashCarouselState(
        val items: List<Item>
) {
    data class Item(
            val title: EpoxyCharSequence,
            @StringRes val body: Int,
            @DrawableRes val image: Int,
            @DrawableRes val pageBackground: Int
    )
}
