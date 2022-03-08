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

import io.mockk.coEvery
import io.mockk.mockk
import okhttp3.ResponseBody.Companion.toResponseBody
import org.matrix.android.sdk.internal.session.space.SpaceApi
import org.matrix.android.sdk.internal.session.space.SpacesResponse
import org.matrix.android.sdk.test.fixtures.ResolveSpaceInfoTaskParamsFixture
import org.matrix.android.sdk.test.fixtures.SpacesResponseFixture
import retrofit2.HttpException
import retrofit2.Response

internal class FakeSpaceApi {

    val instance: SpaceApi = mockk()
    val params = ResolveSpaceInfoTaskParamsFixture.aResolveSpaceInfoTaskParams()
    val response = SpacesResponseFixture.aSpacesResponse()

    fun givenStableEndpointWorks() {
        coEvery { instance.getSpaceHierarchy(params.spaceId, params.suggestedOnly, params.limit, params.maxDepth, params.from) } returns response
    }

    fun givenStableEndpointFails() {
        val errorResponse = Response.error<SpacesResponse>(500, "".toResponseBody())
        coEvery { instance.getSpaceHierarchy(params.spaceId, params.suggestedOnly, params.limit, params.maxDepth, params.from) } throws HttpException(errorResponse)
    }

    fun givenUnstableEndpointWorks() {
        coEvery { instance.getSpaceHierarchyUnstable(params.spaceId, params.suggestedOnly, params.limit, params.maxDepth, params.from) } returns response
    }
}
