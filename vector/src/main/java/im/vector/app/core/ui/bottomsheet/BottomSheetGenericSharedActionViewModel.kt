/*
 * Copyright 2020-2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */
package im.vector.app.core.ui.bottomsheet

import im.vector.app.core.platform.VectorSharedAction
import im.vector.app.core.platform.VectorSharedActionViewModel

/**
 * Activity shared view model to handle bottom sheet quick actions.
 */
abstract class BottomSheetGenericSharedActionViewModel<Action : VectorSharedAction> : VectorSharedActionViewModel<Action>()
