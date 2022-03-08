/*
 * Copyright 2021 The Matrix.org Foundation C.I.C.
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
import org.matrix.android.sdk.internal.session.space.SpaceApi
import org.matrix.android.sdk.test.fixtures.ResolveSpaceInfoTaskParamsFixture
import org.matrix.android.sdk.test.fixtures.SpacesResponseFixture

internal class FakeSpaceApi {

    val instance: SpaceApi = mockk()
    val params = ResolveSpaceInfoTaskParamsFixture.aResolveSpaceInfoTaskParams()
    val response = SpacesResponseFixture.aSpacesResponse()

    fun givenStableEndpointWorks() {
        coEvery { instance.getSpaceHierarchy(params.spaceId, params.suggestedOnly, params.limit, params.maxDepth, params.from) } returns response
    }

    fun givenStableEndpointFails() {
        coEvery { instance.getSpaceHierarchy(params.spaceId, params.suggestedOnly, params.limit, params.maxDepth, params.from) } throws Exception()
    }

    fun givenUnstableEndpointWorks() {
        coEvery { instance.getSpaceHierarchyUnstable(params.spaceId, params.suggestedOnly, params.limit, params.maxDepth, params.from) } returns response
    }
}
