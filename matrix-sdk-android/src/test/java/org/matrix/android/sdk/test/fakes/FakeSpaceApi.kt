/*
 * Copyright 2025 New Vector Ltd.
 * Copyright 2021 The Matrix.org Foundation C.I.C.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package org.matrix.android.sdk.test.fakes

import io.mockk.coEvery
import io.mockk.mockk
import org.matrix.android.sdk.internal.session.space.SpaceApi
import org.matrix.android.sdk.internal.session.space.SpacesResponse
import org.matrix.android.sdk.test.fixtures.ResolveSpaceInfoTaskParamsFixture

internal class FakeSpaceApi {

    val instance: SpaceApi = mockk()
    val params = ResolveSpaceInfoTaskParamsFixture.aResolveSpaceInfoTaskParams()

    fun givenStableEndpointReturns(response: SpacesResponse) {
        coEvery { instance.getSpaceHierarchy(params.spaceId, params.suggestedOnly, params.limit, params.maxDepth, params.from) } returns response
    }

    fun givenStableEndpointThrows(throwable: Throwable) {
        coEvery { instance.getSpaceHierarchy(params.spaceId, params.suggestedOnly, params.limit, params.maxDepth, params.from) } throws throwable
    }

    fun givenUnstableEndpointReturns(response: SpacesResponse) {
        coEvery { instance.getSpaceHierarchyUnstable(params.spaceId, params.suggestedOnly, params.limit, params.maxDepth, params.from) } returns response
    }
}
