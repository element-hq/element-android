/*
 * Copyright (c) 2020 New Vector Ltd
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
package org.matrix.android.sdk.internal.session.pushers.sygnal

import okhttp3.OkHttpClient
import org.matrix.android.sdk.api.session.pushers.SygnalFailure
import org.matrix.android.sdk.internal.di.Unauthenticated
import org.matrix.android.sdk.internal.network.NetworkConstants
import org.matrix.android.sdk.internal.network.RetrofitFactory
import org.matrix.android.sdk.internal.network.executeRequest
import org.matrix.android.sdk.internal.task.Task
import javax.inject.Inject

internal interface SygnalNotifyTask : Task<SygnalNotifyTask.Params, Unit> {
    data class Params(
            val url: String,
            val appId: String,
            val pushKey: String,
            val eventId: String
    )
}

internal class DefaultSygnalNotifyTask @Inject constructor(
        private val retrofitFactory: RetrofitFactory,
        @Unauthenticated private val unauthenticatedOkHttpClient: OkHttpClient
) : SygnalNotifyTask {

    override suspend fun execute(params: SygnalNotifyTask.Params) {
        val sygnalApi = retrofitFactory.create(
                unauthenticatedOkHttpClient,
                params.url.substringBefore(NetworkConstants.URI_SYGNAL_PREFIX_PATH)
        )
                .create(SygnalAPI::class.java)

        val response = executeRequest<SygnalNotifyResponse>(null) {
            apiCall = sygnalApi.notify(
                    SygnalNotifyBody(
                            SygnalNotification(
                                    eventId = params.eventId,
                                    devices = listOf(
                                            SygnalDevice(
                                                    params.appId,
                                                    params.pushKey
                                            )
                                    )
                            )
                    )
            )
        }

        if (response.rejectedPushKey.contains(params.pushKey)) {
            throw SygnalFailure.PusherRejected
        }
    }
}
