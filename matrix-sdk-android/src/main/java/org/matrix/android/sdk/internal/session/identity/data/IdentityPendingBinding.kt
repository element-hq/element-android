/*
 * Copyright 2025 New Vector Ltd.
 * Copyright 2020 The Matrix.org Foundation C.I.C.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package org.matrix.android.sdk.internal.session.identity.data

internal data class IdentityPendingBinding(
        /* Managed by Riot */
        val clientSecret: String,
        /* Managed by Riot */
        val sendAttempt: Int,
        /* Provided by the identity server */
        val sid: String
)
