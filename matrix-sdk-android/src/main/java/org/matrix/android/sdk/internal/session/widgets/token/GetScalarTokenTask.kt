/*
 * Copyright 2025 New Vector Ltd.
 * Copyright 2020 The Matrix.org Foundation C.I.C.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package org.matrix.android.sdk.internal.session.widgets.token

import org.matrix.android.sdk.api.failure.Failure
import org.matrix.android.sdk.api.failure.MatrixError
import org.matrix.android.sdk.api.session.widgets.WidgetManagementFailure
import org.matrix.android.sdk.internal.network.executeRequest
import org.matrix.android.sdk.internal.session.openid.GetOpenIdTokenTask
import org.matrix.android.sdk.internal.session.widgets.WidgetsAPI
import org.matrix.android.sdk.internal.session.widgets.WidgetsAPIProvider
import org.matrix.android.sdk.internal.task.Task
import javax.inject.Inject
import javax.net.ssl.HttpsURLConnection

internal interface GetScalarTokenTask : Task<GetScalarTokenTask.Params, String> {

    data class Params(
            val serverUrl: String,
            val forceRefresh: Boolean = false
    )
}

private const val WIDGET_API_VERSION = "1.1"

internal class DefaultGetScalarTokenTask @Inject constructor(
        private val widgetsAPIProvider: WidgetsAPIProvider,
        private val scalarTokenStore: ScalarTokenStore,
        private val getOpenIdTokenTask: GetOpenIdTokenTask
) : GetScalarTokenTask {

    override suspend fun execute(params: GetScalarTokenTask.Params): String {
        val widgetsAPI = widgetsAPIProvider.get(params.serverUrl)
        return if (params.forceRefresh) {
            scalarTokenStore.clearToken(params.serverUrl)
            getNewScalarToken(widgetsAPI, params.serverUrl)
        } else {
            val scalarToken = scalarTokenStore.getToken(params.serverUrl)
            if (scalarToken == null) {
                getNewScalarToken(widgetsAPI, params.serverUrl)
            } else {
                validateToken(widgetsAPI, params.serverUrl, scalarToken)
            }
        }
    }

    private suspend fun getNewScalarToken(widgetsAPI: WidgetsAPI, serverUrl: String): String {
        val openId = getOpenIdTokenTask.execute(Unit)
        val registerWidgetResponse = executeRequest(null) {
            widgetsAPI.register(openId, WIDGET_API_VERSION)
        }
        if (registerWidgetResponse.scalarToken == null) {
            // Should not happen
            throw IllegalStateException("Scalar token is null")
        }
        scalarTokenStore.setToken(serverUrl, registerWidgetResponse.scalarToken)
        return validateToken(widgetsAPI, serverUrl, registerWidgetResponse.scalarToken)
    }

    private suspend fun validateToken(widgetsAPI: WidgetsAPI, serverUrl: String, scalarToken: String): String {
        return try {
            executeRequest(null) {
                widgetsAPI.validateToken(scalarToken, WIDGET_API_VERSION)
            }
            scalarToken
        } catch (failure: Throwable) {
            if (failure is Failure.ServerError && failure.httpCode == HttpsURLConnection.HTTP_FORBIDDEN) {
                if (failure.error.code == MatrixError.M_TERMS_NOT_SIGNED) {
                    throw WidgetManagementFailure.TermsNotSignedException(serverUrl, scalarToken)
                } else {
                    scalarTokenStore.clearToken(serverUrl)
                    getNewScalarToken(widgetsAPI, serverUrl)
                }
            } else {
                throw failure
            }
        }
    }
}
