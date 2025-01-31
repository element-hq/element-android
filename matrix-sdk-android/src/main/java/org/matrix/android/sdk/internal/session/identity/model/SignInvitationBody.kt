/*
 * Copyright 2025 New Vector Ltd.
 * Copyright 2021 The Matrix.org Foundation C.I.C.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package org.matrix.android.sdk.internal.session.identity.model

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
internal data class SignInvitationBody(
        /** The Matrix user ID of the user accepting the invitation.*/
        val mxid: String,
        /** The token from the call to store- invite..*/
        val token: String,
        /** The private key, encoded as Unpadded base64. */
        @Json(name = "private_key")
        val privateKey: String
)
