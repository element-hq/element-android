/*
 * Copyright 2019-2025 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */
package im.vector.app.features.home.room.list.actions

import im.vector.app.core.platform.VectorSharedActionViewModel
import javax.inject.Inject

/**
 * Activity shared view model to handle room list quick actions.
 */
class RoomListQuickActionsSharedActionViewModel @Inject constructor() : VectorSharedActionViewModel<RoomListQuickActionsSharedAction>()
