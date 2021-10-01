/*
 * Copyright 2020 The Matrix.org Foundation C.I.C.
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

package org.matrix.android.sdk.internal.session.integrationmanager

import org.matrix.android.sdk.api.auth.data.WellKnown
import org.matrix.android.sdk.internal.database.model.WellknownIntegrationManagerConfigEntity
import javax.inject.Inject

internal class IntegrationManagerConfigExtractor @Inject constructor() {

    fun extract(wellKnown: WellKnown): WellknownIntegrationManagerConfigEntity? {
        wellKnown.integrations?.get("managers")?.let {
            (it as? List<*>)?.let { configs ->
                configs.forEach { config ->
                    (config as? Map<*, *>)?.let { map ->
                        val apiUrl = map["api_url"] as? String
                        val uiUrl = map["ui_url"] as? String ?: apiUrl
                        if (apiUrl != null &&
                                apiUrl.startsWith("https://") &&
                                uiUrl!!.startsWith("https://")) {
                            return WellknownIntegrationManagerConfigEntity(
                                    apiUrl = apiUrl,
                                    uiUrl = uiUrl
                            )
                        }
                    }
                }
            }
        }
        return null
    }
}
