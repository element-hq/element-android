/*
 * Copyright 2025 New Vector Ltd.
 * Copyright 2021 The Matrix.org Foundation C.I.C.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package org.matrix.android.sdk.internal.session.identity

import dagger.Lazy
import okhttp3.OkHttpClient
import org.matrix.android.sdk.api.session.identity.model.SignInvitationResult
import org.matrix.android.sdk.internal.di.Unauthenticated
import org.matrix.android.sdk.internal.di.UserId
import org.matrix.android.sdk.internal.network.RetrofitFactory
import org.matrix.android.sdk.internal.task.Task
import javax.inject.Inject

internal interface Sign3pidInvitationTask : Task<Sign3pidInvitationTask.Params, SignInvitationResult> {
    data class Params(
            val token: String,
            val url: String,
            val privateKey: String
    )
}

internal class DefaultSign3pidInvitationTask @Inject constructor(
        @Unauthenticated
        private val okHttpClient: Lazy<OkHttpClient>,
        private val retrofitFactory: RetrofitFactory,
        @UserId private val userId: String
) : Sign3pidInvitationTask {

    override suspend fun execute(params: Sign3pidInvitationTask.Params): SignInvitationResult {
        val identityAPI = retrofitFactory
                .create(okHttpClient, "https://${params.url}")
                .create(IdentityAPI::class.java)
        return identityAPI.signInvitationDetails(params.token, params.privateKey, userId)
    }
}
