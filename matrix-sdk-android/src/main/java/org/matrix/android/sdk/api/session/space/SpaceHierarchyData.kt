/*
 * Copyright 2025 New Vector Ltd.
 * Copyright 2021 The Matrix.org Foundation C.I.C.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package org.matrix.android.sdk.api.session.space

import org.matrix.android.sdk.api.session.room.model.RoomSummary
import org.matrix.android.sdk.api.session.room.model.SpaceChildInfo
import org.matrix.android.sdk.api.session.space.model.SpaceChildSummaryEvent

data class SpaceHierarchyData(
        val rootSummary: RoomSummary,
        val children: List<SpaceChildInfo>,
        val childrenState: List<SpaceChildSummaryEvent>,
        val nextToken: String? = null
)
