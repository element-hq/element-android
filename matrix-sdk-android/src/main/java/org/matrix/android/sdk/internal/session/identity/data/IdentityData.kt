/*
 * Copyright 2025 New Vector Ltd.
 * Copyright 2020 The Matrix.org Foundation C.I.C.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package org.matrix.android.sdk.internal.session.identity.data

internal data class IdentityData(
        val identityServerUrl: String?,
        val token: String?,
        val hashLookupPepper: String?,
        val hashLookupAlgorithm: List<String>,
        val userConsent: Boolean
)
