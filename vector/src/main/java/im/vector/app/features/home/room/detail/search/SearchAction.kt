/*
 * Copyright 2020-2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only
 * Please see LICENSE in the repository root for full details.
 */

package im.vector.app.features.home.room.detail.search

import im.vector.app.core.platform.VectorViewModelAction

sealed class SearchAction : VectorViewModelAction {
    data class SearchWith(val searchTerm: String) : SearchAction()
    object LoadMore : SearchAction()
    object Retry : SearchAction()
}
