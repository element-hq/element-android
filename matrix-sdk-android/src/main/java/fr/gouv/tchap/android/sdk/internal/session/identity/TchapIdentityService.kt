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

package fr.gouv.tchap.android.sdk.internal.session.identity

import dagger.Lazy
import fr.gouv.tchap.android.sdk.api.session.identity.IdentityService
import fr.gouv.tchap.sdk.internal.session.identity.model.Platform
import okhttp3.OkHttpClient
import org.matrix.android.sdk.api.session.identity.ThreePid
import org.matrix.android.sdk.api.session.identity.toMedium
import org.matrix.android.sdk.internal.di.Unauthenticated
import org.matrix.android.sdk.internal.network.RetrofitFactory
import javax.inject.Inject

internal class TchapIdentityService @Inject constructor(
        @Unauthenticated
        private val unauthenticatedOkHttpClient: Lazy<OkHttpClient>,
        private val retrofitFactory: RetrofitFactory,
        private val identityRequestHomeServerTask: IdentityRequestHomeServerTask
) : IdentityService {

    override suspend fun getPlatform(url: String, threePid: ThreePid): Platform {
        val api = retrofitFactory.create(unauthenticatedOkHttpClient, url).create(IdentityAPI::class.java)
        val params = IdentityRequestHomeServerTask.Params(api, threePid.value, threePid.toMedium())

        return identityRequestHomeServerTask.execute(params)
    }
}
