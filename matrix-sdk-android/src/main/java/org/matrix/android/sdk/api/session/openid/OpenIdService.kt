/*
 * Copyright 2025 New Vector Ltd.
 * Copyright 2021 The Matrix.org Foundation C.I.C.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package org.matrix.android.sdk.api.session.openid

interface OpenIdService {

    /**
     * Gets an OpenID token object that the requester may supply to another service to verify their identity in Matrix.
     * The generated token is only valid for exchanging for user information from the federation API for OpenID.
     */
    suspend fun getOpenIdToken(): OpenIdToken
}
