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

package org.matrix.client

import com.squareup.moshi.Moshi
import okhttp3.OkHttpClient
import org.matrix.client.api.AccountAPI
import org.matrix.client.api.AccountDataAPI
import org.matrix.client.api.CapabilitiesAPI
import org.matrix.client.api.CryptoAPI
import org.matrix.client.api.DirectoryAPI
import org.matrix.client.api.FederationAPI
import org.matrix.client.api.FilterAPI
import org.matrix.client.api.GroupAPI
import org.matrix.client.api.IdentityAPI
import org.matrix.client.api.IdentityAuthAPI
import org.matrix.client.api.MediaAPI
import org.matrix.client.api.OpenIdAPI
import org.matrix.client.api.ProfileAPI
import org.matrix.client.api.PushGatewayAPI
import org.matrix.client.api.PushRulesAPI
import org.matrix.client.api.PushersAPI
import org.matrix.client.api.RoomAPI
import org.matrix.client.api.RoomKeysAPI
import org.matrix.client.api.SearchAPI
import org.matrix.client.api.SearchUserAPI
import org.matrix.client.api.SignOutAPI
import org.matrix.client.api.SpaceAPI
import org.matrix.client.api.SyncAPI
import org.matrix.client.api.TermsAPI
import org.matrix.client.api.ThirdPartyAPI
import org.matrix.client.api.VoipAPI
import org.matrix.client.api.WellKnownAPI
import org.matrix.client.api.WidgetsAPI
import org.matrix.client.utils.RetrofitFactory

class MatrixAuthenticatedClient(private val okHttpClient: OkHttpClient,
                                private val moshi: Moshi,
                                private val baseUrl: String,
                                private val accessTokenProvider: () -> String?) {

    private val retrofit by lazy {
        RetrofitFactory(moshi).create(okHttpClient, baseUrl, accessTokenProvider)
    }

    val accountAPI by lazy {
        retrofit.create(AccountAPI::class.java)
    }

    val accountDataAPI by lazy {
        retrofit.create(AccountDataAPI::class.java)
    }

    val capabilitiesAPI by lazy {
        retrofit.create(CapabilitiesAPI::class.java)
    }

    val cryptoAPI by lazy {
        retrofit.create(CryptoAPI::class.java)
    }

    val directoryAPI by lazy {
        retrofit.create(DirectoryAPI::class.java)
    }

    val federationAPI by lazy {
        retrofit.create(FederationAPI::class.java)
    }

    val filterAPI by lazy {
        retrofit.create(FilterAPI::class.java)
    }

    val groupAPI by lazy {
        retrofit.create(GroupAPI::class.java)
    }

    val identityAPI by lazy {
        retrofit.create(IdentityAPI::class.java)
    }

    val identityAuthAPI by lazy {
        retrofit.create(IdentityAuthAPI::class.java)
    }

    val mediaAPI by lazy {
        retrofit.create(MediaAPI::class.java)
    }

    val openIdAPI by lazy {
        retrofit.create(OpenIdAPI::class.java)
    }

    val profileAPI by lazy {
        retrofit.create(ProfileAPI::class.java)
    }

    val pushersAPI by lazy {
        retrofit.create(PushersAPI::class.java)
    }

    val pushGatewayAPI by lazy {
        retrofit.create(PushGatewayAPI::class.java)
    }

    val pushRulesAPI by lazy {
        retrofit.create(PushRulesAPI::class.java)
    }

    val roomAPI by lazy {
        retrofit.create(RoomAPI::class.java)
    }

    val roomKeysAPI by lazy {
        retrofit.create(RoomKeysAPI::class.java)
    }

    val searchAPI by lazy {
        retrofit.create(SearchAPI::class.java)
    }

    val searchUserAPI by lazy {
        retrofit.create(SearchUserAPI::class.java)
    }

    val signOutAPI by lazy {
        retrofit.create(SignOutAPI::class.java)
    }

    val spaceAPI by lazy {
        retrofit.create(SpaceAPI::class.java)
    }

    val syncAPI by lazy {
        retrofit.create(SyncAPI::class.java)
    }

    val termsAPI by lazy {
        retrofit.create(TermsAPI::class.java)
    }

    val thirdPartyAPI by lazy {
        retrofit.create(ThirdPartyAPI::class.java)
    }

    val voipAPI by lazy {
        retrofit.create(VoipAPI::class.java)
    }

    val wellKnownAPI by lazy {
        retrofit.create(WellKnownAPI::class.java)
    }

    val widgetsAPI by lazy {
        retrofit.create(WidgetsAPI::class.java)
    }
}
