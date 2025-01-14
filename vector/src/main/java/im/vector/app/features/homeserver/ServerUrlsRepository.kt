/*
 * Copyright 2018-2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package im.vector.app.features.homeserver

import android.content.SharedPreferences
import androidx.core.content.edit
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
    private fun getDefaultHomeServerUrl() = stringProvider.getString(im.vector.app.config.R.string.matrix_org_server_url)
}
