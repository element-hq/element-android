/*
 * Copyright 2025 New Vector Ltd.
 * Copyright 2020 The Matrix.org Foundation C.I.C.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package org.matrix.android.sdk.api.auth

interface UIABaseAuth {
    /**
     * This is a session identifier that the client must pass back to the homeserver,
     * if one is provided, in subsequent attempts to authenticate in the same API call.
     */
    val session: String?

    fun hasAuthInfo(): Boolean

    fun copyWithSession(session: String): UIABaseAuth

    fun asMap(): Map<String, *>
}
