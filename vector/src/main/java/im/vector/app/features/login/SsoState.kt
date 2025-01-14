/*
 * Copyright 2022-2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package im.vector.app.features.login

import android.os.Parcelable
import kotlinx.parcelize.Parcelize
import org.matrix.android.sdk.api.auth.data.SsoIdentityProvider

sealed interface SsoState : Parcelable {
    @Parcelize
    data class IdentityProviders(val providers: List<SsoIdentityProvider>) : SsoState

    @Parcelize
    object Fallback : SsoState

    fun isFallback() = this == Fallback

    fun providersOrNull() = when (this) {
        Fallback -> null
        is IdentityProviders -> providers.takeIf { it.isNotEmpty() }
    }
}

fun List<SsoIdentityProvider>?.toSsoState() = this
        ?.takeIf { it.isNotEmpty() }
        ?.let { SsoState.IdentityProviders(it) }
        ?: SsoState.Fallback
