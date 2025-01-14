/*
 * Copyright 2022-2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package im.vector.app.features.location.option

enum class LocationSharingOption {
    /**
     * Current user's location.
     */
    USER_CURRENT,

    /**
     * User's location during a certain amount of time.
     */
    USER_LIVE,

    /**
     * Static location pinned by the user.
     */
    PINNED
}
