/*
 * Copyright 2022-2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package im.vector.app.features.onboarding

import android.os.Parcelable
import im.vector.app.features.onboarding.AuthenticationDescription.AuthenticationType
import kotlinx.parcelize.Parcelize
import org.matrix.android.sdk.api.auth.data.SsoIdentityProvider

sealed interface AuthenticationDescription : Parcelable {
    @Parcelize
    object Login : AuthenticationDescription

    @Parcelize
    data class Register(val type: AuthenticationType) : AuthenticationDescription

    enum class AuthenticationType {
        Password,
        Apple,
        Facebook,
        GitHub,
        GitLab,
        Google,
        SSO,
        Other
    }
}

fun SsoIdentityProvider?.toAuthenticationType() = when (this?.brand) {
    SsoIdentityProvider.BRAND_GOOGLE -> AuthenticationType.Google
    SsoIdentityProvider.BRAND_GITHUB -> AuthenticationType.GitHub
    SsoIdentityProvider.BRAND_APPLE -> AuthenticationType.Apple
    SsoIdentityProvider.BRAND_FACEBOOK -> AuthenticationType.Facebook
    SsoIdentityProvider.BRAND_GITLAB -> AuthenticationType.GitLab
    SsoIdentityProvider.BRAND_TWITTER -> AuthenticationType.SSO
    null -> AuthenticationType.SSO
    else -> AuthenticationType.SSO
}
