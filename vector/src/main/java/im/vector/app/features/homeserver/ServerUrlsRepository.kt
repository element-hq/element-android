/*
 * Copyright 2018 New Vector Ltd
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

package im.vector.app.features.homeserver

import android.content.SharedPreferences
import androidx.core.content.edit
import im.vector.app.R
import im.vector.app.core.di.DefaultPreferences
import im.vector.app.core.resources.StringProvider
import javax.inject.Inject

/**
 * Object to store and retrieve home and identity server urls.
 * Note: this class is not used.
 */
class ServerUrlsRepository @Inject constructor(
        @DefaultPreferences
        private val sharedPreferences: SharedPreferences,
        private val stringProvider: StringProvider,
) {
    companion object {
        // Keys used to store default servers urls from the referrer
        private const val DEFAULT_REFERRER_HOME_SERVER_URL_PREF = "default_referrer_home_server_url"
        private const val DEFAULT_REFERRER_IDENTITY_SERVER_URL_PREF = "default_referrer_identity_server_url"

        // Keys used to store current homeserver url and identity url
        const val HOME_SERVER_URL_PREF = "home_server_url"
        const val IDENTITY_SERVER_URL_PREF = "identity_server_url"
    }

    /**
     * Save home and identity sever urls received by the Referrer receiver.
     */
    fun setDefaultUrlsFromReferrer(homeServerUrl: String, identityServerUrl: String) {
        sharedPreferences
                .edit {
                    if (homeServerUrl.isNotEmpty()) {
                        putString(DEFAULT_REFERRER_HOME_SERVER_URL_PREF, homeServerUrl)
                    }

                    if (identityServerUrl.isNotEmpty()) {
                        putString(DEFAULT_REFERRER_IDENTITY_SERVER_URL_PREF, identityServerUrl)
                    }
                }
    }

    /**
     * Save home and identity sever urls entered by the user. May be custom or default value.
     */
    fun saveServerUrls(homeServerUrl: String, identityServerUrl: String) {
        sharedPreferences
                .edit {
                    putString(HOME_SERVER_URL_PREF, homeServerUrl)
                    putString(IDENTITY_SERVER_URL_PREF, identityServerUrl)
                }
    }

    /**
     * Return last used homeserver url, or the default one from referrer or the default one from resources.
     */
    fun getLastHomeServerUrl(): String {
        return sharedPreferences.getString(
                HOME_SERVER_URL_PREF,
                sharedPreferences.getString(
                        DEFAULT_REFERRER_HOME_SERVER_URL_PREF,
                        getDefaultHomeServerUrl()
                )!!
        )!!
    }

    /**
     * Return true if url is the default homeserver url form resources.
     */
    fun isDefaultHomeServerUrl(url: String) = url == getDefaultHomeServerUrl()

    /**
     * Return default homeserver url from resources.
     */
    private fun getDefaultHomeServerUrl() = stringProvider.getString(R.string.matrix_org_server_url)
}
