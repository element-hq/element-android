/*
 * Copyright (c) 2021 New Vector Ltd
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

package fr.gouv.tchap.android.sdk.internal.services.threepidplatformdiscover

import dagger.Lazy
import fr.gouv.tchap.android.sdk.api.services.threepidplatformdiscover.ThreePidPlatformDiscoverService
import fr.gouv.tchap.android.sdk.internal.services.threepidplatformdiscover.model.Platform
import okhttp3.OkHttpClient
import org.matrix.android.sdk.api.session.identity.ThreePid
import org.matrix.android.sdk.api.session.identity.toMedium
import org.matrix.android.sdk.internal.di.Unauthenticated
import org.matrix.android.sdk.internal.network.RetrofitFactory
import javax.inject.Inject

internal class TchapThreePidPlatformDiscoverService @Inject constructor(
        @Unauthenticated
        private val unauthenticatedOkHttpClient: Lazy<OkHttpClient>,
        private val retrofitFactory: RetrofitFactory,
        private val threePidPlatformDiscoverTask: ThreePidPlatformDiscoverTask
) : ThreePidPlatformDiscoverService {

    override suspend fun getPlatform(url: String, threePid: ThreePid): Platform {
        val api = retrofitFactory.create(unauthenticatedOkHttpClient, url).create(ThreePidPlatformDiscoverAPI::class.java)
        val params = ThreePidPlatformDiscoverTask.Params(api, threePid.value, threePid.toMedium())

        return threePidPlatformDiscoverTask.execute(params)
    }
}
