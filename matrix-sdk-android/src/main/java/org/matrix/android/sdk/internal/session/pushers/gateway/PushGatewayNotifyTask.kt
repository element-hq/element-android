/*
 * Copyright 2025 New Vector Ltd.
 * Copyright 2020 The Matrix.org Foundation C.I.C.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */
package org.matrix.android.sdk.internal.session.pushers.gateway

import okhttp3.OkHttpClient
import org.matrix.android.sdk.api.session.pushers.PushGatewayFailure
import org.matrix.android.sdk.internal.di.Unauthenticated
import org.matrix.android.sdk.internal.network.NetworkConstants
import org.matrix.android.sdk.internal.network.RetrofitFactory
import org.matrix.android.sdk.internal.network.executeRequest
import org.matrix.android.sdk.internal.task.Task
import javax.inject.Inject

internal interface PushGatewayNotifyTask : Task<PushGatewayNotifyTask.Params, Unit> {
    data class Params(
            val url: String,
            val appId: String,
            val pushKey: String,
            val eventId: String
    )
}

internal class DefaultPushGatewayNotifyTask @Inject constructor(
        private val retrofitFactory: RetrofitFactory,
        @Unauthenticated private val unauthenticatedOkHttpClient: OkHttpClient
) : PushGatewayNotifyTask {

    override suspend fun execute(params: PushGatewayNotifyTask.Params) {
        val sygnalApi = retrofitFactory.create(
                unauthenticatedOkHttpClient,
                params.url.substringBefore(NetworkConstants.URI_PUSH_GATEWAY_PREFIX_PATH)
        )
                .create(PushGatewayAPI::class.java)

        val response = executeRequest(null) {
            sygnalApi.notify(
                    PushGatewayNotifyBody(
                            PushGatewayNotification(
                                    eventId = params.eventId,
                                    devices = listOf(
                                            PushGatewayDevice(
                                                    params.appId,
                                                    params.pushKey
                                            )
                                    )
                            )
                    )
            )
        }

        if (response.rejectedPushKeys.contains(params.pushKey)) {
            throw PushGatewayFailure.PusherRejected
        }
    }
}
