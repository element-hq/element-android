/*
 * Copyright 2025 New Vector Ltd.
 * Copyright 2020 The Matrix.org Foundation C.I.C.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package org.matrix.android.sdk.api.session.file

interface ContentDownloadStateTracker {
    fun track(key: String, updateListener: UpdateListener)
    fun unTrack(key: String, updateListener: UpdateListener)
    fun clear()

    sealed class State {
        object Idle : State()
        data class Downloading(val current: Long, val total: Long, val indeterminate: Boolean) : State()
        object Decrypting : State()
        object Success : State()
        data class Failure(val errorCode: Int) : State()
    }

    interface UpdateListener {
        fun onDownloadStateUpdate(state: State)
    }
}
