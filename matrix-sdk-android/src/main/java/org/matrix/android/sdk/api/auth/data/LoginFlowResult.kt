/*
 * Copyright 2025 New Vector Ltd.
 * Copyright 2020 The Matrix.org Foundation C.I.C.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package org.matrix.android.sdk.api.auth.data

data class LoginFlowResult(
        val supportedLoginTypes: List<String>,
        val ssoIdentityProviders: List<SsoIdentityProvider>?,
        val isLoginAndRegistrationSupported: Boolean,
        val homeServerUrl: String,
        val isOutdatedHomeserver: Boolean,
        val isLogoutDevicesSupported: Boolean
)
