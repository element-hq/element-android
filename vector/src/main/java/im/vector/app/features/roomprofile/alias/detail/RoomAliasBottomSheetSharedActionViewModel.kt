/*
 * Copyright 2019-2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */
package im.vector.app.features.roomprofile.alias.detail

import im.vector.app.core.platform.VectorSharedActionViewModel
import javax.inject.Inject

/**
 * Activity shared view model to handle room alias quick actions.
 */
class RoomAliasBottomSheetSharedActionViewModel @Inject constructor() : VectorSharedActionViewModel<RoomAliasBottomSheetSharedAction>()
