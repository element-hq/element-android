/*
 * Copyright (c) 2022 New Vector Ltd
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
