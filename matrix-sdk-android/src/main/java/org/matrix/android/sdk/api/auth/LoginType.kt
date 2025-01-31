/*
 * Copyright 2025 New Vector Ltd.
 * Copyright 2022 The Matrix.org Foundation C.I.C.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package org.matrix.android.sdk.api.auth

enum class LoginType {
    PASSWORD,
    SSO,
    UNSUPPORTED,
    CUSTOM,
    DIRECT,
    UNKNOWN;

    companion object {

        fun fromName(name: String) = when (name) {
            PASSWORD.name -> PASSWORD
            SSO.name -> SSO
            UNSUPPORTED.name -> UNSUPPORTED
            CUSTOM.name -> CUSTOM
            DIRECT.name -> DIRECT
            else -> UNKNOWN
        }
    }
}
