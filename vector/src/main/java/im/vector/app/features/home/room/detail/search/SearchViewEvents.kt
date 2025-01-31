/*
 * Copyright 2020-2025 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package im.vector.app.features.home.room.detail.search

import im.vector.app.core.platform.VectorViewEvents

sealed class SearchViewEvents : VectorViewEvents {
    data class Failure(val throwable: Throwable) : SearchViewEvents()
}
