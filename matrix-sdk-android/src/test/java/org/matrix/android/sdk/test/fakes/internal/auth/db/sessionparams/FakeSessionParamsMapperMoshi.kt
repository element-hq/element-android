/*
 * Copyright (c) 2022 The Matrix.org Foundation C.I.C.
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

package org.matrix.android.sdk.test.fakes.internal.auth.db.sessionparams

import android.net.Uri
import com.squareup.moshi.Moshi
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldBeNull
import org.matrix.android.sdk.api.auth.LoginType
import org.matrix.android.sdk.api.auth.data.Credentials
import org.matrix.android.sdk.api.auth.data.HomeServerConnectionConfig
import org.matrix.android.sdk.api.auth.data.SessionParams
import org.matrix.android.sdk.api.auth.data.sessionId
import org.matrix.android.sdk.internal.auth.db.SessionParamsEntity
import org.matrix.android.sdk.test.fakes.internal.auth.db.sessionparams.FakeCredentialsJsonAdapter.Companion.CREDENTIALS_JSON
import org.matrix.android.sdk.test.fakes.internal.auth.db.sessionparams.FakeCredentialsJsonAdapter.Companion.credentials
import org.matrix.android.sdk.test.fakes.internal.auth.db.sessionparams.FakeHomeServerConnectionConfigJsonAdapter.Companion.HOME_SERVER_CONNECTION_CONFIG_JSON
import org.matrix.android.sdk.test.fakes.internal.auth.db.sessionparams.FakeHomeServerConnectionConfigJsonAdapter.Companion.homeServerConnectionConfig
import org.matrix.android.sdk.test.fixtures.SessionParamsEntityFixture.aSessionParamsEntity
import org.matrix.android.sdk.test.fixtures.SessionParamsFixture.aSessionParams

internal class FakeSessionParamsMapperMoshi {

    val instance: Moshi = mockk()
    private val credentialsJsonAdapter = FakeCredentialsJsonAdapter()
    private val homeServerConnectionConfigAdapter = FakeHomeServerConnectionConfigJsonAdapter()

    init {
        mockkStatic(Uri::class)
        every { Uri.parse(any()) } returns mockk()
        every { instance.adapter(Credentials::class.java) } returns credentialsJsonAdapter.instance
        every { instance.adapter(HomeServerConnectionConfig::class.java) } returns homeServerConnectionConfigAdapter.instance
    }

    fun assertSessionParamsWasMappedSuccessfully(sessionParams: SessionParams?) {
        sessionParams shouldBeEqualTo SessionParams(
                credentials,
                homeServerConnectionConfig,
                sessionParamsEntity.isTokenValid,
                LoginType.fromName(sessionParamsEntity.loginType)
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
                sessionParams.loginType.name,
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
    }
}
