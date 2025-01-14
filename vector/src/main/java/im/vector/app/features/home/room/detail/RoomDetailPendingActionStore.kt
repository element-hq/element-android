/*
 * Copyright 2020-2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package im.vector.app.features.home.room.detail

import im.vector.app.core.utils.TemporaryStore
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.time.Duration.Companion.seconds

// Store to keep a pending action from sub screen of a room detail
@Singleton
class RoomDetailPendingActionStore @Inject constructor() : TemporaryStore<RoomDetailPendingAction>(10.seconds)
