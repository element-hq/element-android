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
