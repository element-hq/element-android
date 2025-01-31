/*
 * Copyright 2025 New Vector Ltd.
 * Copyright 2020 The Matrix.org Foundation C.I.C.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package org.matrix.android.sdk.internal.session.identity

import dagger.Lazy
import okhttp3.OkHttpClient
import org.matrix.android.sdk.api.session.identity.IdentityServiceError
import org.matrix.android.sdk.internal.di.UnauthenticatedWithCertificate
import org.matrix.android.sdk.internal.network.RetrofitFactory
import org.matrix.android.sdk.internal.session.identity.data.IdentityStore
import org.matrix.android.sdk.internal.session.openid.GetOpenIdTokenTask
import org.matrix.android.sdk.internal.task.Task
import javax.inject.Inject

internal interface EnsureIdentityTokenTask : Task<Unit, Unit>

internal class DefaultEnsureIdentityTokenTask @Inject constructor(
        private val identityStore: IdentityStore,
        private val retrofitFactory: RetrofitFactory,
        @UnauthenticatedWithCertificate
        private val unauthenticatedOkHttpClient: Lazy<OkHttpClient>,
        private val getOpenIdTokenTask: GetOpenIdTokenTask,
        private val identityRegisterTask: IdentityRegisterTask
) : EnsureIdentityTokenTask {

    override suspend fun execute(params: Unit) {
        val identityData = identityStore.getIdentityData() ?: throw IdentityServiceError.NoIdentityServerConfigured
        val url = identityData.identityServerUrl ?: throw IdentityServiceError.NoIdentityServerConfigured

        if (identityData.token == null) {
            // Try to get a token
            val token = getNewIdentityServerToken(url)
            identityStore.setToken(token)
        }
    }

    private suspend fun getNewIdentityServerToken(url: String): String {
        val api = retrofitFactory.create(unauthenticatedOkHttpClient, url).create(IdentityAuthAPI::class.java)

        val openIdToken = getOpenIdTokenTask.execute(Unit)
        val token = identityRegisterTask.execute(IdentityRegisterTask.Params(api, openIdToken))

        return token.token
    }
}
