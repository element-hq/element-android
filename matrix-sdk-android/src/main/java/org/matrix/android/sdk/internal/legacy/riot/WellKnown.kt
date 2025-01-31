/*
 * Copyright 2025 New Vector Ltd.
 * Copyright 2020 The Matrix.org Foundation C.I.C.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */
package org.matrix.android.sdk.internal.legacy.riot

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

/**
 * <b>IMPORTANT:</b> This class is imported from Riot-Android to be able to perform a migration. Do not use it for any other purpose
 *
 * https://matrix.org/docs/spec/client_server/r0.4.0.html#server-discovery
 * <pre>
 * {
 *     "m.homeserver": {
 *         "base_url": "https://matrix.org"
 *     },
 *     "m.identity_server": {
 *         "base_url": "https://vector.im"
 *     }
 *     "m.integrations": {
 *          "managers": [
 *              {
 *                  "api_url": "https://integrations.example.org",
 *                  "ui_url": "https://integrations.example.org/ui"
 *              },
 *              {
 *                  "api_url": "https://bots.example.org"
 *              }
 *          ]
 *    }
 *     "im.vector.riot.jitsi": {
 *         "preferredDomain": "https://jitsi.riot.im/"
 *     }
 * }
 * </pre>
 */
@JsonClass(generateAdapter = true)
class WellKnown {

    @JvmField
    @Json(name = "m.homeserver")
    var homeServer: WellKnownBaseConfig? = null

    @JvmField
    @Json(name = "m.identity_server")
    var identityServer: WellKnownBaseConfig? = null

    @JvmField
    @Json(name = "m.integrations")
    var integrations: Map<String, *>? = null

    /**
     * Returns the list of integration managers proposed.
     */
    fun getIntegrationManagers(): List<WellKnownManagerConfig> {
        val managers = ArrayList<WellKnownManagerConfig>()
        integrations?.get("managers")?.let {
            (it as? ArrayList<*>)?.let { configs ->
                configs.forEach { config ->
                    (config as? Map<*, *>)?.let { map ->
                        val apiUrl = map["api_url"] as? String
                        val uiUrl = map["ui_url"] as? String ?: apiUrl
                        if (apiUrl != null &&
                                apiUrl.startsWith("https://") &&
                                uiUrl!!.startsWith("https://")) {
                            managers.add(
                                    WellKnownManagerConfig(
                                            apiUrl = apiUrl,
                                            uiUrl = uiUrl
                                    )
                            )
                        }
                    }
                }
            }
        }
        return managers
    }

    @JvmField
    @Json(name = "im.vector.riot.jitsi")
    var jitsiServer: WellKnownPreferredConfig? = null
}
