/*
 * Copyright 2025 New Vector Ltd.
 * Copyright 2021 The Matrix.org Foundation C.I.C.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package org.matrix.android.sdk.test.fixtures

import org.matrix.android.sdk.internal.session.space.SpaceChildSummaryResponse
import org.matrix.android.sdk.internal.session.space.SpacesResponse

internal object SpacesResponseFixture {
    fun aSpacesResponse(
            nextBatch: String? = null,
            rooms: List<SpaceChildSummaryResponse>? = null,
    ) = SpacesResponse(
            nextBatch,
            rooms,
    )
}
