/*
 * Copyright 2025 New Vector Ltd.
 * Copyright 2022 The Matrix.org Foundation C.I.C.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package org.matrix.android.sdk.api.session.uia

import org.matrix.android.sdk.api.auth.UIABaseAuth

data class DefaultBaseAuth(
        /**
         * This is a session identifier that the client must pass back to the homeserver,
         * if one is provided, in subsequent attempts to authenticate in the same API call.
         */
        override val session: String? = null

) : UIABaseAuth {
    override fun hasAuthInfo() = true

    override fun copyWithSession(session: String) = this.copy(session = session)

    override fun asMap(): Map<String, *> = mapOf("session" to session)
}
