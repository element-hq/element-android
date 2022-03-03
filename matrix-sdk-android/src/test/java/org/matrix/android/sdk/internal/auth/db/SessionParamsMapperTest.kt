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

import io.mockk.every
import org.amshove.kluent.shouldBeNull
import org.junit.Test
import org.matrix.android.sdk.test.fakes.FakeSessionParamsMapperMoshi
import org.matrix.android.sdk.test.fakes.FakeSessionParamsMapperMoshi.Companion.nullSessionParams
import org.matrix.android.sdk.test.fakes.FakeSessionParamsMapperMoshi.Companion.nullSessionParamsEntity
import org.matrix.android.sdk.test.fakes.FakeSessionParamsMapperMoshi.Companion.sessionParams
import org.matrix.android.sdk.test.fakes.FakeSessionParamsMapperMoshi.Companion.sessionParamsEntity

class SessionParamsMapperTest {

    private val fakeMoshi = FakeSessionParamsMapperMoshi()
    private val sessionParamsMapper = SessionParamsMapper(fakeMoshi.instance)

    @Test
    fun `when mapping entity, then map as SessionParams`() {

        val output = sessionParamsMapper.map(sessionParamsEntity)

        fakeMoshi.assertSessionParamsWasMappedSuccessfully(output)
    }

    @Test
    fun `given null input, when mapping entity, then return null`() {

        val output = sessionParamsMapper.map(nullSessionParamsEntity)

        fakeMoshi.assertSessionParamsIsNull(output)
    }

    @Test
    fun `given null credentials, when mapping entity, then return null`() {
        fakeMoshi.givenCredentialsFromJsonIsNull()

        val output = sessionParamsMapper.map(sessionParamsEntity)

        fakeMoshi.assertSessionParamsIsNull(output)
    }

    @Test
    fun `given null homeServerConnectionConfig, when mapping entity, then return null`() {
        fakeMoshi.givenHomeServerConnectionConfigFromJsonIsNull()

        val output = sessionParamsMapper.map(sessionParamsEntity)

        fakeMoshi.assertSessionParamsIsNull(output)
    }

    @Test
    fun `when mapping sessionParams, then map as SessionParamsEntity`() {

        val output = sessionParamsMapper.map(sessionParams)

        fakeMoshi.assertSessionParamsEntityWasMappedSuccessfully(output)
    }

    @Test
    fun `given null input, when mapping sessionParams, then return null`() {

        val output = sessionParamsMapper.map(nullSessionParams)

        fakeMoshi.assertSessionParamsEntityWasMappedSuccessfully(output)
    }

    @Test
    fun `given null credentials json, when mapping sessionParams, then return null`() {
        fakeMoshi.givenCredentialsToJsonIsNull()

        val output = sessionParamsMapper.map(sessionParams)

        fakeMoshi.assertSessionParamsEntityIsNull(output)
    }

    @Test
    fun `given null homeServerConnectionConfig json, when mapping sessionParams, then return null`() {
        fakeMoshi.givenHomeServerConnectionConfigToJsonIsNull()

        val output = sessionParamsMapper.map(sessionParams)

        fakeMoshi.assertSessionParamsEntityIsNull(output)
    }
}
