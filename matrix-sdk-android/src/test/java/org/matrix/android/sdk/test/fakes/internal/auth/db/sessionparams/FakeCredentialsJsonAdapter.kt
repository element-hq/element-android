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

import com.squareup.moshi.JsonAdapter
import io.mockk.every
import io.mockk.mockk
import org.matrix.android.sdk.api.auth.data.Credentials
import org.matrix.android.sdk.test.fakes.internal.auth.db.sessionparams.FakeSessionParamsMapperMoshi.Companion.sessionParams
import org.matrix.android.sdk.test.fakes.internal.auth.db.sessionparams.FakeSessionParamsMapperMoshi.Companion.sessionParamsEntity
import org.matrix.android.sdk.test.fixtures.CredentialsFixture.aCredentials

internal class FakeCredentialsJsonAdapter {

    val instance: JsonAdapter<Credentials> = mockk()

    init {
        every { instance.fromJson(sessionParamsEntity.credentialsJson) } returns credentials
        every { instance.toJson(sessionParams.credentials) } returns CREDENTIALS_JSON
    }

    fun givenNullDeserialization() {
        every { instance.fromJson(sessionParamsEntity.credentialsJson) } returns null
    }

    fun givenNullSerialization() {
        every { instance.toJson(credentials) } returns null
    }

    companion object {
        val credentials = aCredentials()
        const val CREDENTIALS_JSON = "credentials_json"
    }
}
