/*
 * Copyright 2019-2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package im.vector.app.features.login

import android.net.Uri
import android.os.Parcelable
import kotlinx.parcelize.Parcelize

/**
 * Parameters extracted from a configuration url
 * Ex: https://mobile.element.io?hs_url=https%3A%2F%2Fexample.modular.im&is_url=https%3A%2F%2Fcustom.identity.org
 *
 * Note: On Element, identityServerUrl will never be used, so is declared private. Keep it for compatibility reason.
 */
@Parcelize
data class LoginConfig(
        val homeServerUrl: String?,
        private val identityServerUrl: String?
) : Parcelable {

    companion object {
        const val CONFIG_HS_PARAMETER = "hs_url"
        private const val CONFIG_IS_PARAMETER = "is_url"

        fun parse(from: Uri): LoginConfig {
            return LoginConfig(
                    homeServerUrl = from.getQueryParameter(CONFIG_HS_PARAMETER),
                    identityServerUrl = from.getQueryParameter(CONFIG_IS_PARAMETER)
            )
        }
    }
}
