/*
 * Copyright 2025 New Vector Ltd.
 * Copyright 2020 The Matrix.org Foundation C.I.C.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package org.matrix.android.sdk.api.auth.data

object LoginFlowTypes {
    const val PASSWORD = "m.login.password"
    const val OAUTH2 = "m.login.oauth2"
    const val EMAIL_CODE = "m.login.email.code"
    const val EMAIL_URL = "m.login.email.url"
    const val EMAIL_IDENTITY = "m.login.email.identity"
    const val MSISDN = "m.login.msisdn"
    const val RECAPTCHA = "m.login.recaptcha"
    const val DUMMY = "m.login.dummy"
    const val TERMS = "m.login.terms"
    const val TOKEN = "m.login.token"
    const val SSO = "m.login.sso"
}
