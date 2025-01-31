/*
 * Copyright 2025 New Vector Ltd.
 * Copyright 2021 The Matrix.org Foundation C.I.C.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package org.matrix.android.sdk.api.session.space

sealed class JoinSpaceResult {
    object Success : JoinSpaceResult()
    data class Fail(val error: Throwable) : JoinSpaceResult()

    /** Success fully joined the space, but failed to join all or some of it's rooms. */
    data class PartialSuccess(val failedRooms: Map<String, Throwable>) : JoinSpaceResult()

    fun isSuccess() = this is Success || this is PartialSuccess
}
