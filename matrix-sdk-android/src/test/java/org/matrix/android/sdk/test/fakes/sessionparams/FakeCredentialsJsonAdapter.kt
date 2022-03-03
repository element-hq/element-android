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

package org.matrix.android.sdk.test.fakes.sessionparams

import com.squareup.moshi.JsonAdapter
import io.mockk.every
import io.mockk.mockk
import org.matrix.android.sdk.api.auth.data.Credentials

internal class FakeCredentialsJsonAdapter {

    val instance: JsonAdapter<Credentials> = mockk()

    init {
        every { instance.fromJson(FakeSessionParamsMapperMoshi.sessionParamsEntity.credentialsJson) } returns credentials
        every { instance.toJson(FakeSessionParamsMapperMoshi.sessionParams.credentials) } returns CREDENTIALS_JSON
    }

    fun givenNullDeserialization() {
        every { instance.fromJson(FakeSessionParamsMapperMoshi.sessionParamsEntity.credentialsJson) } returns null
    }

    fun givenNullSerialization() {
        every { instance.toJson(credentials) } returns null
    }

    companion object {
        val credentials: Credentials = mockk()
        const val CREDENTIALS_JSON = "credentials_json"
    }
}
