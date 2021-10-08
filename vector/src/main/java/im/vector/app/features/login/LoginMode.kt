/*
 * Copyright 2019 New Vector Ltd
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package im.vector.app.features.login

import android.os.Parcelable
import kotlinx.parcelize.Parcelize
import org.matrix.android.sdk.api.auth.data.SsoIdentityProvider

sealed class LoginMode : Parcelable
/** because persist state */ {
    @Parcelize object Unknown : LoginMode()
    @Parcelize object Password : LoginMode()
    @Parcelize data class Sso(val ssoIdentityProviders: List<SsoIdentityProvider>?) : LoginMode()
    @Parcelize data class SsoAndPassword(val ssoIdentityProviders: List<SsoIdentityProvider>?) : LoginMode()
    @Parcelize object Unsupported : LoginMode()
}

fun LoginMode.ssoIdentityProviders(): List<SsoIdentityProvider>? {
    return when (this) {
        is LoginMode.Sso            -> ssoIdentityProviders
        is LoginMode.SsoAndPassword -> ssoIdentityProviders
        else                        -> null
    }
}

fun LoginMode.hasSso(): Boolean {
    return when (this) {
        is LoginMode.Sso            -> true
        is LoginMode.SsoAndPassword -> true
        else                        -> false
    }
}
