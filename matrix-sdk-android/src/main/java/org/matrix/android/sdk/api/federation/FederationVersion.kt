/*
 * Copyright 2025 New Vector Ltd.
 * Copyright 2021 The Matrix.org Foundation C.I.C.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package org.matrix.android.sdk.api.federation

/**
 * Ref: https://matrix.org/docs/spec/server_server/latest#get-matrix-federation-v1-version
 */
data class FederationVersion(
        /**
         * Arbitrary name that identify this implementation.
         */
        val name: String?,
        /**
         * Version of this implementation. The version format depends on the implementation.
         */
        val version: String?
)
