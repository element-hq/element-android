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
