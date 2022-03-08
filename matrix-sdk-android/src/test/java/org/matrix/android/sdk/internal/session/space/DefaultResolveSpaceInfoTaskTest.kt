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

package org.matrix.android.sdk.internal.session.space

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runBlockingTest
import org.amshove.kluent.shouldBeEqualTo
import org.junit.Test
import org.matrix.android.sdk.test.fakes.FakeGlobalErrorReceiver
import org.matrix.android.sdk.test.fakes.FakeSpaceApi

@ExperimentalCoroutinesApi
class DefaultResolveSpaceInfoTaskTest {

    private val spaceApi = FakeSpaceApi()
    private val globalErrorReceiver = FakeGlobalErrorReceiver()
    private val resolveSpaceInfoTask = DefaultResolveSpaceInfoTask(spaceApi.instance, globalErrorReceiver)

    @Test
    fun `given stable endpoint works, when execute, then return stable api data`() = runBlockingTest {
        spaceApi.givenStableEndpointWorks()

        val result = resolveSpaceInfoTask.execute(spaceApi.params)

        result shouldBeEqualTo spaceApi.response
    }

    @Test
    fun `given stable endpoint fails, when execute, then fallback to unstable endpoint`() = runBlockingTest {
        spaceApi.givenStableEndpointFails()
        spaceApi.givenUnstableEndpointWorks()

        val result = resolveSpaceInfoTask.execute(spaceApi.params)

        result shouldBeEqualTo spaceApi.response
    }
}
