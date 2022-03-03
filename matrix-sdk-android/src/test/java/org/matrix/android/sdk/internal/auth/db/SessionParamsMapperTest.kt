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

package org.matrix.android.sdk.internal.auth.db

import com.squareup.moshi.JsonAdapter
import com.squareup.moshi.Moshi
import io.mockk.every
import io.mockk.mockk
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldBeNull
import org.junit.Before
import org.junit.Test
import org.matrix.android.sdk.api.auth.data.Credentials
import org.matrix.android.sdk.api.auth.data.HomeServerConnectionConfig
import org.matrix.android.sdk.api.auth.data.SessionParams
import org.matrix.android.sdk.api.auth.data.sessionId
import org.matrix.android.sdk.internal.auth.login.LoginType
import org.matrix.android.sdk.test.fixtures.SessionParamsEntityFixture.aSessionParamsEntity
import org.matrix.android.sdk.test.fixtures.SessionParamsFixture.aSessionParams

class SessionParamsMapperTest {

    private val moshi: Moshi = mockk()
    private lateinit var sessionParamsMapper: SessionParamsMapper

    private val credentialsAdapter: JsonAdapter<Credentials> = mockk()
    private val homeServerConnectionAdapter: JsonAdapter<HomeServerConnectionConfig> = mockk()

    @Before
    fun setup() {
        every { moshi.adapter(Credentials::class.java) } returns mockk()
        every { moshi.adapter(HomeServerConnectionConfig::class.java) } returns mockk()
        sessionParamsMapper = SessionParamsMapper(moshi)

        every { credentialsAdapter.fromJson(sessionParamsEntity.credentialsJson) } returns credentials
        every { homeServerConnectionAdapter.fromJson(sessionParamsEntity.homeServerConnectionConfigJson) } returns homeServerConnectionConfig
        every { credentialsAdapter.toJson(sessionParams.credentials) } returns CREDENTIALS_JSON
        every { homeServerConnectionAdapter.toJson(sessionParams.homeServerConnectionConfig) } returns HOME_SERVER_CONNECTION_CONFIG_JSON
    }

    @Test
    fun `when mapping entity, then map as SessionParams`() {

        val output = sessionParamsMapper.map(sessionParamsEntity)!!

        output shouldBeEqualTo SessionParams(
                credentials,
                homeServerConnectionConfig,
                sessionParamsEntity.isTokenValid,
                LoginType.fromValue(sessionParamsEntity.loginType)
        )
    }

    @Test
    fun `given null input, when mapping entity, then return null`() {
        val nullEntity: SessionParamsEntity? = null

        val output = sessionParamsMapper.map(nullEntity)

        output.shouldBeNull()
    }

    @Test
    fun `given null credentials, when mapping entity, then return null`() {
        every { credentialsAdapter.fromJson(sessionParamsEntity.credentialsJson) } returns null

        val output = sessionParamsMapper.map(sessionParamsEntity)

        output.shouldBeNull()
    }

    @Test
    fun `given null homeServerConnectionConfig, when mapping entity, then return null`() {
        every { homeServerConnectionAdapter.fromJson(sessionParamsEntity.homeServerConnectionConfigJson) } returns null

        val output = sessionParamsMapper.map(sessionParamsEntity)

        output.shouldBeNull()
    }

    @Test
    fun `when mapping sessionParams, then map as SessionParamsEntity`() {

        val output = sessionParamsMapper.map(sessionParams)

        output shouldBeEqualTo SessionParamsEntity(
                sessionParams.credentials.sessionId(),
                sessionParams.userId,
                CREDENTIALS_JSON,
                HOME_SERVER_CONNECTION_CONFIG_JSON,
                sessionParams.isTokenValid,
                sessionParams.loginType.value,
        )
    }

    @Test
    fun `given null input, when mapping sessionParams, then return null`() {
        val nullSessionParams: SessionParams? = null

        val output = sessionParamsMapper.map(nullSessionParams)

        output.shouldBeNull()
    }

    @Test
    fun `given null credentials json, when mapping sessionParams, then return null`() {
        every { credentialsAdapter.toJson(credentials) } returns null

        val output = sessionParamsMapper.map(sessionParams)

        output.shouldBeNull()
    }

    @Test
    fun `given null homeServerConnectionConfig json, when mapping sessionParams, then return null`() {
        every { homeServerConnectionAdapter.toJson(homeServerConnectionConfig) } returns null

        val output = sessionParamsMapper.map(sessionParams)

        output.shouldBeNull()
    }

    companion object {
        private val sessionParamsEntity = aSessionParamsEntity()
        private val sessionParams = aSessionParams()

        private val credentials: Credentials = mockk()
        private val homeServerConnectionConfig: HomeServerConnectionConfig = mockk()
        private const val CREDENTIALS_JSON = "credentials_json"
        private const val HOME_SERVER_CONNECTION_CONFIG_JSON = "home_server_connection_config_json"
    }
}
