/*
 * Copyright 2019-2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package im.vector.app.features.home.room.detail.timeline.helper

import im.vector.app.core.utils.DimensionConverter
import im.vector.app.features.home.room.detail.timeline.style.TimelineLayoutSettings
import im.vector.app.features.home.room.detail.timeline.style.TimelineLayoutSettingsProvider
import javax.inject.Inject

class AvatarSizeProvider @Inject constructor(
        private val dimensionConverter: DimensionConverter,
        private val layoutSettingsProvider: TimelineLayoutSettingsProvider
) {

    private val avatarStyle by lazy {
        when (layoutSettingsProvider.getLayoutSettings()) {
            TimelineLayoutSettings.MODERN -> AvatarStyle.SMALL
            TimelineLayoutSettings.BUBBLE -> AvatarStyle.BUBBLE
        }
    }

    val leftGuideline: Int by lazy {
        dimensionConverter.dpToPx(avatarStyle.avatarSizeDP + avatarStyle.marginDP)
    }

    val avatarSize: Int by lazy {
        dimensionConverter.dpToPx(avatarStyle.avatarSizeDP)
    }

    companion object {

        enum class AvatarStyle(val avatarSizeDP: Int, val marginDP: Int) {
            BIG(50, 8),
            MEDIUM(40, 8),
            SMALL(30, 8),
            BUBBLE(28, 4),
            NONE(0, 8)
        }
    }
}
