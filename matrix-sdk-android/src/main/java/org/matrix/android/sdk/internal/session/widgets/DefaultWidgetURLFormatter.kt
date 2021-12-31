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

package org.matrix.android.sdk.internal.session.widgets

import org.matrix.android.sdk.api.MatrixConfiguration
import org.matrix.android.sdk.api.session.Session
import org.matrix.android.sdk.api.session.SessionLifecycleObserver
import org.matrix.android.sdk.api.session.integrationmanager.IntegrationManagerConfig
import org.matrix.android.sdk.api.session.integrationmanager.IntegrationManagerService
import org.matrix.android.sdk.api.session.widgets.WidgetURLFormatter
import org.matrix.android.sdk.api.util.appendParamToUrl
import org.matrix.android.sdk.api.util.appendParamsToUrl
import org.matrix.android.sdk.internal.session.SessionScope
import org.matrix.android.sdk.internal.session.integrationmanager.IntegrationManager
import org.matrix.android.sdk.internal.session.widgets.token.GetScalarTokenTask
import javax.inject.Inject

@SessionScope
internal class DefaultWidgetURLFormatter @Inject constructor(private val integrationManager: IntegrationManager,
                                                             private val getScalarTokenTask: GetScalarTokenTask,
                                                             private val matrixConfiguration: MatrixConfiguration
) : IntegrationManagerService.Listener, WidgetURLFormatter, SessionLifecycleObserver {

    private lateinit var currentConfig: IntegrationManagerConfig
    private var whiteListedUrls: List<String> = emptyList()

    override fun onSessionStarted(session: Session) {
        setupWithConfiguration()
        integrationManager.addListener(this)
    }

    override fun onSessionStopped(session: Session) {
        integrationManager.removeListener(this)
    }

    override fun onConfigurationChanged(configs: List<IntegrationManagerConfig>) {
        setupWithConfiguration()
    }

    private fun setupWithConfiguration() {
        val preferredConfig = integrationManager.getPreferredConfig()
        if (!this::currentConfig.isInitialized || preferredConfig != currentConfig) {
            currentConfig = preferredConfig
            whiteListedUrls = if (matrixConfiguration.integrationWidgetUrls.isEmpty()) {
                listOf(preferredConfig.restUrl)
            } else {
                matrixConfiguration.integrationWidgetUrls
            }
        }
    }

    /**
     * Takes care of fetching a scalar token if required and build the final url.
     */
    override suspend fun format(baseUrl: String, params: Map<String, String>, forceFetchScalarToken: Boolean, bypassWhitelist: Boolean): String {
        return if (bypassWhitelist || isWhiteListed(baseUrl)) {
            val taskParams = GetScalarTokenTask.Params(currentConfig.restUrl, forceFetchScalarToken)
            val scalarToken = getScalarTokenTask.execute(taskParams)
            buildString {
                append(baseUrl)
                appendParamToUrl("scalar_token", scalarToken)
                appendParamsToUrl(params)
            }
        } else {
            buildString {
                append(baseUrl)
                appendParamsToUrl(params)
            }
        }
    }

    private fun isWhiteListed(url: String): Boolean {
        val allowed: List<String> = whiteListedUrls
        for (allowedUrl in allowed) {
            if (url.startsWith(allowedUrl)) {
                return true
            }
        }
        return false
    }
}
