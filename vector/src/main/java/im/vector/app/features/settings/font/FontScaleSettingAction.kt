/*
 * Copyright 2022-2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package im.vector.app.features.settings.font

import im.vector.app.core.platform.VectorViewModelAction
import im.vector.app.features.settings.FontScaleValue

sealed class FontScaleSettingAction : VectorViewModelAction {
    data class UseSystemSettingChangedAction(val useSystemSettings: Boolean) : FontScaleSettingAction()
    data class FontScaleChangedAction(val fontScale: FontScaleValue) : FontScaleSettingAction()
}
