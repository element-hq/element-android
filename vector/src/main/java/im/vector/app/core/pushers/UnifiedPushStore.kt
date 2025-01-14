/*
 * Copyright 2022-2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package im.vector.app.core.pushers

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit
import im.vector.app.core.di.DefaultPreferences
import javax.inject.Inject

class UnifiedPushStore @Inject constructor(
        val context: Context,
        val fcmHelper: FcmHelper,
        @DefaultPreferences
        private val defaultPrefs: SharedPreferences,
) {
    /**
     * Retrieves the UnifiedPush Endpoint.
     *
     * @return the UnifiedPush Endpoint or null if not received
     */
    fun getEndpoint(): String? {
        return defaultPrefs.getString(PREFS_ENDPOINT_OR_TOKEN, null)
    }

    /**
     * Store UnifiedPush Endpoint to the SharedPrefs.
     *
     * @param endpoint the endpoint to store
     */
    fun storeUpEndpoint(endpoint: String?) {
        defaultPrefs.edit {
            putString(PREFS_ENDPOINT_OR_TOKEN, endpoint)
        }
    }

    /**
     * Retrieves the Push Gateway.
     *
     * @return the Push Gateway or null if not defined
     */
    fun getPushGateway(): String? {
        return defaultPrefs.getString(PREFS_PUSH_GATEWAY, null)
    }

    /**
     * Store Push Gateway to the SharedPrefs.
     *
     * @param gateway the push gateway to store
     */
    fun storePushGateway(gateway: String?) {
        defaultPrefs.edit {
            putString(PREFS_PUSH_GATEWAY, gateway)
        }
    }

    companion object {
        private const val PREFS_ENDPOINT_OR_TOKEN = "UP_ENDPOINT_OR_TOKEN"
        private const val PREFS_PUSH_GATEWAY = "PUSH_GATEWAY"
    }
}
