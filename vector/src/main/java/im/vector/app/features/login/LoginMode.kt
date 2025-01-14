/*
 * Copyright 2019-2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package im.vector.app.features.login

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

sealed class LoginMode : Parcelable { // Parcelable because persist state

    @Parcelize object Unknown : LoginMode()
    @Parcelize object Password : LoginMode()
    @Parcelize data class Sso(val ssoState: SsoState, val hasOidcCompatibilityFlow: Boolean) : LoginMode()
    @Parcelize data class SsoAndPassword(val ssoState: SsoState, val hasOidcCompatibilityFlow: Boolean) : LoginMode()
    @Parcelize object Unsupported : LoginMode()
}

fun LoginMode.ssoState(): SsoState {
    return when (this) {
        is LoginMode.Sso -> ssoState
        is LoginMode.SsoAndPassword -> ssoState
        else -> SsoState.Fallback
    }
}

fun LoginMode.hasSso(): Boolean {
    return when (this) {
        is LoginMode.Sso -> true
        is LoginMode.SsoAndPassword -> true
        else -> false
    }
}
