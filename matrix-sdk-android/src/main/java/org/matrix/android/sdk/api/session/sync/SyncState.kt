/*
 * Copyright 2025 New Vector Ltd.
 * Copyright 2020 The Matrix.org Foundation C.I.C.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package org.matrix.android.sdk.api.session.sync

sealed class SyncState {
    object Idle : SyncState()
    data class Running(val afterPause: Boolean) : SyncState()
    object Paused : SyncState()
    object Killing : SyncState()
    object Killed : SyncState()
    object NoNetwork : SyncState()
    object InvalidToken : SyncState()
}
