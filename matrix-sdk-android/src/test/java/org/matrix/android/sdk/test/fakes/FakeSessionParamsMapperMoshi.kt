/*
 * Copyright (c) 2022 New Vector Ltd
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

package org.matrix.android.sdk.test.fakes

import com.squareup.moshi.JsonAdapter
import com.squareup.moshi.Moshi
import io.mockk.every
import io.mockk.mockk
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldBeNull
import org.matrix.android.sdk.api.auth.data.Credentials
import org.matrix.android.sdk.api.auth.data.HomeServerConnectionConfig
import org.matrix.android.sdk.api.auth.data.SessionParams
import org.matrix.android.sdk.api.auth.data.sessionId
import org.matrix.android.sdk.internal.auth.db.SessionParamsEntity
import org.matrix.android.sdk.internal.auth.login.LoginType
import org.matrix.android.sdk.test.fixtures.SessionParamsEntityFixture.aSessionParamsEntity
import org.matrix.android.sdk.test.fixtures.SessionParamsFixture.aSessionParams

internal class FakeSessionParamsMapperMoshi {

    val instance: Moshi = mockk()
    private val credentialsAdapter: JsonAdapter<Credentials> = mockk()
    private val homeServerConnectionConfigAdapter: JsonAdapter<HomeServerConnectionConfig> = mockk()

    init {
       stubAdapters()
       stubJsonConversions()
    }

    private fun stubAdapters() {
        every { instance.adapter(Credentials::class.java) } returns credentialsAdapter
        every { instance.adapter(HomeServerConnectionConfig::class.java) } returns homeServerConnectionConfigAdapter
    }

    private fun stubJsonConversions() {
        every { credentialsAdapter.fromJson(sessionParamsEntity.credentialsJson) } returns credentials
        every { homeServerConnectionConfigAdapter.fromJson(sessionParamsEntity.homeServerConnectionConfigJson) } returns homeServerConnectionConfig
        every { credentialsAdapter.toJson(sessionParams.credentials) } returns CREDENTIALS_JSON
        every { homeServerConnectionConfigAdapter.toJson(sessionParams.homeServerConnectionConfig) } returns HOME_SERVER_CONNECTION_CONFIG_JSON
    }

    fun givenCredentialsFromJsonIsNull() {
        every { credentialsAdapter.fromJson(sessionParamsEntity.credentialsJson) } returns null
    }

    fun givenHomeServerConnectionConfigFromJsonIsNull() {
        every { homeServerConnectionConfigAdapter.fromJson(sessionParamsEntity.homeServerConnectionConfigJson) } returns null
    }

    fun givenCredentialsToJsonIsNull() {
        every { credentialsAdapter.toJson(credentials) } returns null
    }

    fun givenHomeServerConnectionConfigToJsonIsNull() {
        every { homeServerConnectionConfigAdapter.toJson(homeServerConnectionConfig) } returns null
    }

    fun assertSessionParamsWasMappedSuccessfully(sessionParams: SessionParams?) {
        sessionParams shouldBeEqualTo SessionParams(
                credentials,
                homeServerConnectionConfig,
                sessionParamsEntity.isTokenValid,
                LoginType.fromValue(sessionParamsEntity.loginType)
        )
    }

    fun assertSessionParamsIsNull(sessionParams: SessionParams?) {
        sessionParams.shouldBeNull()
    }

    fun assertSessionParamsEntityWasMappedSuccessfully(sessionParamsEntity: SessionParamsEntity?) {
        sessionParamsEntity shouldBeEqualTo SessionParamsEntity(
                sessionParams.credentials.sessionId(),
                sessionParams.userId,
                CREDENTIALS_JSON,
                HOME_SERVER_CONNECTION_CONFIG_JSON,
                sessionParams.isTokenValid,
                sessionParams.loginType.value,
        )
    }

    fun assertSessionParamsEntityIsNull(sessionParamsEntity: SessionParamsEntity?) {
        sessionParamsEntity.shouldBeNull()
    }

    companion object {
        val sessionParams = aSessionParams()
        val sessionParamsEntity = aSessionParamsEntity()
        val nullSessionParams: SessionParams? = null
        val nullSessionParamsEntity: SessionParamsEntity? = null

        private val credentials: Credentials = mockk()
        private val homeServerConnectionConfig: HomeServerConnectionConfig = mockk()
        private const val CREDENTIALS_JSON = "credentials_json"
        private const val HOME_SERVER_CONNECTION_CONFIG_JSON = "home_server_connection_config_json"
    }
}
