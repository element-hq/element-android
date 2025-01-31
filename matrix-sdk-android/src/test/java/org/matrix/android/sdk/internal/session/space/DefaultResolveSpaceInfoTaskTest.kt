/*
 * Copyright 2025 New Vector Ltd.
 * Copyright 2021 The Matrix.org Foundation C.I.C.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package org.matrix.android.sdk.internal.session.space

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import okhttp3.ResponseBody.Companion.toResponseBody
import org.amshove.kluent.shouldBeEqualTo
import org.junit.Test
import org.matrix.android.sdk.test.fakes.FakeGlobalErrorReceiver
import org.matrix.android.sdk.test.fakes.FakeSpaceApi
import org.matrix.android.sdk.test.fixtures.SpacesResponseFixture.aSpacesResponse
import retrofit2.HttpException
import retrofit2.Response

@ExperimentalCoroutinesApi
internal class DefaultResolveSpaceInfoTaskTest {

    private val spaceApi = FakeSpaceApi()
    private val globalErrorReceiver = FakeGlobalErrorReceiver()
    private val resolveSpaceInfoTask = DefaultResolveSpaceInfoTask(spaceApi.instance, globalErrorReceiver)

    @Test
    fun `given stable endpoint works, when execute, then return stable api data`() = runTest {
        spaceApi.givenStableEndpointReturns(response)

        val result = resolveSpaceInfoTask.execute(spaceApi.params)

        result shouldBeEqualTo response
    }

    @Test
    fun `given stable endpoint fails, when execute, then fallback to unstable endpoint`() = runTest {
        spaceApi.givenStableEndpointThrows(httpException)
        spaceApi.givenUnstableEndpointReturns(response)

        val result = resolveSpaceInfoTask.execute(spaceApi.params)

        result shouldBeEqualTo response
    }

    companion object {
        private val response = aSpacesResponse()
        private val httpException = HttpException(Response.error<SpacesResponse>(500, "".toResponseBody()))
    }
}
