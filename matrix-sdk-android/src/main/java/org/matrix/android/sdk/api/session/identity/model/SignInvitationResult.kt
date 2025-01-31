/*
 * Copyright 2025 New Vector Ltd.
 * Copyright 2022 The Matrix.org Foundation C.I.C.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package org.matrix.android.sdk.api.session.identity.model

import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class SignInvitationResult(
        /**
         * The Matrix user ID of the user accepting the invitation.
         */
        val mxid: String,
        /**
         * The Matrix user ID of the user who sent the invitation.
         */
        val sender: String,
        /**
         * The token from the call to store- invite..
         */
        val signatures: Map<String, *>,
        /**
         * The token for the invitation.
         */
        val token: String
)
