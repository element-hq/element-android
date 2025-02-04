/*
 * Copyright 2021-2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package im.vector.app.features.spaces.preview

import im.vector.app.core.platform.VectorViewEvents

sealed class SpacePreviewViewEvents : VectorViewEvents {
    object Dismiss : SpacePreviewViewEvents()
    object JoinSuccess : SpacePreviewViewEvents()
    data class JoinFailure(val message: String?) : SpacePreviewViewEvents()
}
