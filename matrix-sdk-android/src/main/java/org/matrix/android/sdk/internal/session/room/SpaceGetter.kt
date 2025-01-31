/*
 * Copyright 2025 New Vector Ltd.
 * Copyright 2021 The Matrix.org Foundation C.I.C.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package org.matrix.android.sdk.internal.session.room

import org.matrix.android.sdk.api.session.space.Space
import javax.inject.Inject

internal interface SpaceGetter {
    fun get(spaceId: String): Space?
}

internal class DefaultSpaceGetter @Inject constructor(
        private val roomGetter: RoomGetter
) : SpaceGetter {

    override fun get(spaceId: String): Space? {
        return roomGetter.getRoom(spaceId)?.asSpace()
    }
}
