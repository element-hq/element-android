/*
 * Copyright 2022-2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package im.vector.app.features.location.live

sealed class LiveLocationMessageBannerViewState(
        open val bottomStartCornerRadiusInDp: Float,
        open val bottomEndCornerRadiusInDp: Float,
) {

    data class Emitter(
            override val bottomStartCornerRadiusInDp: Float,
            override val bottomEndCornerRadiusInDp: Float,
            val remainingTimeInMillis: Long,
            val isStopButtonCenteredVertically: Boolean
    ) : LiveLocationMessageBannerViewState(bottomStartCornerRadiusInDp, bottomEndCornerRadiusInDp)

    data class Watcher(
            override val bottomStartCornerRadiusInDp: Float,
            override val bottomEndCornerRadiusInDp: Float,
            val formattedLocalTimeOfEndOfLive: String,
    ) : LiveLocationMessageBannerViewState(bottomStartCornerRadiusInDp, bottomEndCornerRadiusInDp)
}
