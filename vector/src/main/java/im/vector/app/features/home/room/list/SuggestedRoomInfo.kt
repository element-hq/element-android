/*
 * Copyright 2021-2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package im.vector.app.features.home.room.list

import com.airbnb.mvrx.Async
import org.matrix.android.sdk.api.session.room.model.SpaceChildInfo

class SuggestedRoomInfo(
        val rooms: List<SpaceChildInfo>,
        val joinEcho: Map<String, Async<Unit>>
)
