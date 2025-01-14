/*
 * Copyright 2022-2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package im.vector.app.features.location.live.map

import im.vector.app.core.platform.VectorViewModelAction

sealed class LiveLocationMapAction : VectorViewModelAction {
    data class AddMapSymbol(val key: String, val value: Long) : LiveLocationMapAction()
    data class RemoveMapSymbol(val key: String) : LiveLocationMapAction()
    object StopSharing : LiveLocationMapAction()
    object ShowMapLoadingError : LiveLocationMapAction()
    object ZoomToUserLocation : LiveLocationMapAction()
}
